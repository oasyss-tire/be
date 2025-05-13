package com.inspection.as.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.inspection.as.dto.ServiceRequestImageDTO;
import com.inspection.as.entity.ServiceRequest;
import com.inspection.as.entity.ServiceRequestImage;
import com.inspection.as.repository.ServiceRequestImageRepository;
import com.inspection.as.repository.ServiceRequestRepository;
import com.inspection.entity.Code;
import com.inspection.entity.User;
import com.inspection.repository.CodeRepository;
import com.inspection.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceRequestImageService {
    
    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceRequestImageRepository serviceRequestImageRepository;
    private final CodeRepository codeRepository;
    private final UserRepository userRepository;
    
    @Value("${file.service-request-image.path:./uploads/service-request-images}")
    private String uploadDir;
    
    /**
     * AS 접수 이미지 업로드
     */
    @Transactional
    public List<ServiceRequestImageDTO> uploadImages(Long serviceRequestId, List<MultipartFile> files, String imageTypeCode, String userId) {
        ServiceRequest serviceRequest = serviceRequestRepository.findById(serviceRequestId)
                .orElseThrow(() -> new EntityNotFoundException("AS 접수를 찾을 수 없습니다: " + serviceRequestId));
        
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        
        Code imageType = codeRepository.findById(imageTypeCode)
                .orElseThrow(() -> new EntityNotFoundException("이미지 유형 코드를 찾을 수 없습니다: " + imageTypeCode));
        
        List<ServiceRequestImage> savedImages = new ArrayList<>();
        
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                try {
                    String fileName = saveFile(file);
                    String imageUrl = "/service-request-images/" + fileName;
                    
                    ServiceRequestImage image = ServiceRequestImage.builder()
                            .serviceRequest(serviceRequest)
                            .imageUrl(imageUrl)
                            .imageType(imageType)
                            .active(true)
                            .uploadBy(user)
                            .build();
                    
                    savedImages.add(serviceRequestImageRepository.save(image));
                } catch (IOException e) {
                    log.error("이미지 저장 중 오류 발생: {}", e.getMessage(), e);
                    throw new RuntimeException("이미지 저장 중 오류가 발생했습니다.", e);
                }
            }
        }
        
        return savedImages.stream()
                .map(ServiceRequestImageDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 파일 저장 및 파일명 반환
     */
    public String saveFile(MultipartFile file) throws IOException {
        // 업로드 디렉토리 생성
        File uploadDirectory = new File(uploadDir);
        if (!uploadDirectory.exists()) {
            uploadDirectory.mkdirs();
        }
        
        // 현재 날짜 기반으로 하위 디렉토리 생성
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        File dateDirectory = new File(uploadDirectory, dateStr);
        if (!dateDirectory.exists()) {
            dateDirectory.mkdirs();
        }
        
        // 파일명 생성 (UUID + 원본 파일명)
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        if (originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + extension;
        
        // 파일 저장 경로 생성
        Path targetLocation = Paths.get(dateDirectory.getAbsolutePath(), fileName);
        
        // 파일 저장
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        
        return dateStr + "/" + fileName;
    }
    
    /**
     * AS 접수 ID로 이미지 조회
     */
    @Transactional(readOnly = true)
    public List<ServiceRequestImageDTO> getImagesByServiceRequestId(Long serviceRequestId) {
        List<ServiceRequestImage> images = serviceRequestImageRepository.findByServiceRequestServiceRequestIdAndActiveTrue(serviceRequestId);
        
        return images.stream()
                .map(ServiceRequestImageDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * AS 접수 ID와 이미지 유형으로 이미지 조회
     */
    @Transactional(readOnly = true)
    public List<ServiceRequestImageDTO> getImagesByServiceRequestIdAndType(Long serviceRequestId, String imageTypeCode) {
        List<ServiceRequestImage> images = serviceRequestImageRepository.findByServiceRequestServiceRequestIdAndImageTypeCodeIdAndActiveTrue(
                serviceRequestId, imageTypeCode);
        
        return images.stream()
                .map(ServiceRequestImageDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 이미지 삭제 (실제로는 비활성화)
     */
    @Transactional
    public void deleteImage(Long imageId) {
        ServiceRequestImage image = serviceRequestImageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("이미지를 찾을 수 없습니다: " + imageId));
        
        image.setActive(false);
        serviceRequestImageRepository.save(image);
    }
} 