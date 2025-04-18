package com.inspection.entity;

import java.time.LocalDateTime;

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
@Table(name = "contract_history")
@Getter
@Setter
@NoArgsConstructor
public class ContractHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;
    
    @Column(nullable = false)
    private String actionType; // CONTRACT_CREATED, SIGN_COMPLETED, CONTRACT_APPROVED, RESIGN_REQUESTED, RESIGN_APPROVED, RESIGN_COMPLETED 등
    
    @Column(nullable = false)
    private LocalDateTime createdAt; // 이벤트 발생 시간
    
    // 주체 정보 (누가)
    @Column
    private Long actorId; // 행위자 ID
    
    @Column
    private String actorType; // ADMIN, PARTICIPANT 등
    
    @Column
    private String actorName; // 행위자 이름
    
    // 대상 정보 (무엇에)
    @Column
    private Long targetId; // 대상 ID (참여자 ID, 템플릿 ID 등)
    
    @Column
    private String targetName; // 대상 이름 또는 설명
    
    // 부가 정보
    @Column(length = 1000)
    private String description; // 상세 설명
    
    @Column
    private String ipAddress; // 이벤트 발생 IP 주소

    /**
     * 이력 생성을 위한 기본 생성자
     */
    public ContractHistory(Contract contract, String actionType) {
        this.contract = contract;
        this.actionType = actionType;
        this.createdAt = LocalDateTime.now();
    }
    
    /**
     * 이력 생성을 위한 확장 생성자 (설명 포함)
     */
    public ContractHistory(Contract contract, String actionType, String description) {
        this(contract, actionType);
        this.description = description;
    }
    
    /**
     * 주체 정보 설정 (누가)
     */
    public ContractHistory setActor(Long actorId, String actorType, String actorName) {
        this.actorId = actorId;
        this.actorType = actorType;
        this.actorName = actorName;
        return this;
    }
    
    /**
     * 대상 정보 설정 (무엇에)
     */
    public ContractHistory setTarget(Long targetId, String targetName) {
        this.targetId = targetId;
        this.targetName = targetName;
        return this;
    }
    
    /**
     * IP 주소 설정
     */
    public ContractHistory setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }
    
    /**
     * 액션 타입 상수 정의
     */
    public static final class ActionType {
        public static final String CONTRACT_CREATED = "CONTRACT_CREATED";     // 계약 생성
        public static final String SIGN_COMPLETED = "SIGN_COMPLETED";         // 서명 완료
        public static final String CONTRACT_APPROVED = "CONTRACT_APPROVED";   // 계약 승인 완료
        public static final String RESIGN_REQUESTED = "RESIGN_REQUESTED";     // 재서명 요청
        public static final String RESIGN_APPROVED = "RESIGN_APPROVED";       // 재서명 요청 승인
        public static final String RESIGN_COMPLETED = "RESIGN_COMPLETED";     // 재서명 완료
        public static final String CONTRACT_REJECTED = "CONTRACT_REJECTED";   // 계약 거부
        public static final String CONTRACT_CANCELLED = "CONTRACT_CANCELLED"; // 계약 취소
    }
    
    /**
     * 행위자 타입 상수 정의
     */
    public static final class ActorType {
        public static final String ADMIN = "ADMIN";             // 관리자
        public static final String PARTICIPANT = "PARTICIPANT"; // 계약 참여자
        public static final String SYSTEM = "SYSTEM";           // 시스템
    }
} 