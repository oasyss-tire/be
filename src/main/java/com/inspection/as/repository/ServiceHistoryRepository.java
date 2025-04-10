package com.inspection.as.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.inspection.as.entity.ServiceHistory;
import com.inspection.as.entity.ServiceRequest;

@Repository
public interface ServiceHistoryRepository extends JpaRepository<ServiceHistory, Long>, JpaSpecificationExecutor<ServiceHistory> {
    
    // 특정 AS 접수에 대한 모든 작업 이력 조회
    List<ServiceHistory> findByServiceRequest(ServiceRequest serviceRequest);
    
    // 특정 AS 접수 ID에 대한 모든 작업 이력 조회
    List<ServiceHistory> findByServiceRequestServiceRequestId(Long serviceRequestId);
    
    // 특정 날짜 범위 내의 작업 이력 조회
    List<ServiceHistory> findByActionDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // 특정 작업 유형의 작업 이력 조회
    List<ServiceHistory> findByActionTypeCodeId(String actionTypeCode);
    
    // 특정 작업자가 수행한 작업 이력 조회
    List<ServiceHistory> findByPerformedById(Long performedById);
    
    // 특정 AS 접수에 대한 작업 이력 개수 조회
    long countByServiceRequestServiceRequestId(Long serviceRequestId);
    
    // 특정 내용을 포함하는 작업 이력 검색
    List<ServiceHistory> findByActionDescriptionContaining(String keyword);
    
    // 특정 부품을 사용한 작업 이력 검색
    List<ServiceHistory> findByPartsUsedContaining(String partName);
    
    /**
     * 최근 작업일 기준 이력 조회
     */
    List<ServiceHistory> findByActionDateGreaterThanEqualOrderByActionDateDesc(LocalDateTime fromDate);
    
    /**
     * 특정 시설물의 AS 이력 조회
     */
    List<ServiceHistory> findByServiceRequestFacilityFacilityId(Long facilityId);
} 