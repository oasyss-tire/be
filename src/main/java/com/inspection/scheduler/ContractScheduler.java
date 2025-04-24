package com.inspection.scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.inspection.entity.Company;
import com.inspection.entity.CompanyTrusteeHistory;
import com.inspection.entity.Contract;
import com.inspection.repository.CompanyTrusteeHistoryRepository;
import com.inspection.service.ContractService;
import com.inspection.util.DateProvider;
import com.inspection.repository.CompanyRepository;
import com.inspection.repository.ContractRepository;
import com.inspection.repository.CodeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 계약 상태 관리 스케줄러
 * 매일 0시에 실행되어 계약 상태를 업데이트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContractScheduler {

    private final CompanyTrusteeHistoryRepository trusteeHistoryRepository;
    private final DateProvider dateProvider;
    private final ContractService contractService;
    private final CompanyRepository companyRepository;
    private final ContractRepository contractRepository;
    private final CodeRepository codeRepository;
    
    // 상태 코드 상수 정의
    private static final String CONTRACT_STATUS_EXPIRED = "001002_0007";  // 계약 만료 상태 코드
    
    /**
     * 매일 0시 5분에 계약 상태를 확인하고 업데이트
     * cron 표현식: 초 분 시 일 월 요일
     */
    
     @Scheduled(cron = "0 5 0 * * *")
    // @Scheduled(cron = "0 * * * * *")
    @Transactional
    public Map<String, Object> processContractStatus() {
        // 현재 날짜 확인 (테스트를 위해 DateProvider 사용)
        LocalDate today = dateProvider.getCurrentDate();
        log.info("계약 상태 업데이트 스케줄러 실행: {}", today);

        // 결과를 저장할 맵
        Map<String, Object> result = new HashMap<>();
        int deactivatedCount = 0;
        int activatedCount = 0;

        try {
            // 1. 보험 종료일이 지난 활성 수탁자 이력 및 관련 계약 비활성화
            deactivatedCount = deactivateExpiredContracts(today);
            
            // 2. 보험 시작일이 된 비활성 수탁자 이력 및 관련 계약 활성화
            activatedCount = activateNewContracts(today);

            result.put("status", "success");
            result.put("date", today.toString());
            result.put("deactivatedCount", deactivatedCount);
            result.put("activatedCount", activatedCount);
            
            log.info("계약 상태 업데이트 완료: 비활성화={}, 활성화={}", deactivatedCount, activatedCount);
        } catch (Exception e) {
            log.error("계약 상태 업데이트 중 오류 발생", e);
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * 보험 종료일이 지난 활성 수탁자 이력 및 관련 계약 비활성화
     */
    private int deactivateExpiredContracts(LocalDate today) {
        // 활성 상태이며 보험 종료일이 오늘 이전인 수탁자 이력 조회
        List<CompanyTrusteeHistory> expiredHistories = trusteeHistoryRepository.findByIsActiveTrueAndInsuranceEndDateBefore(today);
        
        if (expiredHistories.isEmpty()) {
            log.info("종료할 계약이 없습니다: {}", today);
            return 0;
        }

        // 계약 만료 상태 코드 조회
        var expiredStatusCode = codeRepository.findById(CONTRACT_STATUS_EXPIRED)
            .orElse(null);
        if (expiredStatusCode == null) {
            log.warn("계약 만료 상태 코드를 찾을 수 없습니다: {}", CONTRACT_STATUS_EXPIRED);
        }

        for (CompanyTrusteeHistory history : expiredHistories) {
            // 계약 종료일 로깅
            log.info("수탁자 이력 종료 처리: 회사={}, 수탁자={}, 보험종료일={}, 이력ID={}",
                    history.getCompany().getStoreName(),
                    history.getTrustee(),
                    history.getInsuranceEndDate(),
                    history.getId());

            // 비활성화 처리
            history.setActive(false);
            trusteeHistoryRepository.save(history);
            
            // 관련 계약도 비활성화 처리 (추가)
            if (history.getContract() != null && history.getContract().isActive()) {
                Contract contract = history.getContract();
                contract.setActive(false);
                contract.setLastModifiedAt(LocalDateTime.now());
                
                // 계약 상태 코드를 '계약 만료'로 변경
                if (expiredStatusCode != null) {
                    contract.setStatusCode(expiredStatusCode);
                    log.info("계약 상태 코드 변경: 계약ID={}, 상태='계약 만료'", contract.getId());
                }
                
                contractRepository.save(contract);
                log.info("계약 비활성화 처리: 계약ID={}, 계약명={}", contract.getId(), contract.getTitle());
            }
        }

        log.info("총 {}개 수탁자 이력 및 관련 계약 종료 처리 완료", expiredHistories.size());
        return expiredHistories.size();
    }

    /**
     * 보험 시작일이 된 비활성 수탁자 이력 및 관련 계약 활성화
     */
    private int activateNewContracts(LocalDate today) {
        // 비활성 상태이며 보험 시작일이 오늘인 수탁자 이력 조회
        List<CompanyTrusteeHistory> newHistories = trusteeHistoryRepository.findByIsActiveFalseAndInsuranceStartDate(today);
        
        if (newHistories.isEmpty()) {
            log.info("활성화할 계약이 없습니다: {}", today);
            return 0;
        }

        for (CompanyTrusteeHistory history : newHistories) {
            Company company = history.getCompany();
            
            log.info("수탁자 이력 활성화 처리: 회사={}, 수탁자={}, 보험시작일={}, 이력ID={}",
                    company.getStoreName(),
                    history.getTrustee(),
                    history.getInsuranceStartDate(),
                    history.getId());

            // 1. 먼저 기존 활성화된 이력이 있으면 비활성화
            trusteeHistoryRepository.findByCompanyAndIsActiveTrue(company)
                .ifPresent(activeHistory -> {
                    activeHistory.setActive(false);
                    trusteeHistoryRepository.save(activeHistory);
                    log.info("기존 활성 이력 비활성화: 회사={}, 수탁자={}, 이력ID={}", 
                        company.getStoreName(), activeHistory.getTrustee(), activeHistory.getId());
                });

            // 2. 새 이력 활성화
            history.setActive(true);
            
            // 3. Company 테이블에 수탁자 정보 업데이트
            history.applyToCompany(company);
            companyRepository.save(company);
            log.info("회사 정보 업데이트 완료: 회사ID={}, 수탁자={}", company.getId(), history.getTrustee());
            
            // 관련 계약 활성화 처리 (추가)
            if (history.getContract() != null && !history.getContract().isActive()) {
                Contract contract = history.getContract();
                contract.setActive(true);
                contract.setLastModifiedAt(LocalDateTime.now());
                contractRepository.save(contract);
                log.info("계약 활성화 처리: 계약ID={}, 계약명={}", contract.getId(), contract.getTitle());
            }
            
            // 4. 이력 저장
            trusteeHistoryRepository.save(history);
        }

        log.info("총 {}개 수탁자 이력 및 관련 계약 활성화 처리 완료", newHistories.size());
        return newHistories.size();
    }

    /**
     * 계약 만료 사전 알림 처리 (옵션)
     * 만료 7일 전 알림 등 필요시 구현
     */
    @Scheduled(cron = "0 10 0 * * *")
    public void notifyExpiringContracts() {
        LocalDate today = dateProvider.getCurrentDate();
        LocalDate expiryDate = today.plusDays(7); // 7일 후 만료 예정

        // 보험 종료일 기준으로 만료 예정 계약 조회로 변경
        List<CompanyTrusteeHistory> expiringHistories = 
            trusteeHistoryRepository.findByIsActiveTrueAndInsuranceEndDate(expiryDate);
        
        if (!expiringHistories.isEmpty()) {
            log.info("만료 예정 계약 알림 필요: {}건, 보험만료일={}", expiringHistories.size(), expiryDate);
            // TODO: 알림 로직 구현 (이메일, SMS 등)
        }
    }
} 