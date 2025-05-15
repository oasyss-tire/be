package com.inspection.facility.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.inspection.facility.entity.DailyInventoryClosing;

@Repository
public interface DailyInventoryClosingRepository extends JpaRepository<DailyInventoryClosing, Long> {
    
    // 특정 일자, 회사, 시설물 유형에 대한 마감 데이터 조회
    Optional<DailyInventoryClosing> findByClosingDateAndCompanyIdAndFacilityTypeCodeId(
            LocalDate closingDate, Long companyId, String facilityTypeCodeId);
    
    // 특정 일자의 모든 마감 데이터 조회 (성능 최적화)
    @Query(value = "SELECT d FROM DailyInventoryClosing d WHERE d.closingDate = :closingDate")
    List<DailyInventoryClosing> findByClosingDate(@Param("closingDate") LocalDate closingDate);
    
    // 특정 일자, 회사의 마감 데이터 조회
    List<DailyInventoryClosing> findByClosingDateAndCompanyId(LocalDate closingDate, Long companyId);
    
    /**
     * 특정 일자, 회사 목록에 대한 마감 데이터 조회 (배치 처리 최적화)
     * @param closingDate 마감 날짜
     * @param companyIds 회사 ID 목록
     * @return 마감 데이터 목록
     */
    @Query("SELECT d FROM DailyInventoryClosing d WHERE d.closingDate = :closingDate AND d.company.id IN :companyIds")
    List<DailyInventoryClosing> findByClosingDateAndCompanyIdIn(
            @Param("closingDate") LocalDate closingDate, 
            @Param("companyIds") List<Long> companyIds);
    
    /**
     * 특정 일자, 회사 목록, 마감 여부에 대한 마감 데이터 조회 (배치 처리 최적화)
     * @param closingDate 마감 날짜
     * @param companyIds 회사 ID 목록
     * @param isClosed 마감 여부
     * @return 마감 데이터 목록
     */
    @Query("SELECT d FROM DailyInventoryClosing d WHERE d.closingDate = :closingDate " +
           "AND d.company.id IN :companyIds AND d.isClosed = :isClosed")
    List<DailyInventoryClosing> findByClosingDateAndCompanyIdInAndIsClosed(
            @Param("closingDate") LocalDate closingDate, 
            @Param("companyIds") List<Long> companyIds,
            @Param("isClosed") boolean isClosed);
    
    // 특정 회사, 시설물 유형의 가장 최근 마감 데이터 조회 (MySQL 최적화)
    @Query(value = "SELECT d FROM DailyInventoryClosing d " +
           "WHERE d.company.id = :companyId AND d.facilityType.codeId = :facilityTypeCodeId " +
           "ORDER BY d.closingDate DESC")
    List<DailyInventoryClosing> findLatestByCompanyIdAndFacilityTypeCodeId(
            @Param("companyId") Long companyId, 
            @Param("facilityTypeCodeId") String facilityTypeCodeId);
    
    // 단일 결과를 위한 최적화된 쿼리
    @Query(value = "SELECT d FROM DailyInventoryClosing d " +
           "WHERE d.company.id = :companyId AND d.facilityType.codeId = :facilityTypeCodeId " +
           "ORDER BY d.closingDate DESC LIMIT 1", nativeQuery = false)
    Optional<DailyInventoryClosing> findFirstByCompanyIdAndFacilityTypeCodeIdOrderByClosingDateDesc(
            @Param("companyId") Long companyId, 
            @Param("facilityTypeCodeId") String facilityTypeCodeId);
    
    // 특정 기간의 마감 데이터 조회 (페이징을 통한 최적화)
    @Query(value = "SELECT d FROM DailyInventoryClosing d " +
           "WHERE d.closingDate BETWEEN :startDate AND :endDate " +
           "ORDER BY d.closingDate DESC")
    List<DailyInventoryClosing> findByClosingDateBetween(
            @Param("startDate") LocalDate startDate, 
            @Param("endDate") LocalDate endDate);
    
