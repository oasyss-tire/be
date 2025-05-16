package com.inspection.facility.repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.inspection.entity.Code;
import com.inspection.entity.Company;
import com.inspection.facility.entity.Facility;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, Long>, JpaSpecificationExecutor<Facility> {
    
    // 브랜드별 시설물 조회
    List<Facility> findByBrand(Code brand);
    
    // 특정 위치 회사의 시설물 조회
    List<Facility> findByLocationCompany(Company locationCompany);
    List<Facility> findByLocationCompanyId(Long locationCompanyId);
    
    // 페이징 기능을 추가한 위치 회사별 시설물 조회
    Page<Facility> findByLocationCompanyId(Long locationCompanyId, Pageable pageable);
    
    // 특정 소유 회사의 시설물 조회
    List<Facility> findByOwnerCompany(Company ownerCompany);
    List<Facility> findByOwnerCompanyId(Long ownerCompanyId);
    
    // 페이징 기능을 추가한 소유 회사별 시설물 조회
    Page<Facility> findByOwnerCompanyId(Long ownerCompanyId, Pageable pageable);
    
    // 시설물 유형별 시설물 조회
    List<Facility> findByFacilityType(Code facilityType);
    
    // 시설물 상태별 시설물 조회
    List<Facility> findByStatus(Code status);
    
    // 설치 유형별 시설물 조회
    List<Facility> findByInstallationType(Code installationType);
    
    // 시리얼번호별 시설물 조회 (중복체크 용도)
    Optional<Facility> findBySerialNumber(String serialNumber);
    
    // 관리번호별 시설물 조회 (중복체크 용도)
    Optional<Facility> findByManagementNumber(String managementNumber);
    
    // 관리번호 존재 여부 확인
    boolean existsByManagementNumber(String managementNumber);
    
    // 설치일 범위별 시설물 조회
    List<Facility> findByInstallationDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // 보증 만료일 범위별 시설물 조회
    List<Facility> findByWarrantyEndDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // 보증 만료일이 특정 일자 이전인 시설물 조회
    List<Facility> findByWarrantyEndDateBefore(LocalDateTime date);
    
    // 복합 검색 조건 (위치 회사 + 상태)
    List<Facility> findByLocationCompanyIdAndStatus(Long locationCompanyId, Code status);
    
    // 복합 검색 조건 (위치 회사 + 브랜드)
    List<Facility> findByLocationCompanyIdAndBrand(Long locationCompanyId, Code brand);
    
    // 복합 검색 조건 (소유 회사 + 상태)
    List<Facility> findByOwnerCompanyIdAndStatus(Long ownerCompanyId, Code status);
    
    // 복합 검색 조건 (소유 회사 + 브랜드)
    List<Facility> findByOwnerCompanyIdAndBrand(Long ownerCompanyId, Code brand);
    
    // 시설물명에 특정 키워드가 포함된 시설물 조회 (JPQL 사용)
    @Query("SELECT f FROM Facility f WHERE f.serialNumber LIKE %:keyword% OR f.managementNumber LIKE %:keyword%")
    List<Facility> searchByKeyword(@Param("keyword") String keyword);
    
    // 위치 회사 주소에 특정 키워드가 포함된 시설물 조회
    @Query("SELECT f FROM Facility f JOIN f.locationCompany lc WHERE lc.address LIKE %:locationKeyword%")
    List<Facility> findByLocationCompanyAddressContaining(@Param("locationKeyword") String locationKeyword);
    
    // 키워드로 검색 (시리얼번호, 관리번호, 위치/소유 회사)
    @Query("SELECT f FROM Facility f WHERE " +
           "(:keyword IS NULL OR f.serialNumber LIKE %:keyword% OR f.managementNumber LIKE %:keyword%) AND " +
           "(:locationCompanyId IS NULL OR f.locationCompany.id = :locationCompanyId) AND " +
           "(:ownerCompanyId IS NULL OR f.ownerCompany.id = :ownerCompanyId)")
    Page<Facility> searchWithCompanies(
        @Param("keyword") String keyword,
        @Param("locationCompanyId") Long locationCompanyId,
        @Param("ownerCompanyId") Long ownerCompanyId,
        Pageable pageable
    );
    
    // 간단한 키워드 검색
    @Query("SELECT f FROM Facility f WHERE " +
           "(:keyword IS NULL OR f.serialNumber LIKE %:keyword% OR f.managementNumber LIKE %:keyword%) AND " +
           "(:companyId IS NULL OR f.locationCompany.id = :companyId OR f.ownerCompany.id = :companyId)")
    Page<Facility> search(
        @Param("keyword") String keyword,
        @Param("companyId") Long companyId,
        Pageable pageable
    );
    
    // 보증 만료 예정 시설물 조회
    @Query("SELECT f FROM Facility f WHERE " + 
           "f.warrantyEndDate BETWEEN :startDate AND :endDate AND " +
           "(:companyId IS NULL OR f.locationCompany.id = :companyId OR f.ownerCompany.id = :companyId)")
    List<Facility> findWarrantyExpiring(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate,
        @Param("companyId") Long companyId
    );
    
    // 마지막으로 생성된 시설물 조회 (시리얼 번호 생성 시 사용)
    Optional<Facility> findTopByOrderByFacilityIdDesc();
    
    // 특정 회계연도/월에 감가상각 대상이 되는 시설물 조회 (이미 처리된 시설물 제외, 폐기된 시설물 제외)
    @Query("SELECT f FROM Facility f WHERE f.facilityId NOT IN :processedIds " +
           "AND f.status.codeId != '002003_0003' " + // 폐기 상태가 아님
           "AND f.currentValue > 0 " + // 현재 가치가 0보다 큼
           "AND f.depreciationMethod IS NOT NULL " + // 감가상각 방법이 설정됨
           "AND f.usefulLifeMonths > 0") // 내용연수가 설정됨
    List<Facility> findFacilitiesForDepreciation(@Param("processedIds") List<Long> processedFacilityIds);
    
    // 특정 날짜 범위와 시설물 타입별 생성된 시설물 수 카운트
    int countByFacilityType_CodeIdAndCreatedAtBetween(String facilityTypeCode, LocalDateTime startDate, LocalDateTime endDate);
    
    // 시설물 유형별 카운트 조회
    @Query("SELECT f.facilityType.codeId AS typeCode, COUNT(f) AS count FROM Facility f WHERE f.isActive = true GROUP BY f.facilityType.codeId")
    List<Object[]> countGroupByFacilityType();
    
    // 시설물 유형별 카운트를 Map으로 반환
    @Query("SELECT f.facilityType.codeId as typeCode, COUNT(f) as count FROM Facility f WHERE f.isActive = true GROUP BY f.facilityType.codeId")
    default Map<String, Long> countByFacilityTypeCode() {
        Map<String, Long> result = new HashMap<>();
        List<Object[]> counts = countGroupByFacilityType();
        
        for (Object[] count : counts) {
            String typeCode = (String) count[0];
            Long countValue = ((Number) count[1]).longValue();
            result.put(typeCode, countValue);
        }
        
        return result;
    }
} 