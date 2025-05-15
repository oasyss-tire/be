package com.inspection.facility.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    // 캐시 맵 (회사ID_시설물유형ID -> 최근 마감 데이터)
    private final Map<String, DailyInventoryClosing> latestClosingCache = new HashMap<>();
    private LocalDateTime cacheLastUpdated = LocalDateTime.now().minusHours(1); // 초기값은 1시간 전
    private final long CACHE_EXPIRY_SECONDS = 300; // 5분 캐시 만료
    private boolean isCacheInitialized = false;

    /**
     * 캐시 초기화 메서드
     * 자주 조회되는 회사와 시설물 유형에 대한 마감 데이터를 미리 로드
     */
    private synchronized void initializeCache() {
        if (isCacheInitialized) {
            return;
        }
        
        try {
            log.info("캐시 초기화 시작...");
            long startTime = System.currentTimeMillis();
            
            // 모든 회사 조회
            List<Company> companies = companyRepository.findAll();
            
            // 모든 시설물 유형 조회
            List<Code> facilityTypes = codeRepository.findByCodeGroupGroupId("002001");
            
            // 주요 회사-시설물 유형 조합에 대한 최신 마감 데이터 미리 로드
            for (Company company : companies) {
                for (Code facilityType : facilityTypes) {
                    String key = company.getId() + "_" + facilityType.getCodeId();
                    
                    // DB에서 최신 마감 데이터 조회
                    Optional<DailyInventoryClosing> latestClosingOpt = dailyClosingRepository
                            .findTopByCompanyIdAndFacilityTypeCodeIdAndIsClosedTrueOrderByClosingDateDesc(
                                    company.getId(), facilityType.getCodeId());
                    
                    // 캐시에 저장
                    latestClosingOpt.ifPresent(closing -> latestClosingCache.put(key, closing));
                }
            }
            
            cacheLastUpdated = LocalDateTime.now();
            isCacheInitialized = true;
            
            long endTime = System.currentTimeMillis();
            log.info("캐시 초기화 완료: {}개 항목 로드, 소요시간: {}ms", 
                    latestClosingCache.size(), (endTime - startTime));
        } catch (Exception e) {
            log.error("캐시 초기화 중 오류: {}", e.getMessage());
        }
    }
    
    /**
     * 특정 날짜의 일일 마감 처리
     * @param closingDate 마감 날짜
     * @param userId 마감 처리자 ID
     * @return 성공적으로 마감된 레코드 수
     */
    @Transactional
    public int processDailyClosing(LocalDate closingDate, String userId) {
        log.info("일일 마감 처리 시작: {}", closingDate);
        long startTime = System.currentTimeMillis();
        
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
        
        log.info("마감 처리 준비 완료: 회사 {}개, 시설물 유형 {}개", companies.size(), facilityTypes.size());
        
        // 기존 마감 데이터를 맵으로 변환하여 조회 속도 향상
        Map<String, DailyInventoryClosing> existingClosingMap = new HashMap<>();
        for (DailyInventoryClosing closing : existingClosings) {
            String key = closing.getCompany().getId() + "_" + closing.getFacilityType().getCodeId();
            existingClosingMap.put(key, closing);
        }
        
        // 이전 마감 데이터와 시간을 캐싱하기 위한 맵
        Map<String, Integer> previousQuantityCache = new HashMap<>();
        Map<String, LocalDateTime> lastClosingTimeCache = new HashMap<>();
        
        List<DailyInventoryClosing> closingsToSave = new ArrayList<>();
        int processedCount = 0;
        int batchSize = 100;  // 배치 저장 크기
        
        // 현재 처리 시작 시간 기록
        LocalDateTime currentProcessingTime = LocalDateTime.now();
        
        // 4. 각 회사와 시설물 유형 조합에 대한 마감 데이터 생성
        for (Company company : companies) {
            for (Code facilityType : facilityTypes) {
                String key = company.getId() + "_" + facilityType.getCodeId();
                
                // 4.1 해당 조합의 기존 마감 데이터 확인 (캐시된 맵 활용)
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
                
                // 4.2 이전 마감 데이터 조회 (캐시 먼저 확인)
                int previousDayQuantity = 0;
                LocalDateTime lastClosingTime = LocalDateTime.of(2000, 1, 1, 0, 0); // 기본값
                
                if (previousQuantityCache.containsKey(key)) {
                    // 캐시에서 가져오기
                    previousDayQuantity = previousQuantityCache.get(key);
                    lastClosingTime = lastClosingTimeCache.get(key);
                } else {
                    // 직전 날짜의 마감 데이터를 효율적으로 조회
                    LocalDate previousDay = closingDate.minusDays(1);
                    Optional<DailyInventoryClosing> previousClosingOpt = dailyClosingRepository
                            .findByClosingDateAndCompanyIdAndFacilityTypeCodeIdAndIsClosed(
                                    previousDay, company.getId(), facilityType.getCodeId(), true);
                    
                    if (previousClosingOpt.isPresent()) {
                        DailyInventoryClosing previousClosing = previousClosingOpt.get();
                        previousDayQuantity = previousClosing.getClosingQuantity();
                        lastClosingTime = previousClosing.getClosedAt() != null ? 
                                previousClosing.getClosedAt() : previousClosing.getCreatedAt();
                    } else {
                        // 이전 날짜를 일괄 조회 (루프 최적화)
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
                    
                    // 결과를 캐시에 저장
                    previousQuantityCache.put(key, previousDayQuantity);
                    lastClosingTimeCache.put(key, lastClosingTime);
                }
                
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
                
                // 배치 크기에 도달하면 일괄 저장 후 리스트 비우기
                if (closingsToSave.size() >= batchSize) {
                    dailyClosingRepository.saveAll(closingsToSave);
                    log.info("일마감 처리 진행 중: {}개 완료", processedCount);
                    closingsToSave.clear();
                }
            }
        }
        
        // 5. 남은 데이터 일괄 저장
        if (!closingsToSave.isEmpty()) {
            dailyClosingRepository.saveAll(closingsToSave);
        }
        
        long endTime = System.currentTimeMillis();
        log.info("일일 마감 처리 완료: {}, 처리 건수: {}, 소요시간: {}ms", closingDate, processedCount, (endTime - startTime));
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
     * @param pageable 페이징 정보 (선택적)
     * @return 페이징된 재고 상태 DTO
     */
    public Page<InventoryStatusDTO> getDailyInventoryStatus(LocalDate date, Long companyId, String facilityTypeCodeId, Pageable pageable) {
        log.info("일별 재고 현황 조회 요청(페이징): {}, 회사ID: {}, 시설물유형: {}", date, companyId, facilityTypeCodeId);
        long startTime = System.currentTimeMillis();
        
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
        
        // DTO로 변환
        List<InventoryStatusDTO> statusList = closings.stream()
                .map(this::convertToInventoryStatusDTO)
                .collect(Collectors.toList());
        
        // 정렬 적용
        if (pageable.getSort().isSorted()) {
            pageable.getSort().forEach(order -> {
                String property = order.getProperty();
                Sort.Direction direction = order.getDirection();
                
                Comparator<InventoryStatusDTO> comparator = null;
                switch (property) {
                    case "companyName":
                        comparator = Comparator.comparing(InventoryStatusDTO::getCompanyName,
                                Comparator.nullsLast(String::compareTo));
                        break;
                    case "facilityTypeName":
                        comparator = Comparator.comparing(InventoryStatusDTO::getFacilityTypeName,
                                Comparator.nullsLast(String::compareTo));
                        break;
                    case "closingQuantity":
                        comparator = Comparator.comparing(InventoryStatusDTO::getClosingQuantity,
                                Comparator.nullsLast(Integer::compareTo));
                        break;
                    default:
                        comparator = Comparator.comparing(InventoryStatusDTO::getCompanyName,
                                Comparator.nullsLast(String::compareTo));
                }
                
                if (direction.isDescending()) {
                    comparator = comparator.reversed();
                }
                
                Collections.sort(statusList, comparator);
            });
        }
        
        // 페이징 적용
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), statusList.size());
        
        if (start > end) {
            start = 0;
            end = Math.min(pageable.getPageSize(), statusList.size());
        }
        
        List<InventoryStatusDTO> pageContent = statusList.subList(start, end);
        Page<InventoryStatusDTO> page = new PageImpl<>(pageContent, pageable, statusList.size());
        
        long endTime = System.currentTimeMillis();
        log.info("일별 재고 현황 조회 완료: 총 {}개 중 {}개 결과, 소요시간: {}ms", 
                statusList.size(), pageContent.size(), (endTime - startTime));
        
        return page;
    }
    
    /**
     * 특정 연월의 재고 상태 조회
     * @param year 조회할 연도
     * @param month 조회할 월
     * @param companyId 회사 ID (선택적)
     * @param facilityTypeCodeId 시설물 유형 코드 ID (선택적)
     * @param pageable 페이징 정보 (선택적)
     * @return 페이징된 재고 상태 DTO
     */
    public Page<InventoryStatusDTO> getMonthlyInventoryStatus(int year, int month, Long companyId, String facilityTypeCodeId, Pageable pageable) {
        log.info("월별 재고 현황 조회 요청(페이징): {}-{}, 회사ID: {}, 시설물유형: {}", year, month, companyId, facilityTypeCodeId);
        long startTime = System.currentTimeMillis();
        
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
        
        // DTO로 변환
        List<InventoryStatusDTO> statusList = closings.stream()
                .map(this::convertToInventoryStatusDTO)
                .collect(Collectors.toList());
        
        // 정렬 적용
        if (pageable.getSort().isSorted()) {
            pageable.getSort().forEach(order -> {
                String property = order.getProperty();
                Sort.Direction direction = order.getDirection();
                
                Comparator<InventoryStatusDTO> comparator = null;
                switch (property) {
                    case "companyName":
                        comparator = Comparator.comparing(InventoryStatusDTO::getCompanyName,
                                Comparator.nullsLast(String::compareTo));
                        break;
                    case "facilityTypeName":
                        comparator = Comparator.comparing(InventoryStatusDTO::getFacilityTypeName,
                                Comparator.nullsLast(String::compareTo));
                        break;
                    case "closingQuantity":
                        comparator = Comparator.comparing(InventoryStatusDTO::getClosingQuantity,
                                Comparator.nullsLast(Integer::compareTo));
                        break;
                    default:
                        comparator = Comparator.comparing(InventoryStatusDTO::getCompanyName,
                                Comparator.nullsLast(String::compareTo));
                }
                
                if (direction.isDescending()) {
                    comparator = comparator.reversed();
                }
                
                Collections.sort(statusList, comparator);
            });
        }
        
        // 페이징 적용
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), statusList.size());
        
        if (start > end) {
            start = 0;
            end = Math.min(pageable.getPageSize(), statusList.size());
        }
        
        List<InventoryStatusDTO> pageContent = statusList.subList(start, end);
        Page<InventoryStatusDTO> page = new PageImpl<>(pageContent, pageable, statusList.size());
        
        long endTime = System.currentTimeMillis();
        log.info("월별 재고 현황 조회 완료: 총 {}개 중 {}개 결과, 소요시간: {}ms", 
                statusList.size(), pageContent.size(), (endTime - startTime));
        
        return page;
    }
    
    /**
     * 특정 일자의 재고 상태 조회 (페이징 없는 버전 - 하위 호환성 유지)
     * @param date 조회할 날짜
     * @param companyId 회사 ID (선택적)
     * @param facilityTypeCodeId 시설물 유형 코드 ID (선택적)
     * @return 재고 상태 DTO 목록
     */
    public List<InventoryStatusDTO> getDailyInventoryStatus(LocalDate date, Long companyId, String facilityTypeCodeId) {
        // PageRequest.of(0, Integer.MAX_VALUE)를 사용하여 모든 결과 가져오기
        return getDailyInventoryStatus(date, companyId, facilityTypeCodeId, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
    }
    
    /**
     * 특정 연월의 재고 상태 조회 (페이징 없는 버전 - 하위 호환성 유지)
     * @param year 조회할 연도
     * @param month 조회할 월
     * @param companyId 회사 ID (선택적)
     * @param facilityTypeCodeId 시설물 유형 코드 ID (선택적)
     * @return 재고 상태 DTO 목록
     */
    public List<InventoryStatusDTO> getMonthlyInventoryStatus(int year, int month, Long companyId, String facilityTypeCodeId) {
        // PageRequest.of(0, Integer.MAX_VALUE)를 사용하여 모든 결과 가져오기
        return getMonthlyInventoryStatus(year, month, companyId, facilityTypeCodeId, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
    }
    
    /**
     * 캐시된 최신 마감 데이터를 가져오는 메서드
     * @param companyId 회사 ID
     * @param facilityTypeCodeId 시설물 유형 코드 ID
     * @return 최신 마감 데이터 (Optional)
     */
    private Optional<DailyInventoryClosing> getCachedLatestClosing(Long companyId, String facilityTypeCodeId) {
        // 캐시가 초기화되지 않았으면 초기화
        if (!isCacheInitialized) {
            initializeCache();
        }
        
        String key = companyId + "_" + facilityTypeCodeId;
        
        // 캐시가 만료되었는지 확인
        if (LocalDateTime.now().isAfter(cacheLastUpdated.plusSeconds(CACHE_EXPIRY_SECONDS))) {
            log.trace("캐시가 만료되어 초기화합니다.");
            synchronized (this) {
                if (LocalDateTime.now().isAfter(cacheLastUpdated.plusSeconds(CACHE_EXPIRY_SECONDS))) {
                    isCacheInitialized = false;
                    latestClosingCache.clear();
                    initializeCache();
                }
            }
        }
        
        // 캐시에 있으면 반환
        if (latestClosingCache.containsKey(key)) {
            return Optional.ofNullable(latestClosingCache.get(key));
        }
        
        // 캐시에 없으면 DB에서 조회
        try {
            Optional<DailyInventoryClosing> latestClosingOpt = dailyClosingRepository
                    .findTopByCompanyIdAndFacilityTypeCodeIdAndIsClosedTrueOrderByClosingDateDesc(
                            companyId, facilityTypeCodeId);
            
            // 캐시에 저장
            latestClosingOpt.ifPresent(closing -> {
                synchronized (latestClosingCache) {
                    latestClosingCache.put(key, closing);
                }
            });
            
            return latestClosingOpt;
        } catch (Exception e) {
            log.error("최신 마감 데이터 조회 중 오류: 회사={}, 시설물유형={}, 오류={}", 
                    companyId, facilityTypeCodeId, e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * 현재 시점의 재고 상태 조회
     * @param companyId 회사 ID (선택적)
     * @param facilityTypeCodeId 시설물 유형 코드 ID (선택적)
     * @return 현재 재고 상태 DTO 목록
     */
    public List<CurrentInventoryStatusDTO> getCurrentInventoryStatus(Long companyId, String facilityTypeCodeId) {
        log.info("현재 재고 현황 조회 요청: 회사ID: {}, 시설물유형: {}", companyId, facilityTypeCodeId);
        // 캐시를 사용하는 메서드 호출
        return getCurrentInventoryStatusCached(companyId, facilityTypeCodeId);
    }
    
    /**
     * 현재 시점의 재고 상태 조회 (캐시 사용)
     * @param companyId 회사 ID (선택적)
     * @param facilityTypeCodeId 시설물 유형 코드 ID (선택적)
     * @return 현재 재고 상태 DTO 목록
     */
    public List<CurrentInventoryStatusDTO> getCurrentInventoryStatusCached(Long companyId, String facilityTypeCodeId) {
        log.info("캐시 사용 현재 재고 현황 조회 요청: 회사ID: {}, 시설물유형: {}", companyId, facilityTypeCodeId);
        long startTime = System.currentTimeMillis();
        
        List<CurrentInventoryStatusDTO> result = new ArrayList<>();
        
        try {
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
            
            log.debug("조회 대상: 회사 {}개, 시설물 유형 {}개", companies.size(), facilityTypes.size());
            
            if (companies.isEmpty()) {
                log.warn("조회할 회사가 없습니다. 회사ID: {}", companyId);
                return createEmptyResultWithDefaultData(facilityTypes);
            }
            
            if (facilityTypes.isEmpty()) {
                log.warn("조회할 시설물 유형이 없습니다. 시설물유형ID: {}", facilityTypeCodeId);
                return createEmptyResultWithDefaultData(null);
            }
            
            // 3. 현재 시간 (모든 트랜잭션에 공통으로 사용)
            LocalDateTime currentTime = LocalDateTime.now();
            
            // 4. 각 회사와 시설물 유형 조합에 대해 처리
            for (Company company : companies) {
                for (Code facilityType : facilityTypes) {
                    String key = company.getId() + "_" + facilityType.getCodeId();
                    
                    try {
                        // 캐시에서 최신 마감 데이터 조회
                        Optional<DailyInventoryClosing> latestClosingOpt = getCachedLatestClosing(
                                company.getId(), facilityType.getCodeId());
                        
                        if (latestClosingOpt.isPresent()) {
                            DailyInventoryClosing latestClosing = latestClosingOpt.get();
                            LocalDate latestClosingDate = latestClosing.getClosingDate();
                            LocalDateTime latestClosingTime = latestClosing.getClosedAt() != null ? 
                                    latestClosing.getClosedAt() : latestClosing.getCreatedAt();
                            
                            // 최근 마감 이후의 트랜잭션 조회
                            int recentInbound = transactionRepository.countInboundTransactionsBetweenClosingTimes(
                                    latestClosingTime, currentTime, company.getId(), facilityType.getCodeId());
                            
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
                        } else {
                            // 마감 데이터가 없는 경우 전체 트랜잭션 조회
                            LocalDateTime veryPastTime = LocalDateTime.of(2000, 1, 1, 0, 0);
                            
                            int totalInbound = transactionRepository.countInboundTransactionsBetweenClosingTimes(
                                    veryPastTime, currentTime, company.getId(), facilityType.getCodeId());
                            
                            int totalOutbound = transactionRepository.countOutboundTransactionsBetweenClosingTimes(
                                    veryPastTime, currentTime, company.getId(), facilityType.getCodeId());
                            
                            // 현재 재고 = 총 입고 - 총 출고
                            int currentQuantity = totalInbound - totalOutbound;
                            
                            // 결과 추가
                            result.add(CurrentInventoryStatusDTO.builder()
                                    .companyId(company.getId())
                                    .companyName(company.getStoreName())
                                    .facilityTypeCodeId(facilityType.getCodeId())
                                    .facilityTypeName(facilityType.getCodeName())
                                    .latestClosingDate(null)  // 마감 날짜 없음
                                    .baseQuantity(0)          // 기준 수량 0
                                    .recentInbound(totalInbound)
                                    .recentOutbound(totalOutbound)
                                    .currentQuantity(currentQuantity)
                                    .build());
                        }
                    } catch (Exception e) {
                        log.error("재고 현황 계산 중 오류: key={}, 오류={}", key, e.getMessage());
                    }
                }
            }
            
            // 정렬 (회사명 -> 시설물 유형명)
            result.sort(Comparator
                    .comparing(CurrentInventoryStatusDTO::getCompanyName)
                    .thenComparing(CurrentInventoryStatusDTO::getFacilityTypeName));
            
        } catch (Exception e) {
            log.error("현재 재고 현황 조회 중 예외 발생: {}", e.getMessage());
        }
        
        long endTime = System.currentTimeMillis();
        log.info("캐시 사용 현재 재고 현황 조회 완료: {}개 결과, 소요시간: {}ms", result.size(), (endTime - startTime));
        
        // 결과가 없으면 기본 데이터 생성
        if (result.isEmpty()) {
            log.warn("조회 결과가 없어 기본 데이터를 생성합니다.");
            return createEmptyResultWithDefaultData(null);
        }
        
        return result;
    }
    
    /**
     * 결과가 없을 때 기본 데이터를 생성하는 헬퍼 메서드
     */
    private List<CurrentInventoryStatusDTO> createEmptyResultWithDefaultData(List<Code> facilityTypes) {
        List<CurrentInventoryStatusDTO> defaultResult = new ArrayList<>();
        
        try {
            // 회사 목록 가져오기 (최소 1개)
            List<Company> companies = companyRepository.findAll();
            if (companies.isEmpty()) {
                log.warn("등록된 회사가 없습니다. 기본 데이터를 생성할 수 없습니다.");
                return defaultResult;
            }
            
            // 시설물 유형 목록 가져오기 (제공되지 않은 경우)
            if (facilityTypes == null || facilityTypes.isEmpty()) {
                facilityTypes = codeRepository.findByCodeGroupGroupId("002001");
                if (facilityTypes.isEmpty()) {
                    log.warn("등록된 시설물 유형이 없습니다. 기본 데이터를 생성할 수 없습니다.");
                    return defaultResult;
                }
            }
            
            // 기본 회사와 시설물 유형으로 빈 데이터 생성
            Company defaultCompany = companies.get(0);
            Code defaultFacilityType = facilityTypes.get(0);
            
            defaultResult.add(CurrentInventoryStatusDTO.builder()
                    .companyId(defaultCompany.getId())
                    .companyName(defaultCompany.getStoreName())
                    .facilityTypeCodeId(defaultFacilityType.getCodeId())
                    .facilityTypeName(defaultFacilityType.getCodeName())
                    .latestClosingDate(null)
                    .baseQuantity(0)
                    .recentInbound(0)
                    .recentOutbound(0)
                    .currentQuantity(0)
                    .build());
            
            log.info("기본 데이터 생성 완료: 회사={}, 시설물유형={}", 
                    defaultCompany.getStoreName(), defaultFacilityType.getCodeName());
            
        } catch (Exception e) {
            log.error("기본 데이터 생성 중 오류 발생: {}", e.getMessage(), e);
        }
        
        return defaultResult;
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
        
        // User 정보 설정
        if (closing.getClosedBy() != null) {
            dto.setClosedBy(closing.getClosedBy().getId());
            dto.setUserId(closing.getClosedBy().getUserId());
            dto.setUserName(closing.getClosedBy().getUserName());
        } else {
            dto.setClosedBy(null);
        }
        
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
        
        // User 정보 설정
        if (closing.getClosedBy() != null) {
            dto.setClosedBy(closing.getClosedBy().getId());
            dto.setUserId(closing.getClosedBy().getUserId());
            dto.setUserName(closing.getClosedBy().getUserName());
        } else {
            dto.setClosedBy(null);
        }
        
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

    /**
     * 현재 재고 상태를 DB 페이징으로 조회 (성능 최적화)
     * @param companyId 회사 ID (선택적)
     * @param facilityTypeCodeId 시설물 유형 코드 ID (선택적)
     * @param pageable 페이징 정보
     * @return 페이징된 현재 재고 상태 DTO
     */
    public Page<CurrentInventoryStatusDTO> getCurrentInventoryStatusDbPaged(
            Long companyId, String facilityTypeCodeId, Pageable pageable) {
        
        log.info("DB 페이징 현재 재고 현황 조회 요청: 회사ID: {}, 시설물유형: {}", companyId, facilityTypeCodeId);
        long startTime = System.currentTimeMillis();
        
        try {
            // DB에서 페이징 처리하여 최신 마감 데이터 조회
            Page<DailyInventoryClosing> closingsPage = dailyClosingRepository
                    .findLatestClosingsByCompanyAndFacilityTypePaged(companyId, facilityTypeCodeId, pageable);
            
            // 현재 시간 (모든 트랜잭션에 공통으로 사용)
            LocalDateTime currentTime = LocalDateTime.now();
            
            // 마감 데이터가 없는 경우 처리
            if (closingsPage.getTotalElements() == 0) {
                log.info("마감 데이터가 없어 트랜잭션만으로 현재 재고를 계산합니다.");
                
                // 회사 목록 조회
                List<Company> companies;
                if (companyId != null) {
                    companies = companyRepository.findById(companyId)
                            .map(List::of)
                            .orElse(Collections.emptyList());
                } else {
                    companies = companyRepository.findAll();
                }
                
                // 시설물 유형 목록 조회
                List<Code> facilityTypes;
                if (facilityTypeCodeId != null) {
                    facilityTypes = codeRepository.findById(facilityTypeCodeId)
                            .map(List::of)
                            .orElse(Collections.emptyList());
                } else {
                    facilityTypes = codeRepository.findByCodeGroupGroupId("002001");
                }
                
                if (companies.isEmpty() || facilityTypes.isEmpty()) {
                    log.warn("조회할 회사 또는 시설물 유형이 없습니다.");
                    return Page.empty(pageable);
                }
                
                // 결과 목록 생성
                List<CurrentInventoryStatusDTO> resultList = new ArrayList<>();
                LocalDateTime veryPastTime = LocalDateTime.of(2000, 1, 1, 0, 0);
                
                // 각 회사와 시설물 유형 조합에 대해 트랜잭션 조회
                for (Company company : companies) {
                    for (Code facilityType : facilityTypes) {
                        try {
                            // 전체 트랜잭션 기간의 입출고 조회
                            int totalInbound = transactionRepository.countInboundTransactionsBetweenClosingTimes(
                                    veryPastTime, currentTime, company.getId(), facilityType.getCodeId());
                            
                            int totalOutbound = transactionRepository.countOutboundTransactionsBetweenClosingTimes(
                                    veryPastTime, currentTime, company.getId(), facilityType.getCodeId());
                            
                            // 현재 재고 = 총 입고 - 총 출고
                            int currentQuantity = totalInbound - totalOutbound;
                            
                            // 결과 추가
                            resultList.add(CurrentInventoryStatusDTO.builder()
                                    .companyId(company.getId())
                                    .companyName(company.getStoreName())
                                    .facilityTypeCodeId(facilityType.getCodeId())
                                    .facilityTypeName(facilityType.getCodeName())
                                    .latestClosingDate(null)  // 마감 날짜 없음
                                    .baseQuantity(0)          // 기준 수량 0
                                    .recentInbound(totalInbound)
                                    .recentOutbound(totalOutbound)
                                    .currentQuantity(currentQuantity)
                                    .build());
                        } catch (Exception e) {
                            log.error("트랜잭션 조회 중 오류: 회사ID={}, 시설물유형={}, 오류={}", 
                                    company.getId(), facilityType.getCodeId(), e.getMessage());
                        }
                    }
                }
                
                // 정렬 및 페이징 적용
                if (pageable.getSort().isSorted()) {
                    pageable.getSort().forEach(order -> {
                        String property = order.getProperty();
                        Sort.Direction direction = order.getDirection();
                        
                        Comparator<CurrentInventoryStatusDTO> comparator = null;
                        switch (property) {
                            case "companyName":
                                comparator = Comparator.comparing(CurrentInventoryStatusDTO::getCompanyName,
                                        Comparator.nullsLast(String::compareTo));
                                break;
                            case "facilityTypeName":
                                comparator = Comparator.comparing(CurrentInventoryStatusDTO::getFacilityTypeName,
                                        Comparator.nullsLast(String::compareTo));
                                break;
                            case "currentQuantity":
                                comparator = Comparator.comparing(CurrentInventoryStatusDTO::getCurrentQuantity,
                                        Comparator.nullsLast(Integer::compareTo));
                                break;
                            default:
                                comparator = Comparator.comparing(CurrentInventoryStatusDTO::getCompanyName,
                                        Comparator.nullsLast(String::compareTo));
                        }
                        
                        if (direction.isDescending()) {
                            comparator = comparator.reversed();
                        }
                        
                        Collections.sort(resultList, comparator);
                    });
                }
                
                // 페이징 적용
                int start = (int) pageable.getOffset();
                int end = Math.min((start + pageable.getPageSize()), resultList.size());
                
                if (start > end) {
                    start = 0;
                    end = Math.min(pageable.getPageSize(), resultList.size());
                }
                
                List<CurrentInventoryStatusDTO> pageContent = resultList.subList(start, end);
                Page<CurrentInventoryStatusDTO> page = new PageImpl<>(pageContent, pageable, resultList.size());
                
                long endTime = System.currentTimeMillis();
                log.info("트랜잭션 기반 현재 재고 현황 조회 완료: 총 {}개 중 {}개 결과, 소요시간: {}ms", 
                        resultList.size(), pageContent.size(), (endTime - startTime));
                
                return page;
            }
            
            // 마감 데이터를 DTO로 변환 (기존 로직)
            Page<CurrentInventoryStatusDTO> resultPage = closingsPage.map(closing -> {
                try {
                    // 회사 및 시설물 유형 정보
                    Company company = closing.getCompany();
                    Code facilityType = closing.getFacilityType();
                    
                    // 마감 시간
                    LocalDateTime closingTime = closing.getClosedAt() != null ? 
                            closing.getClosedAt() : closing.getCreatedAt();
                    
                    // 마감 이후 트랜잭션 조회
                    int recentInbound = transactionRepository.countInboundTransactionsBetweenClosingTimes(
                            closingTime, currentTime, company.getId(), facilityType.getCodeId());
                    
                    int recentOutbound = transactionRepository.countOutboundTransactionsBetweenClosingTimes(
                            closingTime, currentTime, company.getId(), facilityType.getCodeId());
                    
                    // 현재 재고 수량 계산
                    int currentQuantity = closing.getClosingQuantity() + recentInbound - recentOutbound;
                    
                    // DTO 생성
                    return CurrentInventoryStatusDTO.builder()
                            .companyId(company.getId())
                            .companyName(company.getStoreName())
                            .facilityTypeCodeId(facilityType.getCodeId())
                            .facilityTypeName(facilityType.getCodeName())
                            .latestClosingDate(closing.getClosingDate())
                            .baseQuantity(closing.getClosingQuantity())
                            .recentInbound(recentInbound)
                            .recentOutbound(recentOutbound)
                            .currentQuantity(currentQuantity)
                            .build();
                } catch (Exception e) {
                    log.error("재고 현황 변환 중 오류: id={}, 오류={}", closing.getId(), e.getMessage());
                    
                    // 오류 시 기본 데이터 반환
                    return CurrentInventoryStatusDTO.builder()
                            .companyId(closing.getCompany().getId())
                            .companyName(closing.getCompany().getStoreName())
                            .facilityTypeCodeId(closing.getFacilityType().getCodeId())
                            .facilityTypeName(closing.getFacilityType().getCodeName())
                            .latestClosingDate(closing.getClosingDate())
                            .baseQuantity(closing.getClosingQuantity())
                            .recentInbound(0)
                            .recentOutbound(0)
                            .currentQuantity(closing.getClosingQuantity())
                            .build();
                }
            });
            
            long endTime = System.currentTimeMillis();
            log.info("DB 페이징 현재 재고 현황 조회 완료: 총 {}개 결과, 소요시간: {}ms", 
                    resultPage.getTotalElements(), (endTime - startTime));
            
            return resultPage;
        } catch (Exception e) {
            log.error("DB 페이징 현재 재고 현황 조회 중 오류: {}", e.getMessage());
            return Page.empty(pageable);
        }
    }

    /**
     * 특정 연월의 모든 일자(1일부터 말일까지)에 대한 마감 상태 조회
     * @param year 조회할 연도
     * @param month 조회할 월
     * @param companyId 회사 ID (선택적)
     * @return 해당 월의 모든 일자별 마감 상태 목록
     */
    public List<Map<String, Object>> getDailyClosingStatusByMonth(int year, int month, Long companyId) {
        log.info("월별 일일 마감 상태 조회: {}년 {}월, 회사ID: {}", year, month, companyId);
        long startTime = System.currentTimeMillis();
        
        // 1. 회사 정보 조회
        Company company = null;
        if (companyId != null) {
            company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new RuntimeException("회사 정보를 찾을 수 없습니다: " + companyId));
        }
        
        // 2. 해당 월의 시작일과 종료일 계산
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        
        // 3. 해당 월의 모든 날짜에 대한 마감 상태 맵 생성
        Map<LocalDate, Boolean> closingStatusMap = new HashMap<>();
        Map<LocalDate, LocalDateTime> closingTimeMap = new HashMap<>();
        
        // 4. 해당 월에 마감된 일자 조회
        List<DailyInventoryClosing> closings;
        if (companyId != null) {
            closings = dailyClosingRepository.findByCompanyIdAndClosingDateBetweenAndIsClosed(
                    companyId, startDate, endDate, true);
        } else {
            closings = dailyClosingRepository.findByClosingDateBetweenAndIsClosed(
                    startDate, endDate, true);
        }
        
        // 일자별 마감 데이터 그룹화
        Map<LocalDate, List<DailyInventoryClosing>> closingsByDate = closings.stream()
                .collect(Collectors.groupingBy(DailyInventoryClosing::getClosingDate));
        
        // 5. 결과 목록 생성 (1일부터 말일까지)
        List<Map<String, Object>> resultList = new ArrayList<>();
        LocalDate currentDate = startDate;
        
        while (!currentDate.isAfter(endDate)) {
            Map<String, Object> dayStatus = new HashMap<>();
            dayStatus.put("date", currentDate);
            
            // 해당 일자의 마감 데이터가 있는지 확인
            List<DailyInventoryClosing> dayClosings = closingsByDate.getOrDefault(currentDate, new ArrayList<>());
            
            // 해당 일자에 대한 마감 상태 설정
            boolean isClosed = !dayClosings.isEmpty();
            
            // 마감 시간 설정 (마감된 경우만)
            LocalDateTime closedAt = null;
            if (isClosed && !dayClosings.isEmpty()) {
                // 가장 최근의 마감 시간 사용
                closedAt = dayClosings.stream()
                        .map(DailyInventoryClosing::getClosedAt)
                        .filter(time -> time != null)
                        .max(LocalDateTime::compareTo)
                        .orElse(null);
                
                // 마감 처리자 정보 추가
                DailyInventoryClosing latestClosing = dayClosings.stream()
                        .filter(closing -> closing.getClosedAt() != null)
                        .max(Comparator.comparing(DailyInventoryClosing::getClosedAt))
                        .orElse(null);
                
                if (latestClosing != null && latestClosing.getClosedBy() != null) {
                    User closedBy = latestClosing.getClosedBy();
                    dayStatus.put("closedBy", closedBy.getUserId());
                    dayStatus.put("closedByName", closedBy.getUserName());
                }
            }
            
            // 결과 맵에 정보 추가
            dayStatus.put("isClosed", isClosed);
            dayStatus.put("closedAt", closedAt);
            dayStatus.put("dayOfMonth", currentDate.getDayOfMonth());
            
            resultList.add(dayStatus);
            
            // 다음 날짜로 이동
            currentDate = currentDate.plusDays(1);
        }
        
        long endTime = System.currentTimeMillis();
        log.info("월별 일일 마감 상태 조회 완료: {}년 {}월, {}개 결과, 소요시간: {}ms", 
                year, month, resultList.size(), (endTime - startTime));
        
        return resultList;
    }

    /**
     * 특정 연도의 모든 월(1월부터 12월까지)에 대한 마감 상태 조회
     * @param year 조회할 연도
     * @param companyId 회사 ID (선택적)
     * @return 해당 연도의 모든 월별 마감 상태 목록
     */
    public List<Map<String, Object>> getMonthlyClosingStatusByYear(int year, Long companyId) {
        log.info("연간 월마감 상태 조회: {}년, 회사ID: {}", year, companyId);
        long startTime = System.currentTimeMillis();
        
        // 1. 회사 정보 조회
        Company company = null;
        if (companyId != null) {
            company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new RuntimeException("회사 정보를 찾을 수 없습니다: " + companyId));
        }
        
        // 2. 해당 연도의 월마감 데이터 조회
        List<MonthlyInventoryClosing> closings;
        if (companyId != null) {
            closings = monthlyClosingRepository.findByYearAndCompanyId(year, companyId);
        } else {
            closings = monthlyClosingRepository.findByYear(year);
        }
        
        // 3. 월별 마감 데이터 그룹화
        Map<Integer, List<MonthlyInventoryClosing>> closingsByMonth = closings.stream()
                .collect(Collectors.groupingBy(MonthlyInventoryClosing::getMonth));
        
        // 4. 결과 목록 생성 (1월부터 12월까지)
        List<Map<String, Object>> resultList = new ArrayList<>();
        
        for (int month = 1; month <= 12; month++) {
            Map<String, Object> monthStatus = new HashMap<>();
            monthStatus.put("year", year);
            monthStatus.put("month", month);
            
            // 해당 월의 마감 데이터가 있는지 확인
            List<MonthlyInventoryClosing> monthClosings = closingsByMonth.getOrDefault(month, new ArrayList<>());
            
            // 해당 월에 대한 마감 상태 설정
            boolean isClosed = !monthClosings.isEmpty();
            
            // 마감 시간 설정 (마감된 경우만)
            LocalDateTime closedAt = null;
            if (isClosed && !monthClosings.isEmpty()) {
                // 가장 최근의 마감 시간 사용
                closedAt = monthClosings.stream()
                        .map(MonthlyInventoryClosing::getClosedAt)
                        .filter(time -> time != null)
                        .max(LocalDateTime::compareTo)
                        .orElse(null);
            }
            
            // 결과 맵에 정보 추가
            monthStatus.put("isClosed", isClosed);
            monthStatus.put("closedAt", closedAt);
            
            // 마감된 경우 추가 정보
            if (isClosed && !monthClosings.isEmpty()) {
                // 마감 처리자 정보 (첫 번째 항목 기준)
                User closedBy = monthClosings.get(0).getClosedBy();
                if (closedBy != null) {
                    monthStatus.put("closedBy", closedBy.getUserId());
                    monthStatus.put("closedByName", closedBy.getUserName());
                }
            }
            
            resultList.add(monthStatus);
        }
        
        long endTime = System.currentTimeMillis();
        log.info("연간 월마감 상태 조회 완료: {}년, {}개 결과, 소요시간: {}ms", 
                year, resultList.size(), (endTime - startTime));
        
        return resultList;
    }
} 