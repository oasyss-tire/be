package com.inspection.facility.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.inspection.entity.Code;
import com.inspection.facility.dto.FacilityImageDTO;
import com.inspection.facility.entity.Facility;
import com.inspection.facility.entity.Image;
import com.inspection.facility.repository.FacilityRepository;
import com.inspection.facility.repository.ImageRepository;
import com.inspection.repository.CodeRepository;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacilityImageService {
    
    private final ImageRepository imageRepository;
    private final FacilityRepository facilityRepository;
    private final CodeRepository codeRepository;
    
    @Value("${file.facility-image.path}")
    private String uploadPath;
    
    private Path storageLocation;
    
    @PostConstruct
    public void init() {
        try {
            this.storageLocation = Paths.get(uploadPath).toAbsolutePath().normalize();
            Files.createDirectories(this.storageLocation);
            log.info("시설물 이미지 저장 디렉토리 초기화 완료: {}", this.storageLocation);
        } catch (IOException e) {
            log.error("시설물 이미지 저장 디렉토리 생성 실패", e);
            throw new RuntimeException("시설물 이미지 저장 디렉토리를 생성할 수 없습니다.", e);
        }
    }
    
    /**
     * 시설물 이미지 목록 조회
     */
    @Transactional(readOnly = true)
    public List<FacilityImageDTO> getFacilityImages(Long facilityId) {
        if (!facilityRepository.existsById(facilityId)) {
            throw new EntityNotFoundException("시설물을 찾을 수 없습니다: " + facilityId);
        }
        
        return imageRepository.findByFacilityFacilityIdAndActiveTrue(facilityId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 시설물 이미지 상세 조회
     */
    @Transactional(readOnly = true)
    public FacilityImageDTO getFacilityImage(Long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("시설물 이미지를 찾을 수 없습니다: " + imageId));
        
        return convertToDTO(image);
    }
    
    /**
     * 시설물 이미지 파일 로드
     */
    public Resource loadFacilityImageFile(String filename) {
        try {
            Path filePath = this.storageLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("시설물 이미지 파일을 찾을 수 없습니다: " + filename);
            }
        } catch (IOException e) {
            throw new RuntimeException("시설물 이미지 파일을 로드할 수 없습니다: " + filename, e);
        }
    }
    
    /**
     * 시설물 이미지 업로드
     */
    @Transactional
    public FacilityImageDTO uploadFacilityImage(Long facilityId, MultipartFile file, String imageTypeCode, String uploadBy) {
        // 시설물 조회
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new EntityNotFoundException("시설물을 찾을 수 없습니다: " + facilityId));
        
        // 이미지 유형 코드 조회
        Code imageType = codeRepository.findById(imageTypeCode)
                .orElseThrow(() -> new EntityNotFoundException("이미지 유형 코드를 찾을 수 없습니다: " + imageTypeCode));
        
        // 파일 저장
        String fileName = storeFile(file);
        
        // 이미지 엔티티 생성 및 저장
        Image image = Image.builder()
            .facility(facility)
            .imageType(imageType)
            .imageUrl(fileName)
            .active(true)
            .uploadBy(uploadBy)
            .build();
        
        Image savedImage = imageRepository.save(image);
        log.info("시설물 이미지가 업로드되었습니다. 이미지 ID: {}, 파일명: {}", savedImage.getImageId(), fileName);
        
        return convertToDTO(savedImage);
    }
    
    /**
     * 시설물 이미지 수정
     */
    @Transactional
    public FacilityImageDTO updateFacilityImage(Long imageId, MultipartFile file, String imageTypeCode, String updatedBy) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("시설물 이미지를 찾을 수 없습니다: " + imageId));
        
        // 이미지 유형 수정
        if (imageTypeCode != null && !imageTypeCode.isEmpty()) {
            Code imageType = codeRepository.findById(imageTypeCode)
                    .orElseThrow(() -> new EntityNotFoundException("이미지 유형 코드를 찾을 수 없습니다: " + imageTypeCode));
            image.setImageType(imageType);
        }
        
        // 파일 수정
        if (file != null && !file.isEmpty()) {
            // 기존 파일 삭제
            deleteFileFromDisk(image.getImageUrl());
            
            // 새 파일 저장
            String fileName = storeFile(file);
            image.setImageUrl(fileName);
        }
        
        // 수정자 정보 업데이트
        if (updatedBy != null && !updatedBy.isEmpty()) {
            image.setUploadBy(updatedBy);
        }
        
        Image updatedImage = imageRepository.save(image);
        log.info("시설물 이미지가 수정되었습니다. 이미지 ID: {}", imageId);
        
        return convertToDTO(updatedImage);
    }
    
    /**
     * 시설물 이미지 삭제
     */
    @Transactional
    public void deleteFacilityImage(Long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("시설물 이미지를 찾을 수 없습니다: " + imageId));
        
        // 파일 시스템에서 이미지 삭제
        boolean deleted = deleteFileFromDisk(image.getImageUrl());
        
        if (deleted) {
            // DB에서 이미지 삭제
            imageRepository.delete(image);
            log.info("시설물 이미지가 삭제되었습니다. 이미지 ID: {}", imageId);
        } else {
            // 파일 삭제 실패 시 비활성화 처리
            image.setActive(false);
            imageRepository.save(image);
            log.warn("시설물 이미지 파일 삭제 실패, 비활성화 처리됨. 이미지 ID: {}", imageId);
        }
    }
    
    /**
     * 시설물ID에 연결된 모든 이미지 삭제
     */
    @Transactional
    public void deleteAllFacilityImages(Long facilityId) {
        List<Image> images = imageRepository.findByFacilityFacilityId(facilityId);
        
        for (Image image : images) {
            deleteFileFromDisk(image.getImageUrl());
        }
        
        imageRepository.deleteByFacilityFacilityId(facilityId);
        log.info("시설물의 모든 이미지가 삭제되었습니다. 시설물 ID: {}", facilityId);
    }
    
    /**
     * 파일을 저장하고 저장된 파일명을 반환
     */
    private String storeFile(MultipartFile file) {
        try {
            // 원본 파일명 정리
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            
            // 위험한 경로 체크
            if (originalFilename.contains("..")) {
                throw new RuntimeException("파일명에 잘못된 경로가 포함되어 있습니다: " + originalFilename);
            }
            
            // 파일명 충돌 방지를 위한 UUID 사용
            String fileExtension = getFileExtension(originalFilename);
            String newFilename = UUID.randomUUID().toString() + fileExtension;
            
            // 파일 저장
            Path targetLocation = this.storageLocation.resolve(newFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("시설물 이미지 파일 저장 완료: {}, 저장명: {}", originalFilename, newFilename);
            return newFilename;
        } catch (IOException ex) {
            log.error("시설물 이미지 파일 저장 실패", ex);
            throw new RuntimeException("시설물 이미지 파일을 저장할 수 없습니다.", ex);
        }
    }
    
    /**
     * 파일 삭제
     */
    private boolean deleteFileFromDisk(String filename) {
        try {
            Path filePath = this.storageLocation.resolve(filename);
            return Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            log.error("시설물 이미지 파일 삭제 실패: {}", filename, ex);
            return false;
        }
    }
    
    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(lastDotIndex);
        }
        return "";
    }
    
    /**
     * Entity -> DTO 변환
     */
    private FacilityImageDTO convertToDTO(Image image) {
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/facility-images/view/")
                .path(image.getImageUrl())
                .toUriString();
        
        return FacilityImageDTO.builder()
                .imageId(image.getImageId())
                .imageUrl(fileDownloadUri)
                .imageTypeCode(image.getImageType() != null ? image.getImageType().getCodeId() : null)
                .imageTypeName(image.getImageType() != null ? image.getImageType().getCodeName() : null)
                .facilityId(image.getFacility() != null ? image.getFacility().getFacilityId() : null)
                .active(image.isActive())
                .uploadBy(image.getUploadBy())
                .createdAt(image.getCreatedAt())
                .updatedAt(image.getUpdatedAt())
                .build();
    }
    
    /**
     * 모든 시설물의 정면 이미지만 조회 (썸네일용)
     */
    @Transactional(readOnly = true)
    public List<FacilityImageDTO> getAllFrontImages() {
        // 정면 이미지 타입 코드 (002005_0001)
        String frontImageTypeCode = "002005_0001";
        
        // 정면 이미지 타입 코드가 있는지 확인
        Code frontImageType = codeRepository.findById(frontImageTypeCode)
                .orElseThrow(() -> new EntityNotFoundException("정면 이미지 유형 코드를 찾을 수 없습니다: " + frontImageTypeCode));
        
        // 활성화된 모든 시설물 정면 이미지 조회
        List<Image> frontImages = imageRepository.findByImageTypeCodeIdAndActiveTrue(frontImageTypeCode);
        
        return frontImages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 특정 시설물들의 정면 이미지만 조회 (썸네일용)
     */
    @Transactional(readOnly = true)
    public Map<Long, FacilityImageDTO> getFrontImagesByFacilityIds(List<Long> facilityIds) {
        // 정면 이미지 타입 코드 (002005_0001)
        String frontImageTypeCode = "002005_0001";
        
        // 입력된 시설물 ID들에 대한 정면 이미지 조회
        List<Image> frontImages = imageRepository.findByFacilityIdInAndImageTypeCodeAndActiveTrue(
                facilityIds, frontImageTypeCode);
        
        // 시설물 ID를 키로, 이미지 정보를 값으로 하는 맵 생성
        Map<Long, FacilityImageDTO> result = new HashMap<>();
        for (Image image : frontImages) {
            Long facilityId = image.getFacility().getFacilityId();
            if (!result.containsKey(facilityId)) {
                result.put(facilityId, convertToDTO(image));
            }
        }
        
        return result;
    }
} 