package com.inspection.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateContractRequest {
    private String title;                   // 계약 제목
    private String description;             // 계약 설명/비고
    private LocalDate startDate;        // 계약 시작일
    private LocalDate expiryDate;       // 계약 만료일
    private LocalDate insuranceStartDate;   // 하자보증증권 보험시작일
    private LocalDate insuranceEndDate;     // 하자보증증권 보험종료일
    
    // 단일 템플릿 대신 다중 템플릿 지원
    // private Long templateId;             // 선택된 템플릿 ID
    private List<Long> templateIds;         // 선택된 템플릿 ID 목록
    
    private Long companyId;                 // 계약 회사 ID
    private String createdBy;               // 계약 작성자 (이름) - 기존 호환성 유지
    private Long userId;                    // 계약 작성자 사용자 ID (User 엔티티 연결용)
    private String department;              // 담당 부서
    private String contractTypeCodeId;        // 계약 구분 코드 ID (신규/재계약/1회차/2회차 등)
    private List<CreateParticipantRequest> participants;  // 서명 참여자 목록
    
    private List<String> documentCodeIds;   // 참여자가 업로드해야 할 문서 코드 목록
    
    private Long trusteeHistoryId;     // 수탁자 이력 ID (추가)
}

