package com.inspection.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.inspection.entity.Facility;
import com.inspection.entity.FacilityStatus;
import java.util.List;

public interface FacilityRepository extends JpaRepository<Facility, Long> {
    List<Facility> findByCompany_CompanyId(Long companyId);
    List<Facility> findByStatus(FacilityStatus status);
    List<Facility> findByCurrentLocation(String location);

    Page<Facility> findByNameContainingOrCodeContaining(
        String name, String code, Pageable pageable);
    
    Page<Facility> findByStatus(FacilityStatus status, Pageable pageable);
    
    Page<Facility> findByCurrentLocation(String location, Pageable pageable);
} 