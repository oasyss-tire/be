package com.inspection.exception;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 오류 응답을 표현하는 클래스
 * 클라이언트에게 일관된 형식의 오류 정보를 제공합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private LocalDateTime timestamp; // 오류 발생 시간
    private int status;              // HTTP 상태 코드
    private String error;            // 오류 유형
    private String message;          // 오류 메시지
    private String path;             // 요청 경로
} 