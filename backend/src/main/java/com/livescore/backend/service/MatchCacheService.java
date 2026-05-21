package com.livescore.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MatchCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    // In-memory fallback map if Redis is offline
    private final ConcurrentHashMap<String, String> memoryFallbackMap = new ConcurrentHashMap<>();

    public MatchCacheService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void cacheData(String key, String value, long timeoutSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis write failed, falling back to in-memory map: {}", e.getMessage());
            memoryFallbackMap.put(key, value);
        }
    }

    public String getCachedData(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis read failed, reading from in-memory fallback map: {}", e.getMessage());
            return memoryFallbackMap.get(key);
        }
    }
}
