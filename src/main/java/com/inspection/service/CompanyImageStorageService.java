package com.inspection.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CompanyImageStorageService {
    
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;
    
    // 실제 이미지가 저장되는 하위 디렉토리 (company-images)
    private static final String IMAGE_SUBDIR = "company-images";
    
    /**
     * 회사 이미지 파일을 저장하고 파일 경로를 반환합니다.
     * 
     * @param file 저장할 파일
     * @param subDir 저장할 하위 디렉토리
     * @return 저장된 파일의 경로
     */
    public String storeFile(MultipartFile file, String subDir) {
        try {
            // 파일명 생성 (UUID + 원본 파일명)
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            
            // 저장 경로 생성 (uploads/company-images/company)
            Path targetDir = Paths.get(uploadDir, IMAGE_SUBDIR, subDir).toAbsolutePath().normalize();
            
            // 디렉토리가 없으면 생성
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            
            // 파일 저장
            Path targetPath = targetDir.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("회사 이미지 파일 저장 완료: {}", targetPath);
            
            // 상대 경로 반환 (DB에 저장) - company/fileName 형식
            return subDir + "/" + fileName;
        } catch (IOException ex) {
            log.error("회사 이미지 파일 저장 실패: {}", ex.getMessage());
            throw new RuntimeException("회사 이미지 파일 저장에 실패했습니다.", ex);
        }
    }
    
    /**
     * 회사 이미지 파일을 삭제합니다.
     * 
     * @param filePath 삭제할 파일의 경로
     */
    public void deleteFile(String filePath) {
        try {
            // 실제 파일 경로 (uploads/company-images/company/xxx.jpg)
            Path targetPath = Paths.get(uploadDir, IMAGE_SUBDIR, filePath).toAbsolutePath().normalize();
            Files.deleteIfExists(targetPath);
            log.info("회사 이미지 파일 삭제 완료: {}", targetPath);
        } catch (IOException ex) {
            log.error("회사 이미지 파일 삭제 실패: {}", ex.getMessage());
        }
    }

    /**
     * 이미지 파일을 Resource로 로드합니다.
     * 
     * @param fileName 파일명 (예: company/xxx.jpg)
     * @return 이미지 파일 Resource
     */
    public Resource loadImageAsResource(String fileName) {
        try {
            // 실제 파일 경로 (uploads/company-images/fileName)
            Path filePath = Paths.get(uploadDir, IMAGE_SUBDIR, fileName).normalize();
            log.info("이미지 파일 로드 시도: {}", filePath);
            
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                log.info("이미지 파일 로드 성공: {}", filePath);
                return resource;
            } else {
                log.error("이미지 파일을 찾을 수 없음: {}", filePath);
                throw new RuntimeException("파일을 찾을 수 없습니다: " + fileName);
            }
        } catch (MalformedURLException e) {
            log.error("이미지 파일 URL 오류: {}", fileName, e);
            throw new RuntimeException("파일을 찾을 수 없습니다: " + fileName, e);
        }
    }
} 