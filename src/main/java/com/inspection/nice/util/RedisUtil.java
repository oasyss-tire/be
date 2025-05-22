package com.inspection.nice.util;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * Redis 데이터 저장 및 조회를 위한 유틸리티 클래스
 * NICE 본인인증 과정에서 사용되는 세션 데이터를 관리
 */
@Slf4j
@Component
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;
    
    // NICE 인증 관련 키의 접두어 (키 충돌 방지)
    private static final String NICE_KEY_PREFIX = "NICE_AUTH:";
    
    // Qualifier를 통해 특정 빈을 주입받도록 함
    public RedisUtil(@Qualifier("niceRedisTemplate") RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 데이터를 Redis에 저장
     * 
     * @param key 저장할 데이터의 키
     * @param value 저장할 데이터의 값
     */
    public void setData(String key, Object value) {
        String prefixedKey = NICE_KEY_PREFIX + key;
        redisTemplate.opsForValue().set(prefixedKey, value);
        log.debug("Redis에 데이터 저장: key={}", prefixedKey);
    }
    
    /**
     * 데이터를 Redis에 저장 (만료 시간 설정)
     * 
     * @param key 저장할 데이터의 키
     * @param value 저장할 데이터의 값
     * @param expireTime 만료 시간(초)
     */
    public void setDataExpire(String key, Object value, long expireTime) {
        String prefixedKey = NICE_KEY_PREFIX + key;
        redisTemplate.opsForValue().set(prefixedKey, value, expireTime, TimeUnit.SECONDS);
        log.debug("Redis에 데이터 저장 (만료시간 {}초): key={}", expireTime, prefixedKey);
    }
    
    /**
     * Redis에서 데이터 조회
     * 
     * @param key 조회할 데이터의 키
     * @return 저장된 데이터 (없으면 null)
     */
    public Object getData(String key) {
        String prefixedKey = NICE_KEY_PREFIX + key;
        Object value = redisTemplate.opsForValue().get(prefixedKey);
        log.debug("Redis에서 데이터 조회: key={}, 결과={}", prefixedKey, (value != null ? "성공" : "실패(데이터 없음)"));
        return value;
    }
    
    /**
     * Redis에서 데이터 삭제
     * 
     * @param key 삭제할 데이터의 키
     */
    public void deleteData(String key) {
        String prefixedKey = NICE_KEY_PREFIX + key;
        Boolean result = redisTemplate.delete(prefixedKey);
        log.debug("Redis에서 데이터 삭제: key={}, 결과={}", prefixedKey, (result != null && result ? "성공" : "실패"));
    }
    
    /**
     * 키의 만료 시간 설정
     * 
     * @param key 만료 시간을 설정할 키
     * @param expireTime 만료 시간(초)
     * @return 설정 성공 여부
     */
    public Boolean setExpire(String key, long expireTime) {
        String prefixedKey = NICE_KEY_PREFIX + key;
        Boolean result = redisTemplate.expire(prefixedKey, expireTime, TimeUnit.SECONDS);
        log.debug("Redis 키 만료 시간 설정: key={}, 시간={}초, 결과={}", 
                  prefixedKey, expireTime, (result != null && result ? "성공" : "실패"));
        return result;
    }
    
    /**
     * 키의 존재 여부 확인
     * 
     * @param key 확인할 키
     * @return 키 존재 여부
     */
    public Boolean hasKey(String key) {
        String prefixedKey = NICE_KEY_PREFIX + key;
        Boolean result = redisTemplate.hasKey(prefixedKey);
        return result != null && result;
    }
} 