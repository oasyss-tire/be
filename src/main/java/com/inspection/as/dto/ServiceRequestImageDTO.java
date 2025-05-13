package com.inspection.as.dto;

import java.time.LocalDateTime;

import com.inspection.as.entity.ServiceRequestImage;

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
public class ServiceRequestImageDTO {
    
    private Long imageId;
    private Long serviceRequestId;
    private String imageUrl;
    private String imageTypeCode;
    private String imageTypeName;
    private boolean active;
    private Long uploadById;
    private String uploadByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * ServiceRequestImage 엔티티를 DTO로 변환
     */
    public static ServiceRequestImageDTO fromEntity(ServiceRequestImage entity) {
        return ServiceRequestImageDTO.builder()
                .imageId(entity.getImageId())
                .serviceRequestId(entity.getServiceRequest().getServiceRequestId())
                .imageUrl(entity.getImageUrl())
                .imageTypeCode(entity.getImageType() != null ? entity.getImageType().getCodeId() : null)
                .imageTypeName(entity.getImageType() != null ? entity.getImageType().getCodeName() : null)
                .active(entity.isActive())
                .uploadById(entity.getUploadBy() != null ? entity.getUploadBy().getId() : null)
                .uploadByName(entity.getUploadBy() != null ? entity.getUploadBy().getUserName() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
} 