package com.inspection.facility.dto;

import java.time.LocalDateTime;

import com.inspection.facility.entity.FacilityTransactionImage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilityTransactionImageDTO {
    
    private Long imageId;
    private Long transactionId;
    private String imageUrl;
    private String imageTypeCode;
    private String imageTypeName;
    private boolean active;
    private String uploadBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 엔티티를 DTO로 변환하는 정적 메서드
    public static FacilityTransactionImageDTO fromEntity(FacilityTransactionImage image) {
        return FacilityTransactionImageDTO.builder()
                .imageId(image.getImageId())
                .transactionId(image.getTransaction().getTransactionId())
                .imageUrl(image.getImageUrl())
                .imageTypeCode(image.getImageType() != null ? image.getImageType().getCodeId() : null)
                .imageTypeName(image.getImageType() != null ? image.getImageType().getCodeName() : null)
                .active(image.isActive())
                .uploadBy(image.getUploadBy())
                .createdAt(image.getCreatedAt())
                .updatedAt(image.getUpdatedAt())
                .build();
    }
}
