package com.inspection.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inspection.entity.Code;
import com.inspection.entity.CodeAttribute;

public interface CodeAttributeRepository extends JpaRepository<CodeAttribute, Long> {
    // 코드 ID로 속성 조회
    List<CodeAttribute> findByCodeCodeId(String codeId);
    
    // 코드 ID와 속성 키로 속성 조회
    Optional<CodeAttribute> findByCodeCodeIdAndAttributeKey(String codeId, String attributeKey);
    
    // 코드 ID와 속성 키 패턴으로 삭제
    void deleteByCodeCodeIdAndAttributeKeyStartingWith(String codeId, String keyPrefix);
    
    // Code 엔티티로 속성 조회
    List<CodeAttribute> findByCode(Code code);
} 