    // 특정 회사, 특정 기간의 마감 데이터 조회
    List<DailyInventoryClosing> findByClosingDateBetweenAndCompanyId(
            LocalDate startDate, LocalDate endDate, Long companyId);
    
    // 특정 회사, 특정 시설물 유형, 특정 기간의 마감 데이터 조회
    List<DailyInventoryClosing> findByClosingDateBetweenAndCompanyIdAndFacilityTypeCodeId(
            LocalDate startDate, LocalDate endDate, Long companyId, String facilityTypeCodeId);
    
    // 날짜별 집계 쿼리 (특정 시설물 유형의 전체 수량 집계)
    @Query("SELECT sum(d.closingQuantity) FROM DailyInventoryClosing d " +
           "WHERE d.closingDate = :date AND d.facilityType.codeId = :facilityTypeCodeId")
    Integer sumClosingQuantityByDateAndFacilityType(
            @Param("date") LocalDate date, 
            @Param("facilityTypeCodeId") String facilityTypeCodeId);
    
    // 회사별 시설물 유형별 마지막 마감 날짜 조회
    @Query("SELECT MAX(d.closingDate) FROM DailyInventoryClosing d " +
           "WHERE d.company.id = :companyId AND d.facilityType.codeId = :facilityTypeCodeId")
    LocalDate findLastClosingDateByCompanyAndFacilityType(
            @Param("companyId") Long companyId, 
            @Param("facilityTypeCodeId") String facilityTypeCodeId);
            
    // 특정 회사, 시설물 유형에 대한 마지막 마감된(isClosed=true) 데이터 조회 (MySQL 최적화)
    @Query(value = "SELECT d FROM DailyInventoryClosing d " +
           "WHERE d.company.id = :companyId AND d.facilityType.codeId = :facilityTypeCodeId " +
           "AND d.isClosed = true " +
           "ORDER BY d.closingDate DESC")
    List<DailyInventoryClosing> findLatestClosedByCompanyIdAndFacilityTypeCodeId(
            @Param("companyId") Long companyId, 
            @Param("facilityTypeCodeId") String facilityTypeCodeId);
    
    // 단일 결과를 위한 최적화된 쿼리        
    @Query(value = "SELECT * FROM daily_inventory_closings d " +
           "WHERE d.company_id = :companyId AND d.facility_type_code = :facilityTypeCodeId " +
           "AND d.is_closed = true " +
           "ORDER BY d.closing_date DESC LIMIT 1", nativeQuery = true)
    Optional<DailyInventoryClosing> findTopByCompanyIdAndFacilityTypeCodeIdAndIsClosedTrueOrderByClosingDateDesc(
            @Param("companyId") Long companyId, 
            @Param("facilityTypeCodeId") String facilityTypeCodeId);
            
    /**
     * 특정 회사와 시설물 유형에 대해 특정 날짜 이전의 가장 최근 마감 데이터 조회 (날짜 기준 정렬)
     * @param companyId 회사 ID
     * @param facilityTypeCodeId 시설물 유형 코드 ID
     * @param closingDate 기준 날짜
     * @return 이전 마감 데이터 (Optional)
     */
    @Query(value = "SELECT * FROM daily_inventory_closings d " +
           "WHERE d.company_id = :companyId AND d.facility_type_code = :facilityTypeCodeId " +
           "AND d.closing_date < :closingDate " +
           "AND d.is_closed = true " +
           "ORDER BY d.closing_date DESC LIMIT 1", nativeQuery = true)
    Optional<DailyInventoryClosing> findTopByCompanyIdAndFacilityTypeCodeIdAndClosingDateBeforeAndIsClosedTrueOrderByClosingDateDesc(
            @Param("companyId") Long companyId, 
            @Param("facilityTypeCodeId") String facilityTypeCodeId, 
            @Param("closingDate") LocalDate closingDate);

