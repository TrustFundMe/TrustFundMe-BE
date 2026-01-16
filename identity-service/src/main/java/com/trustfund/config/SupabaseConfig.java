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
            throw new IllegalStateException("Thiếu cấu hình SUPABASE_URL (supabase.url). Hãy set env SUPABASE_URL=https://<project>.supabase.co");
        }
        String base = supabaseUrl.trim();
        if (!(base.startsWith("http://") || base.startsWith("https://"))) {
            throw new IllegalStateException("SUPABASE_URL phải có scheme http/https, ví dụ: https://<project>.supabase.co");
        }
        // bỏ dấu / cuối nếu có để tránh // khi nối chuỗi
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    private String requiredBucket() {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalStateException("Thiếu cấu hình SUPABASE_STORAGE_BUCKET (supabase.storage.bucket). Hãy set env SUPABASE_STORAGE_BUCKET=<bucket_name>");
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

