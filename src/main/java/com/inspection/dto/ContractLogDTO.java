package com.inspection.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.inspection.entity.Contract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 계약 이력 조회를 위한 간략한 계약 정보 DTO
 */
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractLogDTO {
    
    private Long id;
    private String title;                    // 계약 제목
    private String contractNumber;           // 계약 번호
    private String statusCodeId;             // 계약 상태 코드 ID
    private String statusCodeName;           // 계약 상태 이름
    private Integer progressRate;            // 계약 진행률
    private LocalDateTime createdAt;         // 계약 작성일
    private LocalDateTime completedAt;       // 계약 완료일
    private LocalDate startDate;             // 계약 시작일
    private LocalDate expiryDate;            // 계약 만료일
    private String createdBy;                // 계약 작성자
    private boolean active;                  // 활성화 여부
    
    // 계약 회사 정보
    private Long companyId;                  // 회사 ID
    private String companyName;              // 회사 이름
    private String businessNumber;           // 사업자 번호
    
    /**
     * Contract 엔티티에서 간략한 정보만 포함한 DTO로 변환
     */
    public static ContractLogDTO fromEntity(Contract entity) {
        if (entity == null) {
            return null;
        }
        
        ContractLogDTO dto = ContractLogDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .contractNumber(entity.getContractNumber())
                .progressRate(entity.getProgressRate())
                .createdAt(entity.getCreatedAt())
                .completedAt(entity.getCompletedAt())
                .startDate(entity.getStartDate())
                .expiryDate(entity.getExpiryDate())
                .createdBy(entity.getCreatedBy())
                .active(entity.isActive())
                .build();
        
        // 계약 상태 코드 정보
        if (entity.getStatusCode() != null) {
            dto.setStatusCodeId(entity.getStatusCode().getCodeId());
            dto.setStatusCodeName(entity.getStatusCode().getCodeName());
        }
        
        // 회사 정보
        if (entity.getCompany() != null) {
            dto.setCompanyId(entity.getCompany().getId());
            dto.setCompanyName(entity.getCompany().getCompanyName());
            dto.setBusinessNumber(entity.getCompany().getBusinessNumber());
        }
        
        return dto;
    }
} 