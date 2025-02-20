package com.inspection.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.inspection.service.FireSafetyInspectionService;
import com.inspection.dto.FireSafetyInspectionDTO;
import com.inspection.dto.FireSafetyInspectionCreateDTO;
import com.inspection.dto.FireSafetyInspectionUpdateDTO;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.io.IOException;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.Paths;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;


@RestController
@RequestMapping("/api/fire-inspections")
@RequiredArgsConstructor
@Slf4j
public class FireSafetyInspectionController {
    
    private final FireSafetyInspectionService fireSafetyInspectionService;

    @GetMapping
    public ResponseEntity<List<FireSafetyInspectionDTO>> getAllInspections() {
        return ResponseEntity.ok(fireSafetyInspectionService.getAllInspections());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FireSafetyInspectionDTO> getInspection(@PathVariable Long id) {
        return ResponseEntity.ok(fireSafetyInspectionService.getInspectionById(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createInspection(
        @RequestPart("inspectionData") String inspectionDataStr,
        @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            FireSafetyInspectionCreateDTO inspectionData = mapper.readValue(inspectionDataStr, FireSafetyInspectionCreateDTO.class);
            
            // 이미지 파일 저장 및 처리
            List<String> savedImageNames = new ArrayList<>();
            if (images != null && !images.isEmpty()) {
                // uploads/fire-safety-images 디렉토리 생성
                Path uploadPath = Paths.get("uploads/fire-safety-images");
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                
                for (MultipartFile image : images) {
                    String fileName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
                    Path path = uploadPath.resolve(fileName);
                    Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                    savedImageNames.add(fileName);
                }
            }
            
            inspectionData.setAttachments(savedImageNames);  // toString() 제거
            return ResponseEntity.ok(fireSafetyInspectionService.createInspection(inspectionData));
            
        } catch (IOException e) {
            log.error("파일 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("파일 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<FireSafetyInspectionDTO>> getInspectionsByCompany(
        @PathVariable Long companyId
    ) {
        return ResponseEntity.ok(fireSafetyInspectionService.getInspectionsByCompany(companyId));
    }

    @GetMapping("/writer/{userId}")
    public ResponseEntity<List<FireSafetyInspectionDTO>> getInspectionsByWriter(
        @PathVariable Long userId
    ) {
        return ResponseEntity.ok(fireSafetyInspectionService.getInspectionsByWriter(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInspection(@PathVariable Long id) {
        fireSafetyInspectionService.deleteInspection(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateInspection(
        @PathVariable Long id,
        @RequestPart("inspectionData") String inspectionDataStr,
        @RequestPart(value = "images", required = false) List<MultipartFile> newImages
    ) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            FireSafetyInspectionUpdateDTO inspectionData = mapper.readValue(inspectionDataStr, FireSafetyInspectionUpdateDTO.class);
            
            // LinkedHashSet을 사용하여 중복 제거하면서 순서 유지
            Set<String> uniqueImages = new LinkedHashSet<>();
            
            // 기존 이미지 처리
            if (inspectionData.getAttachments() != null) {
                uniqueImages.addAll(inspectionData.getAttachments());
            }
            
            // 새 이미지 처리
            if (newImages != null && !newImages.isEmpty()) {
                Path uploadPath = Paths.get("uploads/fire-safety-images");
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                
                for (MultipartFile image : newImages) {
                    String fileName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
                    Path path = uploadPath.resolve(fileName);
                    
                    if (!Files.exists(path)) {
                        Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                        uniqueImages.add(fileName);
                    }
                }
            }
            
            inspectionData.setAttachments(new ArrayList<>(uniqueImages));
            log.info("Updating inspection {} with images: {}", id, uniqueImages);
            
            return ResponseEntity.ok(fireSafetyInspectionService.updateInspection(id, inspectionData));
        } catch (IOException e) {
            log.error("파일 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("파일 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/manager-signature")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FireSafetyInspectionDTO> saveManagerSignature(
        @PathVariable Long id,
        @RequestBody Map<String, String> payload
    ) {
        String signature = payload.get("signature");
        return ResponseEntity.ok(fireSafetyInspectionService.saveManagerSignature(id, signature));
    }
} 