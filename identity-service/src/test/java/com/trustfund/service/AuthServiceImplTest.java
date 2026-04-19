package com.trustfund.service;

import com.trustfund.exception.exceptions.BadRequestException;
import com.trustfund.exception.exceptions.NotFoundException;
import com.trustfund.exception.exceptions.UnauthorizedException;
import com.trustfund.model.OtpToken;
import com.trustfund.model.User;
import com.trustfund.model.request.LoginRequest;
import com.trustfund.model.request.RegisterRequest;
import com.trustfund.model.request.ResetPasswordRequest;
import com.trustfund.model.request.SendOtpRequest;
import com.trustfund.model.request.VerifyOtpRequest;
import com.trustfund.model.response.AuthResponse;
import com.trustfund.model.response.PasswordResetResponse;
import com.trustfund.repository.OtpTokenRepository;
import com.trustfund.repository.UserRepository;
import com.trustfund.service.implementServices.AuthServiceImpl;
import com.trustfund.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

import io.jsonwebtoken.Claims;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private com.trustfund.service.EmailService emailService;

    @Mock
    private OtpTokenRepository otpTokenRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "otpExpirationMinutes", 10);
    }

    // ============================================================
    // login()
    // ============================================================

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("login_success_passwordMatches_returnsAuthResponseWithTokens")
        void login_success_passwordMatches_returnsAuthResponseWithTokens() {
            LoginRequest request = new LoginRequest();
            request.setEmail("user@example.com");
            request.setPassword("correct_password");

            User user = User.builder()
                    .id(1L)
                    .email("user@example.com")
                    .password("encoded_password")
                    .fullName("Test User")
                    .role(User.Role.USER)
                    .isActive(true)
                    .verified(false)
                    .build();

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("correct_password", "encoded_password")).thenReturn(true);
            when(jwtUtil.generateToken(eq("1"), eq("user@example.com"), eq("USER"))).thenReturn("access_token");
            when(jwtUtil.generateRefreshToken(eq("1"))).thenReturn("refresh_token");
            when(jwtUtil.extractExpiration(anyString())).thenReturn(new Date(System.currentTimeMillis() + 86400000L));

            AuthResponse response = authService.login(request);

            assertThat(response.getAccessToken()).isEqualTo("access_token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh_token");
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getEmail()).isEqualTo("user@example.com");
            assertThat(response.getUser().getId()).isEqualTo(1L);

            verify(passwordEncoder).matches("correct_password", "encoded_password");
            verify(jwtUtil).generateToken("1", "user@example.com", "USER");
            verify(jwtUtil).generateRefreshToken("1");
        }

        @Test
        @DisplayName("login_failure_wrongPassword_throwsUnauthorizedException")
        void login_failure_wrongPassword_throwsUnauthorizedException() {
            LoginRequest request = new LoginRequest();
            request.setEmail("user@example.com");
            request.setPassword("wrong_password");

            User user = User.builder()
                    .id(1L)
                    .email("user@example.com")
                    .password("encoded_correct")
                    .fullName("Test User")
                    .role(User.Role.USER)
                    .isActive(true)
                    .build();

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong_password", "encoded_correct")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid email or password");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("login_failure_userNotFound_throwsUnauthorizedException")
        void login_failure_userNotFound_throwsUnauthorizedException() {
            LoginRequest request = new LoginRequest();
            request.setEmail("nobody@example.com");
            request.setPassword("any_password");

            when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid email or password");
        }
    }

    // ============================================================
    // register()
    // ============================================================

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("register_success_newUser_createsUserWithEncodedPasswordReturnsAuthResponse")
        void register_success_newUser_createsUserWithEncodedPasswordReturnsAuthResponse() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("newuser@example.com");
            request.setPassword("password123");
            request.setFullName("New User");
            request.setPhoneNumber("0909123456");

            when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded_password_hash");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(42L);
                return u;
            });
            when(jwtUtil.generateToken(eq("42"), eq("newuser@example.com"), eq("USER"))).thenReturn("access_token");
            when(jwtUtil.generateRefreshToken(eq("42"))).thenReturn("refresh_token");
            when(jwtUtil.extractExpiration(anyString())).thenReturn(new Date(System.currentTimeMillis() + 86400000L));

            AuthResponse response = authService.register(request);

            assertThat(response.getAccessToken()).isEqualTo("access_token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh_token");
            assertThat(response.getUser()).isNotNull();

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getPassword()).isEqualTo("encoded_password_hash");
            assertThat(savedUser.getEmail()).isEqualTo("newuser@example.com");
            assertThat(savedUser.getFullName()).isEqualTo("New User");
            assertThat(savedUser.getPhoneNumber()).isEqualTo("0909123456");
            assertThat(savedUser.getRole()).isEqualTo(User.Role.USER);
            assertThat(savedUser.getIsActive()).isTrue();
            assertThat(savedUser.getVerified()).isFalse();

            verify(passwordEncoder).encode("password123");
            verify(jwtUtil).generateToken("42", "newuser@example.com", "USER");
            verify(jwtUtil).generateRefreshToken("42");
        }

        @Test
        @DisplayName("register_failure_duplicateEmail_throwsBadRequestException")
        void register_failure_duplicateEmail_throwsBadRequestException() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("existing@example.com");
            request.setPassword("password123");
            request.setFullName("Existing User");

            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Email already exists");

            verify(userRepository, never()).save(any());
        }
    }

    // ============================================================
    // sendOtp()
    // ============================================================

    @Nested
    @DisplayName("sendOtp()")
    class SendOtp {

        @Test
        @DisplayName("sendOtp_success_validEmail_createsOtpTokenAndCallsEmailService")
        void sendOtp_success_validEmail_createsOtpTokenAndCallsEmailService() {
            SendOtpRequest request = new SendOtpRequest();
            request.setEmail("user@example.com");

            User user = User.builder()
                    .id(1L)
                    .email("user@example.com")
                    .fullName("Test User")
                    .isActive(true)
                    .build();

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(otpTokenRepository.findByEmailAndOtpAndUsedFalse(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(otpTokenRepository.save(any(OtpToken.class))).thenAnswer(inv -> {
                OtpToken otp = inv.getArgument(0);
                otp.setId(1L);
                return otp;
            });

            PasswordResetResponse response = authService.sendOtp(request);

            assertThat(response.getSuccess()).isTrue();
            assertThat(response.getMessage()).contains("OTP");

            ArgumentCaptor<OtpToken> otpCaptor = ArgumentCaptor.forClass(OtpToken.class);
            verify(otpTokenRepository).save(otpCaptor.capture());
            OtpToken savedOtp = otpCaptor.getValue();
            assertThat(savedOtp.getEmail()).isEqualTo("user@example.com");
            assertThat(savedOtp.getOtp()).hasSize(6);
            assertThat(savedOtp.getUsed()).isFalse();
            assertThat(savedOtp.getExpiresAt()).isAfter(LocalDateTime.now());

            verify(emailService).sendOtpEmail(
                    eq("user@example.com"),
                    eq(savedOtp.getOtp()),
                    eq("Test User"),
                    eq("reset_password")
            );
        }

        @Test
        @DisplayName("sendOtp_failure_emailNotFound_throwsNotFoundException")
        void sendOtp_failure_emailNotFound_throwsNotFoundException() {
            SendOtpRequest request = new SendOtpRequest();
            request.setEmail("unknown@example.com");

            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.sendOtp(request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Email not found");

            verify(otpTokenRepository, never()).save(any());
            verify(emailService, never()).sendOtpEmail(anyString(), anyString(), anyString(), anyString());
        }
    }

    // ============================================================
    // verifyOtp()
    // ============================================================

    @Nested
    @DisplayName("verifyOtp()")
    class VerifyOtp {

        @Test
        @DisplayName("verifyOtp_success_validOtp_returnsPasswordResetResponseWithToken")
        void verifyOtp_success_validOtp_returnsPasswordResetResponseWithToken() {
            VerifyOtpRequest request = new VerifyOtpRequest();
            request.setEmail("user@example.com");
            request.setOtp("123456");

            OtpToken otpToken = OtpToken.builder()
                    .id(1L)
                    .email("user@example.com")
                    .otp("123456")
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .used(false)
                    .build();

            when(otpTokenRepository.findByEmailAndOtpAndUsedFalse("user@example.com", "123456"))
                    .thenReturn(Optional.of(otpToken));
            when(jwtUtil.generatePasswordResetToken("user@example.com")).thenReturn("password_reset_token");

            PasswordResetResponse response = authService.verifyOtp(request);

            assertThat(response.getSuccess()).isTrue();
            assertThat(response.getToken()).isEqualTo("password_reset_token");
            assertThat(response.getMessage()).contains("verified");

            verify(otpTokenRepository).markAsUsed("user@example.com", "123456");
            verify(otpTokenRepository, never()).delete(any(OtpToken.class));
        }

        @Test
        @DisplayName("verifyOtp_failure_invalidOtp_throwsUnauthorizedException")
        void verifyOtp_failure_invalidOtp_throwsUnauthorizedException() {
            VerifyOtpRequest request = new VerifyOtpRequest();
            request.setEmail("user@example.com");
            request.setOtp("000000");

            when(otpTokenRepository.findByEmailAndOtpAndUsedFalse("user@example.com", "000000"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyOtp(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid or expired OTP");

            verify(otpTokenRepository, never()).markAsUsed(anyString(), anyString());
        }

        @Test
        @DisplayName("verifyOtp_failure_expiredOtp_throwsUnauthorizedException")
        void verifyOtp_failure_expiredOtp_throwsUnauthorizedException() {
            VerifyOtpRequest request = new VerifyOtpRequest();
            request.setEmail("user@example.com");
            request.setOtp("123456");

            OtpToken expiredOtp = OtpToken.builder()
                    .id(1L)
                    .email("user@example.com")
                    .otp("123456")
                    .expiresAt(LocalDateTime.now().minusMinutes(1))
                    .used(false)
                    .build();

            when(otpTokenRepository.findByEmailAndOtpAndUsedFalse("user@example.com", "123456"))
                    .thenReturn(Optional.of(expiredOtp));

            assertThatThrownBy(() -> authService.verifyOtp(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("expired");

            verify(otpTokenRepository).delete(expiredOtp);
            verify(otpTokenRepository, never()).markAsUsed(anyString(), anyString());
        }
    }

    // ============================================================
    // resetPassword()
    // ============================================================

    @Nested
    @DisplayName("resetPassword()")
    class ResetPassword {

        @Test
        @DisplayName("resetPassword_success_validToken_updatesPasswordReturnsSuccess")
        void resetPassword_success_validToken_updatesPasswordReturnsSuccess() {
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken("valid_reset_token");
            request.setNewPassword("NewSecurePass123!");

            when(jwtUtil.isTokenExpired("valid_reset_token")).thenReturn(false);
            when(jwtUtil.extractUsername("valid_reset_token")).thenReturn("user@example.com");
            when(jwtUtil.extractClaim(eq("valid_reset_token"), any())).thenAnswer(inv -> {
                Function<Claims, String> fn = inv.getArgument(1);
                Claims claims = mock(Claims.class);
                when(claims.get("type")).thenReturn("password_reset");
                return fn.apply(claims);
            });

            User user = User.builder()
                    .id(1L)
                    .email("user@example.com")
                    .password("old_password_hash")
                    .build();

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("NewSecurePass123!")).thenReturn("new_encoded_hash");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            PasswordResetResponse response = authService.resetPassword(request);

            assertThat(response.getSuccess()).isTrue();
            assertThat(response.getMessage()).contains("success");
            assertThat(user.getPassword()).isEqualTo("new_encoded_hash");

            verify(passwordEncoder).encode("NewSecurePass123!");
            verify(userRepository).save(user);
        }
    }

    // ============================================================
    // refreshToken()
    // ============================================================

    @Nested
    @DisplayName("refreshToken()")
    class RefreshToken {

        @Test
        @DisplayName("refreshToken_success_validRefreshToken_returnsNewTokenPair")
        void refreshToken_success_validRefreshToken_returnsNewTokenPair() {
            String refreshToken = "valid_refresh_token";

            User user = User.builder()
                    .id(5L)
                    .email("user@example.com")
                    .fullName("Test User")
                    .role(User.Role.USER)
                    .isActive(true)
                    .build();

            when(jwtUtil.extractUsername(refreshToken)).thenReturn("5");
            when(userRepository.findById(5L)).thenReturn(Optional.of(user));
            when(jwtUtil.generateToken(eq("5"), eq("user@example.com"), eq("USER"))).thenReturn("new_access_token");
            when(jwtUtil.generateRefreshToken(eq("5"))).thenReturn("new_refresh_token");
            when(jwtUtil.extractExpiration(anyString())).thenReturn(new Date(System.currentTimeMillis() + 86400000L));

            AuthResponse response = authService.refreshToken(refreshToken);

            assertThat(response.getAccessToken()).isEqualTo("new_access_token");
            assertThat(response.getRefreshToken()).isEqualTo("new_refresh_token");
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getEmail()).isEqualTo("user@example.com");

            verify(jwtUtil).generateToken("5", "user@example.com", "USER");
            verify(jwtUtil).generateRefreshToken("5");
        }

        @Test
        @DisplayName("refreshToken_failure_invalidToken_throwsUnauthorizedException")
        void refreshToken_failure_invalidToken_throwsUnauthorizedException() {
            String invalidToken = "invalid_token";

            when(jwtUtil.extractUsername(invalidToken)).thenThrow(new RuntimeException("Invalid token"));

            assertThatThrownBy(() -> authService.refreshToken(invalidToken))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid refresh token");
        }

        @Test
        @DisplayName("refreshToken_failure_userNotFound_throwsUnauthorizedException")
        void refreshToken_failure_userNotFound_throwsUnauthorizedException() {
            String refreshToken = "valid_but_user_gone_token";

            when(jwtUtil.extractUsername(refreshToken)).thenReturn("999");
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid refresh token");
        }
    }
}
