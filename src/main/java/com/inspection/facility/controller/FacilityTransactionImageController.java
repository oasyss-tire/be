package com.inspection.facility.controller;

import java.util.List;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.inspection.facility.dto.FacilityTransactionImageDTO;
import com.inspection.facility.service.FacilityTransactionImageService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/facility-transaction-images")
@RequiredArgsConstructor
public class FacilityTransactionImageController {

    private final FacilityTransactionImageService transactionImageService;
    
    /**
     * 트랜잭션 이미지 목록 조회
     */
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<List<FacilityTransactionImageDTO>> getTransactionImages(@PathVariable Long transactionId) {
        List<FacilityTransactionImageDTO> images = transactionImageService.getTransactionImages(transactionId);
        return ResponseEntity.ok(images);
    }
    
    /**
     * 트랜잭션 이미지 상세 조회
     */
    @GetMapping("/{imageId}")
    public ResponseEntity<FacilityTransactionImageDTO> getTransactionImage(@PathVariable Long imageId) {
        FacilityTransactionImageDTO image = transactionImageService.getTransactionImage(imageId);
        return ResponseEntity.ok(image);
    }
    
    /**
     * 트랜잭션 이미지 다운로드
     */
    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadTransactionImage(@PathVariable String fileName, HttpServletRequest request) {
        Resource resource = transactionImageService.loadTransactionImageFile(fileName);
        
        // 콘텐츠 타입 결정
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (Exception e) {
            log.warn("트랜잭션 이미지 파일 타입을 결정할 수 없습니다: {}", fileName);
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
     * 트랜잭션 이미지 업로드
     */
    @PostMapping("/transaction/{transactionId}")
    public ResponseEntity<FacilityTransactionImageDTO> uploadTransactionImage(
            @PathVariable Long transactionId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "imageTypeCode", defaultValue = "IMG_TYPE_01") String imageTypeCode,
            @RequestParam(value = "uploadBy", required = false) String uploadBy) {
        
        if (file.isEmpty()) {
            log.warn("트랜잭션 이미지 업로드 실패 - 빈 파일");
            return ResponseEntity.badRequest().build();
        }
        
        FacilityTransactionImageDTO uploadedImage = transactionImageService.uploadTransactionImage(transactionId, file, imageTypeCode, uploadBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(uploadedImage);
    }
    
    /**
     * 트랜잭션 이미지 수정
     */
    @PutMapping("/{imageId}")
    public ResponseEntity<FacilityTransactionImageDTO> updateTransactionImage(
            @PathVariable Long imageId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "imageTypeCode", required = false) String imageTypeCode,
            @RequestParam(value = "updatedBy", required = false) String updatedBy) {
        
        FacilityTransactionImageDTO updatedImage = transactionImageService.updateTransactionImage(imageId, file, imageTypeCode, updatedBy);
        return ResponseEntity.ok(updatedImage);
    }
    
    /**
     * 트랜잭션 이미지 삭제
     */
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteTransactionImage(@PathVariable Long imageId) {
        transactionImageService.deleteTransactionImage(imageId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 트랜잭션의 모든 이미지 삭제
     */
    @DeleteMapping("/transaction/{transactionId}")
    public ResponseEntity<Void> deleteAllTransactionImages(@PathVariable Long transactionId) {
        transactionImageService.deleteAllTransactionImages(transactionId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 트랜잭션 이미지 직접 조회 (인증 필요 없음)
     */
    @GetMapping("/view/{fileName:.+}")
    public ResponseEntity<Resource> viewTransactionImage(@PathVariable String fileName, HttpServletRequest request) {
        Resource resource = transactionImageService.loadTransactionImageFile(fileName);
        
        // 콘텐츠 타입 결정
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (Exception e) {
            log.warn("트랜잭션 이미지 파일 타입을 결정할 수 없습니다: {}", fileName);
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
}
