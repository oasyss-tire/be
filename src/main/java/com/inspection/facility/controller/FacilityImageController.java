package com.inspection.facility.controller;

import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.inspection.facility.dto.FacilityImageDTO;
import com.inspection.facility.service.FacilityImageService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/facility-images")
@RequiredArgsConstructor
public class FacilityImageController {

    private final FacilityImageService facilityImageService;
    
    /**
     * 시설물 이미지 목록 조회
     */
    @GetMapping("/facility/{facilityId}")
    public ResponseEntity<List<FacilityImageDTO>> getFacilityImages(@PathVariable Long facilityId) {

        List<FacilityImageDTO> images = facilityImageService.getFacilityImages(facilityId);
        return ResponseEntity.ok(images);
    }
    
    /**
     * 시설물 이미지 상세 조회
     */
    @GetMapping("/{imageId}")
    public ResponseEntity<FacilityImageDTO> getFacilityImage(@PathVariable Long imageId) {

        FacilityImageDTO image = facilityImageService.getFacilityImage(imageId);
        return ResponseEntity.ok(image);
    }
    
    /**
     * 시설물 이미지 다운로드
     */
    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFacilityImage(@PathVariable String fileName, HttpServletRequest request) {

        
        Resource resource = facilityImageService.loadFacilityImageFile(fileName);
        
        // 콘텐츠 타입 결정
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (Exception e) {
            log.warn("시설물 이미지 파일 타입을 결정할 수 없습니다: {}", fileName);
        }
        
        // 기본 콘텐츠 타입
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
    
    /**
     * 시설물 이미지 업로드
     */
    @PostMapping("/facility/{facilityId}")
    public ResponseEntity<FacilityImageDTO> uploadFacilityImage(
            @PathVariable Long facilityId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "imageTypeCode", defaultValue = "IMG_TYPE_01") String imageTypeCode,
            @RequestParam(value = "uploadBy", required = false) String uploadBy) {

        
        if (file.isEmpty()) {
            log.warn("시설물 이미지 업로드 실패 - 빈 파일");
            return ResponseEntity.badRequest().build();
        }
        
        FacilityImageDTO uploadedImage = facilityImageService.uploadFacilityImage(facilityId, file, imageTypeCode, uploadBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(uploadedImage);
    }
    
    /**
     * 시설물 이미지 수정
     */
    @PutMapping("/{imageId}")
    public ResponseEntity<FacilityImageDTO> updateFacilityImage(
            @PathVariable Long imageId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "imageTypeCode", required = false) String imageTypeCode,
            @RequestParam(value = "updatedBy", required = false) String updatedBy) {

        
        FacilityImageDTO updatedImage = facilityImageService.updateFacilityImage(imageId, file, imageTypeCode, updatedBy);
        return ResponseEntity.ok(updatedImage);
    }
    
    /**
     * 시설물 이미지 삭제
     */
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteFacilityImage(@PathVariable Long imageId) {

        facilityImageService.deleteFacilityImage(imageId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 시설물의 모든 이미지 삭제
     */
    @DeleteMapping("/facility/{facilityId}")
    public ResponseEntity<Void> deleteAllFacilityImages(@PathVariable Long facilityId) {

        facilityImageService.deleteAllFacilityImages(facilityId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 모든 시설물의 정면 이미지 조회 (썸네일용)
     */
    @GetMapping("/front")
    public ResponseEntity<List<FacilityImageDTO>> getAllFrontImages() {

        List<FacilityImageDTO> frontImages = facilityImageService.getAllFrontImages();
        return ResponseEntity.ok(frontImages);
    }
    
    /**
     * 특정 시설물 ID 목록의 정면 이미지 조회 (썸네일용)
     */
    @PostMapping("/front/by-facilities")
    public ResponseEntity<Map<Long, FacilityImageDTO>> getFrontImagesByFacilityIds(
            @RequestBody List<Long> facilityIds) {

        Map<Long, FacilityImageDTO> frontImages = facilityImageService.getFrontImagesByFacilityIds(facilityIds);
        return ResponseEntity.ok(frontImages);
    }
    
    /**
     * 시설물 이미지 직접 조회 (인증 필요 없음)
     */
    @GetMapping("/view/{fileName:.+}")
    public ResponseEntity<Resource> viewFacilityImage(@PathVariable String fileName, HttpServletRequest request) {

        
        Resource resource = facilityImageService.loadFacilityImageFile(fileName);
        
        // 콘텐츠 타입 결정
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (Exception e) {
            log.warn("시설물 이미지 파일 타입을 결정할 수 없습니다: {}", fileName);
        }
        
        // 기본 콘텐츠 타입
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    /**
     * 시설물 QR 코드 생성
     */
    @PostMapping("/qrcode/facility/{facilityId}")
    public ResponseEntity<FacilityImageDTO> generateQrCode(@PathVariable Long facilityId) {
        log.info("시설물 ID {}에 대한 QR 코드 생성 요청", facilityId);
        FacilityImageDTO qrCodeImage = facilityImageService.generateAndSaveQrCode(facilityId);
        return ResponseEntity.ok(qrCodeImage);
    }
} 