package com.ssafy.prj.chat.rag;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chat.rag")
public class ChatRagProperties {

    private Duration historyTtl = Duration.ofDays(7);
    private int historyLimit = 20;
    private int searchTopK = 5;
    private String redisIndexName = "neighborhood-profile-index";
    private String redisKeyPrefix = "neighborhood-profile:";
    private boolean initializeSchema = true;
    private String sessionFallback = "anonymous";
    private String generatedProfilePath = "generated/rag/seoul-gugun";

    public Duration getHistoryTtl() {
        return historyTtl;
    }

    public void setHistoryTtl(Duration historyTtl) {
        this.historyTtl = historyTtl;
    }

    public int getHistoryLimit() {
        return historyLimit;
    }

    public void setHistoryLimit(int historyLimit) {
        this.historyLimit = historyLimit;
    }

    public int getSearchTopK() {
        return searchTopK;
    }

    public void setSearchTopK(int searchTopK) {
        this.searchTopK = searchTopK;
    }

    public String getRedisIndexName() {
        return redisIndexName;
    }

    public void setRedisIndexName(String redisIndexName) {
        this.redisIndexName = redisIndexName;
    }

    public String getRedisKeyPrefix() {
        return redisKeyPrefix;
    }

    public void setRedisKeyPrefix(String redisKeyPrefix) {
        this.redisKeyPrefix = redisKeyPrefix;
    }

    public boolean isInitializeSchema() {
        return initializeSchema;
    }

    public void setInitializeSchema(boolean initializeSchema) {
        this.initializeSchema = initializeSchema;
    }

    public String getSessionFallback() {
        return sessionFallback;
    }

    public void setSessionFallback(String sessionFallback) {
        this.sessionFallback = sessionFallback;
    }

    public String getGeneratedProfilePath() {
        return generatedProfilePath;
    }

    public void setGeneratedProfilePath(String generatedProfilePath) {
        this.generatedProfilePath = generatedProfilePath;
    }
}
