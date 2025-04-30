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
    
    /**
     * 활성화된 계약 목록 조회
     */
    List<Contract> findByActiveTrue();
    
    /**
     * 활성화된 계약 정보와 기본 연관 엔티티들을 함께 조회 (N+1 문제 해결을 위한 페치 조인)
     */
    @Query("SELECT c FROM Contract c " +
           "LEFT JOIN FETCH c.trusteeHistory th " +
           "LEFT JOIN FETCH c.company co " +
           "LEFT JOIN FETCH c.statusCode sc " +
           "LEFT JOIN FETCH c.contractTypeCode ctc " +
           "WHERE c.active = true " +
           "ORDER BY c.createdAt DESC")
    List<Contract> findActiveContractsWithBasicDetails();
    
    /**
     * 비활성화된(만료된) 계약 정보와 기본 연관 엔티티들을 함께 조회
     */
    @Query("SELECT c FROM Contract c " +
           "LEFT JOIN FETCH c.trusteeHistory th " +
           "LEFT JOIN FETCH c.company co " +
           "LEFT JOIN FETCH c.statusCode sc " +
           "LEFT JOIN FETCH c.contractTypeCode ctc " +
           "WHERE c.active = false " +
           "ORDER BY c.createdAt DESC")
    List<Contract> findInactiveContractsWithBasicDetails();
    
    /**
     * 모든 계약 정보(활성화/비활성화 상관없이)와 기본 연관 엔티티들을 함께 조회
     */
    @Query("SELECT c FROM Contract c " +
           "LEFT JOIN FETCH c.trusteeHistory th " +
           "LEFT JOIN FETCH c.company co " +
           "LEFT JOIN FETCH c.statusCode sc " +
           "LEFT JOIN FETCH c.contractTypeCode ctc " +
           "ORDER BY c.createdAt DESC")
    List<Contract> findAllContractsWithBasicDetails();
    
    @Query("SELECT c.contractNumber FROM Contract c " +
           "WHERE c.contractNumber LIKE :prefix% " +
           "ORDER BY c.contractNumber DESC")
    List<String> findContractNumbersByPrefix(@Param("prefix") String prefix);
    
    /**
     * 특정 계약과 기본 연관 엔티티를 함께 조회 (N+1 문제 해결)
     */
    @Query("SELECT c FROM Contract c " +
           "LEFT JOIN FETCH c.trusteeHistory th " +
           "LEFT JOIN FETCH c.company co " +
           "LEFT JOIN FETCH c.statusCode sc " +
           "LEFT JOIN FETCH c.contractTypeCode ctc " +
           "WHERE c.id = :contractId")
    Contract findContractWithBasicDetailsById(@Param("contractId") Long contractId);
    
    /**
     * 특정 계약의 참여자 정보를 조회
     */
    @Query("SELECT c FROM Contract c " +
           "LEFT JOIN FETCH c.participants p " +
           "LEFT JOIN FETCH p.statusCode psc " +
           "WHERE c.id = :contractId")
    Contract findContractWithParticipantsById(@Param("contractId") Long contractId);
    
    /**
     * 특정 계약의 템플릿 매핑 정보를 조회
     */
    @Query("SELECT c FROM Contract c " +
           "LEFT JOIN FETCH c.templateMappings tm " +
           "LEFT JOIN FETCH tm.template t " +
           "WHERE c.id = :contractId")
    Contract findContractWithTemplateMappingsById(@Param("contractId") Long contractId);
    
    boolean existsByContractNumber(String contractNumber);
    
    // 이벤트 로그 조회용 메서드
    
    /**
     * 특정 회사에 속한 계약 목록 조회
     */
    List<Contract> findByCompanyId(Long companyId);
    
    /**
     * 계약 제목으로 검색 (부분 일치)
     */
    List<Contract> findByTitleContaining(String title);
    
    /**
     * 계약 번호로 검색 (부분 일치)
     */
    List<Contract> findByContractNumberContaining(String contractNumber);
    
    /**
     * 계약 제목 또는 계약 번호로 검색 (부분 일치)
     */
    List<Contract> findByTitleContainingOrContractNumberContaining(String title, String contractNumber);
    
    /**
     * 계약 상태 코드로 검색
     */
    List<Contract> findByStatusCodeCodeId(String statusCodeId);
    
    /**
     * 계약 제목과 상태 코드로 검색
     */
    List<Contract> findByTitleContainingAndStatusCodeCodeId(String title, String statusCodeId);
} 