package com.inspection.facility.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 트랜잭션 취소 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CancellationRequest {
    
    @NotBlank(message = "취소 사유는 필수입니다")
    private String reason; // 취소 사유
    
    private String additionalInfo; // 추가 정보 (선택)
} 