    /**
     * 특정 날짜, 회사, 시설물 유형, 마감 여부에 대한 마감 데이터 조회
     * @param closingDate 마감 날짜
     * @param companyId 회사 ID
     * @param facilityTypeCodeId 시설물 유형 코드 ID
     * @param isClosed 마감 여부
     * @return 마감 데이터 (Optional)
     */
    @Query(value = "SELECT * FROM daily_inventory_closings d " +
           "WHERE d.closing_date = :closingDate " +
           "AND d.company_id = :companyId AND d.facility_type_code = :facilityTypeCodeId " +
           "AND d.is_closed = :isClosed LIMIT 1", nativeQuery = true)
    Optional<DailyInventoryClosing> findByClosingDateAndCompanyIdAndFacilityTypeCodeIdAndIsClosed(
            @Param("closingDate") LocalDate closingDate, 
            @Param("companyId") Long companyId, 
            @Param("facilityTypeCodeId") String facilityTypeCodeId, 
            @Param("isClosed") boolean isClosed);
    
    /**
     * 여러 회사 및 시설물 유형에 대해 한 번에 마감 데이터 조회 (배치 처리 최적화)
     * @param closingDate 마감 날짜
     * @return 마감 데이터 목록
     */
    @Query(value = "SELECT d FROM DailyInventoryClosing d " +
           "WHERE d.closingDate = :closingDate " +
           "AND d.isClosed = true")
    List<DailyInventoryClosing> findAllClosedByClosingDate(@Param("closingDate") LocalDate closingDate);

    /**
     * 회사별, 시설물 유형별 최신 마감 데이터 페이징 조회
     * @param companyId 회사 ID (선택적)
     * @param facilityTypeCodeId 시설물 유형 코드 ID (선택적)
     * @param pageable 페이징 정보
     * @return 페이징된 마감 데이터 목록
     */
    @Query(value = "SELECT d.* FROM " +
           "(SELECT company_id, facility_type_code, MAX(closing_date) as max_date " +
           "FROM daily_inventory_closings " +
           "WHERE is_closed = true " +
           "AND (:companyId IS NULL OR company_id = :companyId) " +
           "AND (:facilityTypeCodeId IS NULL OR facility_type_code = :facilityTypeCodeId) " +
           "GROUP BY company_id, facility_type_code) latest " +
           "JOIN daily_inventory_closings d " +
           "ON d.company_id = latest.company_id " +
           "AND d.facility_type_code = latest.facility_type_code " +
           "AND d.closing_date = latest.max_date " +
           "AND d.is_closed = true", 
           countQuery = "SELECT COUNT(*) FROM " +
           "(SELECT company_id, facility_type_code, MAX(closing_date) as max_date " +
           "FROM daily_inventory_closings " +
           "WHERE is_closed = true " +
           "AND (:companyId IS NULL OR company_id = :companyId) " +
           "AND (:facilityTypeCodeId IS NULL OR facility_type_code = :facilityTypeCodeId) " +
           "GROUP BY company_id, facility_type_code) latest",
           nativeQuery = true)
    Page<DailyInventoryClosing> findLatestClosingsByCompanyAndFacilityTypePaged(
            @Param("companyId") Long companyId, 
            @Param("facilityTypeCodeId") String facilityTypeCodeId,
            Pageable pageable);

    /**
     * 특정 기간의 마감 상태가 true인 마감 데이터만 조회
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param isClosed 마감 여부
     * @return 마감 데이터 목록
     */
    List<DailyInventoryClosing> findByClosingDateBetweenAndIsClosed(
            LocalDate startDate, LocalDate endDate, boolean isClosed);
    
    /**
     * 특정 회사, 특정 기간의 마감 상태가 true인 마감 데이터만 조회
     * @param companyId 회사 ID
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param isClosed 마감 여부
     * @return 마감 데이터 목록
     */
    List<DailyInventoryClosing> findByCompanyIdAndClosingDateBetweenAndIsClosed(
            Long companyId, LocalDate startDate, LocalDate endDate, boolean isClosed);
} 