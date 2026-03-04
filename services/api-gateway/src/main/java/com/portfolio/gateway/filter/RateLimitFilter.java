package com.portfolio.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Sliding-window rate limiter backed by Redis.
 * <p>
 * Key strategy:
 * <ul>
 *   <li>Authenticated routes → keyed on {@code X-User-Id} (injected by {@link JwtAuthenticationFilter}).</li>
 *   <li>Public auth routes   → keyed on client IP address.</li>
 * </ul>
 * Default limit: 60 requests per minute per key.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final ReactiveRedisTemplate<String, Long> redisTemplate;

    @Value("${gateway.rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    @Override
    public int getOrder() {
        // Run after JwtAuthenticationFilter (order -100) so X-User-Id is already set.
        return -50;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String key = resolveKey(exchange);
        String redisKey = "rate_limit:" + key;

        return redisTemplate.opsForValue()
                .increment(redisKey)
                .flatMap(count -> {
                    if (count == 1) {
                        // First request in the window — set TTL of 60 seconds
                        return redisTemplate.expire(redisKey, Duration.ofMinutes(1))
                                .then(chain.filter(exchange));
                    }
                    if (count > requestsPerMinute) {
                        log.warn("Rate limit exceeded for key={}, count={}", key, count);
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        exchange.getResponse().getHeaders().add("Retry-After", "60");
                        return exchange.getResponse().setComplete();
                    }
                    return chain.filter(exchange);
                })
                // If Redis is unavailable, fail open (let the request through) to avoid cascading failures
                .onErrorResume(ex -> {
                    log.warn("Rate-limit Redis unavailable, failing open: {}", ex.getMessage());
                    return chain.filter(exchange);
                });
    }

    private String resolveKey(ServerWebExchange exchange) {
        // Prefer user ID (set for authenticated requests), fall back to IP
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (userId != null && !userId.isBlank()) {
            return "user:" + userId;
        }
        // For auth routes (no user ID yet) use the client IP
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        return "ip:" + (remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown");
    }
}
