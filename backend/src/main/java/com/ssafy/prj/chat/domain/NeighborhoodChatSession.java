package com.ssafy.prj.chat.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class NeighborhoodChatSession {
    private String sessionId;
    private NeighborhoodChatStep step = NeighborhoodChatStep.ASK_PLACE;
    private String basePlaceName;
    private Double latitude;
    private Double longitude;
    private Double radiusKm;
    private Integer maxPrice;
    private List<String> priorities = new ArrayList<>();
    private List<ChatPlaceCandidate> placeCandidates = new ArrayList<>();
    private Instant updatedAt = Instant.now();

    public static NeighborhoodChatSession create(String sessionId) {
        NeighborhoodChatSession session = new NeighborhoodChatSession();
        session.setSessionId(sessionId);
        return session;
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

    public String getBasePlaceName() {
        return basePlaceName;
    }

    public void setBasePlaceName(String basePlaceName) {
        this.basePlaceName = basePlaceName;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getRadiusKm() {
        return radiusKm;
    }

    public void setRadiusKm(Double radiusKm) {
        this.radiusKm = radiusKm;
    }

    public Integer getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Integer maxPrice) {
        this.maxPrice = maxPrice;
    }

    public List<String> getPriorities() {
        return priorities;
    }

    public void setPriorities(List<String> priorities) {
        this.priorities = priorities == null ? new ArrayList<>() : new ArrayList<>(priorities);
    }

    public List<ChatPlaceCandidate> getPlaceCandidates() {
        return placeCandidates;
    }

    public void setPlaceCandidates(List<ChatPlaceCandidate> placeCandidates) {
        this.placeCandidates = placeCandidates == null ? new ArrayList<>() : new ArrayList<>(placeCandidates);
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
