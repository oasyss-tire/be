package com.inspection.nice.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.inspection.entity.Contract;
import com.inspection.entity.ContractParticipant;
import com.inspection.entity.NiceAuthenticationLog;
import com.inspection.nice.dto.NiceCertificationRequestDto;
import com.inspection.nice.dto.NiceCertificationResponseDto;
import com.inspection.nice.dto.NiceCertificationResultDto;
import com.inspection.nice.dto.NiceSessionDto;
import com.inspection.nice.service.NiceCertificationService;
import com.inspection.nice.service.NiceCertificationService.TokenRequestInfo;
import com.inspection.repository.ContractParticipantRepository;
import com.inspection.repository.ContractRepository;
import com.inspection.service.NiceAuthenticationLogService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * NICE 본인인증 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/nice/certification")
@RequiredArgsConstructor
public class NiceCertificationController {

    private final NiceCertificationService certificationService;
    private final ObjectMapper objectMapper;
    private final NiceAuthenticationLogService authLogService;
    private final ContractRepository contractRepository;
    private final ContractParticipantRepository participantRepository;
    
    /**
     * 암호화 토큰 요청 API
     * NICE 본인인증 시작 시 필요한 암호화 토큰을 요청합니다.
     * 
     * @return NICE API 암호화 토큰 응답
     * /api/nice/certification/request
     */
    @PostMapping(value = "/request", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> requestCryptoToken() {
        try {
            // 암호화 토큰 요청 - 타입 변경됨
            TokenRequestInfo tokenRequestInfo = certificationService.requestCryptoToken();
            String responseBody = tokenRequestInfo.getTokenResponse();
            
            // 응답에 추가 정보 포함 - 요청 정보를 응답에 포함
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            ((ObjectNode) jsonNode).put("req_dtim", tokenRequestInfo.getReqDtim());
            ((ObjectNode) jsonNode).put("req_no", tokenRequestInfo.getReqNo());
            
            String formattedJson = objectMapper.writeValueAsString(jsonNode);
            
            return ResponseEntity.ok(formattedJson);
        } catch (Exception e) {
            log.error("암호화 토큰 요청 컨트롤러 오류", e);
            
            try {
                // 에러 응답도 예쁘게 포맷팅
                JsonNode errorNode = objectMapper.createObjectNode()
                    .put("success", false)
                    .put("message", e.getMessage());
                
                String formattedError = objectMapper.writeValueAsString(errorNode);
                
                return ResponseEntity.internalServerError().body(formattedError);
            } catch (Exception jsonEx) {
                // JSON 처리 중 오류 발생 시 단순 문자열 반환
                return ResponseEntity.internalServerError()
                    .body("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            }
        }
    }
    
    /**
     * NICE 표준창 호출을 위한 파라미터 준비 API
     * 클라이언트에서 NICE 표준창을 호출하기 위한 파라미터를 생성합니다.
     * 
     * @param requestDto 본인인증 요청 정보
     * @return NICE 표준창 호출에 필요한 파라미터
     * /api/nice/certification/window
     */
    @PostMapping(value = "/window", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> prepareStandardWindowCall(@RequestBody NiceCertificationRequestDto requestDto) {
        try {
            // NICE 표준창 호출 준비
            NiceCertificationResponseDto responseDto = certificationService.prepareStandardWindowCall(requestDto);
            
            // 응답 구성
            ObjectNode responseNode = objectMapper.createObjectNode();
            responseNode.put("success", true);
            responseNode.put("requestNo", responseDto.getRequestNo());
            responseNode.put("tokenVersionId", responseDto.getTokenVersionId());
            responseNode.put("encData", responseDto.getEncData());
            responseNode.put("integrityValue", responseDto.getIntegrityValue());
            responseNode.put("message", responseDto.getResultMessage());
            
            String response = objectMapper.writeValueAsString(responseNode);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("NICE 표준창 호출 준비 오류", e);
            
            try {
                ObjectNode errorNode = objectMapper.createObjectNode()
                    .put("success", false)
                    .put("message", e.getMessage());
                
                String formattedError = objectMapper.writeValueAsString(errorNode);
                return ResponseEntity.internalServerError().body(formattedError);
            } catch (Exception jsonEx) {
                return ResponseEntity.internalServerError()
                    .body("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            }
        }
    }
    
    /**
     * 본인인증 결과 처리 API (콜백)
     * NICE 본인인증 완료 후 콜백으로 전달받은 암호화된 사용자 정보를 처리합니다.
     * 
     * @param tokenVersionId 토큰 버전 ID
     * @param encData 암호화된 데이터
     * @param integrityValue 무결성 체크 값
     * @param requestNo 요청 번호
     * @return 본인인증 결과
     */
    @PostMapping(value = "/callback", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> processCertificationResult(
            @RequestParam("token_version_id") String tokenVersionId,
            @RequestParam("enc_data") String encData,
            @RequestParam("integrity_value") String integrityValue,
            @RequestParam("request_no") String requestNo) {
        
        try {
            // 본인인증 결과 처리
            NiceCertificationResultDto resultDto = certificationService.processCertificationResult(
                tokenVersionId, encData, integrityValue, requestNo);
            
            // 응답 구성
            ObjectNode responseNode = objectMapper.createObjectNode();
            responseNode.put("success", true);
            responseNode.put("requestNo", resultDto.getRequestNo());
            responseNode.put("name", resultDto.getName());
            responseNode.put("birthDate", resultDto.getBirthDate());
            responseNode.put("gender", resultDto.getGender());
            responseNode.put("nationalInfo", resultDto.getNationalInfo());
            responseNode.put("mobileNo", resultDto.getMobileNo());
            responseNode.put("mobileCo", resultDto.getMobileCo());
            responseNode.put("di", resultDto.getDi());
            responseNode.put("ci", resultDto.getCi());
            
            String response = objectMapper.writeValueAsString(responseNode);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("본인인증 결과 처리 오류", e);
            
            try {
                ObjectNode errorNode = objectMapper.createObjectNode()
                    .put("success", false)
                    .put("message", e.getMessage());
                
                String formattedError = objectMapper.writeValueAsString(errorNode);
                return ResponseEntity.internalServerError().body(formattedError);
            } catch (Exception jsonEx) {
                return ResponseEntity.internalServerError()
                    .body("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            }
        }
    }
    
    /**
     * GET 방식 콜백 처리용 API
     * 일부 브라우저에서는 POST가 아닌 GET으로 콜백을 받을 수 있음
     */
    @GetMapping(value = "/callback", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> processCertificationResultGet(
            @RequestParam("token_version_id") String tokenVersionId,
            @RequestParam("enc_data") String encData,
            @RequestParam("integrity_value") String integrityValue,
            @RequestParam("request_no") String requestNo) {
        
        // POST 메서드와 동일한 처리 로직 호출
        return processCertificationResult(tokenVersionId, encData, integrityValue, requestNo);
    }
    
    /**
     * 세션 정보 조회 API (테스트용)
     * 개발 환경에서만 사용하고 운영 환경에서는 제거해야 함
     * 
     * @param sessionId 세션 ID
     * @return 세션 정보
     */
    @GetMapping(value = "/session/{sessionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getSessionInfo(@PathVariable String sessionId) {
        try {
            // 세션 정보 조회
            NiceSessionDto sessionDto = certificationService.getSessionInfo(sessionId);
            
            // 민감 정보 제외하고 응답 구성 (실제 운영에서는 이 API 자체를 제공하지 않는 것이 좋음)
            ObjectNode responseNode = objectMapper.createObjectNode();
            responseNode.put("success", true);
            responseNode.put("sessionId", sessionDto.getSessionId());
            responseNode.put("tokenVersionId", sessionDto.getTokenVersionId());
            responseNode.put("siteCode", sessionDto.getSiteCode());
            responseNode.put("createdAt", sessionDto.getCreatedAt().toString());
            
            // 추가 디버그 정보 포함 (개발 환경에서만)
            responseNode.put("reqDtim", sessionDto.getReqDtim());
            responseNode.put("reqNo", sessionDto.getReqNo());
            responseNode.put("originalString", sessionDto.getOriginalString());
            responseNode.put("hmacKey", sessionDto.getHmacKey());
            
            String response = objectMapper.writeValueAsString(responseNode);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("세션 정보 조회 오류", e);
            
            try {
                ObjectNode errorNode = objectMapper.createObjectNode()
                    .put("success", false)
                    .put("message", e.getMessage());
                
                String formattedError = objectMapper.writeValueAsString(errorNode);
                return ResponseEntity.internalServerError().body(formattedError);
            } catch (Exception jsonEx) {
                return ResponseEntity.internalServerError()
                    .body("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    /**
     * 계약 참여자 본인인증 결과 처리 API (콜백)
     * 특정 계약의 참여자가 NICE 본인인증 완료 후 호출되는 콜백 API
     * 인증 결과를 처리하고 로그를 저장합니다.
     * 
     * @param contractId 계약 ID
     * @param participantId 계약 참여자 ID
     * @param tokenVersionId 토큰 버전 ID
     * @param encData 암호화된 데이터
     * @param integrityValue 무결성 체크 값
     * @param requestNo 요청 번호
     * @param authPurpose 인증 목적 (선택, 기본값: CONTRACT_SIGN)
     * @param request HTTP 요청 (IP, User-Agent 추출용)
     * @return 본인인증 결과
     */
    @PostMapping(value = "/callback/contract/{contractId}/participant/{participantId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> processContractParticipantAuth(
            @PathVariable Long contractId,
            @PathVariable Long participantId,
            @RequestParam("token_version_id") String tokenVersionId,
            @RequestParam("enc_data") String encData,
            @RequestParam("integrity_value") String integrityValue,
            @RequestParam("request_no") String requestNo,
            @RequestParam(value = "auth_purpose", defaultValue = "CONTRACT_SIGN") String authPurpose,
            HttpServletRequest request) {
        
        log.info("계약 참여자 본인인증 결과 처리 시작: contractId={}, participantId={}, requestNo={}",
                contractId, participantId, requestNo);
        
        try {
            // 1. 계약과 참여자 정보 조회
            Contract contract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new EntityNotFoundException("계약을 찾을 수 없습니다: " + contractId));
            
            ContractParticipant participant = participantRepository.findById(participantId)
                    .orElseThrow(() -> new EntityNotFoundException("참여자를 찾을 수 없습니다: " + participantId));
            
            // 2. 계약-참여자 관계 검증
            if (!participant.getContract().getId().equals(contractId)) {
                log.error("계약과 참여자가 일치하지 않습니다: contractId={}, participantId={}",
                        contractId, participantId);
                throw new RuntimeException("계약과 참여자가 일치하지 않습니다.");
            }
            
            // 3. NICE 본인인증 결과 처리
            NiceCertificationResultDto resultDto = certificationService.processCertificationResult(
                tokenVersionId, encData, integrityValue, requestNo);
            
            // 4. 클라이언트 정보 추출
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            // 5. 인증 로그 저장
            NiceAuthenticationLog authLog = authLogService.saveAuthenticationLog(
                    contract,
                    participant,
                    resultDto,
                    ipAddress,
                    userAgent,
                    authPurpose
            );
            
            // 6. 응답 구성
            ObjectNode responseNode = objectMapper.createObjectNode();
            responseNode.put("success", true);
            responseNode.put("contractId", contractId);
            responseNode.put("participantId", participantId);
            responseNode.put("requestNo", resultDto.getRequestNo());
            responseNode.put("responseNo", resultDto.getResponseNo());
            responseNode.put("authSuccess", authLog.isAuthenticationSuccess());
            responseNode.put("authType", resultDto.getAuthType());
            responseNode.put("encTime", resultDto.getEncTime());
            responseNode.put("ci", resultDto.getCi());
            responseNode.put("nationalInfo", resultDto.getNationalInfo());
            responseNode.put("logId", authLog.getId());
            responseNode.put("message", authLog.isAuthenticationSuccess() ? 
                    "본인인증이 성공적으로 완료되었습니다." : "본인인증에 실패했습니다.");
            
            // 개발환경에서만 개인정보 포함 (실제 운영에서는 제거)
            if (authLog.isAuthenticationSuccess()) {
                ObjectNode personalInfo = objectMapper.createObjectNode();
                personalInfo.put("name", resultDto.getName());
                personalInfo.put("birthDate", resultDto.getBirthDate());
                personalInfo.put("gender", resultDto.getGender());
                personalInfo.put("mobileNo", resultDto.getMobileNo());
                personalInfo.put("mobileCo", resultDto.getMobileCo());
                personalInfo.put("di", resultDto.getDi());
                personalInfo.put("nationalInfo", resultDto.getNationalInfo());
                responseNode.set("personalInfo", personalInfo);
            }
            
            String response = objectMapper.writeValueAsString(responseNode);
            log.info("계약 참여자 본인인증 결과 처리 완료: contractId={}, participantId={}, success={}",
                    contractId, participantId, authLog.isAuthenticationSuccess());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("계약 참여자 본인인증 결과 처리 오류: contractId={}, participantId={}",
                    contractId, participantId, e);
            
            try {
                ObjectNode errorNode = objectMapper.createObjectNode()
                    .put("success", false)
                    .put("contractId", contractId)
                    .put("participantId", participantId)
                    .put("requestNo", requestNo)
                    .put("message", e.getMessage());
                
                String formattedError = objectMapper.writeValueAsString(errorNode);
                return ResponseEntity.internalServerError().body(formattedError);
            } catch (Exception jsonEx) {
                return ResponseEntity.internalServerError()
                    .body("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    /**
     * 클라이언트 IP 주소를 추출합니다.
     * 프록시나 로드밸런서를 고려하여 실제 클라이언트 IP를 추출합니다.
     * 
     * @param request HTTP 요청
     * @return 클라이언트 IP 주소
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For 헤더는 여러 IP가 콤마로 구분될 수 있음
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        return request.getRemoteAddr();
    }
} 