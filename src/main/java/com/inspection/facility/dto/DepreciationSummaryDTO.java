package com.inspection.facility.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepreciationSummaryDTO {
    
    private Long facilityId;
    private String facilityName;
    private Integer fiscalYear;
    private Integer fiscalMonth;
    private Double totalDepreciationAmount;
    private Double initialValue;
    private Double currentValue;
    private Double depreciationRate;
    
    /**
     * 감가상각율 계산 (%)
     * 감가상각율 = (총 감가상각액 / 초기 가치) * 100
     */
    public Double calculateDepreciationRate() {
        if (initialValue == null || initialValue == 0 || totalDepreciationAmount == null) {
            return 0.0;
        }
        return (totalDepreciationAmount / initialValue) * 100;
    }
    
    /**
     * 남은 가치 비율 계산 (%)
     * 남은 가치 비율 = (현재 가치 / 초기 가치) * 100
     */
    public Double calculateRemainingValueRate() {
        if (initialValue == null || initialValue == 0 || currentValue == null) {
            return 0.0;
        }
        return (currentValue / initialValue) * 100;
    }
} 