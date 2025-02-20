package com.inspection.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {
    
    @Value("${file.facility-image.path}")
    private String uploadPath;
    
    public String saveImage(MultipartFile file) {
        try {
            log.info("Upload Path: {}", uploadPath);
            
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String savedFileName = UUID.randomUUID().toString() + extension;
            
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            
            Path filePath = uploadDir.resolve(savedFileName);
            log.info("Saving file to: {}", filePath.toString());
            
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            return savedFileName;
        } catch (Exception e) {
            log.error("이미지 저장 실패: {}", e.getMessage(), e);
            throw new RuntimeException("이미지 저장에 실패했습니다.", e);
        }
    }

    public Resource loadImage(String fileName) {
        try {
            Path filePath = Paths.get(uploadPath).resolve(fileName).normalize();
            log.info("Loading image from: {}", filePath.toString());
            
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                log.error("File not found: {}", filePath.toString());
                throw new RuntimeException("이미지 파일을 찾을 수 없습니다: " + fileName);
            }
        } catch (Exception e) {
            log.error("이미지 로드 실패: {}", e.getMessage(), e);
            throw new RuntimeException("이미지 로드 실패: " + fileName, e);
        }
    }

    public void deleteImage(String fileName) {
        try {
            Path filePath = Paths.get(uploadPath).resolve(fileName);
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            log.error("이미지 삭제 실패: {}", e.getMessage(), e);
            throw new RuntimeException("이미지 삭제에 실패했습니다.", e);
        }
    }
} 