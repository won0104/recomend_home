package com.ssafy.prj.chat.service;

import org.springframework.stereotype.Component;

import com.ssafy.prj.chat.dto.ChatBotSlotDTO;

@Component
public class ChatSessionContext {
    private final ThreadLocal<CurrentChatSession> current = new ThreadLocal<>();

    public void set(String sessionId, ChatBotSlotDTO conditions) {
        current.set(new CurrentChatSession(sessionId, conditions));
    }

    public CurrentChatSession get() {
        return current.get();
    }

    public void clear() {
        current.remove();
    }

    public record CurrentChatSession(String sessionId, ChatBotSlotDTO conditions) {
    }
}
