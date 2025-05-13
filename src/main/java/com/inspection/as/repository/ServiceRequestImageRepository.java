package com.inspection.as.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.inspection.as.entity.ServiceRequestImage;

@Repository
public interface ServiceRequestImageRepository extends JpaRepository<ServiceRequestImage, Long> {
    
    List<ServiceRequestImage> findByServiceRequestServiceRequestIdAndActiveTrue(Long serviceRequestId);
    
    List<ServiceRequestImage> findByServiceRequestServiceRequestIdAndImageTypeCodeIdAndActiveTrue(Long serviceRequestId, String imageTypeCode);
} 