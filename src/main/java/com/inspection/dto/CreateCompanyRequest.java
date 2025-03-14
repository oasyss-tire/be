package com.inspection.dto;

import java.time.LocalDate;

import com.inspection.entity.Company;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateCompanyRequest {
    private String storeCode;        // 매장코드
    // storeNumber는 서버에서 자동 생성되므로 선택적 필드로 변경
    private String storeNumber;      // 점번 (자동 생성)
    private String storeName;        // 매장명
    private String trustee;          // 수탁자
    private String trusteeCode;      // 수탁코드
    private String businessNumber;   // 사업자번호
    private String companyName;      // 상호
    private String representativeName; // 대표자명
    private boolean active = true;   // 상태 (기본값: 활성화)
    private LocalDate startDate;     // 시작일자
    private LocalDate endDate;       // 종료일자
    private String managerName;      // 담당자
    private String email;            // 이메일
    private String subBusinessNumber; // 종사업장번호
    private String phoneNumber;      // 휴대폰번호
    private String address;          // 주소
    private String businessType;     // 업태
    private String businessCategory; // 종목
    private String createdBy;        // 등록자
    
    // DTO -> Entity 변환 메서드
    public Company toEntity() {
        Company company = new Company();
        company.setStoreCode(this.storeCode);
        // storeNumber는 서비스에서 자동 생성하므로 여기서 설정하지 않음
        company.setStoreName(this.storeName);
        company.setTrustee(this.trustee);
        company.setTrusteeCode(this.trusteeCode);
        company.setBusinessNumber(this.businessNumber);
        company.setCompanyName(this.companyName);
        company.setRepresentativeName(this.representativeName);
        company.setActive(this.active);
        company.setStartDate(this.startDate);
        company.setEndDate(this.endDate);
        company.setManagerName(this.managerName);
        company.setEmail(this.email);
        company.setSubBusinessNumber(this.subBusinessNumber);
        company.setPhoneNumber(this.phoneNumber);
        company.setAddress(this.address);
        company.setBusinessType(this.businessType);
        company.setBusinessCategory(this.businessCategory);
        company.setCreatedBy(this.createdBy);
        return company;
    }
} 