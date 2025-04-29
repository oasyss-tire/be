package com.inspection.facility.entity;

import java.time.LocalDate;
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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "daily_inventory_closings")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public class DailyInventoryClosing {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private LocalDate closingDate; // 마감일
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company; // 회사/지점
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_type_code", nullable = false)
    private Code facilityType; // 시설물 유형
    
    @Column(nullable = false)
    private Integer previousDayQuantity = 0; // 전일 재고 수량
    
    @Column(nullable = false)
    private Integer inboundQuantity = 0; // 당일 입고 수량
    
    @Column(nullable = false)
    private Integer outboundQuantity = 0; // 당일 출고 수량
    
    @Column(nullable = false)
    private Integer closingQuantity = 0; // 당일 마감 재고 수량
    
    @Column(nullable = false)
    private Boolean isClosed = false; // 마감 완료 여부
    
    private LocalDateTime closedAt; // 마감 처리 시점
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    private User closedBy; // 마감 처리자
    
    private String notes; // 비고/메모
    
    @Column(nullable = true)
    private LocalDateTime processStartTime; // 마감 처리 시작 시간
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 등록날짜
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt; // 수정날짜
}