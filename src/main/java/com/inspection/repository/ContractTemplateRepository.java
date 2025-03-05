package com.inspection.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.inspection.entity.ContractTemplate;
import java.util.List;

@Repository
public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, Long> {
    // 활성화된 템플릿 목록 조회
    List<ContractTemplate> findByIsActiveTrue();
    
    // 템플릿 이름으로 검색
    List<ContractTemplate> findByTemplateNameContaining(String keyword);
    
    // 특정 PDF ID를 사용하는 템플릿 찾기
    ContractTemplate findByOriginalPdfId(String pdfId);
    
    // 특정 PDF ID를 사용하는 활성화된 템플릿 찾기
    ContractTemplate findByOriginalPdfIdAndIsActiveTrue(String pdfId);
} 