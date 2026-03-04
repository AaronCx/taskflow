package com.portfolio.common.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * Shared JWT utility used by every service that needs to issue or validate tokens.
 * <p>
 * Instantiate manually (no Spring dependency) so it can be used in both
 * servlet (auth/task/notification) and reactive (gateway) contexts.
 * <p>
 * Typical Spring wiring:
 * <pre>{@code
 * @Bean
 * JwtTokenProvider jwtTokenProvider(
 *         @Value("${app.jwt.secret}") String secret,
 *         @Value("${app.jwt.expiration-ms}") long expirationMs) {
 *     return new JwtTokenProvider(secret, expirationMs);
 * }
 * }</pre>
 */
@Slf4j
public class JwtTokenProvider {

    private final Key signingKey;
    private final long expirationMs;

    public JwtTokenProvider(String base64Secret, long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.expirationMs = expirationMs;
    }

    // ── Token generation ──────────────────────────────────────────────────────

    /**
     * Generate a signed JWT for the given subject (email) with optional extra claims.
     */
    public String generateToken(String subject, Map<String, Object> extraClaims) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expirationMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Convenience overload — no extra claims. */
    public String generateToken(String subject) {
        return generateToken(subject, Map.of());
    }

    // ── Token validation ──────────────────────────────────────────────────────

    /**
     * Returns {@code true} when the token is well-formed, correctly signed,
     * and not yet expired.
     */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    // ── Claim extraction ──────────────────────────────────────────────────────

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(parseClaims(token));
    }

    public Claims extractAllClaims(String token) {
        return parseClaims(token);
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
