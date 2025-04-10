package com.inspection.as.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiveServiceRequestDTO {
    
    @NotNull(message = "예상 완료일은 필수입니다.")
    private LocalDateTime expectedCompletionDate;
} 