package com.trustfund.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class SupabaseConfig {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.storage.bucket}")
    private String bucketName;

    private String normalizedBaseUrl() {
        if (supabaseUrl == null || supabaseUrl.trim().isEmpty()) {
            throw new IllegalStateException("Missing SUPABASE_URL");
        }
        String base = supabaseUrl.trim();
        if (!(base.startsWith("http://") || base.startsWith("https://"))) {
            throw new IllegalStateException("SUPABASE_URL must include http/https scheme");
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    private String requiredBucket() {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalStateException("Missing SUPABASE_STORAGE_BUCKET");
        }
        return bucketName.trim();
    }

    public String getUploadUrl() {
        return normalizedBaseUrl() + "/storage/v1/object/" + requiredBucket();
    }

    public String getDeleteUrl(String fileName) {
        return normalizedBaseUrl() + "/storage/v1/object/" + requiredBucket() + "/" + fileName;
    }

    public String getPublicUrl(String fileName) {
        return normalizedBaseUrl() + "/storage/v1/object/public/" + requiredBucket() + "/" + fileName;
    }
}

