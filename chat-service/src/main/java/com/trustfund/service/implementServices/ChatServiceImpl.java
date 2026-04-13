package com.trustfund.service.implementServices;

import com.trustfund.exception.exceptions.BadRequestException;
import com.trustfund.exception.exceptions.ForbiddenException;
import com.trustfund.exception.exceptions.NotFoundException;
import com.trustfund.client.NotificationServiceClient;
import com.trustfund.model.Conversation;
import com.trustfund.model.Message;
import com.trustfund.model.request.CreateConversationRequest;
import com.trustfund.model.request.NotificationRequest;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final NotificationServiceClient notificationServiceClient;

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
            // User can create conversation without specifying staffId
        }

        if (request.getCampaignId() != null) {
            java.util.List<Conversation> list = conversationRepository.findByCampaignId(request.getCampaignId());
            if (list != null && !list.isEmpty()) {
                Conversation existing = list.get(0);
                if ("ROLE_STAFF".equals(role) && existing.getStaffId() == null) {
                    existing.setStaffId(userId);
                    existing = conversationRepository.save(existing);
                }
                return toConversationResponse(existing);
            }
        }

        Conversation conversation = Conversation.builder()
                .staffId(staffId)
                .fundOwnerId(fundOwnerId)
                .campaignId(request.getCampaignId())
                .build();

        Conversation saved = conversationRepository.save(conversation);

        // Save a bot welcome message for every newly created conversation
        Message welcomeMessage = Message.builder()
                .conversationId(saved.getId())
                .senderId(0L) // 0L = bot sentinel ID
                .content("Xin chào bạn cần hỗ trợ gì ạ? Chúng tôi sẽ hỗ trợ giải đáp trong thời gian ngắn nhất")
                .isRead(false)
                .build();
        messageRepository.save(welcomeMessage);

        return toConversationResponse(saved);
    }

    @Override
    public ConversationResponse getConversationById(Long conversationId, Long userId, String role) {
        if ("ROLE_STAFF".equals(role)) {
            conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new NotFoundException("Conversation not found"));
        } else {
            conversationRepository.findByIdAndUserId(conversationId, userId)
                    .orElseThrow(() -> new NotFoundException("Conversation not found"));
        }

        return toConversationResponse(conversationRepository.findById(conversationId).get());
    }

    @Override
    public ConversationResponse getConversationByCampaignId(Long campaignId, Long userId) {
        return conversationRepository.findByFundOwnerIdAndCampaignId(userId, campaignId)
                .stream()
                .findFirst()
                .map(this::toConversationResponse)
                .orElseThrow(() -> new NotFoundException("Conversation not found for this campaign"));
    }

    @Override
    public java.util.List<ConversationResponse> getConversations(Long userId, String role) {
        if ("ROLE_STAFF".equals(role)) {
            return conversationRepository.findAllOrderByLastMessageAtDesc()
                    .stream()
                    .map(this::toConversationResponse)
                    .collect(java.util.stream.Collectors.toList());
        }
        return conversationRepository.findByUserId(userId)
                .stream()
                .map(this::toConversationResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public java.util.List<ConversationResponse> getAllConversations() {
        return conversationRepository.findAllOrderByLastMessageAtDesc()
                .stream()
                .map(this::toConversationResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(Long conversationId, SendMessageRequest request, Long senderId, String role) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));

        System.out.println("[ChatService] Processing message: senderId=" + senderId + ", role=" + role);
        System.out.println("[ChatService] Conversation#" + conversationId + ": staffId=" + conversation.getStaffId()
                + ", fundOwnerId=" + conversation.getFundOwnerId());

        // If conversation has no staff and sender is staff, assign this staff
        if (conversation.getStaffId() == null && "ROLE_STAFF".equals(role)) {
            System.out
                    .println("[ChatService] Assigning staff member " + senderId + " to conversation " + conversationId);
            conversation.setStaffId(senderId);
            conversationRepository.save(conversation);
        }

        // Verify sender is part of the conversation (Staff members can reply to any
        // conversation)
        boolean isStaff = "ROLE_STAFF".equals(role);
        boolean isAssignedStaff = senderId.equals(conversation.getStaffId());
        boolean isFundOwner = senderId.equals(conversation.getFundOwnerId());

        if (!isStaff && !isAssignedStaff && !isFundOwner) {
            System.err.println(
                    "[ChatService] FORBIDDEN: Sender " + senderId + " (" + role + ") is not part of conversation "
                            + conversationId);
            throw new ForbiddenException("You are not part of this conversation");
        }

        // Create and save message
        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .content(request.getContent())
                .createdAt(LocalDateTime.now())
                .isRead(false)
                .build();

        Message saved = messageRepository.save(message);
        System.out.println("[ChatService] SUCCESS: Message saved. ID: " + saved.getId() + " Sender: " + senderId);

        // Update conversation's last message time
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // Notify the recipient
        Long receiverId = senderId.equals(conversation.getFundOwnerId())
                ? conversation.getStaffId()
                : conversation.getFundOwnerId();

        if (receiverId != null && !receiverId.equals(0L)) {
            NotificationRequest notificationRequest = NotificationRequest.builder()
                    .userId(receiverId)
                    .type("NEW_MESSAGE")
                    .targetId(conversationId)
                    .targetType("Conversation")
                    .title("Tin nhắn mới")
                    .content(request.getContent())
                    .data(Map.of("senderId", senderId, "conversationId", conversationId))
                    .build();
            notificationServiceClient.sendNotification(notificationRequest);
        }

        return toMessageResponse(saved);
    }

    @Override
    public java.util.List<MessageResponse> getMessages(Long conversationId, Long userId, String role) {
        if ("ROLE_STAFF".equals(role)) {
            conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new NotFoundException("Conversation not found"));
        } else {
            conversationRepository.findByIdAndUserId(conversationId, userId)
                    .orElseThrow(() -> new NotFoundException("Conversation not found"));
        }

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
