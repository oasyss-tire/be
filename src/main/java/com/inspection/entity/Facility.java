package com.inspection.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;  
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.CascadeType;

@Entity
@Getter @Setter
public class Facility extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;  // 소속 회사
    
    private String name;                // 시설물명
    private String code;                // 관리 코드
    private String location;            // 기존 위치
    private BigDecimal acquisitionCost; // 취득 가액
    private LocalDate acquisitionDate;  // 취득일
    
    @Enumerated(EnumType.STRING)
    private FacilityStatus status;      // 상태(사용중/폐기/분실/매각)
    
    private LocalDate statusChangeDate; // 상태 변경일
    private String currentLocation;     // 현재 위치
    private String description;         // 설명
    
    @OneToMany(mappedBy = "facility", cascade = CascadeType.ALL)
    private List<FacilityContract> contracts = new ArrayList<>();
    
    @OneToMany(mappedBy = "facility", cascade = CascadeType.ALL)
    private List<FacilityImage> images = new ArrayList<>();
} 