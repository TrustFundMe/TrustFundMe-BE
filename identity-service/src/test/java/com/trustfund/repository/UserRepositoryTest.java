package com.trustfund.repository;

import com.trustfund.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("findByEmail returns user when exists")
    void findByEmailReturnsUserWhenExists() {
        User user = User.builder()
                .email("test@example.com")
                .password("encoded")
                .fullName("Test User")
                .role(User.Role.USER)
                .isActive(true)
                .verified(false)
                .build();
        entityManager.persistAndFlush(user);

        Optional<User> result = userRepository.findByEmail("test@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getFullName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("findByEmail returns empty when not found")
    void findByEmailReturnsEmptyWhenNotFound() {
        Optional<User> result = userRepository.findByEmail("unknown@example.com");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail returns true when email exists")
    void existsByEmailReturnsTrueWhenExists() {
        User user = User.builder()
                .email("taken@example.com")
                .password("encoded")
                .fullName("Taken User")
                .role(User.Role.USER)
                .isActive(true)
                .verified(false)
                .build();
        entityManager.persistAndFlush(user);

        assertThat(userRepository.existsByEmail("taken@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nottaken@example.com")).isFalse();
    }

    @Test
    @DisplayName("findAllByRole returns only users with that role")
    void findAllByRoleReturnsOnlyMatchingRole() {
        User user1 = User.builder()
                .email("user@example.com")
                .password("encoded")
                .fullName("User")
                .role(User.Role.USER)
                .isActive(true)
                .verified(false)
                .build();

        User staff1 = User.builder()
                .email("staff@example.com")
                .password("encoded")
                .fullName("Staff User")
                .role(User.Role.STAFF)
                .isActive(true)
                .verified(false)
                .build();

        entityManager.persistAndFlush(user1);
        entityManager.persistAndFlush(staff1);

        List<User> staffMembers = userRepository.findAllByRole(User.Role.STAFF);

        assertThat(staffMembers).hasSize(1);
        assertThat(staffMembers.get(0).getEmail()).isEqualTo("staff@example.com");
    }

    @Test
    @DisplayName("save persists user and can be retrieved by id")
    void savePersistsAndCanBeRetrieved() {
        User user = User.builder()
                .email("persist@example.com")
                .password("encoded")
                .fullName("Persist User")
                .role(User.Role.USER)
                .isActive(true)
                .verified(false)
                .build();
        User saved = userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        Optional<User> found = userRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("persist@example.com");
    }

    @Test
    @DisplayName("deleteById removes user from database")
    void deleteByIdRemovesUser() {
        User user = User.builder()
                .email("delete@example.com")
                .password("encoded")
                .fullName("Delete Me")
                .role(User.Role.USER)
                .isActive(true)
                .verified(false)
                .build();
        User saved = userRepository.save(user);
        entityManager.flush();

        userRepository.deleteById(saved.getId());
        entityManager.flush();

        assertThat(userRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("existsByPhoneNumber returns true when phone exists")
    void existsByPhoneNumberReturnsTrueWhenExists() {
        User user = User.builder()
                .email("phone@example.com")
                .password("encoded")
                .fullName("Phone User")
                .phoneNumber("0909123456")
                .role(User.Role.USER)
                .isActive(true)
                .verified(false)
                .build();
        entityManager.persistAndFlush(user);

        assertThat(userRepository.existsByPhoneNumber("0909123456")).isTrue();
        assertThat(userRepository.existsByPhoneNumber("0909000000")).isFalse();
    }

    @Test
    @DisplayName("findAll returns paginated results")
    void findAllPaginatedWorks() {
        for (int i = 0; i < 5; i++) {
            User user = User.builder()
                    .email("pageuser" + i + "@example.com")
                    .password("encoded")
                    .fullName("Page User " + i)
                    .role(User.Role.USER)
                    .isActive(true)
                    .verified(false)
                    .build();
            entityManager.persistAndFlush(user);
        }

        Page<User> page = userRepository.findAll(PageRequest.of(0, 3));

        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.isFirst()).isTrue();
        assertThat(page.isLast()).isFalse();
    }
}