package com.inspection.facility.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class FacilityTransactionRequest {
    
    @NotNull(message = "시설물 ID는 필수입니다")
    private Long facilityId;                  // 시설물 ID
    
    @NotNull(message = "트랜잭션 유형 코드는 필수입니다")
    private String transactionTypeCode;       // 트랜잭션 유형 코드
    
    private LocalDateTime transactionDate;    // 트랜잭션 발생일시 (기본값: 현재시간)
    
    private Long fromCompanyId;               // 출발 회사 ID
    private Long toCompanyId;                 // 도착 회사 ID
    
    private String notes;                     // 비고/메모
    
    private String statusAfterCode;           // 트랜잭션 후 상태 코드
    
    private LocalDateTime expectedReturnDate; // 반납 예정일 (대여 시)
    private LocalDateTime actualReturnDate;   // 실제 반납일 (반납 시)
    
    private Long relatedTransactionId;        // 연관 트랜잭션 ID
    private Long serviceRequestId;            // 관련 AS 요청 ID
    
    private String transactionRef;            // 트랜잭션 참조 번호
    
    private String batchId;                   // 배치 ID (동일 작업으로 생성된 트랜잭션 그룹)
} 