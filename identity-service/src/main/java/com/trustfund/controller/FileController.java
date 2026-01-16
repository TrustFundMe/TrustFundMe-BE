package com.trustfund.controller;

import com.trustfund.service.implementServices.SupabaseStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "File upload/delete using Supabase Storage")
public class FileController {

    private final SupabaseStorageService supabaseStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file to Supabase Storage")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String publicUrl = supabaseStorageService.uploadFile(file);
            return ResponseEntity.status(HttpStatus.CREATED).body(publicUrl);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload failed: " + e.getMessage());
        }
    }

    @DeleteMapping
    @Operation(summary = "Delete file from Supabase Storage")
    public ResponseEntity<String> deleteFile(@RequestParam("fileName") String fileName) {
        try {
            supabaseStorageService.deleteFile(fileName);
            return ResponseEntity.ok("Deleted: " + fileName);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Delete failed: " + e.getMessage());
        }
    }
}

