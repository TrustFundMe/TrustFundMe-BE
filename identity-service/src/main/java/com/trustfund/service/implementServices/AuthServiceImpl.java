package com.trustfund.service.implementServices;

import com.trustfund.exception.exceptions.BadRequestException;
import com.trustfund.exception.exceptions.UnauthorizedException;
import com.trustfund.model.User;
import com.trustfund.model.request.LoginRequest;
import com.trustfund.model.request.RegisterRequest;
import com.trustfund.model.request.SupabaseLoginRequest;
import com.trustfund.model.response.AuthResponse;
import com.trustfund.model.response.UserInfo;
import com.trustfund.repository.UserRepository;
import com.trustfund.service.interfaceServices.AuthService;
import com.trustfund.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @org.springframework.beans.factory.annotation.Value("${supabase.jwt.secret:}")
    private String supabaseJwtSecret;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .role(User.Role.USER)
                .isActive(true)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());

        String accessToken = jwtUtil.generateToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name()
        );
        String refreshToken = jwtUtil.generateRefreshToken(user.getId().toString());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.extractExpiration(accessToken).getTime() - System.currentTimeMillis())
                .user(UserInfo.fromUser(user))
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.getIsActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }

        String accessToken = jwtUtil.generateToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name()
        );
        String refreshToken = jwtUtil.generateRefreshToken(user.getId().toString());

        log.info("User logged in successfully: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.extractExpiration(accessToken).getTime() - System.currentTimeMillis())
                .user(UserInfo.fromUser(user))
                .build();
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        try {
            String userId = jwtUtil.extractUsername(refreshToken);
            User user = userRepository.findById(Long.parseLong(userId))
                    .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

            if (!user.getIsActive()) {
                throw new UnauthorizedException("Account is deactivated");
            }

            String newAccessToken = jwtUtil.generateToken(
                    user.getId().toString(),
                    user.getEmail(),
                    user.getRole().name()
            );
            String newRefreshToken = jwtUtil.generateRefreshToken(user.getId().toString());

            return AuthResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .expiresIn(jwtUtil.extractExpiration(newAccessToken).getTime() - System.currentTimeMillis())
                    .user(UserInfo.fromUser(user))
                    .build();
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid refresh token");
        }
    }

    @Override
    @Transactional
    public AuthResponse loginWithSupabase(SupabaseLoginRequest request) {
        if (supabaseJwtSecret == null || supabaseJwtSecret.isBlank()) {
            throw new BadRequestException("Supabase JWT secret is not configured");
        }

        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(supabaseJwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(request.getAccessToken())
                    .getPayload();
        } catch (Exception e) {
            log.error("Supabase token validation failed", e);
            throw new UnauthorizedException("Invalid Supabase token");
        }

        String email = claims.get("email", String.class);
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Supabase token missing email");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .fullName(email)
                    .role(User.Role.USER)
                    .isActive(true)
                    .build();
            user = userRepository.save(user);
            log.info("Created user from Supabase token: {}", email);
        } else if (!user.getIsActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }

        String accessToken = jwtUtil.generateToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name()
        );
        String refreshToken = jwtUtil.generateRefreshToken(user.getId().toString());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.extractExpiration(accessToken).getTime() - System.currentTimeMillis())
                .user(UserInfo.fromUser(user))
                .build();
    }
}


