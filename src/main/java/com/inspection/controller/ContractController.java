package com.inspection.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.inspection.dto.ContractDTO;
import com.inspection.dto.CreateContractRequest;
import com.inspection.dto.ParticipantDetailDTO;
import com.inspection.entity.Contract;
import com.inspection.service.ContractService;
import com.inspection.dto.PhoneVerificationRequest;
import com.inspection.dto.ErrorResponse;
import com.inspection.util.EncryptionUtil;

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
}