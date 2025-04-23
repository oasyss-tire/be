package com.inspection.dto;

import java.time.LocalDateTime;
import java.time.LocalDate;
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
    private LocalDate startDate;
    private LocalDate expiryDate;
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
    private String storeNumber;      // 점번 (001~999) 추가
    private String storeTelNumber;   // 매장 전화번호 (055-123-4567) 추가
    private LocalDate insuranceStartDate; // 하자보증증권 보험시작일 추가
    private LocalDate insuranceEndDate;   // 하자보증증권 보험종료일 추가
    
    // 수탁자 이력 정보 추가
    private Long trusteeHistoryId;    // 수탁자 이력 ID
    private String trusteeName;       // 수탁자 이름
    private String trusteeCode;       // 수탁자 코드
    private String representativeName; // 대표자 이름
    private String businessNumber;    // 사업자 번호
    
    private List<ParticipantDTO> participants;

    public ContractDTO(Contract contract, EncryptionUtil encryptionUtil) {
        this.id = contract.getId();
        this.title = contract.getTitle();
        this.progressRate = contract.getProgressRate();
        this.createdAt = contract.getCreatedAt();
        
        // LocalDateTime -> LocalDate 변환 (만약 LocalDateTime인 경우)
        if (contract.getStartDate() != null) {
            this.startDate = contract.getStartDate();
        }
        if (contract.getExpiryDate() != null) {
            this.expiryDate = contract.getExpiryDate();
        }
        
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
            this.companyName = contract.getCompany().getCompanyName();
            this.storeCode = contract.getCompany().getStoreCode();
            this.storeNumber = contract.getCompany().getStoreNumber();
            this.storeTelNumber = contract.getCompany().getStoreTelNumber();
            
            // Company와 Contract에서 LocalDate 타입 그대로 사용
            this.insuranceStartDate = contract.getCompany().getInsuranceStartDate();
            this.insuranceEndDate = contract.getCompany().getInsuranceEndDate();
        }
        
        // 수탁자 이력 정보 설정
        if (contract.getTrusteeHistory() != null) {
            this.trusteeHistoryId = contract.getTrusteeHistory().getId();
            this.trusteeName = contract.getTrusteeHistory().getTrustee();
            this.trusteeCode = contract.getTrusteeHistory().getTrusteeCode();
            this.representativeName = contract.getTrusteeHistory().getRepresentativeName();
            this.businessNumber = contract.getTrusteeHistory().getBusinessNumber();
        } else {
            // 수탁자 이력이 없는 경우 Contract의 편의 메서드 사용
            this.trusteeName = contract.getTrusteeName();
            this.trusteeCode = contract.getTrusteeCode();
            this.representativeName = contract.getRepresentativeName();
            this.businessNumber = contract.getBusinessNumber();
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