package com.inspection.nice.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * NICE 본인인증을 위한 암호화 유틸리티 클래스
 * 대칭키 생성, 암호화, 복호화, HMAC 생성 기능 제공
 */
@Slf4j
@Component
public class NiceCryptoUtil {

    // 대칭키 알고리즘 (AES)
    private static final String KEY_ALGORITHM = "AES";
    // 암호화 알고리즘 (AES-CBC-PKCS5Padding)
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    // HMAC 알고리즘 (HmacSHA256)
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    // AES 키 사이즈 (128bit)
    private static final int KEY_SIZE = 128;
    
    /**
     * 랜덤 대칭키(AES)를 생성합니다.
     * 
     * @return Base64로 인코딩된 대칭키 문자열
     */
    public String generateSymmetricKey() {
        try {
            // KeyGenerator 초기화
            KeyGenerator keyGen = KeyGenerator.getInstance(KEY_ALGORITHM);
            keyGen.init(KEY_SIZE);
            
            // 키 생성 및 Base64 인코딩
            SecretKey secretKey = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            log.error("대칭키 생성 중 오류 발생", e);
            throw new RuntimeException("대칭키 생성 실패", e);
        }
    }
    
    /**
     * 랜덤 IV(Initialization Vector)를 생성합니다.
     * 
     * @return Base64로 인코딩된 IV 문자열
     */
    public String generateIv() {
        byte[] iv = new byte[16]; // AES 블록 크기
        new SecureRandom().nextBytes(iv);
        return Base64.getEncoder().encodeToString(iv);
    }
    
    /**
     * 세션 ID를 생성합니다.
     * 
     * @return 생성된 세션 ID 문자열
     */
    public String generateSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 문자열 데이터를 대칭키로 암호화합니다.
     * 
     * @param data 암호화할 데이터
     * @param keyBase64 Base64로 인코딩된 대칭키
     * @param ivBase64 Base64로 인코딩된 IV
     * @return Base64로 인코딩된 암호화된 데이터
     */
    public String encrypt(String data, String keyBase64, String ivBase64) {
        try {
            // NICE 가이드에 맞게 구현 - 키와 IV를 직접 문자열로 사용
            byte[] keyBytes = keyBase64.getBytes("UTF-8");
            byte[] ivBytes = ivBase64.getBytes("UTF-8");
            
            log.info("암호화 상세 정보 - 키: {}, IV: {}, 원본 데이터: {}", 
                keyBase64, ivBase64, data);
            
            // 암호화 설정
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            
            // 암호화 및 Base64 인코딩 - EUC-KR 인코딩으로 변경
            byte[] encrypted = cipher.doFinal(data.getBytes("EUC-KR"));
            String encryptedBase64 = Base64.getEncoder().encodeToString(encrypted);
            
            log.info("암호화 완료 - 암호화 결과: {}", encryptedBase64);
            return encryptedBase64;
        } catch (Exception e) {
            log.error("데이터 암호화 중 오류 발생", e);
            throw new RuntimeException("데이터 암호화 실패", e);
        }
    }
    
    /**
     * 암호화된 데이터를 대칭키로 복호화합니다.
     * 
     * @param encryptedBase64 Base64로 인코딩된 암호화된 데이터
     * @param keyBase64 Base64로 인코딩된 대칭키
     * @param ivBase64 Base64로 인코딩된 IV
     * @return 복호화된 데이터 문자열
     */
    public String decrypt(String encryptedBase64, String keyBase64, String ivBase64) {
        try {
            // NICE 가이드에 맞게 구현 - 키와 IV를 직접 문자열로 사용
            byte[] keyBytes = keyBase64.getBytes("UTF-8");
            byte[] ivBytes = ivBase64.getBytes("UTF-8");
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedBase64);
            
            log.info("복호화 상세 정보 - 키: {}, IV: {}", keyBase64, ivBase64);
            
            // 복호화 설정
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            
            // 복호화 및 문자열 변환
            byte[] decrypted = cipher.doFinal(encryptedBytes);
            String decryptedStr = new String(decrypted, "EUC-KR");
            
            log.info("복호화 완료 - 결과 길이: {}", decryptedStr.length());
            return decryptedStr;
        } catch (Exception e) {
            log.error("데이터 복호화 중 오류 발생", e);
            throw new RuntimeException("데이터 복호화 실패", e);
        }
    }
    
    /**
     * HMAC 무결성 체크 값을 생성합니다.
     * 
     * @param data HMAC을 생성할 원본 데이터
     * @param keyBase64 Base64로 인코딩된 HMAC 키
     * @return Base64로 인코딩된 HMAC 값
     */
    public String generateHmac(String data, String keyBase64) {
        try {
            // NICE 가이드에 맞게 구현 - 키를 직접 문자열로 사용
            byte[] keyBytes = keyBase64.getBytes("UTF-8");
            
            log.info("HMAC 생성 - 키: {}, 데이터 길이: {}", keyBase64, data.length());
            
            // HMAC 생성
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, HMAC_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(keySpec);
            
            // HMAC 계산 및 Base64 인코딩
            byte[] hmac = mac.doFinal(data.getBytes("EUC-KR"));
            String hmacBase64 = Base64.getEncoder().encodeToString(hmac);
            
            log.info("HMAC 생성 완료 - 결과: {}", hmacBase64);
            return hmacBase64;
        } catch (Exception e) {
            log.error("HMAC 생성 중 오류 발생", e);
            throw new RuntimeException("HMAC 생성 실패", e);
        }
    }
    
    /**
     * SHA-256 해시 값을 생성합니다.
     * 
     * @param data 해시를 생성할 원본 데이터
     * @return Base64로 인코딩된 SHA-256 해시 값
     */
    public String generateSHA256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes("EUC-KR"));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("SHA-256 해시 생성 중 오류 발생", e);
            throw new RuntimeException("SHA-256 해시 생성 실패", e);
        }
    }
} 