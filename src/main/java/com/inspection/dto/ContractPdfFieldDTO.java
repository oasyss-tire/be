package com.inspection.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import com.inspection.entity.ContractPdfField;

@Getter
@Setter
@NoArgsConstructor
public class ContractPdfFieldDTO {
    private String id;
    private String fieldId;
    private String pdfId;
    private String fieldName;
    private String type;        // signature, text, checkbox 등
    private Double relativeX;
    private Double relativeY;
    private Double relativeWidth;
    private Double relativeHeight;
    private Integer page;
    private String value;

    public ContractPdfFieldDTO(ContractPdfField field) {
        this.id = field.getFieldId();
        this.fieldId = field.getFieldId();
        this.pdfId = field.getPdfId();
        this.fieldName = field.getFieldName();
        this.type = field.getType();
        this.relativeX = field.getRelativeX();
        this.relativeY = field.getRelativeY();
        this.relativeWidth = field.getRelativeWidth();
        this.relativeHeight = field.getRelativeHeight();
        this.page = field.getPage();
        this.value = field.getValue();
    }
} 