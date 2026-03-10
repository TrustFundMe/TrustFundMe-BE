package com.trustfund.service;

import com.trustfund.config.SupabaseConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupabaseStorageService {

    private final SupabaseConfig supabaseConfig;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public StoredFile uploadFile(MultipartFile file) throws IOException, InterruptedException {
        String uniqueFilename = UUID.randomUUID() + "-" + file.getOriginalFilename();
        String encodedFilename = URLEncoder.encode(uniqueFilename, StandardCharsets.UTF_8).replace("+", "%20");
        String uploadUrl = supabaseConfig.getUploadUrl() + "/" + encodedFilename;

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        System.out.println("[Supabase] Uploading to: " + uploadUrl);
        System.out.println("[Supabase] Content-Type: " + contentType + ", Size: " + file.getSize() + " bytes");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseConfig.getSupabaseKey())
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header("x-upsert", "true")  // allow overwrite if file exists
                .PUT(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                .build();

        // Read body for better error reporting
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int statusCode = response.statusCode();

        if (statusCode >= 200 && statusCode < 300) {
            String publicUrl = supabaseConfig.getPublicUrl(encodedFilename);
            System.out.println("[Supabase] Upload OK → " + publicUrl);
            return new StoredFile(uniqueFilename, encodedFilename, publicUrl);
        } else {
            String body = response.body();
            System.err.println("[Supabase] Upload FAILED! Status: " + statusCode);
            System.err.println("[Supabase] URL: " + uploadUrl);
            System.err.println("[Supabase] Response body: " + body);
            System.err.println("[Supabase] Key (first 30 chars): " + supabaseConfig.getSupabaseKey().substring(0, Math.min(30, supabaseConfig.getSupabaseKey().length())));
            
            HttpStatus status = HttpStatus.resolve(statusCode);
            if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
            
            throw new ResponseStatusException(status, 
                "Supabase upload failed [" + statusCode + "]: " + body);
        }
    }

    public void deleteFile(String storedName) throws IOException, InterruptedException {
        String encodedFilename = URLEncoder.encode(storedName, StandardCharsets.UTF_8);
        String deleteUrl = supabaseConfig.getDeleteUrl(encodedFilename);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(deleteUrl))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseConfig.getSupabaseKey())
                .DELETE()
                .build();

        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        int statusCode = response.statusCode();

        if (statusCode < 200 || statusCode >= 300) {
            System.err.println("Supabase delete failed! Status: " + statusCode + ", URL: " + deleteUrl);
            throw new RuntimeException("Supabase delete failed with status code: " + statusCode);
        }
    }

    public void deleteFileByPublicUrl(String publicUrl) throws IOException, InterruptedException {
        if (publicUrl == null || publicUrl.isBlank()) {
            return;
        }
        int idx = publicUrl.lastIndexOf('/');
        if (idx == -1 || idx == publicUrl.length() - 1) {
            throw new IllegalArgumentException("Invalid Supabase public URL: " + publicUrl);
        }
        String encodedFilename = publicUrl.substring(idx + 1);
        String storedName = java.net.URLDecoder.decode(encodedFilename, StandardCharsets.UTF_8);
        deleteFile(storedName);
    }

    public record StoredFile(String storedName, String encodedName, String publicUrl) {
    }
}
