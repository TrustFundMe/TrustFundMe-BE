package com.trustfund.service.interfaceServices;

import java.util.List;
import java.util.Set;

public interface UserPostSeenService {
    /** Mark a post as seen by user. Returns true if this was a new view (incremented), false if already seen. */
    boolean markSeen(Long userId, Long postId);

    /** Batch mark — mark multiple posts as seen. Idempotent. */
    void markSeenBatch(Long userId, List<Long> postIds);

    /** Get set of post IDs the user has seen. */
    Set<Long> getSeenPostIds(Long userId);

    /** Check if user has seen a specific post. */
    boolean isSeen(Long userId, Long postId);
}
