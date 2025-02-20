package com.inspection.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.inspection.dto.FacilityDTO;
import com.inspection.dto.FacilityContractDTO;
import com.inspection.entity.FacilityStatus;
import com.inspection.service.FacilityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import com.inspection.dto.FacilityDetailDTO;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import com.inspection.service.ImageService;
import com.inspection.dto.FacilityImageDTO;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import com.inspection.dto.FacilityStatusHistoryDTO;

@RestController
@RequestMapping("/api/facilities")
@RequiredArgsConstructor
public class FacilityController {
    private final FacilityService facilityService;
    private final ImageService imageService;
    private static final Logger log = LoggerFactory.getLogger(FacilityController.class);
    
    // 시설물 등록
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FacilityDTO> registerFacility(
            @RequestPart("facilityDto") FacilityDTO dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        
        log.info("Received facilityDto: {}", dto);
        if (dto.getCompanyId() == null) {
            throw new IllegalArgumentException("Company ID must not be null");
        }
        
        FacilityDTO savedFacility = facilityService.registerFacility(dto);
        
        // 이미지가 있다면 업로드
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                facilityService.uploadImage(savedFacility.getId(), image, null);
            }
        }
        
        return ResponseEntity.ok(savedFacility);
    }
    
    // 시설물 납품/이동 계약 등록
    @PostMapping("/{facilityId}/contracts")
    public ResponseEntity<FacilityContractDTO> createContract(
            @PathVariable Long facilityId,
            @RequestBody FacilityContractDTO dto) {
        dto.setFacilityId(facilityId);
        return ResponseEntity.ok(facilityService.createContract(dto));
    }
    
    // 시설물 상태 변경
    @PutMapping("/{facilityId}/status")
    public ResponseEntity<FacilityDTO> updateStatus(
            @PathVariable Long facilityId,
            @RequestParam FacilityStatus status) {
        return ResponseEntity.ok(facilityService.updateStatus(facilityId, status));
    }

    // 시설물 목록 조회
    @GetMapping
    public ResponseEntity<Page<FacilityDTO>> getFacilities(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) FacilityStatus status,
            @RequestParam(required = false) String location,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(facilityService.getFacilities(keyword, status, location, pageable));
    }

    // 시설물 상세 조회
    @GetMapping("/{facilityId}")
    public ResponseEntity<FacilityDetailDTO> getFacilityDetail(@PathVariable Long facilityId) {
        return ResponseEntity.ok(facilityService.getFacilityDetail(facilityId));
    }

    // 시설물 정보 수정
    @PutMapping("/{facilityId}")
    public ResponseEntity<FacilityDTO> updateFacility(
            @PathVariable Long facilityId,
            @RequestBody FacilityDTO dto) {
        return ResponseEntity.ok(facilityService.updateFacility(facilityId, dto));
    }

    // 이미지 업로드
    @PostMapping("/{facilityId}/images")
    public ResponseEntity<FacilityImageDTO> uploadImage(
            @PathVariable Long facilityId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String description) {
        return ResponseEntity.ok(facilityService.uploadImage(facilityId, file, description));
    }

    // 이미지 파일 조회 (실제 이미지 반환)
    @GetMapping("/images/{fileName}")
    public ResponseEntity<Resource> getImage(@PathVariable String fileName) {
        try {
            log.info("Requesting image: {}", fileName);
            Resource resource = imageService.loadImage(fileName);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
                .body(resource);
        } catch (Exception e) {
            log.error("Image load failed: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // 이미지 삭제
    @DeleteMapping("/{facilityId}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long facilityId,
            @PathVariable Long imageId) {
        facilityService.deleteImage(facilityId, imageId);
        return ResponseEntity.ok().build();
    }

    // 이미지 정보 수정
    @PutMapping("/{facilityId}/images/{imageId}")
    public ResponseEntity<FacilityImageDTO> updateImage(
            @PathVariable Long facilityId,
            @PathVariable Long imageId,
            @RequestParam String description) {
        return ResponseEntity.ok(facilityService.updateImage(facilityId, imageId, description));
    }

    // 계약 정보 수정
    @PutMapping("/{facilityId}/contracts/{contractId}")
    public ResponseEntity<FacilityContractDTO> updateContract(
            @PathVariable Long facilityId,
            @PathVariable Long contractId,
            @RequestBody FacilityContractDTO dto) {
        return ResponseEntity.ok(facilityService.updateContract(facilityId, contractId, dto));
    }

    // 계약 삭제
    @DeleteMapping("/{facilityId}/contracts/{contractId}")
    public ResponseEntity<Void> deleteContract(
            @PathVariable Long facilityId,
            @PathVariable Long contractId) {
        facilityService.deleteContract(facilityId, contractId);
        return ResponseEntity.ok().build();
    }

    // 이미지 목록 조회 (메타데이터)
    @GetMapping("/{facilityId}/images")
    public ResponseEntity<List<FacilityImageDTO>> getFacilityImages(@PathVariable Long facilityId) {
        return ResponseEntity.ok(facilityService.getFacilityImages(facilityId));
    }

    // 상태 이력 조회 엔드포인트 추가
    @GetMapping("/{facilityId}/status-history")
    public ResponseEntity<List<FacilityStatusHistoryDTO>> getStatusHistory(@PathVariable Long facilityId) {
        return ResponseEntity.ok(facilityService.getStatusHistory(facilityId));
    }

    // 위치 변경
    @PutMapping("/{facilityId}/location")
    public ResponseEntity<FacilityDTO> updateLocation(
            @PathVariable Long facilityId,
            @RequestParam String location) {
        FacilityDTO dto = new FacilityDTO();
        dto.setCurrentLocation(location);
        return ResponseEntity.ok(facilityService.updateFacility(facilityId, dto));
    }
} 