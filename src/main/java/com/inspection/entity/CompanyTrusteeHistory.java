package com.inspection.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "company_trustee_history")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor
public class CompanyTrusteeHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    
    @Column(nullable = false)
    private boolean isActive = true;
    
    // 수탁자 관련 정보
    @Column(length = 100)
    private String trustee;          // 수탁자 (정재현타이어(창원점))
    
    @Column(length = 10)
    private String trusteeCode;      // 수탁코드 (2600)
    
    @Column(length = 50)
    private String representativeName; // 대표자명 (정재현)
    
    @Column(length = 50)
    private String managerName;      // 담당자 (정재현)
    
    @Column(length = 200)            // 길이를 100에서 200으로 증가 (암호화 후 길이 고려)
    private String email;            // 이메일 (mail@mail.com)
    
    @Column(length = 100)            // 길이를 20에서 100으로 증가 (암호화 후 길이 고려)
    private String phoneNumber;      // 휴대폰번호 (010-0000-0000)
    
    @Column(length = 100)            // 길이를 20에서 100으로 증가 (암호화 후 길이 고려)
    private String businessNumber;   // 사업자번호 (000-00-00000)
    
    @Column(length = 10)
    private String subBusinessNumber; // 종사업장번호 (0103)
    
    @Column(length = 100)
    private String companyName;      // 상호 (정재현타이어(창원점))
    
    @Column(length = 100)            // 길이를 20에서 100으로 증가 (암호화 후 길이 고려)
    private String storeTelNumber;   // 매장 전화번호 (055-123-4567)
    
    @Column(length = 100)
    private String businessType;     // 업태 (도소매,서비스)
    
    @Column(length = 100)
    private String businessCategory; // 종목 (상품대리,기타도급)
    
    // 계약 관련 정보
    @Column
    private LocalDate startDate;     // 수탁 시작일
    
    @Column
    private LocalDate endDate;       // 수탁 종료일
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;       // 관련 계약
    
    @Column
    private LocalDate insuranceStartDate; // 하자보증증권 보험시작일
    
    @Column
    private LocalDate insuranceEndDate;   // 하자보증증권 보험종료일

    // 사용자 ID 추가
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;               // 연결된 사용자
    
    // 추가 관리 필드
    @Column(length = 50)
    private String modifiedBy;       // 수정자
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime modifiedAt; // 수정일시
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;  // 생성일시
    
    @Column(length = 200)
    private String reason;           // 변경 사유
    
    // 편의 메서드: Company에서 수탁자 정보 복사
    public void copyFromCompany(Company company) {
        this.trustee = company.getTrustee();
        this.trusteeCode = company.getTrusteeCode();
        this.representativeName = company.getRepresentativeName();
        this.managerName = company.getManagerName();
        this.email = company.getEmail();
        this.phoneNumber = company.getPhoneNumber();
        this.businessNumber = company.getBusinessNumber();
        this.subBusinessNumber = company.getSubBusinessNumber();
        this.companyName = company.getCompanyName();
        this.storeTelNumber = company.getStoreTelNumber();
        this.businessType = company.getBusinessType();
        this.businessCategory = company.getBusinessCategory();
        this.insuranceStartDate = company.getInsuranceStartDate();
        this.insuranceEndDate = company.getInsuranceEndDate();
        this.startDate = company.getStartDate();
        this.endDate = company.getEndDate();
    }
    
    // 편의 메서드: Company에 수탁자 정보 적용
    public void applyToCompany(Company company) {
        company.setTrustee(this.trustee);
        company.setTrusteeCode(this.trusteeCode);
        company.setRepresentativeName(this.representativeName);
        company.setManagerName(this.managerName);
        company.setEmail(this.email);
        company.setPhoneNumber(this.phoneNumber);
        company.setBusinessNumber(this.businessNumber);
        company.setSubBusinessNumber(this.subBusinessNumber);
        company.setCompanyName(this.companyName);
        company.setStoreTelNumber(this.storeTelNumber);
        company.setBusinessType(this.businessType);
        company.setBusinessCategory(this.businessCategory);
        company.setInsuranceStartDate(this.insuranceStartDate);
        company.setInsuranceEndDate(this.insuranceEndDate);
        company.setStartDate(this.startDate);
        company.setEndDate(this.endDate);
    }
}
