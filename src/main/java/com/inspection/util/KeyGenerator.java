package com.inspection.util;

import java.security.SecureRandom;
import java.util.Base64;

public class KeyGenerator {
    public static void main(String[] args) {
        // AES-256용 32바이트 키 생성
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[32];
        secureRandom.nextBytes(key);
        
        // Base64로 인코딩
        String encodedKey = Base64.getEncoder().encodeToString(key);
        System.out.println("Generated Key (AES-256): " + encodedKey);
        
        // AES-128용 16바이트 키 생성
        byte[] key128 = new byte[16];
        secureRandom.nextBytes(key128);
        String encodedKey128 = Base64.getEncoder().encodeToString(key128);
        System.out.println("Generated Key (AES-128): " + encodedKey128);
    }
} 