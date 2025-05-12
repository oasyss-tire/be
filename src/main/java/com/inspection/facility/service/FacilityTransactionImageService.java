package com.inspection.facility.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.inspection.entity.Code;
import com.inspection.facility.dto.FacilityTransactionImageDTO;
import com.inspection.facility.entity.FacilityTransaction;
import com.inspection.facility.entity.FacilityTransactionImage;
import com.inspection.facility.repository.FacilityTransactionImageRepository;
import com.inspection.facility.repository.FacilityTransactionRepository;
import com.inspection.repository.CodeRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacilityTransactionImageService {

    private final FacilityTransactionRepository transactionRepository;
    private final FacilityTransactionImageRepository transactionImageRepository;
    private final CodeRepository codeRepository;
    
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;
    
    /**
     * 트랜잭션 이미지 목록 조회
     */
    @Transactional(readOnly = true)
    public List<FacilityTransactionImageDTO> getTransactionImages(Long transactionId) {
        List<FacilityTransactionImage> images = transactionImageRepository.findByTransactionTransactionIdAndActiveTrue(transactionId);
        return images.stream()
                .map(FacilityTransactionImageDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 트랜잭션 이미지 상세 조회
     */
    @Transactional(readOnly = true)
    public FacilityTransactionImageDTO getTransactionImage(Long imageId) {
        FacilityTransactionImage image = transactionImageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("트랜잭션 이미지를 찾을 수 없습니다: " + imageId));
        
        return FacilityTransactionImageDTO.fromEntity(image);
    }
    
    /**
     * 트랜잭션 이미지 업로드
     */
    @Transactional
    public FacilityTransactionImageDTO uploadTransactionImage(Long transactionId, MultipartFile file, String imageTypeCode, String uploadBy) {
        try {
            // 트랜잭션 조회
            FacilityTransaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new EntityNotFoundException("트랜잭션을 찾을 수 없습니다: " + transactionId));
            
            // 이미지 유형 코드 조회
            Code imageType = null;
            if (StringUtils.hasText(imageTypeCode)) {
                imageType = codeRepository.findById(imageTypeCode)
                        .orElseThrow(() -> new EntityNotFoundException("이미지 유형 코드를 찾을 수 없습니다: " + imageTypeCode));
            }
            
            // 파일 저장 경로 설정
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = "tx_" + transactionId + "_" + timestamp + "_" + UUID.randomUUID().toString() + fileExtension;
            
            // 디렉토리 생성
            String uploadPath = uploadDir + "/facility-transaction";
            Path uploadLocation = Paths.get(uploadPath).toAbsolutePath().normalize();
            Files.createDirectories(uploadLocation);
            
            // 파일 저장
            Path targetLocation = uploadLocation.resolve(newFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            // 이미지 URL 생성
            String imageUrl = "/facility-transaction/" + newFilename;
            
            // 이미지 엔티티 생성 및 저장
            FacilityTransactionImage image = FacilityTransactionImage.builder()
                    .transaction(transaction)
                    .imageUrl(imageUrl)
                    .imageType(imageType)
                    .active(true)
                    .uploadBy(uploadBy)
                    .build();
            
            FacilityTransactionImage savedImage = transactionImageRepository.save(image);
            
            return FacilityTransactionImageDTO.fromEntity(savedImage);
        } catch (IOException e) {
            log.error("트랜잭션 이미지 업로드 중 오류 발생", e);
            throw new RuntimeException("트랜잭션 이미지 업로드 실패: " + e.getMessage());
        }
    }
    
    /**
     * 트랜잭션 이미지 수정
     */
    @Transactional
    public FacilityTransactionImageDTO updateTransactionImage(Long imageId, MultipartFile file, String imageTypeCode, String updatedBy) {
        try {
            // 이미지 조회
            FacilityTransactionImage image = transactionImageRepository.findById(imageId)
                    .orElseThrow(() -> new EntityNotFoundException("트랜잭션 이미지를 찾을 수 없습니다: " + imageId));
            
            // 이미지 유형 코드 수정 (제공된 경우)
            if (StringUtils.hasText(imageTypeCode)) {
                Code imageType = codeRepository.findById(imageTypeCode)
                        .orElseThrow(() -> new EntityNotFoundException("이미지 유형 코드를 찾을 수 없습니다: " + imageTypeCode));
                image.setImageType(imageType);
            }
            
            // 파일 수정 (제공된 경우)
            if (file != null && !file.isEmpty()) {
                // 기존 파일 삭제
                String existingImageUrl = image.getImageUrl();
                if (existingImageUrl != null && existingImageUrl.startsWith("/facility-transaction/")) {
                    String existingFilename = existingImageUrl.substring(existingImageUrl.lastIndexOf("/") + 1);
                    Path existingFilePath = Paths.get(uploadDir + "/facility-transaction/" + existingFilename);
                    Files.deleteIfExists(existingFilePath);
                }
                
                // 새 파일 저장
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
                String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String newFilename = "tx_" + image.getTransaction().getTransactionId() + "_" + timestamp + "_" + UUID.randomUUID().toString() + fileExtension;
                
                // 디렉토리 생성
                String uploadPath = uploadDir + "/facility-transaction";
                Path uploadLocation = Paths.get(uploadPath).toAbsolutePath().normalize();
                Files.createDirectories(uploadLocation);
                
                // 파일 저장
                Path targetLocation = uploadLocation.resolve(newFilename);
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
                
                // 이미지 URL 업데이트
                image.setImageUrl("/facility-transaction/" + newFilename);
            }
            
            // 업데이트 정보 설정
            if (StringUtils.hasText(updatedBy)) {
                image.setUploadBy(updatedBy);
            }
            
            // 저장 및 반환
            FacilityTransactionImage updatedImage = transactionImageRepository.save(image);
            return FacilityTransactionImageDTO.fromEntity(updatedImage);
        } catch (IOException e) {
            log.error("트랜잭션 이미지 수정 중 오류 발생", e);
            throw new RuntimeException("트랜잭션 이미지 수정 실패: " + e.getMessage());
        }
    }
    
    /**
     * 트랜잭션 이미지 삭제
     */
    @Transactional
    public void deleteTransactionImage(Long imageId) {
        try {
            // 이미지 조회
            FacilityTransactionImage image = transactionImageRepository.findById(imageId)
                    .orElseThrow(() -> new EntityNotFoundException("트랜잭션 이미지를 찾을 수 없습니다: " + imageId));
            
            // 파일 삭제
            String imageUrl = image.getImageUrl();
            if (imageUrl != null && imageUrl.startsWith("/facility-transaction/")) {
                String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                Path filePath = Paths.get(uploadDir + "/facility-transaction/" + filename);
                Files.deleteIfExists(filePath);
            }
            
            // 이미지 엔티티 삭제
            transactionImageRepository.delete(image);
        } catch (IOException e) {
            log.error("트랜잭션 이미지 삭제 중 오류 발생", e);
            throw new RuntimeException("트랜잭션 이미지 삭제 실패: " + e.getMessage());
        }
    }
    
    /**
     * 트랜잭션의 모든 이미지 삭제
     */
    @Transactional
    public void deleteAllTransactionImages(Long transactionId) {
        List<FacilityTransactionImage> images = transactionImageRepository.findByTransactionTransactionIdAndActiveTrue(transactionId);
        
        for (FacilityTransactionImage image : images) {
            try {
                // 파일 삭제
                String imageUrl = image.getImageUrl();
                if (imageUrl != null && imageUrl.startsWith("/facility-transaction/")) {
                    String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                    Path filePath = Paths.get(uploadDir + "/facility-transaction/" + filename);
                    Files.deleteIfExists(filePath);
                }
            } catch (IOException e) {
                log.error("트랜잭션 이미지 파일 삭제 중 오류 발생: {}", e.getMessage());
            }
        }
        
        // 이미지 엔티티 일괄 삭제
        transactionImageRepository.deleteByTransactionTransactionId(transactionId);
    }
    
    /**
     * 이미지 파일 로드
     */
    public Resource loadTransactionImageFile(String fileName) {
        try {
            Path filePath = Paths.get(uploadDir + "/facility-transaction/" + fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("트랜잭션 이미지 파일을 찾을 수 없습니다: " + fileName);
            }
        } catch (Exception e) {
            throw new RuntimeException("트랜잭션 이미지 파일 로드 실패: " + e.getMessage());
        }
    }
}
