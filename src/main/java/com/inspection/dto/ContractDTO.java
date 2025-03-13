package com.inspection.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.inspection.entity.Contract;
import com.inspection.util.EncryptionUtil;

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
    private Long templateId;
    private String templateName;
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
        
        if (contract.getTemplate() != null) {
            this.templateId = contract.getTemplate().getId();
            this.templateName = contract.getTemplate().getTemplateName();
        }
        
        this.participants = contract.getParticipants().stream()
            .map(participant -> new ParticipantDTO(participant, encryptionUtil))
            .collect(Collectors.toList());
    }
} 