package com.inspection.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

import com.inspection.entity.Contract;
import com.inspection.entity.ContractTemplateMapping;
import com.inspection.util.EncryptionUtil;
import com.inspection.entity.Code;
import com.inspection.entity.CodeAttribute;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ContractDTO {
    private Long id;
    private String title;
    private Integer progressRate;
    private LocalDateTime createdAt;
    private LocalDateTime startDate;
    private LocalDateTime expiryDate;
    private LocalDateTime completedAt;
    private LocalDateTime lastModifiedAt;
    private String createdBy;
    private String department;
    private String contractNumber;
    private boolean active;
    
    // 계약 상태 관련 필드 추가
    private String statusCodeId;
    private String statusName;
    private Map<String, String> statusAttributes;
    
    // 관리자 승인 관련 필드 추가
    private LocalDateTime approvedAt;
    private String approvedBy;
    private String rejectionReason;
    
    // 단일 템플릿 정보 대신 다중 템플릿 정보
    // private Long templateId;
    // private String templateName;
    private List<TemplateInfoDTO> templates;
    
    private Long companyId;
    private String companyName;
    private String storeCode;
    private List<ParticipantDTO> participants;

    public ContractDTO(Contract contract, EncryptionUtil encryptionUtil) {
        this.id = contract.getId();
        this.title = contract.getTitle();
        this.progressRate = contract.getProgressRate();
        this.createdAt = contract.getCreatedAt();
        this.startDate = contract.getStartDate();
        this.expiryDate = contract.getExpiryDate();
        this.completedAt = contract.getCompletedAt();
        this.lastModifiedAt = contract.getLastModifiedAt();
        this.createdBy = contract.getCreatedBy();
        this.department = contract.getDepartment();
        this.contractNumber = contract.getContractNumber();
        this.active = contract.isActive();
        
        // 계약 상태 정보 설정
        Code statusCode = contract.getStatusCode();
        if (statusCode != null) {
            this.statusCodeId = statusCode.getCodeId();
            this.statusName = statusCode.getCodeName();
            
            // 상태 속성 정보 설정
            this.statusAttributes = new HashMap<>();
            if (statusCode.getAttributes() != null) {
                for (CodeAttribute attr : statusCode.getAttributes()) {
                    this.statusAttributes.put(attr.getAttributeKey(), attr.getAttributeValue());
                }
            }
        }
        
        // 관리자 승인 관련 정보 설정
        this.approvedAt = contract.getApprovedAt();
        this.approvedBy = contract.getApprovedBy();
        this.rejectionReason = contract.getRejectionReason();
        
        // 템플릿 정보 변환
        this.templates = contract.getTemplateMappings().stream()
            .map(mapping -> new TemplateInfoDTO(mapping))
            .collect(Collectors.toList());
        
        if (contract.getCompany() != null) {
            this.companyId = contract.getCompany().getId();
            this.companyName = contract.getCompany().getStoreName();
            this.storeCode = contract.getCompany().getStoreCode();
        }
        
        this.participants = contract.getParticipants().stream()
            .map(participant -> new ParticipantDTO(participant, encryptionUtil))
            .collect(Collectors.toList());
    }
    
    @Getter @Setter
    public static class TemplateInfoDTO {
        private Long id;              // 매핑 ID
        private Long templateId;      // 템플릿 ID
        private String templateName;  // 템플릿 이름
        private String processedPdfId; // 처리된 PDF ID
        private Integer sortOrder;     // 정렬 순서
        
        public TemplateInfoDTO(ContractTemplateMapping mapping) {
            this.id = mapping.getId();
            this.templateId = mapping.getTemplate().getId();
            this.templateName = mapping.getTemplate().getTemplateName();
            this.processedPdfId = mapping.getProcessedPdfId();
            this.sortOrder = mapping.getSortOrder();
        }
    }
} 