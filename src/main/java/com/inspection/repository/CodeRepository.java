package com.inspection.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inspection.entity.Code;

public interface CodeRepository extends JpaRepository<Code, String> {
    // 그룹 ID로 코드 조회
    List<Code> findByCodeGroupGroupId(String groupId);
    
    // 그룹 ID로 활성화된 코드만 조회
    List<Code> findByCodeGroupGroupIdAndActiveTrue(String groupId);
    
    // 코드 ID로 활성화된 코드 조회
    Optional<Code> findByCodeIdAndActiveTrue(String codeId);
    
    // 자동생성 관련 메서드
    // 특정 그룹 내에서 코드 ID가 가장 큰 코드 조회
    Optional<Code> findTopByCodeGroupGroupIdOrderByCodeIdDesc(String groupId);
} 