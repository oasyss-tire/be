package com.inspection.service;

import com.inspection.dto.CreateContractRequest;
import com.inspection.dto.CreateParticipantRequest;
import com.inspection.dto.ParticipantDetailDTO;
import com.inspection.entity.Contract;
import com.inspection.entity.ContractParticipant;
import com.inspection.entity.ContractTemplate;
import com.inspection.entity.ContractPdfField;
import com.inspection.repository.ContractRepository;
import com.inspection.repository.ContractTemplateRepository;
import com.inspection.repository.ContractParticipantRepository;
import com.inspection.repository.ContractPdfFieldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.format.DateTimeFormatter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ContractService {
    private final ContractRepository contractRepository;
    private final ContractTemplateRepository templateRepository;
    private final ContractParticipantRepository participantRepository;
    private final ContractPdfFieldRepository contractPdfFieldRepository;
    private final PdfService pdfService;
    
    public Contract createContract(CreateContractRequest request) {
        log.info("Creating new contract: {}", request.getTitle());
        
        // 1. 템플릿 조회
        ContractTemplate template = templateRepository.findById(request.getTemplateId())
            .orElseThrow(() -> new RuntimeException("Template not found: " + request.getTemplateId()));
            
        // 2. Contract 엔티티 생성
        Contract contract = new Contract();
        contract.setTitle(request.getTitle());
        contract.setDescription(request.getDescription());
        contract.setStartDate(request.getStartDate());
        contract.setExpiryDate(request.getExpiryDate());
        contract.setDeadlineDate(request.getDeadlineDate());
        contract.setTemplate(template);
        contract.setCreatedBy(request.getCreatedBy());
        contract.setDepartment(request.getDepartment());
        contract.setContractNumber(request.getContractNumber());
        contract.setContractPdfId(template.getProcessedPdfId());
        
        // 3. 기본값 설정
        contract.setCreatedAt(LocalDateTime.now());
        contract.setActive(true);
        contract.setProgressRate(0);
        
        // 4. 참여자 정보 설정
        List<ContractParticipant> participants = request.getParticipants().stream()
            .map(participantRequest -> {
                ContractParticipant participant = createParticipant(participantRequest, contract);
                
                try {
                    // 4.1 참여자별 PDF ID 생성
                    String participantPdfId = generateParticipantPdfId(
                        template.getProcessedPdfId(), 
                        participantRequest.getName()
                    );
                    
                    // 4.2 템플릿 PDF 복사
                    pdfService.copyTemplateForParticipant(
                        template.getProcessedPdfId(), 
                        participantPdfId
                    );
                    
                    // 4.3 템플릿의 필드 정보 복사
                    copyPdfFields(template.getOriginalPdfId(), participantPdfId, template);
                    
                    // 4.4 참여자에 PDF ID 설정
                    participant.setPdfId(participantPdfId);
                    log.info("Created participant PDF and fields: {} for {}", 
                        participantPdfId, participant.getName());
                    
                } catch (Exception e) {
                    log.error("Error creating PDF for participant: {}", participant.getName(), e);
                    throw new RuntimeException("PDF 생성 중 오류가 발생했습니다.", e);
                }
                
                return participant;
            })
            .collect(Collectors.toList());
        
        participants.forEach(contract::addParticipant);
        
        // 5. 저장
        Contract savedContract = contractRepository.save(contract);
        log.info("Contract created successfully: {}", savedContract.getId());
        
        return savedContract;
    }
    
    @Transactional(readOnly = true)
    public Contract getContract(Long contractId) {
        return contractRepository.findById(contractId)
            .orElseThrow(() -> new RuntimeException("Contract not found: " + contractId));
    }
    
    @Transactional(readOnly = true)
    public List<Contract> getActiveContracts() {
        return contractRepository.findByActiveTrueOrderByCreatedAtDesc();
    }
    
    public void deactivateContract(Long contractId, String reason) {
        Contract contract = getContract(contractId);
        contract.setActive(false);
        contract.setCancelReason(reason);
        contract.setLastModifiedAt(LocalDateTime.now());
        contractRepository.save(contract);
        log.info("Contract deactivated: {}", contractId);
    }
    
    private ContractParticipant createParticipant(CreateParticipantRequest request, Contract contract) {
        ContractParticipant participant = new ContractParticipant();
        participant.setName(request.getName());
        participant.setEmail(request.getEmail());
        participant.setPhoneNumber(request.getPhoneNumber());
        participant.setNotifyType(request.getNotifyType());
        participant.setSigned(false);
        return participant;
    }
    
    private String generateParticipantPdfId(String templatePdfId, String participantName) {
        String baseName = templatePdfId.substring(0, templatePdfId.lastIndexOf("."));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return String.format("%s_%s_%s.pdf", baseName, participantName, timestamp);
    }
    
    @Transactional(readOnly = true)
    public ParticipantDetailDTO getParticipantDetail(Long contractId, Long participantId) {
        ContractParticipant participant = participantRepository.findByContractIdAndId(contractId, participantId)
            .orElseThrow(() -> new RuntimeException("Participant not found"));
            
        return new ParticipantDetailDTO(participant);
    }
    
    public void completeParticipantSign(Long contractId, Long participantId) {
        ContractParticipant participant = participantRepository.findByContractIdAndId(contractId, participantId)
            .orElseThrow(() -> new RuntimeException("Participant not found"));
        
        participant.setSigned(true);
        participant.setSignedAt(LocalDateTime.now());
        
        // 계약 진행률 업데이트
        Contract contract = participant.getContract();
        contract.calculateProgressRate();
        
        participantRepository.save(participant);
        log.info("Participant sign completed: {}", participantId);
    }
    
    /**
     * PDF 필드 정보 복사
     */
    private void copyPdfFields(String sourcePdfId, String targetPdfId, ContractTemplate template) {
        // 템플릿의 originalPdfId로 필드 정보 조회
        List<ContractPdfField> templateFields = contractPdfFieldRepository.findByPdfId(template.getOriginalPdfId());
        log.info("Found {} template fields for originalPdfId: {}", templateFields.size(), template.getOriginalPdfId());
        
        if (templateFields.isEmpty()) {
            log.warn("No template fields found for originalPdfId: {}", template.getOriginalPdfId());
            return;
        }

        List<ContractPdfField> participantFields = templateFields.stream()
            .map(templateField -> {
                ContractPdfField participantField = new ContractPdfField();
                participantField.setPdfId(targetPdfId);
                participantField.setFieldId(templateField.getFieldId());
                participantField.setType(templateField.getType());
                participantField.setRelativeX(templateField.getRelativeX());
                participantField.setRelativeY(templateField.getRelativeY());
                participantField.setRelativeWidth(templateField.getRelativeWidth());
                participantField.setRelativeHeight(templateField.getRelativeHeight());
                participantField.setPage(templateField.getPage());
                participantField.setFieldName(templateField.getFieldName());
                participantField.setTemplate(template);
                return participantField;
            })
            .collect(Collectors.toList());
            
        try {
            List<ContractPdfField> savedFields = contractPdfFieldRepository.saveAll(participantFields);
            log.info("Successfully copied {} fields from template to participant pdfId: {}", 
                savedFields.size(), targetPdfId);
        } catch (Exception e) {
            log.error("Error copying fields to participant PDF: ", e);
            throw new RuntimeException("필드 정보 복사 중 오류가 발생했습니다.", e);
        }
    }
} 