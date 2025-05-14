package com.inspection.as.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateServiceRequestDTO {
    
    private LocalDateTime requestDate;
    
    private Boolean isReceived;
    
    @Size(max = 2000, message = "접수 내용은 최대 2000자까지 입력 가능합니다")
    private String requestContent;
    
    private Long managerId;
    
    private LocalDateTime expectedCompletionDate;
    
    private LocalDateTime completionDate;
    
    private Boolean isCompleted;
    
    private String serviceTypeCode;
    
    private String priorityCode;
    
    private Double cost;
    
    @Size(max = 500, message = "메모는 최대 500자까지 입력 가능합니다")
    private String notes;
    
    private String departmentTypeCode;  // 담당 부서 유형 코드 (003001_0001: 메인장비팀, 003001_0002: 전기팀, 003001_0003: 시설팀)
} 