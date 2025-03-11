package com.inspection.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.io.IOException;
import java.util.stream.Collectors;
import com.inspection.entity.ContractTemplate;
import com.inspection.entity.ContractPdfField;
import com.inspection.repository.ContractTemplateRepository;
import com.inspection.dto.ContractPdfFieldDTO;
import com.inspection.repository.ContractPdfFieldRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class ContractTemplateService {
    private final PdfProcessingService pdfProcessingService;
    private final PdfStorageService pdfStorageService;
    private final ContractTemplateRepository templateRepository;
    private final ContractPdfFieldRepository contractPdfFieldRepository;
    
    public ContractTemplate createTemplate(String templateName, String originalPdfId, String description) throws IOException {
        // 1. 기존 필드 조회
        List<ContractPdfField> existingFields = contractPdfFieldRepository.findByPdfId(originalPdfId);
        
        // 2. 템플릿 PDF 생성 (파일명 형식 통일)
        byte[] originalPdf = pdfStorageService.loadPdf(originalPdfId);
        byte[] processedPdf = pdfProcessingService.addFieldsToPdf(originalPdf, existingFields);
        String processedPdfId = originalPdfId.replace(".pdf", "_template.pdf");
        pdfStorageService.savePdf(processedPdfId, processedPdf);
        
        // 3. 템플릿 생성 및 저장
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
    
    public byte[] previewTemplate(Long templateId) throws IOException {
        ContractTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("Template not found"));
            
        // 기존 PdfProcessingService의 미리보기 기능 활용
        byte[] originalPdf = pdfStorageService.loadPdf(template.getOriginalPdfId());
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
        return templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));
    }
    
    @Transactional(readOnly = true)
    public List<ContractTemplate> getAllTemplates() {
        return templateRepository.findAllTemplates();  // 모든 템플릿 조회
    }
} 