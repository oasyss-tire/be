package com.inspection.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.inspection.entity.FacilityContract;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FacilityContractRepository extends JpaRepository<FacilityContract, Long> {
    List<FacilityContract> findByFacilityId(Long facilityId);
    List<FacilityContract> findByVendorName(String vendorName);
    List<FacilityContract> findByEndDateBefore(LocalDate date);
    List<FacilityContract> findByIsPaidFalse();
    Optional<FacilityContract> findByIdAndFacilityId(Long id, Long facilityId);
} 