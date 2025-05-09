package com.inspection.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inspection.entity.ContractPdfField;
import com.inspection.entity.ContractTemplate;
import com.inspection.repository.ContractPdfFieldRepository;
import com.inspection.repository.ContractTemplateRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ContractTemplateService {
    private final PdfProcessingService pdfProcessingService;
    private final PdfStorageService pdfStorageService;
    private final ContractTemplateRepository templateRepository;
    private final ContractPdfFieldRepository contractPdfFieldRepository;
    

    // 템플릿을 생성하기 위한 메서드
    // 최초 계약서를 사용하기위해 템플릿을 만들때 사용되는 API에 사용됨
    public ContractTemplate createTemplate(String templateName, String originalPdfId, String description) throws IOException {
        // 1. 기존 필드 조회
        List<ContractPdfField> existingFields = contractPdfFieldRepository.findByPdfId(originalPdfId);
        
        // 2. 템플릿 PDF 생성 (새로운 파일명 형식 적용)
        byte[] originalPdf = pdfStorageService.loadOriginalPdf(originalPdfId);
        byte[] processedPdf = pdfProcessingService.addFieldsToPdf(originalPdf, existingFields);
        
        // 3. PDF에 로고 워터마크 추가 - 템플릿 생성 단계에서 추가
        processedPdf = pdfProcessingService.addLogoWatermark(
            processedPdf, 
            "/images/tirebank_logo.png"
        );
        
        // 새로운 템플릿 PDF ID 생성: 타임스탬프_template_파일명.pdf
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String originalFileName = extractOriginalFileName(originalPdfId);
        String processedPdfId = timestamp + "_template_" + originalFileName + ".pdf";
        
        // 명확한 템플릿 PDF 저장 메서드 사용
        pdfStorageService.saveTemplatePdf(processedPdfId, processedPdf);
        
        // 4. 템플릿 생성 및 저장
        ContractTemplate template = new ContractTemplate();
        template.setTemplateName(templateName);
        template.setDescription(description);
        template.setOriginalPdfId(originalPdfId);
        template.setProcessedPdfId(processedPdfId);
        template.setCreatedAt(LocalDateTime.now());
        template.setActive(true);
        template.addFields(existingFields);
        
        return templateRepository.save(template);
    }
    
    /**
     * 원본 PDF ID에서 파일명 추출
     */
    private String extractOriginalFileName(String originalPdfId) {
        // 원본 PDF ID 형식이 "타임스탬프_original_파일명.pdf"인 경우
        if (originalPdfId.contains("_original_")) {
            int startIndex = originalPdfId.indexOf("_original_") + "_original_".length();
            int endIndex = originalPdfId.lastIndexOf(".");
            if (startIndex > 0 && endIndex > startIndex) {
                return originalPdfId.substring(startIndex, endIndex);
            }
        }
        
        // 기존 형식에서 추출할 수 없는 경우, 확장자 제외한 전체 파일명 사용
        int dotIndex = originalPdfId.lastIndexOf(".");
        if (dotIndex > 0) {
            return originalPdfId.substring(0, dotIndex);
        }
        
        // 아무 것도 안되면 원본 ID 그대로 반환
        return originalPdfId;
    }
    
    public byte[] previewTemplate(Long templateId) throws IOException {
        ContractTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("Template not found"));
            
        // 기존 PdfProcessingService의 미리보기 기능 활용
        byte[] originalPdf = pdfStorageService.loadOriginalPdf(template.getOriginalPdfId());
        return pdfProcessingService.addFieldsToPdf(originalPdf, template.getFields());
    }
    
    // 추가 유틸리티 메서드들
    // @Transactional(readOnly = true)
    // public List<ContractTemplate> getActiveTemplates() {
    //     return templateRepository.findByIsActiveTrue();
    // }
    
    public void deactivateTemplate(Long templateId) {
        ContractTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("Template not found"));
        template.setActive(false);
        templateRepository.save(template);
    }
    
    public ContractTemplate getTemplate(Long templateId) {
        return templateRepository.findByIdWithFields(templateId)
            .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));
    }
    
    @Transactional(readOnly = true)
    public List<ContractTemplate> getAllTemplates() {
        return templateRepository.findAllTemplates();  // 모든 템플릿 조회
    }
} 