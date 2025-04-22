package com.inspection.as.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inspection.as.dto.CompleteServiceRequestDTO;
import com.inspection.as.dto.CreateServiceRequestDTO;
import com.inspection.as.dto.ReceiveServiceRequestDTO;
import com.inspection.as.dto.ServiceRequestDTO;
import com.inspection.as.dto.UpdateServiceRequestDTO;
import com.inspection.as.entity.ServiceRequest;
import com.inspection.as.repository.ServiceRequestRepository;
import com.inspection.entity.Code;
import com.inspection.entity.Company;
import com.inspection.entity.User;
import com.inspection.facility.entity.Facility;
import com.inspection.facility.repository.FacilityRepository;
import com.inspection.repository.CodeRepository;
import com.inspection.repository.CompanyRepository;
import com.inspection.repository.UserRepository;
import com.inspection.facility.dto.ServiceTransactionRequest;
import com.inspection.facility.service.FacilityTransactionService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceRequestService {
    
    private final ServiceRequestRepository serviceRequestRepository;
    private final FacilityRepository facilityRepository;
    private final CodeRepository codeRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final FacilityTransactionService facilityTransactionService;
    
    /**
     * 모든 AS 접수 조회
     */
    @Transactional(readOnly = true)
    public List<ServiceRequestDTO> getAllServiceRequests() {
        List<ServiceRequestDTO> dtoList = serviceRequestRepository.findAll().stream()
                .map(ServiceRequestDTO::fromEntity)
                .collect(Collectors.toList());
        
        // 회사 정보 설정
        for (ServiceRequestDTO dto : dtoList) {
            Long companyId = dto.getFacilityId() != null ? 
                    facilityRepository.findById(dto.getFacilityId())
                    .map(this::getCompanyIdFromFacility)
                    .orElse(null) : null;
            
            dto.setCompanyName(getCompanyName(companyId));
        }
        
        return dtoList;
    }
    
    /**
     * 페이징된 AS 접수 조회
     */
    @Transactional(readOnly = true)
    public Page<ServiceRequestDTO> getServiceRequests(Pageable pageable) {
        Page<ServiceRequestDTO> dtoPage = serviceRequestRepository.findAll(pageable)
                .map(ServiceRequestDTO::fromEntity);
        
        // 회사 정보 설정
        dtoPage.getContent().forEach(dto -> {
            Long companyId = dto.getFacilityId() != null ? 
                    facilityRepository.findById(dto.getFacilityId())
                    .map(this::getCompanyIdFromFacility)
                    .orElse(null) : null;
            
            dto.setCompanyName(getCompanyName(companyId));
        });
        
        return dtoPage;
    }
    
    /**
     * AS 접수 상세 조회
     */
    @Transactional(readOnly = true)
    public ServiceRequestDTO getServiceRequestById(Long id) {
        ServiceRequest serviceRequest = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AS 접수를 찾을 수 없습니다: " + id));
        
        ServiceRequestDTO dto = ServiceRequestDTO.fromEntity(serviceRequest);
        
        // 회사 정보 설정
        Long companyId = getCompanyIdFromFacility(serviceRequest.getFacility());
        dto.setCompanyName(getCompanyName(companyId));
        
        return dto;
    }
    
    /**
     * AS 접수 상세 조회 (이력 포함)
     */
    @Transactional(readOnly = true)
    public ServiceRequestDTO getServiceRequestWithHistories(Long id) {
        ServiceRequest serviceRequest = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AS 접수를 찾을 수 없습니다: " + id));
        
        ServiceRequestDTO dto = ServiceRequestDTO.fromEntityWithHistories(serviceRequest);
        
        // 회사 정보 설정
        Long companyId = getCompanyIdFromFacility(serviceRequest.getFacility());
        dto.setCompanyName(getCompanyName(companyId));
        
        return dto;
    }
    
    /**
     * 특정 시설물의 AS 접수 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ServiceRequestDTO> getServiceRequestsByFacilityId(Long facilityId) {
        List<ServiceRequestDTO> dtoList = serviceRequestRepository.findByFacilityFacilityId(facilityId).stream()
                .map(ServiceRequestDTO::fromEntity)
                .collect(Collectors.toList());
        
        // 회사 정보 설정
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new EntityNotFoundException("시설물을 찾을 수 없습니다: " + facilityId));
        
        Long companyId = getCompanyIdFromFacility(facility);
        String companyName = getCompanyName(companyId);
        
        dtoList.forEach(dto -> dto.setCompanyName(companyName));
        
        return dtoList;
    }
    
    /**
     * 특정 시설물의 AS 접수 목록을 serviceRequestId 기준 최신순으로 조회
     */
    @Transactional(readOnly = true)
    public List<ServiceRequestDTO> getServiceRequestsByFacilityIdOrderByLatest(Long facilityId) {
        List<ServiceRequestDTO> dtoList = serviceRequestRepository.findByFacilityFacilityIdOrderByServiceRequestIdDesc(facilityId).stream()
                .map(ServiceRequestDTO::fromEntity)
                .collect(Collectors.toList());
        
        // 회사 정보 설정
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new EntityNotFoundException("시설물을 찾을 수 없습니다: " + facilityId));
        
        Long companyId = getCompanyIdFromFacility(facility);
        String companyName = getCompanyName(companyId);
        
        dtoList.forEach(dto -> dto.setCompanyName(companyName));
        
        return dtoList;
    }
    
    /**
     * 특정 관리자에게 배정된 AS 접수 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ServiceRequestDTO> getServiceRequestsByManagerId(Long managerId) {
        List<ServiceRequestDTO> dtoList = serviceRequestRepository.findByManagerId(managerId).stream()
                .map(ServiceRequestDTO::fromEntity)
                .collect(Collectors.toList());
        
        // 회사 정보 설정
        dtoList.forEach(dto -> {
            Long companyId = dto.getFacilityId() != null ? 
                    facilityRepository.findById(dto.getFacilityId())
                    .map(this::getCompanyIdFromFacility)
                    .orElse(null) : null;
            
            dto.setCompanyName(getCompanyName(companyId));
        });
        
        return dtoList;
    }
    
    /**
     * 특정 사용자가 요청한 AS 접수 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ServiceRequestDTO> getServiceRequestsByRequesterId(Long requesterId) {
        List<ServiceRequestDTO> dtoList = serviceRequestRepository.findByRequesterId(requesterId).stream()
                .map(ServiceRequestDTO::fromEntity)
                .collect(Collectors.toList());
        
        // 회사 정보 설정
        dtoList.forEach(dto -> {
            Long companyId = dto.getFacilityId() != null ? 
                    facilityRepository.findById(dto.getFacilityId())
                    .map(this::getCompanyIdFromFacility)
                    .orElse(null) : null;
            
            dto.setCompanyName(getCompanyName(companyId));
        });
        
        return dtoList;
    }
    
    /**
     * 완료되지 않은 AS 접수 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ServiceRequestDTO> getIncompleteServiceRequests() {
        List<ServiceRequestDTO> dtoList = serviceRequestRepository.findByIsCompletedFalse().stream()
                .map(ServiceRequestDTO::fromEntity)
                .collect(Collectors.toList());
        
        // 회사 정보 설정
        dtoList.forEach(dto -> {
            Long companyId = dto.getFacilityId() != null ? 
                    facilityRepository.findById(dto.getFacilityId())
                    .map(this::getCompanyIdFromFacility)
                    .orElse(null) : null;
            
            dto.setCompanyName(getCompanyName(companyId));
        });
        
        return dtoList;
    }
    
    /**
     * 특정 서비스 유형의 AS 접수 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ServiceRequestDTO> getServiceRequestsByServiceType(String serviceTypeCode) {
        List<ServiceRequestDTO> dtoList = serviceRequestRepository.findByServiceTypeCodeId(serviceTypeCode).stream()
                .map(ServiceRequestDTO::fromEntity)
                .collect(Collectors.toList());
        
        // 회사 정보 설정
        dtoList.forEach(dto -> {
            Long companyId = dto.getFacilityId() != null ? 
                    facilityRepository.findById(dto.getFacilityId())
                    .map(this::getCompanyIdFromFacility)
                    .orElse(null) : null;
            
            dto.setCompanyName(getCompanyName(companyId));
        });
        
        return dtoList;
    }
    
    /**
     * 특정 우선순위의 AS 접수 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ServiceRequestDTO> getServiceRequestsByPriority(String priorityCode) {
        List<ServiceRequestDTO> dtoList = serviceRequestRepository.findByPriorityCodeId(priorityCode).stream()
                .map(ServiceRequestDTO::fromEntity)
                .collect(Collectors.toList());
        
        // 회사 정보 설정
        dtoList.forEach(dto -> {
            Long companyId = dto.getFacilityId() != null ? 
                    facilityRepository.findById(dto.getFacilityId())
                    .map(this::getCompanyIdFromFacility)
                    .orElse(null) : null;
            
            dto.setCompanyName(getCompanyName(companyId));
        });
        
        return dtoList;
    }
    
    /**
     * 특정 일수 내에 예상 완료일인 AS 접수 조회
     */
    @Transactional(readOnly = true)
    public List<ServiceRequestDTO> getUpcomingDueServiceRequests(int days) {
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime dueDate = today.plusDays(days);
        
        List<ServiceRequestDTO> dtoList = serviceRequestRepository.findByExpectedCompletionDateBetweenAndIsCompletedFalse(today, dueDate).stream()
                .map(ServiceRequestDTO::fromEntity)
                .collect(Collectors.toList());
        
        // 회사 정보 설정
        dtoList.forEach(dto -> {
            Long companyId = dto.getFacilityId() != null ? 
                    facilityRepository.findById(dto.getFacilityId())
                    .map(this::getCompanyIdFromFacility)
                    .orElse(null) : null;
            
            dto.setCompanyName(getCompanyName(companyId));
        });
        
        return dtoList;
    }
    
    /**
     * 특정 날짜 범위 내의 AS 접수 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ServiceRequestDTO> getServiceRequestsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<ServiceRequestDTO> dtoList = serviceRequestRepository.findByRequestDateBetween(startDate, endDate).stream()
                .map(ServiceRequestDTO::fromEntity)
                .collect(Collectors.toList());
        
        // 회사 정보 설정
        dtoList.forEach(dto -> {
            Long companyId = dto.getFacilityId() != null ? 
                    facilityRepository.findById(dto.getFacilityId())
                    .map(this::getCompanyIdFromFacility)
                    .orElse(null) : null;
            
            dto.setCompanyName(getCompanyName(companyId));
        });
        
        return dtoList;
    }
    
    /**
     * 단일 ServiceRequestDTO의 회사 정보를 설정하는 헬퍼 메소드
     */
    private ServiceRequestDTO enrichDtoWithCompanyInfo(ServiceRequestDTO dto, Facility facility) {
        Long companyId = getCompanyIdFromFacility(facility);
        dto.setCompanyName(getCompanyName(companyId));
        return dto;
    }
    
    /**
     * AS 접수 생성
     */
    @Transactional
    public ServiceRequestDTO createServiceRequest(CreateServiceRequestDTO dto, String userId) {
        // 유저 정보 가져오기
        User requester = userRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        
        // 시설물 정보 가져오기
        Facility facility = facilityRepository.findById(dto.getFacilityId())
                .orElseThrow(() -> new EntityNotFoundException("시설물을 찾을 수 없습니다: " + dto.getFacilityId()));
        
        // 시설물 원래 위치 정보 가져오기
        Long originalLocationCompanyId = null;
        if (facility.getLocationCompany() != null) {
            originalLocationCompanyId = facility.getLocationCompany().getId();
        }
        
        // 서비스 유형 코드 가져오기
        Code serviceType = codeRepository.findById(dto.getServiceTypeCode())
                .orElseThrow(() -> new EntityNotFoundException("서비스 유형 코드를 찾을 수 없습니다: " + dto.getServiceTypeCode()));
        
        // 우선순위 코드 가져오기
        Code priority = codeRepository.findById(dto.getPriorityCode())
                .orElseThrow(() -> new EntityNotFoundException("우선순위 코드를 찾을 수 없습니다: " + dto.getPriorityCode()));
        
        // 상태 코드 가져오기 - 기본값으로 "접수 중" 상태 사용
        Code status = codeRepository.findById("002010_0001") // 접수 중 상태 코드
                .orElseThrow(() -> new EntityNotFoundException("상태 코드를 찾을 수 없습니다: 002010_0001"));
        
        // 접수번호 생성
        String requestNumber = generateRequestNumber();
        
        // ServiceRequest 객체 생성
        ServiceRequest serviceRequest = ServiceRequest.builder()
                .requestNumber(requestNumber)
                .facility(facility)
                .requestDate(dto.getRequestDate())
                .requestContent(dto.getRequestContent())
                .requester(requester)
                .isReceived(false)
                .isCompleted(false)
                .serviceType(serviceType)
                .priority(priority)
                .status(status)
                .notes(dto.getNotes())
                .originalLocationCompanyId(originalLocationCompanyId)  // 원래 위치 회사 ID 저장
                .build();
        
        ServiceRequest savedServiceRequest = serviceRequestRepository.save(serviceRequest);
        
        return enrichDtoWithCompanyInfo(ServiceRequestDTO.fromEntity(savedServiceRequest), facility);
    }
    
    /**
     * 이전 메소드 호환성 위한 오버로드 메소드
     */
    @Transactional
    public ServiceRequestDTO createServiceRequest(CreateServiceRequestDTO dto) {
        // 요청자 ID가 제공된 경우 해당 ID를 사용
        if (dto.getRequesterId() != null) {
            User requester = userRepository.findById(dto.getRequesterId())
                    .orElseThrow(() -> new EntityNotFoundException("요청자를 찾을 수 없습니다: " + dto.getRequesterId()));
            return createServiceRequestWithUser(dto, requester);
        } else {
            throw new IllegalArgumentException("요청자 ID가 제공되지 않았습니다.");
        }
    }
    
    /**
     * 실제 ServiceRequest 생성을 처리하는 비공개 메소드
     */
    private ServiceRequestDTO createServiceRequestWithUser(CreateServiceRequestDTO dto, User requester) {
        // 시설물 조회
        Facility facility = facilityRepository.findById(dto.getFacilityId())
                .orElseThrow(() -> new EntityNotFoundException("시설물을 찾을 수 없습니다: " + dto.getFacilityId()));
        
        // 서비스 유형 코드 조회
        Code serviceType = codeRepository.findById(dto.getServiceTypeCode())
                .orElseThrow(() -> new EntityNotFoundException("서비스 유형 코드를 찾을 수 없습니다: " + dto.getServiceTypeCode()));
        
        // 우선순위 코드 조회
        Code priority = codeRepository.findById(dto.getPriorityCode())
                .orElseThrow(() -> new EntityNotFoundException("우선순위 코드를 찾을 수 없습니다: " + dto.getPriorityCode()));
        
        // 관리자 조회 (선택적)
        User manager = null;
        if (dto.getManagerId() != null) {
            manager = userRepository.findById(dto.getManagerId())
                    .orElseThrow(() -> new EntityNotFoundException("관리자를 찾을 수 없습니다: " + dto.getManagerId()));
        }
        
        // 접수번호 생성 (현재 날짜 + 일련번호)
        String requestNumber = generateRequestNumber();
        
        // 서비스 요청 상태를 "접수 중"으로 설정
        Code serviceStatus = codeRepository.findById("002010_0001") // 접수 중 상태 코드
                .orElseThrow(() -> new EntityNotFoundException("서비스 요청 상태 코드를 찾을 수 없습니다: 002010_0001"));
        
        // ServiceRequest 엔티티 생성
        ServiceRequest serviceRequest = ServiceRequest.builder()
                .requestNumber(requestNumber)
                .facility(facility)
                .requestDate(dto.getRequestDate())
                .isReceived(dto.getIsReceived())
                .requestContent(dto.getRequestContent())
                .requester(requester)
                .manager(manager)
                .expectedCompletionDate(dto.getExpectedCompletionDate())
                .isCompleted(dto.getIsCompleted())
                .serviceType(serviceType)
                .priority(priority)
                .status(serviceStatus) // 서비스 요청 상태 설정
                .cost(dto.getCost())
                .notes(dto.getNotes())
                .build();
        
        // 시설물 상태 업데이트: 수리중(002003_0002)
        Code repairStatus = codeRepository.findById("002003_0002")
                .orElseThrow(() -> new EntityNotFoundException("시설물 상태 코드를 찾을 수 없습니다: 002003_0002"));
        facility.setStatus(repairStatus);
        facilityRepository.save(facility);
        
        ServiceRequest savedServiceRequest = serviceRequestRepository.save(serviceRequest);
        
        ServiceRequestDTO resultDto = ServiceRequestDTO.fromEntity(savedServiceRequest);
        return enrichDtoWithCompanyInfo(resultDto, facility);
    }
    
    /**
     * AS 접수 수정
     */
    @Transactional
    public ServiceRequestDTO updateServiceRequest(Long id, UpdateServiceRequestDTO dto) {
        ServiceRequest serviceRequest = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AS 접수를 찾을 수 없습니다: " + id));
        
        // 요청일 수정 (선택적)
        if (dto.getRequestDate() != null) {
            serviceRequest.setRequestDate(dto.getRequestDate());
        }
        
        // 접수 여부 수정 (선택적)
        if (dto.getIsReceived() != null) {
            serviceRequest.setIsReceived(dto.getIsReceived());
        }
        
        // 접수 내용 수정 (선택적)
        if (dto.getRequestContent() != null) {
            serviceRequest.setRequestContent(dto.getRequestContent());
        }
        
        // 관리자 수정 (선택적)
        if (dto.getManagerId() != null) {
            User manager = userRepository.findById(dto.getManagerId())
                    .orElseThrow(() -> new EntityNotFoundException("관리자를 찾을 수 없습니다: " + dto.getManagerId()));
            serviceRequest.setManager(manager);
        }
        
        // 예상 완료일 수정 (선택적)
        if (dto.getExpectedCompletionDate() != null) {
            serviceRequest.setExpectedCompletionDate(dto.getExpectedCompletionDate());
        }
        
        // 완료일 수정 (선택적)
        if (dto.getCompletionDate() != null) {
            serviceRequest.setCompletionDate(dto.getCompletionDate());
        }
        
        // 완료 여부 수정 (선택적)
        if (dto.getIsCompleted() != null) {
            serviceRequest.setIsCompleted(dto.getIsCompleted());
        }
        
        // 서비스 유형 수정 (선택적)
        if (dto.getServiceTypeCode() != null) {
            Code serviceType = codeRepository.findById(dto.getServiceTypeCode())
                    .orElseThrow(() -> new EntityNotFoundException("서비스 유형 코드를 찾을 수 없습니다: " + dto.getServiceTypeCode()));
            serviceRequest.setServiceType(serviceType);
        }
        
        // 우선순위 수정 (선택적)
        if (dto.getPriorityCode() != null) {
            Code priority = codeRepository.findById(dto.getPriorityCode())
                    .orElseThrow(() -> new EntityNotFoundException("우선순위 코드를 찾을 수 없습니다: " + dto.getPriorityCode()));
            serviceRequest.setPriority(priority);
        }
        
        // 비용 수정 (선택적)
        if (dto.getCost() != null) {
            serviceRequest.setCost(dto.getCost());
        }
        
        // 비고 수정 (선택적)
        if (dto.getNotes() != null) {
            serviceRequest.setNotes(dto.getNotes());
        }
        
        ServiceRequest updatedServiceRequest = serviceRequestRepository.save(serviceRequest);
        
        ServiceRequestDTO resultDto = ServiceRequestDTO.fromEntity(updatedServiceRequest);
        return enrichDtoWithCompanyInfo(resultDto, updatedServiceRequest.getFacility());
    }
    
    /**
     * AS 접수 삭제
     */
    @Transactional
    public void deleteServiceRequest(Long id) {
        ServiceRequest serviceRequest = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AS 접수를 찾을 수 없습니다: " + id));
        
        serviceRequestRepository.delete(serviceRequest);
    }
    
    /**
     * AS 접수 완료 처리 (로그인한 사용자를 담당자로 설정)
     */
    @Transactional
    public ServiceRequestDTO markAsReceived(Long id, String userId, ReceiveServiceRequestDTO dto) {
        ServiceRequest serviceRequest = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AS 접수를 찾을 수 없습니다: " + id));
        
        // 현재 로그인한 사용자를 담당자로 설정
        User manager = userRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        
        serviceRequest.setIsReceived(true);
        serviceRequest.setManager(manager);
        serviceRequest.setExpectedCompletionDate(dto.getExpectedCompletionDate());
        
        // 서비스 요청 상태를 "접수 완료"로 변경
        Code serviceStatus = codeRepository.findById("002010_0002") // 접수 완료 상태 코드
                .orElseThrow(() -> new EntityNotFoundException("서비스 요청 상태 코드를 찾을 수 없습니다: 002010_0002"));
        serviceRequest.setStatus(serviceStatus);
        
        // 시설물 상태는 수리중(002003_0002)로 유지
        Facility facility = serviceRequest.getFacility();
        Code repairStatus = codeRepository.findById("002003_0002")
                .orElseThrow(() -> new EntityNotFoundException("시설물 상태 코드를 찾을 수 없습니다: 002003_0002"));
        facility.setStatus(repairStatus);
        facilityRepository.save(facility);
        
        ServiceRequest updatedServiceRequest = serviceRequestRepository.save(serviceRequest);
        
        // 시설물 이동 트랜잭션 생성 (요청자 회사 -> AS센터)
        try {
            if (facility != null) {
                ServiceTransactionRequest transactionRequest = new ServiceTransactionRequest();
                transactionRequest.setFacilityId(facility.getFacilityId());
                transactionRequest.setServiceRequestId(serviceRequest.getServiceRequestId());
                
                // 출발지: 시설물의 현재 위치 회사
                Long fromCompanyId = getCompanyIdFromFacility(facility);
                if (fromCompanyId != null) {
                    transactionRequest.setFromCompanyId(fromCompanyId);
                    
                    // 도착지: AS 처리를 수행하는 회사 (기본값으로 관리자의 소속 회사 사용)
                    Long toCompanyId = manager.getCompany() != null ? manager.getCompany().getId() : null;
                    if (toCompanyId != null) {
                        transactionRequest.setToCompanyId(toCompanyId);
                        transactionRequest.setIsReturn(false); // AS센터로 이동
                        transactionRequest.setNotes("AS 접수 완료에 따른 수리 센터 이동");
                        
                        // 트랜잭션 생성
                        facilityTransactionService.processService(transactionRequest);
                    } else {
                        log.warn("관리자의 소속 회사 정보가 없어 AS 이동 트랜잭션을 생성할 수 없습니다. 관리자 ID: {}", manager.getId());
                    }
                } else {
                    log.warn("시설물의 위치 회사 정보가 없어 AS 이동 트랜잭션을 생성할 수 없습니다. 시설물 ID: {}", facility.getFacilityId());
                }
            }
        } catch (Exception e) {
            log.error("시설물 AS 이동 트랜잭션 생성 중 오류 발생: {}", e.getMessage(), e);
            // 트랜잭션 생성 실패가 AS 접수 자체를 실패시키지 않도록 예외 처리
        }
        
        ServiceRequestDTO resultDto = ServiceRequestDTO.fromEntity(updatedServiceRequest);
        return enrichDtoWithCompanyInfo(resultDto, facility);
    }
    
    /**
     * AS 접수 완료 처리 (기존 호환성 유지)
     */
    @Transactional
    public ServiceRequestDTO markAsReceived(Long id) {
        ServiceRequest serviceRequest = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AS 접수를 찾을 수 없습니다: " + id));
        
        serviceRequest.setIsReceived(true);
        
        // 서비스 요청 상태를 "접수 완료"로 변경
        Code serviceStatus = codeRepository.findById("002010_0002") // 접수 완료 상태 코드
                .orElseThrow(() -> new EntityNotFoundException("서비스 요청 상태 코드를 찾을 수 없습니다: 002010_0002"));
        serviceRequest.setStatus(serviceStatus);
        
        // 시설물 상태는 수리중(002003_0002)로 유지
        Facility facility = serviceRequest.getFacility();
        Code repairStatus = codeRepository.findById("002003_0002")
                .orElseThrow(() -> new EntityNotFoundException("시설물 상태 코드를 찾을 수 없습니다: 002003_0002"));
        facility.setStatus(repairStatus);
        facilityRepository.save(facility);
        
        ServiceRequest updatedServiceRequest = serviceRequestRepository.save(serviceRequest);
        
        ServiceRequestDTO resultDto = ServiceRequestDTO.fromEntity(updatedServiceRequest);
        return enrichDtoWithCompanyInfo(resultDto, facility);
    }
    
    /**
     * AS 완료 처리 (로그인한 사용자를 담당자로 설정, 비용 입력)
     */
    @Transactional
    public ServiceRequestDTO markAsCompleted(Long id, String userId, CompleteServiceRequestDTO dto) {
        ServiceRequest serviceRequest = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AS 접수를 찾을 수 없습니다: " + id));
        
        // 현재 로그인한 사용자를 담당자로 설정
        User manager = userRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        
        serviceRequest.setIsCompleted(true);
        serviceRequest.setCompletionDate(LocalDateTime.now());
        serviceRequest.setManager(manager);
        serviceRequest.setCost(dto.getCost());
        
        // 서비스 요청 상태를 "AS 수리완료"로 변경
        Code serviceStatus = codeRepository.findById("002010_0003") // AS 수리완료 상태 코드
                .orElseThrow(() -> new EntityNotFoundException("서비스 요청 상태 코드를 찾을 수 없습니다: 002010_0003"));
        serviceRequest.setStatus(serviceStatus);
        
        // 시설물 상태 업데이트: 사용 중(002003_0001)으로 복원
        Facility facility = serviceRequest.getFacility();
        Code normalStatus = codeRepository.findById("002003_0001") // 사용 중 상태 코드
                .orElseThrow(() -> new EntityNotFoundException("시설물 상태 코드를 찾을 수 없습니다: 002003_0001"));
        facility.setStatus(normalStatus);
        
        // 원래 위치 회사가 저장되어 있다면, 시설물의 위치를 원래 회사로 복구
        if (serviceRequest.getOriginalLocationCompanyId() != null) {
            Company originalCompany = companyRepository.findById(serviceRequest.getOriginalLocationCompanyId())
                .orElseThrow(() -> new EntityNotFoundException("원래 위치 회사를 찾을 수 없습니다: " + serviceRequest.getOriginalLocationCompanyId()));

            
            facility.setLocationCompany(originalCompany);
        } else {
            log.warn("시설물의 원래 위치 정보가 없어 위치를 복구할 수 없습니다. 시설물 ID: {}", facility.getFacilityId());
        }
        
        facilityRepository.save(facility);
        
        ServiceRequest updatedServiceRequest = serviceRequestRepository.save(serviceRequest);

        
        // 시설물 복귀 트랜잭션 생성 (AS센터 -> 요청자 회사)
        try {
            if (facility != null) {
                // 관련 AS 이동 트랜잭션 찾기 (없을 수도 있음)
                Long relatedTransactionId = null;
                try {
                    List<com.inspection.facility.entity.FacilityTransaction> transactions = 
                            facilityTransactionService.getTransactionsByServiceRequestId(serviceRequest.getServiceRequestId());
                    
                    if (!transactions.isEmpty()) {
                        // 가장 최근 트랜잭션 ID를 관련 트랜잭션으로 설정
                        relatedTransactionId = transactions.get(0).getTransactionId();
                    }
                } catch (Exception e) {
                    log.warn("관련 AS 이동 트랜잭션을 찾는 중 오류 발생: {}", e.getMessage());
                }
                
                ServiceTransactionRequest transactionRequest = new ServiceTransactionRequest();
                transactionRequest.setFacilityId(facility.getFacilityId());
                transactionRequest.setServiceRequestId(serviceRequest.getServiceRequestId());
                
                // 출발지: 현재 관리자의 소속 회사 (AS 센터)
                Long fromCompanyId = manager.getCompany() != null ? manager.getCompany().getId() : null;
                if (fromCompanyId != null) {
                    transactionRequest.setFromCompanyId(fromCompanyId);
                    
                    // 도착지: 원래 시설물 위치 회사 (originalLocationCompanyId 사용)
                    Long toCompanyId = serviceRequest.getOriginalLocationCompanyId();
                    
                    // 원래 위치 정보가 없는 경우 대체 로직 (이전과 동일)
                    if (toCompanyId == null) {
                        // 요청자의 회사 정보 시도
                        if (serviceRequest.getRequester() != null && 
                            serviceRequest.getRequester().getCompany() != null) {
                            toCompanyId = serviceRequest.getRequester().getCompany().getId();
                        }
                        // 시설물의 소유 회사 정보 시도
                        else if (facility.getOwnerCompany() != null) {
                            toCompanyId = facility.getOwnerCompany().getId();
                        }
                    }
                    
                    if (toCompanyId != null) {
                        transactionRequest.setToCompanyId(toCompanyId);
                        transactionRequest.setIsReturn(true); // 원래 위치로 복귀
                        transactionRequest.setNotes("AS 완료에 따른 시설물 복귀");
                        if (relatedTransactionId != null) {
                            transactionRequest.setRelatedTransactionId(relatedTransactionId);
                        }
                        
                        // 트랜잭션 생성
                        facilityTransactionService.processService(transactionRequest);

                    } else {
                        log.warn("원래 위치 회사 정보를 찾을 수 없어 AS 복귀 트랜잭션을 생성할 수 없습니다. 시설물 ID: {}", facility.getFacilityId());
                    }
                } else {
                    log.warn("관리자의 소속 회사 정보가 없어 AS 복귀 트랜잭션을 생성할 수 없습니다. 관리자 ID: {}", manager.getId());
                }
            }
        } catch (Exception e) {
            log.error("시설물 AS 복귀 트랜잭션 생성 중 오류 발생: {}", e.getMessage(), e);
            // 트랜잭션 생성 실패가 AS 완료 자체를 실패시키지 않도록 예외 처리
        }
        
        ServiceRequestDTO resultDto = ServiceRequestDTO.fromEntity(updatedServiceRequest);
        return enrichDtoWithCompanyInfo(resultDto, facility);
    }
    
    /**
     * AS 완료 처리 (기존 호환성 유지)
     */
    @Transactional
    public ServiceRequestDTO markAsCompleted(Long id) {
        ServiceRequest serviceRequest = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AS 접수를 찾을 수 없습니다: " + id));
        
        serviceRequest.setIsCompleted(true);
        serviceRequest.setCompletionDate(LocalDateTime.now());
        
        // 서비스 요청 상태를 "AS 수리완료"로 변경
        Code serviceStatus = codeRepository.findById("002010_0003") // AS 수리완료 상태 코드
                .orElseThrow(() -> new EntityNotFoundException("서비스 요청 상태 코드를 찾을 수 없습니다: 002010_0003"));
        serviceRequest.setStatus(serviceStatus);
        
        // 시설물 상태 업데이트: 사용 중(002003_0001)으로 복원
        Facility facility = serviceRequest.getFacility();
        Code normalStatus = codeRepository.findById("002003_0001") // 사용 중 상태 코드
                .orElseThrow(() -> new EntityNotFoundException("시설물 상태 코드를 찾을 수 없습니다: 002003_0001"));
        facility.setStatus(normalStatus);
        facilityRepository.save(facility);
        
        ServiceRequest updatedServiceRequest = serviceRequestRepository.save(serviceRequest);
        
        ServiceRequestDTO resultDto = ServiceRequestDTO.fromEntity(updatedServiceRequest);
        return enrichDtoWithCompanyInfo(resultDto, facility);
    }
    
    /**
     * AS 검색
     */
    @Transactional(readOnly = true)
    public Page<ServiceRequestDTO> searchServiceRequests(Specification<ServiceRequest> spec, Pageable pageable) {
        return serviceRequestRepository.findAll(spec, pageable)
                .map(ServiceRequestDTO::fromEntity);
    }
    
    /**
     * 접수번호 생성
     * 형식: SR + 날짜(YYMMDD) + 5자리 랜덤숫자
     */
    private String generateRequestNumber() {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String randomSuffix = String.format("%05d", (int) (Math.random() * 100000));
        return "SR" + datePrefix + randomSuffix;
    }
    
    /**
     * 회사 이름 조회
     */
    private String getCompanyName(Long companyId) {
        if (companyId == null) {
            return null;
        }
        
        Optional<Company> companyOpt = companyRepository.findById(companyId);
        return companyOpt.map(Company::getStoreName).orElse("매장 " + companyId);
    }
    
    // 유틸리티 메서드로 facility에서 companyId를 얻는 메서드 추가
    private Long getCompanyIdFromFacility(Facility facility) {
        if (facility == null) return null;
        
        // 위치 회사 정보가 있으면 우선 사용 (AS는 주로 위치 회사 관련이므로)
        if (facility.getLocationCompany() != null) {
            return facility.getLocationCompany().getId();
        }
        // 없으면 소유 회사 정보 사용
        else if (facility.getOwnerCompany() != null) {
            return facility.getOwnerCompany().getId();
        }
        
        return null;
    }
} 