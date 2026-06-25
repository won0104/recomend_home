package com.ssafy.prj.chat.dto;

import java.util.ArrayList;
import java.util.List;

import com.ssafy.prj.model.dto.neighborhood.RecommendedNeighborhood;

public class ChatSessionState {
    private ChatConversationState state = ChatConversationState.COLLECTING_CONDITIONS;
    private ChatBotSlotDTO lastConditions;
    private List<RecommendedNeighborhood> lastRecommendations = new ArrayList<>();

    public ChatConversationState getState() {
        return state;
    }

    public void setState(ChatConversationState state) {
        this.state = state == null ? ChatConversationState.COLLECTING_CONDITIONS : state;
    }

    public ChatBotSlotDTO getLastConditions() {
        return lastConditions;
    }

    public void setLastConditions(ChatBotSlotDTO lastConditions) {
        this.lastConditions = lastConditions;
    }

    public List<RecommendedNeighborhood> getLastRecommendations() {
        return lastRecommendations;
    }

    public void setLastRecommendations(List<RecommendedNeighborhood> lastRecommendations) {
        this.lastRecommendations = lastRecommendations == null ? new ArrayList<>() : lastRecommendations;
    }

    public boolean hasRecommendations() {
        return lastRecommendations != null && !lastRecommendations.isEmpty();
    }
}
