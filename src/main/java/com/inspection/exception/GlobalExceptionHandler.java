package com.inspection.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
/* 전역 예외 처리를 담당하는 클래스 
/inspection/{id}/detail 에 사용 */
public class GlobalExceptionHandler {

    // GlobalExceptionHandler에서 사용됨
    @ExceptionHandler(InspectionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleInspectionNotFound(InspectionNotFoundException e) {
        ErrorResponse response = new ErrorResponse();
        response.setCode("404");
        response.setMessage(e.getMessage());
        return ResponseEntity.status(404).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception e) {
        ErrorResponse response = new ErrorResponse();
        response.setCode("500");
        response.setMessage("서버 오류가 발생했습니다");
        return ResponseEntity.status(500).body(response);
    }
} 