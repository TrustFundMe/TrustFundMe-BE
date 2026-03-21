package com.trustfund.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private Long userId;
    private String type;
    private Long targetId;
    private String targetType;
    private String title;
    private String content;
    private Map<String, Object> data;
}
