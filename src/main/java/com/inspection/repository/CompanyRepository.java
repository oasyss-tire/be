package com.inspection.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.inspection.entity.Company;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    
    // 매장코드로 회사 찾기
    Optional<Company> findByStoreCode(String storeCode);
    
    // 사업자번호로 회사 찾기
    Optional<Company> findByBusinessNumber(String businessNumber);
    
    // 수탁코드로 회사 찾기
    Optional<Company> findByTrusteeCode(String trusteeCode);
    
    // 종사업장번호로 회사 찾기
    Optional<Company> findBySubBusinessNumber(String subBusinessNumber);
    
    // 활성화된 회사 목록 찾기
    List<Company> findByActiveTrue();
    
    // 활성화된 회사 목록 페이징 처리
    Page<Company> findByActiveTrue(Pageable pageable);
    
    // 모든 회사 목록 페이징 처리
    Page<Company> findAll(Pageable pageable);
    
    // 매장명으로 회사 검색 (부분 일치)
    List<Company> findByStoreNameContaining(String storeName);
    
    // 매장명으로 회사 검색 (부분 일치) - 페이징 처리
    Page<Company> findByStoreNameContaining(String storeName, Pageable pageable);
    
    // 회사 이미지 정보와 함께 회사 정보 조회
    @Query("SELECT c FROM Company c LEFT JOIN FETCH c.companyImage WHERE c.id = ?1")
    Optional<Company> findWithImageById(Long id);
    
    // 활성화된 모든 회사 정보와 이미지 정보 함께 조회
    @Query("SELECT c FROM Company c LEFT JOIN FETCH c.companyImage WHERE c.active = true")
    List<Company> findAllActiveWithImages();
    
    // 가장 큰 매장 번호 조회
    @Query("SELECT MAX(c.storeNumber) FROM Company c")
    String findMaxStoreNumber();
    
    // 이벤트 로그 조회용 메서드
    // 매장명, 사업자번호 또는 매장코드로 회사 검색 (키워드 검색)
    List<Company> findByStoreNameContainingOrBusinessNumberContainingOrStoreCodeContaining(
            String storeNameKeyword, String businessNumberKeyword, String storeCodeKeyword);
    
    // 매장명, 사업자번호 또는 매장코드로 회사 검색 (키워드 검색) - 페이징 처리
    Page<Company> findByStoreNameContainingOrBusinessNumberContainingOrStoreCodeContaining(
            String storeNameKeyword, String businessNumberKeyword, String storeCodeKeyword, Pageable pageable);
} 