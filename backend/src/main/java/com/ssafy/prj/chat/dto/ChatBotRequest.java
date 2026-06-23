package com.ssafy.prj.chat.dto;

import java.util.List;

public record ChatBotRequest(String message, List<ChatMessageDTO> history) {
}
