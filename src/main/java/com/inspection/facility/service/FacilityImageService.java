package com.inspection.facility.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    
    /**
     * 시설물 QR 코드 이미지 생성 및 저장
     */
    @Transactional
    public FacilityImageDTO generateAndSaveQrCode(Long facilityId) {
        log.info("시설물 ID {}에 대한 QR 코드 생성 시작", facilityId);
        
        try {
            // 시설물 조회
            Facility facility = facilityRepository.findById(facilityId)
                    .orElseThrow(() -> new EntityNotFoundException("시설물을 찾을 수 없습니다: " + facilityId));
            
            // 이미지 타입 조회 (QR 코드 타입)
            String qrCodeTypeCode = "002005_0005"; // QR 코드 타입 코드
            Code qrCodeType = codeRepository.findById(qrCodeTypeCode)
                    .orElseThrow(() -> new EntityNotFoundException("QR 코드 이미지 타입을 찾을 수 없습니다: " + qrCodeTypeCode));
            
            // 해당 시설물에 이미 QR 코드가 있는지 확인
            List<Image> existingQrCodes = imageRepository.findByFacilityIdInAndImageTypeCodeAndActiveTrue(
                    List.of(facilityId), qrCodeTypeCode);
            
            if (!existingQrCodes.isEmpty()) {
                log.info("시설물 ID {}에 대한 QR 코드가 이미 존재합니다. 기존 QR 코드를 반환합니다.", facilityId);
                return convertToDTO(existingQrCodes.get(0));
            }
            
            // QR 코드 URL 생성 (상세 페이지 URL)
            String facilityDetailUrl = "https://tirebank.jebee.net//facility-detail/" + facilityId;
            String encodedUrl = URLEncoder.encode(facilityDetailUrl, StandardCharsets.UTF_8.toString());
            String qrApiUrl = "https://quickchart.io/qr?text=" + encodedUrl;
            
            log.debug("QR 코드 API URL: {}", qrApiUrl);
            
            // QR 이미지 다운로드
            URL url = new URL(qrApiUrl);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            // 현재 날짜 및 시간을 한국식 형식으로 변환 (예: 20230801_153045)
            String dateTimeFormat = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            
            // 시설물 관리번호 가져오기 (없는 경우 ID 사용)
            String managementNumber = facility.getManagementNumber();
            if (managementNumber == null || managementNumber.isEmpty()) {
                managementNumber = "ID" + facilityId;
            } else {
                // 관리번호에 있을 수 있는 특수문자 제거 (파일명에 사용 불가한 문자 제거)
                managementNumber = managementNumber.replaceAll("[\\\\/:*?\"<>|]", "_");
            }
            
            // 파일명 생성 (관리번호_날짜시간.png)
            String fileName = "QR_" + managementNumber + "_" + dateTimeFormat + ".png";
            Path targetLocation = this.storageLocation.resolve(fileName);
            
            // 이미지 저장
            try (InputStream inputStream = connection.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }
            
            log.info("QR 코드 이미지가 저장되었습니다: {}", targetLocation);
            
            // 이미지 엔티티 생성 및 저장 (서브폴더 경로 없이 파일명만 저장)
            Image qrImage = Image.builder()
                    .facility(facility)
                    .imageType(qrCodeType)
                    .imageUrl(fileName)
                    .active(true)
                    .uploadBy("SYSTEM")
                    .build();
            
            Image savedImage = imageRepository.save(qrImage);
            log.info("QR 코드 이미지 정보가 DB에 저장되었습니다. 이미지 ID: {}", savedImage.getImageId());
            
            return convertToDTO(savedImage);
            
        } catch (Exception e) {
            log.error("QR 코드 생성 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("QR 코드 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
} 