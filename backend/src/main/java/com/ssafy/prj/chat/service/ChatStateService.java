package com.ssafy.prj.chat.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.prj.chat.dto.ChatBotSlotDTO;
import com.ssafy.prj.chat.dto.ChatConversationState;
import com.ssafy.prj.chat.dto.ChatMessageDTO;
import com.ssafy.prj.chat.dto.ChatSessionState;
import com.ssafy.prj.model.dto.neighborhood.RecommendedNeighborhood;

@Service
public class ChatStateService {
    private static final Logger log = LoggerFactory.getLogger(ChatStateService.class);
    private static final String MESSAGE_KEY_PREFIX = "chat:messages:";
    private static final String STATE_KEY_PREFIX = "chat:state:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;
    private final int maxHistorySize;
    private final Map<String, List<ChatMessageDTO>> fallbackMessages = new ConcurrentHashMap<>();
    private final Map<String, ChatSessionState> fallbackStates = new ConcurrentHashMap<>();

    public ChatStateService(
        StringRedisTemplate redisTemplate,
        ObjectMapper objectMapper,
        @Value("${chat.redis.ttl-hours:168}") long ttlHours,
        @Value("${chat.redis.max-history-size:40}") int maxHistorySize
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofHours(ttlHours);
        this.maxHistorySize = Math.max(4, maxHistorySize);
    }

    public String normalizeSessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return "anonymous:" + UUID.randomUUID();
        }
        return sessionId.trim();
    }

    public List<ChatMessageDTO> getRecentMessages(String sessionId) {
        String normalized = normalizeSessionId(sessionId);
        try {
            List<String> values = redisTemplate.opsForList().range(messageKey(normalized), -maxHistorySize, -1);
            if (values == null) {
                return List.of();
            }
            List<ChatMessageDTO> messages = new ArrayList<>();
            for (String value : values) {
                messages.add(objectMapper.readValue(value, ChatMessageDTO.class));
            }
            return messages;
        } catch (Exception exception) {
            log.debug("Redis chat messages read failed. sessionId={}", normalized, exception);
            return new ArrayList<>(fallbackMessages.getOrDefault(normalized, List.of()));
        }
    }

    public void appendMessage(String sessionId, ChatMessageDTO message) {
        if (message == null || message.content() == null || message.content().trim().isEmpty()) {
            return;
        }

        String normalized = normalizeSessionId(sessionId);
        try {
            String key = messageKey(normalized);
            redisTemplate.opsForList().rightPush(key, objectMapper.writeValueAsString(message));
            redisTemplate.opsForList().trim(key, -maxHistorySize, -1);
            redisTemplate.expire(key, ttl);
        } catch (Exception exception) {
            log.debug("Redis chat message append failed. sessionId={}", normalized, exception);
            fallbackMessages.compute(normalized, (key, current) -> {
                List<ChatMessageDTO> messages = current == null ? new ArrayList<>() : new ArrayList<>(current);
                messages.add(message);
                if (messages.size() > maxHistorySize) {
                    return new ArrayList<>(messages.subList(messages.size() - maxHistorySize, messages.size()));
                }
                return messages;
            });
        }
    }

    public ChatSessionState getState(String sessionId) {
        String normalized = normalizeSessionId(sessionId);
        try {
            String json = redisTemplate.opsForValue().get(stateKey(normalized));
            if (json == null || json.isBlank()) {
                return fallbackStates.getOrDefault(normalized, new ChatSessionState());
            }
            return objectMapper.readValue(json, ChatSessionState.class);
        } catch (Exception exception) {
            log.debug("Redis chat state read failed. sessionId={}", normalized, exception);
            return fallbackStates.getOrDefault(normalized, new ChatSessionState());
        }
    }

    public void saveState(String sessionId, ChatSessionState state) {
        if (state == null) {
            return;
        }
        String normalized = normalizeSessionId(sessionId);
        fallbackStates.put(normalized, state);
        try {
            redisTemplate.opsForValue().set(stateKey(normalized), objectMapper.writeValueAsString(state), ttl);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("챗봇 상태를 직렬화할 수 없습니다.", exception);
        } catch (Exception exception) {
            log.debug("Redis chat state save failed. sessionId={}", normalized, exception);
        }
    }

    public void markCollecting(String sessionId, ChatBotSlotDTO conditions) {
        ChatSessionState state = getState(sessionId);
        state.setState(ChatConversationState.COLLECTING_CONDITIONS);
        state.setLastConditions(conditions);
        saveState(sessionId, state);
    }

    public void saveRecommendations(
        String sessionId,
        ChatBotSlotDTO conditions,
        List<RecommendedNeighborhood> recommendations
    ) {
        ChatSessionState state = getState(sessionId);
        state.setState(ChatConversationState.RECOMMENDATION_READY);
        state.setLastConditions(conditions);
        state.setLastRecommendations(recommendations);
        saveState(sessionId, state);
    }

    public void markExplaining(String sessionId) {
        ChatSessionState state = getState(sessionId);
        state.setState(ChatConversationState.EXPLAINING_NEIGHBORHOOD);
        saveState(sessionId, state);
    }

    private String messageKey(String sessionId) {
        return MESSAGE_KEY_PREFIX + sessionId;
    }

    private String stateKey(String sessionId) {
        return STATE_KEY_PREFIX + sessionId;
    }
}
