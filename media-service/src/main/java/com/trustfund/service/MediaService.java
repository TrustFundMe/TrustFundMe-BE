package com.trustfund.service;

import com.trustfund.model.request.MediaUploadRequest;
import com.trustfund.model.response.MediaFileResponse;
import java.io.IOException;
import java.util.List;

public interface MediaService {
    MediaFileResponse uploadMedia(MediaUploadRequest request) throws IOException, InterruptedException;

    MediaFileResponse getMediaById(Long id);

    List<MediaFileResponse> getMediaByPostId(Long postId);

    List<MediaFileResponse> getMediaByCampaignId(Long campaignId);

    void deleteMedia(Long id) throws IOException, InterruptedException;
}
