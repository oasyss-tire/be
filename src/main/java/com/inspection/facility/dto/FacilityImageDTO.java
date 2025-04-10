package com.inspection.facility.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilityImageDTO {
    
    private Long imageId;
    private String imageUrl;
    private String imageTypeCode;
    private String imageTypeName;
    private Long facilityId;
    private boolean active;
    private String uploadBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 