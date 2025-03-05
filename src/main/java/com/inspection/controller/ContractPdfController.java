package com.inspection.controller;

import com.inspection.dto.ContractPdfFieldDTO;
import com.inspection.dto.SaveContractPdfFieldsRequest;
import com.inspection.entity.ContractPdfField;
import com.inspection.service.ContractPdfService;
import com.inspection.service.PdfProcessingService;
import com.inspection.service.PdfStorageService;
import com.inspection.repository.ContractPdfFieldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import java.util.stream.Collectors;
import java.util.Base64;
import java.util.Map;
import com.inspection.exception.ValidationException;

@Slf4j
@RestController
@RequestMapping("/api/contract-pdf")
@RequiredArgsConstructor
public class ContractPdfController {
    private final ContractPdfService contractPdfService;
    private final PdfProcessingService pdfProcessingService;
    private final PdfStorageService pdfStorageService;
    private final ContractPdfFieldRepository contractPdfFieldRepository;


    // PDF 필드 저장
    @PostMapping("/fields")
    public ResponseEntity<Void> saveFields(@RequestBody SaveContractPdfFieldsRequest request) {
        log.info("Received fields request: pdfId={}, fieldCount={}", 
            request.getPdfId(), request.getFields().size());
        log.debug("Fields data: {}", request.getFields());
        
        try {
            contractPdfService.saveFields(request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error saving fields", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    // PDF 필드 조회
    @GetMapping("/fields/{pdfId}")
    public ResponseEntity<List<ContractPdfFieldDTO>> getFields(@PathVariable String pdfId) {
        List<ContractPdfFieldDTO> fields = contractPdfService.getFieldsByPdfId(pdfId);
        return ResponseEntity.ok(fields);
    }


    // PDF 업로드드
    @PostMapping("/upload")
    public ResponseEntity<String> uploadPdf(@RequestParam("file") MultipartFile file) throws IOException {
        String pdfId = generatePdfId(file.getOriginalFilename());
        pdfStorageService.savePdf(pdfId, file.getBytes());
        return ResponseEntity.ok(pdfId);
    }


    // 저장된 PDF 미리보기
    @GetMapping("/preview/{pdfId}")
    public ResponseEntity<byte[]> getPreviewPdf(@PathVariable String pdfId) throws IOException {
        byte[] originalPdf = pdfStorageService.loadPdf(pdfId);
        List<ContractPdfField> fields = contractPdfFieldRepository.findByPdfId(pdfId);
        byte[] processedPdf = pdfProcessingService.addFieldsToPdf(originalPdf, fields);
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"preview.pdf\"")
            .header("X-Frame-Options", "SAMEORIGIN")
            .header("Content-Security-Policy", "frame-ancestors 'self'")
            .body(processedPdf);
    }


    // 저장된 PDF 저장
    @PostMapping("/save/{pdfId}")
    public ResponseEntity<String> savePdfWithFields(@PathVariable String pdfId) throws IOException {
        try {
            // 원본 PDF와 필드 정보 로드
            byte[] originalPdf = pdfStorageService.loadPdf(pdfId);
            List<ContractPdfField> fields = contractPdfFieldRepository.findByPdfId(pdfId);
            
            // 필드가 추가된 PDF 생성
            byte[] processedPdf = pdfProcessingService.addFieldsToPdf(originalPdf, fields);
            
            // 새로운 파일명 생성 (예: original_with_fields.pdf)
            String newPdfId = pdfId.replace(".pdf", "_with_fields.pdf");
            
            // 처리된 PDF 저장
            pdfStorageService.savePdf(newPdfId, processedPdf);
            
            log.info("Saved PDF with fields: {}", newPdfId);
            return ResponseEntity.ok(newPdfId);
            
        } catch (Exception e) {
            log.error("Error saving PDF with fields", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to save PDF with fields");
        }
    }


    // 저장된 PDF 다운로드
    @GetMapping("/download/{pdfId}")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String pdfId) throws IOException {
        byte[] pdf = pdfStorageService.loadPdf(pdfId);
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + pdfId + "\"")
            .body(pdf);
    }

    // 저장된 PDF 목록 조회
    @GetMapping("/view/{pdfId}")
    public ResponseEntity<byte[]> viewSavedPdf(@PathVariable String pdfId) throws IOException {
        try {
            byte[] pdf = pdfStorageService.loadPdf(pdfId);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + pdfId + "\"")
                .body(pdf);
                
        } catch (Exception e) {
            log.error("Error loading saved PDF: {}", pdfId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }


    // 저장된 PDF 목록 조회
    @GetMapping("/saved-pdfs")
    public ResponseEntity<List<String>> getSavedPdfList() {
        try {
            List<String> pdfList = pdfStorageService.getAllPdfIds()
                .stream()
                .filter(id -> id.endsWith("_with_fields.pdf"))
                .collect(Collectors.toList());
                
            return ResponseEntity.ok(pdfList);
            
        } catch (Exception e) {
            log.error("Error getting saved PDF list", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/fields/{pdfId}/value")
    public ResponseEntity<?> addFieldValue(
        @PathVariable String pdfId,
        @RequestParam String fieldName,
        @RequestBody Map<String, Object> value
    ) {
        try {
            // 1. 필드 찾기
            ContractPdfField field = contractPdfFieldRepository.findByPdfIdAndFieldName(pdfId, fieldName)
                .orElseThrow(() -> new RuntimeException("Field not found: " + fieldName));
            
            // 2. 값 검증
            String fieldType = field.getType();
            Object fieldValue = value.get("value");
            validateFieldValue(fieldType, fieldValue);
            
            // 3. 필드 값 업데이트
            field.setValue(fieldValue.toString());
            
            // 4. 저장
            ContractPdfField updatedField = contractPdfFieldRepository.save(field);
            
            // 5. PDF 업데이트 (선택적)
            if (value.containsKey("updatePdf") && Boolean.TRUE.equals(value.get("updatePdf"))) {
                updatePdfWithFieldValue(pdfId, field);
            }
            
            // 6. DTO로 변환하여 반환 (이제 String ID를 사용)
            return ResponseEntity.ok(new ContractPdfFieldDTO(updatedField));
            
        } catch (ValidationException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating field value: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to update field value: " + e.getMessage());
        }
    }

    // 값 검증 메소드
    private void validateFieldValue(String fieldType, Object value) throws ValidationException {
        if (value == null) {
            throw new ValidationException("Field value cannot be null");
        }

        switch (fieldType) {
            case "signature":
                if (!(value instanceof String) || !((String) value).startsWith("data:image")) {
                    throw new ValidationException("Invalid signature format");
                }
                break;
            case "checkbox":
                if (!(value instanceof Boolean) && !(value.toString().equals("true") || value.toString().equals("false"))) {
                    throw new ValidationException("Checkbox value must be boolean");
                }
                break;
            case "text":
                if (!(value instanceof String)) {
                    throw new ValidationException("Text value must be string");
                }
                break;
            default:
                throw new ValidationException("Unknown field type: " + fieldType);
        }
    }

    // PDF 업데이트 메소드
    private void updatePdfWithFieldValue(String pdfId, ContractPdfField field) throws IOException {
        byte[] originalPdf = pdfStorageService.loadPdf(pdfId);
        byte[] processedPdf = pdfProcessingService.addValueToField(
            originalPdf,
            field,
            field.getValue(),
            field.getType()
        );
        pdfStorageService.savePdf(pdfId, processedPdf);
    }

    @PostMapping("/download-signed/{pdfId}")
    public ResponseEntity<byte[]> downloadSignedPdf(@PathVariable String pdfId) {
        try {
            // 1. 원본 PDF 로드
            byte[] originalPdf = pdfStorageService.loadPdf(pdfId);
            
            // 2. 해당 PDF의 모든 필드와 값 조회
            List<ContractPdfField> fields = contractPdfFieldRepository.findByPdfId(pdfId);
            
            // 3. PDF에 필드 값 추가
            byte[] processedPdf = pdfProcessingService.addValuesToFields(originalPdf, fields);
            
            // 4. 파일명 설정
            String filename = pdfId.replace(".pdf", "_signed.pdf");
            
            // 5. PDF 다운로드 응답 생성
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(processedPdf);
                
        } catch (Exception e) {
            log.error("Error creating signed PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String generatePdfId(String originalFilename) {
        return System.currentTimeMillis() + "_" + originalFilename.replaceAll("[^a-zA-Z0-9.]", "_");
    }
} 