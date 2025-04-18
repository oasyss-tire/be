package com.inspection.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.inspection.dto.ContractPdfFieldDTO;
import com.inspection.dto.ContractTemplateDTO;
import com.inspection.dto.ParticipantPdfFieldDTO;
import com.inspection.dto.SaveContractPdfFieldsRequest;
import com.inspection.entity.ContractParticipant;
import com.inspection.entity.ContractPdfField;
import com.inspection.entity.ContractTemplate;
import com.inspection.entity.ParticipantPdfField;
import com.inspection.entity.ParticipantTemplateMapping;
import com.inspection.entity.Contract;
import com.inspection.exception.ValidationException;
import com.inspection.repository.ContractParticipantRepository;
import com.inspection.repository.ContractPdfFieldRepository;
import com.inspection.repository.ContractTemplateRepository;
import com.inspection.repository.ParticipantPdfFieldRepository;
import com.inspection.repository.ParticipantTemplateMappingRepository;
import com.inspection.service.ContractPdfService;
import com.inspection.service.ContractTemplateService;
import com.inspection.service.EmailService;
import com.inspection.service.PdfProcessingService;
import com.inspection.service.PdfStorageService;
import com.inspection.util.EncryptionUtil;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    private final EmailService emailService;
    private final EncryptionUtil encryptionUtil;
    private final ParticipantPdfFieldRepository participantPdfFieldRepository;

    @Value("${file.upload.path}")
    private String uploadPath;

    @Value("${frontend.base-url}")
    private String frontendBaseUrl;

    // 계약 ID와 비밀번호를 매핑하기 위한 캐시
    private final Map<String, String> contractPasswordCache = new ConcurrentHashMap<>();

    
    // PDF 업로드 (원본)
    // 최초 계약서 업로드 시 원본파일 저장용 API
    @PostMapping("/upload")
    public ResponseEntity<String> uploadPdf(@RequestParam("file") MultipartFile file) throws IOException {
        String originalPdfId = generatePdfId(file.getOriginalFilename(), "original");
        
        // 원본 PDF 저장
        pdfStorageService.saveOriginalPdf(originalPdfId, file.getBytes());
        
        return ResponseEntity.ok(originalPdfId);
    }


    // PDF 필드 저장
    // 계약서 업로드 시 서명 필드 저장용 API
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
    public ResponseEntity<List<?>> getFields(@PathVariable String pdfId) {
        // 템플릿 필드인지 참여자 필드인지 확인
        List<ContractPdfFieldDTO> templateFields = contractPdfService.getFieldsByPdfId(pdfId);
        
        if (!templateFields.isEmpty()) {
            // 템플릿 필드인 경우
            return ResponseEntity.ok(templateFields);
        } else {
            // 참여자 필드인 경우
            List<ParticipantPdfFieldDTO> participantFields = contractPdfService.getParticipantFieldsByPdfId(pdfId);
            return ResponseEntity.ok(participantFields);
        }
    }


 

    // 템플릿 저장
    // 최초 계약서를 사용하기위해 템플릿을 만들때 사용되는 API 
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
        
        // 최신순(생성일 기준) 정렬 적용
        List<ContractTemplateDTO> dtos = templates.stream()
            .map(ContractTemplateDTO::new)
            .sorted((a, b) -> {
                // CreatedAt이 null인 경우 처리
                if (a.getCreatedAt() == null) return 1;
                if (b.getCreatedAt() == null) return -1;
                // 내림차순 정렬 (최신순)
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            })
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
            // 새로운 디렉토리 구조를 먼저 확인
            boolean isTemplate = pdfId.contains("_template");
            String contractDir = isTemplate ? "uploads/contracts/template" : "uploads/contracts/original";
            Path contractPath = Paths.get(contractDir, pdfId);
            
            if (Files.exists(contractPath)) {
                byte[] pdf = Files.readAllBytes(contractPath);
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + pdfId + "\"")
                    .body(pdf);
            }
            
            // 기존 경로들도 확인 (하위 호환성 유지)
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

    // 서명 값 입력
    @PostMapping("/fields/{pdfId}/value") 
    public ResponseEntity<?> addFieldValue(
        @PathVariable String pdfId,
        @RequestParam String fieldName,
        @RequestBody Map<String, Object> value
    ) {
        try {
            // 1. 필드 찾기 - ParticipantPdfField 사용
            ParticipantPdfField field = participantPdfFieldRepository.findByPdfIdAndFieldName(pdfId, fieldName)
                .orElseThrow(() -> new RuntimeException("Field not found: " + fieldName));
            
            // 2. 값 검증
            String fieldType = field.getType();
            Object fieldValue = value.get("value");
            validateFieldValue(fieldType, fieldValue);
            
            // 3. 필드 값 업데이트
            // 민감 정보 필드 (001004_0001: 모바일 번호, 001004_0002: 주민등록번호) 암호화
            String valueToSave = fieldValue.toString();
            
            // format_code_id를 통해 민감 정보 확인
            boolean isSensitiveField = false;
            if (field.getFormat() != null) {
                String formatCode = field.getFormat().getCodeId();
                isSensitiveField = "001004_0001".equals(formatCode) || "001004_0002".equals(formatCode);
                
                if (isSensitiveField) {
                    log.info("민감 정보 필드 식별: {} ({})", fieldName, formatCode);
                    // 민감 정보 암호화
                    valueToSave = encryptionUtil.encrypt(valueToSave);
                    log.info("민감정보 암호화 처리 완료: {}", fieldName);
                }
            }
            
            field.setValue(valueToSave);
            log.info("필드 값 저장: {} = {}", fieldName, valueToSave.substring(0, Math.min(10, valueToSave.length())) + (valueToSave.length() > 10 ? "..." : ""));
            
            // 4. 저장
            ParticipantPdfField updatedField = participantPdfFieldRepository.save(field);
            
            // 5. PDF 업데이트 (선택적)
            if (value.containsKey("updatePdf") && Boolean.TRUE.equals(value.get("updatePdf"))) {
                updatePdfWithFieldValue(pdfId, field);
            }
            
            // 6. DTO로 변환하여 반환 (민감정보는 복호화하여 응답)
            ParticipantPdfFieldDTO responseDto = new ParticipantPdfFieldDTO(updatedField);
            
            // 클라이언트에게 응답할 때는 필드 값이 암호화된 경우 복호화해서 반환
            if (isSensitiveField) {
                String decryptedValue = encryptionUtil.decrypt(updatedField.getValue());
                responseDto.setValue(decryptedValue);
                log.info("응답용 민감정보 복호화 처리: {}", fieldName);
            }
            
            return ResponseEntity.ok(responseDto);
            
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
            case "confirmText":
                if (!(value instanceof String)) {
                    throw new ValidationException("Text value must be string");
                }
                break;
            default:
                throw new ValidationException("Unknown field type: " + fieldType);
        }
    }

    // PDF 업데이트 메소드
    private void updatePdfWithFieldValue(String pdfId, ParticipantPdfField field) throws IOException {
        // 파일 유형 확인 (템플릿 vs 원본)
        boolean isTemplate = pdfId.contains("_template");
        
        // 적절한 메서드로 PDF 로드
        byte[] originalPdf;
        if (isTemplate) {
            originalPdf = pdfStorageService.loadTemplatePdf(pdfId);
        } else {
            originalPdf = pdfStorageService.loadOriginalPdf(pdfId);
        }
        
        // 필드 값 처리 (암호화된 값인 경우 복호화)
        String fieldValue = field.getValue();
        String fieldName = field.getFieldName();
        
        try {
            // 민감 정보 필드인 경우
            boolean isSensitiveField = false;
            if (field.getFormat() != null) {
                String formatCode = field.getFormat().getCodeId();
                isSensitiveField = "001004_0001".equals(formatCode) || "001004_0002".equals(formatCode);
                
                if (isSensitiveField) {
                    // 값이 암호화되어 있는지 확인하고 복호화
                    try {
                        fieldValue = encryptionUtil.decrypt(fieldValue);
                        log.info("민감정보 복호화 처리: {} ({})", fieldName, formatCode);
                    } catch (Exception e) {
                        log.error("민감정보 복호화 중 오류 발생: {}", e.getMessage());
                        // 복호화 실패해도 진행 (원본 값 사용)
                    }
                }
            }
        } catch (Exception e) {
            log.error("필드 값 처리 중 오류: {}", e.getMessage());
        }
        
        // 필드 값 추가
        byte[] processedPdf = pdfProcessingService.addValueToField(
            originalPdf,
            field,
            fieldValue,
            field.getType()
        );
        
        // 적절한 메서드로 PDF 저장
        if (isTemplate) {
            pdfStorageService.saveTemplatePdf(pdfId, processedPdf);
        } else {
            pdfStorageService.saveOriginalPdf(pdfId, processedPdf);
        }
    }


    // 서명된 PDF 저장
    @PostMapping("/download-signed/{pdfId}")
    public ResponseEntity<byte[]> saveSignedPdf(@PathVariable String pdfId) {
        try {
            // 1. 파일명에서 정보 추출 및 서명된 PDF 파일명 생성
            // 기존 pdfId가 "타임스탬프_SIGNING_계약번호_원본파일명_참여자이름.pdf" 형식이라고 가정
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            
            // 6. ParticipantTemplateMapping 조회 (먼저 조회하여 참여자 정보 확보)
            ParticipantTemplateMapping templateMapping = templateMappingRepository.findByPdfId(pdfId)
                .orElseThrow(() -> new RuntimeException("템플릿 매핑 정보를 찾을 수 없습니다: " + pdfId));
            
            ContractParticipant participant = templateMapping.getParticipant();
            Contract contract = participant.getContract();
            
            // 원본 PDF ID에서 정보 추출
            String originalFileName = extractOriginalFileName(pdfId);
            
            // 계약 번호 추출 - 파일명에서 추출 실패 시 계약 객체에서 직접 가져옴
            String contractNumber = contract.getContractNumber();
            
            String participantName = participant.getName().replaceAll("[^\\p{L}\\p{N}]", "_"); // 특수문자 제거
            
            // 새로운 서명된 PDF ID 생성: 타임스탬프_SIGNED_계약번호_원본파일명_참여자이름.pdf
            String signedPdfId = timestamp + "_SIGNED_" + contractNumber + "_" + originalFileName + "_" + participantName + ".pdf";
            
            log.info("서명된 PDF 파일명 생성: {}", signedPdfId);
            
            // 2. participants 폴더에서 원본 PDF 읽기
            Path originalPath = Paths.get(uploadPath, "participants", pdfId);
            if (!Files.exists(originalPath)) {
                throw new RuntimeException("원본 PDF를 찾을 수 없습니다: " + pdfId);
            }
            byte[] originalPdf = Files.readAllBytes(originalPath);
            
            // 3. 해당 PDF의 모든 필드와 값 조회 (ParticipantPdfField 사용)
            List<ParticipantPdfField> fields = participantPdfFieldRepository.findByPdfId(pdfId);
            log.info("Found {} fields with values for PDF: {}", fields.size(), pdfId);
            
            // 4. PDF에 필드 값 추가 (민감 정보는 복호화됨)
            byte[] processedPdf = pdfProcessingService.addValuesToFields(originalPdf, fields);
            
            // 5. 현재 시간(서명 시간) 생성
            LocalDateTime signedTime = LocalDateTime.now();
            
            // 7. 시리얼 넘버 생성
            Long participantId = templateMapping.getParticipant().getId();
            String contractInfo = pdfId + "_" + participantId + "_" + signedTime.toString();
            String serialNumber = generateSerialNumber(contractInfo);
            log.info("Generated serial number for PDF: {}", serialNumber);
            
            // 8. PDF 하단에 서명 시간 및 시리얼 넘버 추가
            String timeInfo = "서명 완료 시간: " + signedTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String serialInfo = "시리얼 넘버: " + serialNumber;
            processedPdf = pdfProcessingService.addSignatureTimeToPdf(
                processedPdf, 
                timeInfo + "    " + serialInfo
            );
            
            // 10. PDF 암호화 (필요한 경우)
            Long contractId = templateMapping.getParticipant().getContract().getId();
            String contractCacheKey = "contract_" + contractId;
            String password;
            
            if (contractPasswordCache.containsKey(contractCacheKey)) {
                // 이미 해당 계약에 대한 비밀번호가 생성되었으면 그것을 사용
                password = contractPasswordCache.get(contractCacheKey);
                log.info("기존 계약 비밀번호 재사용 - 계약ID: {}", contractId);
            } else {
                // 새로운 비밀번호 생성 후 캐시에 저장
                password = generateSecurePassword();
                contractPasswordCache.put(contractCacheKey, password);
                log.info("새 계약 비밀번호 생성 - 계약ID: {}", contractId);
            }
            
            processedPdf = encryptPdfWithPassword(processedPdf, password);
            
            // 11. signed 폴더에 저장
            Path signedDir = Paths.get(uploadPath, "signed");
            if (!Files.exists(signedDir)) {
                Files.createDirectories(signedDir);
            }
            
            Path signedPath = signedDir.resolve(signedPdfId);
            Files.write(signedPath, processedPdf);
            
            // 12. ParticipantTemplateMapping 업데이트
            templateMapping.setSigned(true);
            templateMapping.setSignedAt(signedTime);
            templateMapping.setSignedPdfId(signedPdfId);
            templateMapping.setSerialNumber(serialNumber);
            templateMapping.setDocumentPassword(encryptionUtil.encrypt(password)); // 암호화하여 저장
            
            templateMappingRepository.save(templateMapping);
            log.info("Updated template mapping with signed PDF ID and password: {}", signedPdfId);
            
            // 13. 이메일로 암호 전송 (참여자 정보가 있는 경우)
            if (participant != null && participant.getEmail() != null && !participant.getEmail().isEmpty()) {
                try {
                    // 이메일 복호화
                    String decryptedEmail = encryptionUtil.decrypt(participant.getEmail());
                    
                    // 계약 제목 가져오기
                    String contractTitle = participant.getContract().getTitle();
                    
                    // 서명 완료 URL 생성 (프론트엔드 URL)
                    String redirectUrl = frontendBaseUrl + "/contract-signed?id=" + signedPdfId;
                    
                    // 중복 이메일 발송 방지 키 생성
                    String emailSentKey = "email_password_sent_" + participantId + "_" + contractId;
                    
                    // 이미 비밀번호 이메일을 보냈는지 확인
                    if (!contractPasswordCache.containsKey(emailSentKey)) {
                        // 암호가 포함된 이메일 발송
                        emailService.sendPdfPasswordEmail(
                            decryptedEmail,
                            participant.getName(),
                            password,
                            signedPdfId
                        );
                        
                        // 이메일 발송 표시 (캐시에 저장)
                        contractPasswordCache.put(emailSentKey, "sent");
                        log.info("PDF 암호 이메일 발송 완료: 참여자={}, 이메일={}", 
                                participant.getName(), 
                                decryptedEmail.substring(0, Math.min(3, decryptedEmail.length())) + "***");
                    } else {
                        log.info("PDF 암호 이메일 이미 발송됨 (중복 방지): 참여자={}", participant.getName());
                    }
                } catch (Exception e) {
                    log.error("암호 이메일 발송 오류", e);
                }
            }
            
            // 14. 한글 파일명 인코딩 처리
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
    
    /**
     * PDF ID에서 원본 파일명을 추출합니다.
     */
    private String extractOriginalFileName(String pdfId) {
        // 새 형식 (타임스탬프_SIGNING_계약번호_원본파일명_참여자이름.pdf)에서 추출
        // 앞에서부터 세 번째 언더스코어 이후부터 네 번째 언더스코어 이전까지가 원본 파일명
        
        String[] parts = pdfId.split("_");
        if (parts.length >= 5) { // 최소 5개 이상의 부분으로 나뉘어야 함
            // 원본 파일명 부분 (인덱스 3부터 마지막-1까지)
            StringBuilder fileName = new StringBuilder();
            for (int i = 3; i < parts.length - 1; i++) {
                fileName.append(parts[i]);
                if (i < parts.length - 2) {
                    fileName.append("_"); // 중간에 있던 언더스코어 복원
                }
            }
            return fileName.toString();
        }
        
        // 기존 형식 또는 파싱할 수 없는 경우
        return "document";
    }
    
    /**
     * PDF ID에서 계약 번호를 추출합니다.
     */
    private String extractContractNumber(String pdfId) {
        // 새 형식 (타임스탬프_SIGNING_계약번호_원본파일명_참여자이름.pdf)에서 추출
        // 두 번째 언더스코어 이후부터 세 번째 언더스코어 이전까지가 계약 번호
        
        String[] parts = pdfId.split("_");
        if (parts.length >= 4 && "SIGNING".equals(parts[1])) {
            return parts[2]; // 계약 번호 부분
        }
        
        // 기존 형식 또는 파싱할 수 없는 경우
        return "UNKNOWN";
    }

    // 서명 완료 된 PDF 저장
    @GetMapping("/download-signed-pdf/{pdfId}")
    public ResponseEntity<byte[]> downloadSignedPdf(@PathVariable String pdfId) {
        try {
            log.info("서명된 PDF 다운로드 요청: {}", pdfId);
            
            // 서명된 PDF ID 찾기
            String signedPdfId = pdfId;
            Path signedPath = null;
            
            // pdfId가 원본 PDF ID인 경우, 해당 매핑에서 서명된 PDF ID 조회 시도
            try {
                if (!pdfId.contains("_SIGNED_") && !pdfId.contains("_RESIGNED_")) {
                    ParticipantTemplateMapping mapping = templateMappingRepository.findByPdfId(pdfId)
                        .orElseThrow(() -> new RuntimeException("템플릿 매핑 정보를 찾을 수 없습니다: " + pdfId));
                        
                    if (mapping.getSignedPdfId() == null) {
                        throw new RuntimeException("아직 서명되지 않은 PDF입니다: " + pdfId);
                    }
                        
                    signedPdfId = mapping.getSignedPdfId();
                }
            } catch (Exception e) {
                log.warn("매핑 정보에서 서명된 PDF ID를 찾지 못했습니다. 직접 경로 확인 시도: {}", e.getMessage());
                // 매핑 정보를 찾지 못하더라도 계속 진행 (다음 단계에서 직접 파일 찾기 시도)
            }
            
            // 먼저 "signed" 디렉토리에서 파일 찾기 시도
            signedPath = Paths.get(uploadPath, "signed", signedPdfId);
            
            // "signed" 디렉토리에 없으면 "resigned" 디렉토리에서 찾기 시도
            if (!Files.exists(signedPath)) {
                signedPath = Paths.get(uploadPath, "resigned", signedPdfId);
            }
            
            // 그래도 없으면 에러
            if (!Files.exists(signedPath)) {
                log.warn("서명된 PDF를 찾을 수 없습니다. 경로: {}", signedPath);
                throw new RuntimeException("서명된 PDF를 찾을 수 없습니다: " + signedPdfId);
            }
            
            log.info("서명된 PDF 찾음: {}", signedPath);
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
            List<Map<String, Object>> signedPdfs = mappings.stream()
                .map(mapping -> {
                    // 재서명된 PDF가 있는 경우 재서명 PDF 정보만 반환
                    if (mapping.getResignedPdfId() != null) {
                        Map<String, Object> resignedInfo = new HashMap<>();
                        resignedInfo.put("pdfId", mapping.getResignedPdfId());
                        resignedInfo.put("templateName", mapping.getContractTemplateMapping().getTemplate().getTemplateName() + " (재서명)");
                        resignedInfo.put("downloadUrl", "/api/contract-pdf/download-signed-pdf/" + mapping.getResignedPdfId());
                        resignedInfo.put("isResigned", true);
                        resignedInfo.put("signedAt", mapping.getResignedAt());
                        return resignedInfo;
                    } 
                    // 재서명된 PDF가 없는 경우 원본 서명 PDF 정보 반환
                    else if (mapping.getSignedPdfId() != null) {
                        Map<String, Object> pdfInfo = new HashMap<>();
                        pdfInfo.put("pdfId", mapping.getSignedPdfId());
                        pdfInfo.put("templateName", mapping.getContractTemplateMapping().getTemplate().getTemplateName());
                        pdfInfo.put("downloadUrl", "/api/contract-pdf/download-signed-pdf/" + mapping.getSignedPdfId());
                        pdfInfo.put("isResigned", false);
                        pdfInfo.put("signedAt", mapping.getSignedAt());
                        return pdfInfo;
                    }
                    
                    return null;
                })
                .filter(Objects::nonNull) // null 값 제거
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(signedPdfs);
                
        } catch (Exception e) {
            log.error("Error getting all signed PDFs: {}", participantId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // 파일명 생성 메서드
    // 원본, 템플릿, 서명후 파일명 생성
    private String generatePdfId(String originalFilename, String type) {
        // 현재 시간을 읽기 쉬운 형식으로 포맷팅 (yyyyMMddHHmmss)
        String timestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        
        // 확장자 추출
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex);
            originalFilename = originalFilename.substring(0, dotIndex);
        }
        
        // 파일명에서 특수문자 제거 (한글은 유지)
        // 자바스크립트 정규식과 달리 자바 정규식은 한글을 포함한 유니코드 문자를 보존
        String cleanName = originalFilename.replaceAll("[^\\p{L}\\p{N}]", "_");
        
        // 파일명 조합
        switch (type) {
            case "original":
                return timestamp + "_original_" + cleanName + extension;
            case "template":
                return timestamp + "_template_" + cleanName + extension;
            case "signed":
                return timestamp + "_signed_" + cleanName + extension;
            default:
                return timestamp + "_" + cleanName + extension;
        }
    }

    

    // 서명된 PDF 미리보기
    @GetMapping("/preview-signed-pdf/{pdfId}")
    public ResponseEntity<byte[]> previewSignedPdf(@PathVariable String pdfId) {
        try {
            // 서명된 PDF ID 찾기
            String signedPdfId = pdfId;
            
            // pdfId가 원본 PDF ID인 경우, 해당 매핑에서 서명된 PDF ID 조회
            if (!pdfId.startsWith("signed_") && !pdfId.startsWith("resigned_")) {
                ParticipantTemplateMapping mapping = templateMappingRepository.findByPdfId(pdfId)
                    .orElseThrow(() -> new RuntimeException("템플릿 매핑 정보를 찾을 수 없습니다: " + pdfId));
                
                if (mapping.getSignedPdfId() == null) {
                    throw new RuntimeException("아직 서명되지 않은 PDF입니다: " + pdfId);
                }
                
                signedPdfId = mapping.getSignedPdfId();
            }
            
            // 경로 결정 (일반 서명 또는 재서명)
            Path signedPath;
            if (pdfId.startsWith("resigned_")) {
                signedPath = Paths.get(uploadPath, "resigned", pdfId);
            } else {
                signedPath = Paths.get(uploadPath, "signed", signedPdfId);
            }
            
            if (!Files.exists(signedPath)) {
                log.warn("PDF not found at path: {}", signedPath);
                throw new RuntimeException("서명된 PDF를 찾을 수 없습니다: " + signedPdfId);
            }
            
            byte[] pdfBytes = Files.readAllBytes(signedPath);
            
            // 브라우저에서 바로 열리도록 inline으로 설정
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + signedPdfId + "\"")
                .header("X-Frame-Options", "SAMEORIGIN")
                .header("Content-Security-Policy", "frame-ancestors 'self'")
                .body(pdfBytes);
                
        } catch (Exception e) {
            log.error("서명된 PDF 미리보기 오류: {}", pdfId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * 계약 정보를 기반으로 50자리의 고유한 시리얼 넘버를 생성합니다.
     * 
     * @param contractInfo 계약 관련 정보
     * @return 50자리 시리얼 넘버
     */
    private String generateSerialNumber(String contractInfo) {
        try {
            // 1. UUID 생성 (36자리)
            String uuid = java.util.UUID.randomUUID().toString().replace("-", "");
            
            // 2. 계약 정보의 SHA-256 해시 생성
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(contractInfo.getBytes(StandardCharsets.UTF_8));
            
            // 3. 해시를 16진수 문자열로 변환
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            // 4. UUID(32자리, 하이픈 제거)와 해시의 일부(18자리)를 조합하여 50자리 시리얼 넘버 생성
            String serialNumber = uuid + hexString.toString().substring(0, 18);
            
            // 5. 시리얼 넘버에 하이픈을 추가하여 가독성 향상 (선택사항)
            // XXXXX-XXXXX-XXXXX-XXXXX-XXXXX-XXXXX-XXXXX-XXXXX-XXXXX-XXXXX 형식
            StringBuilder formattedSerial = new StringBuilder();
            for (int i = 0; i < serialNumber.length(); i++) {
                if (i > 0 && i % 5 == 0) {
                    formattedSerial.append('-');
                }
                formattedSerial.append(serialNumber.charAt(i));
            }
            
            return formattedSerial.toString();
        } catch (Exception e) {
            log.error("시리얼 넘버 생성 중 오류 발생", e);
            // 오류 발생 시 대체 시리얼 넘버 반환
            return "ERR-" + System.currentTimeMillis() + "-" + 
                   contractInfo.hashCode() + "-" + 
                   java.util.UUID.randomUUID().toString().substring(0, 10);
        }
    }

    /**
     * 안전한 랜덤 비밀번호를 생성합니다.
     * @return 생성된 비밀번호
     */
    private String generateSecurePassword() {
        // 문자, 숫자, 특수문자 조합으로 8자리 비밀번호 생성
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*";
        StringBuilder sb = new StringBuilder();
        SecureRandom random = new SecureRandom();
        
        // 최소 1개의 숫자, 1개의 특수문자, 1개의 대문자를 포함하도록 설정
        sb.append(chars.substring(0, 26).charAt(random.nextInt(26))); // 대문자
        sb.append(chars.substring(52, 62).charAt(random.nextInt(10))); // 숫자
        sb.append(chars.substring(62).charAt(random.nextInt(7))); // 특수문자
        
        // 나머지 5개 문자는 전체 문자셋에서 랜덤 선택
        for (int i = 0; i < 5; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        // 문자열 섞기
        char[] password = sb.toString().toCharArray();
        for (int i = 0; i < password.length; i++) {
            int j = random.nextInt(password.length);
            char temp = password[i];
            password[i] = password[j];
            password[j] = temp;
        }
        
        return new String(password);
    }
    
    /**
     * PDF에 암호를 적용합니다.
     * 
     * @param pdf PDF 바이트 배열
     * @param password 적용할 암호
     * @return 암호화된 PDF 바이트 배열
     */
    private byte[] encryptPdfWithPassword(byte[] pdf, String password) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfReader reader = new PdfReader(pdf);
            PdfStamper stamper = new PdfStamper(reader, baos);
            
            // 암호 설정 및 권한 제한 (인쇄만 허용)
            stamper.setEncryption(
                password.getBytes(),          // 문서 열기 암호
                password.getBytes(),          // 권한 암호 (동일하게 설정)
                PdfWriter.ALLOW_PRINTING,     // 인쇄 허용, 나머지 모두 제한
                PdfWriter.ENCRYPTION_AES_128  // AES 128비트 암호화
            );
            
            stamper.close();
            reader.close();
            
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("PDF 암호화 실패", e);
            throw new RuntimeException("PDF 암호화 중 오류 발생", e);
        }
    }

    // 캐시 정리 메서드 (필요시 주기적으로 호출)
    @Scheduled(fixedRate = 86400000) // 24시간마다 실행
    public void cleanPasswordCache() {
        log.info("계약 비밀번호 캐시 정리 - 현재 캐시 크기: {}", contractPasswordCache.size());
        contractPasswordCache.clear();
        log.info("계약 비밀번호 캐시 정리 완료");
    }

    private void saveSignedPdf(Long participantId, Long contractId, Long templateId, 
                             String signatureImgPath, String signatureDateTime) {
        try {
            log.info("서명된 PDF 저장 시작 - 참여자ID: {}, 계약ID: {}, 템플릿ID: {}", participantId, contractId, templateId);
            
            // 저장할 각종 정보 조회
            ContractParticipant participant = participantRepository.findById(participantId)
                    .orElseThrow(() -> new RuntimeException("참여자를 찾을 수 없습니다: " + participantId));
                    
            ParticipantTemplateMapping mapping = templateMappingRepository.findByParticipant_IdAndContractTemplateMapping_Template_Id(participantId, templateId)
                    .orElseThrow(() -> new RuntimeException("참여자 템플릿 매핑을 찾을 수 없습니다"));
            
            ContractTemplate template = contractTemplateRepository.findById(templateId)
                    .orElseThrow(() -> new RuntimeException("템플릿을 찾을 수 없습니다: " + templateId));
            
            // PDF 파일 경로 가져오기
            String pdfFilePath = mapping.getPdfId();
            if (pdfFilePath == null || pdfFilePath.isEmpty()) {
                throw new RuntimeException("PDF 파일 경로가 없습니다");
            }
            
            // 새로운 서명된 PDF 파일명 생성
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            
            // 계약번호는 파일명에서 추출하지 않고, 계약 객체에서 직접 가져옴
            String contractNumber = participant.getContract().getContractNumber();
            
            String originalFileName = extractOriginalFileName(pdfFilePath);
            String participantName = participant.getName().replaceAll("[^\\p{L}\\p{N}]", "_"); // 특수문자 제거
            
            // 새로운 형식으로 서명된 PDF 파일명 생성
            String signedPdfFileName = timestamp + "_SIGNED_" + contractNumber + "_" + originalFileName + "_" + participantName + ".pdf";
            log.info("새로운 서명된 PDF 파일명 생성: {}", signedPdfFileName);
            
            String fullPdfPath = uploadPath + "/" + pdfFilePath;
            File pdfFile = new File(fullPdfPath);
            
            if (!pdfFile.exists()) {
                throw new RuntimeException("PDF 파일을 찾을 수 없습니다: " + fullPdfPath);
            }
            
            // 시그너처 이미지 파일 가져오기
            File signatureImgFile = new File(signatureImgPath);
            if (!signatureImgFile.exists()) {
                throw new RuntimeException("서명 이미지 파일을 찾을 수 없습니다: " + signatureImgPath);
            }
            
            // 서명된 PDF 저장 경로
            String signedPdfPath = uploadPath + "/signed/" + signedPdfFileName;
            
            // 계약별 비밀번호 생성 또는 재사용
            String password;
            String contractCacheKey = "contract_" + contractId;
            if (contractPasswordCache.containsKey(contractCacheKey)) {
                // 이미 해당 계약에 대한 비밀번호가 생성되었으면 그것을 사용
                password = contractPasswordCache.get(contractCacheKey);
                log.info("기존 계약 비밀번호 재사용 - 계약ID: {}", contractId);
            } else {
                // 새로운 비밀번호 생성 후 캐시에 저장
                password = generateSecurePassword();
                contractPasswordCache.put(contractCacheKey, password);
                log.info("새 계약 비밀번호 생성 - 계약ID: {}", contractId);
            }
            
            // signed 디렉토리 확인 및 생성
            Path signedDir = Paths.get(uploadPath, "signed");
            if (!Files.exists(signedDir)) {
                Files.createDirectories(signedDir);
            }
            
            // 서명 이미지를 PDF에 추가하고 시리얼 넘버 워터마크 삽입
            try (PDDocument document = PDDocument.load(pdfFile)) {
                // 시리얼 넘버 생성 또는 가져오기
                String serialNumber = mapping.getSerialNumber();
                if (serialNumber == null || serialNumber.isEmpty()) {
                    // 계약 정보로 시리얼 넘버 생성
                    String contractInfo = pdfFile.getName() + "_" + participantId + "_" + LocalDateTime.now().toString();
                    serialNumber = generateSerialNumber(contractInfo);
                    mapping.setSerialNumber(serialNumber);
                }
                
                // 워터마크 추가 (시리얼 넘버 + 서명 시간)
                PDPage firstPage = document.getPage(0);
                PDPageContentStream contentStream = new PDPageContentStream(document, firstPage, PDPageContentStream.AppendMode.APPEND, true, true);
                
                // 워터마크 스타일 설정
                contentStream.setFont(PDType1Font.HELVETICA, 8);
                contentStream.setNonStrokingColor(100, 100, 100); // 회색
                
                // 왼쪽 하단에 시리얼 넘버 추가
                contentStream.beginText();
                contentStream.newLineAtOffset(30, 30);
                contentStream.showText("Serial: " + serialNumber);
                contentStream.endText();
                
                // 서명 시간 추가
                contentStream.beginText();
                contentStream.newLineAtOffset(30, 20);
                contentStream.showText("Signed: " + signatureDateTime);
                contentStream.endText();
                
                contentStream.close();
                
                // PDF 바이트 스트림으로 변환
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                document.save(baos);
                
                // PDF 암호화 적용
                byte[] encryptedPdf = encryptPdfWithPassword(baos.toByteArray(), password);
                
                // 암호화된 PDF 파일로 저장
                try (FileOutputStream fos = new FileOutputStream(signedPdfPath)) {
                    fos.write(encryptedPdf);
                }
            }
            
            // ParticipantTemplateMapping 업데이트
            mapping.setSigned(true);
            mapping.setSignedAt(LocalDateTime.now());
            mapping.setSignedPdfId(signedPdfFileName);
            mapping.setDocumentPassword(encryptionUtil.encrypt(password)); // 암호화하여 저장
            
            templateMappingRepository.save(mapping);
            log.info("Updated template mapping with signed PDF ID and password: {}", signedPdfFileName);
            
            // 참여자의 이메일이 있으면 비밀번호 이메일 발송
            // 이미 보낸 비밀번호는 다시 보내지 않도록 처리
            if (participant.getEmail() != null && !participant.getEmail().isEmpty()) {
                try {
                    String decryptedEmail = encryptionUtil.decrypt(participant.getEmail());
                    String contractTitle = participant.getContract().getTitle();
                    
                    // 이미 이메일을 보냈는지 확인하기 위한 키 생성
                    String emailSentKey = "email_password_sent_" + participantId + "_" + contractId;
                    
                    // 이미 비밀번호 이메일을 보냈는지 확인
                    if (!contractPasswordCache.containsKey(emailSentKey)) {
                        // 암호가 포함된 이메일 발송
                        emailService.sendPdfPasswordEmail(
                            decryptedEmail,
                            participant.getName(),
                            password,
                            signedPdfFileName
                        );
                        
                        // 이메일 발송 표시 (캐시에 저장)
                        contractPasswordCache.put(emailSentKey, "sent");
                        log.info("PDF 암호 이메일 발송 완료: 참여자={}, 이메일={}", 
                                participant.getName(), 
                                decryptedEmail.substring(0, Math.min(3, decryptedEmail.length())) + "***");
                    } else {
                        log.info("PDF 암호 이메일 이미 발송됨 (중복 방지): 참여자={}", participant.getName());
                    }
                } catch (Exception e) {
                    log.error("PDF 암호 이메일 발송 실패: {}", e.getMessage());
                    // 이메일 전송 실패는 치명적 오류가 아니므로 진행
                }
            }
            
            log.info("서명된 PDF 저장 완료 - 참여자: {}, 파일: {}", participant.getName(), signedPdfFileName);
        } catch (Exception e) {
            log.error("서명된 PDF 저장 실패: {}", e.getMessage(), e);
            throw new RuntimeException("서명된 PDF 저장 중 오류 발생: " + e.getMessage(), e);
        }
    }

    /**
     * 계약서 비밀번호 조회
     */
    @GetMapping("/password/{signedPdfId}")
    public ResponseEntity<Map<String, String>> getPdfPassword(
            @PathVariable String signedPdfId,
            @RequestParam(required = false) String token) {
        try {
            // URL 디코딩
            String decodedPdfId = java.net.URLDecoder.decode(signedPdfId, StandardCharsets.UTF_8.name());
            log.info("디코딩된 PDF ID: {}", decodedPdfId);
            
            // 1. 템플릿 매핑 정보 조회 (서명된 PDF ID로 조회 또는 재서명된 PDF ID로 조회)
            ParticipantTemplateMapping templateMapping = null;
            
            // 먼저 서명된 PDF ID로 조회
            try {
                templateMapping = templateMappingRepository.findBySignedPdfId(decodedPdfId)
                    .orElse(null);
                
                if (templateMapping == null) {
                    log.info("서명된 PDF ID로 매핑을 찾지 못했습니다. 재서명된 PDF ID로 조회합니다: {}", decodedPdfId);
                    
                    // 서명된 PDF ID로 찾지 못했으면 재서명된 PDF ID로 조회
                    templateMapping = templateMappingRepository.findByResignedPdfId(decodedPdfId)
                        .orElseThrow(() -> new RuntimeException("템플릿 매핑 정보를 찾을 수 없습니다: " + decodedPdfId));
                    
                    log.info("재서명된 PDF ID로 매핑을 찾았습니다: {}", decodedPdfId);
                }
            } catch (Exception e) {
                log.error("템플릿 매핑 정보 조회 중 오류 발생: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("message", "매핑 정보를 찾을 수 없습니다: " + e.getMessage()));
            }
            
            // 2. 참여자 정보 조회
            ContractParticipant participant = templateMapping.getParticipant();
            
            // 3. 인증 확인 (토큰이 제공된 경우 userDetails 체크 생략)
            if (token == null || token.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Collections.singletonMap("message", "인증 정보가 없습니다."));
            }
            
            // 4. 저장된 암호화된 비밀번호 조회
            String encryptedPassword = templateMapping.getDocumentPassword();
            if (encryptedPassword == null || encryptedPassword.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // 5. 비밀번호 복호화
            String password = encryptionUtil.decrypt(encryptedPassword);
            
            Map<String, String> response = new HashMap<>();
            response.put("password", password);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("계약서 비밀번호 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 계약서 비밀번호 이메일 재전송
     */
    @PostMapping("/password/{signedPdfId}/send-email")
    public ResponseEntity<Map<String, String>> sendPasswordEmail(
            @PathVariable String signedPdfId,
            @RequestParam(required = false) String token) {
        try {
            // URL 디코딩
            String decodedPdfId = java.net.URLDecoder.decode(signedPdfId, StandardCharsets.UTF_8.name());
            log.info("디코딩된 PDF ID: {}", decodedPdfId);
            
            // 1. 템플릿 매핑 정보 조회 (서명된 PDF ID로 조회 또는 재서명된 PDF ID로 조회)
            ParticipantTemplateMapping templateMapping = null;
            
            // 먼저 서명된 PDF ID로 조회
            try {
                templateMapping = templateMappingRepository.findBySignedPdfId(decodedPdfId)
                    .orElse(null);
                
                if (templateMapping == null) {
                    log.info("서명된 PDF ID로 매핑을 찾지 못했습니다. 재서명된 PDF ID로 조회합니다: {}", decodedPdfId);
                    
                    // 서명된 PDF ID로 찾지 못했으면 재서명된 PDF ID로 조회
                    templateMapping = templateMappingRepository.findByResignedPdfId(decodedPdfId)
                        .orElseThrow(() -> new RuntimeException("템플릿 매핑 정보를 찾을 수 없습니다: " + decodedPdfId));
                    
                    log.info("재서명된 PDF ID로 매핑을 찾았습니다: {}", decodedPdfId);
                }
            } catch (Exception e) {
                log.error("템플릿 매핑 정보 조회 중 오류 발생: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("message", "매핑 정보를 찾을 수 없습니다: " + e.getMessage()));
            }
            
            // 2. 참여자 정보 조회
            ContractParticipant participant = templateMapping.getParticipant();
            
            // 3. 인증 확인 (토큰이 제공된 경우 userDetails 체크 생략)
            if (token == null || token.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Collections.singletonMap("message", "인증 정보가 없습니다."));
            }
            
            // 4. 저장된 암호화된 비밀번호 조회
            String encryptedPassword = templateMapping.getDocumentPassword();
            if (encryptedPassword == null || encryptedPassword.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("message", "비밀번호 정보가 없습니다."));
            }
            
            // 5. 비밀번호 복호화
            String password = encryptionUtil.decrypt(encryptedPassword);
            
            // 6. 이메일로 비밀번호 전송
            if (participant.getEmail() != null && !participant.getEmail().isEmpty()) {
                String decryptedEmail = encryptionUtil.decrypt(participant.getEmail());
                emailService.sendPdfPasswordEmail(
                    decryptedEmail,
                    participant.getName(),
                    password,
                    decodedPdfId
                );
                
                Map<String, String> response = new HashMap<>();
                response.put("message", "비밀번호가 이메일로 전송되었습니다.");
                response.put("email", decryptedEmail.substring(0, 3) + "***" + 
                    decryptedEmail.substring(decryptedEmail.indexOf('@')));
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", "이메일 정보가 없습니다."));
            }
            
        } catch (Exception e) {
            log.error("계약서 비밀번호 이메일 전송 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Collections.singletonMap("message", "비밀번호 이메일 전송 중 오류가 발생했습니다."));
        }
    }

} 