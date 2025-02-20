package com.inspection.exception;

/* 점검 기록을 찾을 수 없을 때 예외 처리 */
public class InspectionNotFoundException extends RuntimeException {
    public InspectionNotFoundException(Long id) {
        super("점검 기록을 찾을 수 없습니다. ID: " + id);
    }
} 