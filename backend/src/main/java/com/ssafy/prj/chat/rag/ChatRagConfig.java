package com.ssafy.prj.chat.rag;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import redis.clients.jedis.JedisPooled;

@Configuration
@EnableConfigurationProperties(ChatRagProperties.class)
public class ChatRagConfig {

    @Bean
    public JedisPooled ragJedisPooled(
        @Value("${spring.data.redis.host:localhost}") String host,
        @Value("${spring.data.redis.port:6379}") int port
    ) {
        return new JedisPooled(host, port);
    }

    @Bean
    @Lazy
    public VectorStore neighborhoodProfileVectorStore(
        JedisPooled jedisPooled,
        EmbeddingModel embeddingModel,
        ChatRagProperties properties
    ) {
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
            .indexName(properties.getRedisIndexName())
            .prefix(properties.getRedisKeyPrefix())
            .metadataFields(
                RedisVectorStore.MetadataField.tag("sido"),
                RedisVectorStore.MetadataField.tag("sigungu"),
                RedisVectorStore.MetadataField.tag("dong"),
                RedisVectorStore.MetadataField.tag("category"),
                RedisVectorStore.MetadataField.tag("lawdCd")
            )
            .initializeSchema(properties.isInitializeSchema())
            .build();
    }
}
