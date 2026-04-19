package com.trustfund.service;

import com.trustfund.client.NotificationServiceClient;
import com.trustfund.exception.exceptions.NotFoundException;
import com.trustfund.model.Conversation;
import com.trustfund.model.Message;
import com.trustfund.model.request.CreateConversationRequest;
import com.trustfund.model.request.NotificationRequest;
import com.trustfund.model.request.SendMessageRequest;
import com.trustfund.model.response.ConversationResponse;
import com.trustfund.model.response.MessageResponse;
import com.trustfund.repository.ConversationRepository;
import com.trustfund.repository.MessageRepository;
import com.trustfund.service.implementServices.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private NotificationServiceClient notificationServiceClient;

    @InjectMocks
    private ChatServiceImpl chatService;

    @Captor
    private ArgumentCaptor<Message> messageCaptor;

    @Captor
    private ArgumentCaptor<NotificationRequest> notificationCaptor;

    private Conversation sampleConversation;
    private Message sampleMessage;

    @BeforeEach
    void setUp() {
        sampleConversation = Conversation.builder()
                .id(1L)
                .staffId(100L)
                .fundOwnerId(200L)
                .campaignId(10L)
                .lastMessageAt(LocalDateTime.now().minusHours(1))
                .createdAt(LocalDateTime.now().minusDays(2))
                .updatedAt(LocalDateTime.now())
                .build();

        sampleMessage = Message.builder()
                .id(1L)
                .conversationId(1L)
                .senderId(200L)
                .content("Hello, I need help")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // sendMessage tests
    // ─────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("sendMessage")
    class SendMessageTests {

        @Test
        @DisplayName("sendMessage_validSender_savesMessageAndReturnsMessageResponse")
        void sendMessage_validSender_savesMessageAndReturnsMessageResponse() {
            // Arrange
            SendMessageRequest request = SendMessageRequest.builder()
                    .content("Hello, I need help")
                    .build();

            when(conversationRepository.findById(1L)).thenReturn(Optional.of(sampleConversation));
            when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
                Message m = invocation.getArgument(0);
                m.setId(1L);
                return m;
            });
            when(conversationRepository.save(any(Conversation.class))).thenReturn(sampleConversation);

            // Act
            MessageResponse response = chatService.sendMessage(1L, request, 200L, "ROLE_FUND_OWNER");

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getConversationId()).isEqualTo(1L);
            assertThat(response.getSenderId()).isEqualTo(200L);
            assertThat(response.getContent()).isEqualTo("Hello, I need help");
            assertThat(response.getIsRead()).isFalse();

            verify(messageRepository).save(messageCaptor.capture());
            Message savedMessage = messageCaptor.getValue();
            assertThat(savedMessage.getConversationId()).isEqualTo(1L);
            assertThat(savedMessage.getSenderId()).isEqualTo(200L);
            assertThat(savedMessage.getContent()).isEqualTo("Hello, I need help");

            verify(conversationRepository).save(any(Conversation.class));
        }

        @Test
        @DisplayName("sendMessage_conversationNotFound_throwsNotFoundException")
        void sendMessage_conversationNotFound_throwsNotFoundException() {
            // Arrange
            SendMessageRequest request = SendMessageRequest.builder()
                    .content("Hello")
                    .build();

            when(conversationRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> chatService.sendMessage(999L, request, 200L, "ROLE_FUND_OWNER"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Conversation not found");

            verify(messageRepository, never()).save(any(Message.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // getConversations tests
    // ─────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getConversations")
    class GetConversationsTests {

        @Test
        @DisplayName("getConversations_staffRole_returnsListOrderedByLastMessageAtDesc")
        void getConversations_staffRole_returnsListOrderedByLastMessageAtDesc() {
            // Arrange
            Conversation conv1 = Conversation.builder()
                    .id(1L).staffId(100L).fundOwnerId(200L)
                    .lastMessageAt(LocalDateTime.now().minusHours(2))
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .updatedAt(LocalDateTime.now())
                    .build();
            Conversation conv2 = Conversation.builder()
                    .id(2L).staffId(100L).fundOwnerId(300L)
                    .lastMessageAt(LocalDateTime.now().minusHours(1))
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .updatedAt(LocalDateTime.now())
                    .build();
            Conversation conv3 = Conversation.builder()
                    .id(3L).staffId(100L).fundOwnerId(400L)
                    .lastMessageAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // Repository is expected to return already sorted desc
            when(conversationRepository.findAllOrderByLastMessageAtDesc())
                    .thenReturn(List.of(conv3, conv2, conv1));

            // Act
            List<ConversationResponse> responses = chatService.getConversations(100L, "ROLE_STAFF");

            // Assert
            assertThat(responses).hasSize(3);
            assertThat(responses.get(0).getId()).isEqualTo(3L);
            assertThat(responses.get(1).getId()).isEqualTo(2L);
            assertThat(responses.get(2).getId()).isEqualTo(1L);

            verify(conversationRepository).findAllOrderByLastMessageAtDesc();
            verify(conversationRepository, never()).findByUserId(anyLong());
        }

        @Test
        @DisplayName("getConversations_nonStaffRole_returnsConversationsForUser")
        void getConversations_nonStaffRole_returnsConversationsForUser() {
            // Arrange
            Conversation conv1 = Conversation.builder()
                    .id(1L).staffId(100L).fundOwnerId(200L)
                    .lastMessageAt(LocalDateTime.now().minusHours(1))
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(conversationRepository.findByUserId(200L)).thenReturn(List.of(conv1));

            // Act
            List<ConversationResponse> responses = chatService.getConversations(200L, "ROLE_FUND_OWNER");

            // Assert
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getId()).isEqualTo(1L);

            verify(conversationRepository).findByUserId(200L);
            verify(conversationRepository, never()).findAllOrderByLastMessageAtDesc();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // getMessages tests
    // ─────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMessages")
    class GetMessagesTests {

        @Test
        @DisplayName("getMessages_staffRole_returnsMessagesForConversation")
        void getMessages_staffRole_returnsMessagesForConversation() {
            // Arrange
            Message msg1 = Message.builder()
                    .id(1L).conversationId(1L).senderId(200L)
                    .content("Hello").isRead(true)
                    .createdAt(LocalDateTime.now().minusHours(1))
                    .build();
            Message msg2 = Message.builder()
                    .id(2L).conversationId(1L).senderId(100L)
                    .content("Hi there").isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(conversationRepository.findById(1L)).thenReturn(Optional.of(sampleConversation));
            when(messageRepository.findByConversationIdOrderByCreatedAtAsc(1L))
                    .thenReturn(List.of(msg1, msg2));

            // Act
            List<MessageResponse> responses = chatService.getMessages(1L, 100L, "ROLE_STAFF");

            // Assert
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getId()).isEqualTo(1L);
            assertThat(responses.get(0).getContent()).isEqualTo("Hello");
            assertThat(responses.get(1).getId()).isEqualTo(2L);
            assertThat(responses.get(1).getContent()).isEqualTo("Hi there");

            verify(conversationRepository).findById(1L);
            verify(messageRepository).findByConversationIdOrderByCreatedAtAsc(1L);
        }

        @Test
        @DisplayName("getMessages_nonStaffRole_withValidUserId_returnsMessages")
        void getMessages_nonStaffRole_withValidUserId_returnsMessages() {
            // Arrange
            Message msg = Message.builder()
                    .id(1L).conversationId(1L).senderId(200L)
                    .content("Test message").isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(conversationRepository.findByIdAndUserId(1L, 200L))
                    .thenReturn(Optional.of(sampleConversation));
            when(messageRepository.findByConversationIdOrderByCreatedAtAsc(1L))
                    .thenReturn(List.of(msg));

            // Act
            List<MessageResponse> responses = chatService.getMessages(1L, 200L, "ROLE_FUND_OWNER");

            // Assert
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getContent()).isEqualTo("Test message");

            verify(conversationRepository).findByIdAndUserId(1L, 200L);
            verify(messageRepository).findByConversationIdOrderByCreatedAtAsc(1L);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // createConversation tests
    // ─────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createConversation")
    class CreateConversationTests {

        @Test
        @DisplayName("createConversation_fundOwnerCreatesNew_returnsConversationResponse")
        void createConversation_fundOwnerCreatesNew_returnsConversationResponse() {
            // Arrange
            CreateConversationRequest request = CreateConversationRequest.builder()
                    .fundOwnerId(200L)
                    .staffId(100L)
                    .campaignId(10L)
                    .build();

            Conversation savedConversation = Conversation.builder()
                    .id(5L)
                    .staffId(100L)
                    .fundOwnerId(200L)
                    .campaignId(10L)
                    .lastMessageAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(conversationRepository.findByCampaignId(10L)).thenReturn(List.of());
            when(conversationRepository.save(any(Conversation.class))).thenReturn(savedConversation);
            when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
                Message m = invocation.getArgument(0);
                m.setId(99L);
                return m;
            });

            // Act
            ConversationResponse response = chatService.createConversation(request, 200L, "ROLE_FUND_OWNER");

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(5L);
            assertThat(response.getStaffId()).isEqualTo(100L);
            assertThat(response.getFundOwnerId()).isEqualTo(200L);
            assertThat(response.getCampaignId()).isEqualTo(10L);

            verify(conversationRepository).save(any(Conversation.class));
            // A welcome bot message should be saved
            verify(messageRepository).save(messageCaptor.capture());
            Message welcomeMsg = messageCaptor.getValue();
            assertThat(welcomeMsg.getSenderId()).isEqualTo(0L); // bot sentinel ID
            assertThat(welcomeMsg.getContent()).contains("Xin chào");
        }

        @Test
        @DisplayName("createConversation_staffWithExistingCampaign_returnsExistingConversation")
        void createConversation_staffWithExistingCampaign_returnsExistingConversation() {
            // Arrange
            CreateConversationRequest request = CreateConversationRequest.builder()
                    .fundOwnerId(200L)
                    .campaignId(10L)
                    .build();

            Conversation existingConv = Conversation.builder()
                    .id(3L)
                    .staffId(null) // unassigned
                    .fundOwnerId(200L)
                    .campaignId(10L)
                    .lastMessageAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now().minusDays(3))
                    .updatedAt(LocalDateTime.now())
                    .build();

            Conversation updatedConv = Conversation.builder()
                    .id(3L)
                    .staffId(100L)
                    .fundOwnerId(200L)
                    .campaignId(10L)
                    .lastMessageAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now().minusDays(3))
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(conversationRepository.findByCampaignId(10L)).thenReturn(List.of(existingConv));
            when(conversationRepository.save(any(Conversation.class))).thenReturn(updatedConv);

            // Act
            ConversationResponse response = chatService.createConversation(request, 100L, "ROLE_STAFF");

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(3L);
            assertThat(response.getStaffId()).isEqualTo(100L);
            // Bot message is only sent when no existing conversation is reused
            verify(messageRepository, never()).save(any(Message.class));
        }

        @Test
        @DisplayName("createConversation_staffMissingFundOwnerId_throwsBadRequestException")
        void createConversation_staffMissingFundOwnerId_throwsBadRequestException() {
            // Arrange
            CreateConversationRequest request = CreateConversationRequest.builder()
                    .fundOwnerId(null)
                    .campaignId(null)
                    .build();

            // Act & Assert
            assertThatThrownBy(() -> chatService.createConversation(request, 100L, "ROLE_STAFF"))
                    .isInstanceOf(com.trustfund.exception.exceptions.BadRequestException.class)
                    .hasMessageContaining("Fund Owner ID is required");

            verify(conversationRepository, never()).save(any(Conversation.class));
        }
    }
}