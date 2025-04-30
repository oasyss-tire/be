package com.inspection.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.inspection.entity.ContractEventLog;

@Repository
public interface ContractEventLogRepository extends JpaRepository<ContractEventLog, Long> {
    
    // 계약 ID로 이력 조회 (최신순)
    List<ContractEventLog> findByContractIdOrderByEventTimeDesc(Long contractId);
    
    // 특정 계약에 대한 특정 타입의 이벤트 조회
    @Query("SELECT l FROM ContractEventLog l WHERE l.contract.id = :contractId AND l.eventTypeCode.id = :eventTypeCodeId ORDER BY l.eventTime DESC")
    List<ContractEventLog> findByContractIdAndEventTypeCodeId(@Param("contractId") Long contractId, @Param("eventTypeCodeId") String eventTypeCodeId);
    
    // 특정 참여자에 대한 이력 조회
    List<ContractEventLog> findByParticipantIdOrderByEventTimeDesc(Long participantId);
    
    // 특정 사용자가 발생시킨 이력 조회
    List<ContractEventLog> findByUserIdOrderByEventTimeDesc(Long userId);
    
    // 특정 액터 ID가 발생시킨 이력 조회
    List<ContractEventLog> findByActorIdOrderByEventTimeDesc(String actorId);
    
    // 기간별 이력 조회
    List<ContractEventLog> findByEventTimeBetweenOrderByEventTimeDesc(LocalDateTime startTime, LocalDateTime endTime);
    
    // 이벤트 타입 코드 ID로 이력 조회
    @Query("SELECT l FROM ContractEventLog l WHERE l.eventTypeCode.id = :eventTypeCodeId ORDER BY l.eventTime DESC")
    List<ContractEventLog> findByEventTypeCodeIdOrderByEventTimeDesc(@Param("eventTypeCodeId") String eventTypeCodeId);
    
    // 복합 조건 이력 조회 (페이징 처리 추가 가능)
    @Query("SELECT l FROM ContractEventLog l WHERE " +
           "(:contractId IS NULL OR l.contract.id = :contractId) AND " +
           "(:participantId IS NULL OR l.participant.id = :participantId) AND " +
           "(:eventTypeCodeId IS NULL OR l.eventTypeCode.id = :eventTypeCodeId) AND " + 
           "(:startTime IS NULL OR l.eventTime >= :startTime) AND " +
           "(:endTime IS NULL OR l.eventTime <= :endTime) " +
           "ORDER BY l.eventTime DESC")
    List<ContractEventLog> searchEventLogs(
            @Param("contractId") Long contractId,
            @Param("participantId") Long participantId,
            @Param("eventTypeCodeId") String eventTypeCodeId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
} 