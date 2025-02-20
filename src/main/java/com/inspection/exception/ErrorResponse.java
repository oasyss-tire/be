package com.inspection.exception;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

/* 에러 응답을 위한 DTO 클래스
예시) {"code": "404", "message": "점검 기록을 찾을 수 없습니다"}
 */
public class ErrorResponse {
    private String code;
    private String message;
} 