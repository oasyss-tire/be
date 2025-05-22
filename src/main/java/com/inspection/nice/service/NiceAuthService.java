package com.inspection.nice.service;

import java.util.Collections;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspection.nice.util.NiceUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * NICE 본인인증 API 관련 인증 서비스
 * 기관 토큰 발급 및 관리 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NiceAuthService {

    private final NiceUtil niceUtil;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // 기관 토큰 캐시 (한 번 발급받으면 계약 기간 동안 유효하므로 캐시 유효기간을 따로 관리할 필요 없음)
    private static String cachedAccessToken = null;
    
    /**
     * 기관 토큰을 발급받습니다.
     * 이미 발급받은 토큰이 있으면 그대로 반환하고, 없으면 새로 발급받습니다.
     * NICE API 원본 응답을 그대로 반환합니다.
     *
     * @return NICE API 원본 응답 문자열
     */
    public String getAccessToken() {
        // 이미 발급받은 토큰이 있으면 해당 토큰 사용
        if (cachedAccessToken != null) {
            log.debug("이미 발급된 기관 토큰 사용");
            // 원본 응답 형식으로 반환
            return String.format(
                "{\"dataHeader\":{\"GW_RSLT_CD\":\"1200\",\"GW_RSLT_MSG\":\"오류 없음\"},\"dataBody\":{\"access_token\":\"%s\",\"token_type\":\"bearer\",\"scope\":\"default\"}}",
                cachedAccessToken
            );
        }
        
        log.info("새로운 기관 토큰 발급 시작");
        
        try {
            // API 요청 준비
            String url = niceUtil.getApiUrl() + "/digital/niceid/oauth/oauth/token";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("Authorization", niceUtil.createBasicAuthHeader());
            
            // 요청 바디 구성
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");
            body.add("scope", "default");
            
            // API 호출
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                String.class
            );
            
            // 응답 처리
            if (response.getStatusCode().is2xxSuccessful()) {
                // 응답에서 토큰 추출하여 캐시에 저장
                JsonNode root = objectMapper.readTree(response.getBody());
                String accessToken = root.path("dataBody").path("access_token").asText();
                
                // 토큰을 정적 변수에 저장 (계약 기간 동안 유효하므로 별도 만료 처리 없음)
                cachedAccessToken = accessToken;
                
                log.info("기관 토큰 발급 성공");
                
                // 원본 응답 그대로 반환
                return response.getBody();
            } else {
                log.error("기관 토큰 발급 실패: HTTP 상태 코드={}", response.getStatusCodeValue());
                throw new RuntimeException("기관 토큰 발급 실패: " + response.getStatusCodeValue());
            }
        } catch (Exception e) {
            log.error("기관 토큰 발급 중 오류 발생", e);
            throw new RuntimeException("기관 토큰 발급 오류: " + e.getMessage(), e);
        }
    }
    
    /**
     * 저장된 기관 토큰 값을 직접 반환합니다.
     * 내부 서비스에서 토큰만 필요할 때 사용합니다.
     * 
     * @return 저장된 기관 토큰 값
     */
    public String getAccessTokenValue() {
        if (cachedAccessToken == null) {
            // 토큰이 없으면 발급 받고 저장된 값 반환
            String response = getAccessToken();
            try {
                JsonNode root = objectMapper.readTree(response);
                return root.path("dataBody").path("access_token").asText();
            } catch (Exception e) {
                log.error("토큰 파싱 오류", e);
                throw new RuntimeException("토큰 파싱 오류", e);
            }
        }
        return cachedAccessToken;
    }
} 