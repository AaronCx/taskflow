package com.portfolio.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Key resolvers for Spring Cloud Gateway's built-in RequestRateLimiter filter.
 * These are referenced by SpEL expressions in application.yml.
 */
@Configuration
public class KeyResolverConfig {

    /**
     * Rate-limit key for authenticated routes: the user's ID header injected by the JWT filter.
     * Falls back to IP if the header is absent (shouldn't happen on protected routes).
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }
            var addr = exchange.getRequest().getRemoteAddress();
            return Mono.just("ip:" + (addr != null ? addr.getAddress().getHostAddress() : "unknown"));
        };
    }

    /**
     * Rate-limit key for public auth routes: client IP address.
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            var addr = exchange.getRequest().getRemoteAddress();
            return Mono.just("ip:" + (addr != null ? addr.getAddress().getHostAddress() : "unknown"));
        };
    }
}
