package com.inspection.facility.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inspection.entity.Code;
import com.inspection.entity.Company;
import com.inspection.entity.User;
import com.inspection.facility.dto.CurrentInventoryStatusDTO;
import com.inspection.facility.dto.InventoryStatusDTO;
import com.inspection.facility.entity.DailyInventoryClosing;
import com.inspection.facility.entity.FacilityTransaction;
import com.inspection.facility.entity.MonthlyInventoryClosing;
import com.inspection.facility.repository.DailyInventoryClosingRepository;
import com.inspection.facility.repository.FacilityTransactionRepository;
import com.inspection.facility.repository.MonthlyInventoryClosingRepository;
import com.inspection.repository.CodeRepository;
import com.inspection.repository.CompanyRepository;
import com.inspection.repository.UserRepository;
import com.inspection.facility.service.FacilityTransactionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryClosingService {

    private final DailyInventoryClosingRepository dailyClosingRepository;
    private final MonthlyInventoryClosingRepository monthlyClosingRepository;
    private final FacilityTransactionRepository transactionRepository;
    private final CompanyRepository companyRepository;
    private final CodeRepository codeRepository;
    private final UserRepository userRepository;

    /**
     * 특정 날짜의 일일 마감 처리
     * @param closingDate 마감 날짜
     * @param userId 마감 처리자 ID
     * @return 성공적으로 마감된 레코드 수
     */
    @Transactional
    public int processDailyClosing(LocalDate closingDate, String userId) {
        log.info("일일 마감 처리 시작: {}", closingDate);
        
        // 사용자 조회
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("마감 처리자 정보를 찾을 수 없습니다: " + userId));
        
        // 1. 해당 날짜의 기존 마감 데이터가 있는지 확인
        List<DailyInventoryClosing> existingClosings = dailyClosingRepository.findByClosingDate(closingDate);
        if (!existingClosings.isEmpty() && existingClosings.stream().anyMatch(DailyInventoryClosing::getIsClosed)) {
            log.warn("이미 마감된 날짜입니다: {}", closingDate);
            return 0;
        }
        
        // 2. 모든 회사 목록 조회
        List<Company> companies = companyRepository.findAll();
        
        // 3. 모든 시설물 유형 코드 조회 (시설물 유형 코드 그룹 ID는 '002001')
        List<Code> facilityTypes = codeRepository.findByCodeGroupGroupId("002001");
        
        List<DailyInventoryClosing> closingsToSave = new ArrayList<>();
        int processedCount = 0;
        
        // 현재 처리 시작 시간 기록
        LocalDateTime currentProcessingTime = LocalDateTime.now();
        
        // 4. 각 회사와 시설물 유형 조합에 대한 마감 데이터 생성
        for (Company company : companies) {
            for (Code facilityType : facilityTypes) {
                // 4.1 해당 조합의 기존 마감 데이터 확인
                Optional<DailyInventoryClosing> existingClosingOpt = dailyClosingRepository
                        .findByClosingDateAndCompanyIdAndFacilityTypeCodeId(
                                closingDate, company.getId(), facilityType.getCodeId());
                
                // 4.2 직전 날짜의 마감 데이터 조회 (이전 날짜 마감 데이터 찾기)
                // 가장 최근 마감 데이터가 아닌 정확히 직전 날짜의 마감 데이터를 찾음
                LocalDate previousDay = closingDate.minusDays(1); // 정확히 하루 전 날짜
                Optional<DailyInventoryClosing> previousClosingOpt = dailyClosingRepository
                        .findByClosingDateAndCompanyIdAndFacilityTypeCodeIdAndIsClosed(
                                previousDay, company.getId(), facilityType.getCodeId(), true);
                
                // 전일 마감 수량과 마감 시간 설정
                int previousDayQuantity = 0;
                LocalDateTime lastClosingTime = LocalDateTime.of(2000, 1, 1, 0, 0); // 매우 과거 시간
                
                if (previousClosingOpt.isPresent()) {
                    DailyInventoryClosing previousClosing = previousClosingOpt.get();
                    
                    // 직전 날짜의 마감 데이터이므로 별도 검증 없이 사용
                    previousDayQuantity = previousClosing.getClosingQuantity();
                    
                    // 이전 마감 시간 설정 (마감 시간이 없으면 생성 시간 사용)
                    lastClosingTime = previousClosing.getClosedAt() != null ? 
                            previousClosing.getClosedAt() : previousClosing.getCreatedAt();
                } else {
                    // 직전 날짜의 마감이 없으면, 더 이전 날짜의 마감 찾기 (최대 30일)
                    for (int i = 2; i <= 30; i++) {
                        LocalDate olderDay = closingDate.minusDays(i);
                        Optional<DailyInventoryClosing> olderClosingOpt = dailyClosingRepository
                                .findByClosingDateAndCompanyIdAndFacilityTypeCodeIdAndIsClosed(
                                        olderDay, company.getId(), facilityType.getCodeId(), true);
                        
                        if (olderClosingOpt.isPresent()) {
                            DailyInventoryClosing olderClosing = olderClosingOpt.get();
                            previousDayQuantity = olderClosing.getClosingQuantity();
                            lastClosingTime = olderClosing.getClosedAt() != null ? 
                                    olderClosing.getClosedAt() : olderClosing.getCreatedAt();
                            break;
                        }
                    }
                }
                
                // 4.3 마감 처리 시작 시간 기록을 위한 객체 생성/업데이트
                DailyInventoryClosing closingData;
                if (existingClosingOpt.isPresent()) {
                    // 기존 데이터 업데이트
                    closingData = existingClosingOpt.get();
                } else {
                    // 새 데이터 생성
                    closingData = new DailyInventoryClosing();
                    closingData.setClosingDate(closingDate);
                    closingData.setCompany(company);
                    closingData.setFacilityType(facilityType);
                }
                
                // 처리 시작 시간 설정
                closingData.setProcessStartTime(currentProcessingTime);
                closingData = dailyClosingRepository.save(closingData); // 저장하여 ID 생성
                
                // 4.4 마지막 마감 시간부터 현재 처리 시작 시간까지의 트랜잭션 조회
                // 입고 수량 계산
                int inboundQuantity = transactionRepository.countInboundTransactionsBetweenClosingTimes(
                        lastClosingTime, currentProcessingTime, company.getId(), facilityType.getCodeId());
                
                // 출고 수량 계산
                int outboundQuantity = transactionRepository.countOutboundTransactionsBetweenClosingTimes(
                        lastClosingTime, currentProcessingTime, company.getId(), facilityType.getCodeId());
                
                // 4.5 당일 마감 수량 계산
                int closingQuantity = previousDayQuantity + inboundQuantity - outboundQuantity;
                
                // 4.6 마감 데이터 업데이트
                closingData.setPreviousDayQuantity(previousDayQuantity);
                closingData.setInboundQuantity(inboundQuantity);
                closingData.setOutboundQuantity(outboundQuantity);
                closingData.setClosingQuantity(closingQuantity);
                closingData.setIsClosed(true);
                closingData.setClosedAt(LocalDateTime.now());
                closingData.setClosedBy(user);
                
                closingsToSave.add(closingData);
                processedCount++;
            }
        }
        
        // 5. 데이터 저장
        dailyClosingRepository.saveAll(closingsToSave);
        
        log.info("일일 마감 처리 완료: {}, 처리 건수: {}", closingDate, processedCount);
        return processedCount;
    }
    
    /**
     * 특정 연월의 월간 마감 처리
     * @param year 마감 연도
     * @param month 마감 월
     * @param userId 마감 처리자 ID
     * @return 성공적으로 마감된 레코드 수
     */
    @Transactional
    public int processMonthlyClosing(int year, int month, String userId) {
        log.info("월간 마감 처리 시작: {}-{}", year, month);
        
        // 사용자 조회
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("마감 처리자 정보를 찾을 수 없습니다: " + userId));
        
        // 1. 해당 연월의 기존 마감 데이터가 있는지 확인
        List<MonthlyInventoryClosing> existingClosings = monthlyClosingRepository.findByYearAndMonth(year, month);
        if (!existingClosings.isEmpty() && existingClosings.stream().anyMatch(MonthlyInventoryClosing::getIsClosed)) {
            log.warn("이미 마감된 연월입니다: {}-{}", year, month);
            return 0;
        }
        
        // 2. 해당 월의 시작일과 종료일 계산
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
        
        // 3. 해당 월의 모든 일별 마감 데이터 조회
        List<DailyInventoryClosing> dailyClosings = dailyClosingRepository.findByClosingDateBetween(
                startOfMonth, endOfMonth);
        
        // 4. 해당 월의 마지막 날이 마감되었는지 확인
        boolean isLastDayClosed = dailyClosings.stream()
                .anyMatch(closing -> closing.getClosingDate().equals(endOfMonth) && closing.getIsClosed());
        
        if (!isLastDayClosed) {
            log.warn("해당 월의 마지막 날({})이 아직 마감되지 않았습니다.", endOfMonth);
            return 0;
        }
        
        List<MonthlyInventoryClosing> closingsToSave = new ArrayList<>();
        int processedCount = 0;
        
        // 5. 일마감 데이터가 있는 경우의 처리
        if (!dailyClosings.isEmpty()) {
            // 5.1 회사 및 시설물 유형별로 일별 마감 데이터 그룹화
            Map<String, List<DailyInventoryClosing>> groupedClosings = dailyClosings.stream()
                    .filter(DailyInventoryClosing::getIsClosed) // 마감된 항목만 포함
                    .collect(Collectors.groupingBy(closing -> 
                            closing.getCompany().getId() + "_" + closing.getFacilityType().getCodeId()));
            
            // 5.2 각 그룹별로 월간 마감 데이터 생성
            for (Map.Entry<String, List<DailyInventoryClosing>> entry : groupedClosings.entrySet()) {
                List<DailyInventoryClosing> groupClosings = entry.getValue();
                if (groupClosings.isEmpty()) continue;
                
                // 그룹의 첫 번째 항목에서 회사와 시설물 유형 정보 추출
                DailyInventoryClosing sample = groupClosings.get(0);
                Company company = sample.getCompany();
                Code facilityType = sample.getFacilityType();
                
                MonthlyInventoryClosing closingData = processMonthlyClosingForCompanyAndType(
                        year, month, company, facilityType, user, groupClosings);
                
                closingsToSave.add(closingData);
                processedCount++;
            }
        }
        
        // 6. 전월에 마감 데이터가 있지만 당월에 일마감 데이터가 없는 경우 처리
        // 모든 회사와 시설물 유형 조합을 찾아 전월 마감 데이터가 있는지 확인
        int previousMonth = month == 1 ? 12 : month - 1;
        int previousYear = month == 1 ? year - 1 : year;
        
        List<MonthlyInventoryClosing> previousMonthClosings = monthlyClosingRepository.findByYearAndMonth(
                previousYear, previousMonth);
        
        for (MonthlyInventoryClosing prevClosing : previousMonthClosings) {
            // 이미 처리된 회사-시설물 유형 조합인지 확인
            String key = prevClosing.getCompany().getId() + "_" + prevClosing.getFacilityType().getCodeId();
            boolean alreadyProcessed = closingsToSave.stream()
                    .anyMatch(closing -> 
                            closing.getCompany().getId().equals(prevClosing.getCompany().getId()) &&
                            closing.getFacilityType().getCodeId().equals(prevClosing.getFacilityType().getCodeId()));
            
            // 아직 처리되지 않은 조합이면 0 입출고로 마감 데이터 생성
            if (!alreadyProcessed) {
                MonthlyInventoryClosing newClosing = new MonthlyInventoryClosing();
                newClosing.setYear(year);
                newClosing.setMonth(month);
                newClosing.setCompany(prevClosing.getCompany());
                newClosing.setFacilityType(prevClosing.getFacilityType());
                newClosing.setPreviousMonthQuantity(prevClosing.getClosingQuantity());
                newClosing.setTotalInboundQuantity(0);  // 입고량 0
                newClosing.setTotalOutboundQuantity(0); // 출고량 0
                newClosing.setClosingQuantity(prevClosing.getClosingQuantity()); // 전월 마감수량 그대로 유지
                newClosing.setIsClosed(true);
                newClosing.setClosedAt(LocalDateTime.now());
                newClosing.setClosedBy(user);
                
                closingsToSave.add(newClosing);
                processedCount++;
                
                log.info("트랜잭션 없는 월마감 생성: {}-{}, 회사: {}, 시설물유형: {}, 이월수량: {}",
                        year, month, prevClosing.getCompany().getId(), 
                        prevClosing.getFacilityType().getCodeId(), prevClosing.getClosingQuantity());
            }
        }
        
        // 7. 데이터 저장
        if (!closingsToSave.isEmpty()) {
            monthlyClosingRepository.saveAll(closingsToSave);
        }
        
        log.info("월간 마감 처리 완료: {}-{}, 처리된 레코드 수: {}", year, month, processedCount);
        return processedCount;
    }
    
    /**
     * 회사 및 시설물 유형별 월마감 데이터 생성/업데이트 처리
     */
    private MonthlyInventoryClosing processMonthlyClosingForCompanyAndType(
            int year, int month, Company company, Code facilityType, User user, 
            List<DailyInventoryClosing> groupClosings) {
        
        // 6.1 해당 조합의 기존 마감 데이터 확인
        Optional<MonthlyInventoryClosing> existingClosingOpt = monthlyClosingRepository
                .findByYearAndMonthAndCompanyIdAndFacilityTypeCodeId(
                        year, month, company.getId(), facilityType.getCodeId());
        
        // 6.2 전월 마감 데이터 조회
        Optional<MonthlyInventoryClosing> previousMonthClosingOpt;
        if (month == 1) {
            previousMonthClosingOpt = monthlyClosingRepository
                    .findByYearAndMonthAndCompanyIdAndFacilityTypeCodeId(
                            year - 1, 12, company.getId(), facilityType.getCodeId());
        } else {
            previousMonthClosingOpt = monthlyClosingRepository
                    .findByYearAndMonthAndCompanyIdAndFacilityTypeCodeId(
                            year, month - 1, company.getId(), facilityType.getCodeId());
        }
        
        // 전월 재고 수량 (없으면 0)
        int previousMonthQuantity = previousMonthClosingOpt
                .map(MonthlyInventoryClosing::getClosingQuantity)
                .orElse(0);
        
        // 6.3 일별 마감 합계로 월간 입고/출고 수량 계산
        int totalInboundQuantity = groupClosings.stream()
                .mapToInt(DailyInventoryClosing::getInboundQuantity)
                .sum();
        
        int totalOutboundQuantity = groupClosings.stream()
                .mapToInt(DailyInventoryClosing::getOutboundQuantity)
                .sum();
        
        // 6.4 월말 마감 수량 계산 (마지막 일의 마감 수량을 월말 마감 수량으로 사용)
        // 일별 마감이 날짜 순으로 정렬된 상태가 아닐 수 있으므로, 마지막 날짜를 찾아 해당 마감 수량 사용
        Optional<DailyInventoryClosing> lastDayClosing = groupClosings.stream()
                .filter(DailyInventoryClosing::getIsClosed)
                .max(Comparator.comparing(DailyInventoryClosing::getClosingDate));
                
        int closingQuantity = lastDayClosing
                .map(DailyInventoryClosing::getClosingQuantity)
                .orElse(previousMonthQuantity + totalInboundQuantity - totalOutboundQuantity);
        
        // 6.5 마감 데이터 저장 또는 업데이트
        MonthlyInventoryClosing closingData;
        
        if (existingClosingOpt.isPresent()) {
            // 기존 데이터 업데이트
            closingData = existingClosingOpt.get();
            closingData.setPreviousMonthQuantity(previousMonthQuantity);
            closingData.setTotalInboundQuantity(totalInboundQuantity);
            closingData.setTotalOutboundQuantity(totalOutboundQuantity);
            closingData.setClosingQuantity(closingQuantity);
            closingData.setIsClosed(true);
            closingData.setClosedAt(LocalDateTime.now());
            closingData.setClosedBy(user);
        } else {
            // 새 데이터 생성
            closingData = new MonthlyInventoryClosing();
            closingData.setYear(year);
            closingData.setMonth(month);
            closingData.setCompany(company);
            closingData.setFacilityType(facilityType);
            closingData.setPreviousMonthQuantity(previousMonthQuantity);
            closingData.setTotalInboundQuantity(totalInboundQuantity);
            closingData.setTotalOutboundQuantity(totalOutboundQuantity);
            closingData.setClosingQuantity(closingQuantity);
            closingData.setIsClosed(true);
            closingData.setClosedAt(LocalDateTime.now());
            closingData.setClosedBy(user);
        }
        
        return closingData;
    }
    
    /**
     * 특정 일자의 재고 상태 조회
     * @param date 조회할 날짜
     * @param companyId 회사 ID (선택적)
     * @param facilityTypeCodeId 시설물 유형 코드 ID (선택적)
     * @return 재고 상태 DTO 목록
     */
    public List<InventoryStatusDTO> getDailyInventoryStatus(LocalDate date, Long companyId, String facilityTypeCodeId) {
        List<DailyInventoryClosing> closings;
        
        if (companyId != null && facilityTypeCodeId != null) {
            // 특정 회사, 특정 시설물 유형에 대한 조회
            Optional<DailyInventoryClosing> closingOpt = dailyClosingRepository
                    .findByClosingDateAndCompanyIdAndFacilityTypeCodeId(date, companyId, facilityTypeCodeId);
            closings = closingOpt.map(List::of).orElse(List.of());
        } else if (companyId != null) {
            // 특정 회사의 모든 시설물 유형에 대한 조회
            closings = dailyClosingRepository.findByClosingDateAndCompanyId(date, companyId);
        } else {
            // 모든 회사, 모든 시설물 유형에 대한 조회
            closings = dailyClosingRepository.findByClosingDate(date);
        }
        
        return closings.stream()
                .map(this::convertToInventoryStatusDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 특정 연월의 재고 상태 조회
     * @param year 조회할 연도
     * @param month 조회할 월
     * @param companyId 회사 ID (선택적)
     * @param facilityTypeCodeId 시설물 유형 코드 ID (선택적)
     * @return 재고 상태 DTO 목록
     */
    public List<InventoryStatusDTO> getMonthlyInventoryStatus(int year, int month, Long companyId, String facilityTypeCodeId) {
        List<MonthlyInventoryClosing> closings;
        
        if (companyId != null && facilityTypeCodeId != null) {
            // 특정 회사, 특정 시설물 유형에 대한 조회
            Optional<MonthlyInventoryClosing> closingOpt = monthlyClosingRepository
                    .findByYearAndMonthAndCompanyIdAndFacilityTypeCodeId(year, month, companyId, facilityTypeCodeId);
            closings = closingOpt.map(List::of).orElse(List.of());
        } else if (companyId != null) {
            // 특정 회사의 모든 시설물 유형에 대한 조회
            closings = monthlyClosingRepository.findByYearAndMonthAndCompanyId(year, month, companyId);
        } else {
            // 모든 회사, 모든 시설물 유형에 대한 조회
            closings = monthlyClosingRepository.findByYearAndMonth(year, month);
        }
        
        return closings.stream()
                .map(this::convertToInventoryStatusDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 현재 시점의 재고 상태 조회
     * @param companyId 회사 ID (선택적)
     * @param facilityTypeCodeId 시설물 유형 코드 ID (선택적)
     * @return 현재 재고 상태 DTO 목록
     */
    public List<CurrentInventoryStatusDTO> getCurrentInventoryStatus(Long companyId, String facilityTypeCodeId) {
        LocalDate today = LocalDate.now();
        List<CurrentInventoryStatusDTO> result = new ArrayList<>();
        
        // 1. 회사 목록 조회
        List<Company> companies;
        if (companyId != null) {
            companies = companyRepository.findById(companyId)
                    .map(List::of)
                    .orElse(List.of());
        } else {
            companies = companyRepository.findAll();
        }
        
        // 2. 시설물 유형 목록 조회
        List<Code> facilityTypes;
        if (facilityTypeCodeId != null) {
            facilityTypes = codeRepository.findById(facilityTypeCodeId)
                    .map(List::of)
                    .orElse(List.of());
        } else {
            facilityTypes = codeRepository.findByCodeGroupGroupId("002001");
        }
        
        // 3. 조회 대상 조합(회사+시설물 유형) 식별을 위한 맵
        Map<String, Boolean> processedCombinations = new HashMap<>();
        
        // 4. 일마감 데이터 기반 재고 조회
        for (Company company : companies) {
            for (Code facilityType : facilityTypes) {
                String combinationKey = company.getId() + "_" + facilityType.getCodeId();
                
                // 4.1 가장 최근 일일 마감 데이터 조회
                Optional<DailyInventoryClosing> latestClosingOpt = dailyClosingRepository
                        .findTopByCompanyIdAndFacilityTypeCodeIdAndIsClosedTrueOrderByClosingDateDesc(
                                company.getId(), facilityType.getCodeId());
                
                // 마감 데이터가 있는 경우 처리
                if (latestClosingOpt.isPresent()) {
                    DailyInventoryClosing latestClosing = latestClosingOpt.get();
                    LocalDate latestClosingDate = latestClosing.getClosingDate();
                    LocalDateTime latestClosingTime = latestClosing.getClosedAt();
                    
                    // 최근 마감 이후부터 현재까지의 트랜잭션 집계
                    LocalDateTime currentTime = LocalDateTime.now();
                    
                    // 입고 트랜잭션 계산 (취소되지 않은 트랜잭션들)
                    int recentInbound = transactionRepository.countInboundTransactionsBetweenClosingTimes(
                            latestClosingTime, currentTime, company.getId(), facilityType.getCodeId());
                    
                    // 출고 트랜잭션 계산 (취소되지 않은 트랜잭션들)
                    int recentOutbound = transactionRepository.countOutboundTransactionsBetweenClosingTimes(
                            latestClosingTime, currentTime, company.getId(), facilityType.getCodeId());
                    
                    // 현재 재고 수량 계산
                    int currentQuantity = latestClosing.getClosingQuantity() + recentInbound - recentOutbound;
                    
                    // 결과 추가
                    result.add(CurrentInventoryStatusDTO.builder()
                            .companyId(company.getId())
                            .companyName(company.getStoreName())
                            .facilityTypeCodeId(facilityType.getCodeId())
                            .facilityTypeName(facilityType.getCodeName())
                            .latestClosingDate(latestClosingDate)
                            .baseQuantity(latestClosing.getClosingQuantity())
                            .recentInbound(recentInbound)
                            .recentOutbound(recentOutbound)
                            .currentQuantity(currentQuantity)
                            .build());
                    
                    // 처리 완료 표시
                    processedCombinations.put(combinationKey, true);
                }
            }
        }
        
        // 5. 마감 데이터는 없지만 트랜잭션이 있는 조합 조회
        // 5.1 모든 트랜잭션 집합 추출 (회사와 시설물 유형 조합)
        LocalDateTime veryPastTime = LocalDateTime.of(2000, 1, 1, 0, 0); // 매우 과거 시간
        LocalDateTime currentTime = LocalDateTime.now();
        
        // 각 회사의 트랜잭션 조회 (폐기 상태가 아닌 시설물 위주로)
        for (Company company : companies) {
            // 트랜잭션이 존재하는 시설물 가져오기 (폐기 상태가 아닌 시설물 중심)
            List<FacilityTransaction> allTransactions = transactionRepository.findActiveFacilitiesByCompanyIdAndTransactionDateAfter(
                    company.getId(), veryPastTime);
                    
            // 각 트랜잭션에서 시설물 유형 추출 및 중복 제거
            Set<String> facilityTypesInTransactions = allTransactions.stream()
                    .map(tx -> tx.getFacility().getFacilityType().getCodeId())
                    .collect(Collectors.toSet());
            
            // 시설물 유형 필터링 (특정 시설물 유형만 조회 요청된 경우)
            if (facilityTypeCodeId != null) {
                facilityTypesInTransactions.removeIf(typeId -> !typeId.equals(facilityTypeCodeId));
            }
            
            // 각 시설물 유형에 대해 마감 데이터가 없는 경우 처리
            for (String typeId : facilityTypesInTransactions) {
                String combinationKey = company.getId() + "_" + typeId;
                
                // 이미 처리된 조합은 건너뜀
                if (processedCombinations.containsKey(combinationKey)) {
                    continue;
                }
                
                // 해당 시설물 유형 코드 조회
                Code facilityType = codeRepository.findById(typeId).orElse(null);
                if (facilityType == null) continue;
                
                // 입고/출고 수량 계산
                int totalInbound = transactionRepository.countInboundTransactionsBetweenClosingTimes(
                        veryPastTime, currentTime, company.getId(), typeId);
                        
                int totalOutbound = transactionRepository.countOutboundTransactionsBetweenClosingTimes(
                        veryPastTime, currentTime, company.getId(), typeId);
                
                // 현재 재고 = 총 입고 - 총 출고
                int currentQuantity = totalInbound - totalOutbound;
                
                // 결과 추가 (마감 기준 수량 없음)
                result.add(CurrentInventoryStatusDTO.builder()
                        .companyId(company.getId())
                        .companyName(company.getStoreName())
                        .facilityTypeCodeId(typeId)
                        .facilityTypeName(facilityType.getCodeName())
                        .latestClosingDate(null)  // 마감 날짜 없음
                        .baseQuantity(0)          // 기준 수량 0
                        .recentInbound(totalInbound)
                        .recentOutbound(totalOutbound)
                        .currentQuantity(currentQuantity)
                        .build());
            }
        }
        
        return result;
    }
    
    /**
     * 특정 기간 동안의 재고 추이 조회
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param companyId 회사 ID (선택적)
     * @param facilityTypeCodeId 시설물 유형 코드 ID (선택적)
     * @return 날짜별 재고 상태 목록
     */
    public Map<LocalDate, List<InventoryStatusDTO>> getInventoryTrend(LocalDate startDate, LocalDate endDate, Long companyId, String facilityTypeCodeId) {
        List<DailyInventoryClosing> closings;
        
        if (companyId != null && facilityTypeCodeId != null) {
            // 특정 회사, 특정 시설물 유형에 대한 조회
            closings = dailyClosingRepository.findByClosingDateBetweenAndCompanyIdAndFacilityTypeCodeId(
                    startDate, endDate, companyId, facilityTypeCodeId);
        } else if (companyId != null) {
            // 특정 회사의 모든 시설물 유형에 대한 조회
            closings = dailyClosingRepository.findByClosingDateBetweenAndCompanyId(
                    startDate, endDate, companyId);
        } else {
            // 모든 회사, 모든 시설물 유형에 대한 조회
            closings = dailyClosingRepository.findByClosingDateBetween(startDate, endDate);
        }
        
        // 날짜별로 그룹화
        return closings.stream()
                .map(this::convertToInventoryStatusDTO)
                .collect(Collectors.groupingBy(InventoryStatusDTO::getClosingDate));
    }
    
    // DailyInventoryClosing 엔티티를 InventoryStatusDTO로 변환
    private InventoryStatusDTO convertToInventoryStatusDTO(DailyInventoryClosing closing) {
        InventoryStatusDTO dto = new InventoryStatusDTO();
        dto.setClosingId(closing.getId());
        dto.setClosingDate(closing.getClosingDate());
        dto.setCompanyId(closing.getCompany().getId());
        dto.setStoreCode(closing.getCompany().getStoreCode());
        dto.setCompanyName(closing.getCompany().getStoreName());
        dto.setFacilityTypeCodeId(closing.getFacilityType().getCodeId());
        dto.setFacilityTypeName(closing.getFacilityType().getCodeName());
        dto.setPreviousQuantity(closing.getPreviousDayQuantity());
        dto.setInboundQuantity(closing.getInboundQuantity());
        dto.setOutboundQuantity(closing.getOutboundQuantity());
        dto.setClosingQuantity(closing.getClosingQuantity());
        dto.setIsClosed(closing.getIsClosed());
        dto.setClosedAt(closing.getClosedAt());
        dto.setClosedBy(closing.getClosedBy() != null ? closing.getClosedBy().getId() : null);
        return dto;
    }
    
    // MonthlyInventoryClosing 엔티티를 InventoryStatusDTO로 변환
    private InventoryStatusDTO convertToInventoryStatusDTO(MonthlyInventoryClosing closing) {
        // 월간 마감의 경우 closingDate는 해당 월의 마지막 날로 설정
        LocalDate closingDate = LocalDate.of(closing.getYear(), closing.getMonth(), 1)
                .plusMonths(1).minusDays(1);
        
        InventoryStatusDTO dto = new InventoryStatusDTO();
        dto.setClosingId(closing.getId());
        dto.setClosingDate(closingDate);
        dto.setCompanyId(closing.getCompany().getId());
        dto.setStoreCode(closing.getCompany().getStoreCode());
        dto.setCompanyName(closing.getCompany().getStoreName());
        dto.setFacilityTypeCodeId(closing.getFacilityType().getCodeId());
        dto.setFacilityTypeName(closing.getFacilityType().getCodeName());
        dto.setPreviousQuantity(closing.getPreviousMonthQuantity());
        dto.setInboundQuantity(closing.getTotalInboundQuantity());
        dto.setOutboundQuantity(closing.getTotalOutboundQuantity());
        dto.setClosingQuantity(closing.getClosingQuantity());
        dto.setIsClosed(closing.getIsClosed());
        dto.setClosedAt(closing.getClosedAt());
        dto.setClosedBy(closing.getClosedBy() != null ? closing.getClosedBy().getId() : null);
        return dto;
    }

    /**
     * 특정 날짜의 일일 마감 재계산
     * @param closingDate 재계산할 날짜
     * @param userId 재계산 요청자 ID
     * @return 성공적으로 재계산된 레코드 수
     */
    @Transactional
    public int recalculateDailyClosing(LocalDate closingDate, String userId) {
        log.info("일일 마감 재계산 시작: {}", closingDate);
        
        // 1. 해당 일자의 마감 데이터 조회
        List<DailyInventoryClosing> existingClosings = dailyClosingRepository.findByClosingDate(closingDate);
        if (existingClosings.isEmpty()) {
            log.warn("재계산할 마감 데이터가 없습니다: {}", closingDate);
            return 0;
        }
        
        log.info("삭제할 마감 데이터 수: {}", existingClosings.size());
        
        // 2. 영향을 받는 월 마감 확인
        int closingYear = closingDate.getYear();
        int closingMonth = closingDate.getMonthValue();
        
        List<MonthlyInventoryClosing> monthlyClosings = monthlyClosingRepository
                .findByYearAndMonth(closingYear, closingMonth);
        
        // 3. 해당 월의 월마감이 있는 경우 확인
        boolean hasMonthlyClosing = !monthlyClosings.isEmpty() && 
                                   monthlyClosings.stream().anyMatch(MonthlyInventoryClosing::getIsClosed);
        
        if (hasMonthlyClosing) {
            log.warn("{}년 {}월의 월마감이 이미 완료되어 일마감 재계산이 불가능합니다. 먼저 월마감을 취소해주세요.", 
                    closingYear, closingMonth);
            throw new RuntimeException("월마감이 완료된 기간의 일마감은 재계산할 수 없습니다.");
        }
        
        // 4. 기존 마감 데이터 삭제
        dailyClosingRepository.deleteAll(existingClosings);
        log.info("기존 마감 데이터 삭제 완료: {}", closingDate);
        
        // 5. 마감 데이터 재계산 (기존 processDailyClosing 메서드 호출)
        int processedCount = processDailyClosing(closingDate, userId);
        log.info("일일 마감 재계산 완료: {}, 처리 건수: {}", closingDate, processedCount);
        
        return processedCount;
    }
} 