package com.inspection.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.inspection.entity.Contract;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    List<Contract> findByActiveTrueOrderByCreatedAtDesc();
    
    @Query("SELECT c.contractNumber FROM Contract c " +
           "WHERE c.contractNumber LIKE :prefix% " +
           "ORDER BY c.contractNumber DESC")
    List<String> findContractNumbersByPrefix(@Param("prefix") String prefix);
    
    boolean existsByContractNumber(String contractNumber);
} 