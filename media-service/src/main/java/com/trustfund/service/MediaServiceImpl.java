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
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final MediaRepository mediaRepository;
    private final SupabaseStorageService supabaseStorageService;
    private final RestTemplate restTemplate;

    @Value("${app.identity-service.url:http://localhost:8081}")
    private String identityServiceUrl;

    @Override
    @Transactional
    public MediaFileResponse uploadMedia(MediaUploadRequest request) throws IOException, InterruptedException {
        // 1. Compute Hash BEFORE upload
        byte[] fileBytes = request.getFile().getBytes();
        String fileHash = computeSHA256(fileBytes);
        log.info(">>> MediaServiceImpl: Computed SHA-256: {}", fileHash);

        // 2. Upload to Supabase
        SupabaseStorageService.StoredFile storedFile = supabaseStorageService.uploadFile(request.getFile());

        // 3. Auto-detect mediaType if not provided
        MediaType finalMediaType = request.getMediaType();
        if (finalMediaType == null) {
            finalMediaType = detectMediaType(request.getFile().getContentType());
        }

        // 4. Save metadata to DB
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

        try {
            Media savedMedia = mediaRepository.save(media);
            log.info(">>> MediaServiceImpl: DB Save successful, ID: {}", savedMedia.getId());
            
            // [AUDIT] Create audit log for evidence upload
            createAuditLogForMedia(savedMedia, fileHash);
            
            return mapToResponse(savedMedia);
        } catch (Exception e) {
            log.error(">>> MediaServiceImpl: DB Save FAILED! {}", e.getMessage());
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
        return mediaRepository.findByExpenditureIdAndStatusNot(expenditureId, "DELETED").stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<MediaFileResponse> getMediaByExpenditureItemId(Long expenditureItemId) {
        return mediaRepository.findByExpenditureItemIdAndStatusNot(expenditureItemId, "DELETED").stream()
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
        log.info(">>> MediaServiceImpl: Registering media - URL: {}, Type: {}, PostID: {}, CampaignID: {}", 
                request.getUrl(), request.getMediaType(), request.getPostId(), request.getCampaignId());
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
    public void unlinkFromPost(Long id) {
        mediaRepository.findById(id).ifPresent(media -> {
            media.setPostId(null);
            mediaRepository.save(media);
        });
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

    @Override
    @Transactional
    public MediaFileResponse updateMediaFile(Long id, MediaUploadRequest request)
            throws IOException, InterruptedException {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Media not found with id: " + id));

        // 1. Delete old file from Supabase
        if (media.getUrl() != null) {
            supabaseStorageService.deleteFileByPublicUrl(media.getUrl());
        }

        // 2. Upload new file to Supabase
        SupabaseStorageService.StoredFile storedFile = supabaseStorageService.uploadFile(request.getFile());

        // 3. Update metadata
        media.setUrl(storedFile.publicUrl());
        media.setFileName(request.getFile().getOriginalFilename());
        media.setContentType(request.getFile().getContentType());
        media.setSizeBytes(request.getFile().getSize());

        if (request.getDescription() != null) {
            media.setDescription(request.getDescription());
        }

        Media savedMedia = mediaRepository.save(media);
        return mapToResponse(savedMedia);
    }

    private MediaType detectMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.FILE;
        }

        String type = contentType.toLowerCase();
        log.info(">>> detectMediaType: detecting type for content-type: {}", type);

        if (type.startsWith("image/")) {
            return MediaType.PHOTO;
        }
        if (type.startsWith("video/")) {
            return MediaType.VIDEO;
        }

        if (type.contains("excel") || 
            type.contains("spreadsheet") || 
            type.contains("csv") || 
            type.contains("ms-excel") || 
            type.contains("officedocument.spreadsheetml")) {
            return MediaType.EXCEL;
        }

        if (type.contains("pdf") ||
            type.contains("document") ||
            type.contains("word") ||
            type.contains("msword") ||
            type.contains("text") ||
            type.contains("officedocument.wordprocessingml")) {
            return MediaType.DOCUMENT;
        }

        return MediaType.FILE;
    }

    private String computeSHA256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 hashing failed", e);
        }
    }

    private void createAuditLogForMedia(Media media, String fileHash) {
        try {
            log.info("➔ [AUDIT] Creating audit log for media: {}", media.getId());
            
            Map<String, Object> snapshotMap = new java.util.HashMap<>();
            snapshotMap.put("mediaId", media.getId());
            snapshotMap.put("fileHash", fileHash);
            snapshotMap.put("fileName", media.getFileName());
            snapshotMap.put("url", media.getUrl());
            snapshotMap.put("campaignId", media.getCampaignId());
            snapshotMap.put("expenditureId", media.getExpenditureId());

            String snapshot = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(snapshotMap);
            
            Map<String, Object> auditRequest = new java.util.HashMap<>();
            auditRequest.put("entityType", "MEDIA_EVIDENCE");
            auditRequest.put("entityId", media.getId());
            auditRequest.put("action", "EVIDENCE_UPLOADED");
            auditRequest.put("dataSnapshot", snapshot);
            auditRequest.put("actorId", 0);
            auditRequest.put("actorName", "System");
            
            String auditUrl = identityServiceUrl + "/api/audit";
            restTemplate.postForObject(auditUrl, auditRequest, Map.class);
            log.info("✅ [AUDIT] Audit log for media {} created successfully.", media.getId());
        } catch (Exception e) {
            log.error("❌ [AUDIT] Failed to create audit log for media {}: {}", media.getId(), e.getMessage());
        }
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
