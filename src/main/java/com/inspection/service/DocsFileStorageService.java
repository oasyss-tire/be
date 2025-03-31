package com.inspection.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DocsFileStorageService {

    private Path rootLocation;

    @PostConstruct
    public void init() {
        try {
            // "uploads/contracts/docs" 디렉토리에 파일 저장
            this.rootLocation = Paths.get("uploads/contracts/docs");
            Files.createDirectories(rootLocation);
            log.info("문서 저장소 초기화 완료: {}", rootLocation.toAbsolutePath());
        } catch (IOException e) {
            log.error("문서 저장소 초기화 실패", e);
            throw new RuntimeException("문서 저장소를 초기화할 수 없습니다", e);
        }
    }

    /**
     * 파일을 저장하고 저장된 파일명을 반환합니다.
     * 
     * @param file 저장할 파일
     * @param directory 저장 디렉토리 (null이면 기본 위치에 저장)
     * @return 저장된 파일명 (경로 포함)
     */
    public String storeFile(MultipartFile file, String directory) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("빈 파일은 저장할 수 없습니다.");
        }
        
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf(".");
        
        if (lastDotIndex > 0) {
            extension = originalFilename.substring(lastDotIndex);
        }
        
        // 고유한 파일명 생성 - 접두사로 계약/참여자 정보 포함 가능
        String storedFilename = UUID.randomUUID().toString() + extension;
        
        // 파일 저장 경로 설정
        Path targetDirectory = this.rootLocation;
        
        // 지정된 디렉토리가 있는 경우에만 하위 경로 생성
        if (directory != null && !directory.isEmpty()) {
            targetDirectory = this.rootLocation.resolve(directory);
            
            // 전체 디렉토리 경로 생성 (상위 경로 포함)
            try {
                Files.createDirectories(targetDirectory);
                log.info("디렉토리 생성 완료: {}", targetDirectory);
            } catch (IOException e) {
                log.error("디렉토리 생성 실패: {}", targetDirectory, e);
                throw new IOException("디렉토리 생성 실패: " + targetDirectory, e);
            }
        }
        
        // 파일 저장 경로 (전체 경로)
        Path destinationFile = targetDirectory.resolve(storedFilename);
        
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("파일 저장 완료: {}", destinationFile);
            
            // 저장된 파일 경로 반환 (상대 경로)
            return directory != null ? directory + "/" + storedFilename : storedFilename;
        } catch (IOException e) {
            log.error("파일 저장 실패: {}", originalFilename, e);
            throw new IOException("파일 저장 실패: " + originalFilename, e);
        }
    }
    
    /**
     * 저장된 파일을 삭제합니다.
     * 
     * @param filename 삭제할 파일명 (경로 포함)
     */
    public void deleteFile(String filename) throws IOException {
        if (filename == null || filename.isEmpty()) {
            return;
        }
        
        Path file = Paths.get(this.rootLocation.toString(), filename);
        
        try {
            if (Files.exists(file)) {
                Files.delete(file);
                log.info("파일 삭제 완료: {}", file);
            } else {
                log.warn("삭제할 파일이 존재하지 않습니다: {}", file);
            }
        } catch (IOException e) {
            log.error("파일 삭제 실패: {}", filename, e);
            throw new IOException("파일 삭제 실패: " + filename, e);
        }
    }
    
    /**
     * 저장된 파일을 조회합니다.
     * 
     * @param filename 조회할 파일명 (경로 포함)
     * @return 파일 바이트 배열
     */
    public byte[] getFile(String filename) throws IOException {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("파일명이 비어있습니다.");
        }
        
        Path file = Paths.get(this.rootLocation.toString(), filename);
        
        if (Files.exists(file)) {
            log.info("파일 조회: {}", file);
            return Files.readAllBytes(file);
        } else {
            log.error("파일이 존재하지 않습니다: {}", file);
            throw new IOException("파일이 존재하지 않습니다: " + filename);
        }
    }
} 