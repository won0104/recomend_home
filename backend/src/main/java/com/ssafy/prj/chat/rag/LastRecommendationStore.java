package com.ssafy.prj.chat.rag;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class LastRecommendationStore {

    private static final Logger log = LoggerFactory.getLogger(LastRecommendationStore.class);
    private static final String KEY_PREFIX = "chat:last-recommendation:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatRagProperties properties;

    public LastRecommendationStore(
        StringRedisTemplate redisTemplate,
        ObjectMapper objectMapper,
        ChatRagProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public void save(String sessionId, NeighborhoodRecommendationSnapshot snapshot) {
        if (snapshot == null || snapshot.recommendations() == null || snapshot.recommendations().isEmpty()) {
            return;
        }
        try {
            String key = key(sessionId);
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(snapshot), properties.getHistoryTtl());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to save last recommendation snapshot.", exception);
        } catch (RuntimeException exception) {
            log.warn("Last recommendation snapshot was not saved. sessionId={}", sessionId, exception);
        }
    }

    public Optional<NeighborhoodRecommendationSnapshot> find(String sessionId) {
        String json;
        try {
            json = redisTemplate.opsForValue().get(key(sessionId));
        } catch (RuntimeException exception) {
            log.warn("Last recommendation snapshot was not loaded. sessionId={}", sessionId, exception);
            return Optional.empty();
        }
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, NeighborhoodRecommendationSnapshot.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to read last recommendation snapshot.", exception);
        }
    }

    private String key(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
