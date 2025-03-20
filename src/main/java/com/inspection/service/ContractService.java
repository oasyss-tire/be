package com.inspection.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inspection.dto.CreateContractRequest;
import com.inspection.dto.CreateParticipantRequest;
import com.inspection.dto.ParticipantDetailDTO;
import com.inspection.entity.Contract;
import com.inspection.entity.Company;
import com.inspection.entity.ContractParticipant;
import com.inspection.entity.ContractPdfField;
import com.inspection.entity.ContractTemplate;
import com.inspection.entity.ContractTemplateMapping;
import com.inspection.entity.ParticipantTemplateMapping;
import com.inspection.repository.ContractParticipantRepository;
import com.inspection.repository.ContractPdfFieldRepository;
import com.inspection.repository.ContractRepository;
import com.inspection.repository.ContractTemplateRepository;
import com.inspection.repository.CompanyRepository;
import com.inspection.util.EncryptionUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ContractService {
    private final ContractRepository contractRepository;
    private final ContractTemplateRepository templateRepository;
    private final ContractParticipantRepository participantRepository;
    private final ContractPdfFieldRepository contractPdfFieldRepository;
    private final CompanyRepository companyRepository;
    private final PdfService pdfService;
    private final EncryptionUtil encryptionUtil;
    
    public Contract createContract(CreateContractRequest request) {
        log.info("Creating new contract: {}", request.getTitle());
        
        if (request.getTemplateIds() == null || request.getTemplateIds().isEmpty()) {
            throw new RuntimeException("At least one template must be selected");
        }
        
        // 1. 회사 조회
        Company company = null;
        if (request.getCompanyId() != null) {
            company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found: " + request.getCompanyId()));
        }
            
        // 2. Contract 엔티티 생성
        Contract contract = new Contract();
        contract.setTitle(request.getTitle());
        contract.setDescription(request.getDescription());
        contract.setStartDate(request.getStartDate());
        contract.setExpiryDate(request.getExpiryDate());
        contract.setDeadlineDate(request.getDeadlineDate());
        contract.setCompany(company);
        contract.setCreatedBy(request.getCreatedBy());
        contract.setDepartment(request.getDepartment());
        
        // 3. 기본값 설정
        LocalDateTime now = LocalDateTime.now();
        contract.setCreatedAt(now);
        contract.setActive(true);
        contract.setProgressRate(0);
        
        // 계약번호 생성 및 설정
        String prefix = String.format("CT-%d-%02d%02d-", 
            now.getYear(), 
            now.getMonthValue(), 
            now.getDayOfMonth()
        );
        
        // 오늘 날짜의 계약번호들 조회
        List<String> todayNumbers = contractRepository.findContractNumbersByPrefix(prefix);
        
        // 다음 시퀀스 번호 찾기
        int sequence = 1;
        if (!todayNumbers.isEmpty()) {
            String lastNumber = todayNumbers.get(0); // 가장 최근 번호
            sequence = Integer.parseInt(lastNumber.substring(lastNumber.length() - 3)) + 1;
        }
        
        // 번호 범위 체크 (001-999)
        if (sequence > 999) {
            throw new RuntimeException("일일 최대 계약 생성 수(999)를 초과했습니다.");
        }
        
        // 계약번호 설정
        contract.setContractNumber(prefix + String.format("%03d", sequence));
        
        // 4. 템플릿 매핑 처리
        List<ContractTemplate> templates = new ArrayList<>();
        for (Long templateId : request.getTemplateIds()) {
            ContractTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));
            templates.add(template);
        }
        
        // 첫 번째 템플릿의 PDF ID를 대표 PDF로 설정
        if (!templates.isEmpty()) {
            contract.setContractPdfId(templates.get(0).getProcessedPdfId());
        }
        
        // 템플릿 매핑 생성
        for (int i = 0; i < templates.size(); i++) {
            ContractTemplate template = templates.get(i);
            contract.addTemplateMapping(template, i + 1, template.getProcessedPdfId());
        }
        
        // 5. 참여자 정보 설정
        List<ContractParticipant> participants = request.getParticipants().stream()
            .map(participantRequest -> {
                ContractParticipant participant = createParticipant(participantRequest, contract);
                return participant;
            })
            .collect(Collectors.toList());
        
        // 임시 저장 - 참여자에 계약 참조 설정을 위해
        Contract savedContract = contractRepository.save(contract);
        
        // 각 참여자별로 각 템플릿에 대한 PDF 생성
        for (ContractParticipant participant : participants) {
            contract.addParticipant(participant);
            
            // 각 템플릿에 대해 참여자별 PDF 생성
            for (ContractTemplateMapping templateMapping : contract.getTemplateMappings()) {
                try {
                    // 참여자별 PDF ID 생성
                    String participantPdfId = generateParticipantPdfId(
                        templateMapping.getProcessedPdfId(), 
                        participant.getName()
                    );
                    
                    // 템플릿 PDF 복사
                    pdfService.copyTemplateForParticipant(
                        templateMapping.getProcessedPdfId(), 
                        participantPdfId
                    );
                    
                    // 템플릿의 필드 정보 복사
                    copyPdfFields(templateMapping.getTemplate().getOriginalPdfId(), 
                                 participantPdfId, templateMapping.getTemplate());
                    
                    // 첫 번째 템플릿의 PDF는 대표 PDF로 설정
                    if (templateMapping.getSortOrder() == 1) {
                        participant.setPdfId(participantPdfId);
                    }
                    
                    // 참여자-템플릿 매핑 생성
                    participant.addTemplateMapping(templateMapping, participantPdfId);
                    
                    log.info("Created participant PDF and fields: {} for {} (Template: {})", 
                        participantPdfId, participant.getName(), templateMapping.getTemplate().getTemplateName());
                    
                } catch (Exception e) {
                    log.error("Error creating PDF for participant: {} (Template: {})", 
                        participant.getName(), templateMapping.getTemplate().getTemplateName(), e);
                    throw new RuntimeException("PDF 생성 중 오류가 발생했습니다.", e);
                }
            }
        }
        
        // 6. 최종 저장
        Contract finalContract = contractRepository.save(contract);
        log.info("Contract created successfully: {}, contractNumber: {}", 
            finalContract.getId(), finalContract.getContractNumber());
        
        return finalContract;
    }

    @Transactional(readOnly = true)
    public Contract getContract(Long contractId) {
        try {
            return contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found: " + contractId));
        } catch (EntityNotFoundException e) {
            log.warn("EntityNotFoundException occurred while fetching contract {}: {}", contractId, e.getMessage());
            throw new RuntimeException("계약을 조회할 수 없습니다. company_id가 설정되지 않았습니다: " + contractId);
        }
    }
    
    @Transactional(readOnly = true)
    public List<Contract> getActiveContracts() {
        try {
            return contractRepository.findByActiveTrueOrderByCreatedAtDesc();
        } catch (EntityNotFoundException e) {
            log.warn("EntityNotFoundException occurred while fetching active contracts: {}", e.getMessage());
            // 회사 ID가 없는 계약은 제외하고 조회
            List<Contract> contracts = contractRepository.findAll().stream()
                .filter(Contract::isActive)
                .filter(contract -> contract.getCompany() != null)
                .sorted((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt()))
                .collect(Collectors.toList());
            return contracts;
        }
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
        participant.setEmail(encryptionUtil.encrypt(request.getEmail()));
        participant.setPhoneNumber(encryptionUtil.encrypt(request.getPhoneNumber()));
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
            
        // 복호화된 값 가져오기
        String decryptedEmail = encryptionUtil.decrypt(participant.getEmail());
        String decryptedPhone = encryptionUtil.decrypt(participant.getPhoneNumber());
        
        // 새로운 생성자 사용
        return new ParticipantDetailDTO(participant, decryptedEmail, decryptedPhone);
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

    public boolean verifyParticipantPhone(Long contractId, Long participantId, String phoneLastDigits) {
        ContractParticipant participant = participantRepository.findByContractIdAndId(contractId, participantId)
            .orElseThrow(() -> new RuntimeException("Participant not found"));
        
        // 암호화된 전화번호를 복호화한 후 마지막 4자리와 비교
        if (participant != null && participant.getPhoneNumber() != null) {
            String decryptedPhone = encryptionUtil.decrypt(participant.getPhoneNumber());
            String participantPhoneLastDigits = decryptedPhone.substring(decryptedPhone.length() - 4);
            return participantPhoneLastDigits.equals(phoneLastDigits);
        }
        
        return false;
    }
} 