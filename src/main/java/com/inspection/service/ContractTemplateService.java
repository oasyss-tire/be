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

@Service
@Transactional
@RequiredArgsConstructor
public class ContractTemplateService {
    private final PdfProcessingService pdfProcessingService;
    private final PdfStorageService pdfStorageService;
    private final ContractTemplateRepository templateRepository;
    
    public ContractTemplate createTemplate(String templateName, String originalPdfId, List<ContractPdfFieldDTO> fieldDtos) throws IOException {
        // DTO를 Entity로 변환
        List<ContractPdfField> fields = fieldDtos.stream()
            .map(dto -> {
                ContractPdfField field = new ContractPdfField();
                field.setFieldId(dto.getId());
                field.setPdfId(originalPdfId);
                field.setFieldName(dto.getFieldName());
                field.setType(dto.getType());
                field.setRelativeX(dto.getRelativeX());
                field.setRelativeY(dto.getRelativeY());
                field.setRelativeWidth(dto.getRelativeWidth());
                field.setRelativeHeight(dto.getRelativeHeight());
                field.setPage(dto.getPage());
                return field;
            })
            .collect(Collectors.toList());

        // 1. 템플릿 기본 정보 설정
        ContractTemplate template = new ContractTemplate();
        template.setTemplateName(templateName);
        template.setOriginalPdfId(originalPdfId);
        template.setCreatedAt(LocalDateTime.now());
        template.setActive(true);
        
        // 2. 필드 연결
        template.addFields(fields);
        
        // 3. 서명 영역이 표시된 PDF 생성 및 저장
        byte[] originalPdf = pdfStorageService.loadPdf(originalPdfId);
        byte[] processedPdf = pdfProcessingService.addFieldsToPdf(originalPdf, fields);
        String processedPdfId = originalPdfId.replace(".pdf", "_with_fields.pdf");
        pdfStorageService.savePdf(processedPdfId, processedPdf);
        
        // 4. 처리된 PDF ID 저장
        template.setProcessedPdfId(processedPdfId);
        
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
    public List<ContractTemplate> getActiveTemplates() {
        return templateRepository.findByIsActiveTrue();
    }
    
    public void deactivateTemplate(Long templateId) {
        ContractTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("Template not found"));
        template.setActive(false);
        templateRepository.save(template);
    }
} 