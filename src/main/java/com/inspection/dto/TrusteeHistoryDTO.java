package com.inspection.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    private String phoneNumber;       // 휴대폰번호
    private String email;             // 이메일
    private LocalDate startDate;      // 계약 시작일
    private LocalDate endDate;        // 계약 종료일
    private LocalDate insuranceStartDate; // 보험 시작일
    private LocalDate insuranceEndDate;   // 보험 종료일
    private boolean active;           // 활성 여부
    private Long userId;              // 연결된 사용자 ID
    
    // 계약 및 템플릿 정보
    private Long contractId;          // 계약 ID
    private String contractNumber;    // 계약 번호
    private List<TemplateInfo> templates = new ArrayList<>(); // 계약에 사용된 템플릿 목록
    
    // UI 표시용 추가 정보
    private String statusLabel;       // 상태 레이블 (예: "현재 계약 중", "예정된 계약", "종료된 계약")
    private String statusType;        // 상태 유형 (예: "active", "pending", "expired")
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateInfo {
        private Long id;              // 템플릿 ID
        private String templateName;  // 템플릿 이름
        private String processedPdfId; // 처리된 PDF ID
        private Integer sortOrder;    // 템플릿 순서
    }
} 