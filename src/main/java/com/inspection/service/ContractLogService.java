package com.inspection.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inspection.dto.CompanyLogDTO;
import com.inspection.dto.ContractLogDTO;
import com.inspection.entity.Company;
import com.inspection.entity.Contract;
import com.inspection.repository.CompanyRepository;
import com.inspection.repository.ContractRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 계약 이력 조회를 위한 서비스
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ContractLogService {

    private final CompanyRepository companyRepository;
    private final ContractRepository contractRepository;
    
    /**
     * 회사 목록 조회 (간략 정보)
     */
    public List<CompanyLogDTO> getCompanyList(String keyword) {
        log.info("이벤트 로그용 회사 목록 조회 (키워드: {})", keyword);
        
        List<Company> companies;
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 키워드 검색 (매장명, 사업자번호, 매장코드 등으로 검색)
            companies = companyRepository.findByStoreNameContainingOrBusinessNumberContainingOrStoreCodeContaining(
                    keyword, keyword, keyword);
        } else {
            // 전체 목록 조회 (활성화된 회사만)
            companies = companyRepository.findByActiveTrue();
        }
        
        return companies.stream()
                .map(CompanyLogDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 특정 회사의 계약 목록 조회
     */
    public List<ContractLogDTO> getContractsByCompanyId(Long companyId) {
        log.info("회사 ID {}의 계약 목록 조회", companyId);
        
        List<Contract> contracts = contractRepository.findByCompanyId(companyId);
        
        return contracts.stream()
                .map(ContractLogDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 모든 계약 목록 조회 (간략 정보)
     */
    public List<ContractLogDTO> getAllContracts() {
        log.info("모든 계약 목록 조회");
        
        List<Contract> contracts = contractRepository.findAll();
        
        return contracts.stream()
                .map(ContractLogDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 검색 조건에 따른 계약 목록 조회
     */
    public List<ContractLogDTO> searchContracts(String keyword, String statusCodeId) {
        log.info("계약 검색 (키워드: {}, 상태: {})", keyword, statusCodeId);
        
        List<Contract> contracts;
        
        if (keyword != null && !keyword.trim().isEmpty() && statusCodeId != null && !statusCodeId.trim().isEmpty()) {
            // 키워드와 상태로 검색
            contracts = contractRepository.findByTitleContainingAndStatusCodeCodeId(keyword, statusCodeId);
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            // 키워드로만 검색
            contracts = contractRepository.findByTitleContainingOrContractNumberContaining(keyword, keyword);
        } else if (statusCodeId != null && !statusCodeId.trim().isEmpty()) {
            // 상태로만 검색
            contracts = contractRepository.findByStatusCodeCodeId(statusCodeId);
        } else {
            // 모든 계약 조회
            contracts = contractRepository.findAll();
        }
        
        return contracts.stream()
                .map(ContractLogDTO::fromEntity)
                .collect(Collectors.toList());
    }
} 