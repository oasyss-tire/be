package com.inspection.facility.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.inspection.as.entity.ServiceRequest;
import com.inspection.as.repository.ServiceRequestRepository;
import com.inspection.entity.Code;
import com.inspection.entity.Company;
import com.inspection.facility.dto.FacilityBatchCreateRequest;
import com.inspection.facility.dto.FacilityCreateRequest;
import com.inspection.facility.dto.FacilityDTO;
import com.inspection.facility.dto.FacilitySearchRequest;
import com.inspection.facility.dto.FacilityUpdateRequest;
import com.inspection.facility.dto.InboundTransactionRequest;
import com.inspection.facility.entity.Facility;
import com.inspection.facility.repository.FacilityRepository;
import com.inspection.repository.CodeRepository;
import com.inspection.repository.CompanyRepository;
import com.inspection.util.EncryptionUtil;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacilityService {

    private final FacilityRepository facilityRepository;
    private final CodeRepository codeRepository;
    private final CompanyRepository companyRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final FacilityTransactionService facilityTransactionService;
    private final FacilityImageService facilityImageService;
    private final EncryptionUtil encryptionUtil;
    
    /**
     * 모든 시설물 조회
     */
    public List<FacilityDTO> getAllFacilities() {
        List<Facility> facilities = facilityRepository.findAll();
        return facilities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 페이징 처리된 시설물 목록 조회
     */
    public Page<FacilityDTO> getFacilitiesWithPaging(Pageable pageable) {
        Page<Facility> facilityPage = facilityRepository.findAll(pageable);
        List<FacilityDTO> dtoList = facilityPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return new PageImpl<>(dtoList, pageable, facilityPage.getTotalElements());
    }
    
    /**
     * ID로 시설물 조회
     */
    @Transactional(readOnly = true)
    public FacilityDTO getFacilityById(Long facilityId) {
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new EntityNotFoundException("시설물을 찾을 수 없습니다: " + facilityId));
        
        return convertToDTO(facility);
    }
    
    /**
     * 특정 매장의 시설물 목록 조회
     */
    @Transactional(readOnly = true)
    public List<FacilityDTO> getFacilitiesByCompanyId(Long companyId) {
        List<Facility> facilities = new ArrayList<>();
        
        // 위치 회사 또는 소유 회사가 일치하는 시설물 조회
        facilities.addAll(facilityRepository.findByLocationCompanyId(companyId));
        facilities.addAll(facilityRepository.findByOwnerCompanyId(companyId));
        
        // 중복 제거
        facilities = facilities.stream()
                .distinct()
                .collect(Collectors.toList());
        
        return facilities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 페이징 처리된 특정 매장의 시설물 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<FacilityDTO> getFacilitiesByCompanyIdWithPaging(Long companyId, Pageable pageable) {
        // 간단한 구현을 위해 search 메서드 재사용
        Page<Facility> facilityPage = facilityRepository.search(null, companyId, pageable);
        List<FacilityDTO> dtoList = facilityPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return new PageImpl<>(dtoList, pageable, facilityPage.getTotalElements());
    }
    
    /**
     * 브랜드별 시설물 목록 조회
     */
    @Transactional(readOnly = true)
    public List<FacilityDTO> getFacilitiesByBrand(String brandCode) {
        Code brand = codeRepository.findById(brandCode)
                .orElseThrow(() -> new EntityNotFoundException("브랜드 코드를 찾을 수 없습니다: " + brandCode));
        
        return facilityRepository.findByBrand(brand).stream()
                .map(FacilityDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 시설물 유형별 시설물 목록 조회
     */
    @Transactional(readOnly = true)
    public List<FacilityDTO> getFacilitiesByType(String typeCode) {
        Code facilityType = codeRepository.findById(typeCode)
                .orElseThrow(() -> new EntityNotFoundException("시설물 유형 코드를 찾을 수 없습니다: " + typeCode));
        
        return facilityRepository.findByFacilityType(facilityType).stream()
                .map(FacilityDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 상태별 시설물 목록 조회
     */
    @Transactional(readOnly = true)
    public List<FacilityDTO> getFacilitiesByStatus(String statusCode) {
        Code status = codeRepository.findById(statusCode)
                .orElseThrow(() -> new EntityNotFoundException("상태 코드를 찾을 수 없습니다: " + statusCode));
        
        return facilityRepository.findByStatus(status).stream()
                .map(FacilityDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 키워드로 시설물 검색
     */
    @Transactional(readOnly = true)
    public List<FacilityDTO> searchFacilitiesByKeyword(String keyword) {
        return facilityRepository.searchByKeyword(keyword).stream()
                .map(FacilityDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 위치 키워드로 시설물 검색
     */
    @Transactional(readOnly = true)
    public List<FacilityDTO> searchFacilitiesByLocation(String locationKeyword) {
        return facilityRepository.findByLocationCompanyAddressContaining(locationKeyword).stream()
                .map(FacilityDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 설치일 기간으로 시설물 검색
     */
    @Transactional(readOnly = true)
    public List<FacilityDTO> searchFacilitiesByInstallationDate(LocalDateTime startDate, LocalDateTime endDate) {
        return facilityRepository.findByInstallationDateBetween(startDate, endDate).stream()
                .map(FacilityDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 보증 만료 예정 시설물 조회
     */
    @Transactional(readOnly = true)
    public List<FacilityDTO> getWarrantyExpiringFacilities(LocalDateTime startDate, LocalDateTime endDate) {
        return facilityRepository.findByWarrantyEndDateBetween(startDate, endDate).stream()
                .map(FacilityDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 보증 만료 예정 시설물 조회 (매장 필터링)
     */
    @Transactional(readOnly = true)
    public List<FacilityDTO> getWarrantyExpiringFacilities(LocalDateTime startDate, LocalDateTime endDate, Long companyId) {
        return facilityRepository.findWarrantyExpiring(startDate, endDate, companyId).stream()
                .map(FacilityDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 통합 검색 - 여러 조건으로 시설물 검색
     * companyId가 제공되면 해당 위치 회사(locationCompany)의 시설물만 반환합니다.
     */
    @Transactional(readOnly = true)
    public Page<FacilityDTO> searchFacilities(FacilitySearchRequest searchRequest, Pageable pageable) {
        Specification<Facility> spec = buildSearchSpecification(searchRequest);
        Page<Facility> facilityPage = facilityRepository.findAll(spec, pageable);
        
        List<FacilityDTO> dtoList = facilityPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return new PageImpl<>(dtoList, pageable, facilityPage.getTotalElements());
    }
    
    /**
     * 간단 키워드 검색
     */
    @Transactional(readOnly = true)
    public Page<FacilityDTO> quickSearch(String keyword, Long companyId, Pageable pageable) {
        Page<Facility> facilityPage = facilityRepository.search(keyword, companyId, pageable);
        
        List<FacilityDTO> dtoList = facilityPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return new PageImpl<>(dtoList, pageable, facilityPage.getTotalElements());
    }
    
    /**
     * 시설물 생성
     */
    @Transactional
    public FacilityDTO createFacility(FacilityCreateRequest request) {
        // 필수 정보 체크
        if (request.getInstallationDate() == null) {
            throw new IllegalArgumentException("설치일은 필수입니다.");
        }
        
        // 관리번호 중복 체크
        if (request.getManagementNumber() != null && !request.getManagementNumber().isBlank()) {
            facilityRepository.findByManagementNumber(request.getManagementNumber())
                .ifPresent(f -> {
                    throw new DataIntegrityViolationException("동일한 관리번호의 시설물이 이미 존재합니다: " + request.getManagementNumber());
                });
        }
        
        Facility facility = new Facility();
        
        // 브랜드 설정
        Code brand = codeRepository.findById(request.getBrandCode())
                .orElseThrow(() -> new EntityNotFoundException("브랜드 코드를 찾을 수 없습니다: " + request.getBrandCode()));
        facility.setBrand(brand);
        
        // 시설물 유형 설정
        Code facilityType = codeRepository.findById(request.getFacilityTypeCode())
                .orElseThrow(() -> new EntityNotFoundException("시설물 유형 코드를 찾을 수 없습니다: " + request.getFacilityTypeCode()));
        facility.setFacilityType(facilityType);
        
        // 설치 유형 설정 (있는 경우)
        if (StringUtils.hasText(request.getInstallationTypeCode())) {
            Code installationType = codeRepository.findById(request.getInstallationTypeCode())
                    .orElseThrow(() -> new EntityNotFoundException("설치 유형 코드를 찾을 수 없습니다: " + request.getInstallationTypeCode()));
            facility.setInstallationType(installationType);
        }
        
        // 상태 설정
        Code status = codeRepository.findById(request.getStatusCode())
                .orElseThrow(() -> new EntityNotFoundException("상태 코드를 찾을 수 없습니다: " + request.getStatusCode()));
        facility.setStatus(status);
        
        // 감가상각 방법 설정 (있는 경우)
        if (StringUtils.hasText(request.getDepreciationMethodCode())) {
            Code depreciationMethod = codeRepository.findById(request.getDepreciationMethodCode())
                    .orElseThrow(() -> new EntityNotFoundException("감가상각 방법 코드를 찾을 수 없습니다: " + request.getDepreciationMethodCode()));
            facility.setDepreciationMethod(depreciationMethod);
        }
        
        // 나머지 필드 설정
        facility.setInstallationDate(request.getInstallationDate());
        facility.setAcquisitionCost(request.getAcquisitionCost());
        facility.setUsefulLifeMonths(request.getUsefulLifeMonths());
        
        // 위치 회사 설정
        if (request.getLocationCompanyId() != null) {
            Company locationCompany = companyRepository.findById(request.getLocationCompanyId())
                .orElseThrow(() -> new EntityNotFoundException("위치 회사를 찾을 수 없습니다: " + request.getLocationCompanyId()));
            facility.setLocationCompany(locationCompany);
        }
        
        // 소유 회사 설정
        if (request.getOwnerCompanyId() != null) {
            Company ownerCompany = companyRepository.findById(request.getOwnerCompanyId())
                .orElseThrow(() -> new EntityNotFoundException("소유 회사를 찾을 수 없습니다: " + request.getOwnerCompanyId()));
            facility.setOwnerCompany(ownerCompany);
        }
        
        // 현재 로그인한 사용자 정보를 이용해 createdBy 설정
        String userId = "SYSTEM"; // 기본값
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !"anonymousUser".equals(authentication.getPrincipal())) {
            userId = authentication.getName(); // JWT에서는 getName()이 userId를 반환

        } else {
            log.warn("인증된 사용자 정보를 찾을 수 없습니다. 기본값 사용: {}", userId);
        }
        facility.setCreatedBy(userId);
        
        // 시리얼 번호 자동 생성
        String serialNumber = generateSerialNumber(request.getFacilityTypeCode(), request.getOwnerCompanyId());
        facility.setSerialNumber(serialNumber);
        
        // 관리번호 설정 (제공된 경우 사용, 없으면 자동 생성)
        if (request.getManagementNumber() != null && !request.getManagementNumber().isBlank()) {
            facility.setManagementNumber(request.getManagementNumber());
        } else {
            // 관리번호 자동 생성
            String managementNumber = generateManagementNumber(request.getFacilityTypeCode());
            facility.setManagementNumber(managementNumber);
        }
        
        // 현재 가치는 취득가액으로 설정
        facility.setCurrentValue(request.getAcquisitionCost());
        
        // 마지막 가치 평가일은 null로 설정 (감가상각이 진행되기 전까지)
        facility.setLastValuationDate(null);
        
        // 보증 만료일 계산: 설치일 + 사용연한(월)
        if (request.getUsefulLifeMonths() != null && request.getUsefulLifeMonths() > 0) {
            LocalDateTime warrantyEndDate = request.getInstallationDate().plusMonths(request.getUsefulLifeMonths());
            facility.setWarrantyEndDate(warrantyEndDate);
        }
        
        Facility savedFacility = facilityRepository.save(facility);
        
        // 입고 트랜잭션 자동 생성
        try {
            if (request.getLocationCompanyId() != null) {
                InboundTransactionRequest transactionRequest = new InboundTransactionRequest();
                transactionRequest.setFacilityId(savedFacility.getFacilityId());
                transactionRequest.setToCompanyId(request.getLocationCompanyId());
                // 출발지(fromCompanyId)는 선택사항이므로 설정하지 않음
                transactionRequest.setStatusAfterCode(request.getStatusCode());
                transactionRequest.setNotes("시설물 최초 등록");
                
                facilityTransactionService.processInbound(transactionRequest);
            } else {
                log.warn("위치 회사 정보가 없어 입고 트랜잭션을 생성하지 않았습니다. 시설물 ID: {}", savedFacility.getFacilityId());
            }
        } catch (Exception e) {
            log.error("시설물 입고 트랜잭션 생성 중 오류 발생: {}", e.getMessage(), e);
            // 트랜잭션 생성 실패가 시설물 생성 자체를 실패시키지는 않도록 예외 처리
        }
        
        // QR 코드 생성 추가
        try {
            facilityImageService.generateAndSaveQrCode(savedFacility.getFacilityId());
            log.info("시설물 ID {}에 대한 QR 코드가 생성되었습니다", savedFacility.getFacilityId());
        } catch (Exception e) {
            log.error("시설물 QR 코드 생성 중 오류 발생: {}", e.getMessage(), e);
            // QR 코드 생성 실패가 시설물 생성 자체를 실패시키지는 않도록 예외 처리
        }
        
        return convertToDTO(savedFacility);
    }
    
    /**
     * 관리번호 자동 생성
     * 형식: [시설물타입약자(2자리)]-[생성일(YYYYMMDD)]-[일련번호(3자리)]
     * 예) 리프트 -> RE-20250424-001
     * 동시성 문제를 고려하여 구현 (synchronized + 트랜잭션 격리 수준 활용)
     */
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.SERIALIZABLE)
    private synchronized String generateManagementNumber(String facilityTypeCode) {
        // 현재 날짜 형식 YYYYMMDD
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // 시설물 타입 코드에 따른 약자 매핑
        String typePrefix = getFacilityTypePrefix(facilityTypeCode);
        
        // 오늘 생성된 해당 타입의 시설물 수를 조회하여 일련번호 생성
        // 락을 획득하여 동시 접근 방지
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        
        // count + 1 값을 가져오기 위해 데이터베이스 쿼리 실행
        int count = facilityRepository.countByFacilityType_CodeIdAndCreatedAtBetween(facilityTypeCode, startOfDay, endOfDay);
        
        // 일련번호 형식: 001, 002, ...
        String sequenceNumber = String.format("%03d", count + 1);
        
        // 관리번호 형식: [시설물타입약자]-[생성일]-[일련번호]
        String managementNumber = String.format("%s-%s-%s", typePrefix, dateStr, sequenceNumber);
        
        // 이미 존재하는지 확인하고, 존재하면 번호 증가
        while (facilityRepository.existsByManagementNumber(managementNumber)) {
            count++;
            sequenceNumber = String.format("%03d", count + 1);
            managementNumber = String.format("%s-%s-%s", typePrefix, dateStr, sequenceNumber);
        }
        
        return managementNumber;
    }
    
    /**
     * 시설물 타입 코드에 따른 약자 반환
     */
    private String getFacilityTypePrefix(String facilityTypeCode) {
        if (facilityTypeCode == null) return "UN"; // Unknown
        
        switch (facilityTypeCode) {
            case "002001_0001": return "RE"; // 리프트 (Ramp/lift)
            case "002001_0002": return "TD"; // 탈부착기 (Tire Detacher)
            case "002001_0003": return "TB"; // 밸런스기 (Tire Balancer)
            case "002001_0004": return "AL"; // 얼라이먼트 (Alignment)
            case "002001_0005": return "TH"; // 타이어 호텔 (Tire Hotel)
            case "002001_0006": return "AM"; // 에어메이트 (Air Mate)
            case "002001_0007": return "CP"; // 콤프레샤 (Compressor)
            case "002001_0008": return "BB"; // 비드부스터 (Bead Booster)
            case "002001_0009": return "CL"; // 체인리프트 (Chain Lift)
            case "002001_0010": return "ES"; // 전기시설 (Electrical System)
            case "002001_0011": return "ET"; // 기타 (Etc)
            default: return "UN"; // Unknown
        }
    }
    
    /**
     * 시리얼 번호 생성
     * 형식: [시설물종류코드(4자리)]-[소유회사점번(3자리)]-[UUID 축약형]-[YYMMDD]
     * 기존에 모델번호 필드가 포함되었으나 해당 필드 제거로 인해 형식 변경
     */
    private String generateSerialNumber(String facilityTypeCode, Long ownerCompanyId) {
        // 현재 날짜 형식 YYMMDD
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        
        // 시설물 종류 코드에서 뒤 4자리 추출 (002001_0001 -> 0001)
        String typeCode = "0000";
        if (facilityTypeCode != null && facilityTypeCode.length() >= 4) {
            int underscoreIndex = facilityTypeCode.lastIndexOf('_');
            if (underscoreIndex != -1 && underscoreIndex + 1 < facilityTypeCode.length()) {
                typeCode = facilityTypeCode.substring(underscoreIndex + 1);
            }
        }
        
        // 소유 회사 점번 (3자리) 가져오기
        String storeNumber = "000";
        if (ownerCompanyId != null) {
            Optional<Company> ownerCompanyOpt = companyRepository.findById(ownerCompanyId);
            if (ownerCompanyOpt.isPresent()) {
                String companyStoreNumber = ownerCompanyOpt.get().getStoreNumber();
                if (companyStoreNumber != null && !companyStoreNumber.isEmpty()) {
                    storeNumber = companyStoreNumber;
                }
            }
        }
        
        // 고유한 식별자 생성 (UUID의 처음 8자리만 사용)
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        
        // 시리얼 번호 형식: [시설물종류코드(4자리)]-[소유회사점번(3자리)]-[UUID 축약형]-[YYMMDD]
        return String.format("%s-%s-%s-%s", typeCode, storeNumber, uniqueId, dateStr);
    }
    
    /**
     * 시설물 수정
     */
    @Transactional
    public FacilityDTO updateFacility(Long facilityId, FacilityUpdateRequest request) {
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new EntityNotFoundException("시설물을 찾을 수 없습니다: " + facilityId));
        
        // 시리얼 번호 중복 체크 (변경된 경우)
        if (request.getSerialNumber() != null && !request.getSerialNumber().equals(facility.getSerialNumber())) {
            facilityRepository.findBySerialNumber(request.getSerialNumber())
                .ifPresent(f -> {
                    throw new DataIntegrityViolationException("동일한 시리얼 번호의 시설물이 이미 존재합니다: " + request.getSerialNumber());
                });
        }
        
        // 브랜드 수정 (제공된 경우)
        updateCodeReference(request.getBrandCode(), facility::setBrand, "브랜드");
        
        // 시설물 유형 수정 (제공된 경우)
        updateCodeReference(request.getFacilityTypeCode(), facility::setFacilityType, "시설물 유형");
        
        // 설치 유형 수정 (제공된 경우)
        if (request.getInstallationTypeCode() != null) {
            if (request.getInstallationTypeCode().isBlank()) {
                facility.setInstallationType(null);
            } else {
                updateCodeReference(request.getInstallationTypeCode(), facility::setInstallationType, "설치 유형");
            }
        }
        
        // 상태 수정 (제공된 경우)
        updateCodeReference(request.getStatusCode(), facility::setStatus, "상태");
        
        // 감가상각 방법 수정 (제공된 경우)
        if (request.getDepreciationMethodCode() != null) {
            if (request.getDepreciationMethodCode().isBlank()) {
                facility.setDepreciationMethod(null);
            } else {
                updateCodeReference(request.getDepreciationMethodCode(), facility::setDepreciationMethod, "감가상각 방법");
            }
        }
        
        // 위치 회사 수정 (제공된 경우)
        if (request.getLocationCompanyId() != null) {
            Company locationCompany = companyRepository.findById(request.getLocationCompanyId())
                .orElseThrow(() -> new EntityNotFoundException("위치 회사를 찾을 수 없습니다: " + request.getLocationCompanyId()));
            facility.setLocationCompany(locationCompany);
        }
        
        // 소유 회사 수정 (제공된 경우)
        if (request.getOwnerCompanyId() != null) {
            Company ownerCompany = companyRepository.findById(request.getOwnerCompanyId())
                .orElseThrow(() -> new EntityNotFoundException("소유 회사를 찾을 수 없습니다: " + request.getOwnerCompanyId()));
            facility.setOwnerCompany(ownerCompany);
        }
        
        // 관리번호 업데이트 (제공된 경우)
        if (request.getManagementNumber() != null) {
            // 관리번호 중복 체크
            facilityRepository.findByManagementNumber(request.getManagementNumber())
                .ifPresent(f -> {
                    // 자기 자신은 제외
                    if (!f.getFacilityId().equals(facilityId)) {
                        throw new DataIntegrityViolationException("동일한 관리번호의 시설물이 이미 존재합니다: " + request.getManagementNumber());
                    }
                });
            facility.setManagementNumber(request.getManagementNumber());
        }
        
        // 나머지 필드 수정 (값이 있는 경우만)

        if (request.getSerialNumber() != null) {
            facility.setSerialNumber(request.getSerialNumber());
        }
        
        if (request.getInstallationDate() != null) {
            facility.setInstallationDate(request.getInstallationDate());
        }
        
        if (request.getAcquisitionCost() != null) {
            facility.setAcquisitionCost(request.getAcquisitionCost());
        }
        
        if (request.getUsefulLifeMonths() != null) {
            facility.setUsefulLifeMonths(request.getUsefulLifeMonths());
            
            // 사용연한이 변경되면 보증 만료일도 재계산
            if (facility.getInstallationDate() != null) {
                LocalDateTime newWarrantyEndDate = facility.getInstallationDate().plusMonths(request.getUsefulLifeMonths());
                facility.setWarrantyEndDate(newWarrantyEndDate);
            }
        }
        
        if (request.getCurrentValue() != null) {
            facility.setCurrentValue(request.getCurrentValue());
            
            // 현재 가치가 변경되면 마지막 가치 평가일도 업데이트
            facility.setLastValuationDate(LocalDateTime.now()); // 가치 변경 시 평가일 업데이트
        }
        
        if (request.getLastValuationDate() != null) {
            facility.setLastValuationDate(request.getLastValuationDate());
        }
        
        if (request.getWarrantyEndDate() != null) {
            facility.setWarrantyEndDate(request.getWarrantyEndDate());
        }
        
        // 현재 로그인한 사용자 정보를 이용해 updatedBy 설정
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !"anonymousUser".equals(authentication.getPrincipal())) {
            String userId = authentication.getName();
            facility.setUpdatedBy(userId);
        }
        
        // 저장 및 변환하여 반환
        Facility updatedFacility = facilityRepository.save(facility);
        
        return convertToDTO(updatedFacility);
    }
    
    /**
     * 시설물 삭제
     */
    @Transactional
    public void deleteFacility(Long facilityId) {
        if (!facilityRepository.existsById(facilityId)) {
            throw new EntityNotFoundException("시설물을 찾을 수 없습니다: " + facilityId);
        }
        
        facilityRepository.deleteById(facilityId);
    }
    
    /**
     * 검색 조건 생성
     */
    private Specification<Facility> buildSearchSpecification(FacilitySearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 키워드 검색 (시리얼번호, 관리번호)
            if (StringUtils.hasText(request.getKeyword())) {
                Predicate serialPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("serialNumber")), 
                    "%" + request.getKeyword().toLowerCase() + "%"
                );
                Predicate managementPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("managementNumber")), 
                    "%" + request.getKeyword().toLowerCase() + "%"
                );
                predicates.add(criteriaBuilder.or(serialPredicate, managementPredicate));
            }
            
            // 관리번호 검색
            if (StringUtils.hasText(request.getManagementNumber())) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("managementNumber")),
                    "%" + request.getManagementNumber().toLowerCase() + "%"
                ));
            }
            
            // 위치 검색
            if (StringUtils.hasText(request.getLocation())) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("locationCompany").get("address")),
                    "%" + request.getLocation().toLowerCase() + "%"
                ));
            }
            
            // 회사 ID 검색 (위치 회사 또는 소유 회사)
            if (request.getCompanyId() != null) {
                Predicate locationCompanyPredicate = criteriaBuilder.equal(
                    root.get("locationCompany").get("id"), request.getCompanyId()
                );
                predicates.add(locationCompanyPredicate);
            }
            
            // 브랜드 코드 검색
            if (StringUtils.hasText(request.getBrandCode())) {
                predicates.add(criteriaBuilder.equal(
                    root.get("brand").get("codeId"), request.getBrandCode()
                ));
            }
            
            // 시설물 유형 검색
            if (StringUtils.hasText(request.getFacilityTypeCode())) {
                predicates.add(criteriaBuilder.equal(
                    root.get("facilityType").get("codeId"), request.getFacilityTypeCode()
                ));
            }
            
            // 상태 코드 검색
            if (StringUtils.hasText(request.getStatusCode())) {
                predicates.add(criteriaBuilder.equal(
                    root.get("status").get("codeId"), request.getStatusCode()
                ));
            }
            
            // 설치 유형 검색
            if (StringUtils.hasText(request.getInstallationTypeCode())) {
                predicates.add(criteriaBuilder.equal(
                    root.get("installationType").get("codeId"), request.getInstallationTypeCode()
                ));
            }
            
            // 설치일 범위 검색
            if (request.getInstallationStartDate() != null && request.getInstallationEndDate() != null) {
                predicates.add(criteriaBuilder.between(
                    root.get("installationDate"),
                    request.getInstallationStartDate(),
                    request.getInstallationEndDate()
                ));
            } else if (request.getInstallationStartDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("installationDate"), request.getInstallationStartDate()
                ));
            } else if (request.getInstallationEndDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("installationDate"), request.getInstallationEndDate()
                ));
            }
            
            // 보증만료일 범위 검색
            if (request.getWarrantyStartDate() != null && request.getWarrantyEndDate() != null) {
                predicates.add(criteriaBuilder.between(
                    root.get("warrantyEndDate"),
                    request.getWarrantyStartDate(),
                    request.getWarrantyEndDate()
                ));
            } else if (request.getWarrantyStartDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("warrantyEndDate"), request.getWarrantyStartDate()
                ));
            } else if (request.getWarrantyEndDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("warrantyEndDate"), request.getWarrantyEndDate()
                ));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * 코드 참조 업데이트 헬퍼 메서드
     */
    private void updateCodeReference(String codeId, java.util.function.Consumer<Code> setter, String codeType) {
        if (StringUtils.hasText(codeId)) {
            Code code = codeRepository.findById(codeId)
                .orElseThrow(() -> new EntityNotFoundException(codeType + " 코드를 찾을 수 없습니다: " + codeId));
            setter.accept(code);
        }
    }
    
    /**
     * Facility 엔티티를 FacilityDTO로 변환 (Company 정보 및 ServiceRequest 정보 포함)
     */
    private FacilityDTO convertToDTO(Facility facility) {
        FacilityDTO dto = FacilityDTO.fromEntity(facility);
        
        // 위치 회사 정보 설정
        if (facility.getLocationCompany() != null) {
            Company locationCompany = facility.getLocationCompany();
            dto.setLocationCompanyId(locationCompany.getId());
            dto.setLocationStoreNumber(locationCompany.getStoreNumber());
            dto.setLocationStoreName(locationCompany.getStoreName());
            
            // 암호화된 주소를 복호화하여 설정
            if (locationCompany.getAddress() != null && !locationCompany.getAddress().isEmpty()) {
                try {
                    String decryptedAddress = encryptionUtil.decrypt(locationCompany.getAddress());
                    dto.setLocationAddress(decryptedAddress);
                } catch (Exception e) {
                    log.error("위치 회사 주소 복호화 중 오류 발생: {}", e.getMessage(), e);
                    // 복호화 실패 시 원본 값 유지
                    dto.setLocationAddress(locationCompany.getAddress());
                }
            } else {
                dto.setLocationAddress(locationCompany.getAddress());
            }
        }
        
        // 소유 회사 정보 설정
        if (facility.getOwnerCompany() != null) {
            Company ownerCompany = facility.getOwnerCompany();
            dto.setOwnerCompanyId(ownerCompany.getId());
            dto.setOwnerStoreNumber(ownerCompany.getStoreNumber());
            dto.setOwnerStoreName(ownerCompany.getStoreName());
        }
        
        // 가장 최근 ServiceRequest 정보 조회 및 설정 (페이징으로 첫 번째 결과만 가져옴)
        Pageable firstResult = PageRequest.of(0, 1);
        Page<ServiceRequest> latestServiceRequestPage = serviceRequestRepository.findLatestByFacilityIdPaged(facility.getFacilityId(), firstResult);
        
        if (latestServiceRequestPage.hasContent()) {
            ServiceRequest serviceRequest = latestServiceRequestPage.getContent().get(0);
            dto.setHasActiveServiceRequest(!serviceRequest.getIsCompleted()); // 완료되지 않았으면 활성화된 상태
            dto.setLatestServiceRequestId(serviceRequest.getServiceRequestId());
            dto.setServiceRequestNumber(serviceRequest.getRequestNumber());
            dto.setServiceStatusCode(serviceRequest.getStatus() != null ? serviceRequest.getStatus().getCodeId() : null);
            dto.setServiceStatusName(serviceRequest.getStatus() != null ? serviceRequest.getStatus().getCodeName() : null);
            dto.setServiceRequestDate(serviceRequest.getRequestDate());
            dto.setExpectedCompletionDate(serviceRequest.getExpectedCompletionDate());
            dto.setCompletionDate(serviceRequest.getCompletionDate());
            dto.setIsReceived(serviceRequest.getIsReceived());
            dto.setIsCompleted(serviceRequest.getIsCompleted());
            
            if (serviceRequest.getManager() != null) {
                dto.setManagerId(serviceRequest.getManager().getId());
                dto.setManagerName(serviceRequest.getManager().getUserName());
            }
        } else {
            dto.setHasActiveServiceRequest(false);
        }
        
        return dto;
    }
    
    /**
     * 시설물 가치 평가일 업데이트
     */
    private void updateValuationDate(Facility facility) {
        facility.setLastValuationDate(LocalDateTime.now()); // 가치 변경 시 평가일 업데이트
    }

    /**
     * 시설물 배치 생성
     */
    @Transactional
    public List<FacilityDTO> createFacilityBatch(FacilityBatchCreateRequest request) {
        // 필수 정보 체크
        if (request.getInstallationDate() == null) {
            throw new IllegalArgumentException("설치일은 필수입니다.");
        }
        
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }
        
        // 관리번호 접두사는 이제 선택적
        
        List<FacilityDTO> createdFacilities = new ArrayList<>();
        
        // 위치 회사 찾기
        Company locationCompany = null;
        if (request.getLocationCompanyId() != null) {
            locationCompany = companyRepository.findById(request.getLocationCompanyId())
                .orElseThrow(() -> new EntityNotFoundException("위치 회사를 찾을 수 없습니다: " + request.getLocationCompanyId()));
        }
        
        // 소유 회사 찾기
        Company ownerCompany = null;
        if (request.getOwnerCompanyId() != null) {
            ownerCompany = companyRepository.findById(request.getOwnerCompanyId())
                .orElseThrow(() -> new EntityNotFoundException("소유 회사를 찾을 수 없습니다: " + request.getOwnerCompanyId()));
        }
        
        // 브랜드 찾기
        Code brand = codeRepository.findById(request.getBrandCode())
                .orElseThrow(() -> new EntityNotFoundException("브랜드 코드를 찾을 수 없습니다: " + request.getBrandCode()));
        
        // 시설물 유형 찾기
        Code facilityType = codeRepository.findById(request.getFacilityTypeCode())
                .orElseThrow(() -> new EntityNotFoundException("시설물 유형 코드를 찾을 수 없습니다: " + request.getFacilityTypeCode()));
        
        // 설치 유형 찾기 (있는 경우)
        Code installationType = null;
        if (StringUtils.hasText(request.getInstallationTypeCode())) {
            installationType = codeRepository.findById(request.getInstallationTypeCode())
                    .orElseThrow(() -> new EntityNotFoundException("설치 유형 코드를 찾을 수 없습니다: " + request.getInstallationTypeCode()));
        }
        
        // 상태 찾기
        Code status = codeRepository.findById(request.getStatusCode())
                .orElseThrow(() -> new EntityNotFoundException("상태 코드를 찾을 수 없습니다: " + request.getStatusCode()));
        
        // 감가상각 방법 찾기 (있는 경우)
        Code depreciationMethod = null;
        if (StringUtils.hasText(request.getDepreciationMethodCode())) {
            depreciationMethod = codeRepository.findById(request.getDepreciationMethodCode())
                    .orElseThrow(() -> new EntityNotFoundException("감가상각 방법 코드를 찾을 수 없습니다: " + request.getDepreciationMethodCode()));
        }
        
        // 현재 로그인한 사용자 정보를 이용해 createdBy 설정
        String userId = "SYSTEM"; // 기본값
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !"anonymousUser".equals(authentication.getPrincipal())) {
            userId = authentication.getName(); // JWT에서는 getName()이 userId를 반환
        } else {
            log.warn("인증된 사용자 정보를 찾을 수 없습니다. 기본값 사용: {}", userId);
        }
        
        // 보증 만료일 계산 (있는 경우)
        LocalDateTime warrantyEndDate = null;
        if (request.getUsefulLifeMonths() != null && request.getUsefulLifeMonths() > 0) {
            warrantyEndDate = request.getInstallationDate().plusMonths(request.getUsefulLifeMonths());
        } else if (request.getWarrantyEndDate() != null) {
            warrantyEndDate = request.getWarrantyEndDate();
        }
        
        // 모든 트랜잭션에 사용할 공통 배치 ID 생성
        String batchId = UUID.randomUUID().toString();
        log.info("시설물 배치 생성 시작: 배치 ID={}, 수량={}", batchId, request.getQuantity());
        
        // 요청된 수량만큼 시설물 생성
        for (int i = 0; i < request.getQuantity(); i++) {
            Facility facility = new Facility();
            
            // 기본 정보 설정
            facility.setBrand(brand);
            facility.setFacilityType(facilityType);
            facility.setInstallationType(installationType);
            facility.setStatus(status);
            facility.setDepreciationMethod(depreciationMethod);
            facility.setInstallationDate(request.getInstallationDate());
            facility.setAcquisitionCost(request.getAcquisitionCost());
            facility.setUsefulLifeMonths(request.getUsefulLifeMonths());
            
            facility.setLocationCompany(locationCompany);
            facility.setOwnerCompany(ownerCompany);
            
            facility.setCreatedBy(userId);
            
            // 시리얼 번호 자동 생성 (각 시설물마다 고유한 시리얼 생성)
            String serialNumber = generateSerialNumber(request.getFacilityTypeCode(), request.getOwnerCompanyId());
            facility.setSerialNumber(serialNumber);
            
            // 관리 번호 설정 (사용자 지정 접두사 또는 자동 생성)
            String managementNumber;
            if (request.getManagementNumberPrefix() != null && !request.getManagementNumberPrefix().isBlank()) {
                // 사용자 지정 접두사 사용 (중복 방지를 위한 로직 추가)
                String prefix = request.getManagementNumberPrefix();
                managementNumber = String.format("%s-%03d", prefix, (i + 1));
                
                // 중복 검사 및 처리
                int suffix = i + 1;
                while (facilityRepository.existsByManagementNumber(managementNumber)) {
                    suffix++;
                    managementNumber = String.format("%s-%03d", prefix, suffix);
                }
            } else {
                // 자동 생성된 형식 사용
                managementNumber = generateManagementNumber(request.getFacilityTypeCode());
            }
            facility.setManagementNumber(managementNumber);
            
            // 현재 가치는 취득가액으로 설정 (특별히 지정된 경우 제외)
            if (request.getCurrentValue() != null) {
                facility.setCurrentValue(request.getCurrentValue());
            } else {
                facility.setCurrentValue(request.getAcquisitionCost());
            }
            
            // 마지막 가치 평가일 설정
            facility.setLastValuationDate(request.getLastValuationDate());
            
            // 보증 만료일 설정
            facility.setWarrantyEndDate(warrantyEndDate);
            
            // 저장 및 결과 목록에 추가
            Facility savedFacility = facilityRepository.save(facility);
            createdFacilities.add(convertToDTO(savedFacility));
            
            
            // 입고 트랜잭션 자동 생성
            try {
                if (request.getLocationCompanyId() != null) {
                    InboundTransactionRequest transactionRequest = new InboundTransactionRequest();
                    transactionRequest.setFacilityId(savedFacility.getFacilityId());
                    transactionRequest.setToCompanyId(request.getLocationCompanyId());
                    transactionRequest.setStatusAfterCode(request.getStatusCode());
                    transactionRequest.setNotes("시설물 배치 생성 - 최초 등록");
                    transactionRequest.setBatchId(batchId); // 공통 배치 ID 설정
                    
                    facilityTransactionService.processInbound(transactionRequest);
                }
            } catch (Exception e) {
                log.error("시설물 입고 트랜잭션 생성 중 오류 발생: {}, 시설물 ID: {}", e.getMessage(), savedFacility.getFacilityId(), e);
                // 트랜잭션 생성 실패가 시설물 생성 자체를 실패시키지는 않도록 예외 처리
            }
            
            // QR 코드 생성 추가
            try {
                facilityImageService.generateAndSaveQrCode(savedFacility.getFacilityId());
                log.info("시설물 ID {}에 대한 QR 코드가 생성되었습니다. 순번: {}/{}", 
                        savedFacility.getFacilityId(), (i + 1), request.getQuantity());
            } catch (Exception e) {
                log.error("시설물 QR 코드 생성 중 오류 발생: {}, 시설물 ID: {}", e.getMessage(), savedFacility.getFacilityId(), e);
                // QR 코드 생성 실패가 시설물 생성 자체를 실패시키지는 않도록 예외 처리
            }
        }

        log.info("시설물 배치 생성 완료. 총 {}개 생성됨, 배치 ID: {}", createdFacilities.size(), batchId);
        return createdFacilities;
    }

    /**
     * 시설물 유형에 해당하는 브랜드 코드 목록 조회
     * @param facilityTypeCode 시설물 유형 코드 (002001_0001 등)
     * @return 해당 시설물 유형에 대한 브랜드 코드 목록
     */
    @Transactional(readOnly = true)
    public List<com.inspection.dto.CodeDTO> getBrandCodesByFacilityType(String facilityTypeCode) {
        // 시설물 코드에서 숫자 부분 추출 (002001_0001 -> 0001)
        String facilityNum = "0001"; // 기본값
        if (facilityTypeCode != null && facilityTypeCode.contains("_")) {
            facilityNum = facilityTypeCode.split("_")[1];
        }
        
        // 브랜드 그룹 ID 조합: 002008 + 숫자부분 (001 ~ 011) -> 002008001 (리프트 품목)
        // facilityNum에서 앞의 '0'을 제거하고 숫자만 추출
        int facilityNumInt = Integer.parseInt(facilityNum);
        String brandGroupId = String.format("002008%03d", facilityNumInt);

        
        // 해당 그룹에 속한 모든 브랜드 코드 조회
        List<Code> brandCodes = codeRepository.findByCodeGroupGroupId(brandGroupId);
        
        // Code 엔티티를 CodeDTO로 변환하여 반환
        return com.inspection.dto.CodeDTO.fromEntities(brandCodes);
    }

    /**
     * 시설물 유형별 총 수량 조회
     * @return 시설물 유형 코드를 키로, 총 수량을 값으로 하는 맵
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getFacilityCountsByType() {
        log.info("시설물 유형별 총 수량 조회");
        
        // 시설물 유형별 수량 조회
        Map<String, Long> countsMap = facilityRepository.countByFacilityTypeCode();
        
        // 결과 맵 생성
        Map<String, Object> result = new LinkedHashMap<>();
        
        // 모든 시설물 유형 코드 목록 조회
        List<Code> facilityTypeCodes = codeRepository.findByCodeGroupGroupId("002001");
        
        // 모든 시설물 유형에 대해 카운트 설정 (0으로 초기화)
        facilityTypeCodes.forEach(code -> {
            String codeId = code.getCodeId();
            String codeName = code.getCodeName();
            Long count = countsMap.getOrDefault(codeId, 0L);
            
            Map<String, Object> typeInfo = new LinkedHashMap<>();
            typeInfo.put("facilityTypeCode", codeId);
            typeInfo.put("facilityTypeName", codeName);
            typeInfo.put("count", count);
            
            result.put(codeId, typeInfo);
        });
        
        // 전체 수량 합계 추가
        long totalCount = countsMap.values().stream().mapToLong(Long::longValue).sum();
        Map<String, Object> totalInfo = new LinkedHashMap<>();
        totalInfo.put("facilityTypeCode", "total");
        totalInfo.put("facilityTypeName", "전체");
        totalInfo.put("count", totalCount);
        result.put("total", totalInfo);
        
        return result;
    }

    /**
     * 사용연한(useful_life_months)만 수정하는 메소드
     */
    @Transactional
    public FacilityDTO updateUsefulLifeMonths(Long facilityId, Integer usefulLifeMonths, String usefulLifeUpdateReason) {
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new EntityNotFoundException("시설물을 찾을 수 없습니다: " + facilityId));
        
        facility.setUsefulLifeMonths(usefulLifeMonths);
        facility.setUsefulLifeUpdateReason(usefulLifeUpdateReason);
        
        // 사용연한이 변경되면 보증 만료일도 재계산
        if (facility.getInstallationDate() != null) {
            LocalDateTime newWarrantyEndDate = facility.getInstallationDate().plusMonths(usefulLifeMonths);
            facility.setWarrantyEndDate(newWarrantyEndDate);
        }
        
        // 현재 로그인한 사용자 정보를 이용해 updatedBy 설정
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !"anonymousUser".equals(authentication.getPrincipal())) {
            String userId = authentication.getName();
            facility.setUpdatedBy(userId);
        }
        
        Facility updatedFacility = facilityRepository.save(facility);
        return convertToDTO(updatedFacility);
    }
} 