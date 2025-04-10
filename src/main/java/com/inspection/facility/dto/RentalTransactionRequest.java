package com.inspection.facility.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 대여 트랜잭션 요청 (소유권 유지, 위치 변경)
 */
@Getter @Setter
public class RentalTransactionRequest {
    
    @NotNull(message = "시설물 ID는 필수입니다")
    private Long facilityId;                  // 시설물 ID
    
    @NotNull(message = "출발 회사 ID는 필수입니다")
    private Long fromCompanyId;               // 대여해주는 회사 ID
    
    @NotNull(message = "도착 회사 ID는 필수입니다")
    private Long toCompanyId;                 // 대여받는 회사 ID
    
    @NotNull(message = "반납 예정일은 필수입니다")
    @Future(message = "반납 예정일은 미래 날짜여야 합니다")
    private LocalDateTime expectedReturnDate; // 반납 예정일
    
    private String notes;                     // 비고/메모
    
    private String statusAfterCode;           // 대여 후 상태 코드 (기본값: 임대중)
    
    private String transactionRef;            // 트랜잭션 참조 번호
} 