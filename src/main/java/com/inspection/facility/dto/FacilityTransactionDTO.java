package com.inspection.facility.dto;

import java.time.LocalDateTime;

import com.inspection.facility.entity.FacilityTransaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilityTransactionDTO {
    
    private Long transactionId;                // 트랜잭션 ID
    private String batchId;                    // 배치 ID (UUID)
    private Long facilityId;                   // 시설물 ID
    private String facilitySerialNumber;       // 시설물 시리얼 번호
    private String facilityTypeName;           // 시설물 유형명
    private String managementNumber;           // 시설물 관리번호
    
    private String brandCode;                  // 브랜드 코드
    private String brandCodeName;              // 브랜드 이름
    
    private String transactionTypeCode;        // 트랜잭션 유형 코드
    private String transactionTypeName;        // 트랜잭션 유형명
    private LocalDateTime transactionDate;     // 트랜잭션 발생일시
    
    private Long fromCompanyId;                // 출발 회사 ID
    private String fromCompanyName;            // 출발 회사명
    private Long toCompanyId;                  // 도착 회사 ID
    private String toCompanyName;              // 도착 회사명
    
    private String notes;                      // 비고/메모
    
    private String statusBeforeCode;           // 트랜잭션 전 상태 코드
    private String statusBeforeName;           // 트랜잭션 전 상태명
    private String statusAfterCode;            // 트랜잭션 후 상태 코드
    private String statusAfterName;            // 트랜잭션 후 상태명
    
    private LocalDateTime expectedReturnDate;  // 반납 예정일
    private LocalDateTime actualReturnDate;    // 실제 반납일
    
    private Long relatedTransactionId;         // 연관 트랜잭션 ID
    private Long serviceRequestId;             // 관련 AS 요청 ID
    
    private Long performedById;                // 트랜잭션 수행자 ID
    private String performedByName;            // 트랜잭션 수행자명
    
    private String transactionRef;             // 트랜잭션 참조 번호
    
    private Boolean isCancelled;               // 취소 여부
    private String cancellationReason;         // 취소 사유
    
    private LocalDateTime createdAt;           // 생성일시
    private LocalDateTime updatedAt;           // 수정일시
    
    /**
     * 엔티티를 DTO로 변환
     */
    public static FacilityTransactionDTO fromEntity(FacilityTransaction entity) {
        if (entity == null) return null;
        
        return FacilityTransactionDTO.builder()
                .transactionId(entity.getTransactionId())
                .batchId(entity.getBatchId())
                .facilityId(entity.getFacility().getFacilityId())
                .facilitySerialNumber(entity.getFacility().getSerialNumber())
                .facilityTypeName(entity.getFacility().getFacilityType().getCodeName())
                .managementNumber(entity.getFacility().getManagementNumber())
                
                .brandCode(entity.getFacility().getBrand() != null ? entity.getFacility().getBrand().getCodeId() : null)
                .brandCodeName(entity.getFacility().getBrand() != null ? entity.getFacility().getBrand().getCodeName() : null)
                
                .transactionTypeCode(entity.getTransactionType().getCodeId())
                .transactionTypeName(entity.getTransactionType().getCodeName())
                .transactionDate(entity.getTransactionDate())
                
                .fromCompanyId(entity.getFromCompany() != null ? entity.getFromCompany().getId() : null)
                .fromCompanyName(entity.getFromCompany() != null ? entity.getFromCompany().getStoreName() : null)
                .toCompanyId(entity.getToCompany() != null ? entity.getToCompany().getId() : null)
                .toCompanyName(entity.getToCompany() != null ? entity.getToCompany().getStoreName() : null)
                
                .notes(entity.getNotes())
                
                .statusBeforeCode(entity.getStatusBefore() != null ? entity.getStatusBefore().getCodeId() : null)
                .statusBeforeName(entity.getStatusBefore() != null ? entity.getStatusBefore().getCodeName() : null)
                .statusAfterCode(entity.getStatusAfter() != null ? entity.getStatusAfter().getCodeId() : null)
                .statusAfterName(entity.getStatusAfter() != null ? entity.getStatusAfter().getCodeName() : null)
                
                .expectedReturnDate(entity.getExpectedReturnDate())
                .actualReturnDate(entity.getActualReturnDate())
                
                .relatedTransactionId(entity.getRelatedTransaction() != null ? entity.getRelatedTransaction().getTransactionId() : null)
                .serviceRequestId(entity.getServiceRequest() != null ? entity.getServiceRequest().getServiceRequestId() : null)
                
                .performedById(entity.getPerformedBy() != null ? entity.getPerformedBy().getId() : null)
                .performedByName(entity.getPerformedBy() != null ? entity.getPerformedBy().getUserName() : null)
                
                .transactionRef(entity.getTransactionRef())
                .isCancelled(entity.getIsCancelled())
                .cancellationReason(entity.getCancellationReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
} 