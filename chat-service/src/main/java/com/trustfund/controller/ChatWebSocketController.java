package com.trustfund.controller;

import com.trustfund.model.request.ChatMessageRequest;
import com.trustfund.model.request.SendMessageRequest;
import com.trustfund.model.response.MessageResponse;
import com.trustfund.service.interfaceServices.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat/{conversationId}")
    @SendTo("/topic/conversation/{conversationId}")
    public MessageResponse sendMessage(
            @DestinationVariable Long conversationId,
            @Payload ChatMessageRequest request) {

        System.out.println("[WebSocket] Receiving message for conversation: " + conversationId);
        System.out.println("[WebSocket] SenderID from Payload: " + request.getSenderId());

        SendMessageRequest sendRequest = SendMessageRequest.builder()
                .content(request.getContent())
                .build();

        // Use the role sent from the frontend if available
        String role = request.getSenderRole() != null ? request.getSenderRole() : "ROLE_USER";
        System.out.println("[WebSocket] User Role: " + role);

        return chatService.sendMessage(conversationId, sendRequest, request.getSenderId(), role);
    }
}
