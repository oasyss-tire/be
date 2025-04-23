package com.inspection.repository;

import com.inspection.entity.Company;
import com.inspection.entity.CompanyTrusteeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyTrusteeHistoryRepository extends JpaRepository<CompanyTrusteeHistory, Long> {
    
    // 특정 회사의 현재 활성화된 수탁자 정보 조회
    Optional<CompanyTrusteeHistory> findByCompanyAndIsActiveTrue(Company company);
    
    // 회사별 수탁자 이력 조회 (시간순)
    List<CompanyTrusteeHistory> findByCompanyOrderByStartDateDesc(Company company);
    
    // 특정 회사의 모든 수탁자 이력 조회
    List<CompanyTrusteeHistory> findByCompany(Company company);
    
    // 현재 활성화된 모든 수탁자 조회
    List<CompanyTrusteeHistory> findByIsActiveTrue();
    
    // 스케줄러 관련 메서드 추가
    
    // 활성 상태이며 종료일이 특정 날짜 이전인 수탁자 이력 조회
    List<CompanyTrusteeHistory> findByIsActiveTrueAndEndDateBefore(LocalDate date);
    
    // 비활성 상태이며 시작일이 특정 날짜인 수탁자 이력 조회
    List<CompanyTrusteeHistory> findByIsActiveFalseAndStartDate(LocalDate date);
    
    // 활성 상태이며 종료일이 특정 날짜인 수탁자 이력 조회 (만료 예정 알림용)
    List<CompanyTrusteeHistory> findByIsActiveTrueAndEndDate(LocalDate date);
    
    // 보험 기간 기준 조회 메서드 (추가)
    
    // 활성 상태이며 보험 종료일이 특정 날짜 이전인 수탁자 이력 조회
    List<CompanyTrusteeHistory> findByIsActiveTrueAndInsuranceEndDateBefore(LocalDate date);
    
    // 비활성 상태이며 보험 시작일이 특정 날짜인 수탁자 이력 조회
    List<CompanyTrusteeHistory> findByIsActiveFalseAndInsuranceStartDate(LocalDate date);
    
    // 활성 상태이며 보험 종료일이 특정 날짜인 수탁자 이력 조회 (만료 예정 알림용)
    List<CompanyTrusteeHistory> findByIsActiveTrueAndInsuranceEndDate(LocalDate date);
}
