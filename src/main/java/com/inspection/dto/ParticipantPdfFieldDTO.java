package com.inspection.dto;

import java.time.LocalDateTime;

import com.inspection.entity.ParticipantPdfField;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private Boolean needsCorrection;
    private String correctionComment;
    private LocalDateTime correctionRequestedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
        this.needsCorrection = field.getNeedsCorrection();
        this.correctionComment = field.getCorrectionComment();
        this.correctionRequestedAt = field.getCorrectionRequestedAt();
        this.createdAt = field.getCreatedAt();
        this.updatedAt = field.getUpdatedAt();
    }
} 