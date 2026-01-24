package com.trustfund.service.implementServices;

import com.trustfund.exception.exceptions.BadRequestException;
import com.trustfund.exception.exceptions.NotFoundException;
import com.trustfund.exception.exceptions.UnauthorizedException;
import com.trustfund.model.OtpToken;
import com.trustfund.model.User;
import com.trustfund.model.request.LoginRequest;
import com.trustfund.model.request.RegisterRequest;
import com.trustfund.model.request.ResetPasswordRequest;
import com.trustfund.model.request.SendOtpRequest;
import com.trustfund.model.request.VerifyEmailRequest;
import com.trustfund.model.request.VerifyOtpRequest;
import com.trustfund.model.response.AuthResponse;
import com.trustfund.model.response.PasswordResetResponse;
import com.trustfund.model.response.UserInfo;
import com.trustfund.repository.OtpTokenRepository;
import com.trustfund.repository.UserRepository;
import com.trustfund.service.EmailService;
import com.trustfund.service.interfaceServices.AuthService;
import com.trustfund.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.trustfund.model.request.GoogleLoginRequest;
import java.util.Collections;
import java.util.UUID;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final OtpTokenRepository otpTokenRepository;

    @Value("${app.otp.expiration:10}")
    private int otpExpirationMinutes;

    @Value("${app.google.client-id}")
    private String googleClientId;

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
                .verified(false)
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
    public PasswordResetResponse sendOtp(SendOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("Email not found"));

        if (!user.getIsActive()) {
            throw new BadRequestException("Account is deactivated");
        }

        // Generate 6-digit OTP
        String otp = generateOtp();

        // Delete any existing unused OTP for this email
        otpTokenRepository.findByEmailAndOtpAndUsedFalse(request.getEmail(), otp)
                .ifPresent(otpTokenRepository::delete);

        // Create new OTP token
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(otpExpirationMinutes);
        OtpToken otpToken = OtpToken.builder()
                .email(request.getEmail())
                .otp(otp)
                .expiresAt(expiresAt)
                .used(false)
                .build();

        otpTokenRepository.save(otpToken);

        // Send OTP via email
        try {
            emailService.sendOtpEmail(user.getEmail(), otp, user.getFullName(), "reset_password");
            log.info("OTP email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send OTP email: {}", e.getMessage());
            // Don't fail the request, OTP is already saved
        }

        return PasswordResetResponse.builder()
                .success(true)
                .message("OTP has been sent to your email")
                .build();
    }

    @Override
    @Transactional
    public PasswordResetResponse verifyOtp(VerifyOtpRequest request) {
        OtpToken otpToken = otpTokenRepository
                .findByEmailAndOtpAndUsedFalse(request.getEmail(), request.getOtp())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired OTP"));

        // Check if OTP is expired
        if (otpToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpTokenRepository.delete(otpToken);
            throw new UnauthorizedException("OTP has expired");
        }

        // Mark OTP as used (OTP chỉ dùng 1 lần để verify)
        otpTokenRepository.markAsUsed(request.getEmail(), request.getOtp());

        // Generate password reset token (JWT) - dùng chung cho verify email và reset password
        String resetToken = jwtUtil.generatePasswordResetToken(request.getEmail());

        log.info("OTP verified successfully for email: {}. Reset token generated.", request.getEmail());

        return PasswordResetResponse.builder()
                .success(true)
                .message("OTP verified successfully. You can now reset your password.")
                .token(resetToken)
                .build();
    }

    @Override
    @Transactional
    public PasswordResetResponse resetPassword(ResetPasswordRequest request) {
        try {
            // Verify JWT token
            if (jwtUtil.isTokenExpired(request.getToken())) {
                throw new UnauthorizedException("Reset token has expired");
            }

            // Extract email from token
            String email = jwtUtil.extractUsername(request.getToken());
            
            // Verify token type (should be password_reset)
            String tokenType = jwtUtil.extractClaim(request.getToken(), claims -> {
                Object type = claims.get("type");
                return type != null ? type.toString() : null;
            });
            
            if (tokenType == null || !"password_reset".equals(tokenType)) {
                throw new UnauthorizedException("Invalid reset token");
            }

            // Get user and update password
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new NotFoundException("User not found"));

            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            log.info("Password reset successfully for user: {}", user.getEmail());

            return PasswordResetResponse.builder()
                    .success(true)
                    .message("Password reset successfully")
                    .build();
        } catch (Exception e) {
            log.error("Password reset failed: {}", e.getMessage());
            throw new UnauthorizedException("Invalid or expired reset token");
        }
    }

    @Override
    @Transactional
    public PasswordResetResponse verifyEmail(VerifyEmailRequest request) {
        try {
            // Verify JWT token
            if (jwtUtil.isTokenExpired(request.getToken())) {
                throw new UnauthorizedException("Verification token has expired");
            }

            // Extract email from token
            String email = jwtUtil.extractUsername(request.getToken());
            
            // Verify token type (should be password_reset - dùng chung cho verify email và reset password)
            String tokenType = jwtUtil.extractClaim(request.getToken(), claims -> {
                Object type = claims.get("type");
                return type != null ? type.toString() : null;
            });
            
            if (tokenType == null || !"password_reset".equals(tokenType)) {
                throw new UnauthorizedException("Invalid verification token");
            }

            // Get user and update verified status
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new NotFoundException("User not found"));

            user.setVerified(true);
            userRepository.save(user);

            log.info("Email verified successfully for user: {}", user.getEmail());

            return PasswordResetResponse.builder()
                    .success(true)
                    .message("Email verified successfully")
                    .build();
        } catch (Exception e) {
            log.error("Email verification failed: {}", e.getMessage());
            throw new UnauthorizedException("Invalid or expired verification token");
        }
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // Generate 6-digit OTP (100000-999999)
        return String.valueOf(otp);
    }
    @Override
    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            // Verify the ID token
            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            if (idToken == null) {
                throw new UnauthorizedException("Invalid Google ID Token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            // Check if user exists
            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                // Register new user
                user = User.builder()
                        .email(email)
                        .password(passwordEncoder.encode(UUID.randomUUID().toString())) // Random password
                        .fullName(name != null ? name : "Google User")
                        .role(User.Role.USER)
                        .isActive(true)
                        .verified(true) // Verified by Google
                        .build();
                user = userRepository.save(user);
                log.info("New user registered via Google: {}", email);
            } else {
                if (!user.getIsActive()) {
                    throw new UnauthorizedException("Account is deactivated");
                }
            }

            // Generate tokens
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

        } catch (Exception e) {
            log.error("Google login failed: {}", e.getMessage());
            throw new UnauthorizedException("Google login failed: " + e.getMessage());
        }
    }

}


