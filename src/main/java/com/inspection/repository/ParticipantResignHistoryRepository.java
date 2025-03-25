package com.inspection.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.inspection.entity.ParticipantResignHistory;

@Repository
public interface ParticipantResignHistoryRepository extends JpaRepository<ParticipantResignHistory, Long> {
    
    /**
     * 참여자 ID로 재서명 이력 조회
     */
    List<ParticipantResignHistory> findByParticipantIdOrderByRequestedAtDesc(Long participantId);
    
    /**
     * 계약 ID로 재서명 이력 조회
     */
    List<ParticipantResignHistory> findByContractIdOrderByRequestedAtDesc(Long contractId);
    
    /**
     * 미처리된(승인 대기 중인) 재서명 요청 조회
     */
    List<ParticipantResignHistory> findByProcessedFalseOrderByRequestedAtAsc();
    
    /**
     * 특정 참여자의 마지막 재서명 이력 조회
     */
    ParticipantResignHistory findTopByParticipantIdOrderByRequestedAtDesc(Long participantId);
} 