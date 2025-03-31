package com.inspection.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "participant_document")
@Getter @Setter
@NoArgsConstructor
public class ParticipantDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;              // 연관된 계약
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_code_id", nullable = false)
    private Code documentCode;              // 문서 코드 (공통 코드)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private ContractParticipant participant; // 연관된 참여자
    
    private boolean required = true;        // 필수 여부 (기본값 true)
    
    // 파일 관련 필드
    private String fileId;                  // 저장된 파일 경로 또는 ID
    private String originalFileName;        // 원본 파일명
    
    // 재서명 관련 필드
    private Boolean needsCorrection = false;      // 재업로드 필요 여부
    private String correctionComment;             // 재업로드 요청 코멘트
    private LocalDateTime correctionRequestedAt;  // 재업로드 요청 시간
    
    // 시간 필드
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;        // 생성 시간
    
    private LocalDateTime uploadedAt;       // 업로드 시간
} 