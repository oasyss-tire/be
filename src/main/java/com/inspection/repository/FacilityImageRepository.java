package com.inspection.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.inspection.entity.FacilityImage;
import java.util.List;
import java.util.Optional;

public interface FacilityImageRepository extends JpaRepository<FacilityImage, Long> {
    Optional<FacilityImage> findByIdAndFacilityId(Long id, Long facilityId);
    List<FacilityImage> findByFacilityId(Long facilityId);
    void deleteByFacilityId(Long facilityId);
} 