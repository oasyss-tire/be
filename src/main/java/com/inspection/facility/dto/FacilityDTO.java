package com.inspection.facility.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.inspection.entity.Code;
import com.inspection.facility.entity.Facility;

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
public class FacilityDTO {
    
    private Long facilityId;            // 시설물번호
    private String managementNumber;    // 관리번호 (시설물 식별용)
    private String brandCode;           // 브랜드 코드
    private String brandName;           // 브랜드명
    private String facilityTypeCode;    // 시설물 항목 코드
    private String facilityTypeName;    // 시설물 항목명
    private String serialNumber;        // 시설물 시리얼 번호
    private LocalDateTime installationDate; // 최초 설치일
    private BigDecimal acquisitionCost; // 취득가액
    private String installationTypeCode;// 설치 유형 코드
    private String installationTypeName;// 설치 유형명
    private Integer usefulLifeMonths;   // 사용연한(개월)
    private String statusCode;          // 현재상태 코드
    private String statusName;          // 현재상태명
    private BigDecimal currentValue;    // 현재 가치(감가상각 후)
    private String depreciationMethodCode; // 감가상각 방법 코드
    private String depreciationMethodName; // 감가상각 방법명
    private LocalDateTime lastValuationDate;   // 마지막 가치 평가일
    private LocalDateTime warrantyEndDate;     // 보증 만료일
    
    // 위치 회사 정보
    private Long locationCompanyId;        // 위치 회사 ID
    private String locationStoreNumber;    // 위치 회사 점번
    private String locationStoreName;      // 위치 회사명
    private String locationAddress;        // 위치 회사 주소
    
    // 소유 회사 정보
    private Long ownerCompanyId;           // 소유 회사 ID
    private String ownerStoreNumber;       // 소유 회사 점번
    private String ownerStoreName;         // 소유 회사명
    
    private String createdBy;              // 작성자 ID
    private LocalDateTime createdAt;       // 등록날짜
    private String updatedBy;              // 수정자 ID
    private LocalDateTime updatedAt;       // 수정날짜
    
    // AS 접수 요청 상태 관련 정보
    private boolean hasActiveServiceRequest;  // 활성화된 AS 접수 요청이 있는지
    private Long latestServiceRequestId;      // 최신 AS 접수 요청 ID
    private String serviceRequestNumber;      // 최신 AS 접수 요청 번호
    private String serviceStatusCode;         // 최신 AS 접수 상태 코드
    private String serviceStatusName;         // 최신 AS 접수 상태명
    private LocalDateTime serviceRequestDate;     // 최신 AS 접수 요청일
    private LocalDateTime expectedCompletionDate; // 예상 완료일
    private LocalDateTime completionDate;         // 완료일
    private Boolean isReceived;              // 접수 완료 여부
    private Boolean isCompleted;             // 완료 여부
    private Long managerId;                  // 처리자 ID
    private String managerName;              // 처리자 이름
    
    // Entity -> DTO 변환
    public static FacilityDTO fromEntity(Facility facility) {
        if (facility == null) return null;
        
        return FacilityDTO.builder()
                .facilityId(facility.getFacilityId())
                .managementNumber(facility.getManagementNumber())
                .brandCode(facility.getBrand() != null ? facility.getBrand().getCodeId() : null)
                .brandName(facility.getBrand() != null ? facility.getBrand().getCodeName() : null)
                .facilityTypeCode(facility.getFacilityType() != null ? facility.getFacilityType().getCodeId() : null)
                .facilityTypeName(facility.getFacilityType() != null ? facility.getFacilityType().getCodeName() : null)
                .serialNumber(facility.getSerialNumber())
                .installationDate(facility.getInstallationDate())
                .acquisitionCost(facility.getAcquisitionCost())
                .installationTypeCode(facility.getInstallationType() != null ? facility.getInstallationType().getCodeId() : null)
                .installationTypeName(facility.getInstallationType() != null ? facility.getInstallationType().getCodeName() : null)
                .usefulLifeMonths(facility.getUsefulLifeMonths())
                .statusCode(facility.getStatus() != null ? facility.getStatus().getCodeId() : null)
                .statusName(facility.getStatus() != null ? facility.getStatus().getCodeName() : null)
                .currentValue(facility.getCurrentValue())
                .depreciationMethodCode(facility.getDepreciationMethod() != null ? facility.getDepreciationMethod().getCodeId() : null)
                .depreciationMethodName(facility.getDepreciationMethod() != null ? facility.getDepreciationMethod().getCodeName() : null)
                .lastValuationDate(facility.getLastValuationDate())
                .warrantyEndDate(facility.getWarrantyEndDate())
                .locationCompanyId(facility.getLocationCompany() != null ? facility.getLocationCompany().getId() : null)
                .ownerCompanyId(facility.getOwnerCompany() != null ? facility.getOwnerCompany().getId() : null)
                .createdBy(facility.getCreatedBy())
                .createdAt(facility.getCreatedAt())
                .updatedBy(facility.getUpdatedBy())
                .updatedAt(facility.getUpdatedAt())
                .build();
    }
} 