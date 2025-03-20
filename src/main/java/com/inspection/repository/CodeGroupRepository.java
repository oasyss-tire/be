package com.inspection.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inspection.entity.CodeGroup;

public interface CodeGroupRepository extends JpaRepository<CodeGroup, String> {
    // 레벨별 코드 그룹 조회
    List<CodeGroup> findByLevel(Integer level);
    
    // 상위 그룹 ID로 하위 그룹 조회
    List<CodeGroup> findByParentGroupGroupId(String parentGroupId);
    
    // 활성화된 코드 그룹만 조회
    List<CodeGroup> findByActiveTrue();
    
    // 자동생성 관련 메서드
    // 특정 레벨에서 GroupId가 가장 큰 그룹 조회
    Optional<CodeGroup> findTopByLevelOrderByGroupIdDesc(Integer level);
    
    // 특정 상위 그룹 내에서 GroupId가 가장 큰 하위 그룹 조회
    Optional<CodeGroup> findTopByParentGroupGroupIdAndLevelOrderByGroupIdDesc(String parentGroupId, Integer level);
} 