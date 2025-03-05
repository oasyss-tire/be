package com.inspection.controller;

import com.inspection.service.ContractTemplateService;
import com.inspection.entity.ContractTemplate;
import com.inspection.dto.SaveContractPdfFieldsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.List;
import com.inspection.service.PdfStorageService;
import com.inspection.repository.ContractTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@RestController
@RequestMapping("/api/contract-templates")
@RequiredArgsConstructor
public class ContractTemplateController {
    private final ContractTemplateService contractTemplateService;
    private final PdfStorageService pdfStorageService;
    private final ContractTemplateRepository contractTemplateRepository;

    // 템플릿 생성 (PDF 업로드 + 필드 정보)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContractTemplate> createTemplate(
        @RequestParam("file") MultipartFile file,
        @RequestParam("templateName") String templateName,
        @RequestParam("fieldsRequest") String fieldsJson
    ) {
        try {
            // JSON 문자열을 DTO로 변환
            ObjectMapper mapper = new ObjectMapper();
            SaveContractPdfFieldsRequest fieldsRequest = mapper.readValue(fieldsJson, SaveContractPdfFieldsRequest.class);
            
            // 1. PDF 파일 저장
            String pdfId = generatePdfId(file.getOriginalFilename());
            pdfStorageService.savePdf(pdfId, file.getBytes());
            
            // 2. 템플릿 생성
            ContractTemplate template = contractTemplateService.createTemplate(
                templateName, 
                pdfId, 
                fieldsRequest.getFields()
            );
            
            return ResponseEntity.ok(template);
        } catch (Exception e) {
            log.error("Error creating template: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // 템플릿 목록 조회
    @GetMapping
    public ResponseEntity<List<ContractTemplate>> getTemplates(
        @RequestParam(required = false) String keyword
    ) {
        List<ContractTemplate> templates = keyword != null ?
            contractTemplateRepository.findByTemplateNameContaining(keyword) :
            contractTemplateService.getActiveTemplates();
        return ResponseEntity.ok(templates);
    }

    // 템플릿 미리보기
    @GetMapping("/{templateId}/preview")
    public ResponseEntity<byte[]> previewTemplate(@PathVariable Long templateId) {
        try {
            byte[] pdfBytes = contractTemplateService.previewTemplate(templateId);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"template_preview.pdf\"")
                .body(pdfBytes);
        } catch (Exception e) {
            log.error("Error previewing template", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // 템플릿 비활성화
    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deactivateTemplate(@PathVariable Long templateId) {
        try {
            contractTemplateService.deactivateTemplate(templateId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deactivating template", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String generatePdfId(String originalFilename) {
        return System.currentTimeMillis() + "_" + originalFilename.replaceAll("[^a-zA-Z0-9.]", "_");
    }
} 