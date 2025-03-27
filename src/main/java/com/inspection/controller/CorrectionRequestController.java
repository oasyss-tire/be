package com.inspection.controller;

import com.inspection.dto.CorrectionFieldDTO;
import com.inspection.dto.CorrectionRequestDTO;
import com.inspection.dto.CorrectionStatusDTO;
import com.inspection.dto.ErrorResponse;
import com.inspection.entity.ContractParticipant;
import com.inspection.repository.ContractParticipantRepository;
import com.inspection.service.CorrectionRequestService;
import com.inspection.service.ParticipantTokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CorrectionRequestController {

    private final CorrectionRequestService correctionRequestService;
    private final ParticipantTokenService participantTokenService;
    private final ContractParticipantRepository participantRepository;

    /**
     * 관리자: 참여자에게 재서명 요청
     */
    @PostMapping("/contracts/{contractId}/participants/{participantId}/request-corrections")
    public ResponseEntity<CorrectionStatusDTO> requestCorrections(
            @PathVariable Long contractId,
            @PathVariable Long participantId,
            @RequestBody CorrectionRequestDTO request) {
        
        log.info("재서명 요청 - 계약 ID: {}, 참여자 ID: {}, 필드 수: {}", 
                contractId, participantId, request.getFieldIds().size());
        
        try {
            CorrectionStatusDTO status = correctionRequestService.createCorrectionRequest(participantId, request);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("재서명 요청 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * 관리자: 참여자의 재서명 상태 조회
     */
    @GetMapping("/contracts/{contractId}/participants/{participantId}/correction-status")
    public ResponseEntity<CorrectionStatusDTO> getCorrectionStatus(
            @PathVariable Long contractId,
            @PathVariable Long participantId) {
        
        log.info("재서명 상태 조회 - 계약 ID: {}, 참여자 ID: {}", contractId, participantId);
        
        try {
            CorrectionStatusDTO status = correctionRequestService.getCorrectionStatus(participantId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("재서명 상태 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * 계약자: 재서명 필요한 필드 목록 조회
     */
    @GetMapping("/participants/{participantId}/correction-fields")
    public ResponseEntity<List<CorrectionFieldDTO>> getCorrectionFields(
            @PathVariable Long participantId) {
        
        log.info("재서명 필드 조회 - 참여자 ID: {}", participantId);
        
        try {
            List<CorrectionFieldDTO> fields = correctionRequestService.getCorrectionFields(participantId);
            return ResponseEntity.ok(fields);
        } catch (Exception e) {
            log.error("재서명 필드 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * 계약자: 재서명 필드 값 저장
     */
    @PutMapping("/participants/{participantId}/correction-fields/{fieldId}")
    public ResponseEntity<CorrectionFieldDTO> updateCorrectionField(
            @PathVariable Long participantId,
            @PathVariable Long fieldId,
            @RequestBody CorrectionFieldDTO fieldDTO) {
        
        log.info("재서명 필드 값 업데이트 - 참여자 ID: {}, 필드 ID: {}", participantId, fieldId);
        
        try {
            CorrectionFieldDTO updatedField = correctionRequestService.updateCorrectionField(participantId, fieldId, fieldDTO);
            return ResponseEntity.ok(updatedField);
        } catch (Exception e) {
            log.error("재서명 필드 업데이트 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * 계약자: 재서명 완료
     */
    @PostMapping("/participants/{participantId}/complete-corrections")
    public ResponseEntity<CorrectionStatusDTO> completeCorrections(
            @PathVariable Long participantId) {
        
        log.info("재서명 완료 처리 - 참여자 ID: {}", participantId);
        
        try {
            CorrectionStatusDTO status = correctionRequestService.completeCorrections(participantId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("재서명 완료 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * 토큰 검증 및 재서명 정보 조회
     */
    @GetMapping("/contracts/correction-request/info")
    public ResponseEntity<?> getCorrectionRequestInfo(@RequestParam String token) {
        try {
            log.info("재서명 요청 정보 조회 - 토큰: {}", token);
            
            // 토큰 검증 및 참여자 정보 추출
            Long participantId = participantTokenService.validateTokenAndGetParticipantId(token);
            
            // 참여자 정보 및 계약 정보 조회
            ContractParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("참여자 정보를 찾을 수 없습니다"));
            
            // 재서명 상태 정보 조회
            CorrectionStatusDTO status = correctionRequestService.getCorrectionStatus(participantId);
            
            // 응답 데이터 구성
            Map<String, Object> response = new HashMap<>();
            response.put("participantId", participant.getId());
            response.put("contractId", participant.getContract().getId());
            response.put("contractTitle", participant.getContract().getTitle());
            response.put("participantName", participant.getName());
            response.put("participantEmail", participant.getEmail());
            
            // 재서명 상태 정보
            String correctionStatus = "PENDING";
            if (status.getStatus() != null) {
                correctionStatus = status.getStatus();
            }
            
            response.put("needsResign", !"COMPLETED".equals(correctionStatus));
            response.put("remainingFields", status.getTotalFields() - status.getCompletedFields());
            response.put("totalFields", status.getTotalFields());
            response.put("token", token);  // 토큰 다시 포함 (프론트에서 재사용)
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("재서명 요청 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("재서명 요청 정보 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
} 