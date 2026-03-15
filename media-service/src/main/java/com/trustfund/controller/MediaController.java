package com.trustfund.controller;

import com.trustfund.model.enums.MediaType;
import com.trustfund.model.request.MediaUploadRequest;
import com.trustfund.model.response.MediaFileResponse;
import com.trustfund.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Tag(name = "Media", description = "Upload/download and metadata management for media files")
public class MediaController {

    private final MediaService mediaService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @GetMapping("/debug/db-schema")
    public ResponseEntity<String> debugDbSchema() {
        try {
            java.util.List<java.util.Map<String, Object>> columns = jdbcTemplate.queryForList("DESCRIBE media");
            StringBuilder sb = new StringBuilder("Table: media\n");
            for (java.util.Map<String, Object> col : columns) {
                sb.append(String.format("Column: %s, Type: %s, Null: %s\n",
                        col.get("Field"), col.get("Type"), col.get("Null")));
            }
            return ResponseEntity.ok(sb.toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/debug/fix-db")
    public ResponseEntity<String> fixDb() {
        try {
            jdbcTemplate.execute("ALTER TABLE media MODIFY COLUMN media_type VARCHAR(50) NOT NULL");
            return ResponseEntity.ok("Successfully updated media_type column to VARCHAR(50)!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Fix failed: " + e.getMessage());
        }
    }

    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file with metadata", description = "Upload a file and save metadata (post, campaign, description)")
    public ResponseEntity<MediaFileResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "postId", required = false) Long postId,
            @RequestParam(name = "campaignId", required = false) Long campaignId,
            @RequestParam(name = "expenditureId", required = false) Long expenditureId,
            @RequestParam(name = "mediaType", required = false) MediaType mediaType,
            @RequestParam(name = "description", required = false) String description) throws IOException, InterruptedException {
        
        System.out.println(">>> MediaController: Uploading file: " + (file != null ? file.getOriginalFilename() : "NULL") 
                + ", size: " + (file != null ? file.getSize() : 0)
                + ", type: " + mediaType + ", campaignId: " + campaignId);

        if (file == null || file.isEmpty()) {
            System.out.println(">>> MediaController: Upload failed - File is empty");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        MediaUploadRequest request = MediaUploadRequest.builder()
                .file(file)
                .postId(postId)
                .campaignId(campaignId)
                .expenditureId(expenditureId)
                .mediaType(mediaType)
                .description(description)
                .build();

        try {
            MediaFileResponse response = mediaService.uploadMedia(request);
            System.out.println(">>> MediaController: Upload success, ID: " + response.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            System.err.println(">>> MediaController: Upload FAILED: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @PostMapping(value = "/upload/conversation", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file for conversation (Chat)", description = "Upload a file and save metadata for a specific conversation")
    public ResponseEntity<MediaFileResponse> uploadForConversation(
            @RequestParam("file") MultipartFile file,
            @RequestParam("conversationId") Long conversationId,
            @RequestParam(name = "mediaType", required = false) MediaType mediaType,
            @RequestParam(name = "description", required = false) String description) throws IOException, InterruptedException {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        MediaUploadRequest request = MediaUploadRequest.builder()
                .file(file)
                .conversationId(conversationId)
                .mediaType(mediaType)
                .description(description)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(mediaService.uploadMedia(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get media by ID", description = "Get details of a media file from database")
    public ResponseEntity<MediaFileResponse> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(mediaService.getMediaById(id));
    }

    @GetMapping("/posts/{postId}")
    @Operation(summary = "Get media by Post ID", description = "Get all media files associated with a post")
    public ResponseEntity<List<MediaFileResponse>> getByPostId(@PathVariable("postId") Long postId) {
        return ResponseEntity.ok(mediaService.getMediaByPostId(postId));
    }

    @GetMapping("/campaigns/{campaignId}")
    @Operation(summary = "Get media by Campaign ID", description = "Get all media files associated with a campaign")
    public ResponseEntity<List<MediaFileResponse>> getByCampaignId(@PathVariable("campaignId") Long campaignId) {
        return ResponseEntity.ok(mediaService.getMediaByCampaignId(campaignId));
    }

    @GetMapping("/conversations/{conversationId}")
    @Operation(summary = "Get media by Conversation ID", description = "Get all media files associated with a conversation")
    public ResponseEntity<List<MediaFileResponse>> getByConversationId(@PathVariable("conversationId") Long conversationId) {
        return ResponseEntity.ok(mediaService.getMediaByConversationId(conversationId));
    }

    @GetMapping("/campaigns/{campaignId}/first-image")
    @Operation(summary = "Get first image by Campaign ID", description = "Get the first image associated with a campaign for cover display")
    public ResponseEntity<MediaFileResponse> getFirstImageByCampaignId(@PathVariable("campaignId") Long campaignId) {
        return ResponseEntity.ok(mediaService.getFirstImageByCampaignId(campaignId));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update media metadata", description = "Update description, postId, or campaignId for a media record")
    public ResponseEntity<MediaFileResponse> update(@PathVariable("id") Long id,
            @RequestBody com.trustfund.model.request.UpdateMediaRequest request) {
        return ResponseEntity.ok(mediaService.updateMedia(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update media status", description = "Update status (APPROVED/REJECTED/PENDING) for a media record")
    public ResponseEntity<MediaFileResponse> updateStatus(@PathVariable("id") Long id,
            @RequestParam("status") String status) {
        return ResponseEntity.ok(mediaService.updateMediaStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete media by ID", description = "Delete media from storage and database")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) throws IOException, InterruptedException {
        mediaService.deleteMedia(id);
        return ResponseEntity.noContent().build();
    }
}
