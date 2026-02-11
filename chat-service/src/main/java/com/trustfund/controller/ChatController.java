package com.trustfund.controller;

import com.trustfund.model.request.CreateConversationRequest;
import com.trustfund.model.request.SendMessageRequest;
import com.trustfund.model.response.ConversationResponse;
import com.trustfund.model.response.MessageResponse;
import com.trustfund.service.interfaceServices.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Chat APIs for communication between Staff and Fund Owners")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    @Operation(summary = "Create conversation", description = "Create a new conversation between staff and fund owner. Fund owners and donors can also create conversations.")
    public ResponseEntity<ConversationResponse> createConversation(
            @Valid @RequestBody CreateConversationRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        String currentRole = authentication.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .orElse("ROLE_FUND_OWNER"); // Default fallback if needed

        ConversationResponse response = chatService.createConversation(request, userId, currentRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get conversations", description = "Get list of conversations. Only STAFF can view all conversations.")
    public ResponseEntity<java.util.List<ConversationResponse>> getConversations() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        String currentRole = authentication.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .orElse(null);

        if (currentRole == null || !currentRole.equals("ROLE_STAFF")) {
            throw new com.trustfund.exception.exceptions.ForbiddenException("Only staff can view conversations");
        }

        return ResponseEntity.ok(chatService.getConversations(userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get conversation by ID", description = "Get conversation details by ID")
    public ResponseEntity<ConversationResponse> getConversationById(@PathVariable("id") Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        ConversationResponse response = chatService.getConversationById(id, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{conversationId}/messages")
    @Operation(summary = "Send message", description = "Send a message in a conversation")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable("conversationId") Long conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long senderId = Long.parseLong(authentication.getName());

        MessageResponse response = chatService.sendMessage(conversationId, request, senderId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{conversationId}/messages")
    @Operation(summary = "Get messages", description = "Get messages in a conversation")
    public ResponseEntity<java.util.List<MessageResponse>> getMessages(
            @PathVariable("conversationId") Long conversationId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        return ResponseEntity.ok(chatService.getMessages(conversationId, userId));
    }
}
