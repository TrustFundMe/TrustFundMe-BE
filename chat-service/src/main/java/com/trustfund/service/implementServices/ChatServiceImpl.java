package com.trustfund.service.implementServices;

import com.trustfund.exception.exceptions.BadRequestException;
import com.trustfund.exception.exceptions.ForbiddenException;
import com.trustfund.exception.exceptions.NotFoundException;
import com.trustfund.model.Conversation;
import com.trustfund.model.Message;
import com.trustfund.model.request.CreateConversationRequest;
import com.trustfund.model.request.SendMessageRequest;
import com.trustfund.model.response.ConversationResponse;
import com.trustfund.model.response.MessageResponse;
import com.trustfund.repository.ConversationRepository;
import com.trustfund.repository.MessageRepository;
import com.trustfund.service.interfaceServices.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @Override
    @Transactional
    public ConversationResponse createConversation(CreateConversationRequest request, Long userId, String role) {
        Long staffId;
        Long fundOwnerId;

        if ("ROLE_STAFF".equals(role)) {
            staffId = userId;
            fundOwnerId = request.getFundOwnerId();
            if (fundOwnerId == null) {
                throw new BadRequestException("Fund Owner ID is required");
            }
        } else {
            // Assume Fund Owner or Donor
            fundOwnerId = userId;
            staffId = request.getStaffId();
            if (staffId == null) {
                // Default staff ID or throw exception
                throw new BadRequestException("Staff ID is required");
            }
        }

        // Check if conversation already exists
        var existingConversation = conversationRepository.findByStaffIdAndFundOwnerId(
                staffId, fundOwnerId);

        if (existingConversation.isPresent()) {
            return toConversationResponse(existingConversation.get());
        }

        Conversation conversation = Conversation.builder()
                .staffId(staffId)
                .fundOwnerId(fundOwnerId)
                .campaignId(request.getCampaignId())
                .build();

        Conversation saved = conversationRepository.save(conversation);
        return toConversationResponse(saved);
    }

    @Override
    public ConversationResponse getConversationById(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));

        return toConversationResponse(conversation);
    }

    @Override
    public java.util.List<ConversationResponse> getConversations(Long userId) {
        return conversationRepository.findByUserId(userId)
                .stream()
                .map(this::toConversationResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(Long conversationId, SendMessageRequest request, Long senderId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));

        // Verify sender is part of the conversation
        if (!conversation.getStaffId().equals(senderId) && !conversation.getFundOwnerId().equals(senderId)) {
            throw new ForbiddenException("You are not part of this conversation");
        }

        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .content(request.getContent())
                .isRead(false)
                .build();

        Message saved = messageRepository.save(message);

        // Update conversation's last message time
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return toMessageResponse(saved);
    }

    @Override
    public java.util.List<MessageResponse> getMessages(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));

        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(this::toMessageResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    private ConversationResponse toConversationResponse(Conversation entity) {
        return ConversationResponse.builder()
                .id(entity.getId())
                .staffId(entity.getStaffId())
                .fundOwnerId(entity.getFundOwnerId())
                .campaignId(entity.getCampaignId())
                .lastMessageAt(entity.getLastMessageAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private MessageResponse toMessageResponse(Message entity) {
        return MessageResponse.builder()
                .id(entity.getId())
                .conversationId(entity.getConversationId())
                .senderId(entity.getSenderId())
                .content(entity.getContent())
                .isRead(entity.getIsRead())
                .readAt(entity.getReadAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
