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
@RequestMapping({ "/api/conversations", "/api/chat/conversations" })
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
    @Operation(summary = "Get conversations", description = "Get list of conversations. STAFF can view all conversations; Fund Owners and Donors can view their own conversations.")
    public ResponseEntity<java.util.List<ConversationResponse>> getConversations() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        String currentRole = authentication.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .orElse(null);

        if ("ROLE_STAFF".equals(currentRole)) {
            return ResponseEntity.ok(chatService.getAllConversations());
        } else {
            // Fund Owners and Donors can only view their own conversations
            return ResponseEntity.ok(chatService.getConversations(userId));
        }
    }

    @GetMapping("/campaign/{campaignId}")
    @Operation(summary = "Get conversation by campaign ID", description = "Find if a conversation exists for the current user and a specific campaign")
    public ResponseEntity<ConversationResponse> getConversationByCampaignId(
            @PathVariable("campaignId") Long campaignId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        ConversationResponse response = chatService.getConversationByCampaignId(campaignId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get conversation by ID", description = "Get conversation details by ID")
    public ResponseEntity<ConversationResponse> getConversationById(@PathVariable("id") Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        String currentRole = authentication.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .orElse("ROLE_FUND_OWNER");

        ConversationResponse response = chatService.getConversationById(id, userId, currentRole);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{conversationId}/messages")
    @Operation(summary = "Send message", description = "Send a message in a conversation")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable("conversationId") Long conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long senderId = Long.parseLong(authentication.getName());

        String currentRole = authentication.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .orElse("ROLE_FUND_OWNER");

        MessageResponse response = chatService.sendMessage(conversationId, request, senderId, currentRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{conversationId}/messages")
    @Operation(summary = "Get messages", description = "Get messages in a conversation")
    public ResponseEntity<java.util.List<MessageResponse>> getMessages(
            @PathVariable("conversationId") Long conversationId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        String currentRole = authentication.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .orElse("ROLE_FUND_OWNER");

        return ResponseEntity.ok(chatService.getMessages(conversationId, userId, currentRole));
    }
}
