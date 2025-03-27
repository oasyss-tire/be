package com.inspection.dto;

import com.inspection.entity.ParticipantPdfField;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorrectionFieldDTO {
    // 필드 ID
    private Long id;
    
    // PDF ID
    private String pdfId;
    
    // 필드 ID
    private String fieldId;
    
    // 필드 이름
    private String fieldName;
    
    // 필드 타입
    private String type;
    
    // X 좌표
    private Double relativeX;
    
    // Y 좌표
    private Double relativeY;
    
    // 너비
    private Double relativeWidth;
    
    // 높이
    private Double relativeHeight;
    
    // 페이지 번호
    private Integer page;
    
    // 현재 값
    private String value;
    
    // 수정 요청 사유
    private String correctionComment;
    
    // 수정 요청 시간
    private LocalDateTime correctionRequestedAt;
    
    // 수정 완료 여부
    private Boolean corrected;
    
    /**
     * ParticipantPdfField 엔티티로부터 DTO 생성
     */
    public CorrectionFieldDTO(ParticipantPdfField field) {
        this.id = field.getId();
        this.pdfId = field.getPdfId();
        this.fieldId = field.getFieldId();
        this.fieldName = field.getFieldName();
        this.type = field.getType();
        this.relativeX = field.getRelativeX();
        this.relativeY = field.getRelativeY();
        this.relativeWidth = field.getRelativeWidth();
        this.relativeHeight = field.getRelativeHeight();
        this.page = field.getPage();
        this.value = field.getValue();
        this.correctionComment = field.getCorrectionComment();
        this.correctionRequestedAt = field.getCorrectionRequestedAt();
        this.corrected = !field.getNeedsCorrection(); // 수정 필요 없으면 수정 완료로 간주
    }
} 