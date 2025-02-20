package com.inspection.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inspection.entity.FireSafetyInspection;

public interface FireSafetyInspectionRepository extends JpaRepository<FireSafetyInspection, Long> {
    List<FireSafetyInspection> findByCompany_CompanyId(Long companyId);
    List<FireSafetyInspection> findByWriter_UserId(Long userId);
    List<FireSafetyInspection> findByInspectionDateBetween(LocalDate startDate, LocalDate endDate);
    List<FireSafetyInspection> findByBuildingNameContaining(String buildingName);
} 