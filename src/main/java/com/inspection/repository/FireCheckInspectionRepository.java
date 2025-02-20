package com.inspection.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.inspection.entity.FireCheckInspection;
import java.time.LocalDate;
import java.util.List;

public interface FireCheckInspectionRepository extends JpaRepository<FireCheckInspection, Long> {
    List<FireCheckInspection> findByInspectionDate(LocalDate inspectionDate);
    List<FireCheckInspection> findByTargetNameContaining(String targetName);
    List<FireCheckInspection> findByInspectorName(String inspectorName);
} 