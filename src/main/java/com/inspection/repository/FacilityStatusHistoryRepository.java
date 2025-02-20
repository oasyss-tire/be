package com.inspection.repository;

import com.inspection.entity.FacilityStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FacilityStatusHistoryRepository extends JpaRepository<FacilityStatusHistory, Long> {
    List<FacilityStatusHistory> findByFacilityIdOrderByStatusChangeDateDesc(Long facilityId);
} 