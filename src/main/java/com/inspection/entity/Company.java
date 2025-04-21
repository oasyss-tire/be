package com.inspection.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "companies")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public class Company {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 10)
    private String storeCode;        // 매장코드 (S00111)
    
    @Column(nullable = false, length = 3)
    private String storeNumber;      // 점번 (001~999)
    
    @Column(nullable = false, length = 1000)
    private String storeName;        // 매장명 (창원점)
    
    @Column(length = 100)
    private String trustee;          // 수탁자 (정재현타이어(창원점))
    
    @Column(length = 10)
    private String trusteeCode;      // 수탁코드 (2600)
    
    @Column(length = 20)
    private String businessNumber;   // 사업자번호 (000-00-00000)
    
    @Column(length = 100)
    private String companyName;      // 상호 (정재현타이어(창원점))
    
    @Column(length = 50)
    private String representativeName; // 대표자명 (정재현)
    
    @Column(nullable = false)
    private boolean active = true;   // 상태 (0 or 1)
    
    @Column
    private LocalDate startDate;     // 시작일자
    
    @Column
    private LocalDate endDate;       // 종료일자
    
    @Column
    private LocalDate insuranceStartDate; // 하자보증증권 보험시작일 
    
    @Column
    private LocalDate insuranceEndDate;   // 하자보증증권 보험종료일 
    
    @Column(length = 50)
    private String managerName;      // 담당자 (정재현)
    
    @Column(length = 100)
    private String email;            // 이메일 (mail@mail.com)
    
    @Column(length = 10)
    private String subBusinessNumber; // 종사업장번호 (0103)
    
    @Column(length = 20)
    private String phoneNumber;      // 휴대폰번호 (010-0000-0000)
    
    @Column(length = 20)
    private String storeTelNumber;   // 매장 전화번호 (055-123-4567)
    
    @Column(length = 255)
    private String address;          // 주소 (경상남도 창원~~)
    
    @Column(length = 100)
    private String businessType;     // 업태 (도소매,서비스)
    
    @Column(length = 100)
    private String businessCategory; // 종목 (상품대리,기타도급)
    
    // 등록한 사람
    @Column(length = 50)
    private String createdBy;        // 등록자
    
    // 등록일 (자동 생성)
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;  // 등록일
    
    // 수정일 (자동 업데이트)
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;  // 수정일
    
    // 회사 이미지와의 관계 설정 (1:1 관계)
    @OneToOne(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private CompanyImage companyImage;
} 