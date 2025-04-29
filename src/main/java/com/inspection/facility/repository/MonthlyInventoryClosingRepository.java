package com.inspection.facility.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.inspection.facility.entity.MonthlyInventoryClosing;

@Repository
public interface MonthlyInventoryClosingRepository extends JpaRepository<MonthlyInventoryClosing, Long> {
    
    // 특정 연월, 회사, 시설물 유형에 대한 마감 데이터 조회
    Optional<MonthlyInventoryClosing> findByYearAndMonthAndCompanyIdAndFacilityTypeCodeId(
            Integer year, Integer month, Long companyId, String facilityTypeCodeId);
    
    // 특정 연월의 모든 마감 데이터 조회
    List<MonthlyInventoryClosing> findByYearAndMonth(Integer year, Integer month);
    
    // 특정 연월, 회사의 마감 데이터 조회
    List<MonthlyInventoryClosing> findByYearAndMonthAndCompanyId(Integer year, Integer month, Long companyId);
    
    // 특정 회사, 시설물 유형의 가장 최근 마감 데이터 조회
    Optional<MonthlyInventoryClosing> findFirstByCompanyIdAndFacilityTypeCodeIdOrderByYearDescMonthDesc(
            Long companyId, String facilityTypeCodeId);
    
    // 특정 연도의 마감 데이터 조회
    List<MonthlyInventoryClosing> findByYear(Integer year);
    
    // 특정 회사, 특정 연도의 마감 데이터 조회
    List<MonthlyInventoryClosing> findByCompanyIdAndYear(Long companyId, Integer year);
    
    // 월별 집계 쿼리 (특정 시설물 유형의 전체 수량 집계)
    @Query("SELECT sum(m.closingQuantity) FROM MonthlyInventoryClosing m " +
           "WHERE m.year = :year AND m.month = :month AND m.facilityType.codeId = :facilityTypeCodeId")
    Integer sumClosingQuantityByYearMonthAndFacilityType(
            @Param("year") Integer year, 
            @Param("month") Integer month,
            @Param("facilityTypeCodeId") String facilityTypeCodeId);
    
    // 회사별 특정 시설물 유형의 전체 연도 마감 데이터 (추이 분석용)
    @Query("SELECT m FROM MonthlyInventoryClosing m " +
           "WHERE m.company.id = :companyId AND m.facilityType.codeId = :facilityTypeCodeId " +
           "ORDER BY m.year, m.month")
    List<MonthlyInventoryClosing> findTrendByCompanyAndFacilityType(
            @Param("companyId") Long companyId, 
            @Param("facilityTypeCodeId") String facilityTypeCodeId);
    
    // 시설물 유형별 연간 입고량 합계 (통계용)
    @Query("SELECT SUM(m.totalInboundQuantity) FROM MonthlyInventoryClosing m " +
           "WHERE m.year = :year AND m.facilityType.codeId = :facilityTypeCodeId")
    Integer sumYearlyInboundByFacilityType(
            @Param("year") Integer year,
            @Param("facilityTypeCodeId") String facilityTypeCodeId);
    
    // 시설물 유형별 연간 출고량 합계 (통계용)
    @Query("SELECT SUM(m.totalOutboundQuantity) FROM MonthlyInventoryClosing m " +
           "WHERE m.year = :year AND m.facilityType.codeId = :facilityTypeCodeId")
    Integer sumYearlyOutboundByFacilityType(
            @Param("year") Integer year,
            @Param("facilityTypeCodeId") String facilityTypeCodeId);
} 