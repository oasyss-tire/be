package com.inspection.facility.dto;

import java.time.LocalDateTime;

import com.inspection.facility.entity.Depreciation;

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
public class DepreciationDTO {
    
    private Long depreciationId;
    private Long facilityId;
    private String facilityName;
    private String managementNumber;      // 시설물 관리번호
    private String brandCode;             // 브랜드 코드
    private String brandCodeName;         // 브랜드명
    private LocalDateTime depreciationDate;
    private Double previousValue;
    private Double depreciationAmount;
    private Double currentValue;
    private String depreciationTypeCode;
    private String depreciationTypeName;
    private String depreciationMethodCode;
    private String depreciationMethodName;
    private Integer fiscalYear;
    private Integer fiscalMonth;
    private Long locationCompanyId;       // 시설물 위치 회사 ID
    private String locationCompanyName;   // 시설물 위치 회사명
    private LocalDateTime createdAt;
    private Long createdById;
    private String createdByName;
    private String notes;
    
    /**
     * Depreciation 엔티티를 DTO로 변환
     */
    public static DepreciationDTO fromEntity(Depreciation entity) {
        return DepreciationDTO.builder()
                .depreciationId(entity.getDepreciationId())
                .facilityId(entity.getFacility().getFacilityId())
                .facilityName(entity.getFacility().getFacilityType().getCodeName())
                .managementNumber(entity.getFacility().getManagementNumber())
                .brandCode(entity.getFacility().getBrand() != null ? entity.getFacility().getBrand().getCodeId() : null)
                .brandCodeName(entity.getFacility().getBrand() != null ? entity.getFacility().getBrand().getCodeName() : null)
                .depreciationDate(entity.getDepreciationDate())
                .previousValue(entity.getPreviousValue())
                .depreciationAmount(entity.getDepreciationAmount())
                .currentValue(entity.getCurrentValue())
                .depreciationTypeCode(entity.getDepreciationType().getCodeId())
                .depreciationTypeName(entity.getDepreciationType().getCodeName())
                .depreciationMethodCode(entity.getDepreciationMethod().getCodeId())
                .depreciationMethodName(entity.getDepreciationMethod().getCodeName())
                .fiscalYear(entity.getFiscalYear())
                .fiscalMonth(entity.getFiscalMonth())
                .locationCompanyId(entity.getFacility().getLocationCompany() != null ? 
                        entity.getFacility().getLocationCompany().getId() : null)
                .locationCompanyName(entity.getFacility().getLocationCompany() != null ? 
                        entity.getFacility().getLocationCompany().getStoreName() : null)
                .createdAt(entity.getCreatedAt())
                .createdById(entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null)
                .createdByName(entity.getCreatedBy() != null ? entity.getCreatedBy().getUserName() : null)
                .notes(entity.getNotes())
                .build();
    }
} 