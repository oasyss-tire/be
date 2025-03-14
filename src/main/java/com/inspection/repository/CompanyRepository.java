package com.inspection.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.inspection.entity.Company;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    
    // 매장코드로 회사 찾기
    Optional<Company> findByStoreCode(String storeCode);
    
    // 사업자번호로 회사 찾기
    Optional<Company> findByBusinessNumber(String businessNumber);
    
    // 활성화된 회사 목록 찾기
    List<Company> findByActiveTrue();
    
    // 매장명으로 회사 검색 (부분 일치)
    List<Company> findByStoreNameContaining(String storeName);
    
    // 회사 이미지 정보와 함께 회사 정보 조회
    @Query("SELECT c FROM Company c LEFT JOIN FETCH c.companyImage WHERE c.id = ?1")
    Optional<Company> findWithImageById(Long id);
    
    // 활성화된 모든 회사 정보와 이미지 정보 함께 조회
    @Query("SELECT c FROM Company c LEFT JOIN FETCH c.companyImage WHERE c.active = true")
    List<Company> findAllActiveWithImages();
    
    // 가장 큰 매장 번호 조회
    @Query("SELECT MAX(c.storeNumber) FROM Company c")
    String findMaxStoreNumber();
} 