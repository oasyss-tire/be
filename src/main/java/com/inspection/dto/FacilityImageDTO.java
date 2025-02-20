package com.inspection.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class FacilityImageDTO {
    private Long id;
    private Long facilityId;
    private String imageUrl;
    private String description;
    private String url;  // getUrl() 메서드 호출을 위해 필요
} 