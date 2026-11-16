
package com.poorvi.url_shortener.ratelimit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    public boolean isAllowed(
            String key,
            int maxRequests,
            Duration duration) {

        Long requests = redisTemplate.opsForValue()
                .increment(key);

        if (requests == null) {
            return false;
        }

        if (requests == 1) {
            redisTemplate.expire(key, duration);
        }

        return requests <= maxRequests;
    }
}
