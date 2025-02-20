package com.inspection.service;

import com.inspection.dto.ContractResponse;
import com.inspection.entity.Contract;
import com.inspection.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.inspection.dto.SignaturePositionRequest;
import com.inspection.service.PdfService;
import com.inspection.entity.ContractStatus;
import com.inspection.entity.Contract;
import com.inspection.dto.SignatureRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractService {
    
    private final ContractRepository contractRepository;
    private final FileService fileService;
    private final PdfService pdfService;

    @Transactional
    public ContractResponse uploadContract(
            String title, 
            String contracteeName, 
            String contracteeEmail,
            String contracteePhoneNumber,
            String contractorName,
            String contractorEmail,
            String contractorPhoneNumber,
            String contractType,
            String description,
            MultipartFile file) {
        
        String pdfUrl = fileService.savePdfFile(file);
        
        Contract contract = new Contract();
        contract.setTitle(title);
        
        // 피계약자 정보 설정
        contract.setContracteeName(contracteeName);
        contract.setContracteeEmail(contracteeEmail);
        contract.setContracteePhoneNumber(contracteePhoneNumber);
        
        // 계약자 정보 설정
        contract.setContractorName(contractorName);
        contract.setContractorEmail(contractorEmail);
        contract.setContractorPhoneNumber(contractorPhoneNumber);
        
        // 기타 정보 설정
        contract.setContractType(contractType);
        contract.setDescription(description);
        contract.setPdfUrl(pdfUrl);
        contract.setOriginalFileName(file.getOriginalFilename());
        contract.setFileSize(file.getSize());
        contract.setContractNumber(generateContractNumber());
        
        Contract savedContract = contractRepository.save(contract);
        
        return mapToResponse(savedContract);
    }

    private String generateContractNumber() {
        return "CT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private ContractResponse mapToResponse(Contract contract) {
        return ContractResponse.builder()
            .id(contract.getId())
            .title(contract.getTitle())
            // 피계약자 정보
            .contracteeName(contract.getContracteeName())
            .contracteeEmail(contract.getContracteeEmail())
            .contracteePhoneNumber(contract.getContracteePhoneNumber())
            // 계약자 정보
            .contractorName(contract.getContractorName())
            .contractorEmail(contract.getContractorEmail())
            .contractorPhoneNumber(contract.getContractorPhoneNumber())
            // 기타 정보
            .contractType(contract.getContractType())
            .description(contract.getDescription())
            .pdfUrl(contract.getPdfUrl())
            .signedPdfUrl(contract.getSignedPdfUrl())
            .status(contract.getStatus())
            .originalFileName(contract.getOriginalFileName())
            .fileSize(contract.getFileSize())
            .expirationDate(contract.getExpirationDate())
            .signedDate(contract.getSignedDate())
            .createdDate(contract.getCreatedDate())
            .contractNumber(contract.getContractNumber())
            .isEmailSent(contract.isEmailSent())
            .emailSentDate(contract.getEmailSentDate())
            .build();
    }

    // 계약서 목록 조회
    @Transactional(readOnly = true)
    public List<ContractResponse> getContracts() {
        return contractRepository.findAll().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    // 계약서 상세 조회
    @Transactional(readOnly = true)
    public ContractResponse getContract(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new RuntimeException("계약서를 찾을 수 없습니다."));
        return mapToResponse(contract);
    }

    // PDF 파일 다운로드
    public ResponseEntity<Resource> downloadPdf(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new RuntimeException("계약서를 찾을 수 없습니다."));
        
        try {
            Path filePath = Paths.get(fileService.getUploadPath(), contract.getPdfUrl());
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                    .body(resource);
            } else {
                throw new RuntimeException("파일을 읽을 수 없습니다.");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("파일을 찾을 수 없습니다.", e);
        }
    }

    // 서명 위치 저장
    @Transactional
    public ContractResponse saveSignaturePosition(Long contractId, SignaturePositionRequest request) {
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new RuntimeException("계약서를 찾을 수 없습니다."));
            
        // JSON으로 변환하여 저장
        ObjectMapper mapper = new ObjectMapper();
        try {
            String positionJson = mapper.writeValueAsString(request);
            contract.setSignaturePosition(positionJson);
            return mapToResponse(contractRepository.save(contract));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("서명 위치 저장에 실패했습니다.", e);
        }
    }

    // 서명 이미지 업로드
    @Transactional
    public ContractResponse uploadSignature(Long contractId, MultipartFile signatureFile) {
        log.info("서명 이미지 업로드 시작 - 계약서 ID: {}", contractId);
        
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new RuntimeException("계약서를 찾을 수 없습니다."));
        
        try {
            // 서명 이미지 저장
            log.info("서명 이미지 저장 시작");
            String signatureImagePath = fileService.saveSignatureImage(signatureFile);
            log.info("서명 이미지 저장 완료: {}", signatureImagePath);
            
            // 서명 위치 정보 확인
            if (contract.getSignaturePosition() == null) {
                throw new RuntimeException("서명 위치 정보가 없습니다.");
            }
            
            // 원본 PDF에 서명 추가
            ObjectMapper mapper = new ObjectMapper();
            SignaturePositionRequest position = mapper.readValue(
                contract.getSignaturePosition(), 
                SignaturePositionRequest.class
            );
            log.info("서명 위치 정보 읽기 완료: {}", position);
            
            String signedPdfPath = pdfService.signPdf(
                contract.getPdfUrl(),
                signatureImagePath,
                position
            );
            log.info("서명된 PDF 생성 완료: {}", signedPdfPath);
            
            // 계약서 상태 업데이트
            contract.setSignedPdfUrl(signedPdfPath);
            contract.setSignedDate(LocalDateTime.now());
            contract.setStatus(ContractStatus.SIGNED);
            
            Contract savedContract = contractRepository.save(contract);
            log.info("계약서 상태 업데이트 완료");
            
            return mapToResponse(savedContract);
        } catch (Exception e) {
            log.error("서명 처리 중 오류 발생", e);
            throw new RuntimeException("서명 처리에 실패했습니다: " + e.getMessage(), e);
        }
    }

    // 서명된 PDF 파일 다운로드
    public ResponseEntity<Resource> downloadSignedPdf(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new RuntimeException("계약서를 찾을 수 없습니다."));
        
        if (contract.getSignedPdfUrl() == null) {
            throw new RuntimeException("서명된 PDF가 없습니다.");
        }
        
        try {
            Path filePath = Paths.get(fileService.getUploadPath(), "signed", contract.getSignedPdfUrl());
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                    .body(resource);
            } else {
                throw new RuntimeException("파일을 읽을 수 없습니다.");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("파일을 찾을 수 없습니다.", e);
        }
    }

    @Transactional
    public ContractResponse addSignature(Long contractId, SignatureRequest request) {
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new RuntimeException("계약서를 찾을 수 없습니다."));
        
        try {
            String signedPdfPath = pdfService.addSignatureOverlay(
                contract.getPdfUrl(), 
                request
            );
            
            // 계약서 상태 업데이트
            contract.setSignedPdfUrl(signedPdfPath);
            contract.setSignedDate(LocalDateTime.now());
            contract.setStatus(ContractStatus.SIGNED);
            
            return mapToResponse(contractRepository.save(contract));
        } catch (IOException e) {
            throw new RuntimeException("서명 처리 실패", e);
        }
    }
} 