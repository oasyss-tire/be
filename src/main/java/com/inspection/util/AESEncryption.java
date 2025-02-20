package com.inspection.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AESEncryption {
    
    @Value("${encryption.key}")
    private String encryptionKey;  // 32자리 키
    
    private SecretKeySpec createSecretKey() {
        try {
            // 키를 32바이트(256비트)로 패딩
            byte[] key = new byte[32];
            byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, key.length));
            return new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            throw new RuntimeException("키 생성 실패", e);
        }
    }
    
    // 암호화
    public String encrypt(String value) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, createSecretKey());
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("암호화 실패: " + e.getMessage(), e);
        }
    }
    
    // 복호화
    public String decrypt(String encrypted) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, createSecretKey());
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encrypted));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("복호화 실패: " + e.getMessage(), e);
        }
    }
} 