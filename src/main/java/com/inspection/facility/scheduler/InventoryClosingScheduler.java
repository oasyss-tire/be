package com.inspection.facility.scheduler;

import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.inspection.facility.service.InventoryClosingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 재고 마감 자동화를 위한 스케줄러
 * 일일 마감 및 월간 마감을 자동으로 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryClosingScheduler {

    private final InventoryClosingService closingService;
    
    /**
     * 일일 마감 자동 처리
     * 매일 새벽 1시에 전일 재고를 마감
     */
    @Scheduled(cron = "0 0 1 * * ?") // 매일 01:00에 실행
    public void scheduleDailyClosing() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("일일 마감 자동 처리 시작: {}", yesterday);
        
        int processedCount = closingService.processDailyClosing(yesterday, "SYSTEM");
        
        log.info("일일 마감 자동 처리 완료: {}, 처리된 레코드: {}", yesterday, processedCount);
    }
    
    /**
     * 월간 마감 자동 처리
     * 매월 1일 새벽 2시에 전월 재고를 마감
     */
    @Scheduled(cron = "0 0 2 1 * ?") // 매월 1일 02:00에 실행
    public void scheduleMonthlyClosing() {
        LocalDate today = LocalDate.now();
        LocalDate lastDayOfPreviousMonth = today.withDayOfMonth(1).minusDays(1);
        
        int year = lastDayOfPreviousMonth.getYear();
        int month = lastDayOfPreviousMonth.getMonthValue();
        
        log.info("월간 마감 자동 처리 시작: {}-{}", year, month);
        
        int processedCount = closingService.processMonthlyClosing(year, month, "SYSTEM");
        
        log.info("월간 마감 자동 처리 완료: {}-{}, 처리된 레코드: {}", year, month, processedCount);
    }
} 