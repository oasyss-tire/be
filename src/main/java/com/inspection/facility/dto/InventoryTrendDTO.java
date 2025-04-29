package com.inspection.facility.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 재고 추이 정보를 담는 DTO
 * 특정 기간의 재고 변화 추이를 표시하기 위한 객체
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTrendDTO {
    
    // 마감 날짜
    private LocalDate closingDate;
    
    // 시설물 유형 정보
    private String facilityTypeCodeId;
    private String facilityTypeName;
    
    // 회사 정보
    private Long companyId;
    private String companyName;
    
    // 해당 날짜의 재고 마감 수량
    private Integer closingQuantity;
    
    // 입고 수량
    private Integer inboundQuantity;
    
    // 출고 수량
    private Integer outboundQuantity;
    
    // 전일(또는 전월) 대비 증감량
    private Integer quantityChange;
    
    // 전일(또는 전월) 대비 증감률 (백분율)
    private Double changeRate;
} 