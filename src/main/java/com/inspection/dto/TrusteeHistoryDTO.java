package com.inspection.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 수탁자 이력 정보 DTO
 * 화면 표시용으로 추가 정보 포함
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrusteeHistoryDTO {
    private Long id;
    private String trustee;           // 수탁자명
    private String trusteeCode;       // 수탁코드
    private String representativeName; // 대표자명
    private String businessNumber;    // 사업자번호
    private LocalDate startDate;      // 계약 시작일
    private LocalDate endDate;        // 계약 종료일
    private LocalDate insuranceStartDate; // 보험 시작일
    private LocalDate insuranceEndDate;   // 보험 종료일
    private boolean active;           // 활성 여부
    
    // UI 표시용 추가 정보
    private String statusLabel;       // 상태 레이블 (예: "현재 계약 중", "예정된 계약", "종료된 계약")
    private String statusType;        // 상태 유형 (예: "active", "pending", "expired")
} 