package com.inspection.dto;

import java.time.LocalDate;

import com.inspection.entity.Company;
import com.inspection.entity.Code;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateCompanyRequest {
    private String storeCode;        // 매장코드
    private String storeNumber;      // 점번 (선택사항, 미입력시 자동생성, 3자리 숫자)
    private String storeName;        // 매장명
    private String trustee;          // 수탁자
    private String trusteeCode;      // 수탁코드
    private String businessNumber;   // 사업자번호
    private String companyName;      // 상호
    private String representativeName; // 대표자명
    private boolean active = true;   // 상태 (기본값: 활성화)
    private LocalDate startDate;     // 시작일자
    private LocalDate endDate;       // 종료일자
    private LocalDate insuranceStartDate; // 하자보증증권 보험시작일
    private LocalDate insuranceEndDate;   // 하자보증증권 보험종료일
    private String managerName;      // 담당자
    private String email;            // 이메일
    private String subBusinessNumber; // 종사업장번호
    private String phoneNumber;      // 휴대폰번호
    private String storeTelNumber;   // 매장 전화번호
    private String address;          // 주소
    private String businessType;     // 업태
    private String businessCategory; // 종목
    private String createdBy;        // 등록자
    private String branchGroupId;    // 지부 그룹 ID
    
    // DTO -> Entity 변환 메서드
    public Company toEntity() {
        Company company = new Company();
        company.setStoreCode(this.storeCode);
        company.setStoreNumber(this.storeNumber);
        company.setStoreName(this.storeName);
        company.setTrustee(this.trustee);
        company.setTrusteeCode(this.trusteeCode);
        company.setBusinessNumber(this.businessNumber);
        company.setCompanyName(this.companyName);
        company.setRepresentativeName(this.representativeName);
        company.setActive(this.active);
        company.setStartDate(this.startDate);
        company.setEndDate(this.endDate);
        company.setInsuranceStartDate(this.insuranceStartDate);
        company.setInsuranceEndDate(this.insuranceEndDate);
        company.setManagerName(this.managerName);
        company.setEmail(this.email);
        company.setSubBusinessNumber(this.subBusinessNumber);
        company.setPhoneNumber(this.phoneNumber);
        company.setStoreTelNumber(this.storeTelNumber);
        company.setAddress(this.address);
        company.setBusinessType(this.businessType);
        company.setBusinessCategory(this.businessCategory);
        company.setCreatedBy(this.createdBy);
        
        // 지부 그룹은 ID만 설정하고 실제 객체는 서비스에서 처리
        if (this.branchGroupId != null && !this.branchGroupId.isEmpty()) {
            Code branchGroup = new Code();
            branchGroup.setCodeId(this.branchGroupId);
            company.setBranchGroup(branchGroup);
        }
        
        return company;
    }
} 