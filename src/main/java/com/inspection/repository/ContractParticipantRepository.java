package com.inspection.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.inspection.entity.Contract;
import com.inspection.entity.ContractParticipant;

@Repository
public interface ContractParticipantRepository extends JpaRepository<ContractParticipant, Long> {
    Optional<ContractParticipant> findByContractIdAndId(Long contractId, Long participantId);
    Optional<ContractParticipant> findByPdfId(String pdfId);
    List<ContractParticipant> findByContractId(Long contractId);
    
    // User ID로 참여자 목록 조회 (직접 연결)
    @Query("SELECT p FROM ContractParticipant p WHERE p.user.id = :userId")
    List<ContractParticipant> findByUserId(@Param("userId") Long userId);
    
    // 현재 로그인한 사용자가 참여한 계약 목록 조회 (user_id로 조회)
    @Query("SELECT DISTINCT p.contract FROM ContractParticipant p JOIN p.contract c WHERE p.user.id = :userId")
    List<Contract> findContractsByUserId(@Param("userId") Long userId);
} 