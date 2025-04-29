package com.inspection.facility.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 출고 트랜잭션 요청 (소유권 이전)
 */
@Getter @Setter
public class OutboundTransactionRequest {
    
    @NotNull(message = "시설물 ID는 필수입니다")
    private Long facilityId;                  // 시설물 ID
    
    @NotNull(message = "출발 회사 ID는 필수입니다")
    private Long fromCompanyId;               // 출발 회사 ID
    
    @NotNull(message = "도착 회사 ID는 필수입니다")
    private Long toCompanyId;                 // 도착할 회사 ID
    
    @NotNull(message = "소유권 이전 여부는 필수입니다")
    private Boolean transferOwnership;        // 소유권 이전 여부
    
    private String notes;                     // 비고/메모
    
    @NotNull(message = "상태 코드는 필수입니다")
    private String statusAfterCode;           // 출고 후 상태 코드
    
    private String transactionRef;            // 트랜잭션 참조 번호
    
    private String batchId;                   // 배치 ID (동일 작업으로 생성된 트랜잭션 그룹)
} 