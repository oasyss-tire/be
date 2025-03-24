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
import com.inspection.service.ContractService;
import com.inspection.dto.PhoneVerificationRequest;
import com.inspection.dto.ErrorResponse;
import com.inspection.util.EncryptionUtil;
import com.inspection.repository.ParticipantTemplateMappingRepository;
import com.inspection.repository.ContractParticipantRepository;
import com.inspection.entity.ParticipantTemplateMapping;
import com.inspection.dto.TemplateSignStatusDTO;
import com.inspection.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;


@Slf4j
@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {
    private final ContractService contractService;
    private final EncryptionUtil encryptionUtil;
    private final ParticipantTemplateMappingRepository templateMappingRepository;
    private final ContractParticipantRepository participantRepository;
    
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
     * 계약 승인 API
     */
    @PostMapping("/{contractId}/approve")
    public ResponseEntity<?> approveContract(
        @PathVariable Long contractId,
        @RequestParam String approver
    ) {
        try {
            log.info("Contract approval request: contractId={}, approver={}", contractId, approver);
                
            Contract contract = contractService.approveContract(contractId, approver);
            return ResponseEntity.ok(new ContractDTO(contract, encryptionUtil));
        } catch (IllegalStateException e) {
            log.warn("Invalid contract state for approval: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error approving contract", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("계약 승인 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 계약 반려 API (수정 요청)
     */
    @PostMapping("/{contractId}/reject")
    public ResponseEntity<?> rejectContract(
        @PathVariable Long contractId,
        @RequestParam String rejectionReason,
        @RequestParam String rejector
    ) {
        try {
            log.info("Contract rejection request: contractId={}, rejector={}", contractId, rejector);
                
            Contract contract = contractService.rejectContract(contractId, rejectionReason, rejector);
            return ResponseEntity.ok(new ContractDTO(contract, encryptionUtil));
        } catch (IllegalStateException e) {
            log.warn("Invalid contract state for rejection: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error rejecting contract", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("계약 반려 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 계약 반려 후 재서명 요청 API
     */
    @PostMapping("/{contractId}/resend")
    public ResponseEntity<?> resendContractForReSign(
        @PathVariable Long contractId,
        @RequestParam String reason,
        @RequestParam String rejector
    ) {
        try {
            log.info("Contract resend request: contractId={}, rejector={}", contractId, rejector);
                
            Contract contract = contractService.resendContractForReSign(contractId, reason, rejector);
            return ResponseEntity.ok(new ContractDTO(contract, encryptionUtil));
        } catch (IllegalStateException e) {
            log.warn("Invalid contract state for resend: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error resending contract", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("계약 재전송 중 오류가 발생했습니다: " + e.getMessage()));
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