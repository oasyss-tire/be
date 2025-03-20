package com.inspection.dto;

import lombok.Getter;
import lombok.Setter;
import com.inspection.entity.ContractParticipant;
import com.inspection.entity.ParticipantTemplateMapping;
import com.inspection.enums.NotificationType;
import com.inspection.util.EncryptionUtil;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter @Setter
public class ParticipantDTO {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private NotificationType notifyType;
    private boolean signed;
    private LocalDateTime signedAt;
    
    // 대표 PDF ID (첫 번째 템플릿의 PDF)
    private String pdfId;
    private String signedPdfId;
    
    // 템플릿별 PDF 정보
    private List<TemplatePdfInfo> templatePdfs;

    public ParticipantDTO(ContractParticipant participant, EncryptionUtil encryptionUtil) {
        this.id = participant.getId();
        this.name = participant.getName();
        this.email = encryptionUtil.decrypt(participant.getEmail());
        this.phoneNumber = encryptionUtil.decrypt(participant.getPhoneNumber());
        this.notifyType = participant.getNotifyType();
        this.signed = participant.isSigned();
        this.signedAt = participant.getSignedAt();
        this.pdfId = participant.getPdfId();
        this.signedPdfId = participant.getSignedPdfId();
        
        // 템플릿별 PDF 정보 설정
        if (participant.getTemplateMappings() != null && !participant.getTemplateMappings().isEmpty()) {
            this.templatePdfs = participant.getTemplateMappings().stream()
                .map(mapping -> new TemplatePdfInfo(mapping))
                .collect(Collectors.toList());
        }
    }
    
    @Getter @Setter
    public static class TemplatePdfInfo {
        private Long mappingId;
        private Long templateId;
        private String templateName;
        private String pdfId;
        private String signedPdfId;
        private boolean signed;
        private LocalDateTime signedAt;
        
        public TemplatePdfInfo(ParticipantTemplateMapping mapping) {
            if (mapping.getContractTemplateMapping() != null) {
                this.mappingId = mapping.getContractTemplateMapping().getId();
                this.templateId = mapping.getContractTemplateMapping().getTemplate().getId();
                this.templateName = mapping.getContractTemplateMapping().getTemplate().getTemplateName();
            }
            this.pdfId = mapping.getPdfId();
            this.signedPdfId = mapping.getSignedPdfId();
            this.signed = mapping.isSigned();
            this.signedAt = mapping.getSignedAt();
        }
    }
} 