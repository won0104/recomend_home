package com.ssafy.prj.chat.repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.prj.chat.domain.NeighborhoodChatSession;

@Repository
public class NeighborhoodChatSessionRepository {
    private static final String KEY_PREFIX = "chat:neighborhood:session:";
    private static final Duration TTL = Duration.ofDays(7);

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final Map<String, NeighborhoodChatSession> fallbackStore = new ConcurrentHashMap<>();

    public NeighborhoodChatSessionRepository(ObjectMapper objectMapper,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    public Optional<NeighborhoodChatSession> findById(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        if (redisTemplate != null) {
            try {
                String json = redisTemplate.opsForValue().get(key(sessionId));
                if (json != null) {
                    return Optional.of(objectMapper.readValue(json, NeighborhoodChatSession.class));
                }
            } catch (Exception ignored) {
            }
        }
        return Optional.ofNullable(fallbackStore.get(sessionId));
    }

    public NeighborhoodChatSession save(NeighborhoodChatSession session) {
        session.setUpdatedAt(Instant.now());
        fallbackStore.put(session.getSessionId(), session);
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(key(session.getSessionId()),
                        objectMapper.writeValueAsString(session), TTL);
            } catch (Exception ignored) {
            }
        }
        return session;
    }

    public void delete(String sessionId) {
        if (sessionId == null) {
            return;
        }
        fallbackStore.remove(sessionId);
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(key(sessionId));
            } catch (Exception ignored) {
            }
        }
    }

    private String key(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
