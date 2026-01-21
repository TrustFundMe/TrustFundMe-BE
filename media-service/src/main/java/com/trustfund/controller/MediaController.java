package com.trustfund.controller;

import com.trustfund.service.SupabaseStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Tag(name = "Media", description = "Upload/download media files")
public class MediaController {

    private final SupabaseStorageService supabaseStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file", description = "Upload a file to Supabase and return public URL")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) throws IOException, InterruptedException {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        SupabaseStorageService.StoredFile stored = supabaseStorageService.uploadFile(file);
        String url = stored.publicUrl();

        return ResponseEntity.status(HttpStatus.CREATED).body(url);
    }

    @DeleteMapping("/by-url")
    @Operation(summary = "Delete media by URL", description = "Delete media file from Supabase by URL (public endpoint)")
    public ResponseEntity<Void> deleteByUrl(@RequestParam("url") String url) throws IOException, InterruptedException {
        supabaseStorageService.deleteFileByPublicUrl(url);
        return ResponseEntity.noContent().build();
    }
}

