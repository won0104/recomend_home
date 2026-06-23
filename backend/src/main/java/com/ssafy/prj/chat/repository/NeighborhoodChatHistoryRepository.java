package com.ssafy.prj.chat.repository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.prj.chat.domain.NeighborhoodChatHistoryMessage;

@Repository
public class NeighborhoodChatHistoryRepository {
    private static final String KEY_PREFIX = "chat:neighborhood:history:";
    private static final Duration TTL = Duration.ofDays(7);
    private static final int MAX_HISTORY_SIZE = 24;

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final Map<String, List<NeighborhoodChatHistoryMessage>> fallbackStore = new ConcurrentHashMap<>();

    public NeighborhoodChatHistoryRepository(ObjectMapper objectMapper,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    public void appendUser(String sessionId, String content) {
        append(sessionId, "user", content);
    }

    public void appendAssistant(String sessionId, String content) {
        append(sessionId, "assistant", content);
    }

    public List<NeighborhoodChatHistoryMessage> recent(String sessionId, int limit) {
        if (sessionId == null || sessionId.isBlank() || limit <= 0) {
            return List.of();
        }

        if (redisTemplate != null) {
            try {
                long size = redisTemplate.opsForList().size(key(sessionId));
                if (size <= 0) {
                    return List.of();
                }
                long start = Math.max(0, size - limit);
                List<String> values = redisTemplate.opsForList().range(key(sessionId), start, size - 1);
                if (values == null || values.isEmpty()) {
                    return List.of();
                }
                List<NeighborhoodChatHistoryMessage> messages = new ArrayList<>();
                for (String value : values) {
                    messages.add(objectMapper.readValue(value, NeighborhoodChatHistoryMessage.class));
                }
                return messages;
            } catch (Exception ignored) {
            }
        }

        List<NeighborhoodChatHistoryMessage> messages = fallbackStore.getOrDefault(sessionId, List.of());
        if (messages.isEmpty()) {
            return List.of();
        }
        int start = Math.max(0, messages.size() - limit);
        return new ArrayList<>(messages.subList(start, messages.size()));
    }

    public void clear(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
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

    private void append(String sessionId, String role, String content) {
        if (sessionId == null || sessionId.isBlank() || content == null) {
            return;
        }

        NeighborhoodChatHistoryMessage message = new NeighborhoodChatHistoryMessage(role, content, Instant.now());
        fallbackStore.compute(sessionId, (key, existing) -> {
            List<NeighborhoodChatHistoryMessage> values = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
            values.add(message);
            trim(values);
            return values;
        });

        if (redisTemplate != null) {
            try {
                String serialized = objectMapper.writeValueAsString(message);
                redisTemplate.opsForList().rightPush(key(sessionId), serialized);
                redisTemplate.opsForList().trim(key(sessionId), -MAX_HISTORY_SIZE, -1);
                redisTemplate.expire(key(sessionId), TTL);
            } catch (Exception ignored) {
            }
        }
    }

    private void trim(List<NeighborhoodChatHistoryMessage> messages) {
        if (messages.size() <= MAX_HISTORY_SIZE) {
            return;
        }
        int removeCount = messages.size() - MAX_HISTORY_SIZE;
        messages.subList(0, removeCount).clear();
    }

    private String key(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
