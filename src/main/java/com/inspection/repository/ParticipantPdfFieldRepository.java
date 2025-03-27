package com.inspection.repository;

import com.inspection.entity.ParticipantPdfField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantPdfFieldRepository extends JpaRepository<ParticipantPdfField, Long> {
    List<ParticipantPdfField> findByPdfId(String pdfId);
    
    List<ParticipantPdfField> findByParticipantId(Long participantId);
    
    List<ParticipantPdfField> findByParticipantIdAndPdfId(Long participantId, String pdfId);
    
    Optional<ParticipantPdfField> findByPdfIdAndFieldName(String pdfId, String fieldName);
    
    List<ParticipantPdfField> findByNeedsCorrectionTrueAndParticipantId(Long participantId);
    
    @Transactional
    void deleteByPdfId(String pdfId);
} 