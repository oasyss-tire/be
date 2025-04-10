package com.inspection.facility.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 이동 트랜잭션 요청 (위치만 변경)
 */
@Getter @Setter
public class MoveTransactionRequest {
    
    @NotNull(message = "시설물 ID는 필수입니다")
    private Long facilityId;                  // 시설물 ID
    
    @NotNull(message = "출발 회사 ID는 필수입니다")
    private Long fromCompanyId;               // 현재 회사 ID
    
    @NotNull(message = "도착 회사 ID는 필수입니다")
    private Long toCompanyId;                 // 이동할 회사 ID
    
    private String notes;                     // 비고/메모
    
    private String statusAfterCode;           // 이동 후 상태 코드
    
    private String transactionRef;            // 트랜잭션 참조 번호
} 