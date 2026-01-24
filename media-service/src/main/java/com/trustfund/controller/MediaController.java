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
                .mediaType(mediaType != null ? mediaType : MediaType.PHOTO)
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

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete media by ID", description = "Delete media from storage and database")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws IOException, InterruptedException {
        mediaService.deleteMedia(id);
        return ResponseEntity.noContent().build();
    }
}
