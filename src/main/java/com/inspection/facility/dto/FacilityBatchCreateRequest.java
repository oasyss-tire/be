package com.inspection.facility.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class FacilityBatchCreateRequest {
    
    @NotBlank(message = "브랜드 코드는 필수입니다")
    private String brandCode;            // 브랜드 코드 (Code 테이블 참조)
    
    @NotBlank(message = "시설물 유형 코드는 필수입니다")
    private String facilityTypeCode;     // 시설물 항목 코드 (Code 테이블 참조)
    
    
    private String managementNumberPrefix;  // 관리번호 접두사 (사용하지 않으면 자동 생성)
    
    @NotNull(message = "수량은 필수입니다")
    @Min(value = 1, message = "수량은 최소 1 이상이어야 합니다")
    private Integer quantity = 1;        // 생성할 시설물 수량 (기본값 1)
    
    @NotNull(message = "설치일은 필수입니다")
    private LocalDateTime installationDate;  // 최초 설치일
    
    @NotNull(message = "취득가액은 필수입니다")
    @PositiveOrZero(message = "취득가액은 0 이상이어야 합니다")
    private BigDecimal acquisitionCost;  // 취득가액
    
    private String installationTypeCode; // 설치 유형 코드 (Code 테이블 참조)
    
    private Integer usefulLifeMonths;    // 사용연한(개월)
    
    @NotBlank(message = "상태 코드는 필수입니다")
    private String statusCode;           // 현재상태 코드 (Code 테이블 참조)
    
    private BigDecimal currentValue;     // 현재 가치(감가상각 후)
    
    private String depreciationMethodCode; // 감가상각 방법 코드 (Code 테이블 참조)
    
    private LocalDateTime lastValuationDate;   // 마지막 가치 평가일
    
    private LocalDateTime warrantyEndDate;     // 보증 만료일
    
    @NotNull(message = "위치 회사 ID는 필수입니다")
    private Long locationCompanyId;      // 위치 회사 ID (Company 테이블 참조)
    
    @NotNull(message = "소유 회사 ID는 필수입니다")
    private Long ownerCompanyId;         // 소유 회사 ID (Company 테이블 참조)
} 