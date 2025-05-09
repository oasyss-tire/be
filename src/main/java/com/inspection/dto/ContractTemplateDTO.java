package com.inspection.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import com.inspection.entity.ContractTemplate;
import com.inspection.entity.ContractPdfField;
import com.fasterxml.jackson.annotation.JsonProperty;

@Slf4j
@Getter @Setter
public class ContractTemplateDTO {
    private Long id;
    private String templateName;
    private String description;
    private String originalPdfId;
    private String processedPdfId;
    private LocalDateTime createdAt;
    @JsonProperty("isActive")
    private boolean isActive;
    private List<ContractPdfFieldDTO> fields;  // 필드 정보 포함

    public ContractTemplateDTO(ContractTemplate template) {
        this.id = template.getId();
        this.templateName = template.getTemplateName();
        this.description = template.getDescription();
        this.originalPdfId = template.getOriginalPdfId();
        this.processedPdfId = template.getProcessedPdfId();
        this.createdAt = template.getCreatedAt();
        this.isActive = template.isActive();
        
        // 필드 정보 변환 로직 개선
        List<ContractPdfField> templateFields = template.getFields();
        if (templateFields != null && !templateFields.isEmpty()) {
            log.info("템플릿 #{} - {} 개의 필드 정보 변환 중", template.getId(), templateFields.size());
            this.fields = templateFields.stream()
                .map(ContractPdfFieldDTO::new)
                .collect(Collectors.toList());
        } else {
            log.warn("템플릿 #{} - 필드 정보가 없거나 로드되지 않았습니다", template.getId());
            this.fields = new ArrayList<>();
        }
    }
} 