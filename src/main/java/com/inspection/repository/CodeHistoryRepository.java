package com.inspection.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inspection.entity.CodeHistory;

public interface CodeHistoryRepository extends JpaRepository<CodeHistory, Long> {
    // 코드 ID로 이력 조회 (최신순)
    List<CodeHistory> findByCodeIdOrderByChangedAtDesc(String codeId);
} 