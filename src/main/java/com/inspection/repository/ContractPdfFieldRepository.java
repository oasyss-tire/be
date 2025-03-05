package com.inspection.repository;

import com.inspection.entity.ContractPdfField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContractPdfFieldRepository extends JpaRepository<ContractPdfField, Long> {
    List<ContractPdfField> findByPdfId(String pdfId);
    void deleteByPdfId(String pdfId);
    Optional<ContractPdfField> findByPdfIdAndFieldName(String pdfId, String fieldName);
} 