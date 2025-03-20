package com.inspection.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inspection.entity.CodeAttribute;

public interface CodeAttributeRepository extends JpaRepository<CodeAttribute, Long> {
    // 코드 ID로 속성 조회
    List<CodeAttribute> findByCodeCodeId(String codeId);
} 