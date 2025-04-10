package com.inspection.facility.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class FacilityUpdateRequest {
    
    private String brandCode;             // 브랜드 코드 (Code 테이블 참조)
    
    private String facilityTypeCode;      // 시설물 항목 코드 (Code 테이블 참조)
    
    private String modelNumber;           // 품목 모델번호 (5600A, 5600X 등)
    
    private String managementNumber;      // 관리번호 (시설물 식별용)
    
    private String serialNumber;          // 시설물 시리얼 번호
    
    private LocalDateTime installationDate;   // 최초 설치일
    
    @PositiveOrZero(message = "취득가액은 0 이상이어야 합니다")
    private BigDecimal acquisitionCost;   // 취득가액
    
    private String installationTypeCode;  // 설치 유형 코드 (Code 테이블 참조)
    
    private Integer usefulLifeMonths;     // 사용연한(개월)
    
    private String statusCode;            // 현재상태 코드 (Code 테이블 참조)
    
    @PositiveOrZero(message = "현재 가치는 0 이상이어야 합니다")
    private BigDecimal currentValue;      // 현재 가치(감가상각 후)
    
    private String depreciationMethodCode; // 감가상각 방법 코드 (Code 테이블 참조)
    
    private LocalDateTime lastValuationDate;    // 마지막 가치 평가일
    
    private LocalDateTime warrantyEndDate;      // 보증 만료일
    
    private Long locationCompanyId;         // 위치 회사 ID (Company 테이블 참조)
    
    private Long ownerCompanyId;            // 소유 회사 ID (Company 테이블 참조)
} 