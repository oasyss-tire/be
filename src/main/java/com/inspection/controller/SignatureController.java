package com.inspection.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.inspection.dto.ErrorResponse;
import com.inspection.dto.PhoneVerificationRequest;
import com.inspection.entity.Contract;
import com.inspection.entity.ContractParticipant;
import com.inspection.repository.ContractParticipantRepository;
import com.inspection.service.ContractService;
import com.inspection.service.ParticipantTokenService;

import io.jsonwebtoken.JwtException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 서명 관련 API를 처리하는 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/signature")
@RequiredArgsConstructor
public class SignatureController {
    private final ParticipantTokenService tokenService;
    private final ContractParticipantRepository participantRepository;
    private final ContractService contractService;
    
    /**
     * 토큰 검증 API
     * 토큰의 유효성을 검증하고 참여자 정보를 반환합니다.
     */
    @GetMapping("/verify-token")
    public ResponseEntity<?> verifyToken(@RequestParam String token) {
        try {
            // 1. 토큰 검증 및 참여자 ID 추출
            log.info("토큰 검증 요청 수신: {}", token.substring(0, Math.min(10, token.length())) + "...");
            Long participantId = tokenService.validateTokenAndGetParticipantId(token);
            
            // 2. 참여자 정보 조회
            ContractParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("참여자 정보를 찾을 수 없습니다: " + participantId));
            
            // 3. 계약 정보 조회 (옵션: 계약이 활성 상태인지 확인)
            if (!participant.getContract().isActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("이 계약은 더 이상 유효하지 않습니다."));
            }
            
            // 4. 재서명 필요 여부 확인
            boolean needsResign = false;
            if (participant.getTemplateMappings() != null && !participant.getTemplateMappings().isEmpty()) {
                needsResign = participant.getTemplateMappings().stream()
                    .anyMatch(mapping -> mapping.isNeedsResign());
            }
            
            // 5. 응답 데이터 구성 (민감 정보 제외)
            Map<String, Object> response = new HashMap<>();
            response.put("isValid", true);
            response.put("participantId", participant.getId());
            response.put("name", participant.getName());
            response.put("contractId", participant.getContract().getId());
            response.put("contractTitle", participant.getContract().getTitle());
            response.put("isSigned", participant.isSigned());
            response.put("needsResign", needsResign);
            
            // 6. 추가 상태 정보 제공
            if (needsResign) {
                response.put("message", "재서명이 필요한 계약입니다.");
                response.put("resignRequestedAt", participant.getTemplateMappings().stream()
                    .filter(m -> m.isNeedsResign())
                    .findFirst()
                    .map(m -> m.getResignRequestedAt())
                    .orElse(null));
            } else if (participant.isSigned()) {
                response.put("signedAt", participant.getSignedAt());
                response.put("message", "이미 서명이 완료된 계약입니다.");
            }
            
            log.info("토큰 검증 성공 - 참여자: {}, 계약: {}, 서명상태: {}, 재서명필요: {}", 
                participant.getName(), 
                participant.getContract().getTitle(),
                participant.isSigned() ? "완료" : "미완료",
                needsResign ? "필요" : "불필요");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException | JwtException e) {
            log.warn("토큰 검증 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("유효하지 않은 링크입니다. 새로운 링크를 요청해주세요."));
        } catch (Exception e) {
            log.error("토큰 검증 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("인증 처리 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 휴대폰 번호 추가 검증 API
     * 토큰 검증 후에도 추가적인 보안을 위해 휴대폰 번호 확인
     */
    @PostMapping("/verify-phone")
    public ResponseEntity<?> verifyPhone(
        @RequestParam Long contractId,
        @RequestParam Long participantId,
        @RequestBody PhoneVerificationRequest request
    ) {
        try {
            log.info("휴대폰 번호 검증 요청: 계약ID={}, 참여자ID={}", contractId, participantId);
            
            boolean isVerified = contractService.verifyParticipantPhone(
                contractId, 
                participantId, 
                request.getPhoneLastDigits()
            );
            
            if (isVerified) {
                log.info("휴대폰 번호 검증 성공");
                return ResponseEntity.ok().build();
            } else {
                log.warn("휴대폰 번호 검증 실패");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("휴대폰 번호가 일치하지 않습니다."));
            }
        } catch (Exception e) {
            log.error("휴대폰 번호 검증 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("인증 처리 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 서명 시작 API
     * 이미 기존 ContractController에 구현된 API를 사용
     * 
     * @PostMapping("/start-signing")
     * public ResponseEntity<?> startSigning(@RequestParam Long contractId, @RequestParam Long participantId) {
     *     return new ResponseEntity<>(HttpStatus.OK);
     * }
     */
    
    // 테스트용 토큰 생성 API - 실제 운영에서는 제거해야 함
    @GetMapping("/generate-test-token")
    public ResponseEntity<String> generateTestToken(@RequestParam Long participantId) {
        String token = tokenService.generateParticipantToken(participantId);
        return ResponseEntity.ok(token);
    }
    
    /**
     * 장기 토큰으로 서명 완료된 참여자 정보를 조회합니다.
     * 이 API는 서명 완료 후 비회원이 계약 정보를 확인할 때 사용됩니다.
     */
    @GetMapping("/signed-contract")
    public ResponseEntity<?> getSignedContractInfo(@RequestParam String token) {
        try {
            // 1. 토큰 검증
            Long participantId = tokenService.validateLongTermToken(token);
            if (participantId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "유효하지 않거나 만료된 토큰입니다."));
            }
            
            // 2. 참여자 정보 조회
            ContractParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new EntityNotFoundException("참여자 정보를 찾을 수 없습니다."));
            
            Contract contract = participant.getContract();
            if (contract == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "계약 정보를 찾을 수 없습니다."));
            }
            
            // 3. 응답 데이터 구성
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            
            // 참여자 정보
            Map<String, Object> participantData = new HashMap<>();
            participantData.put("id", participant.getId());
            participantData.put("name", participant.getName());
            participantData.put("signed", participant.isSigned());
            participantData.put("signedAt", participant.getSignedAt());
            participantData.put("statusName", participant.getStatusCode() != null ? 
                participant.getStatusCode().getCodeName() : null);
            
            // 계약 정보
            Map<String, Object> contractData = new HashMap<>();
            contractData.put("id", contract.getId());
            contractData.put("title", contract.getTitle());
            contractData.put("contractNumber", contract.getContractNumber());
            contractData.put("statusName", contract.getStatusCode() != null ? 
                contract.getStatusCode().getCodeName() : null);
            contractData.put("createdAt", contract.getCreatedAt());
            // 하자보증증권 정보 추가
            contractData.put("insuranceStartDate", contract.getInsuranceStartDate());
            contractData.put("insuranceEndDate", contract.getInsuranceEndDate());
            
            // 서명된 PDF 정보
            List<Map<String, Object>> templatePdfs = new ArrayList<>();
            if (participant.getTemplateMappings() != null) {
                for (var templateMapping : participant.getTemplateMappings()) {
                    Map<String, Object> pdfInfo = new HashMap<>();
                    pdfInfo.put("templateName", templateMapping.getContractTemplateMapping().getTemplate().getTemplateName());
                    pdfInfo.put("pdfId", templateMapping.getPdfId());
                    pdfInfo.put("signedPdfId", templateMapping.getSignedPdfId());
                    templatePdfs.add(pdfInfo);
                }
            }
            
            response.put("participant", participantData);
            response.put("contract", contractData);
            response.put("templates", templatePdfs);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching signed contract info", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "계약 정보를 조회하는 중 오류가 발생했습니다."));
        }
    }
} 