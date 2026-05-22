package com.codesync.session.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis configuration for the Session-Service.
 *
 * We use Redis as a key-value store for the latest in-memory file content.
 * Key pattern: "file:{fileId}:content"
 *
 * This allows:
 *   - Instant access to latest content for late-joining collaborators
 *   - Persistence of in-flight changes even if the DB hasn't been saved yet
 */
@Configuration
public class RedisConfig {

    /**
     * StringRedisTemplate is a convenience wrapper for String key/value ops.
     * Spring Boot auto-configures the RedisConnectionFactory from application.properties.
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public RedisTemplate<String, byte[]> binaryRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, byte[]> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(RedisSerializer.byteArray());
        return template;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}
