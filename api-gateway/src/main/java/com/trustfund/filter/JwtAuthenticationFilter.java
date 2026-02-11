package com.trustfund.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret:TrustFundME2024SecretKeyForJWTTokenGenerationSecureRandomString64Chars}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        // Skip authentication for public endpoints
        if (isPublicEndpoint(path, method)) {
            return chain.filter(exchange);
        }

        String token = extractToken(request);
        if (token == null) {
            return onError(exchange, "Missing authorization token", HttpStatus.UNAUTHORIZED);
        }

        try {
            if (validateToken(token)) {
                Claims claims = getClaims(token);
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Id", claims.getSubject())
                        .header("X-User-Email", claims.get("email", String.class))
                        .build();
                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            } else {
                return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            log.error("Error validating token", e);
            return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean isPublicEndpoint(String path, HttpMethod method) {
        if (method == HttpMethod.OPTIONS) {
            return true;
        }

        // /api/auth - All methods are public
        if (path.startsWith("/api/auth")) {
            return true;
        }

        // /api/media - POST (upload), GET (read) and DELETE are public
        if (path.startsWith("/api/media")) {
            return method == HttpMethod.POST || method == HttpMethod.GET || method == HttpMethod.DELETE;
        }

        // Feed Service - GET methods are public
        if (path.startsWith("/api/feed-posts") || path.startsWith("/api/forum/categories")) {
            return method == HttpMethod.GET;
        }

        // Campaign Service - Only GET methods are public
        if (path.startsWith("/api/campaigns") ||
                path.startsWith("/api/fundraising-goals") ||
                path.startsWith("/api/campaign-follows")) {
            return method == HttpMethod.GET;
        }

        // Users endpoint - check-email is public
        if (path.equals("/api/users/check-email")) {
            return true;
        }

        // WebSocket endpoint - allow SockJS handshake (/ws/**)
        if (path.startsWith("/ws")) {
            return true;
        }

        return false;
    }

    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
