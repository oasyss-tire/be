package com.inspection.nice.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * NICE 본인인증 API 관련 유틸리티 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NiceUtil {

    @Value("${nice.client-id}")
    private String clientId;

    @Value("${nice.client-secret}")
    private String clientSecret;

    @Value("${nice.api-url}")
    private String apiUrl;

    @Value("${nice.product-id}")
    private String productId;

    /**
     * NICE API 인증을 위한 Basic Authentication 값을 생성합니다.
     * @return Base64로 인코딩된 인증 헤더 값
     */
    public String createBasicAuthHeader() {
        String auth = clientId + ":" + clientSecret;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * NICE API URL를 반환합니다.
     * @return API URL
     */
    public String getApiUrl() {
        return apiUrl;
    }

    /**
     * NICE 제품 ID를 반환합니다.
     * @return 제품 ID
     */
    public String getProductId() {
        return productId;
    }

    /**
     * NICE 클라이언트 ID를 반환합니다.
     * @return 클라이언트 ID
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * NICE 클라이언트 시크릿을 반환합니다.
     * @return 클라이언트 시크릿
     */
    public String getClientSecret() {
        return clientSecret;
    }
}