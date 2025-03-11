package com.inspection.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import com.inspection.entity.ContractTemplate;
import com.fasterxml.jackson.annotation.JsonProperty;


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
        this.fields = template.getFields().stream()
            .map(ContractPdfFieldDTO::new)
            .collect(Collectors.toList());
    }
} 