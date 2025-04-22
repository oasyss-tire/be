package com.inspection.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspection.aop.ContractEventLogData;
import com.inspection.entity.Code;
import com.inspection.entity.Contract;
import com.inspection.entity.ContractEventLog;
import com.inspection.entity.ContractParticipant;
import com.inspection.entity.ContractTemplate;
import com.inspection.entity.ContractTemplateMapping;
import com.inspection.entity.ParticipantTemplateMapping;
import com.inspection.entity.User;
import com.inspection.repository.CodeRepository;
import com.inspection.repository.ContractEventLogRepository;
import com.inspection.repository.UserRepository;
import com.inspection.service.ContractService;
import com.inspection.repository.ContractParticipantRepository;
import com.inspection.service.CorrectionRequestService;
import com.inspection.dto.CorrectionRequestDTO;
import com.inspection.entity.ParticipantDocument;
import com.inspection.service.ParticipantDocumentService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * 계약 관련 작업 시 자동으로 이력을 남기는 Aspect
 */
@Aspect
@Component
@RequiredArgsConstructor
public class ContractEventLogAspect {

    private static final Logger log = LoggerFactory.getLogger(ContractEventLogAspect.class);
    
    private final ContractEventLogRepository eventLogRepository;
    private final CodeRepository codeRepository;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final ContractParticipantRepository participantRepository;
    
    // 이벤트 타입 코드 ID 상수
    private static final String EVENT_CONTRACT_CREATED = "001005_0001";     // 계약 생성
    private static final String EVENT_CONTRACT_UPDATED = "001005_0002";     // 계약 수정
    private static final String EVENT_STATUS_CHANGED = "001005_0003";       // 상태 변경
    private static final String EVENT_CONTRACT_APPROVED = "001005_0004";    // 계약 승인
    private static final String EVENT_CONTRACT_REJECTED = "001005_0005";    // 계약 거부
    private static final String EVENT_CONTRACT_DEACTIVATED = "001005_0006"; // 계약 비활성화
    private static final String EVENT_PARTICIPANT_SIGNED = "001005_0007";   // 참여자 서명 완료
    private static final String EVENT_RESIGN_REQUESTED = "001005_0008";     // 재서명 요청(관리자->참여자)
    private static final String EVENT_RESIGN_APPROVED = "001005_0009";      // 재서명 승인
    private static final String EVENT_RESIGN_COMPLETED = "001005_0010";     // 재서명 완료
    private static final String EVENT_PARTICIPANT_RESIGN_REQUESTED = "001005_0011"; // 재서명 요청(참여자->관리자)
    private static final String EVENT_DOCUMENT_UPLOADED = "001005_0012";    // 참여자 문서 업로드
    
    // 액터 타입 코드 ID 상수
    private static final String ACTOR_TYPE_ADMIN = "001006_0001";       // 관리자
    private static final String ACTOR_TYPE_PARTICIPANT = "001006_0002"; // 참여자
    private static final String ACTOR_TYPE_SYSTEM = "001006_0003";      // 시스템
    
