package com.inspection.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.inspection.entity.ParticipantDocument;

@Repository
public interface ParticipantDocumentRepository extends JpaRepository<ParticipantDocument, Long> {
    
    // 계약 ID로 모든 문서 조회
    List<ParticipantDocument> findByContractId(Long contractId);
    
    // 참여자 ID로 모든 문서 조회
    List<ParticipantDocument> findByParticipantId(Long participantId);
    
    // 계약 ID와 참여자 ID로 모든 문서 조회
    List<ParticipantDocument> findByContractIdAndParticipantId(Long contractId, Long participantId);
    
    // 계약 ID와 문서 코드 ID로 모든 문서 조회
    List<ParticipantDocument> findByContractIdAndDocumentCodeCodeId(Long contractId, String documentCodeId);
    
    // 계약, 참여자, 문서 코드로 문서 조회 (단일 결과)
    ParticipantDocument findByContractIdAndParticipantIdAndDocumentCodeCodeId(
            Long contractId, Long participantId, String documentCodeId);
    
    // 계약에 필요한 모든 문서 유형 조회 (중복 제거)
    @Query("SELECT DISTINCT pd.documentCode FROM ParticipantDocument pd WHERE pd.contract.id = :contractId")
    List<Object[]> findDistinctDocumentCodesByContractId(@Param("contractId") Long contractId);
    
    // 계약의 각 문서별 업로드 완료 여부 통계
    @Query("SELECT pd.documentCode.codeId, pd.documentCode.codeName, " +
           "COUNT(pd) as total, SUM(CASE WHEN pd.fileId IS NOT NULL THEN 1 ELSE 0 END) as uploaded " +
           "FROM ParticipantDocument pd WHERE pd.contract.id = :contractId " +
           "GROUP BY pd.documentCode.codeId, pd.documentCode.codeName")
    List<Object[]> getDocumentUploadStatsByContractId(@Param("contractId") Long contractId);
} 