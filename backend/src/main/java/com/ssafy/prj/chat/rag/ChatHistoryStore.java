package com.ssafy.prj.chat.rag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.ssafy.prj.chat.dto.ChatMessageDTO;

@Component
public class ChatHistoryStore {

    private static final Logger log = LoggerFactory.getLogger(ChatHistoryStore.class);
    private static final String KEY_PREFIX = "chat:history:";

    private final StringRedisTemplate redisTemplate;
    private final ChatRagProperties properties;

    public ChatHistoryStore(StringRedisTemplate redisTemplate, ChatRagProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public void append(String sessionId, String role, String content) {
        if (isBlank(content)) {
            return;
        }
        try {
            String key = key(sessionId);
            redisTemplate.opsForList().rightPush(key, role + "\t" + content.trim());
            redisTemplate.opsForList().trim(key, -properties.getHistoryLimit(), -1);
            redisTemplate.expire(key, properties.getHistoryTtl());
        } catch (RuntimeException exception) {
            log.warn("Chat history was not saved. sessionId={}", sessionId, exception);
        }
    }

    public List<ChatMessageDTO> recent(String sessionId) {
        List<String> values;
        try {
            values = redisTemplate.opsForList().range(key(sessionId), -properties.getHistoryLimit(), -1);
        } catch (RuntimeException exception) {
            log.warn("Chat history was not loaded. sessionId={}", sessionId, exception);
            return Collections.emptyList();
        }
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<ChatMessageDTO> messages = new ArrayList<>();
        for (String value : values) {
            int separator = value.indexOf('\t');
            if (separator <= 0) {
                continue;
            }
            messages.add(new ChatMessageDTO(value.substring(0, separator), value.substring(separator + 1)));
        }
        return messages;
    }

    private String key(String sessionId) {
        return KEY_PREFIX + sessionId;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
