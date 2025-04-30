package com.inspection.dto;

import com.inspection.entity.Company;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 계약 이력 조회를 위한 간략한 회사 정보 DTO
 */
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyLogDTO {
    
    private Long id;
    private String storeCode;        // 매장코드
    private String storeNumber;      // 점번
    private String storeName;        // 매장명
    private String trustee;          // 수탁자
    private String businessNumber;   // 사업자번호
    private String companyName;      // 상호
    private String representativeName; // 대표자명
    private boolean active;          // 활성화 상태
    
    /**
     * Company 엔티티에서 변환
     */
    public static CompanyLogDTO fromEntity(Company entity) {
        if (entity == null) {
            return null;
        }
        
        return CompanyLogDTO.builder()
                .id(entity.getId())
                .storeCode(entity.getStoreCode())
                .storeNumber(entity.getStoreNumber())
                .storeName(entity.getStoreName())
                .trustee(entity.getTrustee())
                .businessNumber(entity.getBusinessNumber())
                .companyName(entity.getCompanyName())
                .representativeName(entity.getRepresentativeName())
                .active(entity.isActive())
                .build();
    }
} 