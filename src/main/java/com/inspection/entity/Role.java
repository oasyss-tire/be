package com.inspection.entity;

public enum Role {
    ADMIN,              // 최고 관리자 (모든 권한)
    FINANCE_MANAGER,    // 재경부 매니저 (모든 기능 접근 가능)
    CONTRACT_MANAGER,   // 계약관리 매니저 (계약 관련 기능 + 위수탁업체/사용자 관리)
    FACILITY_MANAGER,   // 시설물관리 매니저 (시설물 관련 기능 + 위수탁업체/사용자 관리)
    AS_MANAGER,         // AS관리 매니저 (시설물에서 수불마감 제외한 기능)
    MANAGER,            // 업체담당자 (기존 MANAGER - 보존)
    USER                // 일반 사용자 (가맹점 계정)
} 