package com.inspection.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 토큰 관리 서비스
 */
@Service
@Slf4j
public class TokenService {
    
    // 무효화된 토큰을 저장하는 Set (메모리에 저장)
    private final Set<String> invalidatedTokens = ConcurrentHashMap.newKeySet();
    
    /**
     * 토큰을 무효화합니다.
     * 
     * @param token 무효화할 토큰
     */
    public void invalidateToken(String token) {
        invalidatedTokens.add(token);
        log.info("토큰이 무효화되었습니다.");
    }
    
    /**
     * 토큰이 무효화되었는지 확인합니다.
     * 
     * @param token 확인할 토큰
     * @return 무효화되었으면 true, 아니면 false
     */
    public boolean isTokenInvalidated(String token) {
        return invalidatedTokens.contains(token);
    }
} 