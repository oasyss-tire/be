package com.inspection.dto;

import java.time.LocalDateTime;

import com.inspection.entity.ParticipantPdfField;
import com.inspection.util.EncryptionUtil;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
public class ParticipantPdfFieldDTO {
    private Long id;
    private Long originalFieldId;
    private Long participantId;
    private String pdfId;
    private String fieldId;
    private String fieldName;
    private String type;
    private Double relativeX;
    private Double relativeY;
    private Double relativeWidth;
    private Double relativeHeight;
    private Integer page;
    private String value;
    private String confirmText;
    private String description;
    private Boolean needsCorrection;
    private String correctionComment;
    private LocalDateTime correctionRequestedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String formatCodeId;

    public ParticipantPdfFieldDTO(ParticipantPdfField field) {
        this.id = field.getId();
        this.originalFieldId = field.getOriginalField() != null ? field.getOriginalField().getId() : null;
        this.participantId = field.getParticipant() != null ? field.getParticipant().getId() : null;
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
        this.confirmText = field.getConfirmText();
        this.description = field.getDescription();
        this.needsCorrection = field.getNeedsCorrection();
        this.correctionComment = field.getCorrectionComment();
        this.correctionRequestedAt = field.getCorrectionRequestedAt();
        this.createdAt = field.getCreatedAt();
        this.updatedAt = field.getUpdatedAt();
        this.formatCodeId = field.getFormat() != null ? field.getFormat().getCodeId() : null;
    }

    public ParticipantPdfFieldDTO(ParticipantPdfField field, EncryptionUtil encryptionUtil) {
        this(field);
        
        if (field.getFormat() != null) {
            String formatCode = field.getFormat().getCodeId();
            if ("001004_0001".equals(formatCode) || "001004_0002".equals(formatCode)) {
                try {
                    if (field.getValue() != null && !field.getValue().isEmpty()) {
                        this.value = encryptionUtil.decrypt(field.getValue());
                        log.info("DTO 변환 시 민감정보 복호화 처리: {} ({})", field.getFieldName(), formatCode);
                    }
                } catch (Exception e) {
                    log.error("민감정보 복호화 중 오류 발생: {} - {}", field.getFieldName(), e.getMessage());
                }
            }
        }
    }
} 