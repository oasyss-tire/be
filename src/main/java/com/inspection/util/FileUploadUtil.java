package com.inspection.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FileUploadUtil {
    @Value("${file.upload-dir}")
    private String uploadDir;
    
    // 허용된 파일 확장자 목록
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        "jpg", "jpeg", "png", "gif",  // 이미지
        "pdf", "doc", "docx",         // 문서
        "xls", "xlsx",                // 엑셀
        "zip", "rar"                  // 압축파일
    );

    public String saveFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            log.info("파일이 null이거나 비어있음");
            return null;
        }

        log.info("파일 저장 시작: {}", file.getOriginalFilename());
        log.info("업로드 디렉토리: {}", uploadDir);

        // 파일 확장자 검증
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        if (!isValidFileExtension(extension)) {
            log.error("지원하지 않는 파일 형식: {}", extension);
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다: " + extension);
        }

        // 업로드 디렉토리 생성
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            log.info("업로드 디렉토리 생성: {}", uploadPath);
            Files.createDirectories(uploadPath);
        }

        // 파일명 생성
        String fileName = UUID.randomUUID().toString() + "_" + originalFilename;
        Path filePath = uploadPath.resolve(fileName);
        
        // 파일 저장
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("파일 저장 완료: {}", fileName);
        
        return fileName;
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private boolean isValidFileExtension(String extension) {
        return ALLOWED_EXTENSIONS.contains(extension.toLowerCase());
    }

    public void deleteFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return;
        }

        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("파일 삭제 실패: " + fileName, e);
        }
    }

    public String getUploadDir() {
        return uploadDir;
    }
}