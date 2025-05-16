package com.inspection.facility.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.inspection.entity.Code;
import com.inspection.entity.Company;
import com.inspection.entity.User;
import com.inspection.facility.entity.DailyInventoryClosing;
import com.inspection.facility.repository.DailyInventoryClosingRepository;
import com.inspection.facility.repository.FacilityTransactionRepository;
import com.inspection.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchInventoryClosingService {

    private final DailyInventoryClosingRepository dailyClosingRepository;
    private final FacilityTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    /**
     * 특정 회사와 시설물 유형에 대한 일일 마감 처리를 비동기로 수행
     * @param company 회사 정보
     * @param facilityType 시설물 유형 정보
     * @param closingDate 마감 일자
     * @param currentProcessingTime 현재 처리 시간
     * @param userId 사용자 ID
     * @param existingClosingMap 기존 마감 데이터 맵 (key: companyId_facilityTypeId)
     * @return 마감 처리 결과
     */
    @Async("inventoryTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Future<DailyInventoryClosing> processCompanyTypeCombination(
            Company company, 
            Code facilityType, 
            LocalDate closingDate,
            LocalDateTime currentProcessingTime,
            String userId,
            Map<String, DailyInventoryClosing> existingClosingMap) {
        
        String key = company.getId() + "_" + facilityType.getCodeId();
        log.debug("비동기 처리 시작: {} ({})", key, Thread.currentThread().getName());
        
        try {
            // 사용자 조회
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("마감 처리자 정보를 찾을 수 없습니다: " + userId));
            
            // 기존 마감 데이터 확인
            DailyInventoryClosing closingData = existingClosingMap.get(key);
            
            if (closingData == null) {
                // 새 데이터 생성
                closingData = new DailyInventoryClosing();
                closingData.setClosingDate(closingDate);
                closingData.setCompany(company);
                closingData.setFacilityType(facilityType);
            }
            
            // 처리 시작 시간 설정
            closingData.setProcessStartTime(currentProcessingTime);
            
            // ID가 없으면 저장하여 ID 생성
            if (closingData.getId() == null) {
                closingData = dailyClosingRepository.save(closingData);
            }
            
            // 직전 날짜의 마감 데이터 조회
            LocalDate previousDay = closingDate.minusDays(1);
            Optional<DailyInventoryClosing> previousClosingOpt = dailyClosingRepository
                    .findByClosingDateAndCompanyIdAndFacilityTypeCodeIdAndIsClosed(
                            previousDay, company.getId(), facilityType.getCodeId(), true);
            
            // 전일 마감 수량과 마감 시간 설정
            int previousDayQuantity = 0;
            LocalDateTime lastClosingTime = LocalDateTime.of(2000, 1, 1, 0, 0);
            
            if (previousClosingOpt.isPresent()) {
                DailyInventoryClosing previousClosing = previousClosingOpt.get();
                previousDayQuantity = previousClosing.getClosingQuantity();
                lastClosingTime = previousClosing.getClosedAt() != null ? 
                        previousClosing.getClosedAt() : previousClosing.getCreatedAt();
            } else {
                // 더 이전 날짜의 마감 데이터 조회 (최적화된 단일 쿼리)
                Optional<DailyInventoryClosing> olderClosingOpt = dailyClosingRepository
                        .findTopByCompanyIdAndFacilityTypeCodeIdAndClosingDateBeforeAndIsClosedTrueOrderByClosingDateDesc(
                                company.getId(), facilityType.getCodeId(), closingDate);
                
                if (olderClosingOpt.isPresent()) {
                    DailyInventoryClosing olderClosing = olderClosingOpt.get();
                    previousDayQuantity = olderClosing.getClosingQuantity();
                    lastClosingTime = olderClosing.getClosedAt() != null ? 
                            olderClosing.getClosedAt() : olderClosing.getCreatedAt();
                }
            }
            
            // 입고 수량 계산
            int inboundQuantity = transactionRepository.countInboundTransactionsBetweenClosingTimes(
                    lastClosingTime, currentProcessingTime, company.getId(), facilityType.getCodeId());
            
            // 출고 수량 계산
            int outboundQuantity = transactionRepository.countOutboundTransactionsBetweenClosingTimes(
                    lastClosingTime, currentProcessingTime, company.getId(), facilityType.getCodeId());
            
            // 당일 마감 수량 계산
            int closingQuantity = previousDayQuantity + inboundQuantity - outboundQuantity;
            
            // 마감 데이터 업데이트
            closingData.setPreviousDayQuantity(previousDayQuantity);
            closingData.setInboundQuantity(inboundQuantity);
            closingData.setOutboundQuantity(outboundQuantity);
            closingData.setClosingQuantity(closingQuantity);
            closingData.setIsClosed(true);
            closingData.setClosedAt(LocalDateTime.now());
            closingData.setClosedBy(user);
            
            // 저장
            closingData = dailyClosingRepository.save(closingData);
            
            log.debug("비동기 처리 완료: {}", key);
            return CompletableFuture.completedFuture(closingData);
        } catch (Exception e) {
            log.error("마감 처리 중 오류: {} - {}", key, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 여러 회사-시설물 유형 조합에 대한 일괄 처리
     * @param companies 회사 목록
     * @param facilityTypes 시설물 유형 목록
     * @param closingDate 마감 일자
     * @param userId 사용자 ID
     * @return 처리된 조합 수
     */
    @Transactional
    public int processBatchClosing(
            List<Company> companies,
            List<Code> facilityTypes,
            LocalDate closingDate,
            String userId) {
        
        log.info("배치 마감 처리 시작: {} (회사 {}개, 시설물 유형 {}개)", closingDate, companies.size(), facilityTypes.size());
        long startTime = System.currentTimeMillis();
        
        // 현재 처리 시간 기록
        LocalDateTime currentProcessingTime = LocalDateTime.now();
        
        // 기존 마감 데이터 조회 및 맵으로 변환
        List<DailyInventoryClosing> existingClosings = dailyClosingRepository.findByClosingDate(closingDate);
        Map<String, DailyInventoryClosing> existingClosingMap = new HashMap<>();
        for (DailyInventoryClosing closing : existingClosings) {
            String key = closing.getCompany().getId() + "_" + closing.getFacilityType().getCodeId();
            existingClosingMap.put(key, closing);
        }
        
        // 비동기 작업 목록
        List<CompletableFuture<DailyInventoryClosing>> futures = new ArrayList<>();
        int totalCombinations = companies.size() * facilityTypes.size();
        
        log.info("총 처리할 조합 수: {}", totalCombinations);
        
        // 각 회사-시설물 유형 조합에 대해 비동기 처리 시작
        for (Company company : companies) {
            for (Code facilityType : facilityTypes) {
                Future<DailyInventoryClosing> future = processCompanyTypeCombination(
                        company, facilityType, closingDate, currentProcessingTime, userId, existingClosingMap);
                
                // CompletableFuture로 변환
                CompletableFuture<DailyInventoryClosing> completableFuture;
                if (future instanceof CompletableFuture) {
                    completableFuture = (CompletableFuture<DailyInventoryClosing>) future;
                } else {
                    completableFuture = CompletableFuture.supplyAsync(() -> {
                        try {
                            return future.get(3, TimeUnit.MINUTES);
                        } catch (Exception e) {
                            log.error("Future 변환 중 오류: {}", e.getMessage(), e);
                            throw new RuntimeException(e);
                        }
                    });
                }
                
                futures.add(completableFuture);
            }
        }
        
        // 모든 비동기 작업 완료 대기
        int completedCount = 0;
        int failedCount = 0;
        
        try {
            // 모든 작업 완료 대기 (최대 10분)
            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allOf.get(10, TimeUnit.MINUTES);
            
            // 결과 수집
            for (CompletableFuture<DailyInventoryClosing> future : futures) {
                try {
                    if (future.isDone() && !future.isCompletedExceptionally()) {
                        DailyInventoryClosing result = future.get();
                        if (result != null) {
                            completedCount++;
                        }
                    } else {
                        failedCount++;
                    }
                } catch (Exception e) {
                    log.error("비동기 작업 결과 처리 중 오류: {}", e.getMessage());
                    failedCount++;
                }
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("비동기 작업 대기 중 오류: {}", e.getMessage(), e);
            
            // 완료된 작업 수 계산
            completedCount = (int) futures.stream()
                    .filter(f -> f.isDone() && !f.isCompletedExceptionally())
                    .count();
            
            failedCount = totalCombinations - completedCount;
        }
        
        long endTime = System.currentTimeMillis();
        log.info("배치 마감 처리 완료: {}, 성공: {}, 실패: {}, 소요시간: {}ms", 
                closingDate, completedCount, failedCount, (endTime - startTime));
        
        return completedCount;
    }
    
    /**
     * 특정 회사 그룹에 대한 일일 마감 처리를 비동기로 수행 (회사별 배치 처리)
     * @param companies 회사 목록
     * @param facilityTypes 시설물 유형 목록
     * @param closingDate 마감 일자
     * @param userId 사용자 ID
     * @return 처리된 조합 수
     */
    @Async("inventoryTaskExecutor")
    public Future<Integer> processCompanyGroup(
            List<Company> companies,
            List<Code> facilityTypes,
            LocalDate closingDate,
            String userId) {
        
        log.debug("회사 그룹 처리 시작: 회사 {}개", companies.size());
        int processedCount = 0;
        
        try {
            // 현재 처리 시간 기록
            LocalDateTime currentProcessingTime = LocalDateTime.now();
            
            // 회사 ID 목록 추출
            List<Long> companyIds = companies.stream()
                    .map(Company::getId)
                    .collect(Collectors.toList());
            
            // 기존 마감 데이터를 한 번에 조회 (최적화된 쿼리 사용)
            List<DailyInventoryClosing> existingClosings = dailyClosingRepository
                    .findByClosingDateAndCompanyIdIn(closingDate, companyIds);
            
            // 기존 마감 데이터 맵 생성
            Map<String, DailyInventoryClosing> existingClosingMap = new HashMap<>();
            for (DailyInventoryClosing closing : existingClosings) {
                String key = closing.getCompany().getId() + "_" + closing.getFacilityType().getCodeId();
                existingClosingMap.put(key, closing);
            }
            
            // 사용자 조회
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("마감 처리자 정보를 찾을 수 없습니다: " + userId));
            
            // 이전 마감 데이터를 미리 조회 (회사별 시설물별 최근 마감 데이터)
            Map<String, DailyInventoryClosing> previousClosingMap = new HashMap<>();
            
            // 전일 마감 데이터를 한 번에 조회 (최적화된 쿼리 사용)
            LocalDate previousDay = closingDate.minusDays(1);
            List<DailyInventoryClosing> previousClosings = dailyClosingRepository
                    .findByClosingDateAndCompanyIdInAndIsClosed(previousDay, companyIds, true);
            
            // 전일 마감 데이터 맵 생성 (회사ID_시설물유형ID 형식의 키 사용)
            for (DailyInventoryClosing closing : previousClosings) {
                String key = closing.getCompany().getId() + "_" + closing.getFacilityType().getCodeId();
                previousClosingMap.put(key, closing);
            }
            
            // 일괄 저장을 위한 리스트
            List<DailyInventoryClosing> closingsToSave = new ArrayList<>();
            
            // 총 처리할 조합 수 계산 - 배치 사이즈 최적화에 사용
            int totalCombinations = companies.size() * facilityTypes.size();
            
            // 배치 크기 최적화: 총 조합 수에 따라 동적으로 배치 크기 결정
            int batchSize;
            if (totalCombinations > 5000) {
                batchSize = 500; // 대량 데이터
            } else if (totalCombinations > 1000) {
                batchSize = 300; // 중간 규모 데이터
            } else {
                batchSize = 150; // 소규모 데이터
            }
            
            log.debug("동적 배치 크기 설정: {}건 (총 예상 조합: {}건)", batchSize, totalCombinations);
            
            // 시설물 유형 코드 목록 추출
            List<String> facilityTypeCodes = facilityTypes.stream().map(Code::getCodeId).collect(Collectors.toList());
            
            // 성능 최적화: 모든 회사-시설물 유형 조합의 마감 시간 맵 미리 구성
            Map<String, LocalDateTime> lastClosingTimeMap = new HashMap<>();
            Map<String, Integer> previousQuantityMap = new HashMap<>();
            
            // 기본 마감 시간 (매우 오래된 시간)
            LocalDateTime defaultLastClosingTime = LocalDateTime.of(2000, 1, 1, 0, 0);
            
            // 1. 전일 마감 데이터의 시간과 수량 설정
            for (DailyInventoryClosing closing : previousClosings) {
                String key = closing.getCompany().getId() + "_" + closing.getFacilityType().getCodeId();
                previousQuantityMap.put(key, closing.getClosingQuantity());
                
                LocalDateTime closingTime = closing.getClosedAt() != null ? 
                        closing.getClosedAt() : closing.getCreatedAt();
                lastClosingTimeMap.put(key, closingTime);
            }
            
            // 2. 이전 마감 데이터가 없는 조합에 대해 예전 마감 데이터 조회 및 캐싱
            Set<String> keysToProcess = new HashSet<>();
            for (Company company : companies) {
                for (Code facilityType : facilityTypes) {
                    String key = company.getId() + "_" + facilityType.getCodeId();
                    if (!lastClosingTimeMap.containsKey(key)) {
                        keysToProcess.add(key);
                    }
                }
            }
            
            // 예전 마감 데이터 일괄 조회를 위한 최적화 쿼리 실행
            if (!keysToProcess.isEmpty()) {
                for (String key : keysToProcess) {
                    String[] parts = key.split("_");
                    Long companyId = Long.parseLong(parts[0]);
                    String facilityTypeCodeId = parts[1];
                    
                    // 더 이전 날짜의 마감 데이터 조회
                    Optional<DailyInventoryClosing> olderClosingOpt = dailyClosingRepository
                            .findTopByCompanyIdAndFacilityTypeCodeIdAndClosingDateBeforeAndIsClosedTrueOrderByClosingDateDesc(
                                    companyId, facilityTypeCodeId, closingDate);
                    
                    if (olderClosingOpt.isPresent()) {
                        DailyInventoryClosing olderClosing = olderClosingOpt.get();
                        previousQuantityMap.put(key, olderClosing.getClosingQuantity());
                        
                        LocalDateTime closingTime = olderClosing.getClosedAt() != null ? 
                                olderClosing.getClosedAt() : olderClosing.getCreatedAt();
                        lastClosingTimeMap.put(key, closingTime);
                    } else {
                        // 마감 데이터가 없는 경우 기본값 설정
                        previousQuantityMap.put(key, 0);
                        lastClosingTimeMap.put(key, defaultLastClosingTime);
                    }
                }
            }
            
            // 최적화를 위해 트랜잭션 통계를 회사/시설물 유형별 마감 시간을 고려하여 일괄 조회
            Map<String, int[]> transactionStatsMap = new HashMap<>();
            
            // 각 회사와 시설물 유형 조합에 대한 트랜잭션 조회를 최적화
            for (Company company : companies) {
                Long companyId = company.getId();
                
                for (Code facilityType : facilityTypes) {
                    String facilityTypeCodeId = facilityType.getCodeId();
                    String key = companyId + "_" + facilityTypeCodeId;
                    
                    // 해당 조합의 마감 시간 조회 (캐싱된 맵에서)
                    LocalDateTime lastClosingTime = lastClosingTimeMap.getOrDefault(key, defaultLastClosingTime);
                    
                    // 입고 수량 계산
                    int inboundQuantity = transactionRepository.countInboundTransactionsBetweenClosingTimes(
                            lastClosingTime, currentProcessingTime, companyId, facilityTypeCodeId);
                    
                    // 출고 수량 계산
                    int outboundQuantity = transactionRepository.countOutboundTransactionsBetweenClosingTimes(
                            lastClosingTime, currentProcessingTime, companyId, facilityTypeCodeId);
                    
                    // 결과 캐싱
                    transactionStatsMap.put(key, new int[] { inboundQuantity, outboundQuantity });
                }
            }
            
            // 각 회사와 시설물 유형 조합에 대한 마감 데이터 생성
            for (Company company : companies) {
                Long companyId = company.getId();
                
                for (Code facilityType : facilityTypes) {
                    String facilityTypeCodeId = facilityType.getCodeId();
                    String key = companyId + "_" + facilityTypeCodeId;
                    
                    // 기존 마감 데이터 확인
                    DailyInventoryClosing closingData = existingClosingMap.get(key);
                    
                    if (closingData == null) {
                        // 새 데이터 생성
                        closingData = new DailyInventoryClosing();
                        closingData.setClosingDate(closingDate);
                        closingData.setCompany(company);
                        closingData.setFacilityType(facilityType);
                    }
                    
                    // 처리 시작 시간 설정
                    closingData.setProcessStartTime(currentProcessingTime);
                    
                    // 일괄 저장을 위해 먼저 ID가 필요한 경우만 저장
                    if (closingData.getId() == null) {
                        closingData = dailyClosingRepository.save(closingData);
                    }
                    
                    // 캐싱된 이전 수량 사용
                    int previousDayQuantity = previousQuantityMap.getOrDefault(key, 0);
                    
                    // 캐싱된 트랜잭션 통계 데이터 사용
                    int inboundQuantity = 0;
                    int outboundQuantity = 0;
                    
                    if (transactionStatsMap.containsKey(key)) {
                        int[] stats = transactionStatsMap.get(key);
                        inboundQuantity = stats[0];
                        outboundQuantity = stats[1];
                    }
                    
                    // 당일 마감 수량 계산
                    int closingQuantity = previousDayQuantity + inboundQuantity - outboundQuantity;
                    
                    // 마감 데이터 업데이트
                    closingData.setPreviousDayQuantity(previousDayQuantity);
                    closingData.setInboundQuantity(inboundQuantity);
                    closingData.setOutboundQuantity(outboundQuantity);
                    closingData.setClosingQuantity(closingQuantity);
                    closingData.setIsClosed(true);
                    closingData.setClosedAt(LocalDateTime.now());
                    closingData.setClosedBy(user);
                    
                    closingsToSave.add(closingData);
                    processedCount++;
                    
                    // 배치 크기에 도달하면 일괄 저장 후 리스트 비우기
                    if (closingsToSave.size() >= batchSize) {
                        dailyClosingRepository.saveAll(closingsToSave);
                        closingsToSave.clear();
                    }
                }
            }
            
            // 남은 데이터 일괄 저장
            if (!closingsToSave.isEmpty()) {
                dailyClosingRepository.saveAll(closingsToSave);
            }
            
            log.debug("회사 그룹 처리 완료: 회사 {}개, 처리된 조합 {}개", companies.size(), processedCount);
            return CompletableFuture.completedFuture(processedCount);
            
        } catch (Exception e) {
            log.error("회사 그룹 처리 중 오류: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 회사를 그룹으로 나누어 병렬 처리하는 메서드
     * @param companies 회사 목록
     * @param facilityTypes 시설물 유형 목록
     * @param closingDate 마감 일자
     * @param userId 사용자 ID
     * @return 처리된 조합 수
     */
    @Transactional
    public int processGroupedBatchClosing(
            List<Company> companies,
            List<Code> facilityTypes,
            LocalDate closingDate,
            String userId) {
        
        log.info("그룹 배치 마감 처리 시작: {} (회사 {}개, 시설물 유형 {}개)", 
                closingDate, companies.size(), facilityTypes.size());
        long startTime = System.currentTimeMillis();
        
        // 회사를 그룹으로 나누기 (최적화된 그룹 크기)
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        
        // CPU 코어 수와 회사 수를 고려하여 최적의 그룹 수 계산
        int totalCompanies = companies.size();
        int idealCompaniesPerGroup; // 그룹당 최적의 회사 수
        int optimalGroupCount;
        
        // CPU 부하와 효율성을 고려한 최적 그룹화 전략
        if (totalCompanies <= availableProcessors) {
            // 회사 수가 코어 수보다 적으면 회사당 하나의 스레드 할당
            idealCompaniesPerGroup = 1;
            optimalGroupCount = totalCompanies;
        } else if (totalCompanies <= availableProcessors * 4) {
            // 회사 수가 코어 수의 4배 이내면 코어당 최대 4개 회사 처리
            optimalGroupCount = availableProcessors;
            idealCompaniesPerGroup = (int) Math.ceil((double) totalCompanies / optimalGroupCount);
        } else {
            // 회사 수가 많을 경우 작업 처리량과 오버헤드 균형을 고려
            // 코어당 약 2-4개 스레드가 일반적으로 효율적
            optimalGroupCount = Math.min(availableProcessors * 3, totalCompanies / 4);
            idealCompaniesPerGroup = (int) Math.ceil((double) totalCompanies / optimalGroupCount);
        }
        
        // 최종 그룹 크기 설정 (너무 크거나 작지 않도록)
        int groupSize = Math.max(1, Math.min(idealCompaniesPerGroup, 10));
        
        // 회사 목록을 그룹으로 나누기
        List<List<Company>> companyGroups = new ArrayList<>();
        for (int i = 0; i < companies.size(); i += groupSize) {
            int end = Math.min(companies.size(), i + groupSize);
            companyGroups.add(companies.subList(i, end));
        }
        
        log.info("회사를 {}개 그룹으로 나누어 처리합니다. (CPU 코어: {}, 그룹당 회사 수: {})",
                companyGroups.size(), availableProcessors, groupSize);
        
        // 각 그룹에 대해 비동기 처리 시작
        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        
        for (List<Company> companyGroup : companyGroups) {
            Future<Integer> future = processCompanyGroup(companyGroup, facilityTypes, closingDate, userId);
            
            // CompletableFuture로 변환
            CompletableFuture<Integer> completableFuture;
            if (future instanceof CompletableFuture) {
                completableFuture = (CompletableFuture<Integer>) future;
            } else {
                completableFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        return future.get(3, TimeUnit.MINUTES);
                    } catch (Exception e) {
                        log.error("Future 변환 중 오류: {}", e.getMessage());
                        throw new RuntimeException(e);
                    }
                });
            }
            
            futures.add(completableFuture);
        }
        
        // 모든 작업 완료 대기 및 결과 집계
        int totalProcessed = 0;
        
        try {
            // 모든 작업 완료 대기 (최대 10분)
            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allOf.get(10, TimeUnit.MINUTES);
            
            // 결과 수집
            for (CompletableFuture<Integer> future : futures) {
                try {
                    Integer result = future.get(30, TimeUnit.SECONDS);
                    if (result != null) {
                        totalProcessed += result;
                    }
                } catch (Exception e) {
                    log.error("그룹 처리 결과 대기 중 오류: {}", e.getMessage());
                }
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("그룹 처리 대기 중 오류: {}", e.getMessage());
            
            // 완료된 작업 수 계산
            for (CompletableFuture<Integer> future : futures) {
                if (future.isDone() && !future.isCompletedExceptionally()) {
                    try {
                        Integer result = future.get(100, TimeUnit.MILLISECONDS);
                        if (result != null) {
                            totalProcessed += result;
                        }
                    } catch (Exception ex) {
                        // 타임아웃 발생 시 무시
                    }
                }
            }
        }
        
        long endTime = System.currentTimeMillis();
        log.info("그룹 배치 마감 처리 완료: {}, 처리된 조합: {}, 소요시간: {}ms", 
                closingDate, totalProcessed, (endTime - startTime));
        
        return totalProcessed;
    }
} 