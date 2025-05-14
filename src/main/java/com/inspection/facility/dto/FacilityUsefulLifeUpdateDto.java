package com.inspection.facility.dto;

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
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacilityUsefulLifeUpdateDto {
    @NotNull(message = "시설물 ID는 필수입니다.")
    private Long id; // 설비 ID
    
    @NotNull(message = "사용연한은 필수입니다.")
    @Min(value = 1, message = "사용연한은 1개월 이상이어야 합니다.")
    private Integer usefulLifeMonths; // 수정할 사용연한(월)
    
    @Size(max = 500, message = "사용연한 수정 이유는 500자 이내여야 합니다.")
    private String usefulLifeUpdateReason; // 사용연한 수정 이유
} 