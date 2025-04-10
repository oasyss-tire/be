package com.inspection.facility.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.inspection.entity.Code;
import com.inspection.entity.Company;
import com.inspection.facility.entity.Facility;
import com.inspection.facility.entity.FacilityTransaction;

@Repository
public interface FacilityTransactionRepository extends JpaRepository<FacilityTransaction, Long> {
    
    // 특정 시설물의 모든 트랜잭션 조회
    List<FacilityTransaction> findByFacilityOrderByTransactionDateDesc(Facility facility);
    
    // 특정 시설물 ID의 모든 트랜잭션 조회
    List<FacilityTransaction> findByFacilityFacilityIdOrderByTransactionDateDesc(Long facilityId);
    
    // 특정 시설물의 최근 트랜잭션 조회
    Optional<FacilityTransaction> findTopByFacilityOrderByTransactionDateDesc(Facility facility);
    
    // 특정 시설물 ID의 최근 트랜잭션 조회
    Optional<FacilityTransaction> findTopByFacilityFacilityIdOrderByTransactionDateDesc(Long facilityId);
    
    // 모든 트랜잭션을 최신순으로 조회
    List<FacilityTransaction> findAllByOrderByTransactionDateDesc();
    
    // 트랜잭션 유형별 트랜잭션 조회
    List<FacilityTransaction> findByTransactionTypeOrderByTransactionDateDesc(Code transactionType);
    List<FacilityTransaction> findByTransactionTypeCodeIdOrderByTransactionDateDesc(String transactionTypeCode);
    
    // 특정 회사의 모든 트랜잭션 조회 (출발지 또는 도착지)
    List<FacilityTransaction> findByFromCompanyOrToCompanyOrderByTransactionDateDesc(Company fromCompany, Company toCompany);
    
    // 특정 회사 ID의 모든 트랜잭션 조회 (출발지 또는 도착지)
    List<FacilityTransaction> findByFromCompanyIdOrToCompanyIdOrderByTransactionDateDesc(Long companyId, Long sameCompanyId);
    
    // 페이징 기능이 있는 트랜잭션 조회
    Page<FacilityTransaction> findAllByOrderByTransactionDateDesc(Pageable pageable);
    
    // 특정 기간 내의 트랜잭션 조회
    List<FacilityTransaction> findByTransactionDateBetweenOrderByTransactionDateDesc(LocalDateTime startDate, LocalDateTime endDate);
    
    // 특정 기간 내의 특정 유형 트랜잭션 조회
    List<FacilityTransaction> findByTransactionTypeCodeIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            String transactionTypeCode, LocalDateTime startDate, LocalDateTime endDate);
    
    // 관련 AS 요청 기준 트랜잭션 조회
    List<FacilityTransaction> findByServiceRequestServiceRequestIdOrderByTransactionDateDesc(Long serviceRequestId);
    
    // 특정 사용자가 수행한 트랜잭션 조회
    List<FacilityTransaction> findByPerformedByIdOrderByTransactionDateDesc(Long userId);
    
    // 대여 트랜잭션 중 아직 반납되지 않은 항목 조회
    @Query("SELECT t FROM FacilityTransaction t WHERE t.transactionType.codeId = :rentalTypeCode " +
           "AND t.relatedTransaction IS NULL AND t.actualReturnDate IS NULL")
    List<FacilityTransaction> findActiveRentals(@Param("rentalTypeCode") String rentalTypeCode);
    
    // 반납 예정일이 특정 날짜 이전인 대여 트랜잭션 조회 (연체 포함)
    @Query("SELECT t FROM FacilityTransaction t WHERE t.transactionType.codeId = :rentalTypeCode " +
           "AND t.relatedTransaction IS NULL AND t.actualReturnDate IS NULL " +
           "AND t.expectedReturnDate <= :date")
    List<FacilityTransaction> findOverdueRentals(
            @Param("rentalTypeCode") String rentalTypeCode, 
            @Param("date") LocalDateTime date);
    
    // 연관 트랜잭션 조회
    Optional<FacilityTransaction> findByRelatedTransaction(FacilityTransaction relatedTransaction);
} 