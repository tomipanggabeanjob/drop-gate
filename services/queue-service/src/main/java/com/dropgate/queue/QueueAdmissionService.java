package com.dropgate.queue;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class QueueAdmissionService {

    private final IMap<String, QueueEntry> lotteryQueue;
    private final IMap<String, String> tickets;
    private final IMap<String, AdmissionToken> admissions;
    private final Map<String, List<SseEmitter>> emittersByDrop = new ConcurrentHashMap<>();
    private final RateLimitService rateLimitService;
    private final ClickEventPublisher clickEventPublisher;

    public QueueAdmissionService(
            RateLimitService rateLimitService,
            ClickEventPublisher clickEventPublisher,
            HazelcastInstance hazelcastInstance) {
        this.rateLimitService = rateLimitService;
        this.clickEventPublisher = clickEventPublisher;
        this.lotteryQueue = hazelcastInstance.getMap("dropgate-queue-entries-v1");
        this.tickets = hazelcastInstance.getMap("dropgate-queue-tickets-v1");
        this.admissions = hazelcastInstance.getMap("dropgate-admissions-v1");
    }

    public QueueAdmissionResponse admit(String dropId, QueueAdmissionRequest request) {
        if (dropId == null || dropId.isBlank()) {
            throw new IllegalArgumentException("dropId is required");
        }
        if (request == null || request.accountId() == null || request.accountId().isBlank()) {
            throw new IllegalArgumentException("accountId is required");
        }

        String normalizedDrop = dropId.trim();
        String accountId = request.accountId().trim();
        String fingerprint = request.fingerprint() == null ? accountId : request.fingerprint().trim();
        if (!rateLimitService.allow(fingerprint)) {
            throw new RateLimitExceededException(fingerprint);
        }

        String ticketKey = stateKey(normalizedDrop, accountId);
        String token;
        lotteryQueue.lock(normalizedDrop);
        try {
            String existingToken = tickets.get(ticketKey);
            if (existingToken != null) {
                return new QueueAdmissionResponse(
                        normalizedDrop,
                        accountId,
                        existingToken,
                        queuePosition(normalizedDrop, accountId));
            }

            token = UUID.randomUUID().toString();
            tickets.put(ticketKey, token);
            lotteryQueue.put(
                    UUID.randomUUID().toString(),
                    new QueueEntry(normalizedDrop, accountId, ThreadLocalRandom.current().nextLong(Long.MAX_VALUE)));
        } finally {
            lotteryQueue.unlock(normalizedDrop);
        }

        clickEventPublisher.publish(normalizedDrop, accountId, fingerprint, "queue_join");
        long position = queuePosition(normalizedDrop, accountId);
        emitPositionUpdate(normalizedDrop, accountId, token, position);
        return new QueueAdmissionResponse(normalizedDrop, accountId, token, position);
    }

    public List<String> getActiveDrops() {
        return lotteryQueue.values().stream()
                .map(QueueEntry::dropId)
                .distinct()
                .toList();
    }

    public QueuePositionResponse position(String dropId, String accountId) {
        if (dropId == null || dropId.isBlank()) {
            throw new IllegalArgumentException("dropId is required");
        }
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("accountId is required");
        }

        long position = queuePosition(dropId, accountId);
        String token = tickets.get(stateKey(dropId, accountId));
        return new QueuePositionResponse(dropId, accountId, token, position);
    }

    public SseEmitter stream(String dropId, String accountId) {
        SseEmitter emitter = new SseEmitter(30_000L);
        emittersByDrop.computeIfAbsent(dropId, ignored -> Collections.synchronizedList(new ArrayList<>())).add(emitter);

        emitter.onCompletion(() -> removeEmitter(dropId, emitter));
        emitter.onTimeout(() -> removeEmitter(dropId, emitter));
        emitter.onError((error) -> removeEmitter(dropId, emitter));

        String token = tickets.get(stateKey(dropId, accountId));
        long currentPosition = queuePosition(dropId, accountId);
        try {
            emitter.send(new QueuePositionResponse(dropId, accountId, token, currentPosition));
        } catch (IOException ex) {
            removeEmitter(dropId, emitter);
        }

        return emitter;
    }

    public List<String> nextBatch(String dropId, int batchSize) {
        if (dropId == null || dropId.isBlank()) {
            throw new IllegalArgumentException("dropId is required");
        }

        List<String> batch = new ArrayList<>();
        if (batchSize <= 0) {
            return batch;
        }

        lotteryQueue.lock(dropId);
        try {
            List<Map.Entry<String, QueueEntry>> queued = lotteryQueue.entrySet().stream()
                    .filter(entry -> entry.getValue().dropId().equals(dropId))
                    .sorted(Comparator.comparingLong(entry -> entry.getValue().score()))
                    .limit(batchSize)
                    .toList();
            for (Map.Entry<String, QueueEntry> entry : queued) {
                batch.add(entry.getValue().accountId());
                lotteryQueue.remove(entry.getKey());
            }
        } finally {
            lotteryQueue.unlock(dropId);
        }
        return batch;
    }

    public AdmissionToken issueAdmissionToken(String dropId, String accountId, long ttlMillis) {
        String tokenValue = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusMillis(ttlMillis);
        AdmissionToken token = new AdmissionToken(dropId, accountId, tokenValue, expiresAt);
        admissions.put(stateKey(dropId, accountId), token);
        return token;
    }

    public List<AdmissionToken> activeAdmissions(String dropId) {
        return admissions.values().stream()
                .filter(token -> token.dropId().equals(dropId) && token.expiresAt().isAfter(Instant.now()))
                .sorted((a, b) -> a.expiresAt().compareTo(b.expiresAt()))
                .toList();
    }

    public void clearAdmission(String dropId, String accountId) {
        admissions.remove(stateKey(dropId, accountId));
    }

    public void pushAdmissionEvent(String dropId, String accountId, String token) {
        tickets.put(stateKey(dropId, accountId), token);
        emitPositionUpdate(dropId, accountId, token, 0L);
    }

    private long queuePosition(String dropId, String accountId) {
        List<QueueEntry> queued = lotteryQueue.values().stream()
                .filter(entry -> entry.dropId().equals(dropId))
                .sorted(Comparator.comparingLong(QueueEntry::score))
                .toList();
        for (int index = 0; index < queued.size(); index++) {
            if (queued.get(index).accountId().equals(accountId)) {
                return index + 1L;
            }
        }
        return -1L;
    }

    private String stateKey(String dropId, String accountId) {
        return dropId.length() + ":" + dropId + ":" + accountId;
    }

    private void emitPositionUpdate(String dropId, String accountId, String token, long position) {
        List<SseEmitter> emitters = emittersByDrop.getOrDefault(dropId, Collections.emptyList());
        QueuePositionResponse payload = new QueuePositionResponse(dropId, accountId, token, position);

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(payload);
            } catch (IOException ignored) {
                removeEmitter(dropId, emitter);
            }
        }
    }

    private void removeEmitter(String dropId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByDrop.get(dropId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByDrop.remove(dropId);
        }
    }

    private record QueueEntry(String dropId, String accountId, long score) implements Serializable {
    }
}
