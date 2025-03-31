package com.inspection.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.inspection.entity.Code;
import com.inspection.entity.Contract;
import com.inspection.entity.ContractParticipant;
import com.inspection.entity.ParticipantDocument;
import com.inspection.repository.CodeRepository;
import com.inspection.repository.ContractParticipantRepository;
import com.inspection.repository.ContractRepository;
import com.inspection.repository.ParticipantDocumentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ParticipantDocumentService {
    
    private final ParticipantDocumentRepository participantDocumentRepository;
    private final ContractRepository contractRepository;
    private final ContractParticipantRepository participantRepository;
    private final CodeRepository codeRepository;
    private final DocsFileStorageService docsFileStorageService; // DocsFileStorageService 사용
    
    /**
     * 계약 생성 시 필요한 문서를 참여자별로 등록
     */
    public List<ParticipantDocument> createRequiredDocumentsForContract(
            Long contractId, List<String> documentCodeIds) {
        
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("계약을 찾을 수 없습니다: " + contractId));
        
        List<ContractParticipant> participants = participantRepository.findByContractId(contractId);
        List<ParticipantDocument> createdDocuments = new ArrayList<>();
        
        // 각 참여자에 대해 각 문서 유형별로 레코드 생성
        for (ContractParticipant participant : participants) {
            for (String documentCodeId : documentCodeIds) {
                Code documentCode = codeRepository.findById(documentCodeId)
                        .orElseThrow(() -> new RuntimeException("문서 코드를 찾을 수 없습니다: " + documentCodeId));
                
                ParticipantDocument document = new ParticipantDocument();
                document.setContract(contract);
                document.setParticipant(participant);
                document.setDocumentCode(documentCode);
                document.setRequired(true);
                document.setCreatedAt(LocalDateTime.now());
                
                createdDocuments.add(participantDocumentRepository.save(document));
                log.info("참여자 문서 등록: 계약ID={}, 참여자ID={}, 문서코드={}", 
                        contractId, participant.getId(), documentCodeId);
            }
        }
        
        return createdDocuments;
    }
    
    /**
     * 참여자가 문서 파일 업로드
     */
    public ParticipantDocument uploadDocument(
            Long contractId, Long participantId, String documentCodeId, 
            MultipartFile file) throws IOException {
        
        // 해당 문서 레코드 조회
        ParticipantDocument document = participantDocumentRepository
                .findByContractIdAndParticipantIdAndDocumentCodeCodeId(
                        contractId, participantId, documentCodeId);
        
        if (document == null) {
            throw new RuntimeException("해당 문서 요구사항이 존재하지 않습니다.");
        }
        
        try {
            // 원본 파일명의 확장자 추출
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            // 파일명에 계약ID, 참여자ID, 문서코드ID를 포함하여 고유명 생성
            String customFileName = String.format("contract_%d_participant_%d_doc_%s_%s%s", 
                    contractId, participantId, 
                    documentCodeId.replace('/', '_').replace('-', '_'),  // 특수문자 처리
                    UUID.randomUUID().toString().substring(0, 8),  // UUID 일부만 사용
                    extension);
            
            // 파일 저장 - 모든 파일을 uploads/contracts/docs/ 경로에 직접 저장
            String savedFileId = docsFileStorageService.storeFile(
                new CustomMultipartFile(file, customFileName), null);
            
            // 문서 정보 업데이트
            document.setFileId(savedFileId);
            document.setOriginalFileName(originalFilename);
            document.setUploadedAt(LocalDateTime.now());
            
            log.info("문서 파일 업로드 완료: 계약ID={}, 참여자ID={}, 문서코드={}, 파일ID={}", 
                    contractId, participantId, documentCodeId, savedFileId);
            
            return participantDocumentRepository.save(document);
        } catch (IOException e) {
            log.error("문서 파일 업로드 실패: 계약ID={}, 참여자ID={}, 문서코드={}", 
                    contractId, participantId, documentCodeId, e);
            throw e;
        }
    }
    
    /**
     * 파일명을 변경할 수 있는 MultipartFile 래퍼 클래스
     */
    private static class CustomMultipartFile implements MultipartFile {
        private final MultipartFile delegate;
        private final String customFilename;
        
        public CustomMultipartFile(MultipartFile delegate, String customFilename) {
            this.delegate = delegate;
            this.customFilename = customFilename;
        }
        
        @Override
        public String getName() {
            return delegate.getName();
        }
        
        @Override
        public String getOriginalFilename() {
            return customFilename;
        }
        
        @Override
        public String getContentType() {
            return delegate.getContentType();
        }
        
        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }
        
        @Override
        public long getSize() {
            return delegate.getSize();
        }
        
        @Override
        public byte[] getBytes() throws IOException {
            return delegate.getBytes();
        }
        
        @Override
        public InputStream getInputStream() throws IOException {
            return delegate.getInputStream();
        }
        
        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            delegate.transferTo(dest);
        }
    }
    
    /**
     * 참여자의 모든 필수 문서가 업로드되었는지 확인
     */
    public boolean checkAllRequiredDocumentsUploaded(Long contractId, Long participantId) {
        List<ParticipantDocument> documents = participantDocumentRepository
                .findByContractIdAndParticipantId(contractId, participantId);
        
        return documents.stream()
                .filter(ParticipantDocument::isRequired)
                .allMatch(doc -> doc.getFileId() != null && !doc.getFileId().isEmpty());
    }
    
    /**
     * 계약에 필요한 문서 유형 목록 조회
     */
    public List<Code> getRequiredDocumentTypesForContract(Long contractId) {
        List<Object[]> results = participantDocumentRepository
                .findDistinctDocumentCodesByContractId(contractId);
        
        List<Code> documentCodes = new ArrayList<>();
        for (Object result : results) {
            documentCodes.add((Code) result);
        }
        
        return documentCodes;
    }
    
    /**
     * 참여자별 문서 업로드 상태 조회
     */
    public List<ParticipantDocument> getParticipantDocuments(Long contractId, Long participantId) {
        return participantDocumentRepository.findByContractIdAndParticipantId(
                contractId, participantId);
    }
    
    /**
     * 참여자 문서 파일 삭제
     */
    public ParticipantDocument deleteDocumentFile(Long documentId) throws IOException {
        // 문서 조회
        ParticipantDocument document = getDocumentById(documentId);
        
        if (document.getFileId() == null || document.getFileId().isEmpty()) {
            log.warn("삭제할 파일이 없습니다: 문서ID={}", documentId);
            return document;
        }
        
        try {
            // 저장된 파일 삭제
            docsFileStorageService.deleteFile(document.getFileId());
            
            // 문서 정보 업데이트
            document.setFileId(null);
            document.setOriginalFileName(null);
            document.setUploadedAt(null);
            
            log.info("문서 파일 삭제 완료: 문서ID={}, 계약ID={}, 참여자ID={}, 문서코드={}", 
                    documentId, 
                    document.getContract().getId(), 
                    document.getParticipant().getId(), 
                    document.getDocumentCode().getCodeId());
            
            return participantDocumentRepository.save(document);
        } catch (IOException e) {
            log.error("문서 파일 삭제 실패: 문서ID={}", documentId, e);
            throw e;
        }
    }
    
    /**
     * 계약의 모든 문서 조회
     */
    public List<ParticipantDocument> getAllDocumentsByContractId(Long contractId) {
        return participantDocumentRepository.findByContractId(contractId);
    }
    
    /**
     * 문서 ID로 특정 문서 조회
     */
    public ParticipantDocument getDocumentById(Long documentId) {
        return participantDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다: " + documentId));
    }
    
    /**
     * ContractService에서 계약 생성 시 필요한 문서 정보를 등록하는 메서드 추가
     * 이 메서드는 ContractService.createContract() 에서 호출되어야 함
     */
    public void setupContractDocuments(Long contractId, List<String> documentCodeIds) {
        try {
            createRequiredDocumentsForContract(contractId, documentCodeIds);
            log.info("계약 문서 요구사항 설정 완료: 계약ID={}, 문서코드수={}", 
                    contractId, documentCodeIds.size());
        } catch (Exception e) {
            log.error("계약 문서 요구사항 설정 실패: 계약ID={}", contractId, e);
            throw new RuntimeException("계약 문서 요구사항 설정 중 오류가 발생했습니다.", e);
        }
    }
} 