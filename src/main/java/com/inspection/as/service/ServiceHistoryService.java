package com.inspection.as.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inspection.as.dto.CreateServiceHistoryDTO;
import com.inspection.as.dto.ServiceHistoryDTO;
import com.inspection.as.entity.ServiceHistory;
import com.inspection.as.entity.ServiceRequest;
import com.inspection.as.repository.ServiceHistoryRepository;
import com.inspection.as.repository.ServiceRequestRepository;
import com.inspection.entity.Code;
import com.inspection.entity.User;
import com.inspection.repository.CodeRepository;
import com.inspection.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceHistoryService {
    
    private final ServiceHistoryRepository serviceHistoryRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final CodeRepository codeRepository;
    private final UserRepository userRepository;
    
    /**
     * 모든 AS 이력 조회
     */
    @Transactional(readOnly = true)
    public List<ServiceHistoryDTO> getAllServiceHistories() {
        return serviceHistoryRepository.findAll().stream()
                .map(ServiceHistoryDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * AS 이력 상세 조회
     */
    @Transactional(readOnly = true)
    public ServiceHistoryDTO getServiceHistoryById(Long id) {
        ServiceHistory serviceHistory = serviceHistoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AS 이력을 찾을 수 없습니다: " + id));
        
        return ServiceHistoryDTO.fromEntity(serviceHistory);
    }
    
    /**
     * 특정 AS 접수의 모든 이력 조회
     */
    @Transactional(readOnly = true)
    public List<ServiceHistoryDTO> getServiceHistoriesByServiceRequestId(Long serviceRequestId) {
        return serviceHistoryRepository.findByServiceRequestServiceRequestId(serviceRequestId).stream()
                .map(ServiceHistoryDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 특정 작업자가 수행한 모든 AS 이력 조회
     */
    @Transactional(readOnly = true)
    public List<ServiceHistoryDTO> getServiceHistoriesByPerformedById(Long performedById) {
        return serviceHistoryRepository.findByPerformedById(performedById).stream()
                .map(ServiceHistoryDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 특정 날짜 범위 내의 AS 이력 조회
     */
    @Transactional(readOnly = true)
    public List<ServiceHistoryDTO> getServiceHistoriesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return serviceHistoryRepository.findByActionDateBetween(startDate, endDate).stream()
                .map(ServiceHistoryDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 특정 작업 유형의 모든 AS 이력 조회
     */
    @Transactional(readOnly = true)
    public List<ServiceHistoryDTO> getServiceHistoriesByActionType(String actionTypeCode) {
        return serviceHistoryRepository.findByActionTypeCodeId(actionTypeCode).stream()
                .map(ServiceHistoryDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * AS 이력 생성
     */
    @Transactional
    public ServiceHistoryDTO createServiceHistory(CreateServiceHistoryDTO dto) {
        // AS 접수 조회
        ServiceRequest serviceRequest = serviceRequestRepository.findById(dto.getServiceRequestId())
                .orElseThrow(() -> new EntityNotFoundException("AS 접수를 찾을 수 없습니다: " + dto.getServiceRequestId()));
        
        // 작업 유형 코드 조회
        Code actionType = codeRepository.findById(dto.getActionTypeCode())
                .orElseThrow(() -> new EntityNotFoundException("작업 유형 코드를 찾을 수 없습니다: " + dto.getActionTypeCode()));
        
        // 작업자 조회 (선택적)
        User performedBy = null;
        if (dto.getPerformedById() != null) {
            performedBy = userRepository.findById(dto.getPerformedById())
                    .orElseThrow(() -> new EntityNotFoundException("작업자를 찾을 수 없습니다: " + dto.getPerformedById()));
        }
        
        // ServiceHistory 엔티티 생성
        ServiceHistory serviceHistory = ServiceHistory.builder()
                .serviceRequest(serviceRequest)
                .actionDate(LocalDateTime.now())
                .actionType(actionType)
                .actionDescription(dto.getActionDescription())
                .partsUsed(dto.getPartsUsed())
                .partsCost(dto.getPartsCost())
                .laborCost(dto.getLaborCost())
                .performedBy(performedBy)
                .build();
        
        ServiceHistory savedServiceHistory = serviceHistoryRepository.save(serviceHistory);
        
        return ServiceHistoryDTO.fromEntity(savedServiceHistory);
    }
    
    /**
     * AS 이력 수정
     */
    @Transactional
    public ServiceHistoryDTO updateServiceHistory(Long id, CreateServiceHistoryDTO dto) {
        ServiceHistory serviceHistory = serviceHistoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AS 이력을 찾을 수 없습니다: " + id));
        
        // AS 접수 조회 (선택적)
        if (dto.getServiceRequestId() != null && !dto.getServiceRequestId().equals(serviceHistory.getServiceRequest().getServiceRequestId())) {
            ServiceRequest serviceRequest = serviceRequestRepository.findById(dto.getServiceRequestId())
                    .orElseThrow(() -> new EntityNotFoundException("AS 접수를 찾을 수 없습니다: " + dto.getServiceRequestId()));
            serviceHistory.setServiceRequest(serviceRequest);
        }
        
        // 작업 유형 수정 (선택적)
        if (dto.getActionTypeCode() != null) {
            Code actionType = codeRepository.findById(dto.getActionTypeCode())
                    .orElseThrow(() -> new EntityNotFoundException("작업 유형 코드를 찾을 수 없습니다: " + dto.getActionTypeCode()));
            serviceHistory.setActionType(actionType);
        }
        
        // 작업 내용 수정 (선택적)
        if (dto.getActionDescription() != null) {
            serviceHistory.setActionDescription(dto.getActionDescription());
        }
        
        // 사용 부품 수정 (선택적)
        if (dto.getPartsUsed() != null) {
            serviceHistory.setPartsUsed(dto.getPartsUsed());
        }
        
        // 부품 비용 수정 (선택적)
        if (dto.getPartsCost() != null) {
            serviceHistory.setPartsCost(dto.getPartsCost());
        }
        
        // 인건비 수정 (선택적)
        if (dto.getLaborCost() != null) {
            serviceHistory.setLaborCost(dto.getLaborCost());
        }
        
        // 작업자 수정 (선택적)
        if (dto.getPerformedById() != null) {
            User performedBy = userRepository.findById(dto.getPerformedById())
                    .orElseThrow(() -> new EntityNotFoundException("작업자를 찾을 수 없습니다: " + dto.getPerformedById()));
            serviceHistory.setPerformedBy(performedBy);
        }
        
        ServiceHistory updatedServiceHistory = serviceHistoryRepository.save(serviceHistory);
        
        return ServiceHistoryDTO.fromEntity(updatedServiceHistory);
    }
    
    /**
     * AS 이력 삭제
     */
    @Transactional
    public void deleteServiceHistory(Long id) {
        ServiceHistory serviceHistory = serviceHistoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AS 이력을 찾을 수 없습니다: " + id));
        
        serviceHistoryRepository.delete(serviceHistory);
    }
} 