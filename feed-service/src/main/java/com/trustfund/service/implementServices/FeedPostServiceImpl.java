package com.trustfund.service.implementServices;

import com.trustfund.model.FeedPost;
import com.trustfund.model.request.CreateFeedPostRequest;
import com.trustfund.model.response.FeedPostResponse;
import com.trustfund.repository.FeedPostRepository;
import com.trustfund.service.interfaceServices.FeedPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedPostServiceImpl implements FeedPostService {

    private final FeedPostRepository feedPostRepository;

    @Override
    public FeedPostResponse create(CreateFeedPostRequest request, Long authorId) {
        FeedPost feedPost = FeedPost.builder()
                .budgetId(request.getBudgetId())
                .authorId(authorId)
                .type(request.getType())
                .visibility(request.getVisibility())
                .title(request.getTitle())
                .content(request.getContent())
                .status(request.getStatus() == null || request.getStatus().isBlank() ? "DRAFT" : request.getStatus())
                .build();

        FeedPost saved = feedPostRepository.save(feedPost);
        return toResponse(saved);
    }

    private FeedPostResponse toResponse(FeedPost entity) {
        return FeedPostResponse.builder()
                .id(entity.getId())
                .budgetId(entity.getBudgetId())
                .authorId(entity.getAuthorId())
                .type(entity.getType())
                .visibility(entity.getVisibility())
                .title(entity.getTitle())
                .content(entity.getContent())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
