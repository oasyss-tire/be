package com.inspection.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.inspection.entity.ContractTemplate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, Long> {
    // 모든 템플릿 조회 (with fields), 생성일 내림차순
    @Query("SELECT t FROM ContractTemplate t LEFT JOIN FETCH t.fields ORDER BY t.createdAt DESC")
    List<ContractTemplate> findAllTemplates();
    
    // 검색 기능, 생성일 내림차순
    @Query("SELECT t FROM ContractTemplate t LEFT JOIN FETCH t.fields WHERE t.templateName LIKE %:keyword% ORDER BY t.createdAt DESC")
    List<ContractTemplate> findByTemplateNameContaining(@Param("keyword") String keyword);
    
    // 특정 PDF ID를 사용하는 템플릿 찾기
    ContractTemplate findByOriginalPdfId(String pdfId);
    
    // 특정 PDF ID를 사용하는 활성화된 템플릿 찾기
    ContractTemplate findByOriginalPdfIdAndIsActiveTrue(String pdfId);
    
    // ID로 템플릿 조회 시 필드도 함께 로드
    @Query("SELECT t FROM ContractTemplate t LEFT JOIN FETCH t.fields WHERE t.id = :id")
    Optional<ContractTemplate> findByIdWithFields(@Param("id") Long id);
} 