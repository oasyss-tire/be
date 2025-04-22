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
@Table(name = "contract_event_log")
@Getter @Setter
@NoArgsConstructor
public class ContractEventLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;
    
    // 참여자 이력인 경우 해당 참여자 정보 (null 가능)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    private ContractParticipant participant;
    
    // 이벤트 타입 코드 (계약생성, 서명완료, 계약승인, 계약반려, 재서명요청 등)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_type_code_id")
    private Code eventTypeCode;
    
    // 이벤트 발생 시간
    private LocalDateTime eventTime;
    
    // 이벤트 발생자 - 시스템 사용자 (관리자) 정보
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    // 이벤트 발생자 ID 또는 이름 (사용자 정보가 없는 경우)
    private String actorId;
    
    // 이벤트 발생자 타입 (관리자, 참여자, 시스템 등)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_type_code_id")
    private Code actorTypeCode;
    
    // 추가 데이터 (JSON 형태로 저장)
    @Column(columnDefinition = "TEXT")
    private String additionalData;
    
    // IP 주소 (보안 감사 목적)
    private String ipAddress;
    
    // 관련 문서 ID (PDF ID 등)
    private String documentId;
    
    // 이벤트 설명 (필요한 경우)
    @Column(columnDefinition = "TEXT")
    private String description;
    
    // 생성자 메서드 - 시스템 사용자(관리자)가 있는 경우
    public static ContractEventLog create(
            Contract contract, 
            ContractParticipant participant, 
            Code eventTypeCode, 
            User user,
            Code actorTypeCode,
            String ipAddress,
            String documentId,
            String additionalData,
            String description) {
        
        ContractEventLog log = new ContractEventLog();
        log.setContract(contract);
        log.setParticipant(participant);
        log.setEventTypeCode(eventTypeCode);
        log.setEventTime(LocalDateTime.now());
        log.setUser(user);
        log.setActorId(user != null ? user.getUserId() : null);
        log.setActorTypeCode(actorTypeCode);
        log.setIpAddress(ipAddress);
        log.setDocumentId(documentId);
        log.setAdditionalData(additionalData);
        log.setDescription(description);
        
        return log;
    }
    
    // 생성자 메서드 - 시스템 사용자(관리자) 정보가 없는 경우
    public static ContractEventLog create(
            Contract contract, 
            ContractParticipant participant, 
            Code eventTypeCode, 
            String actorId, 
            Code actorTypeCode,
            String ipAddress,
            String documentId,
            String additionalData,
            String description) {
        
        ContractEventLog log = new ContractEventLog();
        log.setContract(contract);
        log.setParticipant(participant);
        log.setEventTypeCode(eventTypeCode);
        log.setEventTime(LocalDateTime.now());
        log.setUser(null);
        log.setActorId(actorId);
        log.setActorTypeCode(actorTypeCode);
        log.setIpAddress(ipAddress);
        log.setDocumentId(documentId);
        log.setAdditionalData(additionalData);
        log.setDescription(description);
        
        return log;
    }
    
    // 간단한 생성자 메서드 - 최소 필수 정보만 사용 (시스템에 의한 이벤트인 경우)
    public static ContractEventLog createSimple(
            Contract contract, 
            Code eventTypeCode, 
            String description) {
        
        ContractEventLog log = new ContractEventLog();
        log.setContract(contract);
        log.setEventTypeCode(eventTypeCode);
        log.setEventTime(LocalDateTime.now());
        log.setActorId("SYSTEM");
        log.setDescription(description);
        
        return log;
    }
} 