package com.inspection.facility.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 재고 상태 정보를 담는 DTO
 * 일일 또는 월간 마감 정보를 클라이언트에 전달하기 위한 통합 객체
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryStatusDTO {
    
    // 마감 정보 식별자
    private Long closingId;
    
    // 마감 날짜 (월간 마감의 경우 해당 월의 마지막 날)
    private LocalDate closingDate;
    
    // 회사 정보
    private Long companyId;
    private String storeCode;   // 매장 코드 (S00111)
    private String companyName; // 매장명 (storeName)
    
    // 시설물 유형 정보
    private String facilityTypeCodeId;
    private String facilityTypeName;
    
    // 이전 기간(일 또는 월) 재고 수량
    private Integer previousQuantity;
    
    // 입고 수량
    private Integer inboundQuantity;
    
    // 출고 수량
    private Integer outboundQuantity;
    
    // 마감 수량
    private Integer closingQuantity;
    
    // 마감 여부
    private Boolean isClosed;
    
    // 마감 일시
    private LocalDateTime closedAt;
    
    // 마감 처리자
    private Long closedBy;
} 