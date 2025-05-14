package com.inspection.as.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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
    
    @Size(max = 2000, message = "수리 코멘트는 2000자 이내여야 합니다.")
    private String repairComment; // 수리 코멘트 (수리 내용, 문제 원인 등)
    
    // 이미지 파일 목록
    private List<MultipartFile> images;
} 