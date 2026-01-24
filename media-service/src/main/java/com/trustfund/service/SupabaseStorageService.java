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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupabaseStorageService {

    private final SupabaseConfig supabaseConfig;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public StoredFile uploadFile(MultipartFile file) throws IOException, InterruptedException {
        String uniqueFilename = UUID.randomUUID() + "-" + file.getOriginalFilename();
        String encodedFilename = URLEncoder.encode(uniqueFilename, StandardCharsets.UTF_8);
        String uploadUrl = supabaseConfig.getUploadUrl() + "/" + encodedFilename;

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseConfig.getSupabaseKey())
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                .build();

        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        int statusCode = response.statusCode();

        if (statusCode >= 200 && statusCode < 300) {
            String publicUrl = supabaseConfig.getPublicUrl(encodedFilename);
            return new StoredFile(uniqueFilename, encodedFilename, publicUrl);
        } else {
            throw new RuntimeException("Supabase upload failed with status code: " + statusCode);
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

    public record StoredFile(String storedName, String encodedName, String publicUrl) {}
}

