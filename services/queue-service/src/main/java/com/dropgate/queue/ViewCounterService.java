package com.dropgate.queue;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class ViewCounterService {

    private static final String VIEW_KEY_PREFIX = "views:";

    private final RedisTemplate<String, String> redisTemplate;
    private final DropViewCountRepository repository;

    public ViewCounterService(RedisTemplate<String, String> redisTemplate, DropViewCountRepository repository) {
        this.redisTemplate = redisTemplate;
        this.repository = repository;
    }

    public long recordView(String dropId) {
        try {
            return redisTemplate.opsForValue().increment(VIEW_KEY_PREFIX + dropId);
        } catch (RuntimeException ignored) {
            return 0L;
        }
    }

    @Scheduled(fixedDelay = 5_000)
    public void flushToDatabase() {
        try {
            Set<String> keys = redisTemplate.keys(VIEW_KEY_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                return;
            }

            for (String key : keys) {
                String dropId = key.substring(VIEW_KEY_PREFIX.length());
                String value = redisTemplate.opsForValue().get(key);
                if (value == null || value.isBlank()) {
                    continue;
                }
                long viewCount = Long.parseLong(value);
                DropViewCount entity = repository.findById(dropId)
                        .orElse(new DropViewCount(dropId, 0L));
                entity.setViewCount(entity.getViewCount() + viewCount);
                repository.save(entity);
                redisTemplate.delete(key);
            }
        } catch (RuntimeException ignored) {
            // Retry on the next schedule when Redis is unavailable.
        }
    }
}
