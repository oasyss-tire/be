package com.inspection.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.inspection.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserId(String userId);
    boolean existsByUserId(String userId);
    List<User> findByCompanyId(Long companyId);
    
    // 페이징을 위한 메서드
    Page<User> findAll(Pageable pageable);
    
    // 키워드로 사용자 검색 (사용자 ID, 이름에서 검색)
    Page<User> findByUserIdContainingOrUserNameContaining(String userIdKeyword, String userNameKeyword, Pageable pageable);
} 