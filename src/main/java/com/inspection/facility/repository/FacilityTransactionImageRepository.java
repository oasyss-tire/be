package com.inspection.facility.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.inspection.facility.entity.FacilityTransactionImage;

@Repository
public interface FacilityTransactionImageRepository extends JpaRepository<FacilityTransactionImage, Long> {
    
    List<FacilityTransactionImage> findByTransactionTransactionIdAndActiveTrue(Long transactionId);
    
    void deleteByTransactionTransactionId(Long transactionId);
}
