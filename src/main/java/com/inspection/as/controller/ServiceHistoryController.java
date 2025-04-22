package com.inspection.as.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.inspection.as.dto.CreateServiceHistoryDTO;
import com.inspection.as.dto.ServiceHistoryDTO;
import com.inspection.as.service.ServiceHistoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/service-histories")
@RequiredArgsConstructor
public class ServiceHistoryController {
    
    private final ServiceHistoryService serviceHistoryService;
    
    /**
     * 모든 AS 이력 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<ServiceHistoryDTO>> getAllServiceHistories() {
        List<ServiceHistoryDTO> serviceHistories = serviceHistoryService.getAllServiceHistories();
        return ResponseEntity.ok(serviceHistories);
    }
    
    /**
     * AS 이력 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ServiceHistoryDTO> getServiceHistoryById(@PathVariable Long id) {
        ServiceHistoryDTO serviceHistory = serviceHistoryService.getServiceHistoryById(id);
        return ResponseEntity.ok(serviceHistory);
    }
    
    /**
     * 특정 AS 접수의 이력 목록 조회
     */
    @GetMapping("/service-request/{serviceRequestId}")
    public ResponseEntity<List<ServiceHistoryDTO>> getServiceHistoriesByServiceRequestId(
            @PathVariable Long serviceRequestId) {
        List<ServiceHistoryDTO> serviceHistories = serviceHistoryService.getServiceHistoriesByServiceRequestId(serviceRequestId);
        return ResponseEntity.ok(serviceHistories);
    }
    
    /**
     * 특정 작업자의 AS 이력 목록 조회
     */
    @GetMapping("/performed-by/{performedById}")
    public ResponseEntity<List<ServiceHistoryDTO>> getServiceHistoriesByPerformedById(
            @PathVariable Long performedById) {
        List<ServiceHistoryDTO> serviceHistories = serviceHistoryService.getServiceHistoriesByPerformedById(performedById);
        return ResponseEntity.ok(serviceHistories);
    }
    
    /**
     * 특정 날짜 범위 내의 AS 이력 조회
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<ServiceHistoryDTO>> getServiceHistoriesByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endDate) {
        List<ServiceHistoryDTO> serviceHistories = serviceHistoryService.getServiceHistoriesByDateRange(startDate, endDate);
        return ResponseEntity.ok(serviceHistories);
    }
    
    /**
     * 특정 작업 유형의 AS 이력 목록 조회
     */
    @GetMapping("/action-type/{actionTypeCode}")
    public ResponseEntity<List<ServiceHistoryDTO>> getServiceHistoriesByActionType(
            @PathVariable String actionTypeCode) {
        List<ServiceHistoryDTO> serviceHistories = serviceHistoryService.getServiceHistoriesByActionType(actionTypeCode);
        return ResponseEntity.ok(serviceHistories);
    }
    
    /**
     * AS 이력 생성
     */
    @PostMapping
    public ResponseEntity<ServiceHistoryDTO> createServiceHistory(
            @Valid @RequestBody CreateServiceHistoryDTO dto) {
        ServiceHistoryDTO createdServiceHistory = serviceHistoryService.createServiceHistory(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdServiceHistory);
    }
    
    /**
     * AS 이력 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ServiceHistoryDTO> updateServiceHistory(
            @PathVariable Long id,
            @Valid @RequestBody CreateServiceHistoryDTO dto) {
        ServiceHistoryDTO updatedServiceHistory = serviceHistoryService.updateServiceHistory(id, dto);
        return ResponseEntity.ok(updatedServiceHistory);
    }
    
    /**
     * AS 이력 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServiceHistory(@PathVariable Long id) {
        serviceHistoryService.deleteServiceHistory(id);
        return ResponseEntity.noContent().build();
    }
} 