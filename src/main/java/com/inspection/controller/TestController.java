package com.inspection.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.inspection.scheduler.ContractScheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 테스트용 컨트롤러
 * 개발 중 테스트를 위한 API 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final ContractScheduler contractScheduler;

    /**
     * 계약 스케줄러 수동 실행 API
     * 배치 작업을 즉시 실행시켜 테스트
     */
    @GetMapping("/scheduler")
    public ResponseEntity<?> runContractScheduler() {
        log.info("계약 스케줄러 수동 실행 요청");
        try {
            var result = contractScheduler.processContractStatus();
            log.info("스케줄러 실행 결과: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("스케줄러 실행 중 오류 발생", e);
            return ResponseEntity.internalServerError().body(
                    Map.of("error", e.getMessage()));
        }
    }
} 