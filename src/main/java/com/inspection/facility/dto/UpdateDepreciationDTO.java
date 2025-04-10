package com.inspection.facility.dto;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class UpdateDepreciationDTO {
    
    @NotNull(message = "감가상각 ID는 필수입니다")
    private Long depreciationId;
    
    @NotNull(message = "감가상각 적용일은 필수입니다")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime depreciationDate;
    
    @NotNull(message = "이전 가치는 필수입니다")
    @Min(value = 0, message = "이전 가치는 0 이상이어야 합니다")
    private Double previousValue;
    
    @NotNull(message = "감가상각액은 필수입니다")
    @Min(value = 0, message = "감가상각액은 0 이상이어야 합니다")
    private Double depreciationAmount;
    
    @NotNull(message = "현재 가치는 필수입니다")
    @Min(value = 0, message = "현재 가치는 0 이상이어야 합니다")
    private Double currentValue;
    
    @NotNull(message = "감가상각 유형 코드는 필수입니다")
    private String depreciationTypeCode;
    
    @NotNull(message = "감가상각 방법 코드는 필수입니다")
    private String depreciationMethodCode;
    
    @NotNull(message = "회계연도는 필수입니다")
    private Integer fiscalYear;
    
    @NotNull(message = "회계월은 필수입니다")
    @Min(value = 1, message = "회계월은 1~12 사이여야 합니다")
    private Integer fiscalMonth;
    
    @Size(max = 500, message = "비고는 최대 500자까지 입력 가능합니다")
    private String notes;
} 