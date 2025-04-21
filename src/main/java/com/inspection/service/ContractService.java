package com.inspection.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inspection.dto.CreateContractRequest;
import com.inspection.dto.CreateParticipantRequest;
import com.inspection.dto.ParticipantDetailDTO;
import com.inspection.entity.Code;
import com.inspection.entity.Company;
import com.inspection.entity.Contract;
import com.inspection.entity.ContractParticipant;
import com.inspection.entity.ContractPdfField;
import com.inspection.entity.ContractTemplate;
import com.inspection.entity.ContractTemplateMapping;
import com.inspection.entity.ParticipantPdfField;
import com.inspection.entity.ParticipantResignHistory;
import com.inspection.entity.ParticipantTemplateMapping;
import com.inspection.enums.NotificationType;
import com.inspection.repository.CodeRepository;
import com.inspection.repository.CompanyRepository;
import com.inspection.repository.ContractParticipantRepository;
import com.inspection.repository.ContractPdfFieldRepository;
import com.inspection.repository.ContractRepository;
import com.inspection.repository.ContractTemplateRepository;
import com.inspection.repository.ParticipantPdfFieldRepository;
import com.inspection.repository.ParticipantResignHistoryRepository;
import com.inspection.repository.ParticipantTemplateMappingRepository;
import com.inspection.repository.UserRepository;
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
    private final CodeRepository codeRepository;
    private final EmailService emailService;
    private final SMSService smsService;
    private final ParticipantResignHistoryRepository resignHistoryRepository;
    private final ParticipantTokenService participantTokenService;
    private final ParticipantTemplateMappingRepository participantTemplateMappingRepository;
    private final ParticipantPdfFieldRepository participantPdfFieldRepository;
    private final ParticipantDocumentService participantDocumentService;
    private final UserRepository userRepository;
    
    @Value("${frontend.base-url}")
    private String frontendBaseUrl;
    
    // 계약 상태 코드 상수 정의 (기존 코드 체계에 맞게 수정)
    private static final String CONTRACT_STATUS_TEMP = "001002_0003";      // 임시저장
    private static final String CONTRACT_STATUS_SIGNING = "001002_0004";   // 서명 진행중
    private static final String CONTRACT_STATUS_WAITING = "001002_0001";   // 승인대기
    private static final String CONTRACT_STATUS_COMPLETED = "001002_0002"; // 계약완료
    
    // 참여자 상태 코드 상수 정의
    private static final String PARTICIPANT_STATUS_WAITING = "007001_0003";  // 서명 대기
    private static final String PARTICIPANT_STATUS_SIGNING = "007001_0004";  // 서명 중
    private static final String PARTICIPANT_STATUS_APPROVAL_WAITING = "007001_0001";  // 승인 대기
    private static final String PARTICIPANT_STATUS_APPROVED = "007001_0002";  // 승인 완료
    private static final String PARTICIPANT_STATUS_REJECTED = "007001_0005";  // 승인 거부
    

    // 계약 생성 시 템플릿 매핑 처리
    // 및 참여자 별 PDF 생성
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
        contract.setCompany(company);
        contract.setCreatedBy(request.getCreatedBy());
        contract.setDepartment(request.getDepartment());
        
        // 하자보증증권 정보 설정
        contract.setInsuranceStartDate(request.getInsuranceStartDate());
        contract.setInsuranceEndDate(request.getInsuranceEndDate());
        
        // 3. 기본값 설정
        LocalDateTime now = LocalDateTime.now();
        contract.setCreatedAt(now);
        contract.setActive(true);
        contract.setProgressRate(0);
        
        // 계약 초기 상태 설정 - 임시저장(TEMP) 상태로 설정
        Code tempStatus = codeRepository.findById(CONTRACT_STATUS_TEMP)
            .orElseThrow(() -> new RuntimeException("Contract status code not found: " + CONTRACT_STATUS_TEMP));
        contract.setStatusCode(tempStatus);
        
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
                ContractParticipant participant = createParticipant(contract, participantRequest);
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
                        participant.getName(), contract
                    );
                    
                    // 템플릿 PDF 복사
                    pdfService.copyTemplateForParticipant(
                        templateMapping.getProcessedPdfId(), 
                        participantPdfId
                    );
                    
                    // 템플릿의 필드 정보 복사
                    copyPdfFields(templateMapping.getTemplate().getOriginalPdfId(), 
                                 participantPdfId, templateMapping.getTemplate(), participant);
                    
                    // 첫 번째 템플릿의 PDF는 대표 PDF로 설정
                    if (templateMapping.getSortOrder() == 1) {
                        participant.setPdfId(participantPdfId);
                    }
                    
                    // 참여자-템플릿 매핑 생성 (이미 ID가 있는 참여자 객체 사용)
                    if (participant.getId() == null) {
                        log.error("참여자 ID가 NULL입니다. 매핑 생성 불가: participantName={}", participant.getName());
                        throw new RuntimeException("참여자 ID가 NULL입니다. 참여자가 먼저 저장되어야 합니다.");
                    }
                    
                    log.info("참여자-템플릿 매핑 생성: participantId={}, participantName={}, templateId={}", 
                        participant.getId(), participant.getName(), templateMapping.getTemplate().getId());
                    
                    // 매핑 생성 및 저장
                    ParticipantTemplateMapping ptMapping = new ParticipantTemplateMapping();
                    ptMapping.setContractTemplateMapping(templateMapping);
                    ptMapping.setParticipant(participant);
                    ptMapping.setPdfId(participantPdfId);
                    ptMapping.setSignedPdfId(null);
                    ptMapping.setCreatedAt(LocalDateTime.now());
                    ptMapping.setSigned(false);
                    
                    // 매핑 저장 (참여자 ID가 설정된 상태에서 직접 저장)
                    ParticipantTemplateMapping savedMapping = participantTemplateMappingRepository.save(ptMapping);
                    log.info("참여자-템플릿 매핑 저장 완료: mappingId={}, participantId={}, templateId={}", 
                        savedMapping.getId(), participant.getId(), templateMapping.getTemplate().getId());
                    
                    // 참여자의 템플릿 매핑 리스트에 추가
                    if (participant.getTemplateMappings() == null) {
                        participant.setTemplateMappings(new ArrayList<>());
                    }
                    participant.getTemplateMappings().add(savedMapping);
                    
                    // 변경사항 저장
                    participant = participantRepository.save(participant);
                    
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
        
        // 7. 필요한 문서 설정 (요구사항에 명시된 문서 코드가 있는 경우에만)
        if (request.getDocumentCodeIds() != null && !request.getDocumentCodeIds().isEmpty()) {
            try {
                participantDocumentService.setupContractDocuments(finalContract.getId(), request.getDocumentCodeIds());
                log.info("Contract document requirements set: contractId={}, documentCodeCount={}", 
                    finalContract.getId(), request.getDocumentCodeIds().size());
            } catch (Exception e) {
                log.error("Error setting up contract documents", e);
                // 문서 요구사항 설정 실패는 계약 생성 자체를 실패시키지 않음
            }
        }
        
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
    
    private ContractParticipant createParticipant(Contract contract, CreateParticipantRequest participantRequest) {
        ContractParticipant participant = new ContractParticipant();
        participant.setName(participantRequest.getName());
        
        // 이메일, 전화번호 암호화
        participant.setEmail(encryptionUtil.encrypt(participantRequest.getEmail()));
        participant.setPhoneNumber(encryptionUtil.encrypt(participantRequest.getPhoneNumber()));
        
        participant.setNotifyType(participantRequest.getNotifyType());
        participant.setContract(contract);
        participant.setSigned(false);
        participant.setSignedAt(null);
        
        // 참여자 초기 상태 설정 - 서명 대기
        Code initialStatus = codeRepository.findById(PARTICIPANT_STATUS_WAITING)
            .orElseThrow(() -> new EntityNotFoundException("서명 대기 상태 코드를 찾을 수 없습니다: " + PARTICIPANT_STATUS_WAITING));
        participant.setStatusCode(initialStatus);
        
        // 참여자와 User 연결 (userId가 제공된 경우)
        if (participantRequest.getUserId() != null) {
            userRepository.findById(participantRequest.getUserId())
                .ifPresent(user -> participant.setUser(user));
        }
        
        return participant;
    }
    
    private String generateParticipantPdfId(String templatePdfId, String participantName, Contract contract) {
        // 타임스탬프 생성 (yyyyMMdd_HHmmss 형식)
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        
        // 계약 단계 (계약 생성 시에는 항상 SIGNING)
        String contractStage = "SIGNING";
        
        // 계약번호 가져오기
        String contractNumber = contract.getContractNumber();
        
        // 원본 파일명 추출 (템플릿 ID에서 추출)
        String originalFileName = extractOriginalFileName(templatePdfId);
        
        // 참여자 이름에서 특수문자만 제거 (한글과 영문자 유지)
        String cleanName = participantName.replaceAll("[^\\p{L}\\p{N}]", "_");
        
        // 확장자 추출
        String extension = ".pdf";
        int dotIndex = templatePdfId.lastIndexOf(".");
        if (dotIndex > 0) {
            extension = templatePdfId.substring(dotIndex);
        }
        
        // 파일명 조합: 타임스탬프_계약단계_계약번호_파일명_참여자이름.확장자
        return timestamp + "_" + contractStage + "_" + contractNumber + "_" + originalFileName + "_" + cleanName + extension;
    }
    
    /**
     * 템플릿 ID에서 원본 파일명 추출
     */
    private String extractOriginalFileName(String templatePdfId) {
        // 새 형식: "타임스탬프_template_파일명.pdf"
        if (templatePdfId.contains("_template_")) {
            int startIndex = templatePdfId.indexOf("_template_") + "_template_".length();
            int endIndex = templatePdfId.lastIndexOf(".");
            if (startIndex > 0 && endIndex > startIndex) {
                return templatePdfId.substring(startIndex, endIndex);
            }
        }
        
        // 이전 형식: "타임스탬프_original_파일명_template.pdf"
        if (templatePdfId.contains("_original_") && templatePdfId.contains("_template.pdf")) {
            int startIndex = templatePdfId.indexOf("_original_") + "_original_".length();
            int endIndex = templatePdfId.indexOf("_template.pdf");
            if (startIndex > 0 && endIndex > startIndex) {
                return templatePdfId.substring(startIndex, endIndex);
            }
        }
        
        // 기존 템플릿에서 추출할 수 없는 경우 기본값 반환
        return "contract";
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
    
    @Transactional
    public void completeParticipantSign(Long contractId, Long participantId) {
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new EntityNotFoundException("계약을 찾을 수 없습니다: " + contractId));
            
        ContractParticipant participant = participantRepository.findById(participantId)
            .orElseThrow(() -> new EntityNotFoundException("참여자를 찾을 수 없습니다: " + participantId));
        
        // 참여자가 해당 계약에 속하는지 확인
        if (!participant.getContract().getId().equals(contractId)) {
            throw new IllegalArgumentException("참여자가 해당 계약에 속하지 않습니다.");
        }
        
        // 서명 완료 처리
        participant.setSigned(true);
        participant.setSignedAt(LocalDateTime.now());
        
        // 상태 코드를 '승인 대기'로 변경
        Code approvalWaitingStatus = codeRepository.findById(PARTICIPANT_STATUS_APPROVAL_WAITING)
            .orElseThrow(() -> new EntityNotFoundException("승인 대기 상태 코드를 찾을 수 없습니다: " + PARTICIPANT_STATUS_APPROVAL_WAITING));
        participant.setStatusCode(approvalWaitingStatus);
        
        // 재서명 상태 초기화: 모든 템플릿 매핑의 needsResign 필드를 false로 설정
        if (participant.getTemplateMappings() != null) {
            participant.getTemplateMappings().forEach(mapping -> {
                if (mapping.isNeedsResign()) {
                    log.info("재서명 상태 초기화 - 참여자: {}, PDF ID: {}", 
                        participant.getName(), mapping.getPdfId());
                    mapping.setNeedsResign(false);
                }
            });
        }
        
        // 계약 진행률 계산 및 업데이트
        contract.calculateProgressRate();
        
        participantRepository.save(participant);
        contractRepository.save(contract);  // 변경된 진행률 저장
        
        // 모든 참여자가 서명을 완료했는지 확인하고, 완료되었다면 계약 상태를 '승인대기'로 변경
        checkAllParticipantsSigned(contract);
    }
    
    /**
     * PDF 필드 정보 복사
     */
    private void copyPdfFields(String sourcePdfId, String targetPdfId, ContractTemplate template, ContractParticipant participant) {
        // 템플릿의 originalPdfId로 필드 정보 조회
        List<ContractPdfField> templateFields = contractPdfFieldRepository.findByPdfId(template.getOriginalPdfId());
        log.info("Found {} template fields for originalPdfId: {}", templateFields.size(), template.getOriginalPdfId());
        
        if (templateFields.isEmpty()) {
            log.warn("No template fields found for originalPdfId: {}", template.getOriginalPdfId());
            return;
        }

        List<ParticipantPdfField> participantFields = templateFields.stream()
            .map(templateField -> {
                ParticipantPdfField participantField = new ParticipantPdfField();
                participantField.setOriginalField(templateField);
                participantField.setParticipant(participant);
                participantField.setPdfId(targetPdfId);
                participantField.setFieldId(templateField.getFieldId());
                participantField.setFieldName(templateField.getFieldName());
                participantField.setType(templateField.getType());
                participantField.setRelativeX(templateField.getRelativeX());
                participantField.setRelativeY(templateField.getRelativeY());
                participantField.setRelativeWidth(templateField.getRelativeWidth());
                participantField.setRelativeHeight(templateField.getRelativeHeight());
                participantField.setPage(templateField.getPage());
                participantField.setConfirmText(templateField.getConfirmText());
                participantField.setDescription(templateField.getDescription());
                participantField.setFormat(templateField.getFormat());
                participantField.setTemplate(template);
                return participantField;
            })
            .collect(Collectors.toList());
            
        try {
            List<ParticipantPdfField> savedFields = participantPdfFieldRepository.saveAll(participantFields);
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

    /**
     * 계약 상태 변경
     */
    @Transactional
    public Contract updateContractStatus(Long contractId, String statusCodeId, String updatedBy) {
        Contract contract = getContract(contractId);
        
        Code statusCode = codeRepository.findById(statusCodeId)
            .orElseThrow(() -> new RuntimeException("Status code not found: " + statusCodeId));
        
        contract.setStatusCode(statusCode);
        contract.setLastModifiedAt(LocalDateTime.now());
        
        // 상태별 추가 동작
        if (CONTRACT_STATUS_COMPLETED.equals(statusCodeId)) {
            contract.setApprovedBy(updatedBy);
            contract.setApprovedAt(LocalDateTime.now());
            contract.setCompletedAt(LocalDateTime.now());
        }
        
        return contractRepository.save(contract);
    }
    
    /**
     * 계약 승인
     */
    @Transactional
    public Contract approveContract(Long contractId, String approver) {
        Contract contract = getContract(contractId);
        
        // 승인 가능한 상태인지 확인 (승인대기 상태)
        if (contract.getStatusCode() == null || 
            !CONTRACT_STATUS_WAITING.equals(contract.getStatusCode().getCodeId())) {
            throw new IllegalStateException("승인대기 상태가 아닌 계약은 승인할 수 없습니다.");
        }
        
        // 계약완료 상태로 변경
        Code completedStatus = codeRepository.findById(CONTRACT_STATUS_COMPLETED)
            .orElseThrow(() -> new RuntimeException("Status code not found: " + CONTRACT_STATUS_COMPLETED));
        
        contract.setStatusCode(completedStatus);
        contract.setApprovedBy(approver);
        contract.setApprovedAt(LocalDateTime.now());
        contract.setCompletedAt(LocalDateTime.now());
        contract.setLastModifiedAt(LocalDateTime.now());
        
        return contractRepository.save(contract);
    }
    
    /**
     * 계약 반려 (수정 요청)
     */
    @Transactional
    public Contract rejectContract(Long contractId, String rejectionReason, String rejector) {
        Contract contract = getContract(contractId);
        
        // 반려 가능한 상태인지 확인
        if (contract.getStatusCode() == null || 
            !CONTRACT_STATUS_WAITING.equals(contract.getStatusCode().getCodeId())) {
            throw new IllegalStateException("승인대기 상태가 아닌 계약은 반려할 수 없습니다.");
        }
        
        // 서명 진행중 상태로 변경 (재서명을 위해)
        Code signingStatus = codeRepository.findById(CONTRACT_STATUS_SIGNING)
            .orElseThrow(() -> new RuntimeException("Status code not found: " + CONTRACT_STATUS_SIGNING));
        
        contract.setStatusCode(signingStatus);
        contract.setRejectionReason(rejectionReason);
        contract.setLastModifiedAt(LocalDateTime.now());
        
        // 모든 참여자의 서명 상태 초기화 (재서명 필요)
        // 현재 계약의 참여자들만 초기화
        for (ContractParticipant participant : contract.getParticipants()) {
            participant.setSigned(false);
            participant.setSignedAt(null);
            
            // 승인 관련 상태 초기화
            participant.setApproved(false);
            participant.setApprovedAt(null);
            participant.setApprovalComment(null);
            
            // 참여자 상태 코드 변경
            try {
                Code waitingStatus = codeRepository.findById(PARTICIPANT_STATUS_WAITING)
                    .orElseThrow(() -> new EntityNotFoundException("서명 대기 상태 코드를 찾을 수 없습니다: " + PARTICIPANT_STATUS_WAITING));
                participant.setStatusCode(waitingStatus);
            } catch (Exception e) {
                log.error("참여자 상태 초기화 중 오류 발생", e);
            }
            
            // 템플릿 매핑 상태 초기화
            if (participant.getTemplateMappings() != null) {
                for (var templateMapping : participant.getTemplateMappings()) {
                    templateMapping.setSigned(false);
                    templateMapping.setSignedAt(null);
                    templateMapping.setSignedPdfId(null);
                }
            }
            
            participantRepository.save(participant);
            log.info("참여자 상태 초기화 (반려): 계약ID={}, 참여자ID={}", contractId, participant.getId());
        }
        
        // 서명 진행률 재계산
        contract.calculateProgressRate();
        
        return contractRepository.save(contract);
    }

    /**
     * 계약을 다시 서명할 수 있도록 재전송합니다.
     */
    @Transactional
    public Contract resendContractForReSign(Long contractId, String reason, String rejector) {
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new EntityNotFoundException("계약을 찾을 수 없습니다: " + contractId));
        
        // 상태 검증 - 승인 대기 상태인 경우에만 거부 가능
        if (!contract.getStatusCode().getCodeId().equals(CONTRACT_STATUS_WAITING)) {
            throw new IllegalStateException("승인 대기 상태의 계약만 재전송할 수 있습니다.");
        }
        
        // 계약을 반려 상태로 변경 (rejectContract 내에서 이미 서명 상태 초기화 처리됨)
        contract = rejectContract(contractId, reason, rejector);
        
        // TODO: 여기서 모든 참여자에게 이메일이나 SMS로 재서명 알림을 보내야 함
        log.info("재서명 요청 알림 필요: 계약 ID={}", contractId);
        
        return contract;
    }
    
    /**
     * 관리자가 참여자의 서명을 승인합니다.
     */
    @Transactional
    public ContractParticipant approveByParticipant(Long contractId, Long participantId, String comment) {
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new EntityNotFoundException("계약을 찾을 수 없습니다: " + contractId));
            
        ContractParticipant participant = participantRepository.findById(participantId)
            .orElseThrow(() -> new EntityNotFoundException("참여자를 찾을 수 없습니다: " + participantId));
        
        // 참여자가 해당 계약에 속하는지 확인
        if (!participant.getContract().getId().equals(contractId)) {
            throw new IllegalArgumentException("참여자가 해당 계약에 속하지 않습니다.");
        }
        
        // 서명이 완료된 상태에서만 승인 가능
        if (!participant.isSigned()) {
            throw new IllegalStateException("서명이 완료되지 않은 상태에서는 승인할 수 없습니다.");
        }
        
        // 상태 확인 - 승인 대기 상태인지 확인
        if (participant.getStatusCode() == null || 
            !PARTICIPANT_STATUS_APPROVAL_WAITING.equals(participant.getStatusCode().getCodeId())) {
            throw new IllegalStateException("승인 대기 상태의 참여자만 승인할 수 있습니다.");
        }
        
        // 승인 처리
        participant.setApproved(true);
        participant.setApprovedAt(LocalDateTime.now());
        participant.setApprovalComment(comment);
        
        // 상태를 '승인 완료'로 변경
        Code approvedStatus = codeRepository.findById(PARTICIPANT_STATUS_APPROVED)
            .orElseThrow(() -> new EntityNotFoundException("승인 완료 상태 코드를 찾을 수 없습니다: " + PARTICIPANT_STATUS_APPROVED));
        participant.setStatusCode(approvedStatus);
        
        // 승인된 참여자 저장
        participantRepository.save(participant);
        
        // 모든 참여자가 승인했는지 확인하고 계약 상태 업데이트
        checkAllParticipantsApproved(contract);
        
        return participant;
    }
    
    /**
     * 관리자가 참여자의 서명을 거부합니다.
     */
    @Transactional
    public ContractParticipant rejectByParticipant(Long contractId, Long participantId, String reason) {
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new EntityNotFoundException("계약을 찾을 수 없습니다: " + contractId));
            
        ContractParticipant participant = participantRepository.findById(participantId)
            .orElseThrow(() -> new EntityNotFoundException("참여자를 찾을 수 없습니다: " + participantId));
        
        // 참여자가 해당 계약에 속하는지 확인
        if (!participant.getContract().getId().equals(contractId)) {
            throw new IllegalArgumentException("참여자가 해당 계약에 속하지 않습니다.");
        }
        
        // 서명이 완료된 상태에서만 거부 가능
        if (!participant.isSigned()) {
            throw new IllegalStateException("서명이 완료되지 않은 상태에서는 거부할 수 없습니다.");
        }
        
        // 상태 확인 - 승인 대기 상태인지 확인
        if (participant.getStatusCode() == null || 
            !PARTICIPANT_STATUS_APPROVAL_WAITING.equals(participant.getStatusCode().getCodeId())) {
            throw new IllegalStateException("승인 대기 상태의 참여자만 거부할 수 있습니다.");
        }
        
        log.info("참여자 서명 거부 처리 시작: 계약ID={}, 참여자ID={}", contractId, participantId);
        
        // 거부 처리 - 해당 참여자만 초기화
        participant.setSigned(false);
        participant.setSignedAt(null);
        participant.setApproved(false);
        participant.setApprovedAt(null);
        participant.setApprovalComment(null);
        participant.setRejectionReason(reason);
        
        // 상태를 '서명 대기'로 변경
        try {
            Code waitingStatus = codeRepository.findById(PARTICIPANT_STATUS_WAITING)
                .orElseThrow(() -> new EntityNotFoundException("서명 대기 상태 코드를 찾을 수 없습니다: " + PARTICIPANT_STATUS_WAITING));
            participant.setStatusCode(waitingStatus);
        } catch (Exception e) {
            log.error("참여자 상태 초기화 중 오류 발생", e);
        }
        
        // 해당 참여자의 템플릿 매핑 상태도 초기화
        if (participant.getTemplateMappings() != null) {
            for (var templateMapping : participant.getTemplateMappings()) {
                templateMapping.setSigned(false);
                templateMapping.setSignedAt(null);
                templateMapping.setSignedPdfId(null);
            }
        }
        
        // 계약 상태를 서명 진행중으로 변경 (재서명 필요)
        updateContractStatus(contractId, CONTRACT_STATUS_SIGNING, "System");
        
        // 거부된 참여자 저장
        participantRepository.save(participant);
        log.info("참여자 서명 거부 완료: 계약ID={}, 참여자ID={}", contractId, participantId);
        
        // 계약 진행률 다시 계산
        contract.calculateProgressRate();
        contractRepository.save(contract);
        
        return participant;
    }
    
    /**
     * 모든 참여자가 승인했는지 확인하고 계약 상태 업데이트
     */
    private void checkAllParticipantsApproved(Contract contract) {
        boolean allApproved = contract.getParticipants().stream()
            .allMatch(p -> p.isApproved() || 
                (p.getStatusCode() != null && PARTICIPANT_STATUS_APPROVED.equals(p.getStatusCode().getCodeId())));
        
        if (allApproved) {
            // 모든 참여자가 승인한 경우 계약 완료 상태로 변경
            updateContractStatus(contract.getId(), CONTRACT_STATUS_COMPLETED, "System");
            
            // 계약 완료 시간 기록
            contract.setApprovedAt(LocalDateTime.now());
            contract.setApprovedBy("시스템 자동 승인 (모든 참여자 승인 완료)");
            
            contractRepository.save(contract);
            log.info("모든 참여자 승인 완료: 계약ID={}, 상태='계약완료'", contract.getId());
        }
    }
    
    /**
     * 특정 계약에 속한 참여자들의 서명 및 승인 상태를 초기화 (지정된 참여자 제외)
     * 이 메서드는 계약 전체를 거부했을 때 사용됩니다.
     * 개별 참여자의 서명만 거부할 경우에는 해당 참여자만 초기화합니다.
     */
    private void resetAllParticipantsStatusExcept(Contract contract, Long excludedParticipantId) {
        // 현재 계약의 참여자들만 처리 (다른 계약의 참여자는 건드리지 않음)
        for (ContractParticipant participant : contract.getParticipants()) {
            // 거부한 참여자는 초기화하지 않음
            if (participant.getId().equals(excludedParticipantId)) {
                continue;
            }
            
            participant.setSigned(false);
            participant.setSignedAt(null);
            participant.setApproved(false);
            participant.setApprovedAt(null);
            participant.setApprovalComment(null);
            participant.setRejectionReason(null);
            
            // 상태를 '서명 대기'로 변경
            try {
                Code waitingStatus = codeRepository.findById(PARTICIPANT_STATUS_WAITING)
                    .orElseThrow(() -> new EntityNotFoundException("서명 대기 상태 코드를 찾을 수 없습니다: " + PARTICIPANT_STATUS_WAITING));
                participant.setStatusCode(waitingStatus);
            } catch (Exception e) {
                log.error("참여자 상태 초기화 중 오류 발생", e);
            }
            
            // 해당 참여자의 각 템플릿 매핑에 대한 서명 상태도 초기화
            if (participant.getTemplateMappings() != null) {
                for (var templateMapping : participant.getTemplateMappings()) {
                    templateMapping.setSigned(false);
                    templateMapping.setSignedAt(null);
                    templateMapping.setSignedPdfId(null);
                }
            }
            
            participantRepository.save(participant);
            log.info("참여자 상태 초기화: 계약ID={}, 참여자ID={}", contract.getId(), participant.getId());
        }
        
        // 계약 진행률 다시 계산
        contract.calculateProgressRate();
        contractRepository.save(contract);
    }

    /**
     * 모든 참여자가 서명을 완료했는지 확인하고 계약 상태 업데이트
     */
    private void checkAllParticipantsSigned(Contract contract) {
        // 모든 참여자가 서명을 완료했는지 확인
        boolean allSigned = contract.getParticipants().stream()
            .allMatch(ContractParticipant::isSigned);
        
        // 계약 진행률 계산 및 업데이트
        contract.calculateProgressRate();
        
        // 모든 참여자가 서명을 완료했으면 계약 상태를 '승인대기'로 변경
        if (allSigned) {
            try {
                updateContractStatus(contract.getId(), CONTRACT_STATUS_WAITING, "System");
                log.info("모든 참여자 서명 완료: 계약ID={}, 상태='승인대기'", contract.getId());
            } catch (Exception e) {
                log.error("계약 상태 업데이트 중 오류 발생", e);
            }
        } else {
            // 일부만 서명한 경우 '서명 진행중' 상태로 유지/변경
            try {
                if (contract.getStatusCode() == null || 
                    CONTRACT_STATUS_TEMP.equals(contract.getStatusCode().getCodeId())) {
                    updateContractStatus(contract.getId(), CONTRACT_STATUS_SIGNING, "System");
                    log.info("일부 참여자 서명 완료: 계약ID={}, 상태='서명 진행중'", contract.getId());
                }
            } catch (Exception e) {
                log.error("계약 상태 업데이트 중 오류 발생", e);
            }
        }
        
        // 변경된 진행률 저장
        contractRepository.save(contract);
    }

    /**
     * 참여자의 서명 과정을 시작합니다.
     */
    @Transactional
    public ContractParticipant startParticipantSigning(Long contractId, Long participantId) {
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new EntityNotFoundException("계약을 찾을 수 없습니다: " + contractId));
            
        ContractParticipant participant = participantRepository.findById(participantId)
            .orElseThrow(() -> new EntityNotFoundException("참여자를 찾을 수 없습니다: " + participantId));
        
        // 참여자가 해당 계약에 속하는지 확인
        if (!participant.getContract().getId().equals(contractId)) {
            throw new IllegalArgumentException("참여자가 해당 계약에 속하지 않습니다.");
        }
        
        // 이미 서명한 경우에는 변경하지 않음
        if (participant.isSigned()) {
            log.info("참여자가 이미 서명을 완료했습니다: 참여자ID={}", participantId);
            return participant;
        }
        
        // 상태를 '서명 중'으로 변경
        Code signingStatus = codeRepository.findById(PARTICIPANT_STATUS_SIGNING)
            .orElseThrow(() -> new EntityNotFoundException("서명 중 상태 코드를 찾을 수 없습니다: " + PARTICIPANT_STATUS_SIGNING));
        participant.setStatusCode(signingStatus);
        
        // 필요한 경우 계약 상태도 서명 진행중으로 변경
        if (contract.getStatusCode() == null || 
            CONTRACT_STATUS_TEMP.equals(contract.getStatusCode().getCodeId())) {
            updateContractStatus(contractId, CONTRACT_STATUS_SIGNING, "System");
        }
        
        // 계약 진행률 업데이트 - 서명 중인 참여자도 진행률에 반영
        contract.calculateProgressRate();
        contractRepository.save(contract);
        
        participantRepository.save(participant);
        log.info("참여자 서명 시작: 참여자ID={}", participantId);
        
        return participant;
    }
    
    /**
     * 참여자가 재서명을 요청합니다.
     * 이 메서드는 참여자가 계약 서명 완료 후 결과 페이지에서 재서명을 요청할 때 호출됩니다.
     * 
     * @param contractId 계약 ID
     * @param participantId 참여자 ID
     * @param reason 재서명 요청 이유 (선택)
     * @return 업데이트된 참여자 객체
     */
    @Transactional
    public ContractParticipant requestParticipantReSign(Long contractId, Long participantId, String reason) {
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new EntityNotFoundException("계약을 찾을 수 없습니다: " + contractId));
            
        ContractParticipant participant = participantRepository.findById(participantId)
            .orElseThrow(() -> new EntityNotFoundException("참여자를 찾을 수 없습니다: " + participantId));
        
        // 참여자가 해당 계약에 속하는지 확인
        if (!participant.getContract().getId().equals(contractId)) {
            throw new IllegalArgumentException("참여자가 해당 계약에 속하지 않습니다.");
        }
        
        // 이미 재서명 요청 상태인 경우 예외 발생
        if (participant.getStatusCode() != null && 
            "007001_0006".equals(participant.getStatusCode().getCodeId())) {
            throw new IllegalStateException("이미 재서명 요청 상태입니다.");
        }
        
        log.info("참여자 재서명 요청 처리 시작: 계약ID={}, 참여자ID={}", contractId, participantId);
        
        try {
            // 상태를 '재서명 요청'으로 변경
            Code resignStatus = codeRepository.findById("007001_0006")
                .orElseThrow(() -> new EntityNotFoundException("재서명 요청 상태 코드를 찾을 수 없습니다: 007001_0006"));
            
            participant.setStatusCode(resignStatus);
            
            // 기존 필드에도 정보 저장 (현재 상태 반영)
            participant.setResignRequestReason(reason);
            participant.setResignRequestedAt(LocalDateTime.now());
            
            // 재서명 이력 테이블에 기록
            ParticipantResignHistory history = new ParticipantResignHistory(participant, reason);
            resignHistoryRepository.save(history);
            
            // 참여자 저장
            participantRepository.save(participant);
            log.info("참여자 재서명 요청 완료: 계약ID={}, 참여자ID={}, 이력ID={}", 
                contractId, participantId, history.getId());
        } catch (Exception e) {
            log.error("참여자 재서명 요청 처리 중 오류 발생", e);
            throw e;
        }
        
        return participant;
    }
    
    /**
     * 관리자가 참여자의 재서명 요청을 승인합니다.
     * 이 메서드는 관리자가 참여자의 재서명 요청을 승인할 때 호출됩니다.
     * 
     * @param contractId 계약 ID
     * @param participantId 참여자 ID
     * @param approver 승인자 이름
     * @return 업데이트된 참여자 객체
     */
    @Transactional
    public ContractParticipant approveParticipantReSign(Long contractId, Long participantId, String approver) {
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new EntityNotFoundException("계약을 찾을 수 없습니다: " + contractId));
            
        ContractParticipant participant = participantRepository.findById(participantId)
            .orElseThrow(() -> new EntityNotFoundException("참여자를 찾을 수 없습니다: " + participantId));
        
        // 참여자가 해당 계약에 속하는지 확인
        if (!participant.getContract().getId().equals(contractId)) {
            throw new IllegalArgumentException("참여자가 해당 계약에 속하지 않습니다.");
        }
        
        // 재서명 요청 상태인지 확인
        if (participant.getStatusCode() == null || 
            !"007001_0006".equals(participant.getStatusCode().getCodeId())) {
            throw new IllegalStateException("재서명 요청 상태인 참여자만 승인할 수 있습니다.");
        }
        
        log.info("참여자 재서명 승인 처리 시작: 계약ID={}, 참여자ID={}, 승인자={}", 
            contractId, participantId, approver);
        
        try {
            // 가장 최근의 미처리 재서명 요청 이력 조회
            ParticipantResignHistory latestHistory = resignHistoryRepository
                .findTopByParticipantIdOrderByRequestedAtDesc(participantId);
                
            if (latestHistory == null || latestHistory.isProcessed()) {
                log.warn("미처리된 재서명 요청이 없습니다. 새로운 이력을 생성합니다: 참여자ID={}", participantId);
                latestHistory = new ParticipantResignHistory(participant, "시스템 생성: 이력 누락");
                resignHistoryRepository.save(latestHistory);
            }
            
            // 이력 승인 처리
            latestHistory.approve(approver);
            resignHistoryRepository.save(latestHistory);
            
            // 서명 상태 초기화
            participant.setSigned(false);
            participant.setSignedAt(null);
            participant.setApproved(false);
            participant.setApprovedAt(null);
            participant.setApprovalComment(null);
            
            // 참여자의 템플릿 매핑 서명 상태도 초기화
            if (participant.getTemplateMappings() != null) {
                for (var mapping : participant.getTemplateMappings()) {
                    mapping.setSigned(false);
                    mapping.setSignedAt(null);
                    mapping.setSignedPdfId(null);
                }
            }
            
            // 상태를 '서명 대기'로 변경
            Code waitingStatus = codeRepository.findById(PARTICIPANT_STATUS_WAITING)
                .orElseThrow(() -> new EntityNotFoundException("서명 대기 상태 코드를 찾을 수 없습니다: " + PARTICIPANT_STATUS_WAITING));
            participant.setStatusCode(waitingStatus);
            
            // 기존 필드에도 정보 저장 (현재 상태 반영)
            participant.setResignApprovedBy(approver);
            participant.setResignApprovedAt(LocalDateTime.now());
            
            // 계약 상태 업데이트 (서명 진행중으로 변경)
            if (!CONTRACT_STATUS_SIGNING.equals(contract.getStatusCode().getCodeId())) {
                updateContractStatus(contractId, CONTRACT_STATUS_SIGNING, approver);
            }
            
            // 저장
            participantRepository.save(participant);
            log.info("참여자 재서명 승인 완료: 계약ID={}, 참여자ID={}, 이력ID={}", 
                contractId, participantId, latestHistory.getId());
            
            // 재서명 요청 메일이나 SMS 발송
            try {
                // 새로운 서명 토큰 생성
                String token = participantTokenService.generateParticipantToken(participantId);
                String signatureUrl = frontendBaseUrl + "/contract-sign?token=" + token;
                
                // 참여자의 알림 유형에 따라 이메일 또는 SMS 발송
                NotificationType notifyType = participant.getNotifyType();
                if (NotificationType.EMAIL.equals(notifyType) && participant.getEmail() != null) {
                    // 이메일 복호화
                    String decryptedEmail = encryptionUtil.decrypt(participant.getEmail());
                    
                    // 재서명 요청 이메일 발송 - 전용 메서드 사용
                    emailService.sendResignRequestEmail(
                        participantId,
                        decryptedEmail,
                        participant.getName(),
                        contract.getTitle(),
                        frontendBaseUrl
                    );
                    log.info("재서명 요청 이메일 발송 완료: 참여자ID={}, 이메일={}", 
                        participantId, decryptedEmail.replaceAll("(?<=.{3}).(?=.*@)", "*"));
                } else if (NotificationType.SMS.equals(notifyType) && participant.getPhoneNumber() != null) {
                    // SMS 발송
                    smsService.sendSignatureSMS(participant, signatureUrl, contract.getTitle() + " (재서명)");
                    log.info("재서명 요청 SMS 발송 완료: 참여자ID={}", participantId);
                } else {
                    log.warn("참여자 알림 유형이 없거나 연락처 정보가 없습니다: 참여자ID={}, 알림유형={}", 
                        participantId, notifyType);
                }
            } catch (Exception e) {
                // 알림 발송 실패 시에도 재서명 승인 처리는 계속 진행
                log.error("재서명 요청 알림 발송 실패: {}", e.getMessage(), e);
            }
            
            // 계약 진행률 다시 계산
            contract.calculateProgressRate();
            contractRepository.save(contract);
        } catch (Exception e) {
            log.error("참여자 재서명 승인 처리 중 오류 발생", e);
            throw e;
        }
        
        return participant;
    }

    /**
     * 특정 참여자의 재서명 이력을 조회합니다.
     * 
     * @param contractId 계약 ID
     * @param participantId 참여자 ID
     * @return 재서명 이력 목록
     */
    public List<?> getParticipantResignHistory(Long contractId, Long participantId) {
        // 참여자가 해당 계약에 속하는지 확인
        ContractParticipant participant = participantRepository.findById(participantId)
            .orElseThrow(() -> new EntityNotFoundException("참여자를 찾을 수 없습니다: " + participantId));
            
        if (!participant.getContract().getId().equals(contractId)) {
            throw new IllegalArgumentException("참여자가 해당 계약에 속하지 않습니다.");
        }
        
        // 참여자의 재서명 이력 조회
        return resignHistoryRepository.findByParticipantIdOrderByRequestedAtDesc(participantId);
    }
    
    /**
     * 계약의 모든 재서명 이력을 조회합니다.
     * 
     * @param contractId 계약 ID
     * @return 재서명 이력 목록
     */
    public List<?> getContractResignHistory(Long contractId) {
        // 계약 존재 여부 확인
        if (!contractRepository.existsById(contractId)) {
            throw new EntityNotFoundException("계약을 찾을 수 없습니다: " + contractId);
        }
        
        // 계약의 모든 재서명 이력 조회
        return resignHistoryRepository.findByContractIdOrderByRequestedAtDesc(contractId);
    }
} 