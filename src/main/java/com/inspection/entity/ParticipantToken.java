package com.inspection.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 참여자 토큰 정보를 저장하는 엔터티
 */
@Entity
@Table(name = "participant_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipantToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "participant_id", nullable = false)
    private Long participantId;
    
    @Column(name = "token_value", length = 1000, nullable = false)
    private String tokenValue;
    
    @Column(name = "token_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TokenType tokenType;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "is_active", nullable = false)
    private boolean isActive;
    
    /**
     * 토큰 유형
     */
    public enum TokenType {
        SIGNATURE, // 서명 토큰 (24시간)
        LONG_TERM  // 장기 보관 토큰 (2년)
    }
    
    /**
     * 토큰 생성 시 기본값 설정
     */
    @jakarta.persistence.PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
    }
} 