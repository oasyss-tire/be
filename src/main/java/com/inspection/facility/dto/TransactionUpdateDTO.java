package com.inspection.facility.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 트랜잭션 수정을 위한 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionUpdateDTO {
    
    // 필수 수정 필드
    private String notes; // 비고/메모
    private LocalDateTime transactionDate; // 트랜잭션 발생 일시
    
    // 선택적 수정 필드 (필요에 따라 추가)
    private Long fromCompanyId; // 출발 회사 ID
    private Long toCompanyId; // 도착 회사 ID
    
    // 수정 사유 (감사 추적용)
    private String updateReason;
} 