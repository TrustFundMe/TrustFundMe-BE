package com.trustfund.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageNotificationRequest {
    private Integer recipientId;
    private String senderName;
    private String messageContent;
    private Integer campaignId;
    private String campaignTitle;
}
