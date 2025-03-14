package com.inspection.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.inspection.entity.Company;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CompanyDTO {
    private Long id;
    private String storeCode;        // 매장코드
    private String storeNumber;      // 점번
    private String storeName;        // 매장명
    private String trustee;          // 수탁자
    private String trusteeCode;      // 수탁코드
    private String businessNumber;   // 사업자번호
    private String companyName;      // 상호
    private String representativeName; // 대표자명
    private boolean active;          // 상태
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
    private LocalDateTime createdAt;  // 등록일
    private LocalDateTime updatedAt;  // 수정일
    
    // 이미지 정보
    private CompanyImageDTO imageInfo;
    
    // Entity -> DTO 변환 메서드
    public static CompanyDTO fromEntity(Company company) {
        CompanyDTO dto = new CompanyDTO();
        dto.setId(company.getId());
        dto.setStoreCode(company.getStoreCode());
        dto.setStoreNumber(company.getStoreNumber());
        dto.setStoreName(company.getStoreName());
        dto.setTrustee(company.getTrustee());
        dto.setTrusteeCode(company.getTrusteeCode());
        dto.setBusinessNumber(company.getBusinessNumber());
        dto.setCompanyName(company.getCompanyName());
        dto.setRepresentativeName(company.getRepresentativeName());
        dto.setActive(company.isActive());
        dto.setStartDate(company.getStartDate());
        dto.setEndDate(company.getEndDate());
        dto.setManagerName(company.getManagerName());
        dto.setEmail(company.getEmail());
        dto.setSubBusinessNumber(company.getSubBusinessNumber());
        dto.setPhoneNumber(company.getPhoneNumber());
        dto.setAddress(company.getAddress());
        dto.setBusinessType(company.getBusinessType());
        dto.setBusinessCategory(company.getBusinessCategory());
        dto.setCreatedBy(company.getCreatedBy());
        dto.setCreatedAt(company.getCreatedAt());
        dto.setUpdatedAt(company.getUpdatedAt());
        
        if (company.getCompanyImage() != null) {
            dto.setImageInfo(CompanyImageDTO.fromEntity(company.getCompanyImage()));
        }
        
        return dto;
    }
} 