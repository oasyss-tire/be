package com.inspection.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.inspection.entity.ParticipantToken;
import com.inspection.entity.ParticipantToken.TokenType;

public interface ParticipantTokenRepository extends JpaRepository<ParticipantToken, Long> {
    
    /**
     * 참여자 ID로 모든 토큰을 조회합니다.
     */
    List<ParticipantToken> findByParticipantId(Long participantId);
    
    /**
     * 참여자 ID와 토큰 타입으로 활성 상태인 토큰을 조회합니다.
     */
    List<ParticipantToken> findByParticipantIdAndTokenTypeAndIsActiveTrue(
        Long participantId, TokenType tokenType);
    
    /**
     * 토큰 값으로 토큰을 조회합니다.
     */
    Optional<ParticipantToken> findByTokenValue(String tokenValue);
    
    /**
     * 참여자 ID로 모든 활성 상태의 장기 토큰을 조회합니다.
     */
    @Query("SELECT t FROM ParticipantToken t WHERE t.participantId = :participantId " +
           "AND t.tokenType = 'LONG_TERM' AND t.isActive = true")
    List<ParticipantToken> findActiveLongTermTokensByParticipantId(@Param("participantId") Long participantId);
    
    /**
     * 참여자 ID로 장기 토큰을 모두 비활성화합니다.
     */
    @Query("UPDATE ParticipantToken t SET t.isActive = false " +
           "WHERE t.participantId = :participantId AND t.tokenType = 'LONG_TERM' AND t.isActive = true")
    void deactivateLongTermTokensByParticipantId(@Param("participantId") Long participantId);
} 