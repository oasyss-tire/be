package com.inspection.as.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.inspection.as.entity.ServiceRequest;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long>, JpaSpecificationExecutor<ServiceRequest> {
    
    /**
     * 요청 번호로 AS 접수 조회
     */
    Optional<ServiceRequest> findByRequestNumber(String requestNumber);
    
    /**
     * 특정 시설물의 AS 접수 목록 조회
     */
    List<ServiceRequest> findByFacilityFacilityId(Long facilityId);
    
    /**
     * 특정 시설물의 AS 접수 목록을 최신순(ID 내림차순)으로 조회
     */
    List<ServiceRequest> findByFacilityFacilityIdOrderByServiceRequestIdDesc(Long facilityId);
    
    /**
     * 특정 시설물의 가장 최근 AS 접수 조회
     */
    @Query("SELECT sr FROM ServiceRequest sr WHERE sr.facility.facilityId = :facilityId ORDER BY sr.createdAt DESC")
    Optional<ServiceRequest> findLatestByFacilityId(@Param("facilityId") Long facilityId);
    
    /**
     * 특정 시설물의 아직 완료되지 않은 가장 최근 AS 접수 조회
     */
    @Query("SELECT sr FROM ServiceRequest sr WHERE sr.facility.facilityId = :facilityId AND sr.isCompleted = false ORDER BY sr.createdAt DESC")
    Optional<ServiceRequest> findLatestActiveByFacilityId(@Param("facilityId") Long facilityId);
    
    /**
     * 특정 시설물의 가장 최근 AS 접수 조회 (완료 여부 상관없이)
     */
    @Query(value = "SELECT sr FROM ServiceRequest sr WHERE sr.facility.facilityId = :facilityId ORDER BY sr.createdAt DESC")
    Page<ServiceRequest> findLatestByFacilityIdPaged(@Param("facilityId") Long facilityId, Pageable pageable);
    
    /**
     * 특정 사용자가 요청한 AS 접수 목록 조회
     */
    List<ServiceRequest> findByRequesterId(Long requesterId);
    
    /**
     * 특정 관리자에게 배정된 AS 접수 목록 조회
     */
    List<ServiceRequest> findByManagerId(Long managerId);
    
    /**
     * 접수 완료되지 않은 AS 접수 목록 조회
     */
    List<ServiceRequest> findByIsReceivedFalse();
    
    /**
     * 완료되지 않은 AS 접수 목록 조회
     */
    List<ServiceRequest> findByIsCompletedFalse();
    
    /**
     * 특정 서비스 유형의 AS 접수 목록 조회
     */
    List<ServiceRequest> findByServiceTypeCodeId(String serviceTypeCode);
    
    /**
     * 특정 우선순위의 AS 접수 목록 조회
     */
    List<ServiceRequest> findByPriorityCodeId(String priorityCode);
    
    /**
     * 특정 날짜 범위 내의 AS 접수 목록 조회
     */
    List<ServiceRequest> findByRequestDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 특정 날짜 내에 예상 완료일인 AS 접수 목록 조회
     */
    List<ServiceRequest> findByExpectedCompletionDateAndIsCompletedFalse(LocalDateTime expectedCompletionDate);
    
    /**
     * 특정 날짜 범위 내에 예상 완료일인 AS 접수 목록 조회
     */
    List<ServiceRequest> findByExpectedCompletionDateBetweenAndIsCompletedFalse(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 접수 내용에 특정 키워드가 포함된 AS 접수 목록 조회
     */
    List<ServiceRequest> findByRequestContentContaining(String keyword);
    
    /**
     * 페이징으로 AS 접수 목록 조회
     */
    Page<ServiceRequest> findAll(Pageable pageable);
    
    /**
     * 특정 완료 여부로 페이징된 AS 접수 목록 조회
     */
    Page<ServiceRequest> findByIsCompleted(Boolean isCompleted, Pageable pageable);
    
    /**
     * 특정 접수 여부로 페이징된 AS 접수 목록 조회
     */
    Page<ServiceRequest> findByIsReceived(Boolean isReceived, Pageable pageable);
    
    /**
     * 전체 AS 비용 합계 조회
     */
    @Query("SELECT SUM(sr.cost) FROM ServiceRequest sr")
    Double getTotalServiceCost();
    
    /**
     * 특정 시설물의 전체 AS 비용 합계 조회
     */
    @Query("SELECT SUM(sr.cost) FROM ServiceRequest sr WHERE sr.facility.facilityId = :facilityId")
    Double getTotalServiceCostByFacility(@Param("facilityId") Long facilityId);
} 