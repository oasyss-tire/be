package com.inspection.nice.config;

import java.time.Duration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * NICE API 사용을 위한 설정 클래스
 */
@Configuration
public class NiceApiConfig {

    /**
     * NICE API 호출용 RestTemplate 빈 설정
     * 타임아웃 설정으로 API 응답이 늦어져도 애플리케이션이 멈추지 않도록 함
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * NICE API 전용 JSON 변환용 ObjectMapper 빈 설정
     * JSON 출력 시 예쁘게 들여쓰기 적용
     */
    @Bean(name = "niceApiObjectMapper")
    public ObjectMapper niceApiObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // JSON 출력 시 들여쓰기 적용
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        // Java 8 날짜/시간 타입 지원
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
} 