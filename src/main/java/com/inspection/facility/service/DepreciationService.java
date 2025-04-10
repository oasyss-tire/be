package com.inspection.facility.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inspection.entity.Code;
import com.inspection.entity.User;
import com.inspection.facility.dto.CreateDepreciationDTO;
import com.inspection.facility.dto.DepreciationDTO;
import com.inspection.facility.dto.DepreciationSummaryDTO;
import com.inspection.facility.dto.UpdateDepreciationDTO;
import com.inspection.facility.entity.Depreciation;
import com.inspection.facility.entity.Facility;
import com.inspection.facility.repository.DepreciationRepository;
import com.inspection.facility.repository.FacilityRepository;
import com.inspection.finance.service.VoucherService;
import com.inspection.repository.CodeRepository;
import com.inspection.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepreciationService {

    private final DepreciationRepository depreciationRepository;
    private final FacilityRepository facilityRepository;
    private final CodeRepository codeRepository;
    private final UserRepository userRepository;
    private final VoucherService voucherService;
    
    // 감가상각 유형 코드
    private static final String DEPRECIATION_TYPE_DAILY = "002009_0001";   // 일마감
    private static final String DEPRECIATION_TYPE_MONTHLY = "002009_0002"; // 월마감
    
    // 감가상각 방법 코드
    private static final String DEPRECIATION_METHOD_STRAIGHT_LINE = "002006_0001"; // 정액법
    private static final String DEPRECIATION_METHOD_DECLINING_BALANCE = "002006_0002"; // 정률법

    /**
     * 시설물의 감가상각 기록 조회
     */
    @Transactional(readOnly = true)
    public List<DepreciationDTO> getDepreciationsByFacilityId(Long facilityId) {
        List<Depreciation> depreciations = depreciationRepository.findByFacilityFacilityIdOrderByDepreciationDateDesc(facilityId);
        return depreciations.stream()
                .map(DepreciationDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 페이징된 감가상각 기록 조회
     */
    @Transactional(readOnly = true)
    public Page<DepreciationDTO> getDepreciationsWithPaging(Pageable pageable) {
        Page<Depreciation> depreciationPage = depreciationRepository.findAllByOrderByDepreciationDateDesc(pageable);
        List<DepreciationDTO> dtoList = depreciationPage.getContent().stream()
                .map(DepreciationDTO::fromEntity)
                .collect(Collectors.toList());
        
        return new PageImpl<>(dtoList, pageable, depreciationPage.getTotalElements());
    }
    
    /**
     * 특정 기간의 감가상각 기록 조회
     */
    @Transactional(readOnly = true)
    public List<DepreciationDTO> getDepreciationsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Depreciation> depreciations = depreciationRepository.findByDepreciationDateBetweenOrderByDepreciationDateDesc(startDate, endDate);
        return depreciations.stream()
                .map(DepreciationDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 특정 회계연도/월의 감가상각 기록 조회
     */
    @Transactional(readOnly = true)
    public List<DepreciationDTO> getDepreciationsByFiscalYearAndMonth(Integer fiscalYear, Integer fiscalMonth) {
        List<Depreciation> depreciations = depreciationRepository.findByFiscalYearAndFiscalMonthOrderByDepreciationDateDesc(fiscalYear, fiscalMonth);
        return depreciations.stream()
                .map(DepreciationDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 특정 회계연도/월의 감가상각 요약 정보 조회
     */
    @Transactional(readOnly = true)
    public DepreciationSummaryDTO getDepreciationSummary(Integer fiscalYear, Integer fiscalMonth) {
        List<Depreciation> depreciations = depreciationRepository.findByFiscalYearAndFiscalMonthOrderByDepreciationDateDesc(fiscalYear, fiscalMonth);
        
        // 총 감가상각 금액 계산
        BigDecimal totalDepreciationAmount = depreciations.stream()
                .map(Depreciation::getDepreciationAmount)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 감가상각 대상 시설물 수
        int facilitiesCount = (int) depreciations.stream()
                .map(d -> d.getFacility().getFacilityId())
                .distinct()
                .count();
        
        // DepreciationSummaryDTO 생성자 호출 방식 수정
        return DepreciationSummaryDTO.builder()
                .fiscalYear(fiscalYear)
                .fiscalMonth(fiscalMonth)
                .totalDepreciationAmount(totalDepreciationAmount.doubleValue())
                .initialValue(0.0) // 필요시 실제 초기값 계산 로직 추가
                .currentValue(0.0) // 필요시 실제 현재값 계산 로직 추가
                .build();
    }
    
    /**
     * 단일 시설물에 대한 감가상각 처리
     */
    @Transactional
    public DepreciationDTO processDepreciationForFacility(Long facilityId) {
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new EntityNotFoundException("시설물을 찾을 수 없습니다: " + facilityId));
        
        // 현재 시점의 연/월 구하기
        LocalDateTime now = LocalDateTime.now();
        YearMonth currentYearMonth = YearMonth.from(now);
        
        // 이미 현재 월에 감가상각이 처리되었는지 확인
        boolean alreadyProcessed = depreciationRepository.existsByFacilityFacilityIdAndFiscalYearAndFiscalMonth(
                facilityId, currentYearMonth.getYear(), currentYearMonth.getMonthValue());
        
        if (alreadyProcessed) {
            throw new IllegalStateException(
                    String.format("시설물 ID %d는 이미 %d년 %d월의 감가상각이 처리되었습니다", 
                            facilityId, currentYearMonth.getYear(), currentYearMonth.getMonthValue()));
        }
        
        // 감가상각 처리
        return processDepreciation(facility, DEPRECIATION_TYPE_MONTHLY, now);
    }
    
    /**
     * 모든 시설물에 대한 월별 감가상각 처리
     */
    @Transactional
    public List<DepreciationDTO> processMonthlyDepreciationForAllFacilities() {
        LocalDateTime now = LocalDateTime.now();
        YearMonth currentYearMonth = YearMonth.from(now);
        
        // 현재 회계연도/월에 이미 감가상각 처리된 시설물 ID 목록
        List<Long> processedFacilityIds = depreciationRepository.findFacilityIdsByFiscalYearAndFiscalMonth(
                currentYearMonth.getYear(), currentYearMonth.getMonthValue());
        
        // 감가상각이 필요한 시설물 조회 (폐기된 시설물 제외, 이미 처리된 시설물 제외)
        List<Facility> facilitiesToProcess = facilityRepository.findFacilitiesForDepreciation(processedFacilityIds);
        
        log.info("월별 감가상각 처리 시작: 총 {}개 시설물", facilitiesToProcess.size());
        
        // 각 시설물에 대해 감가상각 처리
        return facilitiesToProcess.stream()
                .map(facility -> processDepreciation(facility, DEPRECIATION_TYPE_MONTHLY, now))
                .collect(Collectors.toList());
    }
    
    /**
     * 감가상각 처리 (내부 메소드)
     */
    private DepreciationDTO processDepreciation(Facility facility, String depreciationTypeCode, LocalDateTime depreciationDate) {
        // 현재 시점의 연/월 구하기
        YearMonth currentYearMonth = YearMonth.from(depreciationDate);
        
        // 감가상각 유형 코드 조회
        Code depreciationType = codeRepository.findById(depreciationTypeCode)
                .orElseThrow(() -> new EntityNotFoundException("감가상각 유형 코드를 찾을 수 없습니다: " + depreciationTypeCode));
        
        // 감가상각 방법 코드 확인
        if (facility.getDepreciationMethod() == null) {
            throw new IllegalStateException("시설물에 감가상각 방법이 설정되어 있지 않습니다: " + facility.getFacilityId());
        }
        
        // 내용연수(월) 확인
        if (facility.getUsefulLifeMonths() == null || facility.getUsefulLifeMonths() <= 0) {
            throw new IllegalStateException("시설물에 유효한 내용연수가 설정되어 있지 않습니다: " + facility.getFacilityId());
        }
        
        // 현재 가치와 취득원가 확인
        BigDecimal acquisitionCost = facility.getAcquisitionCost();
        BigDecimal currentValue = facility.getCurrentValue();
        
        if (acquisitionCost == null || currentValue == null) {
            throw new IllegalStateException("시설물에 취득원가 또는 현재 가치가 설정되어 있지 않습니다: " + facility.getFacilityId());
        }
        
        // 현재 가치가 이미 0이면 더 이상 감가상각할 필요 없음
        if (currentValue.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("시설물 ID {}의 현재 가치가 0 이하이므로 감가상각을 수행하지 않습니다", facility.getFacilityId());
            return null;
        }
        
        // 최소 유지 가치 설정 (1000원)
        BigDecimal minimumValue = BigDecimal.valueOf(1000);
        
        // 현재 가치가 이미 최소값(1000원) 이하면 더 이상 감가상각할 필요 없음
        if (currentValue.compareTo(minimumValue) <= 0) {
            log.info("시설물 ID {}의 현재 가치가 최소값(1000원) 이하이므로 감가상각을 수행하지 않습니다", facility.getFacilityId());
            return null;
        }
        
        // 감가상각액 계산
        BigDecimal depreciationAmount = calculateDepreciationAmount(
                facility.getDepreciationMethod().getCodeId(),
                acquisitionCost,
                currentValue,
                facility.getUsefulLifeMonths());
        
        // 감가상각 후 가치가 최소값(1000원) 미만이 되면, 최소값까지만 감가상각
        if (currentValue.subtract(depreciationAmount).compareTo(minimumValue) < 0) {
            depreciationAmount = currentValue.subtract(minimumValue);
            log.info("시설물 ID {}의 감가상각액이 조정되었습니다: {} → {} (최소값 1000원 유지)", 
                    facility.getFacilityId(), depreciationAmount.add(minimumValue).subtract(currentValue), depreciationAmount);
        }
        
        // 새 장부가액 계산
        BigDecimal newCurrentValue = currentValue.subtract(depreciationAmount);
        
        // 감가상각 엔티티 생성
        Depreciation depreciation = new Depreciation();
        depreciation.setFacility(facility);
        depreciation.setDepreciationDate(depreciationDate);
        depreciation.setPreviousValue(currentValue.doubleValue());
        depreciation.setDepreciationAmount(depreciationAmount.doubleValue());
        depreciation.setCurrentValue(newCurrentValue.doubleValue());
        depreciation.setDepreciationType(depreciationType);
        depreciation.setDepreciationMethod(facility.getDepreciationMethod());
        depreciation.setFiscalYear(currentYearMonth.getYear());
        depreciation.setFiscalMonth(currentYearMonth.getMonthValue());
        
        // 감가상각 수행자 설정
        User createdBy = getCurrentUser();
        if (createdBy != null) {
            depreciation.setCreatedBy(createdBy);
        }
        
        // 감가상각 저장
        Depreciation savedDepreciation = depreciationRepository.save(depreciation);
        
        // 시설물 정보 업데이트
        facility.setCurrentValue(newCurrentValue);
        facility.setLastValuationDate(depreciationDate);
        facilityRepository.save(facility);
        
        log.info("시설물 ID {}에 대한 감가상각 처리 완료: 이전 가치 {}, 감가상각액 {}, 현재 가치 {}", 
                facility.getFacilityId(), currentValue, depreciationAmount, newCurrentValue);
        
        // 전표 생성
        try {
            voucherService.createDepreciationVoucher(facility, depreciationAmount);
            log.info("감가상각 전표 생성 완료: 시설물 ID {}, 감가상각액 {}", 
                    facility.getFacilityId(), depreciationAmount);
        } catch (Exception e) {
            log.error("감가상각 전표 생성 중 오류 발생: {}", e.getMessage(), e);
            // 전표 생성 실패가 감가상각 처리 자체를 실패시키지 않도록 예외 처리
        }
        
        return DepreciationDTO.fromEntity(savedDepreciation);
    }
    
    /**
     * 감가상각액 계산
     */
    private BigDecimal calculateDepreciationAmount(String depreciationMethodCode, BigDecimal acquisitionCost, 
            BigDecimal currentValue, Integer usefulLifeMonths) {
        
        // 정액법 (월 단위 균등 상각)
        if (DEPRECIATION_METHOD_STRAIGHT_LINE.equals(depreciationMethodCode)) {
            return acquisitionCost.divide(BigDecimal.valueOf(usefulLifeMonths), 2, RoundingMode.HALF_UP);
        }
        
        // 정률법 (현재 가치에 일정 비율 적용)
        else if (DEPRECIATION_METHOD_DECLINING_BALANCE.equals(depreciationMethodCode)) {
            // 정률법 상각률 계산: 2/n (n=내용연수(년)) = 24/n(월)
            BigDecimal rate = BigDecimal.valueOf(24).divide(BigDecimal.valueOf(usefulLifeMonths), 4, RoundingMode.HALF_UP);
            return currentValue.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        }
        
        // 기본값: 정액법
        return acquisitionCost.divide(BigDecimal.valueOf(usefulLifeMonths), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * 현재 사용자 조회
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !"anonymousUser".equals(authentication.getPrincipal())) {
            String userId = authentication.getName();
            return userRepository.findByUserId(userId).orElse(null);
        }
        return null;
    }

    /**
     * 감가상각 기록 수정
     */
    @Transactional
    public DepreciationDTO updateDepreciation(UpdateDepreciationDTO updateDTO) {
        // 기존 감가상각 기록 조회
        Depreciation depreciation = depreciationRepository.findById(updateDTO.getDepreciationId())
                .orElseThrow(() -> new EntityNotFoundException("감가상각 기록을 찾을 수 없습니다: " + updateDTO.getDepreciationId()));
        
        // 감가상각 유형 코드 조회
        Code depreciationType = codeRepository.findById(updateDTO.getDepreciationTypeCode())
                .orElseThrow(() -> new EntityNotFoundException("감가상각 유형 코드를 찾을 수 없습니다: " + updateDTO.getDepreciationTypeCode()));
        
        // 감가상각 방법 코드 조회
        Code depreciationMethod = codeRepository.findById(updateDTO.getDepreciationMethodCode())
                .orElseThrow(() -> new EntityNotFoundException("감가상각 방법 코드를 찾을 수 없습니다: " + updateDTO.getDepreciationMethodCode()));
        
        // 감가상각 기록 업데이트
        depreciation.setDepreciationDate(updateDTO.getDepreciationDate());
        depreciation.setPreviousValue(updateDTO.getPreviousValue());
        depreciation.setDepreciationAmount(updateDTO.getDepreciationAmount());
        depreciation.setCurrentValue(updateDTO.getCurrentValue());
        depreciation.setDepreciationType(depreciationType);
        depreciation.setDepreciationMethod(depreciationMethod);
        depreciation.setFiscalYear(updateDTO.getFiscalYear());
        depreciation.setFiscalMonth(updateDTO.getFiscalMonth());
        depreciation.setNotes(updateDTO.getNotes());
        
        // 감가상각 기록 저장
        Depreciation savedDepreciation = depreciationRepository.save(depreciation);
        
        // 시설물 정보 업데이트
        Facility facility = depreciation.getFacility();
        facility.setCurrentValue(BigDecimal.valueOf(updateDTO.getCurrentValue()));
        facility.setLastValuationDate(updateDTO.getDepreciationDate());
        facilityRepository.save(facility);
        
        log.info("감가상각 기록 ID {} 수정 완료", updateDTO.getDepreciationId());
        
        return DepreciationDTO.fromEntity(savedDepreciation);
    }

    /**
     * 모든 감가상각 기록 조회 (최신순)
     */
    @Transactional(readOnly = true)
    public List<DepreciationDTO> getAllDepreciations() {
        List<Depreciation> depreciations = depreciationRepository.findAllByOrderByDepreciationDateDesc();
        return depreciations.stream()
                .map(DepreciationDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 특정 ID의 감가상각 기록 조회
     */
    @Transactional(readOnly = true)
    public DepreciationDTO getDepreciationById(Long id) {
        Depreciation depreciation = depreciationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("감가상각 기록을 찾을 수 없습니다: " + id));
        return DepreciationDTO.fromEntity(depreciation);
    }
    
    /**
     * 특정 시설물의 최신 감가상각 이력 조회
     */
    @Transactional(readOnly = true)
    public DepreciationDTO getLatestDepreciationByFacilityId(Long facilityId) {
        Pageable pageable = Pageable.ofSize(1);
        List<Depreciation> latestDepreciation = depreciationRepository.findLatestByFacilityId(facilityId, pageable);
        
        if (latestDepreciation.isEmpty()) {
            throw new EntityNotFoundException("시설물에 대한 감가상각 기록이 없습니다: " + facilityId);
        }
        
        return DepreciationDTO.fromEntity(latestDepreciation.get(0));
    }
    
    /**
     * 특정 회계연도의 감가상각 이력 조회
     */
    @Transactional(readOnly = true)
    public List<DepreciationDTO> getDepreciationsByFiscalYear(Integer fiscalYear) {
        List<Depreciation> depreciations = depreciationRepository.findByFiscalYear(fiscalYear);
        return depreciations.stream()
                .map(DepreciationDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 특정 회계연도의 감가상각 요약 정보 목록 조회
     */
    @Transactional(readOnly = true)
    public List<DepreciationSummaryDTO> getDepreciationSummaryByFiscalYear(Integer fiscalYear) {
        // 해당 회계연도의 모든 회계월에 대한 요약 정보 조회
        List<DepreciationSummaryDTO> summaries = new java.util.ArrayList<>();
        
        for (int month = 1; month <= 12; month++) {
            try {
                DepreciationSummaryDTO summary = getDepreciationSummary(fiscalYear, month);
                if (summary != null) {
                    summaries.add(summary);
                }
            } catch (Exception e) {
                log.warn("{}년 {}월 감가상각 요약 정보 조회 중 오류 발생: {}", fiscalYear, month, e.getMessage());
            }
        }
        
        return summaries;
    }
    
    /**
     * 감가상각 기록 생성
     */
    @Transactional
    public DepreciationDTO createDepreciation(CreateDepreciationDTO dto) {
        // 시설물 조회
        Facility facility = facilityRepository.findById(dto.getFacilityId())
                .orElseThrow(() -> new EntityNotFoundException("시설물을 찾을 수 없습니다: " + dto.getFacilityId()));
        
        // 코드 조회
        Code depreciationType = codeRepository.findById(dto.getDepreciationTypeCode())
                .orElseThrow(() -> new EntityNotFoundException("감가상각 유형 코드를 찾을 수 없습니다: " + dto.getDepreciationTypeCode()));
                
        Code depreciationMethod = codeRepository.findById(dto.getDepreciationMethodCode())
                .orElseThrow(() -> new EntityNotFoundException("감가상각 방법 코드를 찾을 수 없습니다: " + dto.getDepreciationMethodCode()));
        
        // 현재 가치 검증 (최소 1000원 유지)
        BigDecimal minimumValue = BigDecimal.valueOf(1000);
        BigDecimal currentValue = BigDecimal.valueOf(dto.getCurrentValue());
        
        if (currentValue.compareTo(minimumValue) < 0) {
            throw new IllegalArgumentException("현재 가치는 최소 1000원 이상이어야 합니다: " + dto.getCurrentValue());
        }
        
        // 감가상각 엔티티 생성
        Depreciation depreciation = new Depreciation();
        depreciation.setFacility(facility);
        depreciation.setDepreciationDate(dto.getDepreciationDate());
        depreciation.setPreviousValue(dto.getPreviousValue());
        depreciation.setDepreciationAmount(dto.getDepreciationAmount());
        depreciation.setCurrentValue(dto.getCurrentValue());
        depreciation.setDepreciationType(depreciationType);
        depreciation.setDepreciationMethod(depreciationMethod);
        depreciation.setFiscalYear(dto.getFiscalYear());
        depreciation.setFiscalMonth(dto.getFiscalMonth());
        depreciation.setNotes(dto.getNotes());
        
        // 감가상각 수행자 설정
        User createdBy = getCurrentUser();
        if (createdBy != null) {
            depreciation.setCreatedBy(createdBy);
        }
        
        // 감가상각 저장
        Depreciation savedDepreciation = depreciationRepository.save(depreciation);
        
        // 시설물 정보 업데이트
        facility.setCurrentValue(currentValue);
        facility.setLastValuationDate(dto.getDepreciationDate());
        facilityRepository.save(facility);
        
        log.info("감가상각 기록 생성 완료: 시설물 ID {}, 감가상각액 {}", 
                facility.getFacilityId(), dto.getDepreciationAmount());
        
        // 전표 생성
        try {
            voucherService.createDepreciationVoucher(facility, BigDecimal.valueOf(dto.getDepreciationAmount()));
            log.info("감가상각 전표 생성 완료: 시설물 ID {}, 감가상각액 {}", 
                    facility.getFacilityId(), dto.getDepreciationAmount());
        } catch (Exception e) {
            log.error("감가상각 전표 생성 중 오류 발생: {}", e.getMessage(), e);
            // 전표 생성 실패가 감가상각 처리 자체를 실패시키지 않도록 예외 처리
        }
        
        return DepreciationDTO.fromEntity(savedDepreciation);
    }
} 