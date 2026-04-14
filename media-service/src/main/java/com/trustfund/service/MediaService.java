package com.trustfund.service;

import com.trustfund.model.request.MediaUploadRequest;
import com.trustfund.model.response.MediaFileResponse;
import java.io.IOException;
import java.util.List;

public interface MediaService {
    MediaFileResponse uploadMedia(MediaUploadRequest request) throws IOException, InterruptedException;

    MediaFileResponse updateMediaFile(Long id, com.trustfund.model.request.MediaUploadRequest request)
            throws IOException, InterruptedException;

    MediaFileResponse getMediaById(Long id);

    List<MediaFileResponse> getMediaByPostId(Long postId);

    List<MediaFileResponse> getMediaByCampaignId(Long campaignId);

    List<MediaFileResponse> getMediaByConversationId(Long conversationId);

    List<MediaFileResponse> getMediaByExpenditureId(Long expenditureId);

    List<MediaFileResponse> getMediaByExpenditureItemId(Long expenditureItemId);

    MediaFileResponse getFirstImageByCampaignId(Long campaignId);

    MediaFileResponse updateMedia(Long id, com.trustfund.model.request.UpdateMediaRequest request);

    MediaFileResponse registerMedia(com.trustfund.model.request.RegisterMediaRequest request);

    MediaFileResponse updateMediaStatus(Long id, String status);

    void deleteMedia(Long id) throws IOException, InterruptedException;

    /**
     * Set postId = null for the given media record (unlink from post without
     * deleting the file).
     */
    void unlinkFromPost(Long id);
}
