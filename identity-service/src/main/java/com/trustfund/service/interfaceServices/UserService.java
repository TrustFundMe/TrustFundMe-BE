package com.trustfund.service.interfaceServices;

import com.trustfund.model.request.UpdateUserRequest;
import com.trustfund.model.response.CheckEmailResponse;
import com.trustfund.model.response.UserInfo;

import java.util.List;

public interface UserService {
    List<UserInfo> getAllUsers();

    List<UserInfo> getAllStaff();

    UserInfo getUserById(Long id);

    UserInfo updateUser(Long id, UpdateUserRequest request);

    void deleteUser(Long id);

    UserInfo banUser(Long id, String reason);

    UserInfo unbanUser(Long id);

    CheckEmailResponse checkEmail(String email);

    void upgradeToFundOwner(Long id);

    void upgradeToFundDonor(Long id);
}
