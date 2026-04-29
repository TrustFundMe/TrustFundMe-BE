package com.trustfund.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EncryptionUtils {

    private static final String ALGORITHM = "AES";
    
    @Value("${app.encryption.key:TrustFundMeKey12}") // Should be 16 chars for AES-128
    private String secretKey;

    public String encrypt(String value) {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(secretKey.getBytes("UTF-8"), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

            byte[] encrypted = cipher.doFinal(value.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception ex) {
            throw new RuntimeException("Error while encrypting: " + ex.getMessage());
        }
    }

    public String decrypt(String encrypted) {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(secretKey.getBytes("UTF-8"), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);

            byte[] original = cipher.doFinal(Base64.getDecoder().decode(encrypted));
            return new String(original);
        } catch (Exception ex) {
            throw new RuntimeException("Error while decrypting: " + ex.getMessage());
        }
    }
}
