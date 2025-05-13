package com.inspection.facility.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.inspection.entity.Code;
import com.inspection.entity.Company;
import com.inspection.facility.dto.CurrentInventoryStatusDTO;
import com.inspection.facility.dto.InventoryStatusDTO;
import com.inspection.facility.service.InventoryClosingService;
import com.inspection.facility.service.BatchInventoryClosingService;
import com.inspection.repository.CompanyRepository;
import com.inspection.repository.CodeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.context.ApplicationContext;

import com.inspection.config.ApplicationContextProvider;

@Slf4j
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryClosingController {
    
    private final InventoryClosingService closingService;
    private final BatchInventoryClosingService batchClosingService;
    private final CompanyRepository companyRepository;
    private final CodeRepository codeRepository;
    
    /**
     * 일일 마감 처리 API
     * @param closingDate 마감할 날짜
     * @return 처리 결과
     * /api/v1/inventory/daily-closing?closingDate=2025-04-29
     */
    @PostMapping("/daily-closing")
    public ResponseEntity<Map<String, Object>> processDailyClosing(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate closingDate) {
        
        log.info("일일 마감 처리 요청: {}", closingDate);
        
        // 현재 로그인한 사용자 ID 가져오기
        String userId = getCurrentUserId();
        
        int processedCount = closingService.processDailyClosing(closingDate, userId);
        
        Map<String, Object> response = Map.of(
                "success", true,
                "message", String.format("%s 일자 마감 처리가 완료되었습니다.", closingDate),
                "processedCount", processedCount
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 일일 마감 병렬 처리 API (성능 개선)
     * @param closingDate 마감할 날짜
     * @return 처리 결과
     * /api/v1/inventory/daily-closing-parallel?closingDate=2025-04-29
     */
    @PostMapping("/daily-closing-parallel")
    public ResponseEntity<Map<String, Object>> processDailyClosingParallel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate closingDate) {
        
        log.info("병렬 일일 마감 처리 요청: {}", closingDate);
        
        // 현재 로그인한 사용자 ID 가져오기
        String userId = getCurrentUserId();
        
        int processedCount = batchClosingService.processBatchClosing(
                companyRepository.findAll(),
                codeRepository.findByCodeGroupGroupId("002001"),
                closingDate, 
                userId);
        
        Map<String, Object> response = Map.of(
                "success", true,
                "message", String.format("%s 일자 병렬 마감 처리가 완료되었습니다.", closingDate),
                "processedCount", processedCount
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 일일 마감 그룹 병렬 처리 API (추가 성능 개선)
     * @param closingDate 마감할 날짜
     * @return 처리 결과
     * /api/v1/inventory/daily-closing-grouped?closingDate=2025-04-29
     */
    @PostMapping("/daily-closing-grouped")
    public ResponseEntity<Map<String, Object>> processDailyClosingGrouped(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate closingDate) {
        
        log.info("그룹 병렬 일일 마감 처리 요청: {}", closingDate);
        long startTime = System.currentTimeMillis();
        
        // 현재 로그인한 사용자 ID 가져오기
        String userId = getCurrentUserId();
        
        // 회사 및 시설물 유형 조회
        List<Company> companies = companyRepository.findAll();
        List<Code> facilityTypes = codeRepository.findByCodeGroupGroupId("002001");
        
        log.info("마감 처리 대상: 회사 {}개, 시설물 유형 {}개, 총 조합 {}개", 
                companies.size(), facilityTypes.size(), companies.size() * facilityTypes.size());
        
        // 그룹 병렬 처리 실행
        int processedCount = batchClosingService.processGroupedBatchClosing(
                companies, facilityTypes, closingDate, userId);
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        // 스레드 풀 상태 정보 가져오기 (ThreadPoolTaskExecutor에서 정보 추출)
        Map<String, Object> threadPoolInfo = new HashMap<>();
        try {
            ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) 
                    ApplicationContextProvider.getApplicationContext().getBean("inventoryTaskExecutor");
            
            threadPoolInfo.put("activeThreads", executor.getActiveCount());
            threadPoolInfo.put("poolSize", executor.getPoolSize());
            threadPoolInfo.put("corePoolSize", executor.getCorePoolSize());
            threadPoolInfo.put("maxPoolSize", executor.getMaxPoolSize());
            threadPoolInfo.put("queueSize", executor.getThreadPoolExecutor().getQueue().size());
            threadPoolInfo.put("queueCapacity", executor.getThreadPoolExecutor().getQueue().remainingCapacity());
            threadPoolInfo.put("completedTaskCount", executor.getThreadPoolExecutor().getCompletedTaskCount());
        } catch (Exception e) {
            log.warn("스레드 풀 정보 조회 중 오류: {}", e.getMessage());
            threadPoolInfo.put("error", "스레드 풀 정보를 조회할 수 없습니다.");
        }
        
        // 성능 정보
        Map<String, Object> performanceInfo = new HashMap<>();
        performanceInfo.put("executionTimeMs", executionTime);
        performanceInfo.put("averageTimePerCombination", 
                processedCount > 0 ? (double) executionTime / processedCount : 0);
        performanceInfo.put("combinationsPerSecond", 
                executionTime > 0 ? (double) processedCount * 1000 / executionTime : 0);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", String.format("%s 일자 그룹 병렬 마감 처리가 완료되었습니다.", closingDate));
        response.put("processedCount", processedCount);
        response.put("performance", performanceInfo);
        response.put("threadPool", threadPoolInfo);
        
        log.info("그룹 병렬 마감 처리 완료: 처리된 조합 {}, 소요시간: {}ms (평균 {}ms/조합)", 
                processedCount, executionTime, 
                processedCount > 0 ? (double) executionTime / processedCount : 0);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 월간 마감 처리 API
     * @param year 마감 연도
     * @param month 마감 월
     * @return 처리 결과
     * /api/v1/inventory/monthly-closing?year=2025&month=5
     */
    @PostMapping("/monthly-closing")
    public ResponseEntity<Map<String, Object>> processMonthlyClosing(
            @RequestParam int year, @RequestParam int month) {
        
        log.info("월간 마감 처리 요청: {}-{}", year, month);
        
        // 현재 로그인한 사용자 ID 가져오기
        String userId = getCurrentUserId();
        
        int processedCount = closingService.processMonthlyClosing(year, month, userId);
        
        Map<String, Object> response = Map.of(
                "success", true,
                "message", String.format("%d년 %d월 마감 처리가 완료되었습니다.", year, month),
                "processedCount", processedCount
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 일별 재고 현황 조회 API
     * @param date 조회 날짜
     * @param companyId 회사 ID (선택적)
     * @param facilityTypeCodeId 시설물 유형 코드 ID (선택적)
     * @return 일별 재고 현황 목록
     * /api/v1/inventory/daily-status?date=2025-05-01&companyId=1&facilityTypeCodeId=TIRE
     */
    @GetMapping("/daily-status")
    public ResponseEntity<List<InventoryStatusDTO>> getDailyInventoryStatus(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String facilityTypeCodeId) {
        
        log.info("일별 재고 현황 조회 요청: {}, 회사ID: {}, 시설물유형: {}", 
                date, companyId, facilityTypeCodeId);
        
        List<InventoryStatusDTO> statusList = closingService.getDailyInventoryStatus(
                date, companyId, facilityTypeCodeId);
        
        return ResponseEntity.ok(statusList);
    }
    
    /**
     * 월별 재고 현황 조회 API
     * @param year 조회 연도
     * @param month 조회 월
     * @param companyId 회사 ID (선택적)
     * @param facilityTypeCodeId 시설물 유형 코드 ID (선택적)
     * @return 월별 재고 현황 목록
     * /api/v1/inventory/monthly-status?year=2025&month=5&companyId=1&facilityTypeCodeId=TIRE
     */
    @GetMapping("/monthly-status")
    public ResponseEntity<List<InventoryStatusDTO>> getMonthlyInventoryStatus(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String facilityTypeCodeId) {
        
        log.info("월별 재고 현황 조회 요청: {}-{}, 회사ID: {}, 시설물유형: {}", 
                year, month, companyId, facilityTypeCodeId);
        
        List<InventoryStatusDTO> statusList = closingService.getMonthlyInventoryStatus(
                year, month, companyId, facilityTypeCodeId);
        
        return ResponseEntity.ok(statusList);
    }
    
    /**
     * 현재 재고 현황 조회 API (실시간)
     * @param companyId 회사 ID (선택적)
     * @param facilityTypeCodeId 시설물 유형 코드 ID (선택적)
     * @return 현재 재고 현황 목록
     * /api/v1/inventory/current-status?companyId=1&facilityTypeCodeId=TIRE
     */
    @GetMapping("/current-status")
    public ResponseEntity<List<CurrentInventoryStatusDTO>> getCurrentInventoryStatus(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String facilityTypeCodeId) {
        
        long startTime = System.currentTimeMillis();
        
        // 캐시를 사용하는 최적화된 메서드 호출
        List<CurrentInventoryStatusDTO> statusList = closingService.getCurrentInventoryStatusCached(
                companyId, facilityTypeCodeId);
        
        long endTime = System.currentTimeMillis();
        log.info("현재 재고 현황 API 응답: {}개 결과, 소요시간: {}ms", 
                statusList.size(), (endTime - startTime));
        
        return ResponseEntity.ok(statusList);
    }
    
    /**
     * 재고 추이 조회 API
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param companyId 회사 ID (선택적)
     * @param facilityTypeCodeId 시설물 유형 코드 ID (선택적)
     * @return 재고 추이 데이터
     * /api/v1/inventory/trend?startDate=2025-05-01&endDate=2025-05-31&companyId=1&facilityTypeCodeId=TIRE
     */
    @GetMapping("/trend")
    public ResponseEntity<Map<LocalDate, List<InventoryStatusDTO>>> getInventoryTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String facilityTypeCodeId) {
        
        log.info("재고 추이 조회 요청: {} ~ {}, 회사ID: {}, 시설물유형: {}", 
                startDate, endDate, companyId, facilityTypeCodeId);
        
        Map<LocalDate, List<InventoryStatusDTO>> trendData = closingService.getInventoryTrend(
                startDate, endDate, companyId, facilityTypeCodeId);
        
        return ResponseEntity.ok(trendData);
    }
    
    /**
     * 특정 일자의 일마감 재계산 API
     * @param closingDate 재계산할 날짜
     * @return 처리 결과
     * /api/v1/inventory/daily-closing/recalculate?closingDate=2025-04-29
     */
    @PostMapping("/daily-closing/recalculate")
    public ResponseEntity<Map<String, Object>> recalculateDailyClosing(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate closingDate) {
        
        log.info("일일 마감 재계산 요청: {}", closingDate);
        
        // 현재 로그인한 사용자 ID 가져오기
        String userId = getCurrentUserId();
        
        int processedCount = closingService.recalculateDailyClosing(closingDate, userId);
        
        Map<String, Object> response = Map.of(
                "success", true,
                "message", String.format("%s 일자 마감 재계산이 완료되었습니다.", closingDate),
                "processedCount", processedCount
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 현재 재고 현황 조회 API (페이지네이션 지원)
     * @param companyId 회사 ID (선택적)
     * @param facilityTypeCodeId 시설물 유형 코드 ID (선택적)
     * @param pageable 페이지 정보
     * @return 현재 재고 현황 목록
     * /api/v1/inventory/current-status-paged?companyId=1&facilityTypeCodeId=TIRE&page=0&size=20&sort=companyName,asc
     */
    @GetMapping("/current-status-paged")
    public ResponseEntity<Page<CurrentInventoryStatusDTO>> getCurrentInventoryStatusPaged(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String facilityTypeCodeId,
            Pageable pageable) {
        
        log.info("페이지네이션 현재 재고 현황 조회: 회사ID: {}, 시설물유형: {}", 
                companyId, facilityTypeCodeId);
        long startTime = System.currentTimeMillis();
        
        try {
            // 모든 데이터를 가져온 다음 메모리에서 페이지네이션 처리
            // 캐시를 사용하는 최적화된 메서드 호출
            List<CurrentInventoryStatusDTO> allData = closingService.getCurrentInventoryStatusCached(companyId, facilityTypeCodeId);
            
            if (allData.isEmpty()) {
                log.warn("페이지네이션 처리할 데이터가 없습니다.");
                return ResponseEntity.ok(Page.empty(pageable));
            }
            
            // 정렬 적용
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
                    
                    Collections.sort(allData, comparator);
                });
            }
            
            // 페이지네이션 적용
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), allData.size());
            
            if (start > end) {
                start = 0;
                end = Math.min(pageable.getPageSize(), allData.size());
            }
            
            List<CurrentInventoryStatusDTO> pageContent = allData.subList(start, end);
            Page<CurrentInventoryStatusDTO> page = new PageImpl<>(pageContent, pageable, allData.size());
            
            long endTime = System.currentTimeMillis();
            log.info("페이지네이션 현재 재고 현황 응답: 총 {}개 중 {}개 결과, 소요시간: {}ms", 
                    allData.size(), pageContent.size(), (endTime - startTime));
            
            return ResponseEntity.ok(page);
        } catch (Exception e) {
            log.error("페이지네이션 처리 중 오류: {}", e.getMessage());
            return ResponseEntity.ok(Page.empty(pageable));
        }
    }
    
    /**
     * 현재 재고 현황 조회 API (DB 페이징 지원, 성능 최적화)
     * @param companyId 회사 ID (선택적)
     * @param facilityTypeCodeId 시설물 유형 코드 ID (선택적)
     * @param pageable 페이지 정보
     * @return 현재 재고 현황 목록
     * /api/v1/inventory/current-status-db-paged?companyId=1&facilityTypeCodeId=TIRE&page=0&size=20&sort=companyName,asc
     */
    @GetMapping("/current-status-db-paged")
    public ResponseEntity<Page<CurrentInventoryStatusDTO>> getCurrentInventoryStatusDbPaged(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String facilityTypeCodeId,
            Pageable pageable) {
        
        long startTime = System.currentTimeMillis();
        
        // DB 단에서 페이징 처리하는 최적화된 메서드 호출
        Page<CurrentInventoryStatusDTO> page = closingService.getCurrentInventoryStatusDbPaged(
                companyId, facilityTypeCodeId, pageable);
        
        long endTime = System.currentTimeMillis();
        log.info("DB 페이징 현재 재고 현황 API 응답: 총 {}개 중 {}개 결과, 소요시간: {}ms", 
                page.getTotalElements(), page.getNumberOfElements(), (endTime - startTime));
        
        return ResponseEntity.ok(page);
    }
    
    /**
     * 현재 로그인한 사용자 ID를 가져오는 헬퍼 메서드
     * @return 사용자 ID
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "SYSTEM"; // 기본값
    }
} 