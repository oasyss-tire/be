package com.inspection.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PdfStorageService {
    private final String uploadDir = "uploads/pdfs";
    
    public void savePdf(String pdfId, byte[] content) throws IOException {
        Path dirPath = Paths.get(uploadDir);
        Files.createDirectories(dirPath);
        
        Path filePath = dirPath.resolve(pdfId);
        Files.write(filePath, content);
    }
    
    public byte[] loadPdf(String pdfId) throws IOException {
        Path filePath = Paths.get(uploadDir).resolve(pdfId);
        return Files.readAllBytes(filePath);
    }

    public List<String> getAllPdfIds() {
        // PDF 저장 디렉토리에서 모든 PDF 파일 목록 조회
        File directory = new File(uploadDir);
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".pdf"));
        
        if (files == null) {
            return new ArrayList<>();
        }
        
        return Arrays.stream(files)
            .map(File::getName)
            .collect(Collectors.toList());
    }
} 