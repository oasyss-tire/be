package com.inspection.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.inspection.entity.Contract;
import com.inspection.entity.ContractParticipant;
import com.inspection.entity.NiceAuthenticationLog;

@Repository
public interface NiceAuthenticationLogRepository extends JpaRepository<NiceAuthenticationLog, Long> {
    
    /**
     * 계약별 인증 이력 조회 (최신순)
     */
    List<NiceAuthenticationLog> findByContractOrderByCreatedAtDesc(Contract contract);
    
    /**
     * 계약 참여자별 인증 이력 조회 (최신순)
     */
    List<NiceAuthenticationLog> findByParticipantOrderByCreatedAtDesc(ContractParticipant participant);
    
    /**
     * 계약 참여자의 마지막 성공 인증 조회
     */
    @Query("SELECT n FROM NiceAuthenticationLog n WHERE n.participant = :participant AND n.resultCode = '0000' ORDER BY n.authenticatedAt DESC LIMIT 1")
    Optional<NiceAuthenticationLog> findLastSuccessfulAuthByParticipant(@Param("participant") ContractParticipant participant);
    
    /**
     * 계약 참여자의 최초 성공 인증 조회
     */
    @Query("SELECT n FROM NiceAuthenticationLog n WHERE n.participant = :participant AND n.resultCode = '0000' ORDER BY n.authenticatedAt ASC LIMIT 1")
    Optional<NiceAuthenticationLog> findFirstSuccessfulAuthByParticipant(@Param("participant") ContractParticipant participant);
    
    /**
     * 계약 참여자의 총 성공 인증 횟수
     */
    @Query("SELECT COUNT(n) FROM NiceAuthenticationLog n WHERE n.participant = :participant AND n.resultCode = '0000'")
    long countSuccessfulAuthByParticipant(@Param("participant") ContractParticipant participant);
    
    /**
     * CI로 인증 이력 조회 (동일인 확인용)
     */
    List<NiceAuthenticationLog> findByCiAndResultCodeOrderByCreatedAtDesc(String ci, String resultCode);
    
    /**
     * 기간별 인증 이력 조회
     */
    @Query("SELECT n FROM NiceAuthenticationLog n WHERE n.authenticatedAt BETWEEN :startDate AND :endDate ORDER BY n.authenticatedAt DESC")
    Page<NiceAuthenticationLog> findByAuthenticatedAtBetween(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate, 
        Pageable pageable);
    
    /**
     * 계약별 성공 인증 횟수
     */
    @Query("SELECT COUNT(n) FROM NiceAuthenticationLog n WHERE n.contract = :contract AND n.resultCode = '0000'")
    long countSuccessfulAuthByContract(@Param("contract") Contract contract);
    
    /**
     * 특정 계약의 특정 참여자가 최근 N시간 내에 인증했는지 확인
     */
    @Query("SELECT COUNT(n) > 0 FROM NiceAuthenticationLog n WHERE n.contract = :contract AND n.participant = :participant AND n.resultCode = '0000' AND n.authenticatedAt > :cutoffTime")
    boolean hasRecentSuccessfulAuth(
        @Param("contract") Contract contract,
        @Param("participant") ContractParticipant participant,
        @Param("cutoffTime") LocalDateTime cutoffTime);
} 