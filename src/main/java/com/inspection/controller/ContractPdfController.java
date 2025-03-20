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
import java.util.Map;
import com.inspection.exception.ValidationException;
import com.inspection.entity.ContractTemplate;
import com.inspection.service.ContractTemplateService;
import com.inspection.repository.ContractTemplateRepository;
import com.inspection.dto.ContractTemplateDTO;
import org.springframework.beans.factory.annotation.Value;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.http.HttpStatus;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.inspection.entity.ContractParticipant;
import com.inspection.repository.ContractParticipantRepository;
import java.time.LocalDateTime;
import com.inspection.repository.ParticipantTemplateMappingRepository;
import com.inspection.entity.ParticipantTemplateMapping;
import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping("/api/contract-pdf")
@RequiredArgsConstructor
public class ContractPdfController {
    private final ContractPdfService contractPdfService;
    private final PdfProcessingService pdfProcessingService;
    private final PdfStorageService pdfStorageService;
    private final ContractPdfFieldRepository contractPdfFieldRepository;
    private final ContractTemplateService contractTemplateService;
    private final ContractTemplateRepository contractTemplateRepository;
    private final ContractParticipantRepository participantRepository;
    private final ParticipantTemplateMappingRepository templateMappingRepository;

    @Value("${file.upload.path}")
    private String uploadPath;

    // PDF 필드 저장 (2)
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


    // PDF 필드 조회 (필드 위치 확인)
    @GetMapping("/fields/{pdfId}")
    public ResponseEntity<List<ContractPdfFieldDTO>> getFields(@PathVariable String pdfId) {
        List<ContractPdfFieldDTO> fields = contractPdfService.getFieldsByPdfId(pdfId);
        return ResponseEntity.ok(fields);
    }


    // PDF 업로드 (원본)
    @PostMapping("/upload")
    public ResponseEntity<String> uploadPdf(@RequestParam("file") MultipartFile file) throws IOException {
        String originalPdfId = generatePdfId(file.getOriginalFilename(), "original");
        pdfStorageService.savePdf(originalPdfId, file.getBytes());
        return ResponseEntity.ok(originalPdfId);
    }


    // 저장된 PDF 미리보기  (템플릿 리스트에서 확인가능)
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


