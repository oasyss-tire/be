package com.inspection.facility.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 현재 재고 상태 정보를 담는 DTO
 * 최신 마감 데이터 + 마감 이후 발생한 트랜잭션을 반영한 현재 시점의 재고 정보
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentInventoryStatusDTO {
    
    // 회사 정보
    private Long companyId;
    private String companyName;
    
    // 시설물 유형 정보
    private String facilityTypeCodeId;
    private String facilityTypeName;
    
    // 최신 마감 날짜
    private LocalDate latestClosingDate;
    
    // 최신 마감 시점 재고 수량 (기준 수량)
    private Integer baseQuantity;
    
    // 마감 이후 입고 수량
    private Integer recentInbound;
    
    // 마감 이후 출고 수량
    private Integer recentOutbound;
    
    // 현재 재고 수량 (실시간)
    private Integer currentQuantity;
} 