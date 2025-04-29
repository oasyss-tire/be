package com.inspection.facility.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.inspection.entity.Code;
import com.inspection.entity.Company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "facilities")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public class Facility {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "facility_id")
    private Long facilityId; // 시설물번호 (PK)
    
    @Column(name = "management_number", length = 50, unique = true)
    private String managementNumber; // 사용자 입력 관리번호 (시설물 식별용)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_code", nullable = false)
    private Code brand; // 브랜드 코드 (Code 테이블 참조)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_type_code", nullable = false)
    private Code facilityType; // 시설물 항목 코드 (Code 테이블 참조)
    
    @Column(name = "serial_number")
    private String serialNumber; // 시설물 시리얼 번호
    
    @Column(name = "installation_date")
    private LocalDateTime installationDate; // 최초 설치일
    
    @Column(name = "acquisition_cost", precision = 19, scale = 2)
    private BigDecimal acquisitionCost; // 취득가액
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installation_type_code")
    private Code installationType; // 설치 유형(신품/이전) (Code 테이블 참조)
    
    @Column(name = "useful_life_months")
    private Integer usefulLifeMonths; // 사용연한(개월)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_code")
    private Code status; // 현재상태 (Code 테이블 참조)
    
    @Column(name = "current_value", precision = 19, scale = 2)
    private BigDecimal currentValue; // 현재 가치(감가상각 후)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depreciation_method_code")
    private Code depreciationMethod; // 감가상각 방법 코드 (Code 테이블 참조)
    
    @Column(name = "last_valuation_date")
    private LocalDateTime lastValuationDate; // 마지막 가치 평가일
    
    @Column(name = "warranty_end_date")
    private LocalDateTime warrantyEndDate; // 보증 만료일
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_company_id")
    private Company locationCompany; // 현재 위치 회사 (Company 테이블 참조)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_company_id")
    private Company ownerCompany; // 소유주 회사 (Company 테이블 참조)
    
    @Column(name = "created_by", length = 50)
    private String createdBy; // 작성자 ID (User 테이블 참조)
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 등록날짜
    
    @Column(name = "updated_by", length = 50)
    private String updatedBy;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 수정날짜
    
    // 폐기 관련 정보 추가
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true; // 활성 상태 여부 (폐기되면 false)
    
    @Column(name = "discard_reason", length = 500)
    private String discardReason; // 폐기 사유
    
    @Column(name = "discarded_at")
    private LocalDateTime discardedAt; // 폐기 일시
    
    @Column(name = "discarded_by", length = 50)
    private String discardedBy; // 폐기 처리자 ID
}