package com.inspection.dto;

import java.time.LocalDateTime;

import com.inspection.entity.ContractEventLog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 계약 이벤트 로그 DTO - API 응답에 사용
 */
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractEventLogDTO {
    
    private Long id;
    
    // 계약 정보
    private Long contractId;
    private String contractNumber;
    private String contractTitle;
    
    // 참여자 정보 (있는 경우)
    private Long participantId;
    private String participantName;
    
    // 이벤트 타입 정보
    private String eventTypeCodeId;
    private String eventTypeName;
    
    // 이벤트 발생 시간
    private LocalDateTime eventTime;
    
    // 액터 정보
    private String actorId;
    private String actorName;
    
    // 액터 타입 정보
    private String actorTypeCodeId;
    private String actorTypeName;
    
    // 추가 데이터
    private String additionalData;
    
    // IP 주소
    private String ipAddress;
    
    // 관련 문서 ID
    private String documentId;
    
    // 이벤트 설명
    private String description;
    
    /**
     * Entity에서 DTO로 변환
     */
    public static ContractEventLogDTO fromEntity(ContractEventLog entity) {
        if (entity == null) {
            return null;
        }
        
        ContractEventLogDTO dto = new ContractEventLogDTO();
        dto.setId(entity.getId());
        
        // 계약 정보
        if (entity.getContract() != null) {
            dto.setContractId(entity.getContract().getId());
            dto.setContractNumber(entity.getContract().getContractNumber());
            dto.setContractTitle(entity.getContract().getTitle());
        }
        
        // 참여자 정보
        if (entity.getParticipant() != null) {
            dto.setParticipantId(entity.getParticipant().getId());
            dto.setParticipantName(entity.getParticipant().getName());
        }
        
        // 이벤트 타입 정보
        if (entity.getEventTypeCode() != null) {
            dto.setEventTypeCodeId(entity.getEventTypeCode().getCodeId());
            dto.setEventTypeName(entity.getEventTypeCode().getCodeName());
        }
        
        // 이벤트 발생 시간
        dto.setEventTime(entity.getEventTime());
        
        // 액터 정보
        dto.setActorId(entity.getActorId());
        if (entity.getUser() != null) {
            dto.setActorName(entity.getUser().getUserName());
        } else {
            dto.setActorName(entity.getActorId()); // 사용자 정보가 없으면 ID를 이름으로 사용
        }
        
        // 액터 타입 정보
        if (entity.getActorTypeCode() != null) {
            dto.setActorTypeCodeId(entity.getActorTypeCode().getCodeId());
            dto.setActorTypeName(entity.getActorTypeCode().getCodeName());
        }
        
        // 기타 정보
        dto.setAdditionalData(entity.getAdditionalData());
        dto.setIpAddress(entity.getIpAddress());
        dto.setDocumentId(entity.getDocumentId());
        dto.setDescription(entity.getDescription());
        
        return dto;
    }
} 