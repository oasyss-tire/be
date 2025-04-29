package com.inspection.facility.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 입고 트랜잭션 요청 (신규 시설물 등록 시, 또는 타 회사에서 입고 시)
 */
@Getter @Setter
public class InboundTransactionRequest {
    
    @NotNull(message = "시설물 ID는 필수입니다")
    private Long facilityId;                  // 시설물 ID
    
    @NotNull(message = "입고 회사 ID는 필수입니다")
    private Long toCompanyId;                 // 입고할 회사 ID
    
    private Long fromCompanyId;               // 출발 회사 ID (신규 등록이 아닌 경우)
    
    private String notes;                     // 비고/메모
    
    @NotNull(message = "상태 코드는 필수입니다")
    private String statusAfterCode;           // 입고 후 상태 코드
    
    private String transactionRef;            // 트랜잭션 참조 번호
    
    private String batchId;                   // 배치 ID (동일 작업으로 생성된 트랜잭션 그룹)
} 