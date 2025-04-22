package com.inspection.as.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.inspection.as.dto.CreateServiceRequestDTO;
import com.inspection.as.dto.ServiceRequestDTO;
import com.inspection.as.dto.UpdateServiceRequestDTO;
import com.inspection.as.dto.ReceiveServiceRequestDTO;
import com.inspection.as.dto.CompleteServiceRequestDTO;
import com.inspection.as.service.ServiceRequestService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/service-requests")
@RequiredArgsConstructor
public class ServiceRequestController {
    
    private final ServiceRequestService serviceRequestService;
    
    /**
     * 모든 AS 접수 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<ServiceRequestDTO>> getAllServiceRequests() {
        List<ServiceRequestDTO> serviceRequests = serviceRequestService.getAllServiceRequests();
        return ResponseEntity.ok(serviceRequests);
    }
    
    /**
     * 페이징된 AS 접수 목록 조회
     */
    @GetMapping("/paged")
    public ResponseEntity<Page<ServiceRequestDTO>> getServiceRequestsPaged(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<ServiceRequestDTO> page = serviceRequestService.getServiceRequests(pageable);
        return ResponseEntity.ok(page);
    }
    
    /**
     * AS 접수 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ServiceRequestDTO> getServiceRequestById(@PathVariable Long id) {
        ServiceRequestDTO serviceRequest = serviceRequestService.getServiceRequestById(id);
        return ResponseEntity.ok(serviceRequest);
    }
    
    /**
     * AS 접수 상세 조회 (이력 포함)
     */
    @GetMapping("/{id}/with-histories")
    public ResponseEntity<ServiceRequestDTO> getServiceRequestWithHistories(@PathVariable Long id) {
        ServiceRequestDTO serviceRequest = serviceRequestService.getServiceRequestWithHistories(id);
        return ResponseEntity.ok(serviceRequest);
    }
    
    /**
     * 특정 시설물의 AS 접수 목록 조회
     */
    @GetMapping("/facility/{facilityId}")
    public ResponseEntity<List<ServiceRequestDTO>> getServiceRequestsByFacilityId(@PathVariable Long facilityId) {
        List<ServiceRequestDTO> serviceRequests = serviceRequestService.getServiceRequestsByFacilityIdOrderByLatest(facilityId);
        return ResponseEntity.ok(serviceRequests);
    }
    
    /**
     * 특정 관리자의 AS 접수 목록 조회
     */
    @GetMapping("/manager/{managerId}")
    public ResponseEntity<List<ServiceRequestDTO>> getServiceRequestsByManagerId(@PathVariable Long managerId) {
        List<ServiceRequestDTO> serviceRequests = serviceRequestService.getServiceRequestsByManagerId(managerId);
        return ResponseEntity.ok(serviceRequests);
    }
    
    /**
     * 특정 사용자의 요청 목록 조회
     */
    @GetMapping("/requester/{requesterId}")
    public ResponseEntity<List<ServiceRequestDTO>> getServiceRequestsByRequesterId(@PathVariable Long requesterId) {
        List<ServiceRequestDTO> serviceRequests = serviceRequestService.getServiceRequestsByRequesterId(requesterId);
        return ResponseEntity.ok(serviceRequests);
    }
    
    /**
     * 미완료 AS 접수 목록 조회
     */
    @GetMapping("/incomplete")
    public ResponseEntity<List<ServiceRequestDTO>> getIncompleteServiceRequests() {
        List<ServiceRequestDTO> serviceRequests = serviceRequestService.getIncompleteServiceRequests();
        return ResponseEntity.ok(serviceRequests);
    }
    
    /**
     * 특정 서비스 유형의 AS 접수 목록 조회
     */
    @GetMapping("/type/{serviceTypeCode}")
    public ResponseEntity<List<ServiceRequestDTO>> getServiceRequestsByServiceType(
            @PathVariable String serviceTypeCode) {
        List<ServiceRequestDTO> serviceRequests = serviceRequestService.getServiceRequestsByServiceType(serviceTypeCode);
        return ResponseEntity.ok(serviceRequests);
    }
    
    /**
     * 특정 우선순위의 AS 접수 목록 조회
     */
    @GetMapping("/priority/{priorityCode}")
    public ResponseEntity<List<ServiceRequestDTO>> getServiceRequestsByPriority(
            @PathVariable String priorityCode) {
        List<ServiceRequestDTO> serviceRequests = serviceRequestService.getServiceRequestsByPriority(priorityCode);
        return ResponseEntity.ok(serviceRequests);
    }
    
    /**
     * 완료 예정일이 임박한 AS 접수 목록 조회
     */
    @GetMapping("/upcoming-due")
    public ResponseEntity<List<ServiceRequestDTO>> getUpcomingDueServiceRequests(
            @RequestParam(defaultValue = "7") int days) {
        List<ServiceRequestDTO> serviceRequests = serviceRequestService.getUpcomingDueServiceRequests(days);
        return ResponseEntity.ok(serviceRequests);
    }
    
    /**
     * 특정 날짜 범위 내의 AS 접수 목록 조회
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<ServiceRequestDTO>> getServiceRequestsByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endDate) {
        List<ServiceRequestDTO> serviceRequests = serviceRequestService.getServiceRequestsByDateRange(startDate, endDate);
        return ResponseEntity.ok(serviceRequests);
    }
    
    /**
     * AS 접수 생성
     */
    @PostMapping
    public ResponseEntity<ServiceRequestDTO> createServiceRequest(
            @Valid @RequestBody CreateServiceRequestDTO dto) {
        
        // 현재 로그인한 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();
        
        ServiceRequestDTO createdServiceRequest = serviceRequestService.createServiceRequest(dto, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdServiceRequest);
    }
    
    /**
     * AS 접수 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ServiceRequestDTO> updateServiceRequest(
            @PathVariable Long id,
            @Valid @RequestBody UpdateServiceRequestDTO dto) {
        ServiceRequestDTO updatedServiceRequest = serviceRequestService.updateServiceRequest(id, dto);
        return ResponseEntity.ok(updatedServiceRequest);
    }
    
    /**
     * AS 접수 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServiceRequest(@PathVariable Long id) {
        serviceRequestService.deleteServiceRequest(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * AS 접수 완료 처리
     */
    @PutMapping("/{id}/receive")
    public ResponseEntity<ServiceRequestDTO> markAsReceived(
            @PathVariable Long id,
            @Valid @RequestBody ReceiveServiceRequestDTO dto) {
        // 현재 로그인한 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();
        
        ServiceRequestDTO serviceRequest = serviceRequestService.markAsReceived(id, currentUserId, dto);
        return ResponseEntity.ok(serviceRequest);
    }
    
    /**
     * AS 완료 처리
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<ServiceRequestDTO> markAsCompleted(
            @PathVariable Long id,
            @Valid @RequestBody CompleteServiceRequestDTO dto) {
        // 현재 로그인한 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();
        
        ServiceRequestDTO serviceRequest = serviceRequestService.markAsCompleted(id, currentUserId, dto);
        return ResponseEntity.ok(serviceRequest);
    }
} 