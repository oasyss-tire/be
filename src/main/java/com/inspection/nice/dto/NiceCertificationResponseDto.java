package com.inspection.nice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * NICE 본인인증 요청 응답 데이터 DTO
 * NICE 표준창 호출에 필요한 데이터를 포함합니다.
 */
@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NiceCertificationResponseDto {

    // 요청 번호
    private String requestNo;
    
    // 토큰 버전 ID (NICE 표준창 호출 시 필요)
    private String tokenVersionId;
    
    // 암호화된 데이터 (NICE 표준창 호출 시 필요)
    private String encData;
    
    // 무결성 체크값 (NICE 표준창 호출 시 필요)
    private String integrityValue;
    
    // 결과 상태 코드
    private String resultCode;
    
    // 결과 메시지
    private String resultMessage;
    
    // 세션에 저장할 키 값 (암호화 키)
    private String key;
    
    // 세션에 저장할 IV 값 (초기화 벡터)
    private String iv;
    
    // 세션에 저장할 HMAC 키 값
    private String hmacKey;
} 