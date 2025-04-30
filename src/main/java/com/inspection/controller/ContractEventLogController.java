package com.inspection.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.inspection.dto.CompanyLogDTO;
import com.inspection.dto.ContractEventLogDTO;
import com.inspection.dto.ContractLogDTO;
import com.inspection.entity.ContractEventLog;
import com.inspection.repository.ContractEventLogRepository;
import com.inspection.repository.CodeRepository;
import com.inspection.service.ContractEventLogService;
import com.inspection.service.ContractLogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 계약 이벤트 로그 조회를 위한 REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/contract-event-logs")
@RequiredArgsConstructor
public class ContractEventLogController {

    private final ContractEventLogRepository eventLogRepository;
    private final ContractEventLogService eventLogService;
    private final ContractLogService contractLogService;
    
    /**
     * 회사 목록 조회 API (이력 관리용)
     * @param keyword 검색 키워드 (선택)
     * @return 회사 목록
     */
    @GetMapping("/companies")
    public ResponseEntity<List<CompanyLogDTO>> getCompaniesForLog(
            @RequestParam(required = false) String keyword) {
        log.info("이력 관리용 회사 목록 조회 (키워드: {})", keyword);
        List<CompanyLogDTO> companies = contractLogService.getCompanyList(keyword);
        return ResponseEntity.ok(companies);
    }
    
    /**
     * 특정 회사의 계약 목록 조회 API
     * @param companyId 회사 ID
     * @return 해당 회사의 계약 목록
     */
    @GetMapping("/companies/{companyId}/contracts")
    public ResponseEntity<List<ContractLogDTO>> getContractsByCompanyForLog(
            @PathVariable Long companyId) {
        log.info("이력 관리용 회사 ID {}의 계약 목록 조회", companyId);
        List<ContractLogDTO> contracts = contractLogService.getContractsByCompanyId(companyId);
        return ResponseEntity.ok(contracts);
    }
    
    /**
     * 계약 목록 검색 API (이력 관리용)
     * @param keyword 검색 키워드 (선택)
     * @param statusCodeId 계약 상태 코드 (선택)
     * @return 검색 조건에 맞는 계약 목록
     */
    @GetMapping("/contracts/search")
    public ResponseEntity<List<ContractLogDTO>> searchContractsForLog(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String statusCodeId) {
        log.info("이력 관리용 계약 검색 (키워드: {}, 상태: {})", keyword, statusCodeId);
        List<ContractLogDTO> contracts = contractLogService.searchContracts(keyword, statusCodeId);
        return ResponseEntity.ok(contracts);
    }
    
    /**
     * 특정 계약에 대한 모든 이벤트 로그 조회
     * @param contractId 계약 ID
     * @return 계약 이벤트 로그 목록
     */
    @GetMapping("/contract/{contractId}")
    public ResponseEntity<List<ContractEventLogDTO>> getEventLogsByContract(
            @PathVariable Long contractId) {
        log.info("계약 ID {}에 대한 이벤트 로그 조회", contractId);
        List<ContractEventLogDTO> logs = eventLogService.getEventLogsByContractId(contractId);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * 특정 참여자에 대한 모든 이벤트 로그 조회
     * @param participantId 참여자 ID
     * @return 참여자 관련 이벤트 로그 목록
     */
    @GetMapping("/participant/{participantId}")
    public ResponseEntity<List<ContractEventLogDTO>> getEventLogsByParticipant(
            @PathVariable Long participantId) {
        log.info("참여자 ID {}에 대한 이벤트 로그 조회", participantId);
        List<ContractEventLogDTO> logs = eventLogService.getEventLogsByParticipantId(participantId);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * 특정 이벤트 타입에 대한 이벤트 로그 조회
     * @param eventTypeCodeId 이벤트 타입 코드 ID
     * @return 해당 타입의 이벤트 로그 목록
     */
    @GetMapping("/event-type/{eventTypeCodeId}")
    public ResponseEntity<List<ContractEventLogDTO>> getEventLogsByEventType(
            @PathVariable String eventTypeCodeId) {
        log.info("이벤트 타입 {}에 대한 이벤트 로그 조회", eventTypeCodeId);
        List<ContractEventLogDTO> logs = eventLogService.getEventLogsByEventTypeCodeId(eventTypeCodeId);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * 기간별 이벤트 로그 조회
     * @param startDate 시작 날짜 (yyyy-MM-dd 형식)
     * @param endDate 종료 날짜 (yyyy-MM-dd 형식)
     * @return 해당 기간의 이벤트 로그 목록
     */
    @GetMapping("/period")
    public ResponseEntity<List<ContractEventLogDTO>> getEventLogsByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        log.info("기간 {} ~ {} 동안의 이벤트 로그 조회", startDateTime, endDateTime);
        List<ContractEventLogDTO> logs = eventLogService.getEventLogsByPeriod(startDateTime, endDateTime);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * 복합 조건 이벤트 로그 검색
     * @param contractId 계약 ID (선택)
     * @param participantId 참여자 ID (선택)
     * @param eventTypeCodeId 이벤트 타입 코드 ID (선택)
     * @param startDate 시작 날짜 (선택)
     * @param endDate 종료 날짜 (선택)
     * @return 조건에 맞는 이벤트 로그 목록
     */
    @GetMapping("/search")
    public ResponseEntity<List<ContractEventLogDTO>> searchEventLogs(
            @RequestParam(required = false) Long contractId,
            @RequestParam(required = false) Long participantId,
            @RequestParam(required = false) String eventTypeCodeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        
        log.info("계약 ID: {}, 참여자 ID: {}, 이벤트 타입: {}, 기간: {} ~ {}에 대한 이벤트 로그 검색", 
                contractId, participantId, eventTypeCodeId, startDateTime, endDateTime);
        
        List<ContractEventLogDTO> logs = eventLogService.searchEventLogs(
                contractId, participantId, eventTypeCodeId, startDateTime, endDateTime);
        
        return ResponseEntity.ok(logs);
    }
    
    /**
     * 이벤트 타입별 통계 조회
     * @return 이벤트 타입별 개수
     */
    @GetMapping("/statistics/event-types")
    public ResponseEntity<Map<String, Long>> getEventTypeStatistics() {
        log.info("이벤트 타입별 통계 조회");
        Map<String, Long> statistics = eventLogService.getEventTypeStatistics();
        return ResponseEntity.ok(statistics);
    }
} 