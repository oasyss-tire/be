package com.inspection.facility.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.Setter;

/**
 * 기타 트랜잭션 요청 DTO
 */
@Getter @Setter
public class MiscTransactionRequest {
    
    private Long facilityId;        // 시설물 ID
    private Long locationCompanyId; // 현재 위치 회사 ID
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime transactionDate; // 트랜잭션 일자 (없으면 현재 시간)
    
    private String reason;          // 기타 처리 사유 (재고 차이, 시스템 오류 등)
    private String notes;           // 비고 (상세 설명)
    private String transactionRef;  // 외부 참조 번호
    private String batchId;         // 배치 ID (다수의 시설물 처리 시)
} 