package com.inspection.facility.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 폐기 트랜잭션 요청 (시설물 생애주기 종료)
 */
@Getter @Setter
public class DisposeTransactionRequest {
    
    @NotNull(message = "시설물 ID는 필수입니다")
    private Long facilityId;                  // 시설물 ID
    
    @NotNull(message = "폐기 사유는 필수입니다")
    private String notes;                     // 폐기 사유 및 비고
    
    private String transactionRef;            // 트랜잭션 참조 번호
} 