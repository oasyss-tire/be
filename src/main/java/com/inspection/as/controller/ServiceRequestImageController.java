package com.inspection.as.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.inspection.as.dto.ServiceRequestImageDTO;
import com.inspection.as.service.ServiceRequestImageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/service-request-images")
@RequiredArgsConstructor
public class ServiceRequestImageController {
    
    private final ServiceRequestImageService imageService;
    
    /**
     * AS 접수 ID로 이미지 조회
     */
    @GetMapping("/by-service-request/{serviceRequestId}")
    public ResponseEntity<List<ServiceRequestImageDTO>> getImagesByServiceRequestId(
            @PathVariable Long serviceRequestId) {
        List<ServiceRequestImageDTO> images = imageService.getImagesByServiceRequestId(serviceRequestId);
        return ResponseEntity.ok(images);
    }
    
    /**
     * AS 접수 ID와 이미지 유형으로 이미지 조회
     */
    @GetMapping("/by-service-request/{serviceRequestId}/by-type")
    public ResponseEntity<List<ServiceRequestImageDTO>> getImagesByServiceRequestIdAndType(
            @PathVariable Long serviceRequestId,
            @RequestParam String imageTypeCode) {
        List<ServiceRequestImageDTO> images = imageService.getImagesByServiceRequestIdAndType(serviceRequestId, imageTypeCode);
        return ResponseEntity.ok(images);
    }
    
    /**
     * 이미지 삭제 (실제로는 비활성화)
     */
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long imageId) {
        imageService.deleteImage(imageId);
        return ResponseEntity.noContent().build();
    }
} 