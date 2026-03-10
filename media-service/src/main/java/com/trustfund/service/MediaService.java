package com.trustfund.service;

import com.trustfund.model.request.MediaUploadRequest;
import com.trustfund.model.request.RegisterMediaRequest;
import com.trustfund.model.response.MediaFileResponse;
import java.io.IOException;
import java.util.List;

public interface MediaService {
    MediaFileResponse uploadMedia(MediaUploadRequest request) throws IOException, InterruptedException;

    /** Create a media DB record for a file already uploaded externally (no file upload). */
    MediaFileResponse registerMedia(RegisterMediaRequest request);

    MediaFileResponse getMediaById(Long id);

    List<MediaFileResponse> getMediaByPostId(Long postId);

    List<MediaFileResponse> getMediaByCampaignId(Long campaignId);

    List<MediaFileResponse> getMediaByConversationId(Long conversationId);

    MediaFileResponse getFirstImageByCampaignId(Long campaignId);

    MediaFileResponse updateMedia(Long id, com.trustfund.model.request.UpdateMediaRequest request);

    MediaFileResponse updateMediaStatus(Long id, String status);

    void deleteMedia(Long id) throws IOException, InterruptedException;
}
