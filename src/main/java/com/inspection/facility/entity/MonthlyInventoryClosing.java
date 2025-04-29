package com.inspection.facility.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.inspection.entity.Code;
import com.inspection.entity.Company;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "monthly_inventory_closings")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public class MonthlyInventoryClosing {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Integer year; // 년도
    
    @Column(nullable = false)
    private Integer month; // 월
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company; // 회사/지점
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_type_code", nullable = false)
    private Code facilityType; // 시설물 유형
    
    @Column(nullable = false)
    private Integer previousMonthQuantity = 0; // 전월 재고 수량
    
    @Column(nullable = false)
    private Integer totalInboundQuantity = 0; // 당월 총 입고 수량
    
    @Column(nullable = false)
    private Integer totalOutboundQuantity = 0; // 당월 총 출고 수량
    
    @Column(nullable = false)
    private Integer closingQuantity = 0; // 월말 마감 재고 수량
    
    @Column(nullable = false)
    private Boolean isClosed = false; // 마감 완료 여부
    
    private LocalDateTime closedAt; // 마감 처리 시점
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    private User closedBy; // 마감 처리자
    
    private String notes; // 비고/메모
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 등록날짜
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt; // 수정날짜
}