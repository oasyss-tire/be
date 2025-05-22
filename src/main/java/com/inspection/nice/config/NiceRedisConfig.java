package com.inspection.nice.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * NICE 본인인증 전용 Redis 설정 클래스
 * NICE 본인인증 과정에서 세션 데이터 저장에 사용
 */
@Configuration
public class NiceRedisConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;
    
    private final ObjectMapper objectMapper;
    
    public NiceRedisConfig(@Qualifier("niceApiObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * NICE 본인인증 전용 Redis 연결 팩토리 설정
     * 기본값으로 localhost:6379 사용
     */
    @Bean(name = "niceRedisConnectionFactory")
    public RedisConnectionFactory niceRedisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(redisHost);
        configuration.setPort(redisPort);
        return new LettuceConnectionFactory(configuration);
    }
    
    /**
     * NICE 본인인증 전용 Redis 템플릿 설정
     * 문자열 키와 JSON 직렬화된 값을 사용
     */
    @Bean(name = "niceRedisTemplate")
    public RedisTemplate<String, Object> niceRedisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(niceRedisConnectionFactory());
        
        // 키는 문자열 형태로 직렬화
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        
        // 값은 JSON 형태로 직렬화 (Java 8 날짜/시간 타입 지원)
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        
        // Hash 작업을 위한 직렬화 설정
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);
        
        return redisTemplate;
    }
} 