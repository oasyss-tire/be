package com.inspection.nice.service;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.inspection.nice.dto.NiceCertificationRequestDto;
import com.inspection.nice.dto.NiceCertificationResponseDto;
import com.inspection.nice.dto.NiceCertificationResultDto;
import com.inspection.nice.dto.NiceSessionDto;
import com.inspection.nice.util.NiceCryptoUtil;
import com.inspection.nice.util.NiceUtil;
import com.inspection.nice.util.RedisUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * NICE 본인인증 서비스
 * 암호화 토큰 요청 및 결과 처리 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NiceCertificationService {

    private final NiceUtil niceUtil;
    private final NiceAuthService niceAuthService;
    private final NiceCryptoUtil cryptoUtil;
    private final RedisUtil redisUtil;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // 세션 만료 시간 (5분)
    private static final long SESSION_EXPIRE_SECONDS = 300;
    
    /**
     * 암호화 토큰을 요청합니다.
     * 
     * @return 암호화 토큰 요청 정보와 응답 정보를 포함하는 객체
     */
    public TokenRequestInfo requestCryptoToken() {
        log.info("===== 암호화 토큰 요청 시작 =====");
        
        try {
            // 1. 액세스 토큰 가져오기
            String accessToken = niceAuthService.getAccessTokenValue();
            log.info("[1] 액세스 토큰 획득 완료: {}", accessToken);
            
            // 2. 요청 타임스탬프 및 요청번호 생성
            String reqDtim = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String reqNo = "REQ" + reqDtim + String.format("%04d", new Random().nextInt(10000));
            log.info("[2] 생성된 요청 정보: req_dtim={}, req_no={}", reqDtim, reqNo);
            
            // 3. 요청 URL 및 헤더 설정
            String url = niceUtil.getApiUrl() + "/digital/niceid/api/v1.0/common/crypto/token";
            
            // 4. 타임스탬프 생성
            long timestamp = System.currentTimeMillis() / 1000;
            
            // 5. 인증 헤더 생성 (bearer + Base64(access_token:timestamp:client_id))
            String auth = accessToken + ":" + timestamp + ":" + niceUtil.getClientId();
            String base64Auth = Base64.getEncoder().encodeToString(auth.getBytes("UTF-8"));
            String authHeader = "bearer " + base64Auth;
            log.info("[3] 인증 헤더 생성 완료 (Base64 인코딩)");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("Authorization", authHeader);
            headers.set("productID", niceUtil.getProductId());
            
            // 6. 요청 바디 생성 - NICE API 가이드에 맞게 중첩 JSON 형식으로 구성
            // dataHeader와 dataBody 구조 정확히 구성
            ObjectNode dataHeaderNode = objectMapper.createObjectNode();
            dataHeaderNode.put("CNTY_CD", "ko");
            
            // dataBody의 모든 필드를 명시적으로 문자열 값으로 설정
            ObjectNode dataBodyNode = objectMapper.createObjectNode();
            dataBodyNode.put("req_dtim", reqDtim);  // 문자열 값 설정
            dataBodyNode.put("req_no", reqNo);      // 문자열 값 설정
            dataBodyNode.put("enc_mode", "1");      // 문자열 값 설정
            
            // 전체 요청 구조 구성
            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.set("dataHeader", dataHeaderNode);
            requestNode.set("dataBody", dataBodyNode);
            
            String requestJson = objectMapper.writeValueAsString(requestNode);
            log.info("[4] 요청 JSON: {}", requestJson);
            
            // 7. API 호출
            HttpEntity<String> requestEntity = new HttpEntity<>(requestJson, headers);
            log.info("[5] NICE API 호출 시작: URL={}", url);
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                String.class
            );
            
            // 8. 응답 처리 및 반환
            log.info("[6] 암호화 토큰 요청 성공: 상태코드={}", response.getStatusCodeValue());
            log.info("[7] 응답 본문: {}", response.getBody());
            
            // 요청 정보와 응답 정보를 모두 포함하는 객체 반환
            TokenRequestInfo result = new TokenRequestInfo(reqDtim, reqNo, response.getBody());
            log.info("===== 암호화 토큰 요청 완료 =====");
            return result;
            
        } catch (Exception e) {
            log.error("===== 암호화 토큰 요청 중 오류 발생 =====", e);
            throw new RuntimeException("암호화 토큰 요청 오류: " + e.getMessage(), e);
        }
    }
    
    /**
     * NICE 표준창 호출을 위한 파라미터를 준비합니다.
     * 
     * @param requestDto 본인인증 요청 정보
     * @return NICE 표준창 호출에 필요한 파라미터
     */
    public NiceCertificationResponseDto prepareStandardWindowCall(NiceCertificationRequestDto requestDto) {
        log.info("===== NICE 표준창 호출 준비 시작 =====");
        
        try {
            // 1. 암호화 토큰 요청 (이미 토큰 발급 단계를 포함)
            log.info("[1-2] 기관 토큰 발급 및 암호화 토큰 요청");
            TokenRequestInfo tokenRequestInfo = requestCryptoToken();
            String tokenResponse = tokenRequestInfo.getTokenResponse();
            String reqDtim = tokenRequestInfo.getReqDtim();
            String reqNo = tokenRequestInfo.getReqNo();
            
            log.info("[2] 암호화 토큰 응답: {}", tokenResponse);
            
            // 2. 토큰 정보 추출
            CryptoTokenInfo tokenInfo = extractCryptoTokenInfo(tokenResponse, reqDtim, reqNo);
            String tokenVersionId = tokenInfo.getTokenVersionId();
            String tokenVal = tokenInfo.getTokenVal();
            String siteCode = tokenInfo.getSiteCode();
            
            log.info("[2-1] 암호화 토큰 주요 정보: token_version_id={}, token_val={}, site_code={}", 
                    tokenVersionId, tokenVal, siteCode);
            log.info("[2-2] 요청 정보: req_dtim={}, req_no={}", reqDtim, reqNo);
            
            // NICE 검증용 1단계: reqNo + reqDtim + tokenVal 조합 값 로그 (순서 변경)
            // trim() 제거: 정확한 원본 값 유지
            String originalString = reqDtim + reqNo + tokenVal;
            log.info("[3] original String 생성: {}", originalString);
            
            // 3-4. NICE 가이드에 맞게 수정: original String을 직접 SHA-256 해싱
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(originalString.getBytes("EUC-KR"));
            String hashBase64 = Base64.getEncoder().encodeToString(hashBytes);
            log.info("[4] SHA-256 해싱 결과 (result value): {}", hashBase64);
            
            // 대칭키, IV, HMAC 키 생성 - NICE 설명에 맞춰 추출
            String key = hashBase64.substring(0, 16);  // 앞에서부터 16바이트(문자)
            String iv = hashBase64.substring(hashBase64.length() - 16);  // 뒤에서부터 16바이트(문자)
            String hmacKey = hashBase64.substring(0, 32);  // 앞에서부터 32바이트(문자)
            log.info("[5] 생성된 키 값들:");
            log.info("   - key (대칭키, 앞 16바이트): {}", key);
            log.info("   - iv (초기화벡터, 뒤 16바이트): {}", iv);
            log.info("   - hmacKey (무결성 키, 앞 32바이트): {}", hmacKey);
            
            // 6. 요청 데이터 구성
            ObjectNode plainDataNode = objectMapper.createObjectNode();
            plainDataNode.put("requestno", reqNo);
            plainDataNode.put("returnurl", requestDto.getReturnUrl());
            plainDataNode.put("sitecode", siteCode);
            
            // 선택적 필드
            if (requestDto.getMethodType() != null && !requestDto.getMethodType().isEmpty()) {
                plainDataNode.put("methodtype", requestDto.getMethodType());
            }
            
            if (requestDto.getExtraData() != null && !requestDto.getExtraData().isEmpty()) {
                plainDataNode.put("extra", requestDto.getExtraData());
            }
            
            String plainData = objectMapper.writeValueAsString(plainDataNode);
            // NICE 검증용 4단계: 암호화 전 원본 데이터 로그
            log.info("[6] 요청 데이터 JSON (암호화 전 원본): {}", plainData);
            
            // 7. 데이터 암호화
            String encData = cryptoUtil.encrypt(plainData, key, iv);
            // NICE 검증용 5단계: 암호화된 데이터 로그
            log.info("[7] 암호화된 데이터(enc_data): {}", encData);
            
            // 8. HMAC 무결성 체크 값 생성
            String integrityValue = cryptoUtil.generateHmac(encData, hmacKey);
            // NICE 검증용 6단계: 무결성 체크 값 로그
            log.info("[8] 무결성 체크 값(integrity_value): {}", integrityValue);
            
            // 9. 세션에 저장할 정보를 Redis에 저장
            NiceSessionDto sessionDto = NiceSessionDto.builder()
                    .sessionId(reqNo)  // 요청번호를 세션ID로 사용
                    .symmetricKey(key)
                    .iv(iv)
                    .tokenVersionId(tokenVersionId)
                    .tokenVal(tokenVal)
                    .siteCode(siteCode)
                    .reqDtim(reqDtim)
                    .reqNo(reqNo)
                    .originalString(originalString)  // 원본 문자열 저장
                    .hashBase64(hashBase64)          // 해시 결과 저장
                    .hmacKey(hmacKey)                // HMAC 키 저장
                    // createdAt과 createdAtStr은 자동으로 현재 시간으로 설정됨
                    .build();
            
            // Redis에 세션 저장 (만료 시간 설정)
            redisUtil.setDataExpire(reqNo, sessionDto, SESSION_EXPIRE_SECONDS);
            
            // 10. 응답 DTO 구성
            NiceCertificationResponseDto responseDto = NiceCertificationResponseDto.builder()
                    .requestNo(reqNo)
                    .tokenVersionId(tokenVersionId)
                    .encData(encData)
                    .integrityValue(integrityValue)
                    .resultCode("Success")
                    .resultMessage("NICE 표준창 호출 준비가 완료되었습니다.")
                    .key(key)
                    .iv(iv)
                    .hmacKey(hmacKey)
                    .build();
            
            log.info("[9] 표준창 호출을 위한 값 확인:");
            log.info("   - token_version_id: {}", tokenVersionId);
            log.info("   - enc_data 길이: {}", encData.length());
            log.info("   - integrity_value 길이: {}", integrityValue.length());
            log.info("[10] 표준창 호출 준비 완료: requestNo={}", reqNo);
            log.info("===== NICE 표준창 호출 준비 완료 =====");
            
            return responseDto;
            
        } catch (Exception e) {
            log.error("NICE 표준창 호출 준비 중 오류 발생", e);
            throw new RuntimeException("NICE 표준창 호출 준비 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 본인인증 결과를 처리합니다. (콜백 처리)
     * 
     * @param tokenVersionId 토큰 버전 ID
     * @param encData 암호화된 데이터
     * @param integrityValue 무결성 체크 값
     * @param requestNo 요청 번호 (세션ID)
     * @return 복호화된 본인인증 결과 정보
     */
    public NiceCertificationResultDto processCertificationResult(
            String tokenVersionId, 
            String encData, 
            String integrityValue, 
            String requestNo) {
        
        log.info("===== 본인인증 결과 처리 시작: requestNo={} =====", requestNo);
        log.info("받은 데이터: token_version_id={}, integrity_value={}, enc_data 길이={}", 
                tokenVersionId, integrityValue, encData.length());
        
        try {
            // 1. 세션 정보 조회
            NiceSessionDto sessionDto = getSessionInfo(requestNo);
            log.info("[1] 세션 정보 조회 완료: sessionId={}", sessionDto.getSessionId());
            
            // 2. 토큰 버전 일치 여부 확인
            log.info("[2] 토큰 버전 검증: 세션={}, 수신={}", 
                    sessionDto.getTokenVersionId(), tokenVersionId);
            if (!sessionDto.getTokenVersionId().equals(tokenVersionId)) {
                log.error("토큰 버전이 일치하지 않습니다: expected={}, actual={}", 
                         sessionDto.getTokenVersionId(), tokenVersionId);
                throw new RuntimeException("토큰 버전이 일치하지 않습니다.");
            }
            
            // 3. 무결성 체크 (HMAC 검증)
            // 세션에 저장된 정확한 hmacKey 사용
            String hmacKey = sessionDto.getHmacKey();
            log.info("[3] 세션에서 가져온 무결성 검증 정보:");
            log.info("   - original_string: {}", sessionDto.getOriginalString());
            log.info("   - hash_base64: {}", sessionDto.getHashBase64());
            log.info("   - hmac_key: {}", hmacKey);
            
            String generatedIntegrity = cryptoUtil.generateHmac(encData, hmacKey);
            log.info("[3-1] 무결성 검증: 생성값={}, 수신값={}, 일치여부={}", 
                    generatedIntegrity, integrityValue, generatedIntegrity.equals(integrityValue));
            
            if (!generatedIntegrity.equals(integrityValue)) {
                log.error("무결성 검증 실패: 데이터가 변조되었을 수 있습니다.");
                throw new RuntimeException("무결성 검증 실패: 데이터가 변조되었을 수 있습니다.");
            }
            
            // 4. 암호화된 데이터 복호화
            log.info("[4] 암호화 데이터 복호화 시작: key={}, iv={}", 
                    sessionDto.getSymmetricKey(), sessionDto.getIv());
            String decryptedData = cryptoUtil.decrypt(
                encData, 
                sessionDto.getSymmetricKey(), 
                sessionDto.getIv()
            );
            log.info("[4-1] 복호화된 데이터: {}", decryptedData);
            
            // 5. JSON 파싱
            JsonNode resultNode = objectMapper.readTree(decryptedData);
            
            // 6. 요청 번호 일치 여부 확인
            String resultRequestNo = resultNode.path("requestno").asText();
            log.info("[5] 요청 번호 검증: 세션={}, 결과값={}, 일치여부={}", 
                    requestNo, resultRequestNo, requestNo.equals(resultRequestNo));
            if (!requestNo.equals(resultRequestNo)) {
                log.error("요청 번호가 일치하지 않습니다: expected={}, actual={}", 
                         requestNo, resultRequestNo);
                throw new RuntimeException("요청 번호가 일치하지 않습니다.");
            }
            
            // 7. 본인인증 결과 DTO 구성
            NiceCertificationResultDto resultDto = NiceCertificationResultDto.builder()
                    .requestNo(resultNode.path("requestno").asText())
                    .responseNo(resultNode.path("responseno").asText())
                    .authType(resultNode.path("authtype").asText())
                    .name(resultNode.path("name").asText())
                    .birthDate(resultNode.path("birthdate").asText())
                    .gender(resultNode.path("gender").asText())
                    .nationalInfo(resultNode.path("nationalinfo").asText())
                    .di(resultNode.path("di").asText())
                    .ci(resultNode.path("ci").asText())
                    .mobileNo(resultNode.path("mobileno").asText())
                    .mobileCo(resultNode.path("mobileco").asText())
                    .encTime(resultNode.path("enctime").asText())
                    .resultCode(resultNode.path("resultcode").asText())
                    .siteCode(resultNode.path("sitecode").asText())
                    .build();
            
            // 8. 사용이 완료된 세션 정보 삭제
            redisUtil.deleteData(requestNo);
            
            log.info("[6] 본인인증 결과 처리 완료: name={}, birthDate={}, ci={}", 
                    resultDto.getName(), resultDto.getBirthDate(), resultDto.getCi());
            log.info("===== 본인인증 결과 처리 완료 =====");
            return resultDto;
            
        } catch (Exception e) {
            log.error("본인인증 결과 처리 중 오류 발생", e);
            throw new RuntimeException("본인인증 결과 처리 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 암호화 토큰 정보를 기반으로 세션을 생성하고 대칭키를 생성하여 저장합니다.
     * 
     * @param tokenResponse 암호화 토큰 응답 문자열
     * @return 생성된 세션 ID (클라이언트에 전달됨)
     */
    public String createCertificationSession(String tokenResponse) {
        try {
            // 1. 암호화 토큰 정보 추출
            CryptoTokenInfo tokenInfo = extractCryptoTokenInfo(tokenResponse, null, null);
            
            // 2. 대칭키 및 IV 생성
            String symmetricKey = cryptoUtil.generateSymmetricKey();
            String iv = cryptoUtil.generateIv();
            
            // 3. 세션 ID 생성
            String sessionId = cryptoUtil.generateSessionId();
            
            // 4. 세션 정보 구성
            NiceSessionDto sessionDto = NiceSessionDto.builder()
                    .sessionId(sessionId)
                    .symmetricKey(symmetricKey)
                    .iv(iv)
                    .tokenVersionId(tokenInfo.getTokenVersionId())
                    .tokenVal(tokenInfo.getTokenVal())
                    .siteCode(tokenInfo.getSiteCode())
                    .reqDtim(tokenInfo.getReqDtim())
                    .reqNo(tokenInfo.getReqNo())
                    // createdAt과 createdAtStr은 자동으로 현재 시간으로 설정됨
                    .build();
            
            // 5. Redis에 세션 저장 (만료 시간 설정)
            redisUtil.setDataExpire(sessionId, sessionDto, SESSION_EXPIRE_SECONDS);
            log.info("본인인증 세션 생성 완료: sessionId={}", sessionId);
            
            return sessionId;
        } catch (Exception e) {
            log.error("본인인증 세션 생성 중 오류 발생", e);
            throw new RuntimeException("본인인증 세션 생성 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 세션 ID로 저장된 본인인증 세션 정보를 조회합니다.
     * 
     * @param sessionId 세션 ID
     * @return 본인인증 세션 정보
     */
    public NiceSessionDto getSessionInfo(String sessionId) {
        try {
            Object sessionData = redisUtil.getData(sessionId);
            if (sessionData == null) {
                log.warn("세션 정보가 없거나 만료됨: sessionId={}", sessionId);
                throw new RuntimeException("세션이 만료되었거나 유효하지 않습니다.");
            }
            
            // Redis에서 조회한 데이터를 NiceSessionDto로 변환
            if (sessionData instanceof NiceSessionDto) {
                return (NiceSessionDto) sessionData;
            } else {
                log.error("세션 데이터 형식 오류: {}", sessionData.getClass().getName());
                throw new RuntimeException("세션 데이터 형식이 올바르지 않습니다.");
            }
        } catch (Exception e) {
            log.error("세션 정보 조회 중 오류 발생", e);
            throw new RuntimeException("세션 정보 조회 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 암호화 토큰 응답에서 필요한 값을 추출합니다.
     * 
     * @param tokenResponse 암호화 토큰 응답 JSON 문자열
     * @param originalReqDtim 암호화 토큰 요청 시 사용한 원래 요청 시간 (null이면 응답에서 추출)
     * @param originalReqNo 암호화 토큰 요청 시 사용한 원래 요청 번호 (null이면 응답에서 추출)
     * @return 암호화 토큰 정보가 담긴 객체
     */
    public CryptoTokenInfo extractCryptoTokenInfo(String tokenResponse, String originalReqDtim, String originalReqNo) {
        try {
            JsonNode root = objectMapper.readTree(tokenResponse);
            JsonNode dataBody = root.path("dataBody");
            
            String tokenVersionId = dataBody.path("token_version_id").asText();
            String tokenVal = dataBody.path("token_val").asText();
            String siteCode = dataBody.path("site_code").asText();
            
            // 요청 정보 - 원래 요청에 사용한 값이 제공되면 그 값을 우선 사용
            String reqDtim = originalReqDtim;
            String reqNo = originalReqNo;
            
            // 원래 요청 정보가 제공되지 않았을 경우에만 응답에서 추출 또는 새로 생성
            if (reqDtim == null || reqNo == null) {
                // 먼저 응답에서 정보 추출 시도
                if (root.path("reqInfo").isObject()) {
                    if (reqDtim == null && !root.path("reqInfo").path("req_dtim").asText().isEmpty()) {
                        reqDtim = root.path("reqInfo").path("req_dtim").asText();
                    }
                    if (reqNo == null && !root.path("reqInfo").path("req_no").asText().isEmpty()) {
                        reqNo = root.path("reqInfo").path("req_no").asText();
                    }
                }
                
                // 추출 실패 시 새로 생성
                if (reqDtim == null) {
                    reqDtim = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                }
                if (reqNo == null) {
                    reqNo = "REQ" + reqDtim + String.format("%04d", new Random().nextInt(10000));
                }
            }
            
            log.debug("추출된 토큰 정보: tokenVersionId={}, reqNo={}, reqDtim={}", tokenVersionId, reqNo, reqDtim);
            return new CryptoTokenInfo(tokenVersionId, tokenVal, siteCode, reqDtim, reqNo);
        } catch (Exception e) {
            log.error("암호화 토큰 정보 추출 중 오류", e);
            throw new RuntimeException("암호화 토큰 정보 추출 오류: " + e.getMessage(), e);
        }
    }
    
    /**
     * 토큰 요청 정보를 담는 내부 클래스
     */
    public static class TokenRequestInfo {
        private final String reqDtim;
        private final String reqNo;
        private final String tokenResponse;
        
        public TokenRequestInfo(String reqDtim, String reqNo, String tokenResponse) {
            this.reqDtim = reqDtim;
            this.reqNo = reqNo;
            this.tokenResponse = tokenResponse;
        }
        
        public String getReqDtim() {
            return reqDtim;
        }
        
        public String getReqNo() {
            return reqNo;
        }
        
        public String getTokenResponse() {
            return tokenResponse;
        }
    }
    
    /**
     * 암호화 토큰 정보를 담는 내부 클래스
     */
    public static class CryptoTokenInfo {
        private final String tokenVersionId;
        private final String tokenVal;
        private final String siteCode;
        private final String reqDtim;
        private final String reqNo;
        
        public CryptoTokenInfo(String tokenVersionId, String tokenVal, String siteCode, String reqDtim, String reqNo) {
            this.tokenVersionId = tokenVersionId;
            this.tokenVal = tokenVal;
            this.siteCode = siteCode;
            this.reqDtim = reqDtim;
            this.reqNo = reqNo;
        }

        public String getTokenVersionId() {
            return tokenVersionId;
        }

        public String getTokenVal() {
            return tokenVal;
        }

        public String getSiteCode() {
            return siteCode;
        }

        public String getReqDtim() {
            return reqDtim;
        }

        public String getReqNo() {
            return reqNo;
        }
    }
} 