package com.portfolio.common.security;

/**
 * Lightweight user identity propagated by the API Gateway as HTTP headers.
 * <p>
 * The gateway validates the JWT and writes these headers before forwarding
 * the request to downstream services.  Services trust these headers — they
 * do NOT re-validate the JWT.
 *
 * <pre>
 *   X-User-Id    → userId (Long)
 *   X-User-Email → email  (String)
 *   X-User-Name  → "firstName lastName" (String)
 * </pre>
 */
public record UserContext(
        Long userId,
        String email,
        String fullName
) {}
