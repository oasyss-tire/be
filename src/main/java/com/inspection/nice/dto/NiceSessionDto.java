package com.inspection.nice.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * NICE 본인인증 세션 정보를 저장하는 DTO 클래스
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NiceSessionDto implements Serializable {
    
    private static final long serialVersionUID = 1L;

    // 세션 ID (클라이언트에 전달됨)
    private String sessionId;
    
    // 대칭키 (AES)
    private String symmetricKey;
    
    // 초기화 벡터 (IV)
    private String iv;
    
    // 암호화 토큰 관련 정보
    private String tokenVersionId;
    private String tokenVal;
    private String siteCode;
    
    // 요청 정보
    private String reqDtim;
    private String reqNo;
    
    // 무결성 체크 관련 정보
    private String originalString;  // reqNo + reqDtim + tokenVal 원본 문자열
    private String hashBase64;      // originalString의 SHA-256 해시를 Base64로 인코딩한 값
    private String hmacKey;         // 무결성 키 (hashBase64에서 추출)
    
    // 세션 생성 시간 - JSON 직렬화 문제 방지를 위해 무시
    @JsonIgnore
    private LocalDateTime createdAt;
    
    // 세션 생성 시간 문자열
    private String createdAtStr;
    
    /**
     * 세션 생성 시간 정보를 현재 시간으로 설정하고 문자열도 함께 생성
     */
    public static class NiceSessionDtoBuilder {
        private LocalDateTime createdAt = LocalDateTime.now();
        private String createdAtStr = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
    
    /**
     * 세션 생성 시간 정보를 문자열로 반환
     */
    public String getCreatedAtStr() {
        return createdAtStr != null ? createdAtStr : 
            (createdAt != null ? createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
    }
} 