package com.inspection.facility.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.inspection.facility.dto.CreateDepreciationDTO;
import com.inspection.facility.dto.DepreciationDTO;
import com.inspection.facility.dto.DepreciationSummaryDTO;
import com.inspection.facility.dto.UpdateDepreciationDTO;
import com.inspection.facility.service.DepreciationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/depreciations")
@RequiredArgsConstructor
public class DepreciationController {
    
    private final DepreciationService depreciationService;
    
    /**
     * 모든 감가상각 이력 조회
     */
    @GetMapping
    public ResponseEntity<List<DepreciationDTO>> getAllDepreciations() {
        List<DepreciationDTO> depreciations = depreciationService.getAllDepreciations();
        return ResponseEntity.ok(depreciations);
    }
    
    /**
     * 감가상각 이력 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<DepreciationDTO> getDepreciationById(@PathVariable Long id) {
        DepreciationDTO depreciation = depreciationService.getDepreciationById(id);
        return ResponseEntity.ok(depreciation);
    }
    
    /**
     * 특정 시설물의 감가상각 이력 조회
     */
    @GetMapping("/facility/{facilityId}")
    public ResponseEntity<List<DepreciationDTO>> getDepreciationsByFacilityId(@PathVariable Long facilityId) {
        List<DepreciationDTO> depreciations = depreciationService.getDepreciationsByFacilityId(facilityId);
        return ResponseEntity.ok(depreciations);
    }
    
    /**
     * 특정 시설물의 최신 감가상각 이력 조회
     */
    @GetMapping("/facility/{facilityId}/latest")
    public ResponseEntity<DepreciationDTO> getLatestDepreciationByFacilityId(@PathVariable Long facilityId) {
        DepreciationDTO depreciation = depreciationService.getLatestDepreciationByFacilityId(facilityId);
        return ResponseEntity.ok(depreciation);
    }
    
    /**
     * 특정 날짜 범위 내의 감가상각 이력 조회
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<DepreciationDTO>> getDepreciationsByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endDate) {
        List<DepreciationDTO> depreciations = depreciationService.getDepreciationsByDateRange(startDate, endDate);
        return ResponseEntity.ok(depreciations);
    }
    
    /**
     * 특정 회계연도의 감가상각 이력 조회
     */
    @GetMapping("/fiscal-year/{fiscalYear}")
    public ResponseEntity<List<DepreciationDTO>> getDepreciationsByFiscalYear(@PathVariable Integer fiscalYear) {
        List<DepreciationDTO> depreciations = depreciationService.getDepreciationsByFiscalYear(fiscalYear);
        return ResponseEntity.ok(depreciations);
    }
    
    /**
     * 특정 회계연도 및 회계월의 감가상각 이력 조회
     */
    @GetMapping("/fiscal-year/{fiscalYear}/month/{fiscalMonth}")
    public ResponseEntity<List<DepreciationDTO>> getDepreciationsByFiscalYearAndMonth(
            @PathVariable Integer fiscalYear,
            @PathVariable Integer fiscalMonth) {
        List<DepreciationDTO> depreciations = depreciationService.getDepreciationsByFiscalYearAndMonth(fiscalYear, fiscalMonth);
        return ResponseEntity.ok(depreciations);
    }
    
    /**
     * 특정 회계연도의 시설물별 감가상각 요약 조회
     */
    @GetMapping("/summary/fiscal-year/{fiscalYear}")
    public ResponseEntity<List<DepreciationSummaryDTO>> getDepreciationSummaryByFiscalYear(@PathVariable Integer fiscalYear) {
        List<DepreciationSummaryDTO> summaries = depreciationService.getDepreciationSummaryByFiscalYear(fiscalYear);
        return ResponseEntity.ok(summaries);
    }
    
    /**
     * 감가상각 이력 생성
     */
    @PostMapping
    public ResponseEntity<DepreciationDTO> createDepreciation(@Valid @RequestBody CreateDepreciationDTO dto) {
        DepreciationDTO createdDepreciation = depreciationService.createDepreciation(dto);
        log.info("감가상각 이력이 생성되었습니다. ID: {}", createdDepreciation.getDepreciationId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDepreciation);
    }
    
    /**
     * 감가상각 기록 수정
     */
    @PutMapping("/{depreciationId}")
    public ResponseEntity<DepreciationDTO> updateDepreciation(
            @PathVariable Long depreciationId,
            @Valid @RequestBody UpdateDepreciationDTO updateDTO) {
        // 요청된 ID와 DTO의 ID가 일치하는지 확인
        if (!depreciationId.equals(updateDTO.getDepreciationId())) {
            return ResponseEntity.badRequest().build();
        }
        
        DepreciationDTO updatedDepreciation = depreciationService.updateDepreciation(updateDTO);
        return ResponseEntity.ok(updatedDepreciation);
    }
    
    /**
     * 단일 시설물에 대한 감가상각 처리
     */
    @PostMapping("/facility/{facilityId}/process")
    public ResponseEntity<DepreciationDTO> processDepreciationForFacility(@PathVariable Long facilityId) {
        DepreciationDTO result = depreciationService.processDepreciationForFacility(facilityId);
        log.info("시설물 ID {}에 대한 감가상각이 처리되었습니다.", facilityId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    /**
     * 모든 시설물에 대한 월별 감가상각 처리
     */
    @PostMapping("/process/monthly")
    public ResponseEntity<List<DepreciationDTO>> processMonthlyDepreciationForAllFacilities() {
        List<DepreciationDTO> results = depreciationService.processMonthlyDepreciationForAllFacilities();
        log.info("모든 시설물에 대한 월별 감가상각이 처리되었습니다. 처리된 시설물 수: {}", results.size());
        return ResponseEntity.status(HttpStatus.CREATED).body(results);
    }
} 