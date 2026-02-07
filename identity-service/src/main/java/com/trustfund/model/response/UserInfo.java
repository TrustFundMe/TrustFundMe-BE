package com.trustfund.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.trustfund.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfo {
    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String avatarUrl;
    private User.Role role;
    @JsonProperty("verified")
    private Boolean verified;
    @JsonProperty("isActive")
    private Boolean isActive;

    public static UserInfo fromUser(User user) {
        return UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .verified(user.getVerified())
                .isActive(user.getIsActive())
                .build();
    }
}


