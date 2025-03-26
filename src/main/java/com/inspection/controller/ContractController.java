package com.inspection.controller;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.inspection.dto.ContractDTO;
import com.inspection.dto.CreateContractRequest;
import com.inspection.dto.ParticipantDetailDTO;
import com.inspection.entity.Contract;
import com.inspection.entity.ContractParticipant;
import com.inspection.entity.ParticipantTemplateMapping;
import com.inspection.entity.ParticipantResignHistory;
import com.inspection.service.ContractService;
import com.inspection.dto.PhoneVerificationRequest;
import com.inspection.dto.ErrorResponse;
import com.inspection.util.EncryptionUtil;
import com.inspection.repository.ParticipantTemplateMappingRepository;
import com.inspection.repository.ContractParticipantRepository;
import com.inspection.repository.ContractRepository;
import com.inspection.dto.TemplateSignStatusDTO;
import com.inspection.exception.ResourceNotFoundException;
import com.inspection.service.ParticipantTokenService;
import com.inspection.service.EmailService;
import com.inspection.service.SMSService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;


@Slf4j
@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {
    private final ContractService contractService;
    private final ContractRepository contractRepository;
    private final ParticipantTemplateMappingRepository templateMappingRepository;
    private final ContractParticipantRepository participantRepository;
    private final ParticipantTokenService participantTokenService;
    private final EmailService emailService;
    private final SMSService smsService;
    private final EncryptionUtil encryptionUtil;
    
    @Value("${frontend.base-url}")
    private String frontendBaseUrl;
    
    // 계약 생성
    @PostMapping
    public ResponseEntity<ContractDTO> createContract(@RequestBody CreateContractRequest request) {
        log.info("Contract creation request received: {}", request.getTitle());
        try {
            Contract contract = contractService.createContract(request);
            return ResponseEntity.ok(new ContractDTO(contract, encryptionUtil));
        } catch (Exception e) {
            log.error("Error creating contract", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 계약 상세 조회
    @GetMapping("/{contractId}")
    public ResponseEntity<ContractDTO> getContract(@PathVariable Long contractId) {
        try {
            Contract contract = contractService.getContract(contractId);
            return ResponseEntity.ok(new ContractDTO(contract, encryptionUtil));
        } catch (Exception e) {
            log.error("Error fetching contract: {}", contractId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    // 계약 목록 조회 (활성화된 계약만)
    @GetMapping
    public ResponseEntity<List<ContractDTO>> getContracts() {
        try {
            // 최신순(생성일 기준 내림차순)으로 계약 목록 조회
            List<Contract> contracts = contractService.getActiveContracts();
            
            List<ContractDTO> contractDTOs = contracts.stream()
                .map(contract -> new ContractDTO(contract, encryptionUtil))
                .collect(Collectors.toList());
                
            return ResponseEntity.ok(contractDTOs);
        } catch (Exception e) {
            log.error("Error fetching contracts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // 계약 취소/비활성화
    @DeleteMapping("/{contractId}")
    public ResponseEntity<Void> deactivateContract(
        @PathVariable Long contractId,
        @RequestParam(required = false) String reason
    ) {
        try {
            contractService.deactivateContract(contractId, reason);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deactivating contract: {}", contractId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{contractId}/participants/{participantId}")
    public ResponseEntity<ParticipantDetailDTO> getParticipantDetail(
        @PathVariable Long contractId,
        @PathVariable Long participantId
    ) {
        try {
            ParticipantDetailDTO participant = contractService.getParticipantDetail(contractId, participantId);
            return ResponseEntity.ok(participant);
        } catch (Exception e) {
            log.error("Error fetching participant detail: {}", participantId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{contractId}/participants/{participantId}/sign")
    public ResponseEntity<Void> completeParticipantSign(
        @PathVariable Long contractId,
        @PathVariable Long participantId
    ) {
        try {
            contractService.completeParticipantSign(contractId, participantId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error completing participant sign: {}", participantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 참여자의 서명을 완료하고 장기 토큰을 반환합니다.
     * 이 토큰은 서명 완료 후 페이지 접근 시 참여자 인증에 사용됩니다.
     * 또한, 토큰이 포함된 URL을 참여자 이메일로 자동 전송합니다.
     */
    @PostMapping("/{contractId}/participants/{participantId}/complete-signing")
    public ResponseEntity<?> completeParticipantSigningWithToken(
        @PathVariable Long contractId,
        @PathVariable Long participantId
    ) {
        try {
            // 서명 완료 처리
            contractService.completeParticipantSign(contractId, participantId);
            
            // 참여자 정보 조회
            ContractParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("참여자를 찾을 수 없습니다: " + participantId));
            
            // 계약 정보 조회
            Contract contract = contractService.getContract(contractId);
            
            // 기존 장기 토큰 무효화 후 새 장기 토큰 발급
            String longTermToken = participantTokenService.deactivateAndRegenerateLongTermToken(participantId);
            log.info("기존 장기 토큰 무효화 및 새 토큰 발급 완료: participantId={}", participantId);
            
            // 서명 완료 URL 생성 (프론트엔드 URL)
            String baseUrl = frontendBaseUrl;
            String redirectUrl = baseUrl + "/contract-signed?token=" + longTermToken;
            
            // 참여자 알림 설정에 따라 이메일 또는 SMS로 토큰 URL 전송
            try {
                // 이메일이 있는 경우 이메일 전송
                if (participant.getEmail() != null && !participant.getEmail().trim().isEmpty()) {
                    try {
                        // 이메일이 암호화되어 있는 경우 복호화 시도
                        String decryptedEmail = encryptionUtil.decrypt(participant.getEmail());
                        
                        // 참여자 객체에 임시로 복호화된 이메일 설정 (EmailService에서 사용될 수 있게)
                        ContractParticipant decryptedParticipant = new ContractParticipant();
                        decryptedParticipant.setId(participant.getId());
                        decryptedParticipant.setName(participant.getName());
                        decryptedParticipant.setEmail(decryptedEmail);
                        
                        // 재서명 여부 확인
                        boolean isResigning = participant.getResignRequestedAt() != null && 
                                              participant.getResignApprovedAt() != null;
                        
                        // 재서명 여부에 따라 적절한 이메일 발송
                        if (isResigning) {
                            // 재서명 완료 이메일 발송
                            emailService.sendResignCompletionEmail(decryptedParticipant, redirectUrl, contract.getTitle());
                            log.info("재서명 완료 이메일 전송 성공: participantId={}, email={}", 
                                participantId, decryptedEmail.replaceAll("(?<=.{3}).(?=.*@)", "*"));
                        } else {
                            // 일반 서명 완료 이메일 발송
                            emailService.sendContractCompletionEmail(decryptedParticipant, redirectUrl, contract.getTitle());
                            log.info("계약 완료 이메일 전송 성공: participantId={}, email={}", 
                                participantId, decryptedEmail.replaceAll("(?<=.{3}).(?=.*@)", "*"));
                        }
                    } catch (Exception ex) {
                        log.error("이메일 복호화 또는 전송 실패: {}", ex.getMessage());
                    }
                }
                
                // 휴대폰 번호가 있는 경우 SMS 전송 - 안전하게 처리
                String phoneNumber = participant.getPhoneNumber();
                if (phoneNumber != null && !phoneNumber.isEmpty()) {
                    try {
                        // 암호화된 번호라면 복호화 시도
                        String decryptedPhone = encryptionUtil.decrypt(phoneNumber);
                        
                        // SMS 전송 (복호화된 번호 전달)
                        smsService.sendContractCompletionSMS(participant, redirectUrl, contract.getTitle());
                        log.info("계약 완료 SMS 전송 성공: participantId={}, phone=***", participantId);
                    } catch (Exception ex) {
                        log.error("전화번호 복호화 또는 SMS 전송 실패: {}", ex.getMessage());
                    }
                }
            } catch (Exception e) {
                // 알림 발송 실패 시 로그만 남기고 계속 진행 (중요 로직 아님)
                log.error("계약 완료 알림 전송 실패: {}", e.getMessage());
            }
            
            // 성공 응답 및 토큰 반환
            Map<String, Object> response = Map.of(
                "success", true,
                "token", longTermToken,
                "redirectUrl", redirectUrl
            );
            
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            log.error("Resource not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of("success", false, "message", e.getMessage())
            );
        } catch (Exception e) {
            log.error("Error completing participant sign with token: {}", participantId, e);
            return ResponseEntity.internalServerError().body(
                Map.of("success", false, "message", "서명 완료 처리 중 오류가 발생했습니다.")
            );
        }
    }
    
    @PostMapping("/{contractId}/participants/{participantId}/verify")
    public ResponseEntity<?> verifyParticipant(
        @PathVariable Long contractId,
        @PathVariable Long participantId,
        @RequestBody PhoneVerificationRequest request
    ) {
        try {
            log.info("Participant verification request received for contractId: {}, participantId: {}", 
                contractId, participantId);
                
            boolean isVerified = contractService.verifyParticipantPhone(
                contractId, 
                participantId, 
                request.getPhoneLastDigits()
            );
            
            if (isVerified) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("휴대폰 번호가 일치하지 않습니다."));
            }
        } catch (Exception e) {
            log.error("Error verifying participant", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("인증 처리 중 오류가 발생했습니다."));
        }
    }
    
    // 참여자의 템플릿 서명 상태 조회
    @GetMapping("/{contractId}/participants/{participantId}/template-status")
    public ResponseEntity<List<TemplateSignStatusDTO>> getParticipantTemplateStatus(
            @PathVariable Long contractId,
            @PathVariable Long participantId) {
        
        try {
            // 1. 해당 참여자가 존재하는지 확인
            ContractParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("참여자를 찾을 수 없습니다: " + participantId));
            
            // 2. 해당 계약에 속한 참여자인지 확인
            if (!participant.getContract().getId().equals(contractId)) {
                return ResponseEntity.badRequest().build();
            }
            
            // 3. 참여자의 모든 템플릿 매핑 조회
            List<ParticipantTemplateMapping> mappings = 
                templateMappingRepository.findAllByContractIdAndParticipantId(contractId, participantId);
            
            // 4. DTO로 변환
            List<TemplateSignStatusDTO> statusList = mappings.stream()
                .map(mapping -> {
                    TemplateSignStatusDTO dto = new TemplateSignStatusDTO();
                    dto.setMappingId(mapping.getId());
                    dto.setPdfId(mapping.getPdfId());
                    dto.setSignedPdfId(mapping.getSignedPdfId());
                    dto.setSigned(mapping.isSigned());
                    dto.setSignedAt(mapping.getSignedAt());
                    dto.setTemplateName(mapping.getContractTemplateMapping().getTemplate().getTemplateName());
                    return dto;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(statusList);
            
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("템플릿 서명 상태 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // 모든 참여자의 템플릿 서명 상태 조회
    @GetMapping("/{contractId}/template-status")
    public ResponseEntity<Map<Long, List<TemplateSignStatusDTO>>> getAllParticipantsTemplateStatus(
            @PathVariable Long contractId) {
        
        try {
            // 1. 계약에 속한 모든 참여자 조회
            List<ContractParticipant> participants = participantRepository.findByContractId(contractId);
            
            // 2. 각 참여자별로 템플릿 상태 조회 및 매핑
            Map<Long, List<TemplateSignStatusDTO>> result = new HashMap<>();
            
            for (ContractParticipant participant : participants) {
                List<ParticipantTemplateMapping> mappings = 
                    templateMappingRepository.findAllByContractIdAndParticipantId(contractId, participant.getId());
                
                List<TemplateSignStatusDTO> statusList = mappings.stream()
                    .map(mapping -> {
                        TemplateSignStatusDTO dto = new TemplateSignStatusDTO();
                        dto.setMappingId(mapping.getId());
                        dto.setPdfId(mapping.getPdfId());
                        dto.setSignedPdfId(mapping.getSignedPdfId());
                        dto.setSigned(mapping.isSigned());
                        dto.setSignedAt(mapping.getSignedAt());
                        dto.setTemplateName(mapping.getContractTemplateMapping().getTemplate().getTemplateName());
                        return dto;
                    })
                    .collect(Collectors.toList());
                
                result.put(participant.getId(), statusList);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("모든 참여자 템플릿 서명 상태 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 계약 상태 변경 API
     */
    @PutMapping("/{contractId}/status")
    public ResponseEntity<?> updateContractStatus(
        @PathVariable Long contractId,
        @RequestParam String statusCodeId,
        @RequestParam(required = false) String updatedBy
    ) {
        try {
            log.info("Contract status update request: contractId={}, statusCodeId={}, updatedBy={}", 
                contractId, statusCodeId, updatedBy);
                
            Contract contract = contractService.updateContractStatus(contractId, statusCodeId, updatedBy);
            return ResponseEntity.ok(new ContractDTO(contract, encryptionUtil));
        } catch (Exception e) {
            log.error("Error updating contract status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("계약 상태 변경 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    

    
    /**
     * 참여자 승인 API
     */
    @PostMapping("/{contractId}/participants/{participantId}/approve")
    public ResponseEntity<?> approveByParticipant(
        @PathVariable Long contractId,
        @PathVariable Long participantId,
        @RequestParam(required = false) String comment
    ) {
        try {
            log.info("Participant approval request: contractId={}, participantId={}", contractId, participantId);
                
            ContractParticipant participant = contractService.approveByParticipant(contractId, participantId, comment);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            log.warn("Invalid state for participant approval: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error approving by participant", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("참여자 승인 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 참여자 거부 API
     */
    @PostMapping("/{contractId}/participants/{participantId}/reject")
    public ResponseEntity<?> rejectByParticipant(
        @PathVariable Long contractId,
        @PathVariable Long participantId,
        @RequestParam String reason
    ) {
        try {
            log.info("Participant rejection request: contractId={}, participantId={}", contractId, participantId);
                
            ContractParticipant participant = contractService.rejectByParticipant(contractId, participantId, reason);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            log.warn("Invalid state for participant rejection: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error rejecting by participant", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("참여자 거부 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 참여자가 재서명을 요청하는 API
     * 계약 서명 완료 후 결과 페이지에서 참여자가 직접 재서명을 요청할 때 사용됩니다.
     */
    @PostMapping("/{contractId}/participants/{participantId}/request-resign")
    public ResponseEntity<?> requestReSign(
        @PathVariable Long contractId,
        @PathVariable Long participantId,
        @RequestParam(required = false) String reason
    ) {
        try {
            log.info("Participant resign request: contractId={}, participantId={}", contractId, participantId);
                
            ContractParticipant participant = contractService.requestParticipantReSign(contractId, participantId, reason);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "재서명 요청이 등록되었습니다. 관리자 승인 후 재서명이 가능합니다."
            ));
        } catch (IllegalStateException e) {
            log.warn("Invalid state for resign request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error requesting resign", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("재서명 요청 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 관리자가 참여자의 재서명 요청을 승인하는 API
     * 이를 통해 해당 참여자의 서명 상태가 초기화되고 재서명이 가능해집니다.
     */
    @PostMapping("/{contractId}/participants/{participantId}/approve-resign")
    public ResponseEntity<?> approveReSign(
        @PathVariable Long contractId,
        @PathVariable Long participantId,
        @RequestParam String approver
    ) {
        try {
            log.info("Participant resign approval: contractId={}, participantId={}, approver={}", 
                contractId, participantId, approver);
                
            ContractParticipant participant = contractService.approveParticipantReSign(
                contractId, participantId, approver);
                
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "재서명 요청이 승인되었습니다. 참여자는 이제 재서명할 수 있습니다."
            ));
        } catch (IllegalStateException e) {
            log.warn("Invalid state for resign approval: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error approving resign", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("재서명 승인 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 참여자의 재서명 이력 조회 API
     * 특정 참여자의 모든 재서명 요청 및 승인 이력을 조회합니다.
     */
    @GetMapping("/{contractId}/participants/{participantId}/resign-history")
    public ResponseEntity<?> getResignHistory(
        @PathVariable Long contractId,
        @PathVariable Long participantId
    ) {
        try {
            log.info("Fetching resign history: contractId={}, participantId={}", contractId, participantId);
            
            List<?> history = contractService.getParticipantResignHistory(
                contractId, participantId);
                
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error fetching resign history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("재서명 이력 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 계약의 재서명 이력 조회 API
     * 계약에 속한 모든 참여자의 재서명 요청 및 승인 이력을 조회합니다.
     */
    @GetMapping("/{contractId}/resign-history")
    public ResponseEntity<?> getContractResignHistory(
        @PathVariable Long contractId
    ) {
        try {
            log.info("Fetching contract resign history: contractId={}", contractId);
            
            List<?> history = contractService.getContractResignHistory(contractId);
                
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error fetching contract resign history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("계약 재서명 이력 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 참여자 서명 시작 API
     * 참여자가 서명 페이지에 접근할 때 호출됩니다.
     */
    @PostMapping("/{contractId}/participants/{participantId}/start-signing")
    public ResponseEntity<?> startParticipantSigning(
        @PathVariable Long contractId,
        @PathVariable Long participantId
    ) {
        try {
            log.info("Start participant signing: contractId={}, participantId={}", contractId, participantId);
                
            ContractParticipant participant = contractService.startParticipantSigning(contractId, participantId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            log.warn("Invalid state for starting signing: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error starting participant signing", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("서명 시작 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}