package com.trustfund.service;

import com.trustfund.model.Media;
import com.trustfund.model.request.MediaUploadRequest;
import com.trustfund.model.response.MediaFileResponse;
import com.trustfund.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final MediaRepository mediaRepository;
    private final SupabaseStorageService supabaseStorageService;

    @Override
    @Transactional
    public MediaFileResponse uploadMedia(MediaUploadRequest request) throws IOException, InterruptedException {
        // 1. Upload to Supabase
        SupabaseStorageService.StoredFile storedFile = supabaseStorageService.uploadFile(request.getFile());

        // 2. Save metadata to DB
        Media media = Media.builder()
                .postId(request.getPostId())
                .campaignId(request.getCampaignId())
                .mediaType(request.getMediaType())
                .url(storedFile.publicUrl())
                .description(request.getDescription())
                .fileName(request.getFile().getOriginalFilename())
                .contentType(request.getFile().getContentType())
                .sizeBytes(request.getFile().getSize())
                .build();

        Media savedMedia = mediaRepository.save(media);

        return mapToResponse(savedMedia);
    }

    @Override
    public MediaFileResponse getMediaById(Long id) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Media not found with id: " + id));
        return mapToResponse(media);
    }

    @Override
    public List<MediaFileResponse> getMediaByPostId(Long postId) {
        return mediaRepository.findByPostId(postId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<MediaFileResponse> getMediaByCampaignId(Long campaignId) {
        return mediaRepository.findByCampaignId(campaignId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MediaFileResponse updateMedia(Long id, com.trustfund.model.request.UpdateMediaRequest request) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Media not found with id: " + id));

        if (request.getPostId() != null)
            media.setPostId(request.getPostId());
        if (request.getCampaignId() != null)
            media.setCampaignId(request.getCampaignId());
        if (request.getDescription() != null)
            media.setDescription(request.getDescription());

        Media updatedMedia = mediaRepository.save(media);
        return mapToResponse(updatedMedia);
    }

    @Override
    @Transactional
    public void deleteMedia(Long id) throws IOException, InterruptedException {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Media not found with id: " + id));

        // 1. Delete from Supabase
        supabaseStorageService.deleteFileByPublicUrl(media.getUrl());

        // 2. Delete from DB
        mediaRepository.delete(media);
    }

    private MediaFileResponse mapToResponse(Media media) {
        return MediaFileResponse.builder()
                .id(media.getId())
                .postId(media.getPostId())
                .campaignId(media.getCampaignId())
                .mediaType(media.getMediaType())
                .url(media.getUrl())
                .description(media.getDescription())
                .fileName(media.getFileName())
                .contentType(media.getContentType())
                .sizeBytes(media.getSizeBytes())
                .createdAt(media.getCreatedAt())
                .build();
    }
}
