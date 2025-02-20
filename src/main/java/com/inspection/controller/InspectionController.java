package com.inspection.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.LinkedHashSet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.inspection.dto.InspectionBoardDTO;
import com.inspection.dto.InspectionCreateDTO;
import com.inspection.dto.InspectionDetailDTO;
import com.inspection.exception.InspectionNotFoundException;
import com.inspection.service.InspectionService;
import com.inspection.service.UserService;
import com.inspection.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/inspections")
@RequiredArgsConstructor
@Slf4j
public class InspectionController {
    private final InspectionService inspectionService;
    private final UserService userService;
    
    /* 점검 내용 저장 */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createInspection(
        @RequestPart("inspectionData") String inspectionDataStr,
        @RequestPart(value = "images", required = false) List<MultipartFile> images,
        @AuthenticationPrincipal UserDetails userDetails
    ) throws IOException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            InspectionCreateDTO inspectionData = mapper.readValue(inspectionDataStr, InspectionCreateDTO.class);
            
            User user = userService.getCurrentUser(userDetails.getUsername());
            inspectionData.setUserId(user.getUserId());
            
            // 이미지 파일 저장 및 처리
            List<String> savedImageNames = new ArrayList<>();
            if (images != null && !images.isEmpty()) {
                // uploads/images 디렉토리가 존재하는지 확인하고 없으면 생성
                Path uploadPath = Paths.get("uploads/images");
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
            
            inspectionData.setImages(savedImageNames);
            Long inspectionId = inspectionService.createInspection(inspectionData);
            return ResponseEntity.ok(inspectionId);
        } catch (IOException e) {
            log.error("파일 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("파일 처리 중 오류가 발생했습니다: " + e.getMessage());
        } catch (InspectionNotFoundException e) {
            log.error("점검 기록을 찾을 수 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(e.getMessage());
        } catch (RuntimeException e) {
            log.error("점검 데이터 저장 중 서버 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("서버 오류가 발생했습니다.");
        }
    }

    /* 점검 내용 조회 (전체조회/페이징) */
    @GetMapping
    public ResponseEntity<Page<InspectionBoardDTO>> getInspections(Pageable pageable) {
        Page<InspectionBoardDTO> inspections = inspectionService.getInspections(pageable);
        return ResponseEntity.ok(inspections);
    }

    /* 점검 내용 상세조회 */
    @GetMapping("/{id}/detail")
    public ResponseEntity<InspectionDetailDTO> getInspectionDetail(@PathVariable Long id) {
        try {
            InspectionDetailDTO detailDTO = inspectionService.getInspectionDetail(id);
            return ResponseEntity.ok(detailDTO);
        } catch (InspectionNotFoundException e) {
            throw e;
        }
    }


    @GetMapping("/company/{companyId}")
    public ResponseEntity<Page<InspectionBoardDTO>> getInspectionsByCompany(
        @PathVariable Long companyId, 
        Pageable pageable
    ) {
        Page<InspectionBoardDTO> inspections = inspectionService.getInspectionsByCompany(companyId, pageable);
        return ResponseEntity.ok(inspections);
    }

    @PostMapping("/{id}/manager-signature")
    @PreAuthorize("hasRole('ADMIN')")
    public InspectionDetailDTO saveManagerSignature(
        @PathVariable Long id,
        @RequestBody Map<String, String> payload
    ) {
        String signature = payload.get("signature");
        return inspectionService.saveManagerSignature(id, signature);
    }

    // @GetMapping("/{id}/pdf")
    // public ResponseEntity<Resource> downloadPdf(@PathVariable Long id) {
    //     try {
    //         byte[] pdfBytes = pdfService.generateInspectionPdf(id);
    //         ByteArrayResource resource = new ByteArrayResource(pdfBytes);
            
    //         return ResponseEntity.ok()
    //             .header(HttpHeaders.CONTENT_DISPOSITION, 
    //                 "attachment; filename=inspection_" + id + ".pdf")
    //             .contentType(MediaType.APPLICATION_PDF)
    //             .body(resource);
    //     } catch (Exception e) {
    //         throw new RuntimeException("PDF 다운로드 중 오류 발생", e);
    //     }
    // }


    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateInspection(
        @PathVariable Long id,
        @RequestPart("inspectionData") String inspectionDataStr,
        @RequestPart(value = "images", required = false) List<MultipartFile> newImages
    ) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            InspectionCreateDTO inspectionData = mapper.readValue(inspectionDataStr, InspectionCreateDTO.class);
            
            // LinkedHashSet을 사용하여 중복 제거하면서 순서 유지
            Set<String> uniqueImages = new LinkedHashSet<>();
            
            // 기존 이미지 처리
            if (inspectionData.getImages() != null) {
                uniqueImages.addAll(inspectionData.getImages());
            }
            
            // 새 이미지 처리
            if (newImages != null && !newImages.isEmpty()) {
                Path uploadPath = Paths.get("uploads/images");
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                
                for (MultipartFile image : newImages) {
                    String fileName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
                    Path path = uploadPath.resolve(fileName);
                    
                    // 파일이 이미 존재하는지 확인
                    if (!Files.exists(path)) {
                        Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                        uniqueImages.add(fileName);
                    }
                }
            }
            
            // 중복이 제거된 이미지 목록을 다시 리스트로 변환
            inspectionData.setImages(new ArrayList<>(uniqueImages));
            
            // 디버깅을 위한 로그
            log.info("Updating inspection {} with images: {}", id, uniqueImages);
            
            InspectionDetailDTO updatedInspection = inspectionService.updateInspection(id, inspectionData);
            return ResponseEntity.ok(updatedInspection);
        } catch (IOException e) {
            log.error("파일 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("파일 처리 중 오류가 발생했습니다: " + e.getMessage());
        } catch (InspectionNotFoundException e) {
            log.error("점검 기록을 찾을 수 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(e.getMessage());
        } catch (RuntimeException e) {
            log.error("점검 데이터 수정 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("점검 데이터 수정에 실패했습니다: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")  // 관리자만 삭제 가능
    public ResponseEntity<?> deleteInspection(@PathVariable Long id) {
        try {
            inspectionService.deleteInspection(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("점검 데이터 삭제 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("점검 데이터 삭제에 실패했습니다: " + e.getMessage());
        }
    }
} 