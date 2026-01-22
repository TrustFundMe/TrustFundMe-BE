package com.trustfund.service.implementServices;

import com.trustfund.exception.exceptions.BadRequestException;
import com.trustfund.exception.exceptions.NotFoundException;
import com.trustfund.model.User;
import com.trustfund.model.request.UpdateUserRequest;
import com.trustfund.model.response.CheckEmailResponse;
import com.trustfund.model.response.UserInfo;
import com.trustfund.repository.UserRepository;
import com.trustfund.service.interfaceServices.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;

    @Value("${media.service.url:http://localhost:8083}")
    private String mediaServiceUrl;

    @Override
    @Transactional(readOnly = true)
    public List<UserInfo> getAllUsers() {
        List<User> users = userRepository.findAll();
        log.info("Retrieved {} users", users.size());
        return users.stream()
                .map(UserInfo::fromUser)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfo getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        log.info("Retrieved user with id: {}", id);
        return UserInfo.fromUser(user);
    }

    @Override
    @Transactional
    public UserInfo updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));

        // Kiểm tra email trùng lặp nếu email được thay đổi
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email already exists");
            }
            user.setEmail(request.getEmail());
        }

        // Cập nhật các trường khác nếu có
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAvatarUrl() != null) {
            // Nếu có avatarUrl cũ và khác với mới, xóa file cũ
            String oldAvatarUrl = user.getAvatarUrl();
            if (oldAvatarUrl != null && !oldAvatarUrl.equals(request.getAvatarUrl())) {
                deleteOldAvatarFile(oldAvatarUrl);
            }
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        user = userRepository.save(user);
        log.info("Updated user with id: {}", id);
        return UserInfo.fromUser(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
        log.info("Deleted user with id: {}", id);
    }

    @Override
    @Transactional
    public UserInfo banUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        user.setIsActive(false);
        user = userRepository.save(user);
        log.info("Banned user with id: {}", id);
        return UserInfo.fromUser(user);
    }

    @Override
    @Transactional
    public UserInfo unbanUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        user.setIsActive(true);
        user = userRepository.save(user);
        log.info("Unbanned user with id: {}", id);
        return UserInfo.fromUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public CheckEmailResponse checkEmail(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return CheckEmailResponse.builder()
                    .exists(true)
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .build();
        } else {
            return CheckEmailResponse.builder()
                    .exists(false)
                    .email(email)
                    .fullName(null)
                    .build();
        }
    }

    private void deleteOldAvatarFile(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            return;
        }
        try {
            String deleteUrl = mediaServiceUrl + "/api/media/by-url?url=" + 
                    java.net.URLEncoder.encode(avatarUrl, java.nio.charset.StandardCharsets.UTF_8);
            restTemplate.delete(deleteUrl);
            log.info("Deleted old avatar file: {}", avatarUrl);
        } catch (Exception e) {
            // Log but don't fail the update if delete fails
            log.warn("Failed to delete old avatar file {}: {}", avatarUrl, e.getMessage());
        }
    }
}
