package com.trustfund.service.implementServices;

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

    public String uploadFile(MultipartFile file) throws IOException, InterruptedException {
        String uniqueFilename = UUID.randomUUID() + "-" + file.getOriginalFilename();
        String encodedFilename = URLEncoder.encode(uniqueFilename, StandardCharsets.UTF_8);
        String uploadUrl = supabaseConfig.getUploadUrl() + "/" + encodedFilename;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseConfig.getSupabaseKey())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                .build();

        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        int statusCode = response.statusCode();

        if (statusCode >= 200 && statusCode < 300) {
            return supabaseConfig.getPublicUrl(encodedFilename);
        } else {
            throw new RuntimeException("Supabase upload failed with status code: " + statusCode);
        }
    }

    public void deleteFile(String fileName) throws IOException, InterruptedException {
        String encodedFilename = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
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
}

