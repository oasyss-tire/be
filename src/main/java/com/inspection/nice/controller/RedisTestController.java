package com.inspection.nice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.inspection.nice.util.RedisUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Redis 연결 테스트를 위한 컨트롤러
 * 개발 완료 후 제거 필요
 */
@Slf4j
@RestController
@RequestMapping("/api/nice/test/redis")
@RequiredArgsConstructor
public class RedisTestController {

    private final RedisUtil redisUtil;
    
    /**
     * Redis에 데이터 저장 테스트
     * /api/nice/test/redis/set?key=test-key&value=test-value&expireTime=300
     */
    @GetMapping("/set")
    public ResponseEntity<Map<String, Object>> setRedisData(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(required = false, defaultValue = "60") Long expireTime
    ) {
        try {
            // 만료 시간이 있으면 해당 시간으로 설정, 없으면 그냥 저장
            if (expireTime > 0) {
                redisUtil.setDataExpire(key, value, expireTime);
            } else {
                redisUtil.setData(key, value);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Redis에 데이터가 성공적으로 저장되었습니다.");
            response.put("key", key);
            response.put("value", value);
            response.put("expireTime", expireTime);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Redis 데이터 저장 중 오류 발생", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Redis 데이터 저장 실패: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Redis에서 데이터 조회 테스트
     * /api/nice/test/redis/get?key=test-key
     */
    @GetMapping("/get")
    public ResponseEntity<Map<String, Object>> getRedisData(@RequestParam String key) {
        try {
            Object value = redisUtil.getData(key);
            
            Map<String, Object> response = new HashMap<>();
            if (value != null) {
                response.put("success", true);
                response.put("key", key);
                response.put("value", value);
            } else {
                response.put("success", false);
                response.put("message", "해당 키에 저장된 데이터가 없습니다.");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Redis 데이터 조회 중 오류 발생", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Redis 데이터 조회 실패: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Redis에서 데이터 삭제 테스트
     * /api/nice/test/redis/delete?key=test-key
     */
    @GetMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteRedisData(@RequestParam String key) {
        try {
            // 데이터 삭제 전에 먼저 데이터가 있는지 확인
            boolean exists = redisUtil.hasKey(key);
            redisUtil.deleteData(key);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("existed", exists);
            response.put("message", exists ? 
                "Redis에서 데이터가 성공적으로 삭제되었습니다." : 
                "해당 키에 데이터가 없었지만, 삭제 요청은 성공적으로 처리되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Redis 데이터 삭제 중 오류 발생", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Redis 데이터 삭제 실패: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
} 