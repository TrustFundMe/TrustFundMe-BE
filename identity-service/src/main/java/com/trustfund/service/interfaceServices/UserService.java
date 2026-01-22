package com.trustfund.service.interfaceServices;

import com.trustfund.model.request.UpdateUserRequest;
import com.trustfund.model.response.CheckEmailResponse;
import com.trustfund.model.response.UserInfo;

import java.util.List;

public interface UserService {
    List<UserInfo> getAllUsers();
    UserInfo getUserById(Long id);
    UserInfo updateUser(Long id, UpdateUserRequest request);
    void deleteUser(Long id);
    UserInfo banUser(Long id);
    UserInfo unbanUser(Long id);
    CheckEmailResponse checkEmail(String email);
}
