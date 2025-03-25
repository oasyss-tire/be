package com.inspection.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 참여자 재서명 요청 및 승인 이력을 저장하는 엔티티
 */
@Entity
@Table(name = "participant_resign_history")
@Data
@NoArgsConstructor
public class ParticipantResignHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "participant_id")
    private ContractParticipant participant;
    
    @Column(name = "contract_id")
    private Long contractId;
    
    @Column(name = "request_reason")
    private String requestReason;
    
    @Column(name = "requested_at")
    private LocalDateTime requestedAt;
    
    @Column(name = "requested_by")
    private String requestedBy; // 참여자 본인 또는 관리자
    
    @Column(name = "approved_by")
    private String approvedBy;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "is_processed")
    private boolean processed; // 승인 처리 여부
    
    @Column(name = "process_result")
    private String processResult; // 승인/반려 등 처리 결과
    
    /**
     * 재서명 요청 시 이력 생성을 위한 생성자
     */
    public ParticipantResignHistory(ContractParticipant participant, String requestReason) {
        this.participant = participant;
        this.contractId = participant.getContract().getId();
        this.requestReason = requestReason;
        this.requestedAt = LocalDateTime.now();
        this.requestedBy = participant.getName(); // 기본값으로 참여자 이름 설정
        this.processed = false;
    }
    
    /**
     * 재서명 승인 처리
     */
    public void approve(String approvedBy) {
        this.approvedBy = approvedBy;
        this.approvedAt = LocalDateTime.now();
        this.processed = true;
        this.processResult = "APPROVED";
    }
    
    /**
     * 재서명 요청 반려 처리
     */
    public void reject(String rejectedBy, String reason) {
        this.approvedBy = rejectedBy;
        this.approvedAt = LocalDateTime.now();
        this.processed = true;
        this.processResult = "REJECTED";
        // 반려 사유를 추가하려면 별도 필드 필요
    }
} 