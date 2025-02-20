package com.inspection.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.inspection.entity.Inspection;

@Repository
public interface InspectionRepository extends JpaRepository<Inspection, Long> {
    List<Inspection> findByCompany_CompanyId(Long companyId);
    Page<Inspection> findByCompany_CompanyId(Long companyId, Pageable pageable);
} 