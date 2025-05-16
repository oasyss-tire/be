package com.inspection.facility.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.Setter;

/**
 * 분실 트랜잭션 요청 DTO
 */
@Getter @Setter
public class LostTransactionRequest {
    
    private Long facilityId;        // 시설물 ID
    private Long locationCompanyId; // 현재 위치 회사 ID
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lostDate; // 분실 일자 (없으면 현재 시간)
    
    private String notes;           // 비고 (분실 사유, 상황 등)
    private String transactionRef;  // 외부 참조 번호
    private String batchId;         // 배치 ID (다수의 시설물 처리 시)
} 