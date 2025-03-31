package com.inspection.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.inspection.dto.ParticipantDocumentDto;
import com.inspection.entity.ParticipantDocument;
import com.inspection.service.DocsFileStorageService;
import com.inspection.service.ParticipantDocumentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractDocsController {

    private final ParticipantDocumentService participantDocumentService;
    private final DocsFileStorageService docsFileStorageService;

    /**
     * 계약의 모든 참여자 문서 요구사항 조회
     * 참여자별로 그룹화하여 반환
     * 
     * @param contractId 계약 ID
     * @return 참여자별 문서 목록
     */
    @GetMapping("/{contractId}/documents")
    public ResponseEntity<Map<Long, List<ParticipantDocumentDto>>> getContractDocuments(
            @PathVariable Long contractId) {
        
        log.info("계약 문서 전체 조회 요청: 계약ID={}", contractId);
        
        // 계약의 모든 참여자 문서 조회
        List<ParticipantDocument> allDocuments = participantDocumentService
                .getAllDocumentsByContractId(contractId);
        
        // 엔티티를 DTO로 변환 후 참여자 ID별로 그룹화
        Map<Long, List<ParticipantDocumentDto>> documentsByParticipant = allDocuments.stream()
                .map(ParticipantDocumentDto::fromEntity)
                .collect(Collectors.groupingBy(ParticipantDocumentDto::getParticipantId));
        
        return ResponseEntity.ok(documentsByParticipant);
    }
    
    /**
     * 특정 참여자의 문서 요구사항 조회
     * 
     * @param contractId 계약 ID
     * @param participantId 참여자 ID
     * @return 참여자의 문서 목록
     */
    @GetMapping("/{contractId}/participants/{participantId}/documents")
    public ResponseEntity<List<ParticipantDocumentDto>> getParticipantDocuments(
            @PathVariable Long contractId,
            @PathVariable Long participantId) {
        
        log.info("참여자 문서 조회 요청: 계약ID={}, 참여자ID={}", contractId, participantId);
        
        List<ParticipantDocument> documents = participantDocumentService
                .getParticipantDocuments(contractId, participantId);
        
        List<ParticipantDocumentDto> documentDtos = documents.stream()
                .map(ParticipantDocumentDto::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(documentDtos);
    }
    
    /**
     * 특정 문서 상세 조회
     * 
     * @param documentId 문서 ID
     * @return 문서 상세 정보
     */
    @GetMapping("/documents/{documentId}")
    public ResponseEntity<ParticipantDocumentDto> getDocumentDetail(
            @PathVariable Long documentId) {
        
        log.info("문서 상세 조회 요청: 문서ID={}", documentId);
        
        ParticipantDocument document = participantDocumentService
                .getDocumentById(documentId);
        
        ParticipantDocumentDto documentDto = ParticipantDocumentDto.fromEntity(document);
        
        return ResponseEntity.ok(documentDto);
    }
    
    /**
     * 문서 파일 다운로드
     * 
     * @param documentId 문서 ID
     * @return 파일 리소스
     */
    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long documentId) throws IOException {
        log.info("문서 파일 다운로드 요청: 문서ID={}", documentId);
        
        ParticipantDocument document = participantDocumentService.getDocumentById(documentId);
        
        if (document.getFileId() == null || document.getFileId().isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // 파일 조회
        byte[] fileBytes = docsFileStorageService.getFile(document.getFileId());
        ByteArrayResource resource = new ByteArrayResource(fileBytes);
        
        // 한글 파일명 처리
        String encodedFilename = URLEncoder.encode(document.getOriginalFileName(), StandardCharsets.UTF_8.toString())
                .replaceAll("\\+", "%20");
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }
    
    /**
     * 문서 파일 미리보기
     * 
     * @param documentId 문서 ID
     * @return 파일 리소스
     */
    @GetMapping("/documents/{documentId}/preview")
    public ResponseEntity<Resource> previewDocument(@PathVariable Long documentId) throws IOException {
        log.info("문서 파일 미리보기 요청: 문서ID={}", documentId);
        
        ParticipantDocument document = participantDocumentService.getDocumentById(documentId);
        
        if (document.getFileId() == null || document.getFileId().isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // 파일 조회
        byte[] fileBytes = docsFileStorageService.getFile(document.getFileId());
        ByteArrayResource resource = new ByteArrayResource(fileBytes);
        
        // 파일 타입 확인
        String contentType = determineContentType(document.getOriginalFileName());
        
        // 한글 파일명 처리
        String encodedFilename = URLEncoder.encode(document.getOriginalFileName(), StandardCharsets.UTF_8.toString())
                .replaceAll("\\+", "%20");
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }
    
    /**
     * 파일명에 따른 Content-Type 결정
     */
    private String determineContentType(String fileName) {
        if (fileName == null) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        
        String lowerFileName = fileName.toLowerCase();
        
        if (lowerFileName.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF_VALUE;
        } else if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG_VALUE;
        } else if (lowerFileName.endsWith(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        } else if (lowerFileName.endsWith(".gif")) {
            return MediaType.IMAGE_GIF_VALUE;
        } else if (lowerFileName.endsWith(".txt")) {
            return MediaType.TEXT_PLAIN_VALUE;
        } else if (lowerFileName.endsWith(".html") || lowerFileName.endsWith(".htm")) {
            return MediaType.TEXT_HTML_VALUE;
        } else if (lowerFileName.endsWith(".doc") || lowerFileName.endsWith(".docx")) {
            return "application/msword";
        } else if (lowerFileName.endsWith(".xls") || lowerFileName.endsWith(".xlsx")) {
            return "application/vnd.ms-excel";
        } else if (lowerFileName.endsWith(".ppt") || lowerFileName.endsWith(".pptx")) {
            return "application/vnd.ms-powerpoint";
        }
        
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    /**
     * 문서 파일 삭제
     * 
     * @param documentId 문서 ID
     * @return 업데이트된 문서 정보
     */
    @DeleteMapping("/documents/{documentId}/file")
    public ResponseEntity<ParticipantDocumentDto> deleteDocumentFile(@PathVariable Long documentId) throws IOException {
        log.info("문서 파일 삭제 요청: 문서ID={}", documentId);
        
        try {
            // 파일 삭제 및 문서 정보 업데이트
            ParticipantDocument updatedDocument = participantDocumentService.deleteDocumentFile(documentId);
            
            // 응답 데이터 변환
            ParticipantDocumentDto documentDto = ParticipantDocumentDto.fromEntity(updatedDocument);
            
            return ResponseEntity.ok(documentDto);
        } catch (Exception e) {
            log.error("문서 파일 삭제 실패: 문서ID={}", documentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
