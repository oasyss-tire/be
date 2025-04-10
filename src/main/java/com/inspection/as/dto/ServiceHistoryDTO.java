package com.inspection.as.dto;

import java.time.LocalDateTime;

import com.inspection.as.entity.ServiceHistory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceHistoryDTO {
    
    private Long serviceHistoryId;
    private Long serviceRequestId;
    private String serviceRequestNumber;
    private LocalDateTime actionDate;
    private String actionTypeCode;
    private String actionTypeName;
    private String actionDescription;
    private String partsUsed;
    private Double partsCost;
    private Double laborCost;
    private Double totalCost;
    private Long performedById;
    private String performedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * ServiceHistory 엔티티를 DTO로 변환
     */
    public static ServiceHistoryDTO fromEntity(ServiceHistory entity) {
        return ServiceHistoryDTO.builder()
                .serviceHistoryId(entity.getServiceHistoryId())
                .serviceRequestId(entity.getServiceRequest().getServiceRequestId())
                .serviceRequestNumber(entity.getServiceRequest().getRequestNumber())
                .actionDate(entity.getActionDate())
                .actionTypeCode(entity.getActionType().getCodeId())
                .actionTypeName(entity.getActionType().getCodeName())
                .actionDescription(entity.getActionDescription())
                .partsUsed(entity.getPartsUsed())
                .partsCost(entity.getPartsCost())
                .laborCost(entity.getLaborCost())
                .totalCost(calculateTotalCost(entity.getPartsCost(), entity.getLaborCost()))
                .performedById(entity.getPerformedBy() != null ? entity.getPerformedBy().getId() : null)
                .performedByName(entity.getPerformedBy() != null ? entity.getPerformedBy().getUserName() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
    /**
     * 총 비용 계산
     */
    private static Double calculateTotalCost(Double partsCost, Double laborCost) {
        double pCost = partsCost != null ? partsCost : 0.0;
        double lCost = laborCost != null ? laborCost : 0.0;
        return pCost + lCost;
    }
} 