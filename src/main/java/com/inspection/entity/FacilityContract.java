package com.inspection.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter @Setter
public class FacilityContract extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id")
    private Facility facility;
    
    private String contractNumber;     // 계약번호
    private LocalDate startDate;       // 계약 시작일
    private LocalDate endDate;         // 계약 종료일
    private BigDecimal contractAmount; // 계약금액
    private Boolean isPaid;            // 비용 지불 여부
    private LocalDate paidDate;        // 지불일
    private String vendorName;         // 납품업체명
    private String vendorContact;      // 업체 연락처
    private String description;        // 계약 설명
} 