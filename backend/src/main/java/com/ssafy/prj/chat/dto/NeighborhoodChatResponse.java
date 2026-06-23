package com.ssafy.prj.chat.dto;

import java.util.ArrayList;
import java.util.List;

import com.ssafy.prj.chat.domain.ChatPlaceCandidate;
import com.ssafy.prj.chat.domain.NeighborhoodChatSession;
import com.ssafy.prj.chat.domain.NeighborhoodChatStep;
import com.ssafy.prj.model.dto.neighborhood.NeighborhoodRecommendResponse;

public class NeighborhoodChatResponse {
    private String sessionId;
    private NeighborhoodChatStep step;
    private String message;
    private List<ChatPlaceCandidate> placeCandidates = new ArrayList<>();
    private NeighborhoodChatSession session;
    private NeighborhoodRecommendResponse recommendation;

    public static NeighborhoodChatResponse of(NeighborhoodChatSession session, String message) {
        NeighborhoodChatResponse response = new NeighborhoodChatResponse();
        response.setSessionId(session.getSessionId());
        response.setStep(session.getStep());
        response.setMessage(message);
        response.setPlaceCandidates(session.getPlaceCandidates());
        response.setSession(session);
        return response;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public NeighborhoodChatStep getStep() {
        return step;
    }

    public void setStep(NeighborhoodChatStep step) {
        this.step = step;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ChatPlaceCandidate> getPlaceCandidates() {
        return placeCandidates;
    }

    public void setPlaceCandidates(List<ChatPlaceCandidate> placeCandidates) {
        this.placeCandidates = placeCandidates == null ? new ArrayList<>() : placeCandidates;
    }

    public NeighborhoodChatSession getSession() {
        return session;
    }

    public void setSession(NeighborhoodChatSession session) {
        this.session = session;
    }

    public NeighborhoodRecommendResponse getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(NeighborhoodRecommendResponse recommendation) {
        this.recommendation = recommendation;
    }
}
