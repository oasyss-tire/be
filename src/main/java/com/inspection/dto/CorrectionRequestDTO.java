package com.inspection.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CorrectionRequestDTO {
    // 재서명이 필요한 필드 ID 목록
    private List<Long> fieldIds;
    
    // 재서명 요청 사유 (전체 또는 기본 사유)
    private String correctionComment;
    
    // 필드별 재서명 요청 사유 (선택적)
    private List<FieldCorrectionDTO> fieldCorrections;
    
    // 이메일 발송 여부
    private boolean sendEmail = true;
    
    // 내부 클래스: 필드별 재서명 요청 정보
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldCorrectionDTO {
        private Long fieldId;
        private String comment;
    }
} 