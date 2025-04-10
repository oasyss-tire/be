package com.inspection.facility.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.inspection.facility.entity.Depreciation;

@Repository
public interface DepreciationRepository extends JpaRepository<Depreciation, Long> {
    
    /**
     * 특정 시설물의 감가상각 이력 조회
     */
    List<Depreciation> findByFacilityFacilityId(Long facilityId);
    
    /**
     * 특정 시설물의 감가상각 이력 페이징 조회
     */
    Page<Depreciation> findByFacilityFacilityId(Long facilityId, Pageable pageable);
    
    /**
     * 특정 날짜 범위 내의 감가상각 이력 조회
     */
    List<Depreciation> findByDepreciationDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 특정 회계연도의 감가상각 이력 조회
     */
    List<Depreciation> findByFiscalYear(Integer fiscalYear);
    
    /**
     * 특정 회계연도 및 회계월의 감가상각 이력 조회
     */
    List<Depreciation> findByFiscalYearAndFiscalMonth(Integer fiscalYear, Integer fiscalMonth);
    
    /**
     * 특정 감가상각 유형의 이력 조회
     */
    List<Depreciation> findByDepreciationTypeCodeId(String depreciationTypeCode);
    
    /**
     * 특정 감가상각 방법의 이력 조회
     */
    List<Depreciation> findByDepreciationMethodCodeId(String depreciationMethodCode);
    
    /**
     * 특정 시설물의 최신 감가상각 이력 조회
     */
    @Query("SELECT d FROM Depreciation d WHERE d.facility.facilityId = :facilityId ORDER BY d.depreciationDate DESC, d.createdAt DESC")
    List<Depreciation> findLatestByFacilityId(@Param("facilityId") Long facilityId, Pageable pageable);
    
    /**
     * 특정 시설물의 특정 회계연도 감가상각 총액 조회
     */
    @Query("SELECT SUM(d.depreciationAmount) FROM Depreciation d WHERE d.facility.facilityId = :facilityId AND d.fiscalYear = :fiscalYear")
    Double getTotalDepreciationAmountByFacilityAndFiscalYear(
            @Param("facilityId") Long facilityId, 
            @Param("fiscalYear") Integer fiscalYear);
    
    /**
     * 모든 시설물의 특정 회계연도 감가상각 총액 조회
     */
    @Query("SELECT SUM(d.depreciationAmount) FROM Depreciation d WHERE d.fiscalYear = :fiscalYear")
    Double getTotalDepreciationAmountByFiscalYear(@Param("fiscalYear") Integer fiscalYear);
    
    // 시설물 ID로 감가상각 이력 조회 (날짜 내림차순)
    List<Depreciation> findByFacilityFacilityIdOrderByDepreciationDateDesc(Long facilityId);
    
    // 감가상각 일자 범위로 조회 (날짜 내림차순)
    List<Depreciation> findByDepreciationDateBetweenOrderByDepreciationDateDesc(LocalDateTime startDate, LocalDateTime endDate);
    
    // 회계연도와 회계월로 조회 (날짜 내림차순)
    List<Depreciation> findByFiscalYearAndFiscalMonthOrderByDepreciationDateDesc(Integer fiscalYear, Integer fiscalMonth);
    
    // 모든 감가상각 이력 페이징 조회 (날짜 내림차순)
    Page<Depreciation> findAllByOrderByDepreciationDateDesc(Pageable pageable);
    
    // 특정 시설물의 특정 회계연도/월에 감가상각 이력이 존재하는지 확인
    boolean existsByFacilityFacilityIdAndFiscalYearAndFiscalMonth(Long facilityId, Integer fiscalYear, Integer fiscalMonth);
    
    // 특정 회계연도/월에 이미 감가상각 처리된 시설물 ID 목록 조회
    @Query("SELECT DISTINCT d.facility.facilityId FROM Depreciation d WHERE d.fiscalYear = :year AND d.fiscalMonth = :month")
    List<Long> findFacilityIdsByFiscalYearAndFiscalMonth(@Param("year") Integer fiscalYear, @Param("month") Integer fiscalMonth);
    
    // 모든 감가상각 이력 조회 (날짜 내림차순)
    List<Depreciation> findAllByOrderByDepreciationDateDesc();
} 