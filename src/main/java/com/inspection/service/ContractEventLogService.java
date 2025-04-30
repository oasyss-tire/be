package com.inspection.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inspection.dto.ContractEventLogDTO;
import com.inspection.entity.ContractEventLog;
import com.inspection.repository.ContractEventLogRepository;
import com.inspection.repository.CodeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 계약 이벤트 로그 서비스
 * 계약 및 참여자 관련 작업 이력을 조회하고 관리하는 서비스
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ContractEventLogService {

    private final ContractEventLogRepository eventLogRepository;
    private final CodeRepository codeRepository;
    
    /**
     * 특정 계약에 대한 이벤트 로그 조회
     */
    public List<ContractEventLogDTO> getEventLogsByContractId(Long contractId) {
        List<ContractEventLog> logs = eventLogRepository.findByContractIdOrderByEventTimeDesc(contractId);
        return logs.stream()
                .map(ContractEventLogDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 특정 참여자에 대한 이벤트 로그 조회
     */
    public List<ContractEventLogDTO> getEventLogsByParticipantId(Long participantId) {
        List<ContractEventLog> logs = eventLogRepository.findByParticipantIdOrderByEventTimeDesc(participantId);
        return logs.stream()
                .map(ContractEventLogDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 특정 이벤트 타입에 대한 로그 조회
     */
    public List<ContractEventLogDTO> getEventLogsByEventTypeCodeId(String eventTypeCodeId) {
        // 추가된 레포지토리 메서드 사용
        List<ContractEventLog> logs = eventLogRepository.findByEventTypeCodeIdOrderByEventTimeDesc(eventTypeCodeId);
        return logs.stream()
                .map(ContractEventLogDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 기간별 이벤트 로그 조회
     */
    public List<ContractEventLogDTO> getEventLogsByPeriod(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        List<ContractEventLog> logs = eventLogRepository.findByEventTimeBetweenOrderByEventTimeDesc(
                startDateTime, endDateTime);
        return logs.stream()
                .map(ContractEventLogDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 복합 조건 이벤트 로그 검색
     */
    public List<ContractEventLogDTO> searchEventLogs(
            Long contractId, 
            Long participantId, 
            String eventTypeCodeId,
            LocalDateTime startDateTime, 
            LocalDateTime endDateTime) {
        
        // 추가된 레포지토리 메서드 사용하여 효율적인 쿼리 실행
        List<ContractEventLog> logs = eventLogRepository.searchEventLogs(
                contractId, participantId, eventTypeCodeId, startDateTime, endDateTime);
                
        return logs.stream()
                .map(ContractEventLogDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 이벤트 타입별 통계 조회
     */
    public Map<String, Long> getEventTypeStatistics() {
        List<ContractEventLog> allLogs = eventLogRepository.findAll();
        
        Map<String, Long> statistics = new HashMap<>();
        
        // 이벤트 타입별로 그룹화하여 개수 집계
        allLogs.forEach(log -> {
            if (log.getEventTypeCode() != null) {
                String eventTypeName = log.getEventTypeCode().getCodeName();
                statistics.put(eventTypeName, statistics.getOrDefault(eventTypeName, 0L) + 1);
            }
        });
        
        return statistics;
    }
} 