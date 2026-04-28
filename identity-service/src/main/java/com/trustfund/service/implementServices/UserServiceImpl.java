package com.trustfund.service.implementServices;

import com.trustfund.exception.exceptions.BadRequestException;
import com.trustfund.exception.exceptions.NotFoundException;
import com.trustfund.model.User;
import com.trustfund.model.dto.response.ImportResult;
import com.trustfund.model.request.CreateUserRequest;
import com.trustfund.model.request.UpdateUserRequest;
import com.trustfund.model.response.CheckEmailResponse;
import com.trustfund.model.response.UserInfo;
import com.trustfund.repository.UserRepository;
import com.trustfund.service.interfaceServices.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Value("${campaign.service.url:http://localhost:8082}")
    private String campaignServiceUrl;

    @Override
    @Transactional(readOnly = true)
    public List<UserInfo> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserInfo::fromUser)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserInfo> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserInfo::fromUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserInfo> getAllStaff() {
        List<User> staffs = userRepository.findAllByRole(User.Role.STAFF);
        return staffs.stream()
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
        String newPhoneNumber = request.getPhoneNumber();
        if (newPhoneNumber != null && newPhoneNumber.trim().isEmpty()) {
            newPhoneNumber = null;
        }

        if (newPhoneNumber != null) {
            if (!newPhoneNumber.equals(user.getPhoneNumber())) {
                log.info("Checking phone number uniqueness: current={}, requested={}", user.getPhoneNumber(),
                        newPhoneNumber);
                if (userRepository.existsByPhoneNumber(newPhoneNumber)) {
                    log.warn("Phone number already exists: {}", newPhoneNumber);
                    throw new BadRequestException("Phone number already exists");
                }
                user.setPhoneNumber(newPhoneNumber);
            }
        } else if (user.getPhoneNumber() != null) {
            // User wants to clear their phone number
            user.setPhoneNumber(null);
        }
        if (request.getAvatarUrl() != null) {
            // Nếu có avatarUrl cũ và khác với mới, xóa file cũ
            String oldAvatarUrl = user.getAvatarUrl();
            if (oldAvatarUrl != null && !oldAvatarUrl.equals(request.getAvatarUrl())) {
                deleteOldAvatarFile(oldAvatarUrl);
            }
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getCvUrl() != null) {
            // Nếu có cvUrl cũ và khác với mới, xóa file cũ
            String oldCvUrl = user.getCvUrl();
            if (oldCvUrl != null && !oldCvUrl.equals(request.getCvUrl())) {
                deleteOldAvatarFile(oldCvUrl); // Reuse delete logic as it just hits the media-service by URL
            }
            user.setCvUrl(request.getCvUrl());
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        user = userRepository.save(user);
        log.info("Updated user with id: {}", id);

        // Notify campaign-service to evict cached author info so posts reflect
        // the new avatar/name immediately (fire-and-forget, non-blocking)
        try {
            restTemplate.postForEntity(
                campaignServiceUrl + "/api/feed-posts/internal/evict-user-cache/" + id,
                null,
                Void.class
            );
        } catch (Exception e) {
            log.warn("Could not evict user cache in campaign-service for userId={}: {}", id, e.getMessage());
        }

        return UserInfo.fromUser(user);
    }

    @Override
    @Transactional
    public UserInfo createUser(CreateUserRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        // Kiểm tra email đã tồn tại chưa
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email đã được sử dụng");
        }

        // Kiểm tra số điện thoại đã tồn tại chưa
        String phone = request.getPhoneNumber();
        if (phone != null && !phone.trim().isEmpty()) {
            if (userRepository.existsByPhoneNumber(phone.trim())) {
                throw new BadRequestException("Số điện thoại đã được sử dụng");
            }
        }

        // Xác định role
        User.Role role = User.Role.USER;
        if (request.getRole() != null && !request.getRole().trim().isEmpty()) {
            try {
                role = User.Role.valueOf(request.getRole().toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Vai trò không hợp lệ: " + request.getRole());
            }
        }

        User user = User.builder()
                .email(email)
                .fullName(request.getFullName().trim())
                .phoneNumber(phone != null && !phone.trim().isEmpty() ? phone.trim() : null)
                .role(role)
                .password(passwordEncoder.encode("TrustFund123@"))
                .isActive(true)
                .verified(false)
                .build();

        user = userRepository.save(user);
        log.info("Created user with id: {}, email: {}, role: {}", user.getId(), email, role);
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
    public UserInfo banUser(Long id, String reason) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        user.setIsActive(false);
        user.setBanReason(reason);
        user = userRepository.save(user);
        log.info("Banned user with id: {} for reason: {}", id, reason);
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

    @Override
    @Transactional
    public void upgradeToFundOwner(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));

        // Chỉ nâng cấp nếu đang là USER thường
        if (User.Role.USER.equals(user.getRole())) {
            user.setRole(User.Role.FUND_OWNER);
            userRepository.save(user);
            log.info("Upgraded user with id: {} to FUND_OWNER", id);
        } else {
            log.info("User with id: {} already has role: {} - skipping upgrade", id, user.getRole());
        }
    }

    @Override
    @Transactional
    public void upgradeToFundDonor(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));

        // Chỉ nâng cấp nếu đang là USER thường
        if (User.Role.USER.equals(user.getRole())) {
            user.setRole(User.Role.FUND_DONOR);
            userRepository.save(user);
            log.info("Upgraded user with id: {} to FUND_DONOR", id);
        } else {
            log.info("User with id: {} already has role: {} - skipping upgrade", id, user.getRole());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public java.io.ByteArrayInputStream exportUsersToExcel() {
        List<User> users = userRepository.findAll();
        return com.trustfund.utils.ExcelHelper.usersToExcel(users);
    }

    @Override
    @Transactional
    public ImportResult importUsersFromExcel(org.springframework.web.multipart.MultipartFile file) {
        try {
            log.info("Starting user import from file: {}, size: {}", file.getOriginalFilename(), file.getSize());
            List<User> users = com.trustfund.utils.ExcelHelper.excelToUsers(file.getInputStream());
            log.info("Parsed {} users from Excel", users.size());

            // Phase 1a: Normalize + deduplicate within file
            var emailsInFile = new java.util.HashSet<String>();
            var phonesInFile = new java.util.HashSet<String>();
            var skipped = new java.util.ArrayList<String>();
            var candidateEmails = new java.util.ArrayList<String>();
            var candidatePhones = new java.util.ArrayList<String>();
            var userByEmail = new java.util.LinkedHashMap<String, User>();
            var rowByEmail = new java.util.LinkedHashMap<String, Integer>();

            int rowIndex = 2; // Row 1 is header
            for (User u : users) {
                String email = (u.getEmail() != null ? u.getEmail().toLowerCase().trim() : "");
                String phone = (u.getPhoneNumber() != null ? u.getPhoneNumber().trim() : "");
                String fullName = (u.getFullName() != null ? u.getFullName().trim() : "");

                if (email.isEmpty()) {
                    if (phone.isEmpty() && fullName.isEmpty()) {
                        // Completely empty row, silently ignore
                        rowIndex++;
                        continue;
                    } else {
                        skipped.add("Dòng " + rowIndex + ": Thiếu email ở dòng có dữ liệu");
                        rowIndex++;
                        continue;
                    }
                }

                if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    skipped.add("Dòng " + rowIndex + ": Email không hợp lệ (" + email + ")");
                    rowIndex++;
                    continue;
                }

                if (emailsInFile.contains(email)) {
                    skipped.add("Dòng " + rowIndex + ": Trùng email trong file (" + email + ")");
                    rowIndex++;
                    continue;
                }

                if (!phone.isEmpty()) {
                    if (!phone.matches("^0[0-9]{9}$")) {
                        skipped.add("Dòng " + rowIndex + ": SĐT không hợp lệ (" + phone + ")");
                        rowIndex++;
                        continue;
                    }
                    if (phonesInFile.contains(phone)) {
                        skipped.add("Dòng " + rowIndex + ": Trùng SĐT trong file (" + phone + ")");
                        rowIndex++;
                        continue;
                    }
                }

                if (fullName.isEmpty()) {
                    skipped.add("Dòng " + rowIndex + ": Thiếu họ tên");
                    rowIndex++;
                    continue;
                }

                emailsInFile.add(email);
                if (!phone.isEmpty())
                    phonesInFile.add(phone);
                candidateEmails.add(email);
                if (!phone.isEmpty())
                    candidatePhones.add(phone);
                userByEmail.put(email, u);
                rowByEmail.put(email, rowIndex);
                
                rowIndex++;
            }

            // Phase 1b: Batch check DB existence (single query each)
            var existingEmails = new java.util.HashSet<String>(
                    userRepository.findExistingEmails(candidateEmails));
            var existingPhones = new java.util.HashSet<String>(
                    userRepository.findExistingPhones(candidatePhones));

            // Phase 1c: Final validation against DB
            var validEmails = new java.util.HashSet<String>();
            for (String email : emailsInFile) {
                int rIndex = rowByEmail.get(email);
                if (existingEmails.contains(email)) {
                    skipped.add("Dòng " + rIndex + ": Email đã tồn tại trong hệ thống (" + email + ")");
                    continue;
                }
                validEmails.add(email);
            }

            for (var entry : userByEmail.entrySet()) {
                String email = entry.getKey();
                User u = entry.getValue();
                int rIndex = rowByEmail.get(email);
                String phone = (u.getPhoneNumber() != null ? u.getPhoneNumber().trim() : "");
                if (!phone.isEmpty() && existingPhones.contains(phone) && validEmails.contains(email)) {
                    skipped.add("Dòng " + rIndex + ": SĐT đã tồn tại trong hệ thống (" + phone + ")");
                    validEmails.remove(email);
                }
            }

            // Reject the entire file if there are any errors or duplicates
            if (!skipped.isEmpty()) {
                log.warn("Excel import rejected: found {} errors", skipped.size());
                return ImportResult.builder()
                        .imported(0)
                        .skipped(skipped.size())
                        .skippedReasons(skipped)
                        .build();
            }

            // Phase 2: Build and save valid users
            String defaultPasswordHash = passwordEncoder.encode("TrustFund123@");
            List<User> validUsers = validEmails.stream()
                    .map(email -> {
                        User u = userByEmail.get(email);
                        u.setEmail(email);
                        u.setPassword(defaultPasswordHash);
                        return u;
                    })
                    .collect(java.util.stream.Collectors.toList());

            if (!validUsers.isEmpty()) {
                userRepository.saveAll(validUsers);
            }

            int imported = validUsers.size();
            log.info("Excel import: {} imported", imported);

            return ImportResult.builder()
                    .imported(imported)
                    .skipped(0)
                    .skippedReasons(new java.util.ArrayList<>())
                    .build();

        } catch (java.io.IOException e) {
            throw new RuntimeException("fail to store excel data: " + e.getMessage());
        }
    }

    @Override
    public java.io.ByteArrayInputStream downloadUsersTemplate() {
        return com.trustfund.utils.ExcelHelper.usersToExcelTemplate();
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
