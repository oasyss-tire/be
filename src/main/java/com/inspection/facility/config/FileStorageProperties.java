package com.inspection.facility.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "file")
@Getter @Setter
public class FileStorageProperties {
    
    private String uploadDir = "./uploads";
    
    @Getter @Setter
    private FacilityImage facilityImage = new FacilityImage();
    
    @Getter @Setter
    public static class FacilityImage {
        private String path = "./uploads/facility-images";
    }
    
    /**
     * 시설물 이미지 저장 경로를 반환합니다.
     */
    public String getFacilityImagesPath() {
        return facilityImage.getPath();
    }
} 