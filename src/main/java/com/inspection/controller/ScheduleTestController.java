package com.inspection.controller;

import com.inspection.scheduler.ContractScheduler;
import com.inspection.util.DateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 스케줄러 테스트용 컨트롤러
 * 개발 및 테스트 환경에서만 사용
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/schedule")
@RequiredArgsConstructor
public class ScheduleTestController {

    private final ContractScheduler contractScheduler;
    private final DateProvider dateProvider;
    
    /**
     * 계약 상태 업데이트 스케줄러를 수동으로 실행
     * @param testDate 테스트용 날짜 (YYYY-MM-DD 형식, 미제공 시 현재 날짜 사용)
     * @return 처리 결과
     */
    @PostMapping("/run-contract-check")
    public ResponseEntity<Map<String, Object>> runContractCheck(
            @RequestParam(required = false) String testDate) {
        
        log.info("계약 상태 업데이트 수동 실행 요청: testDate={}", testDate);
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 테스트 날짜가 제공되면 설정
            if (testDate != null) {
                LocalDate date = LocalDate.parse(testDate);
                dateProvider.setOverrideDate(date);
                response.put("testDate", testDate);
            }
            
            // 스케줄러 작업 실행
            Map<String, Object> result = contractScheduler.processContractStatus();
            response.put("result", result);
            response.put("success", true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("스케줄러 수동 실행 중 오류 발생", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } finally {
            // 날짜 오버라이드 초기화
            dateProvider.resetOverrideDate();
        }
    }
    
    /**
     * 현재 설정된 날짜 확인
     */
    @GetMapping("/current-date")
    public ResponseEntity<Map<String, Object>> getCurrentDate() {
        Map<String, Object> response = new HashMap<>();
        response.put("currentDate", dateProvider.getCurrentDate().toString());
        response.put("systemDate", LocalDate.now().toString());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 테스트용 날짜 설정
     */
    @PostMapping("/set-date")
    public ResponseEntity<Map<String, Object>> setTestDate(
            @RequestParam String date) {
        try {
            LocalDate testDate = LocalDate.parse(date);
            dateProvider.setOverrideDate(testDate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "테스트 날짜가 설정되었습니다");
            response.put("testDate", date);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "날짜 형식이 올바르지 않습니다 (YYYY-MM-DD)");
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 테스트용 날짜 초기화
     */
    @PostMapping("/reset-date")
    public ResponseEntity<Map<String, Object>> resetTestDate() {
        dateProvider.resetOverrideDate();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "테스트 날짜가 초기화되었습니다");
        response.put("currentDate", LocalDate.now().toString());
        return ResponseEntity.ok(response);
    }
} 