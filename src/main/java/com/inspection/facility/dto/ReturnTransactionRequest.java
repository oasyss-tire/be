package com.inspection.facility.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 반납 트랜잭션 요청 (원래 소유자에게 반납)
 */
@Getter @Setter
public class ReturnTransactionRequest {
    
    @NotNull(message = "시설물 ID는 필수입니다")
    private Long facilityId;                  // 시설물 ID
    
    @NotNull(message = "대여 트랜잭션 ID는 필수입니다")
    private Long rentalTransactionId;         // 관련 대여 트랜잭션 ID
    
    private LocalDateTime actualReturnDate;   // 실제 반납일 (기본값: 현재시간)
    
    private String notes;                     // 비고/메모
    
    @NotNull(message = "상태 코드는 필수입니다")
    private String statusAfterCode;           // 반납 후 상태 코드
    
    private String transactionRef;            // 트랜잭션 참조 번호
} 