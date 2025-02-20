package com.inspection.controller;

import com.inspection.dto.ContractResponse;
import com.inspection.service.ContractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.net.MalformedURLException;
import com.inspection.dto.SignaturePositionRequest;
import com.inspection.dto.SignatureRequest;


@Slf4j
@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {
    
    private final ContractService contractService;

    @PostMapping
    public ResponseEntity<ContractResponse> uploadContract(
            @RequestParam("title") String title,
            @RequestParam("contracteeName") String contracteeName,
            @RequestParam("contracteeEmail") String contracteeEmail,
            @RequestParam("contracteePhoneNumber") String contracteePhoneNumber,
            @RequestParam("contractorName") String contractorName,
            @RequestParam("contractorEmail") String contractorEmail,
            @RequestParam("contractorPhoneNumber") String contractorPhoneNumber,
            @RequestParam("contractType") String contractType,
            @RequestParam("description") String description,
            @RequestParam("file") MultipartFile file) {
        
        log.info("계약서 업로드 요청 - 제목: {}, 계약자: {}, 피계약자: {}", 
            title, contractorName, contracteeName);
        
        ContractResponse response = contractService.uploadContract(
            title, contracteeName, contracteeEmail, contracteePhoneNumber,
            contractorName, contractorEmail, contractorPhoneNumber,
            contractType, description, file);
            
        return ResponseEntity.ok(response);
    }

    // 계약서 목록 조회
    @GetMapping
    public ResponseEntity<List<ContractResponse>> getContracts() {
        return ResponseEntity.ok(contractService.getContracts());
    }

    // 계약서 상세 조회
    @GetMapping("/{contractId}")
    public ResponseEntity<ContractResponse> getContract(@PathVariable Long contractId) {
        return ResponseEntity.ok(contractService.getContract(contractId));
    }

    // PDF 파일 다운로드
    @GetMapping("/{contractId}/pdf")
    public ResponseEntity<Resource> downloadPdf(@PathVariable Long contractId) {
        return contractService.downloadPdf(contractId);
    }

    // 서명된 PDF 파일 다운로드
    @GetMapping("/{contractId}/signed-pdf")
    public ResponseEntity<Resource> downloadSignedPdf(@PathVariable Long contractId) {
        return contractService.downloadSignedPdf(contractId);
    }

    // 서명 위치 저장
    @PostMapping("/{contractId}/signature-position")
    public ResponseEntity<ContractResponse> saveSignaturePosition(
        @PathVariable Long contractId,
        @RequestBody SignaturePositionRequest request) {
        return ResponseEntity.ok(contractService.saveSignaturePosition(contractId, request));
    }

    // 서명 이미지 업로드
    @PostMapping("/{contractId}/signature")
    public ResponseEntity<ContractResponse> uploadSignature(
        @PathVariable Long contractId,
        @RequestParam("signature") MultipartFile signatureFile) {
        return ResponseEntity.ok(contractService.uploadSignature(contractId, signatureFile));
    }

    @PostMapping("/{contractId}/sign")
    public ResponseEntity<ContractResponse> signContract(
            @PathVariable Long contractId,
            @RequestBody SignatureRequest request) {
        return ResponseEntity.ok(contractService.addSignature(contractId, request));
    }
} 