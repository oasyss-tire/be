package com.inspection.dto;

import com.inspection.entity.ParticipantPdfField;
import com.inspection.util.EncryptionUtil;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
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
    
    // 형식 코드 (추가)
    private String formatCodeId;
    
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
        this.formatCodeId = field.getFormat() != null ? field.getFormat().getCodeId() : null;
    }
    
    /**
     * ParticipantPdfField 엔티티로부터 DTO 생성 (민감 정보 복호화 포함)
     */
    public CorrectionFieldDTO(ParticipantPdfField field, EncryptionUtil encryptionUtil) {
        this(field); // 기본 생성자 호출
        
        // 민감 정보 필드인 경우 복호화
        if (field.getFormat() != null) {
            String formatCode = field.getFormat().getCodeId();
            if ("001004_0001".equals(formatCode) || "001004_0002".equals(formatCode)) {
                try {
                    if (field.getValue() != null && !field.getValue().isEmpty()) {
                        this.value = encryptionUtil.decrypt(field.getValue());
                        log.info("재서명 필드 DTO 변환 시 민감정보 복호화 처리: {} ({})", field.getFieldName(), formatCode);
                    }
                } catch (Exception e) {
                    log.error("민감정보 복호화 중 오류 발생: {} - {}", field.getFieldName(), e.getMessage());
                    // 복호화 실패 시 암호화된 값 그대로 사용
                }
            }
        }
    }
} 