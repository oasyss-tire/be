package com.inspection.enums;

public enum ContractStatus {
    DRAFT,          // 임시저장
    PENDING,        // 서명 대기
    SIGNING,        // 서명 진행 중
    COMPLETED,      // 완료
    REJECTED,       // 거절됨
    EXPIRED,        // 만료됨
    CANCELED        // 취소됨
} 