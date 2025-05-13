package com.inspection.as.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteServiceRequestDTO {
    
    @Min(value = 0, message = "수리 비용은 0 이상이어야 합니다.")
    private Double cost;
    
    // 이미지 파일 목록
    private List<MultipartFile> images;
} 