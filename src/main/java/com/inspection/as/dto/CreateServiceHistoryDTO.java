package com.inspection.as.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateServiceHistoryDTO {
    
    @NotNull(message = "AS 접수 ID는 필수 입력값입니다")
    private Long serviceRequestId;
    
    @NotBlank(message = "작업 유형 코드는 필수 입력값입니다")
    private String actionTypeCode;
    
    @Size(max = 2000, message = "작업 내용은 최대 2000자까지 입력 가능합니다")
    private String actionDescription;
    
    @Size(max = 500, message = "사용 부품은 최대 500자까지 입력 가능합니다")
    private String partsUsed;
    
    private Double partsCost;
    
    private Double laborCost;
    
    private Long performedById;
} 