package com.inspection.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.inspection.entity.ContractParticipant;
import com.inspection.entity.ParticipantTemplateMapping;
import com.inspection.entity.ParticipantToken;
import com.inspection.enums.NotificationType;
import com.inspection.util.EncryptionUtil;

import lombok.Getter;
import lombok.Setter;

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
    
    // 상태 코드 정보 추가
    private String statusCodeId;
    private String statusName;
    private java.util.Map<String, String> statusAttributes;
    
    // 승인 관련 정보 추가
    private boolean approved;
    private LocalDateTime approvedAt;
    private String approvalComment;
    private String rejectionReason;
    
    // 템플릿별 PDF 정보
    private List<TemplatePdfInfo> templatePdfs;

    // User 정보 (로그인된 사용자와 연결된 경우)
    private Long userId;
    private String userLoginId;
    
    // 토큰 정보 추가
    private List<TokenInfo> tokens;

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

        // User 정보 설정
        if (participant.getUser() != null) {
            this.userId = participant.getUser().getId();
            this.userLoginId = participant.getUser().getUserId();
        }
        
        // 토큰 정보는 별도로 설정해야 함 (서비스에서 처리)
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
        private boolean hasPassword;  // 비밀번호 존재 여부
        
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
            this.hasPassword = mapping.getDocumentPassword() != null && !mapping.getDocumentPassword().isEmpty();
        }
    }
    
    @Getter @Setter
    public static class TokenInfo {
        private Long id;
        private String tokenValue;
        private String tokenType;
        private LocalDateTime expiresAt;
        private boolean active;
        
        public TokenInfo(ParticipantToken token) {
            this.id = token.getId();
            this.tokenValue = token.getTokenValue();
            this.tokenType = token.getTokenType().name();
            this.expiresAt = token.getExpiresAt();
            this.active = token.isActive();
        }
    }
} 