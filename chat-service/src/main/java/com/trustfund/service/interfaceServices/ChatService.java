package com.trustfund.service.interfaceServices;

import com.trustfund.model.request.CreateConversationRequest;
import com.trustfund.model.request.SendMessageRequest;
import com.trustfund.model.response.ConversationResponse;
import com.trustfund.model.response.MessageResponse;

public interface ChatService {
    ConversationResponse createConversation(CreateConversationRequest request, Long userId, String role);

    ConversationResponse getConversationById(Long conversationId, Long userId, String role);

    ConversationResponse getConversationByCampaignId(Long campaignId, Long userId);

    java.util.List<ConversationResponse> getConversations(Long userId);

    java.util.List<ConversationResponse> getAllConversations();

    MessageResponse sendMessage(Long conversationId, SendMessageRequest request, Long senderId, String role);

    java.util.List<MessageResponse> getMessages(Long conversationId, Long userId, String role);
}
