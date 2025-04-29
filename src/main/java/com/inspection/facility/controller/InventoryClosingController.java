package com.inspection.facility.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.inspection.facility.dto.CurrentInventoryStatusDTO;
import com.inspection.facility.dto.InventoryStatusDTO;
import com.inspection.facility.service.InventoryClosingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryClosingController {
    
    private final InventoryClosingService closingService;
    
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
        
        log.info("현재 재고 현황 조회 요청: 회사ID: {}, 시설물유형: {}", companyId, facilityTypeCodeId);
        
        List<CurrentInventoryStatusDTO> statusList = closingService.getCurrentInventoryStatus(
                companyId, facilityTypeCodeId);
        
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