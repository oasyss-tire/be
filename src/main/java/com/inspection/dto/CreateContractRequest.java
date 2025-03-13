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
    private Long templateId;                // 선택된 템플릿 ID
    private String createdBy;               // 계약 작성자
    private String department;              // 담당 부서
    private List<CreateParticipantRequest> participants;  // 서명 참여자 목록
}

