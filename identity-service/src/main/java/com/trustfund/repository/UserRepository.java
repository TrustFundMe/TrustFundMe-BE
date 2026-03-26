package com.trustfund.repository;

import com.trustfund.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
    Boolean existsByPhoneNumber(String phoneNumber);
    java.util.List<User> findAllByRole(User.Role role);

    @Query("SELECT u.email FROM User u WHERE u.email IN :emails")
    List<String> findExistingEmails(@Param("emails") List<String> emails);

    @Query("SELECT u.phoneNumber FROM User u WHERE u.phoneNumber IN :phones AND u.phoneNumber IS NOT NULL AND u.phoneNumber <> ''")
    List<String> findExistingPhones(@Param("phones") List<String> phones);
}


