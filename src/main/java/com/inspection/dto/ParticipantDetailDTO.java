package com.inspection.dto;

import com.inspection.entity.ContractParticipant;
import com.inspection.entity.ParticipantTemplateMapping;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.inspection.enums.NotificationType;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class ParticipantDetailDTO {
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
    
    // 승인 관련 필드 추가
    private boolean approved;
    private LocalDateTime approvedAt;
    private String approvalComment;
    private String rejectionReason;
    
    // 상태 코드 정보 추가
    private String statusCodeId;
    private String statusName;
    private java.util.Map<String, String> statusAttributes;
    
    // 템플릿별 PDF 정보
    private List<TemplatePdfInfo> templatePdfs;
    
    // 기존 생성자 수정
    public ParticipantDetailDTO(Long id, String name, String email, String phoneNumber, 
                                NotificationType notifyType, boolean signed, LocalDateTime signedAt,
                                String pdfId, String signedPdfId) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.notifyType = notifyType;
        this.signed = signed;
        this.signedAt = signedAt;
        this.pdfId = pdfId;
        this.signedPdfId = signedPdfId;
        // 승인 관련 필드는 기본값으로 설정 (생성자에서 전달되지 않음)
        this.approved = false;
        this.approvedAt = null;
        this.approvalComment = null;
        this.rejectionReason = null;
    }
    
    // 새로운 생성자 추가
    public ParticipantDetailDTO(ContractParticipant participant, String decryptedEmail, String decryptedPhone) {
        this.id = participant.getId();
        this.name = participant.getName();
        this.email = decryptedEmail;
        this.phoneNumber = decryptedPhone;
        this.notifyType = participant.getNotifyType();
        this.signed = participant.isSigned();
        this.signedAt = participant.getSignedAt();
        this.pdfId = participant.getPdfId();
        this.signedPdfId = participant.getSignedPdfId();
        
        // 승인 관련 필드 설정
        this.approved = participant.isApproved();
        this.approvedAt = participant.getApprovedAt();
        this.approvalComment = participant.getApprovalComment();
        this.rejectionReason = participant.getRejectionReason();
        
        // 상태 코드 정보 설정
        if (participant.getStatusCode() != null) {
            this.statusCodeId = participant.getStatusCode().getCodeId();
            this.statusName = participant.getStatusCode().getCodeName();
            
            // 상태 코드 속성 설정
            if (participant.getStatusCode().getAttributes() != null 
                && !participant.getStatusCode().getAttributes().isEmpty()) {
                this.statusAttributes = new java.util.HashMap<>();
                participant.getStatusCode().getAttributes().forEach(attr -> 
                    this.statusAttributes.put(attr.getAttributeKey(), attr.getAttributeValue()));
            }
        }
        
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