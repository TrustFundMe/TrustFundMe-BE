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
            @RequestParam(required = false) Long postId,
            @RequestParam(required = false) Long campaignId,
            @RequestParam(required = false) MediaType mediaType,
            @RequestParam(required = false) String description) throws IOException, InterruptedException {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        MediaUploadRequest request = MediaUploadRequest.builder()
                .file(file)
                .postId(postId)
                .campaignId(campaignId)
                .mediaType(mediaType)
                .description(description)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(mediaService.uploadMedia(request));
    }

    @PostMapping(value = "/upload/conversation", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file for conversation (Chat)", description = "Upload a file and save metadata for a specific conversation")
    public ResponseEntity<MediaFileResponse> uploadForConversation(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long conversationId,
            @RequestParam(required = false) MediaType mediaType,
            @RequestParam(required = false) String description) throws IOException, InterruptedException {
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
    public ResponseEntity<MediaFileResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(mediaService.getMediaById(id));
    }

    @GetMapping("/posts/{postId}")
    @Operation(summary = "Get media by Post ID", description = "Get all media files associated with a post")
    public ResponseEntity<List<MediaFileResponse>> getByPostId(@PathVariable Long postId) {
        return ResponseEntity.ok(mediaService.getMediaByPostId(postId));
    }

    @GetMapping("/campaigns/{campaignId}")
    @Operation(summary = "Get media by Campaign ID", description = "Get all media files associated with a campaign")
    public ResponseEntity<List<MediaFileResponse>> getByCampaignId(@PathVariable Long campaignId) {
        return ResponseEntity.ok(mediaService.getMediaByCampaignId(campaignId));
    }

    @GetMapping("/conversations/{conversationId}")
    @Operation(summary = "Get media by Conversation ID", description = "Get all media files associated with a conversation")
    public ResponseEntity<List<MediaFileResponse>> getByConversationId(@PathVariable Long conversationId) {
        return ResponseEntity.ok(mediaService.getMediaByConversationId(conversationId));
    }

    @GetMapping("/campaigns/{campaignId}/first-image")
    @Operation(summary = "Get first image by Campaign ID", description = "Get the first image associated with a campaign for cover display")
    public ResponseEntity<MediaFileResponse> getFirstImageByCampaignId(@PathVariable Long campaignId) {
        return ResponseEntity.ok(mediaService.getFirstImageByCampaignId(campaignId));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update media metadata", description = "Update description, postId, or campaignId for a media record")
    public ResponseEntity<MediaFileResponse> update(@PathVariable Long id,
            @RequestBody com.trustfund.model.request.UpdateMediaRequest request) {
        return ResponseEntity.ok(mediaService.updateMedia(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete media by ID", description = "Delete media from storage and database")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws IOException, InterruptedException {
        mediaService.deleteMedia(id);
        return ResponseEntity.noContent().build();
    }
}
