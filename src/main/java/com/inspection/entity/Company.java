package com.inspection.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;

@Entity
@Getter @Setter
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long companyId;
    
    @Column(unique = true)
    private String companyName;
    
    private String phoneNumber;
    private String faxNumber;
    private String notes;
    
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    // 주소 정보 필드 추가
    @Column(length = 255)  // 문자열 길이 제한 (옵션)
    private String address;


    // Company.java에 추가될 필드들
    private String businessNumber;        // 사업자번호
    private LocalDate contractDate;       // 계약일자
    private LocalDate startDate;          // 시작일자
    private LocalDate expiryDate;         // 만기일자
    private BigDecimal monthlyFee;        // 월 비용
    @Enumerated(EnumType.STRING)
    private CompanyStatus status;         // 상태 (enum으로 관리)
    private LocalDate terminationDate;    // 해지일자
    private String businessLicenseImage;  // 사업자등록증 이미지 추가

    // 이미지 관련 필드들
    private String exteriorImage;         // 외관사진
    private String entranceImage;         // 입구사진
    private String mainPanelImage;        // 메인 분전함사진
    private String etcImage1;             // 기타1
    private String etcImage2;             // 기타2
    private String etcImage3;             // 기타3
    private String etcImage4;             // 기타4


    // 건축물 정보 필드 추가
    private LocalDate buildingPermitDate;     // 건축허가일
    private LocalDate occupancyDate;          // 사용승인일
    
    @Column(precision = 10, scale = 2)        // 소수점 2자리까지 허용
    private BigDecimal totalFloorArea;        // 연면적
    
    @Column(precision = 10, scale = 2)        // 소수점 2자리까지 허용
    private BigDecimal buildingArea;          // 건축면적
    
    private Integer numberOfUnits;            // 세대수
    
    private String floorCount;                // 층수 (예: "지상 5층, 지하 1층")
    
    @Column(precision = 10, scale = 2)
    private BigDecimal buildingHeight;        // 높이
    
    private Integer numberOfBuildings;        // 건물동수
    
    @Column(length = 100)
    private String buildingStructure;         // 건축물구조
    
    @Column(length = 100)
    private String roofStructure;             // 지붕구조
    
    @Column(length = 200)
    private String ramp;                      // 경사로
    
    @Column(length = 200)
    private String stairsType;  // 직통계단, 특별피난계단 등
    private Integer stairsCount; // 계단 개수

    @Column(length = 200)
    private String elevatorType; // 승용, 비상용, 피난용
    private Integer elevatorCount; // 승강기 개수
    
    @Column(length = 200)
    private String parkingLot;                // 주차장

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Employee> employees = new ArrayList<>();
} 