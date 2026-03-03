package com.trustfund.filter;

import com.trustfund.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // First, try to get role from X-User-Role header (set by API Gateway)
        String roleFromHeader = request.getHeader("X-User-Role");
        String authHeader = request.getHeader("Authorization");

        String role = null;
        String username = null;

        if (roleFromHeader != null && !roleFromHeader.isEmpty()) {
            // Use role from API Gateway header
            role = roleFromHeader.trim();

            // Try to get username from X-User-Id header
            String userIdFromHeader = request.getHeader("X-User-Id");
            if (userIdFromHeader != null && !userIdFromHeader.isEmpty()) {
                username = userIdFromHeader.trim();
            }
        }

        // If role not from header, try to extract from JWT
        if (role == null || role.isEmpty()) {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            try {
                String token = authHeader.substring(7).trim();
                username = jwtUtil.extractUsername(token);
                role = jwtUtil.extractClaim(token, claims -> claims.get("role", String.class));

                if (username == null || username.isEmpty() || role == null || role.isEmpty()) {
                    log.warn("Invalid token: missing username or role");
                    filterChain.doFilter(request, response);
                    return;
                }

                if (jwtUtil.isTokenExpired(token)) {
                    log.warn("Token expired for user: {}", username);
                    filterChain.doFilter(request, response);
                    return;
                }
            } catch (Exception e) {
                log.error("Error processing JWT token: ", e);
                filterChain.doFilter(request, response);
                return;
            }
        }

        try {
            String authorityName = "ROLE_" + role;
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(authorityName);

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    Collections.singletonList(authority)
            );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.clearContext();
            SecurityContextHolder.getContext().setAuthentication(authToken);

        } catch (Exception e) {
            log.error("Error setting security context: ", e);
        }

        filterChain.doFilter(request, response);
    }
}
