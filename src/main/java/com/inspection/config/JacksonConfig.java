package com.inspection.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Jackson 설정 클래스
 * Java 8 날짜/시간 타입(LocalDate, LocalDateTime 등)을 JSON으로 직렬화/역직렬화할 수 있도록 설정
 */
@Configuration
public class JacksonConfig {

    /**
     * Spring 애플리케이션 전체에서 사용할 ObjectMapper 빈 설정
     * @return JavaTimeModule이 등록된 ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }
} 