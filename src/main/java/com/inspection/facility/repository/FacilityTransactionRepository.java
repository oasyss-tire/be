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
    
    // 배치 ID로 트랜잭션 조회 (최신순)
    List<FacilityTransaction> findByBatchIdOrderByTransactionDateDesc(String batchId);
    
    // 배치 ID로 트랜잭션 수 조회 (취소 제외)
    long countByBatchIdAndIsCancelledFalse(String batchId);
    
    // 배치 ID와 취소 여부로 트랜잭션 조회
    List<FacilityTransaction> findByBatchIdAndIsCancelledOrderByTransactionDateDesc(String batchId, boolean isCancelled);
    
    // 취소되지 않은 배치 ID별 트랜잭션 조회
    List<FacilityTransaction> findByBatchIdAndIsCancelledFalseOrderByTransactionDateDesc(String batchId);
    
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
    
    // 특정 날짜의 모든 배치 ID 조회 (중복 제거, 취소되지 않은 트랜잭션만)
    @Query("SELECT DISTINCT t.batchId FROM FacilityTransaction t " +
           "WHERE DATE(t.transactionDate) = DATE(:date) " +
           "AND t.isCancelled = false " +
           "ORDER BY t.transactionDate DESC")
    List<String> findDistinctBatchIdsByTransactionDate(@Param("date") LocalDateTime date);
    
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
    
    /**
     * 특정 기간, 회사, 시설물 유형에 대한 입고 트랜잭션 수량 조회
     * 마감 시간 기준으로 조회합니다.
     * 입고(002011_0001)와 이동(002011_0003)(도착지로서) 트랜잭션을 포함
     * 취소된 트랜잭션은 제외합니다.
     * 
     * @param lastClosingTime 마지막 마감 처리 시간
     * @param currentProcessingTime 현재 마감 처리 시작 시간
     * @param companyId 회사 ID
     * @param facilityTypeCodeId 시설물 유형 코드 ID
     * @return 입고 수량
     */
    @Query(value = "SELECT /*+ INDEX(ft idx_ft_to_company) INDEX(f idx_facility_type) INDEX(ft idx_ft_transaction_date) */ " +
           "COUNT(DISTINCT ft.facility_id) FROM facility_transactions ft " +
           "JOIN facilities f ON ft.facility_id = f.facility_id " +
           "WHERE ft.transaction_date > :lastClosingTime " +
           "AND ft.transaction_date <= :currentProcessingTime " +
           "AND ft.to_company_id = :companyId " +
           "AND f.facility_type_code = :facilityTypeCodeId " +
           "AND ft.is_cancelled = false " +
           "AND (ft.transaction_type_code = '002011_0001' OR " +
           "    (ft.transaction_type_code = '002011_0003' AND ft.from_company_id != :companyId))", 
           nativeQuery = true)
    int countInboundTransactionsBetweenClosingTimes(
            @Param("lastClosingTime") LocalDateTime lastClosingTime,
            @Param("currentProcessingTime") LocalDateTime currentProcessingTime,
            @Param("companyId") Long companyId,
            @Param("facilityTypeCodeId") String facilityTypeCodeId);
    
    /**
     * 특정 기간, 회사, 시설물 유형에 대한 출고 트랜잭션 수량 조회
     * 마감 시간 기준으로 조회합니다.
     * 출고(002011_0002), 폐기(002011_0007), 이동(002011_0003)(출발지로서) 트랜잭션을 포함
     * 취소된 트랜잭션은 제외합니다.
     * 
     * @param lastClosingTime 마지막 마감 처리 시간
     * @param currentProcessingTime 현재 마감 처리 시작 시간
     * @param companyId 회사 ID
     * @param facilityTypeCodeId 시설물 유형 코드 ID
     * @return 출고 수량
     */
    @Query(value = "SELECT /*+ INDEX(ft idx_ft_from_company) INDEX(f idx_facility_type) INDEX(ft idx_ft_transaction_date) */ " +
           "COUNT(DISTINCT ft.facility_id) FROM facility_transactions ft " +
           "JOIN facilities f ON ft.facility_id = f.facility_id " +
           "WHERE ft.transaction_date > :lastClosingTime " +
           "AND ft.transaction_date <= :currentProcessingTime " +
           "AND ft.from_company_id = :companyId " +
           "AND f.facility_type_code = :facilityTypeCodeId " +
           "AND ft.is_cancelled = false " +
           "AND (ft.transaction_type_code = '002011_0002' OR " + // 출고
           "    ft.transaction_type_code = '002011_0007' OR " + // 폐기
           "    (ft.transaction_type_code = '002011_0003' AND ft.to_company_id != :companyId))", 
           nativeQuery = true)
    int countOutboundTransactionsBetweenClosingTimes(
            @Param("lastClosingTime") LocalDateTime lastClosingTime,
            @Param("currentProcessingTime") LocalDateTime currentProcessingTime,
            @Param("companyId") Long companyId,
            @Param("facilityTypeCodeId") String facilityTypeCodeId);
    
    // 특정 시설물의 특정 트랜잭션 유형 조회 (최신순)
    List<FacilityTransaction> findByFacilityAndTransactionTypeCodeIdOrderByTransactionDateDesc(
            Facility facility, String transactionTypeCode);
    
    /**
     * 특정 회사의 특정 날짜 이후 시설물 트랜잭션 목록 조회
     * 취소된 트랜잭션은 제외합니다.
     * @param companyId 회사 ID
     * @param dateTime 기준 일시
     * @return 트랜잭션 목록
     */
    @Query("SELECT DISTINCT ft FROM FacilityTransaction ft " +
           "WHERE ft.transactionDate > :dateTime " +
           "AND ft.isCancelled = false " +
           "AND (ft.fromCompany.id = :companyId OR ft.toCompany.id = :companyId)")
    List<FacilityTransaction> findActiveFacilitiesByCompanyIdAndTransactionDateAfter(
            @Param("companyId") Long companyId,
            @Param("dateTime") LocalDateTime dateTime);
    
    /**
     * 특정 기간, 회사, 시설물 유형에 대한 활성(폐기되지 않은) 입고 트랜잭션 수량 조회
     * 마감 시간 기준으로 조회합니다.
     * 입고(002011_0001)와 이동(002011_0003)(도착지로서) 트랜잭션을 포함
     * 취소된 트랜잭션과 비활성 시설물(폐기된 시설물)은 제외합니다.
     * 
     * @param lastClosingTime 마지막 마감 처리 시간
     * @param currentProcessingTime 현재 마감 처리 시작 시간
     * @param companyId 회사 ID
     * @param facilityTypeCodeId 시설물 유형 코드 ID
     * @return 입고 수량
     */
    @Query("SELECT COUNT(DISTINCT ft.facility.facilityId) FROM FacilityTransaction ft " +
           "WHERE ft.transactionDate > :lastClosingTime " +
           "AND ft.transactionDate <= :currentProcessingTime " +
           "AND ft.toCompany.id = :companyId " +
           "AND ft.facility.facilityType.codeId = :facilityTypeCodeId " +
           "AND ft.isCancelled = false " +
           "AND ft.facility.isActive = true " + // 폐기 상태 코드 대신 isActive 필드 사용
           "AND (ft.transactionType.codeId = '002011_0001' OR " +
           "    (ft.transactionType.codeId = '002011_0003' AND ft.fromCompany.id != :companyId))")
    int countActiveInboundTransactionsBetweenClosingTimes(
            @Param("lastClosingTime") LocalDateTime lastClosingTime,
            @Param("currentProcessingTime") LocalDateTime currentProcessingTime,
            @Param("companyId") Long companyId,
            @Param("facilityTypeCodeId") String facilityTypeCodeId);
    
    /**
     * 특정 기간, 회사, 시설물 유형에 대한 활성(폐기되지 않은) 출고 트랜잭션 수량 조회
     * 마감 시간 기준으로 조회합니다.
     * 출고(002011_0002), 폐기(002011_0007), 이동(002011_0003)(출발지로서) 트랜잭션을 포함
     * 취소된 트랜잭션과 비활성 시설물(폐기된 시설물)은 제외합니다.
     * 
     * @param lastClosingTime 마지막 마감 처리 시간
     * @param currentProcessingTime 현재 마감 처리 시작 시간
     * @param companyId 회사 ID
     * @param facilityTypeCodeId 시설물 유형 코드 ID
     * @return 출고 수량
     */
    @Query("SELECT COUNT(DISTINCT ft.facility.facilityId) FROM FacilityTransaction ft " +
           "WHERE ft.transactionDate > :lastClosingTime " +
           "AND ft.transactionDate <= :currentProcessingTime " +
           "AND ft.fromCompany.id = :companyId " +
           "AND ft.facility.facilityType.codeId = :facilityTypeCodeId " +
           "AND ft.isCancelled = false " +
           "AND ft.facility.isActive = true " + // 폐기 상태 코드 대신 isActive 필드 사용
           "AND (ft.transactionType.codeId = '002011_0002' OR " + // 출고
           "    ft.transactionType.codeId = '002011_0007' OR " + // 폐기
           "    (ft.transactionType.codeId = '002011_0003' AND ft.toCompany.id != :companyId))")
    int countActiveOutboundTransactionsBetweenClosingTimes(
            @Param("lastClosingTime") LocalDateTime lastClosingTime,
            @Param("currentProcessingTime") LocalDateTime currentProcessingTime,
            @Param("companyId") Long companyId,
            @Param("facilityTypeCodeId") String facilityTypeCodeId);
    
    /**
     * 특정 기간, 회사, 시설물 유형에 대한 폐기 트랜잭션 수량 조회
     * 마감 시간 기준으로 조회합니다.
     * 폐기(002011_0007) 트랜잭션만 포함 (isActive 상관없이)
     * 취소된 트랜잭션은 제외합니다.
     * 
     * @param lastClosingTime 마지막 마감 처리 시간
     * @param currentProcessingTime 현재 마감 처리 시작 시간
     * @param companyId 회사 ID
     * @param facilityTypeCodeId 시설물 유형 코드 ID
     * @return 폐기 수량
     */
    @Query("SELECT COUNT(DISTINCT ft.facility.facilityId) FROM FacilityTransaction ft " +
           "WHERE ft.transactionDate > :lastClosingTime " +
           "AND ft.transactionDate <= :currentProcessingTime " +
           "AND ft.fromCompany.id = :companyId " +
           "AND ft.facility.facilityType.codeId = :facilityTypeCodeId " +
           "AND ft.isCancelled = false " +
           "AND ft.transactionType.codeId = '002011_0007'")
    int countDisposeTransactionsBetweenClosingTimes(
            @Param("lastClosingTime") LocalDateTime lastClosingTime,
            @Param("currentProcessingTime") LocalDateTime currentProcessingTime,
            @Param("companyId") Long companyId,
            @Param("facilityTypeCodeId") String facilityTypeCodeId);
    
    /**
     * 마감 이후 폐기 트랜잭션이 발생한 시설물 수 조회
     * 마감 시간 기준으로 조회합니다.
     * 폐기(002011_0007) 트랜잭션만 포함
     * 취소된 트랜잭션은 제외합니다.
     * 
     * @param lastClosingTime 마지막 마감 처리 시간
     * @param currentProcessingTime 현재 처리 시작 시간
     * @param companyId 회사 ID
     * @param facilityTypeCodeId 시설물 유형 코드 ID
     * @return 폐기 시설물 수
     */
    @Query("SELECT COUNT(DISTINCT ft.facility.facilityId) FROM FacilityTransaction ft " +
           "WHERE ft.transactionDate > :lastClosingTime " +
           "AND ft.transactionDate <= :currentProcessingTime " +
           "AND ft.fromCompany.id = :companyId " +
           "AND ft.facility.facilityType.codeId = :facilityTypeCodeId " +
           "AND ft.isCancelled = false " +
           "AND ft.transactionType.codeId = '002011_0007'")
    int countNewlyDisposedFacilitiesBetweenClosingTimes(
            @Param("lastClosingTime") LocalDateTime lastClosingTime,
            @Param("currentProcessingTime") LocalDateTime currentProcessingTime,
            @Param("companyId") Long companyId,
            @Param("facilityTypeCodeId") String facilityTypeCodeId);
    
    /**
     * 특정 기간, 회사, 시설물 유형에 대한 트랜잭션 통계를 한 번에 조회
     * 마감 시간 기준으로 조회합니다.
     * 입고, 출고 수량을 한 번의 쿼리로 계산합니다.
     * 
     * @param companies 회사 ID 목록
     * @param facilityTypes 시설물 유형 코드 ID 목록
     * @param lastClosingTime 마지막 마감 처리 시간
     * @param currentProcessingTime 현재 마감 처리 시작 시간
     * @return [회사ID, 시설물유형코드ID, 입고수량, 출고수량] 형태의 결과 목록
     */
    @Query(value = "SELECT " +
           "t.company_id as company_id, " +
           "t.facility_type_code as facility_type_code, " +
           "SUM(CASE WHEN t.direction = 'IN' THEN t.count ELSE 0 END) as inbound_count, " +
           "SUM(CASE WHEN t.direction = 'OUT' THEN t.count ELSE 0 END) as outbound_count " +
           "FROM ( " +
           "  SELECT /*+ INDEX(ft idx_ft_to_company) INDEX(f idx_facility_type) */ " +
           "    to_company_id as company_id, " +
           "    f.facility_type_code, " +
           "    COUNT(DISTINCT ft.facility_id) as count, " +
           "    'IN' as direction " +
           "  FROM facility_transactions ft " +
           "  JOIN facilities f ON ft.facility_id = f.facility_id " +
           "  WHERE ft.transaction_date > :lastClosingTime " +
           "    AND ft.transaction_date <= :currentProcessingTime " +
           "    AND ft.to_company_id IN :companyIds " +
           "    AND f.facility_type_code IN :facilityTypes " +
           "    AND ft.is_cancelled = false " +
           "    AND (ft.transaction_type_code = '002011_0001' OR " +
           "        (ft.transaction_type_code = '002011_0003' AND ft.from_company_id != ft.to_company_id)) " +
           "  GROUP BY to_company_id, f.facility_type_code " +
           "  UNION ALL " +
           "  SELECT /*+ INDEX(ft idx_ft_from_company) INDEX(f idx_facility_type) */ " +
           "    from_company_id as company_id, " +
           "    f.facility_type_code, " +
           "    COUNT(DISTINCT ft.facility_id) as count, " +
           "    'OUT' as direction " +
           "  FROM facility_transactions ft " +
           "  JOIN facilities f ON ft.facility_id = f.facility_id " +
           "  WHERE ft.transaction_date > :lastClosingTime " +
           "    AND ft.transaction_date <= :currentProcessingTime " +
           "    AND ft.from_company_id IN :companyIds " +
           "    AND f.facility_type_code IN :facilityTypes " +
           "    AND ft.is_cancelled = false " +
           "    AND (ft.transaction_type_code = '002011_0002' OR " +
           "        ft.transaction_type_code = '002011_0007' OR " +
           "        (ft.transaction_type_code = '002011_0003' AND ft.from_company_id != ft.to_company_id)) " +
           "  GROUP BY from_company_id, f.facility_type_code " +
           ") t " +
           "GROUP BY t.company_id, t.facility_type_code", 
           nativeQuery = true)
    List<Object[]> getBulkTransactionStatistics(
            @Param("companyIds") List<Long> companyIds,
            @Param("facilityTypes") List<String> facilityTypes,
            @Param("lastClosingTime") LocalDateTime lastClosingTime,
            @Param("currentProcessingTime") LocalDateTime currentProcessingTime);
} 