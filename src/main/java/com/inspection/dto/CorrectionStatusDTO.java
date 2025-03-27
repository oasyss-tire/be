package com.inspection.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorrectionStatusDTO {
    // 참여자 ID
    private Long participantId;
    
    // 참여자 이름
    private String participantName;
    
    // 참여자 이메일
    private String participantEmail;
    
    // 계약 ID
    private Long contractId;
    
    // 계약 제목
    private String contractTitle;
    
    // 재서명 요청 시간
    private LocalDateTime correctionRequestedAt;
    
    // 재서명 완료 시간
    private LocalDateTime correctionCompletedAt;
    
    // 재서명 필요한 총 필드 수
    private int totalFields;
    
    // 재서명 완료한 필드 수
    private int completedFields;
    
    // 재서명 상태 (REQUESTED, IN_PROGRESS, COMPLETED)
    private String status;
    
    // 재서명 필요한 필드 목록
    private List<CorrectionFieldDTO> fields;
} 