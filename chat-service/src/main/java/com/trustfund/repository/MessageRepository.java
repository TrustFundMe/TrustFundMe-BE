package com.trustfund.repository;

import com.trustfund.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

       java.util.List<Message> findByConversationIdOrderByCreatedAtDesc(Long conversationId);

       @Query("SELECT COUNT(m) FROM Message m WHERE m.conversationId = :conversationId " +
                     "AND m.senderId != :userId AND m.isRead = false")
       Long countUnreadMessages(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
}
