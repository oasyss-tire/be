package com.inspection.nice.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspection.nice.service.NiceAuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * NICE 본인인증 API 관련 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/nice/auth")
@RequiredArgsConstructor
public class NiceAuthController {

    private final NiceAuthService niceAuthService;
    private final ObjectMapper objectMapper;

    /**
     * 기관 토큰 발급 API
     * 테스트용이므로 실제 운영 환경에서는 보안을 위해 제거 필요
     * 
     * @return NICE API 원본 응답
     * /api/nice/auth/token/token
     */
    @GetMapping(value = "/token", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAccessToken() {
        try {
            // 서비스에서 NICE API 호출 및 원본 응답 반환
            String responseBody = niceAuthService.getAccessToken();
            
            // JSON 문자열을 객체로 변환 후 다시 예쁘게 포맷팅하여 문자열로 반환
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            String formattedJson = objectMapper.writeValueAsString(jsonNode);
            
            return ResponseEntity.ok(formattedJson);
        } catch (Exception e) {
            log.error("기관 토큰 발급 컨트롤러 오류", e);
            
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
} 