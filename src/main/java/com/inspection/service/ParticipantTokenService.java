package com.inspection.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inspection.entity.ParticipantToken;
import com.inspection.entity.ParticipantToken.TokenType;
import com.inspection.repository.ParticipantTokenRepository;
import com.inspection.security.JwtTokenProvider;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 계약 참여자를 위한 토큰 생성 및 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipantTokenService {
    private final JwtTokenProvider jwtTokenProvider;
    private final ParticipantTokenRepository tokenRepository;
    
    /**
     * 참여자 ID 기반으로 서명용 토큰을 생성합니다.
     * 기본 유효기간은 24시간입니다.
     * 
     * @param participantId 참여자 ID
     * @return 생성된 토큰
     */
    @Transactional
    public String generateParticipantToken(Long participantId) {
        // 24시간 유효한 토큰 생성 (밀리초 단위)
        long validityInMilliseconds = 24 * 60 * 60 * 1000;
        
        String token = jwtTokenProvider.createParticipantToken(participantId, validityInMilliseconds);
        log.info("참여자 ID {}에 대한 토큰이 생성되었습니다.", participantId);
        
        // 토큰 정보를 DB에 저장
        saveTokenToDatabase(participantId, token, TokenType.SIGNATURE, validityInMilliseconds);
        
        return token;
    }
    
    /**
     * 토큰의 유효성을 검증하고 참여자 ID를 추출합니다.
     * 
     * @param token 검증할 토큰
     * @return 참여자 ID
     * @throws IllegalArgumentException 토큰이 없거나 빈 문자열인 경우
     * @throws JwtException 토큰이 유효하지 않은 경우
     */
    public Long validateTokenAndGetParticipantId(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("토큰이 제공되지 않았습니다.");
        }
        
        // DB에서 토큰의 활성 상태 확인
        if (!isTokenActive(token)) {
            throw new JwtException("비활성화된 토큰입니다.");
        }
        
        // 토큰 검증
        if (!jwtTokenProvider.validateParticipantToken(token)) {
            throw new JwtException("유효하지 않은 토큰입니다.");
        }
        
        // 참여자 ID 추출
        Long participantId = jwtTokenProvider.getParticipantIdFromToken(token);
        log.info("토큰에서 참여자 ID {}를 추출했습니다.", participantId);
        
        return participantId;
    }

    /**
     * 서명 완료된 참여자를 위한 장기 보관용 토큰을 생성합니다.
     * 이 토큰은 2년간 유효하며, 서명 완료 페이지 접근에 사용됩니다.
     *
     * @param participantId 참여자 ID
     * @return 생성된 토큰 문자열
     */
    @Transactional
    public String generateLongTermToken(Long participantId) {
        // 2년 유효기간 설정 (24 * 365 * 2 = 17,520 시간)
        long expirationHours = 17520L;
        // 시간 단위를 밀리초로 변환: 시간 * 60분 * 60초 * 1000밀리초
        long validityInMilliseconds = expirationHours * 60 * 60 * 1000;
        log.info("장기 보관용 토큰 생성: 참여자 ID {}, 유효기간 {} 시간 ({} 밀리초)", 
                participantId, expirationHours, validityInMilliseconds);
                
        String token = jwtTokenProvider.createParticipantToken(participantId, validityInMilliseconds);
        
        // 토큰 정보를 DB에 저장
        saveTokenToDatabase(participantId, token, TokenType.LONG_TERM, validityInMilliseconds);
        
        return token;
    }

    /**
     * 장기 보관용 토큰을 검증합니다.
     *
     * @param token 검증할 토큰
     * @return 참여자 ID 또는 토큰이 유효하지 않은 경우 null
     */
    public Long validateLongTermToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        
        try {
            // DB에서 토큰의 활성 상태 확인
            if (!isTokenActive(token)) {
                log.warn("비활성화된 장기 토큰입니다.");
                return null;
            }
            
            // 토큰 검증
            if (!jwtTokenProvider.validateParticipantToken(token)) {
                return null;
            }
            
            // 참여자 ID 추출
            return jwtTokenProvider.getParticipantIdFromToken(token);
        } catch (Exception e) {
            log.warn("장기 보관용 토큰 검증 실패: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 참여자의 모든 장기 토큰을 비활성화하고 새 장기 토큰을 생성합니다.
     * 재서명 완료 시 호출되는 메서드입니다.
     * 
     * @param participantId 참여자 ID
     * @return 새로 생성된 장기 토큰
     */
    @Transactional
    public String deactivateAndRegenerateLongTermToken(Long participantId) {
        log.info("참여자 ID {}의 모든 장기 토큰을 비활성화하고 새 토큰을 생성합니다.", participantId);
        
        // 1. 기존 장기 토큰 모두 비활성화
        List<ParticipantToken> existingTokens = tokenRepository.findActiveLongTermTokensByParticipantId(participantId);
        for (ParticipantToken token : existingTokens) {
            token.setActive(false);
            tokenRepository.save(token);
            log.info("장기 토큰 비활성화: 토큰ID={}, 참여자ID={}", token.getId(), participantId);
        }
        
        // 2. 새 장기 토큰 생성
        return generateLongTermToken(participantId);
    }
    
    /**
     * 토큰이 활성 상태인지 확인합니다.
     * 
     * @param token 확인할 토큰
     * @return 활성 상태면 true, 비활성 상태면 false
     */
    private boolean isTokenActive(String token) {
        return tokenRepository.findByTokenValue(token)
                .map(ParticipantToken::isActive)
                .orElse(false);
    }
    
    /**
     * 토큰 정보를 데이터베이스에 저장합니다.
     * 
     * @param participantId 참여자 ID
     * @param token 토큰 값
     * @param tokenType 토큰 타입(일반/장기)
     * @param validityInMilliseconds 유효기간(밀리초)
     */
    private void saveTokenToDatabase(Long participantId, String token, TokenType tokenType, long validityInMilliseconds) {
        // 만료일 계산
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(validityInMilliseconds / 1000);
        
        // 토큰 엔터티 생성 및 저장
        ParticipantToken tokenEntity = ParticipantToken.builder()
                .participantId(participantId)
                .tokenValue(token)
                .tokenType(tokenType)
                .expiresAt(expiresAt)
                .isActive(true)
                .build();
                
        tokenRepository.save(tokenEntity);
        log.info("토큰이 데이터베이스에 저장되었습니다: 참여자ID={}, 토큰타입={}, 만료일={}", 
                 participantId, tokenType, expiresAt);
    }
} 