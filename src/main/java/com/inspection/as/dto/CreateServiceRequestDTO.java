package com.inspection.as.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
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
public class CreateServiceRequestDTO {
    
    @NotNull(message = "시설물 ID는 필수입니다.")
    private Long facilityId;
    
    @NotNull(message = "접수일은 필수입니다.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime requestDate;
    
    @NotBlank(message = "접수 내용은 필수입니다.")
    @Size(max = 2000, message = "접수 내용은 최대 2000자까지 입력 가능합니다.")
    private String requestContent;
    
    private Long requesterId;
    
    private Long managerId;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expectedCompletionDate;
    
    @NotBlank(message = "서비스 유형 코드는 필수입니다.")
    private String serviceTypeCode;
    
    @NotBlank(message = "우선순위 코드는 필수입니다.")
    private String priorityCode;
    
    private String statusCode;
    
    private Double cost;
    
    @Size(max = 500, message = "비고는 최대 500자까지 입력 가능합니다.")
    private String notes;
    
    @Builder.Default
    private Boolean isReceived = false;
    
    @Builder.Default
    private Boolean isCompleted = false;
    
    // 이미지 파일 목록
    private List<MultipartFile> images;
} 