    // 저장된 PDF 다운로드
    @GetMapping("/download/{pdfId}")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String pdfId) throws IOException {
        byte[] pdf = pdfStorageService.loadPdf(pdfId);
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + pdfId + "\"")
            .body(pdf);
    }

    // 저장된 PDF 목록 조회 (템플릿 리스트에서 확인가능)
    @GetMapping("/view/{pdfId}")
    public ResponseEntity<byte[]> viewSavedPdf(@PathVariable String pdfId) throws IOException {
        try {
            // participants 폴더에서 PDF 파일 찾기
            Path pdfPath = Paths.get(uploadPath, "participants", pdfId);
            
            if (!Files.exists(pdfPath)) {
                // participants 폴더에 없으면 pdfs 폴더에서 찾기
                pdfPath = Paths.get(".", "uploads", "pdfs", pdfId);
            }
            
            if (!Files.exists(pdfPath)) {
                throw new RuntimeException("PDF file not found: " + pdfId);
            }

            byte[] pdf = Files.readAllBytes(pdfPath);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + pdfId + "\"")
                .body(pdf);
                
        } catch (Exception e) {
            log.error("Error loading saved PDF: {}", pdfId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }


    // 저장된 PDF 목록 조회 (4)
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

    // 서명 값 입력 (5)
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


    // 서명된 PDF 저장
    @PostMapping("/download-signed/{pdfId}")
    public ResponseEntity<byte[]> saveSignedPdf(@PathVariable String pdfId) {
        try {
            // 1. 서명된 PDF 생성
            String signedPdfId = "signed_" + pdfId;
            
            // 2. participants 폴더에서 원본 PDF 읽기
            Path originalPath = Paths.get(uploadPath, "participants", pdfId);
            if (!Files.exists(originalPath)) {
                throw new RuntimeException("원본 PDF를 찾을 수 없습니다: " + pdfId);
            }
            byte[] originalPdf = Files.readAllBytes(originalPath);
            
            // 3. 해당 PDF의 모든 필드와 값 조회
            List<ContractPdfField> fields = contractPdfFieldRepository.findByPdfId(pdfId);
            log.info("Found {} fields with values for PDF: {}", fields.size(), pdfId);
            
            // 4. PDF에 필드 값 추가
            byte[] processedPdf = pdfProcessingService.addValuesToFields(originalPdf, fields);
            
            // 5. signed 폴더에 저장
            Path signedDir = Paths.get(uploadPath, "signed");
            if (!Files.exists(signedDir)) {
                Files.createDirectories(signedDir);
            }
            
            Path signedPath = signedDir.resolve(signedPdfId);
            Files.write(signedPath, processedPdf);
            
            // 6. ParticipantTemplateMapping 업데이트
            ParticipantTemplateMapping templateMapping = templateMappingRepository.findByPdfId(pdfId)
                .orElseThrow(() -> new RuntimeException("템플릿 매핑 정보를 찾을 수 없습니다: " + pdfId));
            
            templateMapping.setSignedPdfId(signedPdfId);
            templateMapping.setSigned(true);
            templateMapping.setSignedAt(LocalDateTime.now());
            
            templateMappingRepository.save(templateMapping);
            log.info("Updated template mapping signed PDF ID: {}", signedPdfId);
            
            // 7. 한글 파일명 인코딩 처리
            String encodedFilename = URLEncoder.encode(signedPdfId, StandardCharsets.UTF_8.toString())
                .replaceAll("\\+", "%20");
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename*=UTF-8''" + encodedFilename)
                .body(processedPdf);
                
        } catch (Exception e) {
            log.error("Error creating signed PDF: {}", pdfId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 서명된 PDF 다운로드
    @GetMapping("/download-signed-pdf/{pdfId}")
    public ResponseEntity<byte[]> downloadSignedPdf(@PathVariable String pdfId) {
        try {
            // 서명된 PDF ID 찾기
            String signedPdfId = pdfId;
            
            // pdfId가 원본 PDF ID인 경우, 해당 매핑에서 서명된 PDF ID 조회
            if (!pdfId.startsWith("signed_")) {
                ParticipantTemplateMapping mapping = templateMappingRepository.findByPdfId(pdfId)
                    .orElseThrow(() -> new RuntimeException("템플릿 매핑 정보를 찾을 수 없습니다: " + pdfId));
                
                if (mapping.getSignedPdfId() == null) {
                    throw new RuntimeException("아직 서명되지 않은 PDF입니다: " + pdfId);
                }
                
                signedPdfId = mapping.getSignedPdfId();
            }
            
            // 경로 수정
            Path signedPath = Paths.get(uploadPath, "signed", signedPdfId);
            
            if (!Files.exists(signedPath)) {
                log.warn("PDF not found at path: {}", signedPath);
                throw new RuntimeException("서명된 PDF를 찾을 수 없습니다: " + signedPdfId);
            }
            
            byte[] pdfBytes = Files.readAllBytes(signedPath);
            
            // 한글 파일명 인코딩
            String filename = signedPdfId;
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString())
                .replaceAll("\\+", "%20");
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename*=UTF-8''" + encodedFilename)
                .body(pdfBytes);
                
        } catch (Exception e) {
            log.error("Error downloading signed PDF: {}", pdfId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // 모든 서명된 PDF 다운로드 (참여자별)
    @GetMapping("/download-all-signed-pdfs/{participantId}")
    public ResponseEntity<?> downloadAllSignedPdfs(@PathVariable Long participantId) {
        try {
            // 참여자 정보 조회
            ContractParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("참여자 정보를 찾을 수 없습니다: " + participantId));
            
            // 모든 템플릿 매핑 조회
            List<ParticipantTemplateMapping> mappings = participant.getTemplateMappings();
            
            // 서명이 완료되지 않은 템플릿이 있는지 확인
            boolean allSigned = mappings.stream().allMatch(ParticipantTemplateMapping::isSigned);
            if (!allSigned) {
                return ResponseEntity.badRequest().body("모든 문서에 서명이 완료되지 않았습니다.");
            }
            
            // 각 템플릿의 서명된 PDF 경로와 이름을 응답
            List<Map<String, String>> signedPdfs = mappings.stream()
                .filter(mapping -> mapping.getSignedPdfId() != null)
                .map(mapping -> {
                    Map<String, String> pdfInfo = new HashMap<>();
                    pdfInfo.put("pdfId", mapping.getSignedPdfId());
                    pdfInfo.put("templateName", mapping.getContractTemplateMapping().getTemplate().getTemplateName());
                    pdfInfo.put("downloadUrl", "/api/contract-pdf/download-signed-pdf/" + mapping.getSignedPdfId());
                    return pdfInfo;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(signedPdfs);
                
        } catch (Exception e) {
            log.error("Error getting all signed PDFs: {}", participantId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // 파일명 생성 메서드 수정
    private String generatePdfId(String originalFilename, String type) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String cleanName = originalFilename.replaceAll("[^a-zA-Z0-9.]", "_");
        
        switch (type) {
            case "original":
                return timestamp + "_original_" + cleanName;
            case "template":
                return timestamp + "_template_" + cleanName;
            case "signed":
                return timestamp + "_signed_" + cleanName;
            default:
                return timestamp + "_" + cleanName;
        }
    }

    // 템플릿 저장
    @PostMapping("/save-template/{pdfId}")
    public ResponseEntity<ContractTemplate> saveTemplate(
        @PathVariable String pdfId,
        @RequestParam String templateName,
        @RequestParam(required = false) String description
    ) {
        try {
            ContractTemplate savedTemplate = contractTemplateService.createTemplate(
                templateName, pdfId, description
            );
            return ResponseEntity.ok(savedTemplate);
        } catch (Exception e) {
            log.error("Error saving template", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 템플릿 목록 조회
    @GetMapping("/templates")
    public ResponseEntity<List<ContractTemplateDTO>> getTemplates(
        @RequestParam(required = false) String keyword
    ) {
        List<ContractTemplate> templates = keyword != null ?
            contractTemplateRepository.findByTemplateNameContaining(keyword) :
            contractTemplateService.getAllTemplates();
        
        List<ContractTemplateDTO> dtos = templates.stream()
            .map(ContractTemplateDTO::new)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    // 템플릿 상세 조회 (추가)
    @GetMapping("/templates/{templateId}")
    public ResponseEntity<ContractTemplateDTO> getTemplate(@PathVariable Long templateId) {
        ContractTemplate template = contractTemplateService.getTemplate(templateId);
        return ResponseEntity.ok(new ContractTemplateDTO(template));
    }

    // 템플릿 미리보기
    @GetMapping("/templates/{templateId}/preview")
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
    @DeleteMapping("/templates/{templateId}")
    public ResponseEntity<Void> deactivateTemplate(@PathVariable Long templateId) {
        try {
            contractTemplateService.deactivateTemplate(templateId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deactivating template", e);
            return ResponseEntity.internalServerError().build();
        }
    }
} 