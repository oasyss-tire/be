package com.inspection.as.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Arrays;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.inspection.as.dto.CompleteServiceRequestDTO;
import com.inspection.as.dto.CreateServiceRequestDTO;
import com.inspection.as.dto.ReceiveServiceRequestDTO;
import com.inspection.as.dto.ServiceRequestDTO;
import com.inspection.as.dto.UpdateServiceRequestDTO;
import com.inspection.as.service.ServiceRequestService;
import com.inspection.as.specification.ServiceRequestSpecification;

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
     * 페이징된 AS 접수 목록 조회 (필터링 기능 포함)
     */
    @GetMapping("/paged")
    public ResponseEntity<Page<ServiceRequestDTO>> getServiceRequestsPaged(
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "companyName", required = false) String companyName,
            @RequestParam(value = "facilityTypeName", required = false) String facilityTypeName,
            @RequestParam(value = "brandName", required = false) String brandName,
            @RequestParam(value = "branchGroupId", required = false) String branchGroupId,
            @RequestParam(value = "branchGroupName", required = false) String branchGroupName,
            @RequestParam(value = "serviceStatusCode", required = false) String serviceStatusCode,
            @RequestParam(value = "serviceStatusName", required = false) String serviceStatusName,
            @RequestParam(value = "departmentTypeCode", required = false) String departmentTypeCode,
            @RequestParam(value = "departmentTypeName", required = false) String departmentTypeName,
            @RequestParam(value = "requestDateStart", required = false) 
                @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime requestDateStart,
            @RequestParam(value = "requestDateEnd", required = false) 
                @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime requestDateEnd) {
        
        // Specification을 사용한 필터링
        Page<ServiceRequestDTO> page = serviceRequestService.searchServiceRequests(
            ServiceRequestSpecification.withFilters(
                search,
                companyName,
                facilityTypeName,
                brandName,
                branchGroupId,
                branchGroupName,
                serviceStatusCode,
                serviceStatusName,
                departmentTypeCode,
                departmentTypeName,
                requestDateStart,
                requestDateEnd
            ), 
            pageable
        );
        
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
        ServiceRequestDTO serviceRequest = serviceRequestService.getServiceRequestWithAll(id);
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
     * AS 접수 상세 조회 (이력 및 이미지 포함)
     */
    @GetMapping("/{id}/all")
    public ResponseEntity<ServiceRequestDTO> getServiceRequestWithAll(@PathVariable Long id) {
        ServiceRequestDTO serviceRequest = serviceRequestService.getServiceRequestWithAll(id);
        return ResponseEntity.ok(serviceRequest);
    }
    
    /**
     * AS 접수 생성 (이미지 포함)
     */
    @PostMapping("/with-images")
    public ResponseEntity<ServiceRequestDTO> createServiceRequestWithImages(
            @RequestParam("request") String requestJson,
            @RequestParam(value = "images", required = false) MultipartFile[] images) {
        
        try {
            // JSON 문자열을 DTO로 변환
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule()); // JavaTimeModule 등록
            CreateServiceRequestDTO dto = objectMapper.readValue(requestJson, CreateServiceRequestDTO.class);
            
            log.info("이미지 첨부 AS 접수 요청: 시설물 ID={}, 요청 내용={}", 
                    dto.getFacilityId(), dto.getRequestContent());
            
            // 현재 로그인한 사용자 정보 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserId = authentication.getName();
            
            // AS 접수 생성
            ServiceRequestDTO createdServiceRequest = serviceRequestService.createServiceRequest(dto, currentUserId);
            
            // 이미지가 제공된 경우 업로드 처리
            if (images != null && images.length > 0) {
                for (MultipartFile image : images) {
                    if (!image.isEmpty()) {
                        try {
                            serviceRequestService.uploadImage(
                                    createdServiceRequest.getServiceRequestId(), 
                                    image, 
                                    "002005_0008", // AS 접수 이미지 코드
                                    currentUserId);
                        } catch (Exception e) {
                            log.error("AS 접수 이미지 업로드 중 오류 발생: {}", e.getMessage(), e);
                            // 이미지 업로드 실패가 AS 접수 자체를 실패시키지 않도록 예외 처리
                        }
                    }
                }
            }
            
            return ResponseEntity.status(HttpStatus.CREATED).body(createdServiceRequest);
        } catch (Exception e) {
            log.error("AS 접수 요청 처리 중 오류: {}", e.getMessage(), e);
            throw new RuntimeException("AS 접수 처리 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 기존 AS 접수 생성 API 유지 (이미지 없는 버전)
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
     * AS 완료 처리 (이미지 포함)
     */
    @PutMapping("/{id}/complete-with-images")
    public ResponseEntity<ServiceRequestDTO> markAsCompletedWithImages(
            @PathVariable Long id,
            @RequestParam("request") String requestJson,
            @RequestParam(value = "images", required = false) MultipartFile[] images) {
        
        try {
            // JSON 문자열을 DTO로 변환
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule()); // JavaTimeModule 등록
            CompleteServiceRequestDTO dto = objectMapper.readValue(requestJson, CompleteServiceRequestDTO.class);
            
            log.info("이미지 첨부 AS 완료 요청: AS 접수 ID={}, 수리 비용={}", id, dto.getCost());
            
            // 현재 로그인한 사용자 정보 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserId = authentication.getName();
            
            // AS 완료 처리
            ServiceRequestDTO serviceRequest = serviceRequestService.markAsCompleted(id, currentUserId, dto);
            
            // 이미지가 제공된 경우 업로드 처리
            if (images != null && images.length > 0) {
                for (MultipartFile image : images) {
                    if (!image.isEmpty()) {
                        try {
                            serviceRequestService.uploadImage(
                                    id, 
                                    image, 
                                    "002005_0009", // AS 완료 이미지 코드
                                    currentUserId);
                        } catch (Exception e) {
                            log.error("AS 완료 이미지 업로드 중 오류 발생: {}", e.getMessage(), e);
                            // 이미지 업로드 실패가 AS 완료 자체를 실패시키지 않도록 예외 처리
                        }
                    }
                }
            }
            
            return ResponseEntity.ok(serviceRequest);
        } catch (Exception e) {
            log.error("AS 완료 요청 처리 중 오류: {}", e.getMessage(), e);
            throw new RuntimeException("AS 완료 처리 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 기존 AS 완료 처리 API 유지 (이미지 없는 버전)
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