    /**
     * 계약 생성 후 이력 남기기
     */
    @AfterReturning(
        pointcut = "execution(* com.inspection.service.ContractService.createContract(..)) && args(request)",
        returning = "contract"
    )
    public void logContractCreation(JoinPoint joinPoint, Object request, Contract contract) {
        try {
            // 이벤트 타입 코드 조회
            Code eventTypeCode = codeRepository.findById(EVENT_CONTRACT_CREATED)
                .orElseThrow(() -> new RuntimeException("이벤트 타입 코드를 찾을 수 없습니다: " + EVENT_CONTRACT_CREATED));
            
            // 액터 타입 코드 조회
            Code actorTypeCode = codeRepository.findById(ACTOR_TYPE_ADMIN)
                .orElseThrow(() -> new RuntimeException("액터 타입 코드를 찾을 수 없습니다: " + ACTOR_TYPE_ADMIN));
            
            // 현재 사용자 정보 가져오기
            User currentUser = getCurrentUser();
            String actorId = currentUser != null ? currentUser.getUserId() : "관리자";
            
            // 참여자 정보 가져오기 (계약의 주요 참여자 또는 첫 번째 참여자)
            ContractParticipant mainParticipant = null;
            if (contract.getParticipants() != null && !contract.getParticipants().isEmpty()) {
                mainParticipant = contract.getParticipants().get(0);  // 첫 번째 참여자를 주요 참여자로 간주
            }
            
            // request 객체에서 필요한 정보 추출
            String additionalData = null;
            try {
                // 전체 요청 객체 대신 중요 정보만 추출하여 저장
                ContractEventLogData.ContractCreationData creationData = new ContractEventLogData.ContractCreationData();
                creationData.setContractNumber(contract.getContractNumber());
                creationData.setTitle(contract.getTitle());
                creationData.setStatusCode(contract.getStatusCode() != null ? contract.getStatusCode().getCodeId() : null);
                creationData.setDocumentId(contract.getContractPdfId());
                
                // 참여자 정보가 있다면 추가
                if (mainParticipant != null) {
                    creationData.setParticipantName(mainParticipant.getName());
                    creationData.setParticipantId(mainParticipant.getId());
                }
                
                additionalData = objectMapper.writeValueAsString(creationData);
            } catch (JsonProcessingException e) {
                log.warn("추가 데이터 JSON 변환 중 오류 발생: {}", e.getMessage());
            }
            
            // 이력 객체 생성 - 참여자 정보도 포함
            ContractEventLog eventLog = ContractEventLog.create(
                contract,
                mainParticipant,  // 참여자 정보 포함
                eventTypeCode,
                currentUser,  // 사용자 정보
                actorTypeCode,
                getClientIp(),
                contract.getContractPdfId(),  // 문서 ID 설정
                additionalData,
                null  // 설명은 아래에서 별도로 설정
            );
            
            // 이벤트 시간을 계약 생성 시간과 정확히 일치시킴 (법적 무결성 보장)
            eventLog.setEventTime(contract.getCreatedAt());
            
            // 설명 메시지에 참여자 정보 포함
            String participantName = mainParticipant != null ? mainParticipant.getName() : "미지정";
            String userName = currentUser != null ? currentUser.getUserName() : "관리자";
            String description = String.format("%s님이 %s님에게 계약을 생성했습니다. 계약번호: %s", 
                userName, participantName, contract.getContractNumber());
            eventLog.setDescription(description);
            
            // 이력 저장
            eventLogRepository.save(eventLog);
        } catch (Exception e) {
            // 이력 기록 실패 시 예외를 던지지 않고 로그만 남김
            log.error("계약 생성 이력 기록 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 계약 상태 변경 이력 남기기
     */
    @AfterReturning(
        pointcut = "execution(* com.inspection.service.ContractService.updateContractStatus(..)) && args(contractId, statusCodeId, updatedBy)",
        returning = "contract"
    )
    public void logContractStatusChange(JoinPoint joinPoint, Long contractId, String statusCodeId, String updatedBy, Contract contract) {
        try {
            // 이벤트 타입 코드 조회
            Code eventTypeCode = codeRepository.findById(EVENT_STATUS_CHANGED)
                .orElseThrow(() -> new RuntimeException("이벤트 타입 코드를 찾을 수 없습니다: " + EVENT_STATUS_CHANGED));
            
            // 액터 타입 코드 조회
            Code actorTypeCode = codeRepository.findById(ACTOR_TYPE_ADMIN)
                .orElseThrow(() -> new RuntimeException("액터 타입 코드를 찾을 수 없습니다: " + ACTOR_TYPE_ADMIN));
            
            // 상태 코드 정보 가져오기
            Code statusCode = codeRepository.findById(statusCodeId)
                .orElse(null);
            
            // 추가 데이터 생성
            String additionalData = null;
            try {
                additionalData = objectMapper.writeValueAsString(new ContractEventLogData.StatusChangeData(statusCodeId, statusCode != null ? statusCode.getCodeName() : null));
            } catch (JsonProcessingException e) {
                log.warn("추가 데이터 JSON 변환 중 오류 발생: {}", e.getMessage());
            }
            
            // 이벤트 설명 생성
            String description = String.format("계약 상태가 변경되었습니다. 상태: %s, 처리자: %s", 
                statusCode != null ? statusCode.getCodeName() : statusCodeId, updatedBy);
            
            // 이력 객체 생성
            ContractEventLog eventLog = ContractEventLog.create(
                contract,
                null,  // 참여자 정보 없음
                eventTypeCode,
                updatedBy,  // 액터 ID
                actorTypeCode,
                getClientIp(),
                null,  // 문서 ID 없음
                additionalData,
                description
            );
            
            // 이력 저장
            eventLogRepository.save(eventLog);
        } catch (Exception e) {
            log.error("계약 상태 변경 이력 기록 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 계약 승인 이력 남기기
     */
    @AfterReturning(
        pointcut = "execution(* com.inspection.service.ContractService.approveContract(..)) && args(contractId, approver)",
        returning = "contract"
    )
    public void logContractApproval(JoinPoint joinPoint, Long contractId, String approver, Contract contract) {
        try {
            // 이벤트 타입 코드 조회
            Code eventTypeCode = codeRepository.findById(EVENT_CONTRACT_APPROVED)
                .orElseThrow(() -> new RuntimeException("이벤트 타입 코드를 찾을 수 없습니다: " + EVENT_CONTRACT_APPROVED));
            
            // 액터 타입 코드 조회
            Code actorTypeCode = codeRepository.findById(ACTOR_TYPE_ADMIN)
                .orElseThrow(() -> new RuntimeException("액터 타입 코드를 찾을 수 없습니다: " + ACTOR_TYPE_ADMIN));
            
            // 이력 객체 생성
            ContractEventLog eventLog = ContractEventLog.create(
                contract,
                null,  // 참여자 정보 없음
                eventTypeCode,
                approver,  // 액터 ID
                actorTypeCode,
                getClientIp(),
                null,  // 문서 ID 없음
                null,  // 추가 데이터 없음
                String.format("계약이 %s님에 의해 승인되었습니다.", approver)
            );
            
            // 이력 저장
            eventLogRepository.save(eventLog);

        } catch (Exception e) {
            log.error("계약 승인 이력 기록 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 계약 비활성화 이력 남기기
     */
    @AfterReturning(
        pointcut = "execution(* com.inspection.service.ContractService.deactivateContract(..)) && args(contractId, reason)",
        returning = "result"
    )
    public void logContractDeactivation(JoinPoint joinPoint, Long contractId, String reason, Object result) {
        try {
            // ContractService에서 계약 객체를 가져옴
            ContractService contractService = (ContractService) joinPoint.getTarget();
            Contract contract = contractService.getContract(contractId);
            
            // 이벤트 타입 코드 조회
            Code eventTypeCode = codeRepository.findById(EVENT_CONTRACT_DEACTIVATED)
                .orElseThrow(() -> new RuntimeException("이벤트 타입 코드를 찾을 수 없습니다: " + EVENT_CONTRACT_DEACTIVATED));
            
            // 액터 타입 코드 조회
            Code actorTypeCode = codeRepository.findById(ACTOR_TYPE_ADMIN)
                .orElseThrow(() -> new RuntimeException("액터 타입 코드를 찾을 수 없습니다: " + ACTOR_TYPE_ADMIN));
            
            // TODO: 현재 사용자 정보를 가져오는 로직 추가 (Spring Security 활용)
            String actorId = "관리자";  // 기본값
            
            // 추가 데이터 생성
            String additionalData = null;
            try {
                additionalData = objectMapper.writeValueAsString(new ContractEventLogData.DeactivationData(reason));
            } catch (JsonProcessingException e) {
                log.warn("추가 데이터 JSON 변환 중 오류 발생: {}", e.getMessage());
            }
            
            // 이력 객체 생성
            ContractEventLog eventLog = ContractEventLog.create(
                contract,
                null,  // 참여자 정보 없음
                eventTypeCode,
                actorId,
                actorTypeCode,
                getClientIp(),
                null,  // 문서 ID 없음
                additionalData,
                String.format("계약이 비활성화되었습니다. 사유: %s", reason)
            );
            
            // 이력 저장
            eventLogRepository.save(eventLog);
        } catch (Exception e) {
            log.error("계약 비활성화 이력 기록 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 참여자 서명 완료 이력 남기기
     */
    @AfterReturning(
        pointcut = "execution(* com.inspection.service.ContractService.completeParticipantSign(..)) && args(contractId, participantId)",
        returning = "result"
    )
    public void logParticipantSignComplete(JoinPoint joinPoint, Long contractId, Long participantId, Object result) {
        try {
            // ContractService에서 계약 객체를 가져옴
            ContractService contractService = (ContractService) joinPoint.getTarget();
            Contract contract = contractService.getContract(contractId);
            
            // 참여자 객체 직접 조회 (실제 참여자 정보 사용)
            ContractParticipant participant = null;
            if (contract.getParticipants() != null) {
                participant = contract.getParticipants().stream()
                    .filter(p -> p.getId().equals(participantId))
                    .findFirst()
                    .orElse(null);
            }
            
            if (participant == null) {
                log.warn("참여자 서명 완료 이력 기록 중 참여자를 찾을 수 없습니다. 계약ID: {}, 참여자ID: {}", contractId, participantId);
                return;
            }
            
            // 이벤트 타입 코드 조회
            Code eventTypeCode = codeRepository.findById(EVENT_PARTICIPANT_SIGNED)
                .orElseThrow(() -> new RuntimeException("이벤트 타입 코드를 찾을 수 없습니다: " + EVENT_PARTICIPANT_SIGNED));
            
            // 액터 타입 코드 조회
            Code actorTypeCode = codeRepository.findById(ACTOR_TYPE_PARTICIPANT)
                .orElseThrow(() -> new RuntimeException("액터 타입 코드를 찾을 수 없습니다: " + ACTOR_TYPE_PARTICIPANT));
            
            // 참여자 이름 조회
            String participantName = participant.getName();
            
            // 템플릿 매핑이 있는지 확인
            if (participant.getTemplateMappings() == null || participant.getTemplateMappings().isEmpty()) {
                log.warn("참여자 서명 완료 이력 기록 중 템플릿 매핑을 찾을 수 없습니다. 참여자ID: {}", participantId);
                return;
            }
            
            // 각 서명된 템플릿에 대해 별도의 이력 생성
            for (ParticipantTemplateMapping templateMapping : participant.getTemplateMappings()) {
                // 서명되지 않은 템플릿은 건너뜀
                if (!templateMapping.isSigned() || templateMapping.getSignedAt() == null) {
                    continue;
                }
                
                // 템플릿 계약 매핑 정보
                ContractTemplateMapping contractTemplateMapping = templateMapping.getContractTemplateMapping();
                
                // 템플릿 정보
                ContractTemplate template = null;
                String templateName = "템플릿";
                if (contractTemplateMapping != null && contractTemplateMapping.getTemplate() != null) {
                    template = contractTemplateMapping.getTemplate();
                    templateName = template.getTemplateName();
                }
                
                // 서명 시간과 서명된 PDF ID
                LocalDateTime signedAt = templateMapping.getSignedAt();
                String signedPdfId = templateMapping.getSignedPdfId();
                
                // 추가 데이터 생성
                String additionalData = null;
                try {
                    ContractEventLogData.SignCompleteData signData = new ContractEventLogData.SignCompleteData();
                    signData.setParticipantId(participantId);
                    signData.setParticipantName(participantName);
                    signData.setSignedAt(signedAt);
                    signData.setSignedPdfId(signedPdfId);
                    
                    // 템플릿 매핑 ID 추가
                    signData.setTemplateMappingId(templateMapping.getId());
                    
                    // 템플릿 정보 추가
                    if (template != null) {
                        signData.setTemplateId(template.getId());
                        signData.setTemplateName(templateName);
                    }
                    
                    additionalData = objectMapper.writeValueAsString(signData);
                } catch (Exception e) {
                    log.warn("추가 데이터 준비 중 오류 발생: {}", e.getMessage());
                    continue; // 오류 발생 시 다음 템플릿으로 넘어감
                }
                
                // 이력 객체 생성
                ContractEventLog eventLog = ContractEventLog.create(
                    contract,
                    participant,
                    eventTypeCode,
                    participantName,  // 액터 ID
                    actorTypeCode,
                    getClientIp(),
                    signedPdfId,  // 서명된 문서 ID
                    additionalData,
                    String.format("%s님이 '%s' 문서에 서명했습니다. 계약번호: %s", 
                        participantName, templateName, contract.getContractNumber())
                );
                
                // 이벤트 시간을 참여자 서명 시간과 정확히 일치시킴 (법적 무결성 보장)
                if (signedAt != null) {
                    eventLog.setEventTime(signedAt);
                }
                
                // 이력 저장
                eventLogRepository.save(eventLog);
            }
        } catch (Exception e) {
            log.error("참여자 서명 완료 이력 기록 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 관리자 재서명 요청 이력 남기기 (CorrectionRequest 통한 요청)
     */
    @AfterReturning(
        pointcut = "execution(* com.inspection.service.CorrectionRequestService.createCorrectionRequest(..)) && args(participantId, request)",
        returning = "result"
    )
    public void logCorrectionRequest(JoinPoint joinPoint, Long participantId, CorrectionRequestDTO request, Object result) {
        try {
            // 참여자 조회
            ContractParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("참여자 정보를 찾을 수 없습니다: " + participantId));
            
            // 계약 조회
            Contract contract = participant.getContract();
            
            // 이벤트 타입 코드 조회
            Code eventTypeCode = codeRepository.findById(EVENT_RESIGN_REQUESTED)
                .orElseThrow(() -> new RuntimeException("이벤트 타입 코드를 찾을 수 없습니다: " + EVENT_RESIGN_REQUESTED));
            
            // 액터 타입 코드 조회
            Code actorTypeCode = codeRepository.findById(ACTOR_TYPE_ADMIN)
                .orElseThrow(() -> new RuntimeException("액터 타입 코드를 찾을 수 없습니다: " + ACTOR_TYPE_ADMIN));
            
            // 현재 사용자 정보 가져오기
            User currentUser = getCurrentUser();
            String adminName = currentUser != null ? currentUser.getUserName() : "관리자";
            
            // 재서명 사유 추출
            String reason = request.getCorrectionComment();
            
            // 추가 데이터 생성
            String additionalData = null;
            try {
                // 추가 데이터 생성 (관리자가 참여자에게 재서명 요청한 내용)
                ContractEventLogData.ResignRequestData resignData = new ContractEventLogData.ResignRequestData(reason, participantId);
                resignData.setRequestedBy(adminName);
                resignData.setRequestedAt(LocalDateTime.now());
                additionalData = objectMapper.writeValueAsString(resignData);
            } catch (Exception e) {
                log.warn("추가 데이터 JSON 변환 중 오류 발생: {}", e.getMessage());
            }
            
            // 이력 객체 생성
            ContractEventLog eventLog;
            if (currentUser != null) {
                eventLog = ContractEventLog.create(
                    contract,
                    participant,
                    eventTypeCode,
                    currentUser,  // User 객체 직접 전달
                    actorTypeCode,
                    getClientIp(),
                    null,  // 문서 ID 없음
                    additionalData,
                    String.format("%s님이 %s님에게 재서명을 요청하였습니다. 사유: %s", 
                        adminName, participant.getName(), reason)
                );
            } else {
                eventLog = ContractEventLog.create(
                    contract,
                    participant,
                    eventTypeCode,
                    adminName,  // 문자열 ID
                    actorTypeCode,
                    getClientIp(),
                    null,  // 문서 ID 없음
                    additionalData,
                    String.format("%s님이 %s님에게 재서명을 요청하였습니다. 사유: %s", 
                        adminName, participant.getName(), reason)
                );
            }
            
            // 이벤트 시간을 참여자의 재서명 요청 시간과 일치시킴 (법적 무결성 보장)
            // CorrectionRequestService에서 설정된 resign_requested_at 값 사용
            LocalDateTime resignRequestTime = participant.getResignRequestedAt();
            if (resignRequestTime == null) {
                // 참여자 엔티티에 시간이 설정되지 않았을 경우에만 현재 시간 사용
                resignRequestTime = LocalDateTime.now();
                log.warn("참여자의 재서명 요청 시간이 설정되어 있지 않습니다. 현재 시간을 사용합니다.");
            }
            eventLog.setEventTime(resignRequestTime);
            
            // 이력 저장
            eventLogRepository.save(eventLog);
                
        } catch (Exception e) {
            log.error("관리자 재서명 요청 이력 기록 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 재서명 승인 이력 남기기
     */
    @AfterReturning(
        pointcut = "execution(* com.inspection.service.ContractService.approveParticipantReSign(..)) && args(contractId, participantId, approver)",
        returning = "participant"
    )
    public void logResignApproval(JoinPoint joinPoint, Long contractId, Long participantId, String approver, ContractParticipant participant) {
        try {
            // ContractService에서 계약 객체를 가져옴
            ContractService contractService = (ContractService) joinPoint.getTarget();
            Contract contract = contractService.getContract(contractId);
            
            // 이벤트 타입 코드 조회
            Code eventTypeCode = codeRepository.findById(EVENT_RESIGN_APPROVED)
                .orElseThrow(() -> new RuntimeException("이벤트 타입 코드를 찾을 수 없습니다: " + EVENT_RESIGN_APPROVED));
            
            // 현재 사용자 정보 가져오기
            User currentUser = getCurrentUser();
            String adminName = currentUser != null ? currentUser.getUserName() : approver;

            
            // 액터 타입 코드 조회
            Code actorTypeCode = codeRepository.findById(ACTOR_TYPE_ADMIN)
                .orElseThrow(() -> new RuntimeException("액터 타입 코드를 찾을 수 없습니다: " + ACTOR_TYPE_ADMIN));
            
            // 참여자 이름 (참여자 객체에서 가져옴)
            String participantName = participant != null ? participant.getName() : "참여자";
            
            // 재서명 승인 시간 가져오기 (법적 무결성을 위해 정확한 시간 사용)
            LocalDateTime resignApprovedAt = participant.getResignApprovedAt();
            if (resignApprovedAt == null) {
                resignApprovedAt = LocalDateTime.now(); // 승인 시간이 없는 경우 현재 시간 사용
                log.warn("참여자 재서명 승인 시간 정보가 누락되어 현재 시간을 사용합니다. 참여자ID: {}", participantId);
            }
            
            // 추가 데이터 생성
            String additionalData = null;
            try {
                // 재서명 승인 관련 데이터 객체 생성
                ContractEventLogData.ResignApprovalData approvalData = new ContractEventLogData.ResignApprovalData();
                approvalData.setParticipantId(participantId);
                approvalData.setParticipantName(participantName);
                approvalData.setApprovedBy(adminName);
                approvalData.setApprovedAt(resignApprovedAt);
                
                additionalData = objectMapper.writeValueAsString(approvalData);
            } catch (JsonProcessingException e) {
                log.warn("추가 데이터 JSON 변환 중 오류 발생: {}", e.getMessage());
            }
            
            // 이력 객체 생성
            ContractEventLog eventLog = null;
            if (currentUser != null) {
                // User 객체를 직접 전달하는 메서드 사용
                eventLog = ContractEventLog.create(
                    contract,
                    participant,
                    eventTypeCode,
                    currentUser,  // User 객체 직접 전달
                    actorTypeCode,
                    getClientIp(),
                    null,  // 문서 ID 없음
                    additionalData,
                    String.format("%s님의 재서명 요청이 %s님에 의해 승인되었습니다.", participantName, adminName)
                );
            } else {
                // 문자열 ID 전달
                eventLog = ContractEventLog.create(
                    contract,
                    participant,
                    eventTypeCode,
                    approver,  // 문자열 ID
                    actorTypeCode,
                    getClientIp(),
                    null,  // 문서 ID 없음
                    additionalData,
                    String.format("%s님의 재서명 요청이 %s님에 의해 승인되었습니다.", participantName, approver)
                );
            }
            
            // 이벤트 시간을 재서명 승인 시간과 정확히 일치시킴 (법적 무결성 보장)
            eventLog.setEventTime(resignApprovedAt);
            
            // 이력 저장
            eventLog = eventLogRepository.save(eventLog);

        } catch (Exception e) {
            log.error("재서명 승인 이력 기록 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 참여자 거부 이력 남기기
     */
    @AfterReturning(
        pointcut = "execution(* com.inspection.service.ContractService.rejectByParticipant(..)) && args(contractId, participantId, reason)",
        returning = "participant"
    )
    public void logParticipantRejection(JoinPoint joinPoint, Long contractId, Long participantId, String reason, ContractParticipant participant) {
        try {
            // ContractService에서 계약 객체를 가져옴
            ContractService contractService = (ContractService) joinPoint.getTarget();
            Contract contract = contractService.getContract(contractId);
            
            // 이벤트 타입 코드 조회
            Code eventTypeCode = codeRepository.findById(EVENT_CONTRACT_REJECTED)
                .orElseThrow(() -> new RuntimeException("이벤트 타입 코드를 찾을 수 없습니다: " + EVENT_CONTRACT_REJECTED));
            
            // 현재 사용자 정보 가져오기 - 일관성을 위해 다른 메서드와 동일한 방식 사용
            User currentUser = getCurrentUser();
            String adminName = currentUser != null ? currentUser.getUserName() : "관리자";
            
            // 액터 타입 코드 조회
            Code actorTypeCode = codeRepository.findById(ACTOR_TYPE_ADMIN)
                .orElseThrow(() -> new RuntimeException("액터 타입 코드를 찾을 수 없습니다: " + ACTOR_TYPE_ADMIN));
            
            // 참여자 이름 (참여자 객체에서 가져옴)
            String participantName = participant != null ? participant.getName() : "참여자";
            
            // 추가 데이터 생성
            String additionalData = null;
            try {
                ContractEventLogData.ParticipantRejectionData rejectionData = new ContractEventLogData.ParticipantRejectionData();
                rejectionData.setParticipantId(participantId);
                rejectionData.setParticipantName(participantName);
                rejectionData.setReason(reason);
                rejectionData.setRejectedBy(adminName);
                rejectionData.setRejectedAt(LocalDateTime.now());
                
                additionalData = objectMapper.writeValueAsString(rejectionData);
            } catch (JsonProcessingException e) {
                log.warn("추가 데이터 JSON 변환 중 오류 발생: {}", e.getMessage());
            }
            
            // 이력 객체 생성
            ContractEventLog eventLog = null;
            if (currentUser != null) {
                // User 객체를 직접 전달하는 메서드 사용

                eventLog = ContractEventLog.create(
                    contract,
                    participant,
                    eventTypeCode,
                    currentUser,  // User 객체 직접 전달
                    actorTypeCode,
                    getClientIp(),
                    null,  // 문서 ID 없음
                    additionalData,
                    String.format("%s님이 %s님의 서명을 거부하였습니다. 사유: %s, 계약번호: %s", 
                        adminName, participantName, reason, contract.getContractNumber())
                );
            } else {
                // 문자열 ID 전달

                eventLog = ContractEventLog.create(
                    contract,
                    participant,
                    eventTypeCode,
                    adminName,  // 문자열 ID
                    actorTypeCode,
                    getClientIp(),
                    null,  // 문서 ID 없음
                    additionalData,
                    String.format("%s님이 %s님의 서명을 거부하였습니다. 사유: %s, 계약번호: %s", 
                        adminName, participantName, reason, contract.getContractNumber())
                );
            }
            
            // 이력 저장
            eventLog = eventLogRepository.save(eventLog);
            
        } catch (Exception e) {
            log.error("참여자 거부 이력 기록 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 참여자 승인 이력 남기기
     */
    @AfterReturning(
        pointcut = "execution(* com.inspection.service.ContractService.approveByParticipant(..)) && args(contractId, participantId, comment)",
        returning = "participant"
    )
    public void logParticipantApproval(JoinPoint joinPoint, Long contractId, Long participantId, String comment, ContractParticipant participant) {
        try {
            // ContractService에서 계약 객체를 가져옴
            ContractService contractService = (ContractService) joinPoint.getTarget();
            Contract contract = contractService.getContract(contractId);
            
            // 이벤트 타입 코드 조회
            Code eventTypeCode = codeRepository.findById(EVENT_CONTRACT_APPROVED)
                .orElseThrow(() -> new RuntimeException("이벤트 타입 코드를 찾을 수 없습니다: " + EVENT_CONTRACT_APPROVED));
            
            // 현재 사용자 정보 가져오기
            User currentUser = getCurrentUser();
            String adminName = currentUser != null ? currentUser.getUserName() : "관리자";

            
            // 액터 타입 코드 조회
            Code actorTypeCode = codeRepository.findById(ACTOR_TYPE_ADMIN)
                .orElseThrow(() -> new RuntimeException("액터 타입 코드를 찾을 수 없습니다: " + ACTOR_TYPE_ADMIN));
            
            // 참여자 이름 (참여자 객체에서 가져옴)
            String participantName = participant != null ? participant.getName() : "참여자";
            
            // 참여자의 승인 시간 가져오기 (법적 무결성을 위해 정확한 시간 사용)
            LocalDateTime approvalTime = participant.getApprovedAt();
            if (approvalTime == null) {
                approvalTime = LocalDateTime.now(); // 승인 시간이 없는 경우 현재 시간 사용
                log.warn("참여자 승인 시간 정보가 누락되어 현재 시간을 사용합니다. 참여자ID: {}", participantId);
            }
            
            // 추가 데이터 생성
            String additionalData = null;
            try {
                // 승인 관련 데이터 객체 생성
                ContractEventLogData.ParticipantApprovalData approvalData = new ContractEventLogData.ParticipantApprovalData();
                approvalData.setParticipantId(participantId);
                approvalData.setParticipantName(participantName);
                approvalData.setComment(comment);
                approvalData.setApprovedBy(adminName);
                approvalData.setApprovedAt(approvalTime); // 승인 시간 설정
                
                additionalData = objectMapper.writeValueAsString(approvalData);
            } catch (JsonProcessingException e) {
                log.warn("추가 데이터 JSON 변환 중 오류 발생: {}", e.getMessage());
            }
            
            // 이력 객체 생성
            ContractEventLog eventLog = null;
            if (currentUser != null) {
                // User 객체를 직접 전달하는 메서드 사용
                eventLog = ContractEventLog.create(
                    contract,
                    participant,
                    eventTypeCode,
                    currentUser,  // User 객체 직접 전달
                    actorTypeCode,
                    getClientIp(),
                    null,  // 문서 ID 없음
                    additionalData,
                    String.format("%s님이 %s님의 서명을 승인하였습니다. 계약번호: %s", 
                        adminName, participantName, contract.getContractNumber())
                );
            } else {
                // 문자열 ID 전달
                eventLog = ContractEventLog.create(
                    contract,
                    participant,
                    eventTypeCode,
                    adminName,  // 문자열 ID
                    actorTypeCode,
                    getClientIp(),
                    null,  // 문서 ID 없음
                    additionalData,
                    String.format("%s님이 %s님의 서명을 승인하였습니다. 계약번호: %s", 
                        adminName, participantName, contract.getContractNumber())
                );
            }
            
            // 이벤트 시간을 참여자 승인 시간과 정확히 일치시킴 (법적 무결성 보장)
            eventLog.setEventTime(approvalTime);
            
            // 이력 저장
            eventLog = eventLogRepository.save(eventLog);

                
        } catch (Exception e) {
            log.error("참여자 승인 이력 기록 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 재서명 완료 이력 남기기
     */
    @AfterReturning(
        pointcut = "execution(* com.inspection.service.CorrectionRequestService.completeCorrections(..)) && args(participantId)",
        returning = "result"
    )
    public void logResignComplete(JoinPoint joinPoint, Long participantId, Object result) {
        try {
            // 참여자 조회
            ContractParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("참여자 정보를 찾을 수 없습니다: " + participantId));
            
            // 계약 조회
            Contract contract = participant.getContract();
            
            // 이벤트 타입 코드 조회
            Code eventTypeCode = codeRepository.findById(EVENT_RESIGN_COMPLETED)
                .orElseThrow(() -> new RuntimeException("이벤트 타입 코드를 찾을 수 없습니다: " + EVENT_RESIGN_COMPLETED));
            
            // 액터 타입 코드 조회
            Code actorTypeCode = codeRepository.findById(ACTOR_TYPE_PARTICIPANT)
                .orElseThrow(() -> new RuntimeException("액터 타입 코드를 찾을 수 없습니다: " + ACTOR_TYPE_PARTICIPANT));
            
            // 참여자 이름
            String participantName = participant.getName();
            
            // 재서명 완료 시간 (법적 무결성 보장을 위한 중요 시간)
            LocalDateTime resignCompletedTime = null;
            
            // 템플릿 매핑에서 재서명 완료 시간 가져오기
            // 가장 최근에 재서명 완료된 템플릿의 시간 사용
            if (participant.getTemplateMappings() != null && !participant.getTemplateMappings().isEmpty()) {
                for (ParticipantTemplateMapping mapping : participant.getTemplateMappings()) {
                    if (mapping.getResignedAt() != null) {
                        // 가장 최근 시간으로 업데이트
                        if (resignCompletedTime == null || mapping.getResignedAt().isAfter(resignCompletedTime)) {
                            resignCompletedTime = mapping.getResignedAt();
                        }
                    }
                }
            }
            
            // 재서명 시간이 없으면 현재 시간 사용
            if (resignCompletedTime == null) {
                resignCompletedTime = LocalDateTime.now();
                log.warn("참여자의 재서명 완료 시간을 찾을 수 없습니다. 현재 시간을 사용합니다. 참여자 ID: {}", participantId);
            }
            
            // 추가 데이터 생성
            String additionalData = null;
            try {
                // 재서명 완료 관련 데이터 객체 생성
                ContractEventLogData.ResignCompleteData resignData = new ContractEventLogData.ResignCompleteData();
                resignData.setParticipantId(participantId);
                resignData.setParticipantName(participantName);
                resignData.setCompletedAt(resignCompletedTime);
                
                // 재서명된 문서 ID가 있다면 추가
                participant.getTemplateMappings().stream()
                    .filter(m -> m.getResignedPdfId() != null)
                    .forEach(m -> resignData.addResignedDocumentId(m.getResignedPdfId()));
                
                additionalData = objectMapper.writeValueAsString(resignData);
            } catch (Exception e) {
                log.warn("추가 데이터 JSON 변환 중 오류 발생: {}", e.getMessage());
            }
            
            // 이력 객체 생성
            ContractEventLog eventLog = ContractEventLog.create(
                contract,
                participant,
                eventTypeCode,
                participantName,  // 참여자 이름을 액터 ID로 사용
                actorTypeCode,
                getClientIp(),
                null,  // 문서 ID (단일 문서가 아닐 수 있어 additionalData에 포함)
                additionalData,
                String.format("%s님이 재서명을 완료하였습니다. 계약번호: %s", 
                    participantName, contract.getContractNumber())
            );
            
            // 이벤트 시간을 재서명 완료 시간과 정확히 일치시킴
            eventLog.setEventTime(resignCompletedTime);
            
            // 이력 저장
            eventLogRepository.save(eventLog);

                
        } catch (Exception e) {
            log.error("재서명 완료 이력 기록 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 참여자 요청 재서명 이력 남기기
     * 참여자가 관리자에게 재서명을 요청하는 경우
     */
    @AfterReturning(
        pointcut = "execution(* com.inspection.service.ContractService.requestParticipantReSign(..)) && args(contractId, participantId, reason)",
        returning = "participant"
    )
    public void logParticipantResignRequest(JoinPoint joinPoint, Long contractId, Long participantId, String reason, ContractParticipant participant) {
        try {
            // ContractService에서 계약 객체를 가져옴
            ContractService contractService = (ContractService) joinPoint.getTarget();
            Contract contract = contractService.getContract(contractId);
            
            // 이벤트 타입 코드 조회
            Code eventTypeCode = codeRepository.findById(EVENT_PARTICIPANT_RESIGN_REQUESTED)
                .orElseThrow(() -> new RuntimeException("이벤트 타입 코드를 찾을 수 없습니다: " + EVENT_PARTICIPANT_RESIGN_REQUESTED));
            
            // 액터 타입 코드 조회
            Code actorTypeCode = codeRepository.findById(ACTOR_TYPE_PARTICIPANT)
                .orElseThrow(() -> new RuntimeException("액터 타입 코드를 찾을 수 없습니다: " + ACTOR_TYPE_PARTICIPANT));
            
            // 참여자 이름 (참여자 객체에서 가져옴)
            String participantName = participant != null ? participant.getName() : "참여자";
            
            // 재서명 요청 시간 (법적 무결성 보장)
            LocalDateTime resignRequestTime = participant.getResignRequestedAt();
            if (resignRequestTime == null) {
                // 시간이 설정되지 않았다면 현재 시간 사용
                resignRequestTime = LocalDateTime.now();
                log.warn("참여자의 재서명 요청 시간이 설정되어 있지 않습니다. 현재 시간을 사용합니다. 참여자ID: {}", participantId);
            }
            
            // 재서명 요청 사유
            String requestReason = reason;
            if (requestReason == null || requestReason.trim().isEmpty()) {
                requestReason = participant.getResignRequestReason();
                if (requestReason == null || requestReason.trim().isEmpty()) {
                    requestReason = "사유 없음";
                }
            }
            
            // 추가 데이터 생성
            String additionalData = null;
            try {
                // 재서명 요청 관련 데이터 객체 생성
                ContractEventLogData.ResignRequestData resignData = new ContractEventLogData.ResignRequestData(requestReason, participantId);
                resignData.setRequestedBy(participantName);
                resignData.setRequestedAt(resignRequestTime);
                additionalData = objectMapper.writeValueAsString(resignData);
            } catch (Exception e) {
                log.warn("추가 데이터 JSON 변환 중 오류 발생: {}", e.getMessage());
            }
            
            // 이력 객체 생성
            ContractEventLog eventLog = ContractEventLog.create(
                contract,
                participant,
                eventTypeCode,
                participantName,  // 참여자 이름을 액터 ID로 사용
                actorTypeCode,
                getClientIp(),
                null,  // 문서 ID 없음
                additionalData,
                String.format("%s님이 재서명을 요청하였습니다. 사유: %s", 
                    participantName, requestReason)
            );
            
            // 이벤트 시간을 재서명 요청 시간과 정확히 일치시킴
            eventLog.setEventTime(resignRequestTime);
            
            // 이력 저장
            eventLogRepository.save(eventLog);

                
        } catch (Exception e) {
            log.error("참여자 재서명 요청 이력 기록 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 참여자 문서 업로드 로깅
     */
    @AfterReturning(
            pointcut = "execution(* com.inspection.service.ParticipantDocumentService.uploadDocument(..))",
            returning = "document"
    )
    public void logParticipantDocumentUpload(JoinPoint joinPoint, Object document) {
        try {
            if (document == null) {
                log.error("참여자 문서 업로드 로깅 실패: 문서가 null입니다.");
                return;
            }

            ParticipantDocument participantDocument = (ParticipantDocument) document;
            Long participantId = participantDocument.getParticipant().getId();
            LocalDateTime uploadTime = participantDocument.getUploadedAt(); // uploadedAt으로 변경

            ContractParticipant participant = participantRepository.findById(participantId)
                    .orElseThrow(() -> new RuntimeException("참여자를 찾을 수 없습니다: " + participantId));
            Contract contract = participant.getContract();
            
            // 이벤트 타입 코드 조회
            Code eventTypeCode = codeRepository.findById(EVENT_DOCUMENT_UPLOADED)
                .orElseThrow(() -> new RuntimeException("이벤트 타입 코드를 찾을 수 없습니다: " + EVENT_DOCUMENT_UPLOADED));
                
            // 액터 타입 코드 조회
            Code actorTypeCode = codeRepository.findById(ACTOR_TYPE_PARTICIPANT)
                .orElseThrow(() -> new RuntimeException("액터 타입 코드를 찾을 수 없습니다: " + ACTOR_TYPE_PARTICIPANT));

            // 현재 사용자 정보 가져오기 - 기존 코드 패턴에 맞게 수정
            User currentUser = getCurrentUser();
            String participantName = participant.getName();

            // 추가 데이터 생성
            ContractEventLogData.DocumentUploadData data = new ContractEventLogData.DocumentUploadData();
            data.setDocumentId(participantDocument.getId());
            data.setParticipantId(participantId);
            data.setParticipantName(participantName);
            data.setDocumentCodeId(participantDocument.getDocumentCode().getCodeId()); // documentCode로 변경
            data.setDocumentName(participantDocument.getDocumentCode().getCodeName()); // getCodeName() 메서드 사용
            data.setOriginalFileName(participantDocument.getOriginalFileName());
            data.setUploadedAt(uploadTime);

            // JSON 변환
            String additionalData = null;
            try {
                additionalData = objectMapper.writeValueAsString(data);
            } catch (Exception e) {
                log.warn("추가 데이터 JSON 변환 실패: {}", e.getMessage());
            }

            // 이력 객체 생성 - 기존 create 패턴 사용
            ContractEventLog eventLog = ContractEventLog.create(
                contract,
                participant,
                eventTypeCode,
                participantName, // 참여자 이름을 액터 ID로 사용
                actorTypeCode,
                getClientIp(),
                participantDocument.getFileId(), // 문서 파일 ID
                additionalData,
                String.format("%s님이 '%s' 문서를 업로드했습니다. 파일명: %s", 
                    participantName, data.getDocumentName(), participantDocument.getOriginalFileName())
            );
            
            // 이벤트 시간을 문서 업로드 시간과 정확히 일치시킴 (법적 무결성 보장)
            eventLog.setEventTime(uploadTime);

            // 로그 저장
            eventLogRepository.save(eventLog);
        } catch (Exception e) {
            log.error("참여자 문서 업로드 이력 기록 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 현재 로그인한 사용자 정보를 가져오는 메서드
     */
    private User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                String username = null;
                
                if (principal instanceof UserDetails) {
                    username = ((UserDetails) principal).getUsername();
                } else if (principal instanceof String) {
                    username = (String) principal;
                }
                
                if (username != null && !username.equals("anonymousUser")) {
                    return userRepository.findByUserId(username).orElse(null);
                }
            }
        } catch (Exception e) {
            log.warn("현재 사용자 정보를 가져오는 중 오류 발생: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 클라이언트 IP 주소 가져오기
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                
                if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("Proxy-Client-IP");
                }
                if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("WL-Proxy-Client-IP");
                }
                if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("HTTP_CLIENT_IP");
                }
                if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("HTTP_X_FORWARDED_FOR");
                }
                if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                
                return ip;
            }
        } catch (Exception e) {
            log.warn("클라이언트 IP 주소를 가져오는 중 오류 발생: {}", e.getMessage());
        }
        return null;
    }
} 