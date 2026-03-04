package com.portfolio.gateway.filter;

import com.portfolio.common.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Global reactive filter that:
 * <ol>
 *   <li>Passes auth-service routes ({@code /api/auth/**}) through without any JWT check.</li>
 *   <li>Requires a valid Bearer JWT on all other {@code /api/**} routes.</li>
 *   <li>Strips the {@code Authorization} header and injects trusted user-identity headers
 *       ({@code X-User-Id}, {@code X-User-Email}, {@code X-User-Name}) for downstream services.</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    /** Routes that do NOT require a JWT. */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/actuator"
    );

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public int getOrder() {
        // Run before the route-predicate filters so auth is enforced globally.
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Skip JWT validation for public paths
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        // Extract Bearer token
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing Authorization header");
        }

        String token = authHeader.substring(7);
        if (!jwtTokenProvider.isTokenValid(token)) {
            return unauthorized(exchange, "Invalid or expired JWT");
        }

        // Extract user identity from the validated token
        Claims claims = jwtTokenProvider.extractAllClaims(token);
        String email = claims.getSubject();
        String userId = String.valueOf(claims.get("userId", Long.class));
        String fullName = claims.get("fullName", String.class);

        // Mutate request: remove Authorization header, inject identity headers
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(HttpHeaders.AUTHORIZATION);
                    headers.set("X-User-Id", userId != null ? userId : "");
                    headers.set("X-User-Email", email != null ? email : "");
                    headers.set("X-User-Name", fullName != null ? fullName : "");
                })
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String reason) {
        log.debug("Unauthorized request to {}: {}", exchange.getRequest().getURI().getPath(), reason);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
