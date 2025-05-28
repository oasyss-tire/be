package com.inspection.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.inspection.entity.ParticipantTemplateMapping;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ParticipantTemplateMappingRepository extends JpaRepository<ParticipantTemplateMapping, Long> {
    List<ParticipantTemplateMapping> findByParticipantId(Long participantId);
    Optional<ParticipantTemplateMapping> findByPdfId(String pdfId);
    Optional<ParticipantTemplateMapping> findBySignedPdfId(String signedPdfId);
    Optional<ParticipantTemplateMapping> findByResignedPdfId(String resignedPdfId);
    Optional<ParticipantTemplateMapping> findByParticipant_IdAndContractTemplateMapping_Template_Id(Long participantId, Long templateId);
    
    // 참여자 ID로 모든 템플릿 매핑 조회
    @Query("SELECT ptm FROM ParticipantTemplateMapping ptm WHERE ptm.id IN " +
           "(SELECT tm.id FROM ContractParticipant cp JOIN cp.templateMappings tm WHERE cp.id = :participantId)")
    List<ParticipantTemplateMapping> findAllByParticipantId(@Param("participantId") Long participantId);
    
    // 계약 ID에 해당하는 특정 참여자의 모든 템플릿 매핑 조회
    @Query("SELECT ptm FROM ParticipantTemplateMapping ptm " +
           "JOIN ContractParticipant cp ON ptm.id IN (SELECT tm.id FROM cp.templateMappings tm) " +
           "WHERE cp.id = :participantId AND cp.contract.id = :contractId")
    List<ParticipantTemplateMapping> findAllByContractIdAndParticipantId(
            @Param("contractId") Long contractId, 
            @Param("participantId") Long participantId);

    @Query("SELECT ptm FROM ParticipantTemplateMapping ptm " +
           "WHERE ptm.participant.id = :participantId " +
           "AND (" +
           "  (FUNCTION('YEAR', ptm.signedAt) = :year AND FUNCTION('MONTH', ptm.signedAt) = :month AND ptm.signed = true AND ptm.resignedAt IS NULL)" +
           "  OR " +
           "  (FUNCTION('YEAR', ptm.resignedAt) = :year AND FUNCTION('MONTH', ptm.resignedAt) = :month AND ptm.signed = true)" +
           ")")
    List<ParticipantTemplateMapping> findMonthlySignedMappingsForParticipant(
            @Param("participantId") Long participantId,
            @Param("year") int year,
            @Param("month") int month
    );
} 