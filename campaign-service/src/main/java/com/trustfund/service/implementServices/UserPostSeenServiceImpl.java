package com.trustfund.service.implementServices;

import com.trustfund.model.UserPostSeen;
import com.trustfund.repository.FeedPostRepository;
import com.trustfund.repository.UserPostSeenRepository;
import com.trustfund.service.interfaceServices.UserPostSeenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserPostSeenServiceImpl implements UserPostSeenService {

    private final UserPostSeenRepository userPostSeenRepository;
    private final FeedPostRepository feedPostRepository;

    @Override
    @Transactional
    public boolean markSeen(Long userId, Long postId) {
        if (userId == null || postId == null) return false;
        if (userPostSeenRepository.existsByUserIdAndPostId(userId, postId)) return false;
        UserPostSeen seen = UserPostSeen.builder()
                .userId(userId)
                .postId(postId)
                .seenAt(LocalDateTime.now())
                .build();
        userPostSeenRepository.save(seen);
        feedPostRepository.incrementViewCount(postId);
        return true;
    }

    @Override
    @Transactional
    public void markSeenBatch(Long userId, List<Long> postIds) {
        if (userId == null || postIds == null || postIds.isEmpty()) return;
        Set<Long> alreadySeen = new HashSet<>(
            userPostSeenRepository.findByUserIdAndPostIdIn(userId, postIds)
                .stream()
                .map(UserPostSeen::getPostId)
                .toList()
        );
        List<UserPostSeen> toSave = postIds.stream()
                .filter(pid -> !alreadySeen.contains(pid))
                .map(pid -> UserPostSeen.builder()
                        .userId(userId)
                        .postId(pid)
                        .seenAt(LocalDateTime.now())
                        .build())
                .toList();
        userPostSeenRepository.saveAll(toSave);
        toSave.forEach(s -> feedPostRepository.incrementViewCount(s.getPostId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> getSeenPostIds(Long userId) {
        if (userId == null) return Set.of();
        return new HashSet<>(
            userPostSeenRepository.findByUserId(userId)
                .stream()
                .map(UserPostSeen::getPostId)
                .toList()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSeen(Long userId, Long postId) {
        if (userId == null || postId == null) return false;
        return userPostSeenRepository.existsByUserIdAndPostId(userId, postId);
    }
}
