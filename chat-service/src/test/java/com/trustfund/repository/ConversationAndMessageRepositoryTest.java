package com.trustfund.repository;

import com.trustfund.model.Conversation;
import com.trustfund.model.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class ConversationAndMessageRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private ConversationRepository convRepo;
    @Autowired private MessageRepository msgRepo;

    private Conversation persistConv(Long staffId, Long fundOwnerId, Long campaignId) {
        return em.persistAndFlush(Conversation.builder()
                .staffId(staffId).fundOwnerId(fundOwnerId).campaignId(campaignId).build());
    }

    private Message persistMsg(Long convId, Long senderId, boolean read) {
        return em.persistAndFlush(Message.builder()
                .conversationId(convId).senderId(senderId).content("hi").isRead(read).build());
    }

    @Test @DisplayName("findByStaffIdAndFundOwnerId_bidirectional")
    void byStaffAndOwner() {
        persistConv(100L, 200L, null);
        assertThat(convRepo.findByStaffIdAndFundOwnerId(100L, 200L)).isPresent();
        assertThat(convRepo.findByStaffIdAndFundOwnerId(200L, 100L)).isPresent();
    }

    @Test @DisplayName("findByUserId_returnsConversations")
    void byUser() {
        persistConv(100L, 200L, null);
        assertThat(convRepo.findByUserId(100L)).hasSize(1);
        assertThat(convRepo.findByUserId(200L)).hasSize(1);
    }

    @Test @DisplayName("findUnassigned_staffNull")
    void unassigned() {
        persistConv(null, 200L, null);
        assertThat(convRepo.findUnassignedConversation(200L)).isPresent();
    }

    @Test @DisplayName("findByCampaignId_returnsList")
    void byCampaign() {
        persistConv(100L, 200L, 5L);
        assertThat(convRepo.findByCampaignId(5L)).hasSize(1);
    }

    @Test @DisplayName("findByStaffIdAndFundOwnerIdAndCampaignId")
    void byTriple() {
        persistConv(100L, 200L, 5L);
        assertThat(convRepo.findByStaffIdAndFundOwnerIdAndCampaignId(100L, 200L, 5L)).isPresent();
    }

    @Test @DisplayName("findByIdAndUserId_authorized")
    void byIdAndUser() {
        Conversation c = persistConv(100L, 200L, null);
        assertThat(convRepo.findByIdAndUserId(c.getId(), 100L)).isPresent();
        assertThat(convRepo.findByIdAndUserId(c.getId(), 999L)).isEmpty();
    }

    @Test @DisplayName("Message_findByConversationIdOrdered")
    void msgsByConv() {
        Conversation c = persistConv(100L, 200L, null);
        persistMsg(c.getId(), 100L, false);
        persistMsg(c.getId(), 200L, true);
        assertThat(msgRepo.findByConversationIdOrderByCreatedAtAsc(c.getId())).hasSize(2);
    }

    @Test @DisplayName("Message_countUnread_excludesSelf")
    void countUnread() {
        Conversation c = persistConv(100L, 200L, null);
        persistMsg(c.getId(), 200L, false); // sent by other → counted
        persistMsg(c.getId(), 100L, false); // sent by self → excluded
        persistMsg(c.getId(), 200L, true);  // already read
        assertThat(msgRepo.countUnreadMessages(c.getId(), 100L)).isEqualTo(1L);
    }
}
