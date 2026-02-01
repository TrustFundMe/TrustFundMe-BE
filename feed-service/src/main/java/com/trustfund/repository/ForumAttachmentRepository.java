package com.trustfund.repository;

import com.trustfund.model.ForumAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumAttachmentRepository extends JpaRepository<ForumAttachment, Long> {

    List<ForumAttachment> findByPostIdOrderByDisplayOrderAsc(Long postId);

    void deleteByPostId(Long postId);
}
