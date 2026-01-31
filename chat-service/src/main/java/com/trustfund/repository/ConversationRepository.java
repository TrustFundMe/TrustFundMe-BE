package com.trustfund.repository;

import com.trustfund.model.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

       @Query("SELECT c FROM Conversation c WHERE " +
                     "(c.staffId = :staffId AND c.fundOwnerId = :fundOwnerId) OR " +
                     "(c.staffId = :fundOwnerId AND c.fundOwnerId = :staffId)")
       Optional<Conversation> findByStaffIdAndFundOwnerId(@Param("staffId") Long staffId,
                     @Param("fundOwnerId") Long fundOwnerId);

       @Query("SELECT c FROM Conversation c WHERE c.staffId = :userId OR c.fundOwnerId = :userId " +
                     "ORDER BY c.lastMessageAt DESC")
       java.util.List<Conversation> findByUserId(@Param("userId") Long userId);

       @Query("SELECT c FROM Conversation c WHERE c.id = :id AND (c.staffId = :userId OR c.fundOwnerId = :userId)")
       Optional<Conversation> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
