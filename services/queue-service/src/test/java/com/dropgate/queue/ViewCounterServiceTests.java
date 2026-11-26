package com.dropgate.queue;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ViewCounterServiceTests {

    @Test
    void recordsViewsInRedis() {
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        DropViewCountRepository repository = mock(DropViewCountRepository.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("views:drop-9")).thenReturn(4L);

        ViewCounterService service = new ViewCounterService(redisTemplate, repository);

        assertThat(service.recordView("drop-9")).isEqualTo(4L);
    }

    @Test
    void flushesPendingRedisViewsIntoDatabase() {
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        DropViewCountRepository repository = mock(DropViewCountRepository.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.keys("views:*")).thenReturn(Set.of("views:drop-9"));
        when(valueOperations.get("views:drop-9")).thenReturn("3");
        when(repository.findById("drop-9")).thenReturn(Optional.empty());

        ViewCounterService service = new ViewCounterService(redisTemplate, repository);

        service.flushToDatabase();

        verify(repository).save(org.mockito.ArgumentMatchers.argThat(entity ->
                entity.getDropId().equals("drop-9") && entity.getViewCount() == 3L));
        verify(redisTemplate).delete("views:drop-9");
    }
}
