package com.trustfund.service;

import com.trustfund.exception.exceptions.BadRequestException;
import com.trustfund.exception.exceptions.NotFoundException;
import com.trustfund.model.User;
import com.trustfund.model.request.CreateUserRequest;
import com.trustfund.model.request.UpdateUserRequest;
import com.trustfund.model.response.CheckEmailResponse;
import com.trustfund.model.response.UserInfo;
import com.trustfund.repository.UserRepository;
import com.trustfund.service.implementServices.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(userService, "mediaServiceUrl", "http://localhost:8083");
        org.springframework.test.util.ReflectionTestUtils.setField(userService, "campaignServiceUrl", "http://localhost:8082");
    }

    // ============================================================
    // getUserById()
    // ============================================================

    @Nested
    @DisplayName("getUserById()")
    class GetUserById {

        @Test
        @DisplayName("getUserById_found_returnsUserInfo")
        void getUserById_found_returnsUserInfo() {
            User user = User.builder()
                    .id(1L)
                    .email("user@example.com")
                    .fullName("Test User")
                    .phoneNumber("0909123456")
                    .role(User.Role.USER)
                    .isActive(true)
                    .verified(false)
                    .kycVerified(false)
                    .trustScore(0)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            UserInfo result = userService.getUserById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("user@example.com");
            assertThat(result.getFullName()).isEqualTo("Test User");
            assertThat(result.getPhoneNumber()).isEqualTo("0909123456");
            assertThat(result.getRole()).isEqualTo(User.Role.USER);
            assertThat(result.getIsActive()).isTrue();
            assertThat(result.getVerified()).isFalse();
        }

        @Test
        @DisplayName("getUserById_notFound_throwsNotFoundException")
        void getUserById_notFound_throwsNotFoundException() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(99L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("User not found with id: 99");
        }
    }

    // ============================================================
    // banUser()
    // ============================================================

    @Nested
    @DisplayName("banUser()")
    class BanUser {

        @Test
        @DisplayName("banUser_found_setsIsActiveFalseAndBanReason")
        void banUser_found_setsIsActiveFalseAndBanReason() {
            User user = User.builder()
                    .id(1L)
                    .email("user@example.com")
                    .fullName("Test User")
                    .isActive(true)
                    .banReason(null)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserInfo result = userService.banUser(1L, "Spam behavior");

            assertThat(result.getIsActive()).isFalse();
            assertThat(user.getIsActive()).isFalse();
            assertThat(user.getBanReason()).isEqualTo("Spam behavior");
            verify(userRepository).save(user);
        }
    }

    // ============================================================
    // unbanUser()
    // ============================================================

    @Nested
    @DisplayName("unbanUser()")
    class UnbanUser {

        @Test
        @DisplayName("unbanUser_found_setsIsActiveTrue")
        void unbanUser_found_setsIsActiveTrue() {
            User user = User.builder()
                    .id(1L)
                    .email("user@example.com")
                    .fullName("Test User")
                    .isActive(false)
                    .banReason("Previous violation")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserInfo result = userService.unbanUser(1L);

            assertThat(result.getIsActive()).isTrue();
            assertThat(user.getIsActive()).isTrue();
            verify(userRepository).save(user);
        }
    }

    // ============================================================
    // upgradeToFundOwner()
    // ============================================================

    @Nested
    @DisplayName("upgradeToFundOwner()")
    class UpgradeToFundOwner {

        @Test
        @DisplayName("upgradeToFundOwner_USERRole_changesToFUND_OWNER")
        void upgradeToFundOwner_USERRole_changesToFUND_OWNER() {
            User user = User.builder()
                    .id(1L)
                    .email("user@example.com")
                    .fullName("Test User")
                    .role(User.Role.USER)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            userService.upgradeToFundOwner(1L);

            assertThat(user.getRole()).isEqualTo(User.Role.FUND_OWNER);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("upgradeToFundOwner_alreadyFUND_OWNER_noChange")
        void upgradeToFundOwner_alreadyFUND_OWNER_noChange() {
            User user = User.builder()
                    .id(1L)
                    .email("owner@example.com")
                    .fullName("Owner User")
                    .role(User.Role.FUND_OWNER)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            userService.upgradeToFundOwner(1L);

            assertThat(user.getRole()).isEqualTo(User.Role.FUND_OWNER);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("upgradeToFundOwner_notFound_throwsNotFoundException")
        void upgradeToFundOwner_notFound_throwsNotFoundException() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.upgradeToFundOwner(99L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("User not found");
        }
    }

    // ============================================================
    // checkEmail()
    // ============================================================

    @Nested
    @DisplayName("checkEmail()")
    class CheckEmail {

        @Test
        @DisplayName("checkEmail_emailExists_returnsExistsTrueAndFullName")
        void checkEmail_emailExists_returnsExistsTrueAndFullName() {
            User user = User.builder()
                    .id(1L)
                    .email("existing@example.com")
                    .fullName("Existing User")
                    .build();

            when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(user));

            CheckEmailResponse result = userService.checkEmail("existing@example.com");

            assertThat(result.getExists()).isTrue();
            assertThat(result.getEmail()).isEqualTo("existing@example.com");
            assertThat(result.getFullName()).isEqualTo("Existing User");
        }

        @Test
        @DisplayName("checkEmail_emailNotFound_returnsExistsFalse")
        void checkEmail_emailNotFound_returnsExistsFalse() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            CheckEmailResponse result = userService.checkEmail("unknown@example.com");

            assertThat(result.getExists()).isFalse();
            assertThat(result.getEmail()).isEqualTo("unknown@example.com");
            assertThat(result.getFullName()).isNull();
        }
    }

    // ============================================================
    // getAllStaff()
    // ============================================================

    @Nested
    @DisplayName("getAllStaff()")
    class GetAllStaff {

        @Test
        @DisplayName("getAllStaff_returnsOnlySTAFFRoleUsers")
        void getAllStaff_returnsOnlySTAFFRoleUsers() {
            User staff1 = User.builder()
                    .id(1L)
                    .email("staff1@example.com")
                    .fullName("Staff One")
                    .role(User.Role.STAFF)
                    .isActive(true)
                    .verified(false)
                    .build();

            User staff2 = User.builder()
                    .id(2L)
                    .email("staff2@example.com")
                    .fullName("Staff Two")
                    .role(User.Role.STAFF)
                    .isActive(true)
                    .verified(false)
                    .build();

            when(userRepository.findAllByRole(User.Role.STAFF)).thenReturn(List.of(staff1, staff2));

            List<UserInfo> result = userService.getAllStaff();

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(info -> info.getRole() == User.Role.STAFF);
            assertThat(result.get(0).getEmail()).isEqualTo("staff1@example.com");
            assertThat(result.get(1).getEmail()).isEqualTo("staff2@example.com");
        }

        @Test
        @DisplayName("getAllStaff_empty_returnsEmptyList")
        void getAllStaff_empty_returnsEmptyList() {
            when(userRepository.findAllByRole(User.Role.STAFF)).thenReturn(List.of());

            List<UserInfo> result = userService.getAllStaff();

            assertThat(result).isEmpty();
        }
    }

    // ============================================================
    // getAllUsers(Pageable)
    // ============================================================

    @Nested
    @DisplayName("getAllUsers(Pageable)")
    class GetAllUsersPaginated {

        @Test
        @DisplayName("getAllUsers_pagedFindAll_returnsPageOfUserInfo")
        void getAllUsers_pagedFindAll_returnsPageOfUserInfo() {
            User user1 = User.builder()
                    .id(1L)
                    .email("user1@example.com")
                    .fullName("User One")
                    .role(User.Role.USER)
                    .isActive(true)
                    .verified(false)
                    .kycVerified(false)
                    .trustScore(0)
                    .build();

            User user2 = User.builder()
                    .id(2L)
                    .email("user2@example.com")
                    .fullName("User Two")
                    .role(User.Role.USER)
                    .isActive(true)
                    .verified(false)
                    .kycVerified(false)
                    .trustScore(0)
                    .build();

            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(List.of(user1, user2), pageable, 2);

            when(userRepository.findAll(pageable)).thenReturn(userPage);

            Page<UserInfo> result = userService.getAllUsers(pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.getContent().get(0).getEmail()).isEqualTo("user1@example.com");
            assertThat(result.getContent().get(1).getEmail()).isEqualTo("user2@example.com");
        }

        @Test
        @DisplayName("getAllUsers_emptyPage_returnsEmptyPage")
        void getAllUsers_emptyPage_returnsEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(userRepository.findAll(pageable)).thenReturn(emptyPage);

            Page<UserInfo> result = userService.getAllUsers(pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    // ============================================================
    // createUser()
    // ============================================================

    @Nested
    @DisplayName("createUser()")
    class CreateUser {

        @Test
        @DisplayName("createUser_validRequest_createsUserWithEncodedPassword")
        void createUser_validRequest_createsUserWithEncodedPassword() {
            CreateUserRequest request = new CreateUserRequest();
            request.setEmail("newuser@example.com");
            request.setFullName("New User");
            request.setPhoneNumber("0909123456");

            when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
            when(userRepository.existsByPhoneNumber("0909123456")).thenReturn(false);
            when(passwordEncoder.encode("TrustFund123@")).thenReturn("default_encoded_password");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(10L);
                return u;
            });

            UserInfo result = userService.createUser(request);

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getEmail()).isEqualTo("newuser@example.com");
            assertThat(result.getFullName()).isEqualTo("New User");
            assertThat(result.getRole()).isEqualTo(User.Role.USER);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User savedUser = captor.getValue();
            assertThat(savedUser.getPassword()).isEqualTo("default_encoded_password");
            assertThat(savedUser.getIsActive()).isTrue();
            assertThat(savedUser.getVerified()).isFalse();
        }

        @Test
        @DisplayName("createUser_duplicateEmail_throwsBadRequestException")
        void createUser_duplicateEmail_throwsBadRequestException() {
            CreateUserRequest request = new CreateUserRequest();
            request.setEmail("existing@example.com");
            request.setFullName("Any User");

            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("đã được sử dụng");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("createUser_duplicatePhone_throwsBadRequestException")
        void createUser_duplicatePhone_throwsBadRequestException() {
            CreateUserRequest request = new CreateUserRequest();
            request.setEmail("new@example.com");
            request.setFullName("New User");
            request.setPhoneNumber("0909123456");

            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(userRepository.existsByPhoneNumber("0909123456")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Số điện thoại");

            verify(userRepository, never()).save(any());
        }
    }

    // ============================================================
    // updateUser()
    // ============================================================

    @Nested
    @DisplayName("updateUser()")
    class UpdateUser {

        @Test
        @DisplayName("updateUser_fullNameChange_updatesAndReturnsUserInfo")
        void updateUser_fullNameChange_updatesAndReturnsUserInfo() {
            User existing = User.builder()
                    .id(1L)
                    .email("user@example.com")
                    .fullName("Old Name")
                    .isActive(true)
                    .verified(false)
                    .kycVerified(false)
                    .trustScore(0)
                    .build();

            UpdateUserRequest request = new UpdateUserRequest();
            request.setFullName("New Name");

            when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                    .thenReturn(ResponseEntity.ok().build());

            UserInfo result = userService.updateUser(1L, request);

            assertThat(result.getFullName()).isEqualTo("New Name");
            verify(userRepository).save(existing);
        }

        @Test
        @DisplayName("updateUser_newEmailNotDuplicate_updatesEmail")
        void updateUser_newEmailNotDuplicate_updatesEmail() {
            User existing = User.builder()
                    .id(1L)
                    .email("old@example.com")
                    .fullName("User")
                    .isActive(true)
                    .verified(false)
                    .kycVerified(false)
                    .trustScore(0)
                    .build();

            UpdateUserRequest request = new UpdateUserRequest();
            request.setEmail("new@example.com");

            when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                    .thenReturn(ResponseEntity.ok().build());

            UserInfo result = userService.updateUser(1L, request);

            assertThat(result.getEmail()).isEqualTo("new@example.com");
        }

        @Test
        @DisplayName("updateUser_duplicateEmail_throwsBadRequestException")
        void updateUser_duplicateEmail_throwsBadRequestException() {
            User existing = User.builder()
                    .id(1L)
                    .email("user@example.com")
                    .fullName("User")
                    .isActive(true)
                    .build();

            UpdateUserRequest request = new UpdateUserRequest();
            request.setEmail("taken@example.com");

            when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.updateUser(1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Email already exists");
        }

        @Test
        @DisplayName("updateUser_passwordChange_encodesNewPassword")
        void updateUser_passwordChange_encodesNewPassword() {
            User existing = User.builder()
                    .id(1L)
                    .email("user@example.com")
                    .fullName("User")
                    .password("old_hash")
                    .isActive(true)
                    .verified(false)
                    .kycVerified(false)
                    .trustScore(0)
                    .build();

            UpdateUserRequest request = new UpdateUserRequest();
            request.setPassword("newPassword123");

            when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(passwordEncoder.encode("newPassword123")).thenReturn("new_encoded_hash");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                    .thenReturn(ResponseEntity.ok().build());

            userService.updateUser(1L, request);

            assertThat(existing.getPassword()).isEqualTo("new_encoded_hash");
            verify(passwordEncoder).encode("newPassword123");
        }
    }

    // ============================================================
    // deleteUser()
    // ============================================================

    @Nested
    @DisplayName("deleteUser()")
    class DeleteUser {

        @Test
        @DisplayName("deleteUser_existingUser_deletesById")
        void deleteUser_existingUser_deletesById() {
            when(userRepository.existsById(1L)).thenReturn(true);

            userService.deleteUser(1L);

            verify(userRepository).deleteById(1L);
        }

        @Test
        @DisplayName("deleteUser_nonExistingUser_throwsNotFoundException")
        void deleteUser_nonExistingUser_throwsNotFoundException() {
            when(userRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> userService.deleteUser(99L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("User not found with id: 99");
        }
    }
}
