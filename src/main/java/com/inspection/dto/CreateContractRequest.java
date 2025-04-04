package com.inspection.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateContractRequest {
    private String title;                   // 계약 제목
    private String description;             // 계약 설명/비고
    private LocalDateTime startDate;        // 계약 시작일
    private LocalDateTime expiryDate;       // 계약 만료일
    private LocalDateTime deadlineDate;     // 서명 마감 기한
    
    // 단일 템플릿 대신 다중 템플릿 지원
    // private Long templateId;             // 선택된 템플릿 ID
    private List<Long> templateIds;         // 선택된 템플릿 ID 목록
    
    private Long companyId;                 // 계약 회사 ID
    private String createdBy;               // 계약 작성자
    private String department;              // 담당 부서
    private List<CreateParticipantRequest> participants;  // 서명 참여자 목록
    
    private List<String> documentCodeIds;   // 참여자가 업로드해야 할 문서 코드 목록
}

