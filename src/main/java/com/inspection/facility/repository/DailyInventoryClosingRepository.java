package com.inspection.facility.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    
    // 특정 일자의 모든 마감 데이터 조회
    List<DailyInventoryClosing> findByClosingDate(LocalDate closingDate);
    
    // 특정 일자, 회사의 마감 데이터 조회
    List<DailyInventoryClosing> findByClosingDateAndCompanyId(LocalDate closingDate, Long companyId);
    
    // 특정 회사, 시설물 유형의 가장 최근 마감 데이터 조회
    Optional<DailyInventoryClosing> findFirstByCompanyIdAndFacilityTypeCodeIdOrderByClosingDateDesc(
            Long companyId, String facilityTypeCodeId);
    
    // 특정 기간의 마감 데이터 조회
    List<DailyInventoryClosing> findByClosingDateBetween(LocalDate startDate, LocalDate endDate);
    
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
            
    // 특정 회사, 시설물 유형에 대한 마지막 마감된(isClosed=true) 데이터 조회
    Optional<DailyInventoryClosing> findTopByCompanyIdAndFacilityTypeCodeIdAndIsClosedTrueOrderByClosingDateDesc(
            Long companyId, String facilityTypeCodeId);
            
    /**
     * 특정 회사와 시설물 유형에 대해 특정 날짜 이전의 가장 최근 마감 데이터 조회 (날짜 기준 정렬)
     * @param companyId 회사 ID
     * @param facilityTypeCodeId 시설물 유형 코드 ID
     * @param closingDate 기준 날짜
     * @return 이전 마감 데이터 (Optional)
     */
    Optional<DailyInventoryClosing> findTopByCompanyIdAndFacilityTypeCodeIdAndClosingDateBeforeAndIsClosedTrueOrderByClosingDateDesc(
            Long companyId, String facilityTypeCodeId, LocalDate closingDate);

    /**
     * 특정 날짜, 회사, 시설물 유형, 마감 여부에 대한 마감 데이터 조회
     * @param closingDate 마감 날짜
     * @param companyId 회사 ID
     * @param facilityTypeCodeId 시설물 유형 코드 ID
     * @param isClosed 마감 여부
     * @return 마감 데이터 (Optional)
     */
    Optional<DailyInventoryClosing> findByClosingDateAndCompanyIdAndFacilityTypeCodeIdAndIsClosed(
            LocalDate closingDate, Long companyId, String facilityTypeCodeId, boolean isClosed);
} 