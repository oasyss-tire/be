package com.inspection.facility.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.inspection.entity.Code;
import com.inspection.entity.User;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "facility_depreciation")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Depreciation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "depreciation_id")
    private Long depreciationId;  // 감가상각 고유 번호
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;  // 시설물 정보
    
    @Column(name = "depreciation_date", nullable = false)
    private LocalDateTime depreciationDate;  // 감가상각 일자   
    
    @Column(name = "previous_value", nullable = false)
    private Double previousValue;  // 이전 장부 가액
    
    @Column(name = "depreciation_amount", nullable = false)
    private Double depreciationAmount;  // 감가상각 금액
    
    @Column(name = "current_value", nullable = false)
    private Double currentValue;  // 현재 장부 가액
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depreciation_type_code", nullable = false)
    private Code depreciationType; // 감가상각 유형 (일마감, 월마감)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depreciation_method_code", nullable = false)
    private Code depreciationMethod; // 감가상각 방법 (정액법, 정률법)
    
    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;  // 회계연도
    
    @Column(name = "fiscal_month", nullable = false)
    private Integer fiscalMonth;  // 회계월
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;  // 생성일자
    
    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;  // 생성자
    
    @Column(name = "notes", length = 500)
    private String notes;  // 비고
} 