package com.trustfund.service;

import com.trustfund.model.Media;
import com.trustfund.model.enums.MediaType;
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

        // 2. Auto-detect mediaType if not provided
        MediaType finalMediaType = request.getMediaType();
        if (finalMediaType == null) {
            finalMediaType = detectMediaType(request.getFile().getContentType());
        }

        // 3. Save metadata to DB
        Media media = Media.builder()
                .postId(request.getPostId())
                .campaignId(request.getCampaignId())
                .conversationId(request.getConversationId())
                .expenditureId(request.getExpenditureId())
                .expenditureItemId(request.getExpenditureItemId())
                .mediaType(finalMediaType)
                .url(storedFile.publicUrl())
                .description(request.getDescription())
                .fileName(request.getFile().getOriginalFilename())
                .contentType(request.getFile().getContentType())
                .sizeBytes(request.getFile().getSize())
                .build();

        System.out.println(">>> MediaServiceImpl: Saving media - Type: " + finalMediaType + ", URL Length: "
                + (storedFile.publicUrl() != null ? storedFile.publicUrl().length() : 0));
        
        try {
            Media savedMedia = mediaRepository.save(media);
            System.out.println(">>> MediaServiceImpl: DB Save successful, ID: " + savedMedia.getId());
            return mapToResponse(savedMedia);
        } catch (Exception e) {
            System.err.println(">>> MediaServiceImpl: DB Save FAILED!");
            System.err.println(">>> Error type: " + e.getClass().getName());
            System.err.println(">>> Error message: " + e.getMessage());
            
            // Check for specific database errors if possible
            if (e.getCause() != null) {
                System.err.println(">>> Root cause: " + e.getCause().getMessage());
            }
            
            throw new RuntimeException("Lỗi lưu thông tin media vào database: " + e.getMessage(), e);
        }
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
    public List<MediaFileResponse> getMediaByConversationId(Long conversationId) {
        return mediaRepository.findByConversationId(conversationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<MediaFileResponse> getMediaByExpenditureId(Long expenditureId) {
        return mediaRepository.findByExpenditureId(expenditureId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<MediaFileResponse> getMediaByExpenditureItemId(Long expenditureItemId) {
        return mediaRepository.findByExpenditureItemId(expenditureItemId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public MediaFileResponse getFirstImageByCampaignId(Long campaignId) {
        return mediaRepository.findFirstByCampaignIdAndMediaTypeOrderByCreatedAtAsc(campaignId, MediaType.PHOTO)
                .map(this::mapToResponse)
                .orElse(null);
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
        if (request.getConversationId() != null)
            media.setConversationId(request.getConversationId());
        if (request.getExpenditureId() != null)
            media.setExpenditureId(request.getExpenditureId());
        if (request.getExpenditureItemId() != null)
            media.setExpenditureItemId(request.getExpenditureItemId());
        if (request.getDescription() != null)
            media.setDescription(request.getDescription());

        Media updatedMedia = mediaRepository.save(media);
        return mapToResponse(updatedMedia);
    }

    @Override
    @Transactional
    public MediaFileResponse registerMedia(com.trustfund.model.request.RegisterMediaRequest request) {
        Media media = Media.builder()
                .url(request.getUrl())
                .mediaType(request.getMediaType() != null ? request.getMediaType() : MediaType.PHOTO)
                .postId(request.getPostId())
                .campaignId(request.getCampaignId())
                .conversationId(request.getConversationId())
                .expenditureId(request.getExpenditureId())
                .expenditureItemId(request.getExpenditureItemId())
                .description(request.getDescription())
                .fileName(request.getFileName())
                .contentType(request.getContentType())
                .sizeBytes(request.getSizeBytes())
                .build();

        Media savedMedia = mediaRepository.save(media);
        return mapToResponse(savedMedia);
    }

    @Override
    public MediaFileResponse updateMediaStatus(Long id, String status) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Media not found with id: " + id));

        media.setStatus(status);

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

    private MediaType detectMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.FILE; // Default for unknown types
        }

        String type = contentType.toLowerCase();

        // Image types
        if (type.startsWith("image/")) {
            return MediaType.PHOTO;
        }

        // Video types
        if (type.startsWith("video/")) {
            return MediaType.VIDEO;
        }

        // Document types
        if (type.contains("pdf") ||
                type.contains("document") ||
                type.contains("word") ||
                type.contains("excel") ||
                type.contains("spreadsheet") ||
                type.contains("text") ||
                type.contains("msword") ||
                type.contains("ms-excel") ||
                type.contains("officedocument")) {
            return MediaType.FILE;
        }

        // Default fallback
        return MediaType.FILE;
    }

    private MediaFileResponse mapToResponse(Media media) {
        return MediaFileResponse.builder()
                .id(media.getId())
                .postId(media.getPostId())
                .campaignId(media.getCampaignId())
                .conversationId(media.getConversationId())
                .expenditureId(media.getExpenditureId())
                .expenditureItemId(media.getExpenditureItemId())
                .mediaType(media.getMediaType())
                .url(media.getUrl())
                .description(media.getDescription())
                .status(media.getStatus())
                .fileName(media.getFileName())
                .contentType(media.getContentType())
                .sizeBytes(media.getSizeBytes())
                .createdAt(media.getCreatedAt())
                .build();
    }
}
