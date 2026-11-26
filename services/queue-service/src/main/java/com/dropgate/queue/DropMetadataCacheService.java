package com.dropgate.queue;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Service
public class DropMetadataCacheService {

    private static final long DEFAULT_TTL_MS = 30_000L;
    private static final long NEGATIVE_TTL_MS = 5_000L;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Map<String, Object> inflightLocks = new ConcurrentHashMap<>();

    public Optional<DropMetadata> get(String dropId, Supplier<Optional<DropMetadata>> loader) {
        if (dropId == null || dropId.isBlank()) {
            return Optional.empty();
        }

        CacheEntry entry = cache.get(dropId);
        if (entry != null && entry.expiresAt.isAfter(Instant.now())) {
            return entry.value == null ? Optional.empty() : Optional.of(entry.value);
        }

        Object lock = inflightLocks.computeIfAbsent(dropId, key -> new Object());
        synchronized (lock) {
            entry = cache.get(dropId);
            if (entry != null && entry.expiresAt.isAfter(Instant.now())) {
                return entry.value == null ? Optional.empty() : Optional.of(entry.value);
            }

            Optional<DropMetadata> loaded = loader.get();
            long jitter = ThreadLocalRandom.current().nextLong(0, 5_000L);
            long ttlMs = loaded.isPresent() ? DEFAULT_TTL_MS + jitter : NEGATIVE_TTL_MS;
            Instant expiresAt = Instant.now().plusMillis(ttlMs);
            cache.put(dropId, new CacheEntry(loaded.orElse(null), expiresAt));
            inflightLocks.remove(dropId);
            return loaded;
        }
    }

    private record CacheEntry(DropMetadata value, Instant expiresAt) {
    }
}
