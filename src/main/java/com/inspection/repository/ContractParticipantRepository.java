package com.inspection.repository;

import com.inspection.entity.ContractParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ContractParticipantRepository extends JpaRepository<ContractParticipant, Long> {
    Optional<ContractParticipant> findByContractIdAndId(Long contractId, Long participantId);
    Optional<ContractParticipant> findByPdfId(String pdfId);
} 