package com.inspection.facility.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * AS 트랜잭션 요청 (AS 센터로 이동 또는 AS 센터에서 복귀)
 */
@Getter @Setter
public class ServiceTransactionRequest {
    
    @NotNull(message = "시설물 ID는 필수입니다")
    private Long facilityId;                  // 시설물 ID
    
    @NotNull(message = "AS 요청 ID는 필수입니다")
    private Long serviceRequestId;            // 관련 AS 요청 ID
    
    @NotNull(message = "출발 회사 ID는 필수입니다")
    private Long fromCompanyId;               // 출발 회사 ID
    
    @NotNull(message = "도착 회사 ID는 필수입니다")
    private Long toCompanyId;                 // 도착 회사 ID (AS 센터 또는 원래 위치)
    
    // AS 센터로 이동하는 경우 false, AS 센터에서 복귀하는 경우 true
    @NotNull(message = "복귀 여부는 필수입니다")
    private Boolean isReturn;                // AS 센터 복귀 여부
    
    // 이전 AS 트랜잭션 ID (복귀 시에만 필요)
    private Long relatedTransactionId;        // 관련 트랜잭션 ID
    
    private String notes;                     // 비고/메모
    
    @NotNull(message = "상태 코드는 필수입니다")
    private String statusAfterCode;           // AS 처리 후 상태 코드
    
    private String transactionRef;            // 트랜잭션 참조 번호
    
    private String batchId;                   // 배치 ID (동일 작업으로 생성된 트랜잭션 그룹)
} 