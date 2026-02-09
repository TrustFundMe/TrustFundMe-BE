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
            @Payload ChatMessageRequest request
    ) {
        // In a real scenario, senderId should be extracted from the authenticated user (Principal)
        // For simplicity or if trusting the payload (internal/prototype), we use request.getSenderId()
        // Ideally: Long senderId = Long.parseLong(principal.getName());

        SendMessageRequest sendRequest = SendMessageRequest.builder()
                .content(request.getContent())
                .build();
        
        return chatService.sendMessage(conversationId, sendRequest, request.getSenderId()); 
    }
}
