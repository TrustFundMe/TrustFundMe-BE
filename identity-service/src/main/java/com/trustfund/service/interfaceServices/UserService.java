package com.trustfund.service.interfaceServices;

import com.trustfund.model.dto.response.ImportResult;
import com.trustfund.model.request.CreateUserRequest;
import com.trustfund.model.request.UpdateUserRequest;
import com.trustfund.model.response.CheckEmailResponse;
import com.trustfund.model.response.UserInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    List<UserInfo> getAllUsers();

    Page<UserInfo> getAllUsers(Pageable pageable);

    List<UserInfo> getAllStaff();

    UserInfo getUserById(Long id);

    UserInfo updateUser(Long id, UpdateUserRequest request);

    UserInfo createUser(CreateUserRequest request);

    void deleteUser(Long id);

    UserInfo banUser(Long id, String reason);

    UserInfo unbanUser(Long id);

    CheckEmailResponse checkEmail(String email);

    void upgradeToFundOwner(Long id);

    void upgradeToFundDonor(Long id);

    java.io.ByteArrayInputStream exportUsersToExcel();

    ImportResult importUsersFromExcel(org.springframework.web.multipart.MultipartFile file);

    java.io.ByteArrayInputStream downloadUsersTemplate();
}
