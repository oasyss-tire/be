package com.inspection.dto;

import com.inspection.entity.ContractPdfField;
import com.inspection.util.EncryptionUtil;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
    private String confirmText;
    private String description;
    private String formatCodeId; // 입력 형식 코드 ID (전화번호, 주민번호 등)

    public ContractPdfFieldDTO(ContractPdfField field) {
        if (field == null) {
            log.error("ContractPdfFieldDTO 생성 시 필드가 null입니다");
            return;
        }
        
        try {
            // ID 설정 - 내부 ID와 fieldId 명확히 구분
            this.id = String.valueOf(field.getId()); // 내부 DB ID를 문자열로 변환
            this.fieldId = field.getFieldId(); // 프론트엔드에서 생성한 필드 ID
            
            // 나머지 필드 설정
            this.pdfId = field.getPdfId();
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
            this.formatCodeId = field.getFormat() != null ? field.getFormat().getCodeId() : null;
            
            log.debug("필드 DTO 변환 성공: id={}, fieldId={}, fieldName={}", this.id, this.fieldId, this.fieldName);
        } catch (Exception e) {
            log.error("필드 DTO 변환 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    // 암호화된 민감 정보를 복호화하는 생성자 추가
    public ContractPdfFieldDTO(ContractPdfField field, EncryptionUtil encryptionUtil) {
        this(field); // 기존 생성자 호출
        
        // 민감 정보 필드인 경우 복호화
        if (field != null && field.getFormat() != null) {
            String formatCode = field.getFormat().getCodeId();
            if ("001004_0001".equals(formatCode) || "001004_0002".equals(formatCode)) {
                try {
                    if (field.getValue() != null && !field.getValue().isEmpty()) {
                        this.value = encryptionUtil.decrypt(field.getValue());
                        log.info("DTO 변환 시 민감정보 복호화 처리: {} ({})", field.getFieldName(), formatCode);
                    }
                } catch (Exception e) {
                    log.error("민감정보 복호화 중 오류 발생: {} - {}", field.getFieldName(), e.getMessage());
                    // 복호화 실패 시 암호화된 값 그대로 사용
                }
            }
        }
    }
} 