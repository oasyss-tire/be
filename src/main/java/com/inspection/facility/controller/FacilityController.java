package com.inspection.facility.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import com.inspection.facility.dto.FacilityBatchCreateRequest;
import com.inspection.facility.dto.FacilityCreateRequest;
import com.inspection.facility.dto.FacilityDTO;
import com.inspection.facility.dto.FacilitySearchRequest;
import com.inspection.facility.dto.FacilityUpdateRequest;
import com.inspection.facility.dto.FacilityUsefulLifeUpdateDto;
import com.inspection.facility.service.FacilityService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/facilities")
@RequiredArgsConstructor
public class FacilityController {
    
    private final FacilityService facilityService;
    
    /**
     * 페이징 처리된 시설물 목록 조회 (검색 가능)
     */
    @GetMapping
    public ResponseEntity<Page<FacilityDTO>> getFacilities(
            FacilitySearchRequest searchRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "facilityId") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? 
                Sort.by(sortBy).ascending() : 
                Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(facilityService.searchFacilities(searchRequest, pageable));
    }
    
    /**
     * 간단 키워드 검색
     */
    @GetMapping("/search")
    public ResponseEntity<Page<FacilityDTO>> quickSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "facilityId") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? 
                Sort.by(sortBy).ascending() : 
                Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(facilityService.quickSearch(keyword, companyId, pageable));
    }
    
    /**
     * ID로 시설물 조회
     */
    @GetMapping("/{facilityId}")
    public ResponseEntity<FacilityDTO> getFacilityById(@PathVariable Long facilityId) {
        return ResponseEntity.ok(facilityService.getFacilityById(facilityId));
    }
    
    /**
     * 보증 만료 예정 시설물 조회
     */
    @GetMapping("/warranty-expiring")
    public ResponseEntity<List<FacilityDTO>> getWarrantyExpiringFacilities(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Long companyId) {
        return ResponseEntity.ok(facilityService.getWarrantyExpiringFacilities(startDate, endDate, companyId));
    }
    
    /**
     * 시설물 생성
     */
    @PostMapping
    public ResponseEntity<FacilityDTO> createFacility(@Valid @RequestBody FacilityCreateRequest request) {
        FacilityDTO createdFacility = facilityService.createFacility(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdFacility);
    }
    
    /**
     * 시설물 배치 생성
     */
    @PostMapping("/batch")
    public ResponseEntity<List<FacilityDTO>> createFacilityBatch(@Valid @RequestBody FacilityBatchCreateRequest request) {
        List<FacilityDTO> createdFacilities = facilityService.createFacilityBatch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdFacilities);
    }
    
    /**
     * 시설물 수정
     */
    @PutMapping("/{facilityId}")
    public ResponseEntity<FacilityDTO> updateFacility(
            @PathVariable Long facilityId,
            @Valid @RequestBody FacilityUpdateRequest request) {
        FacilityDTO updatedFacility = facilityService.updateFacility(facilityId, request);
        return ResponseEntity.ok(updatedFacility);
    }
    
    /**
     * 시설물 삭제
     */
    @DeleteMapping("/{facilityId}")
    public ResponseEntity<Void> deleteFacility(@PathVariable Long facilityId) {
        facilityService.deleteFacility(facilityId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 시설물 유형에 따른 브랜드 코드 조회
     */
    @GetMapping("/facility-type/{facilityTypeCode}/brands")
    public ResponseEntity<List<com.inspection.dto.CodeDTO>> getBrandsByFacilityType(@PathVariable String facilityTypeCode) {
        return ResponseEntity.ok(facilityService.getBrandCodesByFacilityType(facilityTypeCode));
    }
    
    /**
     * 시설물 유형별 총 수량 조회 (활성화된 시설물만 포함)
     */
    @GetMapping("/counts-by-type")
    public ResponseEntity<Map<String, Object>> getFacilityCountsByType() {
        Map<String, Object> counts = facilityService.getFacilityCountsByType();
        return ResponseEntity.ok(counts);
    }
    
    /**
     * 사용연한만 수정하는 API
     */
    @PutMapping("/useful-life")
    public ResponseEntity<FacilityDTO> updateUsefulLifeMonths(
            @Valid @RequestBody FacilityUsefulLifeUpdateDto request) {
        FacilityDTO updatedFacility = facilityService.updateUsefulLifeMonths(
                request.getId(), 
                request.getUsefulLifeMonths(), 
                request.getUsefulLifeUpdateReason());
        return ResponseEntity.ok(updatedFacility);
    }
} 