package com.inspection.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfStorageService {
    private final String baseDir = "uploads/contracts";
    private final String originalDir = baseDir + "/original";
    private final String templateDir = baseDir + "/template";
    private final String legacyDir = "uploads/pdfs"; // 기존 경로 (하위 호환성 유지)
    
    /**
     * 원본 PDF 파일을 저장합니다.
     * @param pdfId PDF 파일 ID
     * @param content PDF 파일 내용
     * @throws IOException 파일 저장 중 오류 발생 시
     */
    public void saveOriginalPdf(String pdfId, byte[] content) throws IOException {
        Path dirPath = Paths.get(originalDir);
        Files.createDirectories(dirPath);
        
        Path filePath = dirPath.resolve(pdfId);
        Files.write(filePath, content);
        log.info("원본 PDF 파일 저장 완료: {}", pdfId);
    }
    
    /**
     * 템플릿 PDF 파일을 저장합니다.
     * @param pdfId PDF 파일 ID
     * @param content PDF 파일 내용
     * @throws IOException 파일 저장 중 오류 발생 시
     */
    public void saveTemplatePdf(String pdfId, byte[] content) throws IOException {
        Path dirPath = Paths.get(templateDir);
        Files.createDirectories(dirPath);
        
        Path filePath = dirPath.resolve(pdfId);
        Files.write(filePath, content);
        log.info("템플릿 PDF 파일 저장 완료: {}", pdfId);
    }
    
    /**
     * 기존 코드와의 호환성을 위한 메서드 (추후 제거 예정)
     */
    public void savePdf(String pdfId, byte[] content) throws IOException {
        boolean isTemplate = pdfId.contains("_template");
        if (isTemplate) {
            saveTemplatePdf(pdfId, content);
        } else {
            saveOriginalPdf(pdfId, content);
        }
    }
    
    /**
     * 기존 코드와의 호환성을 위한 메서드 (추후 제거 예정)
     */
    public void savePdf(String pdfId, byte[] content, boolean isTemplate) throws IOException {
        if (isTemplate) {
            saveTemplatePdf(pdfId, content);
        } else {
            saveOriginalPdf(pdfId, content);
        }
    }
    
    /**
     * 원본 PDF 파일을 로드합니다.
     * @param pdfId PDF 파일 ID
     * @return PDF 파일 내용
     * @throws IOException 파일 로드 중 오류 발생 시
     */
    public byte[] loadOriginalPdf(String pdfId) throws IOException {
        Path filePath = Paths.get(originalDir).resolve(pdfId);
        
        // 새 경로에 파일이 없으면 기존 경로에서 시도
        if (!Files.exists(filePath)) {
            Path legacyPath = Paths.get(legacyDir).resolve(pdfId);
            
            if (Files.exists(legacyPath)) {
                // 기존 파일이 있으면 마이그레이션
                byte[] content = Files.readAllBytes(legacyPath);
                saveOriginalPdf(pdfId, content);
                return content;
            }
        }
        
        return Files.readAllBytes(filePath);
    }
    
    /**
     * 템플릿 PDF 파일을 로드합니다.
     * @param pdfId PDF 파일 ID
     * @return PDF 파일 내용
     * @throws IOException 파일 로드 중 오류 발생 시
     */
    public byte[] loadTemplatePdf(String pdfId) throws IOException {
        Path filePath = Paths.get(templateDir).resolve(pdfId);
        
        // 새 경로에 파일이 없으면 기존 경로에서 시도
        if (!Files.exists(filePath)) {
            Path legacyPath = Paths.get(legacyDir).resolve(pdfId);
            
            if (Files.exists(legacyPath)) {
                // 기존 파일이 있으면 마이그레이션
                byte[] content = Files.readAllBytes(legacyPath);
                saveTemplatePdf(pdfId, content);
                return content;
            }
        }
        
        return Files.readAllBytes(filePath);
    }
    
    /**
     * 기존 코드와의 호환성을 위한 메서드 (추후 제거 예정)
     */
    public byte[] loadPdf(String pdfId) throws IOException {
        boolean isTemplate = pdfId.contains("_template");
        
        if (isTemplate) {
            return loadTemplatePdf(pdfId);
        } else {
            return loadOriginalPdf(pdfId);
        }
    }
    
    /**
     * 모든 원본 PDF 파일 목록을 반환합니다.
     * @return 원본 PDF 파일 ID 목록
     */
    public List<String> getAllOriginalPdfIds() {
        File directory = new File(originalDir);
        
        if (!directory.exists()) {
            return new ArrayList<>();
        }
        
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".pdf"));
        if (files == null) {
            return new ArrayList<>();
        }
        
        return Arrays.stream(files)
            .map(File::getName)
            .collect(Collectors.toList());
    }
    
    /**
     * 모든 템플릿 PDF 파일 목록을 반환합니다.
     * @return 템플릿 PDF 파일 ID 목록
     */
    public List<String> getAllTemplatePdfIds() {
        File directory = new File(templateDir);
        
        if (!directory.exists()) {
            return new ArrayList<>();
        }
        
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".pdf"));
        if (files == null) {
            return new ArrayList<>();
        }
        
        return Arrays.stream(files)
            .map(File::getName)
            .collect(Collectors.toList());
    }
    
    /**
     * 기존 코드와의 호환성을 위한 메서드 (추후 제거 예정)
     */
    public List<String> getAllPdfIds() {
        List<String> allPdfIds = new ArrayList<>();
        
        // 원본 PDF 목록
        allPdfIds.addAll(getAllOriginalPdfIds());
        
        // 템플릿 PDF 목록
        allPdfIds.addAll(getAllTemplatePdfIds());
        
        // 기존 경로의 PDF 목록 (새 구조에 없는 것만)
        File legacyDirectory = new File(legacyDir);
        if (legacyDirectory.exists()) {
            File[] legacyFiles = legacyDirectory.listFiles((dir, name) -> name.endsWith(".pdf"));
            if (legacyFiles != null) {
                for (File file : legacyFiles) {
                    String fileName = file.getName();
                    if (!allPdfIds.contains(fileName)) {
                        allPdfIds.add(fileName);
                    }
                }
            }
        }
        
        return allPdfIds;
    }
} 