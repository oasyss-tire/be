package com.inspection.dto;

import com.inspection.entity.CompanyImage;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CompanyImageDTO {
    private Long id;
    private String frontImage;      // 회사 정면사진
    private String backImage;       // 회사 후면사진
    private String leftSideImage;   // 회사 측면(좌)사진
    private String rightSideImage;  // 회사 측면(우)사진
    private String fullImage;       // 회사 전체사진
    
    // Entity -> DTO 변환 메서드
    public static CompanyImageDTO fromEntity(CompanyImage image) {
        CompanyImageDTO dto = new CompanyImageDTO();
        dto.setId(image.getId());
        dto.setFrontImage(image.getFrontImage());
        dto.setBackImage(image.getBackImage());
        dto.setLeftSideImage(image.getLeftSideImage());
        dto.setRightSideImage(image.getRightSideImage());
        dto.setFullImage(image.getFullImage());
        return dto;
    }
} 