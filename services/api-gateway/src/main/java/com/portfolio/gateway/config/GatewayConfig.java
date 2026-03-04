package com.portfolio.gateway.config;

import com.portfolio.common.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;

/**
 * Central Spring beans shared across gateway filters.
 */
@Configuration
public class GatewayConfig {

    /**
     * Shared JwtTokenProvider using the same secret as all downstream services.
     * The gateway only validates tokens here; services trust the forwarded headers.
     */
    @Bean
    public JwtTokenProvider jwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
        return new JwtTokenProvider(secret, expirationMs);
    }

    /**
     * ReactiveRedisTemplate wired for rate-limiting counters (String key → Long value).
     */
    @Bean
    public ReactiveRedisTemplate<String, Long> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory factory) {
        return new ReactiveRedisTemplate<>(factory,
                RedisSerializationContext.fromSerializer(
                        new org.springframework.data.redis.serializer.StringRedisSerializer())
                        .equals(null)
                        ? RedisSerializationContext.string()
                        : RedisSerializationContext.string());
    }
}
