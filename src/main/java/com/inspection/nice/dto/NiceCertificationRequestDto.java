package com.inspection.nice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * NICE 본인인증 요청 데이터 DTO
 * 클라이언트에서 전달받은 요청 데이터를 담습니다.
 */
@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NiceCertificationRequestDto {

    // 세션 ID 또는 요청 번호
    private String sessionId;
    
    // 인증 결과를 받을 URL
    private String returnUrl;
    
    // 결과를 받을 HTTP 메소드 타입 (GET/POST)
    private String methodType;
    
    // 추가 파라미터 (선택)
    private String extraData;
} 