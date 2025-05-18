package com.inspection.facility.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inspection.as.entity.ServiceRequest;
import com.inspection.as.repository.ServiceRequestRepository;
import com.inspection.entity.Code;
import com.inspection.entity.Company;
import com.inspection.entity.User;
import com.inspection.facility.dto.FacilityTransactionDTO;
import com.inspection.facility.dto.FacilityTransactionImageDTO;
import com.inspection.facility.dto.FacilityTransactionRequest;
import com.inspection.facility.dto.FacilityTransactionWithImagesDTO;
import com.inspection.facility.dto.TransactionUpdateDTO;
import com.inspection.facility.entity.Facility;
import com.inspection.facility.entity.FacilityTransaction;
import com.inspection.facility.repository.FacilityRepository;
import com.inspection.facility.repository.FacilityTransactionRepository;
import com.inspection.finance.service.VoucherService;
import com.inspection.repository.CodeRepository;
import com.inspection.repository.CompanyRepository;
import com.inspection.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor
public class FacilityTransactionService {

    private final FacilityRepository facilityRepository;
    private final FacilityTransactionRepository transactionRepository;
    private final CodeRepository codeRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final VoucherService voucherService;
    private final FacilityTransactionImageService transactionImageService;
    
    // 트랜잭션 유형 코드 상수 추가
    public static final String TRANSACTION_TYPE_INBOUND = "002011_0001";     // 입고
    public static final String TRANSACTION_TYPE_OUTBOUND = "002011_0002";    // 출고
    public static final String TRANSACTION_TYPE_MOVE = "002011_0003";        // 이동
    public static final String TRANSACTION_TYPE_RENTAL = "002011_0004";      // 임대
    public static final String TRANSACTION_TYPE_RETURN = "002011_0005";      // 반납
    public static final String TRANSACTION_TYPE_SERVICE = "002011_0006";     // AS
    public static final String TRANSACTION_TYPE_DISPOSE = "002011_0007";     // 폐기
    public static final String TRANSACTION_TYPE_LOST = "002011_0008";        // 분실
    public static final String TRANSACTION_TYPE_MISC = "002011_0009";        // 기타
    
    // 시설물 상태 코드 상수 추가
    public static final String STATUS_NORMAL = "002003_0001";        // 사용중
    public static final String STATUS_IN_SERVICE = "002003_0002";    // 수리중
    public static final String STATUS_DISCARDED = "002003_0003";     // 폐기
    public static final String STATUS_RENTAL = "002003_0004";        // 임대중
    public static final String STATUS_LOST = "002003_0008";          // 분실
    public static final String STATUS_MISC = "002003_0009";          // 기타
    public static final String STATUS_INBOUND_CANCELLED = "002003_0007"; // 입고취소

    /**
     * 모든 트랜잭션 조회
     */
    @Transactional(readOnly = true)
    public List<FacilityTransactionDTO> getAllTransactions(String sort) {
        List<FacilityTransaction> transactions;
        
        if ("asc".equalsIgnoreCase(sort)) {
            transactions = transactionRepository.findAllByOrderByCreatedAtAsc();
        } else {
            transactions = transactionRepository.findAllByOrderByCreatedAtDesc();
        }
        
        return transactions.stream()
                .map(FacilityTransactionDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 페이징된 트랜잭션 조회
     */
    @Transactional(readOnly = true)
    public Page<FacilityTransactionDTO> getTransactionsWithPaging(Pageable pageable) {
        Page<FacilityTransaction> transactionPage = transactionRepository.findAllByOrderByCreatedAtDesc(pageable);
        List<FacilityTransactionDTO> dtoList = transactionPage.getContent().stream()
                .map(FacilityTransactionDTO::fromEntity)
                .collect(Collectors.toList());
        
        return new PageImpl<>(dtoList, pageable, transactionPage.getTotalElements());
    }
    
    /**
     * 페이징된 트랜잭션 조회 (이미지 포함)
     */
    @Transactional(readOnly = true)
    public Page<FacilityTransactionWithImagesDTO> getTransactionsWithImagesWithPaging(Pageable pageable) {
        Page<FacilityTransaction> transactionPage = transactionRepository.findAllByOrderByCreatedAtDesc(pageable);
        
        List<FacilityTransactionWithImagesDTO> dtoList = transactionPage.getContent().stream()
                .map(transaction -> {
                    // 트랜잭션 DTO 변환
                    FacilityTransactionDTO transactionDTO = FacilityTransactionDTO.fromEntity(transaction);
                    
                    // 해당 트랜잭션의 이미지 목록 조회
                    List<FacilityTransactionImageDTO> images = 
                            transactionImageService.getTransactionImages(transaction.getTransactionId());
                    
                    // 트랜잭션과 이미지를 합친 DTO 생성
                    return FacilityTransactionWithImagesDTO.builder()
                            .transaction(transactionDTO)
                            .images(images)
                            .build();
                })
                .collect(Collectors.toList());
        
        return new PageImpl<>(dtoList, pageable, transactionPage.getTotalElements());
    }
    
    /**
     * ID로 트랜잭션 조회
     */
    @Transactional(readOnly = true)
    public FacilityTransactionDTO getTransactionById(Long transactionId) {
        FacilityTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("트랜잭션을 찾을 수 없습니다: " + transactionId));
        
        return FacilityTransactionDTO.fromEntity(transaction);
    }
    
    /**
     * 시설물 ID로 트랜잭션 조회
     */
    @Transactional(readOnly = true)
    public List<FacilityTransactionDTO> getTransactionsByFacilityId(Long facilityId) {
        List<FacilityTransaction> transactions = 
                transactionRepository.findByFacilityFacilityIdOrderByTransactionDateDesc(facilityId);
        
        return transactions.stream()
                .map(FacilityTransactionDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 회사 ID로 트랜잭션 조회 (출발지 또는 도착지)
     */
    @Transactional(readOnly = true)
    public List<FacilityTransactionDTO> getTransactionsByCompanyId(Long companyId) {
        List<FacilityTransaction> transactions = 
                transactionRepository.findByFromCompanyIdOrToCompanyIdOrderByTransactionDateDesc(companyId, companyId);
        
        return transactions.stream()
                .map(FacilityTransactionDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 트랜잭션 유형별 조회
     */
    @Transactional(readOnly = true)
    public List<FacilityTransactionDTO> getTransactionsByType(String transactionTypeCode) {
        List<FacilityTransaction> transactions = 
                transactionRepository.findByTransactionTypeCodeIdOrderByTransactionDateDesc(transactionTypeCode);
        
        return transactions.stream()
                .map(FacilityTransactionDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 기간별 트랜잭션 조회
     */
    @Transactional(readOnly = true)
    public List<FacilityTransactionDTO> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<FacilityTransaction> transactions = 
                transactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, endDate);
        
        return transactions.stream()
                .map(FacilityTransactionDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 서비스 요청 ID별 트랜잭션 조회
     */
    @Transactional(readOnly = true)
    public List<FacilityTransaction> getTransactionsByServiceRequestId(Long serviceRequestId) {
        return transactionRepository.findByServiceRequestServiceRequestIdOrderByTransactionDateDesc(serviceRequestId);
    }
    
    /**
     * 서비스 요청 ID별 트랜잭션 DTO 조회
     */
    @Transactional(readOnly = true)
    public List<FacilityTransactionDTO> getTransactionDTOsByServiceRequestId(Long serviceRequestId) {
        List<FacilityTransaction> transactions = 
                transactionRepository.findByServiceRequestServiceRequestIdOrderByTransactionDateDesc(serviceRequestId);
        
        return transactions.stream()
                .map(FacilityTransactionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 기본 트랜잭션 생성 메소드
     */
    @Transactional
    public FacilityTransactionDTO createTransaction(FacilityTransactionRequest request) {
        // 시설물 조회
        Facility facility = facilityRepository.findById(request.getFacilityId())
                .orElseThrow(() -> new EntityNotFoundException("시설물을 찾을 수 없습니다: " + request.getFacilityId()));
        
        // 트랜잭션 유형 코드 조회
        Code transactionType = codeRepository.findById(request.getTransactionTypeCode())
                .orElseThrow(() -> new EntityNotFoundException("트랜잭션 유형 코드를 찾을 수 없습니다: " + request.getTransactionTypeCode()));
        
        // 현재 시설물 상태 저장 (트랜잭션 전 상태)
        Code statusBefore = facility.getStatus();
        
        // 새 상태 코드 조회 (있는 경우)
        Code statusAfter = null;
        if (request.getStatusAfterCode() != null) {
            statusAfter = codeRepository.findById(request.getStatusAfterCode())
                    .orElseThrow(() -> new EntityNotFoundException("상태 코드를 찾을 수 없습니다: " + request.getStatusAfterCode()));
        }
        
        // from회사 조회 (있는 경우)
        Company fromCompany = null;
        if (request.getFromCompanyId() != null) {
            fromCompany = companyRepository.findById(request.getFromCompanyId())
                    .orElseThrow(() -> new EntityNotFoundException("출발 회사를 찾을 수 없습니다: " + request.getFromCompanyId()));
        }
        
        // to회사 조회 (있는 경우)
        Company toCompany = null;
        if (request.getToCompanyId() != null) {
            toCompany = companyRepository.findById(request.getToCompanyId())
                    .orElseThrow(() -> new EntityNotFoundException("도착 회사를 찾을 수 없습니다: " + request.getToCompanyId()));
        }
        
        // 관련 트랜잭션 조회 (있는 경우)
        FacilityTransaction relatedTransaction = null;
        if (request.getRelatedTransactionId() != null) {
            relatedTransaction = transactionRepository.findById(request.getRelatedTransactionId())
                    .orElseThrow(() -> new EntityNotFoundException("관련 트랜잭션을 찾을 수 없습니다: " + request.getRelatedTransactionId()));
        }
        
        // 관련 AS 요청 조회 (있는 경우)
        ServiceRequest serviceRequest = null;
        if (request.getServiceRequestId() != null) {
            serviceRequest = serviceRequestRepository.findById(request.getServiceRequestId())
                    .orElseThrow(() -> new EntityNotFoundException("AS 요청을 찾을 수 없습니다: " + request.getServiceRequestId()));
        }
        
        // 현재 로그인한 사용자 정보를 이용해 수행자 설정
        User performer = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !"anonymousUser".equals(authentication.getPrincipal())) {
            String userId = authentication.getName();
            performer = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        } else {
            throw new IllegalStateException("인증된 사용자만 트랜잭션을 생성할 수 있습니다.");
        }
        
        // 배치 ID 설정 (없는 경우 새로 생성)
        String batchId = request.getBatchId();
        if (batchId == null || batchId.isBlank()) {
            batchId = UUID.randomUUID().toString();
            log.debug("새 배치 ID 생성됨: {}", batchId);
        }
        
        // 트랜잭션 엔티티 생성
        FacilityTransaction transaction = new FacilityTransaction();
        transaction.setFacility(facility);
        transaction.setTransactionType(transactionType);
        transaction.setTransactionDate(request.getTransactionDate() != null ? 
                request.getTransactionDate() : LocalDateTime.now());
        transaction.setFromCompany(fromCompany);
        transaction.setToCompany(toCompany);
        transaction.setNotes(request.getNotes());
        transaction.setStatusBefore(statusBefore);
        transaction.setStatusAfter(statusAfter);
        transaction.setExpectedReturnDate(request.getExpectedReturnDate());
        transaction.setActualReturnDate(request.getActualReturnDate());
        transaction.setRelatedTransaction(relatedTransaction);
        transaction.setServiceRequest(serviceRequest);
        transaction.setPerformedBy(performer);
        transaction.setTransactionRef(request.getTransactionRef());
        transaction.setBatchId(batchId);
        transaction.setIsCancelled(false);
        
        // 트랜잭션 저장
        FacilityTransaction savedTransaction = transactionRepository.save(transaction);
        
        // 시설물 상태 업데이트 (필요한 경우)
        if (statusAfter != null) {
            facility.setStatus(statusAfter);
        }
        
        // 시설물 위치 회사 업데이트 (필요한 경우)
        if (toCompany != null) {
            facility.setLocationCompany(toCompany);
        }
        
        // 변경된 시설물 저장
        facilityRepository.save(facility);
        
        // 트랜잭션 유형에 따른 전표 자동 생성
        try {
            createVoucherForTransaction(savedTransaction);
        } catch (Exception e) {
            log.error("전표 생성 중 오류 발생: {}", e.getMessage(), e);
            // 전표 생성 실패가 트랜잭션 생성 자체를 실패시키지 않도록 예외 처리
        }

        log.info("트랜잭션 생성 완료: ID={}, 유형={}, 배치ID={}", 
                savedTransaction.getTransactionId(), 
                transactionType.getCodeName(),
                batchId);
        
        return FacilityTransactionDTO.fromEntity(savedTransaction);
    }
    
    /**
     * 트랜잭션 유형에 따른 전표 자동 생성
     */
    private void createVoucherForTransaction(FacilityTransaction transaction) {
        String transactionTypeCode = transaction.getTransactionType().getCodeId();
        Facility facility = transaction.getFacility();
        
        switch (transactionTypeCode) {
            case TRANSACTION_TYPE_INBOUND:
                // 입고 트랜잭션 - 자산 취득 전표 생성
                voucherService.createFacilityRegistrationVoucher(facility, transaction);
                break;
                
            case TRANSACTION_TYPE_DISPOSE:
                // 폐기 트랜잭션 - 자산 폐기 전표 생성
                voucherService.createDisposalVoucher(facility, transaction);
                break;
                
            // 필요한 경우 다른 트랜잭션 유형에 대한 전표 생성 로직 추가
                
            default:
                // 기본적으로는 전표를 생성하지 않음
                log.debug("트랜잭션 유형 {}에 대한 자동 전표 생성이 구현되지 않았습니다.", transactionTypeCode);
                break;
        }
    }

    /**
     * 대여 트랜잭션 처리 (소유권 유지, 위치 변경)
     */
    @Transactional
    public FacilityTransactionDTO processRental(com.inspection.facility.dto.RentalTransactionRequest request) {
        // 시설물 조회
        Facility facility = facilityRepository.findById(request.getFacilityId())
                .orElseThrow(() -> new EntityNotFoundException("시설물을 찾을 수 없습니다: " + request.getFacilityId()));
        
        // 이미 대여 중인지 확인
        if (facility.getStatus().getCodeId().equals(STATUS_RENTAL)) {
            throw new IllegalStateException("이미 대여 중인 시설물입니다: " + facility.getSerialNumber());
        }
        
        // 기본 요청 객체 생성
        FacilityTransactionRequest transactionRequest = new FacilityTransactionRequest();
        transactionRequest.setFacilityId(request.getFacilityId());
        transactionRequest.setTransactionTypeCode(TRANSACTION_TYPE_RENTAL);
        transactionRequest.setTransactionDate(LocalDateTime.now());
        transactionRequest.setFromCompanyId(request.getFromCompanyId());
        transactionRequest.setToCompanyId(request.getToCompanyId());
        transactionRequest.setNotes(request.getNotes());
        transactionRequest.setStatusAfterCode(STATUS_RENTAL); // 대여 시에는 항상 대여중 상태로 변경
        transactionRequest.setExpectedReturnDate(request.getExpectedReturnDate());
        transactionRequest.setTransactionRef(request.getTransactionRef());
        
        // 트랜잭션 생성
        FacilityTransactionDTO result = createTransaction(transactionRequest);
        

        
        return result;
    }
    
    /**
     * 반납 트랜잭션 처리 (원래 소유자에게 반납)
     */
    @Transactional
    public FacilityTransactionDTO processReturn(com.inspection.facility.dto.ReturnTransactionRequest request) {
        // 시설물 조회
        Facility facility = facilityRepository.findById(request.getFacilityId())
                .orElseThrow(() -> new EntityNotFoundException("시설물을 찾을 수 없습니다: " + request.getFacilityId()));
        
        // 관련 대여 트랜잭션 조회
        FacilityTransaction rentalTransaction = transactionRepository.findById(request.getRentalTransactionId())
                .orElseThrow(() -> new EntityNotFoundException("대여 트랜잭션을 찾을 수 없습니다: " + request.getRentalTransactionId()));
        
        // 대여 트랜잭션 유형 확인
        if (!rentalTransaction.getTransactionType().getCodeId().equals(TRANSACTION_TYPE_RENTAL)) {
            throw new IllegalArgumentException("관련 트랜잭션이 대여 유형이 아닙니다: " + request.getRentalTransactionId());
        }
        
        // 이미 반납된 대여인지 확인
        if (rentalTransaction.getRelatedTransaction() != null) {
            throw new IllegalStateException("이미 반납된 대여입니다: " + request.getRentalTransactionId());
        }
        
        // 실제 반납일 설정 (제공되지 않은 경우 현재 시간)
        LocalDateTime actualReturnDate = request.getActualReturnDate() != null ? 
                request.getActualReturnDate() : LocalDateTime.now();
        
        // 기본 요청 객체 생성
        FacilityTransactionRequest transactionRequest = new FacilityTransactionRequest();
        transactionRequest.setFacilityId(request.getFacilityId());
        transactionRequest.setTransactionTypeCode(TRANSACTION_TYPE_RETURN);
        transactionRequest.setTransactionDate(actualReturnDate);
        transactionRequest.setFromCompanyId(rentalTransaction.getToCompany().getId()); // 대여 시 도착지가 출발지가 됨
        transactionRequest.setToCompanyId(rentalTransaction.getFromCompany().getId()); // 대여 시 출발지가 도착지가 됨
        transactionRequest.setNotes(request.getNotes());
        transactionRequest.setStatusAfterCode(request.getStatusAfterCode());
        transactionRequest.setActualReturnDate(actualReturnDate);
        transactionRequest.setRelatedTransactionId(request.getRentalTransactionId());
        transactionRequest.setTransactionRef(request.getTransactionRef());
        
        // 트랜잭션 생성
        FacilityTransactionDTO result = createTransaction(transactionRequest);
        
        // 대여 트랜잭션에도 연관 트랜잭션으로 반납 트랜잭션을 설정
        FacilityTransaction returnTransaction = transactionRepository.findById(result.getTransactionId())
                .orElseThrow(() -> new EntityNotFoundException("반납 트랜잭션을 찾을 수 없습니다: " + result.getTransactionId()));
        rentalTransaction.setRelatedTransaction(returnTransaction);
        transactionRepository.save(rentalTransaction);

        
        return result;
    }
    
    /**
     * AS 트랜잭션 처리 (AS 센터로 이동 또는 AS 센터에서 복귀)
     */
    @Transactional
    public FacilityTransactionDTO processService(com.inspection.facility.dto.ServiceTransactionRequest request) {
        // 시설물 조회
        Facility facility = facilityRepository.findById(request.getFacilityId())
                .orElseThrow(() -> new EntityNotFoundException("시설물을 찾을 수 없습니다: " + request.getFacilityId()));
        
        // AS 요청 조회
        ServiceRequest serviceRequest = serviceRequestRepository.findById(request.getServiceRequestId())
                .orElseThrow(() -> new EntityNotFoundException("AS 요청을 찾을 수 없습니다: " + request.getServiceRequestId()));
        
        // AS 복귀인 경우, 관련 AS 트랜잭션 조회
        FacilityTransaction relatedTransaction = null;
        if (request.getIsReturn() && request.getRelatedTransactionId() != null) {
            relatedTransaction = transactionRepository.findById(request.getRelatedTransactionId())
                    .orElseThrow(() -> new EntityNotFoundException("관련 AS 트랜잭션을 찾을 수 없습니다: " + request.getRelatedTransactionId()));
            
            // AS 트랜잭션 유형 확인
            if (!relatedTransaction.getTransactionType().getCodeId().equals(TRANSACTION_TYPE_SERVICE)) {
                throw new IllegalArgumentException("관련 트랜잭션이 AS 유형이 아닙니다: " + request.getRelatedTransactionId());
            }
        }
        
        // 트랜잭션 유형 설정 (AS 또는 AS 복귀)
        String transactionTypeCode = TRANSACTION_TYPE_SERVICE; // AS와 AS 복귀 모두 같은 코드 사용
        
        // 상태 코드 설정 (복귀가 아닌 경우 수리중 상태로 변경)
        String statusAfterCode = request.getIsReturn() ? 
                request.getStatusAfterCode() : STATUS_IN_SERVICE;
        
        // 기본 요청 객체 생성
        FacilityTransactionRequest transactionRequest = new FacilityTransactionRequest();
        transactionRequest.setFacilityId(request.getFacilityId());
        transactionRequest.setTransactionTypeCode(transactionTypeCode);
        transactionRequest.setTransactionDate(LocalDateTime.now());
        transactionRequest.setFromCompanyId(request.getFromCompanyId());
        transactionRequest.setToCompanyId(request.getToCompanyId());
        transactionRequest.setNotes(request.getNotes());
        transactionRequest.setStatusAfterCode(statusAfterCode);
        transactionRequest.setRelatedTransactionId(request.getRelatedTransactionId());
        transactionRequest.setServiceRequestId(request.getServiceRequestId());
        transactionRequest.setTransactionRef(request.getTransactionRef());
        
        // 트랜잭션 생성
        FacilityTransactionDTO result = createTransaction(transactionRequest);
        
        // AS 복귀인 경우, 관련 AS 트랜잭션에 연관 트랜잭션 설정
        if (request.getIsReturn() && relatedTransaction != null) {
            FacilityTransaction returnTransaction = transactionRepository.findById(result.getTransactionId())
                    .orElseThrow(() -> new EntityNotFoundException("AS 복귀 트랜잭션을 찾을 수 없습니다: " + result.getTransactionId()));
            relatedTransaction.setRelatedTransaction(returnTransaction);
            transactionRepository.save(relatedTransaction);
        }
        
        // AS 요청 상태 업데이트 (복귀인 경우 완료로 설정)
        if (request.getIsReturn()) {
            serviceRequest.setIsCompleted(true);
            serviceRequest.setCompletionDate(LocalDateTime.now());
            serviceRequestRepository.save(serviceRequest);
        }
        
        String operationType = request.getIsReturn() ? "복귀" : "이동";

        
        return result;
    }
    
    /**
     * 폐기 트랜잭션 처리 (시설물 생애주기 종료)
     */
    @Transactional
    public FacilityTransactionDTO processDispose(com.inspection.facility.dto.DisposeTransactionRequest request) {
        // 시설물 조회
        Facility facility = facilityRepository.findById(request.getFacilityId())
                .orElseThrow(() -> new EntityNotFoundException("시설물을 찾을 수 없습니다: " + request.getFacilityId()));
        
        // 이미 폐기된 시설물인지 확인 (시설물 상태 및 활성 상태 모두 확인)
        if ((facility.getStatus() != null && facility.getStatus().getCodeId().equals(STATUS_DISCARDED)) || 
            !facility.isActive()) {
            throw new IllegalStateException("이미 폐기된 시설물입니다: " + facility.getSerialNumber());
        }
        
        // 현재 사용자 정보 가져오기
        String userId = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !"anonymousUser".equals(authentication.getPrincipal())) {
            userId = authentication.getName();
        } else {
            throw new IllegalStateException("인증된 사용자만 폐기 처리할 수 있습니다.");
        }
        
        // 기본 요청 객체 생성
        FacilityTransactionRequest transactionRequest = new FacilityTransactionRequest();
        transactionRequest.setFacilityId(request.getFacilityId());
        transactionRequest.setTransactionTypeCode(TRANSACTION_TYPE_DISPOSE);
        transactionRequest.setTransactionDate(LocalDateTime.now());
        transactionRequest.setFromCompanyId(facility.getLocationCompany() != null ? facility.getLocationCompany().getId() : null);
        transactionRequest.setNotes(request.getNotes());
        transactionRequest.setStatusAfterCode(STATUS_DISCARDED); // 폐기 시에는 항상 폐기 상태로 변경
        transactionRequest.setTransactionRef(request.getTransactionRef());
        
        // 마지막 가치 평가일 업데이트
        facility.setLastValuationDate(LocalDateTime.now());
        
        // 새로 추가된 폐기 관련 필드 업데이트
        facility.setActive(false);
        facility.setDiscardReason(request.getNotes()); // 폐기 사유
        facility.setDiscardedAt(LocalDateTime.now()); // 폐기 일시
        facility.setDiscardedBy(userId); // 폐기 처리자
        
        // 트랜잭션 생성 (현재 가치가 유지된 상태로 전표 생성)
        FacilityTransactionDTO result = createTransaction(transactionRequest);
        
        // 트랜잭션 및 전표 생성 후에 현재 가치를 0원으로 설정
        facility.setCurrentValue(BigDecimal.ZERO);
        
        // 변경사항 저장
        facilityRepository.save(facility);
        
        return result;
    }
    
    /**
     * 트랜잭션 삭제 (테스트 환경 또는 잘못 입력된 경우에만 사용)
     */
    @Transactional
    public void deleteTransaction(Long transactionId) {
        if (!transactionRepository.existsById(transactionId)) {
            throw new EntityNotFoundException("트랜잭션을 찾을 수 없습니다: " + transactionId);
        }
        
        transactionRepository.deleteById(transactionId);

    }

    /**
     * 입고 트랜잭션 처리 (새 시설물 또는 외부에서 온 시설물 입고)
     */
    @Transactional
    public FacilityTransactionDTO processInbound(com.inspection.facility.dto.InboundTransactionRequest request) {
        // 시설물 조회
        Facility facility = facilityRepository.findById(request.getFacilityId())
                .orElseThrow(() -> new EntityNotFoundException("시설물을 찾을 수 없습니다: " + request.getFacilityId()));
        
        // 기본 요청 객체 생성
        FacilityTransactionRequest transactionRequest = new FacilityTransactionRequest();
        transactionRequest.setFacilityId(request.getFacilityId());
        transactionRequest.setTransactionTypeCode(TRANSACTION_TYPE_INBOUND);
        transactionRequest.setTransactionDate(LocalDateTime.now());
        transactionRequest.setFromCompanyId(request.getFromCompanyId()); // 출발지는 선택사항
        transactionRequest.setToCompanyId(request.getToCompanyId());     // 도착지(입고 회사)는 필수
        transactionRequest.setNotes(request.getNotes());
        transactionRequest.setStatusAfterCode(request.getStatusAfterCode() != null ? 
                request.getStatusAfterCode() : STATUS_NORMAL);  // 기본 상태는 사용중
        transactionRequest.setTransactionRef(request.getTransactionRef());
        transactionRequest.setBatchId(request.getBatchId());    // 배치 ID 설정
        
        // 트랜잭션 생성
        FacilityTransactionDTO result = createTransaction(transactionRequest);

        
        return result;
    }
    
    /**
     * 출고 트랜잭션 처리 (한 회사에서 다른 회사로 출고)
     */
    @Transactional
    public FacilityTransactionDTO processOutbound(com.inspection.facility.dto.OutboundTransactionRequest request) {
        // 시설물 조회
        Facility facility = facilityRepository.findById(request.getFacilityId())
                .orElseThrow(() -> new EntityNotFoundException("시설물을 찾을 수 없습니다: " + request.getFacilityId()));
        
        // 기본 요청 객체 생성
        FacilityTransactionRequest transactionRequest = new FacilityTransactionRequest();
        transactionRequest.setFacilityId(request.getFacilityId());
        transactionRequest.setTransactionTypeCode(TRANSACTION_TYPE_OUTBOUND);
        transactionRequest.setTransactionDate(LocalDateTime.now());
        transactionRequest.setFromCompanyId(request.getFromCompanyId()); // 출발지 필수
        transactionRequest.setToCompanyId(request.getToCompanyId());     // 도착지 필수
        transactionRequest.setNotes(request.getNotes());
        transactionRequest.setStatusAfterCode(request.getStatusAfterCode() != null ? 
                request.getStatusAfterCode() : STATUS_NORMAL);  // 기본 상태는 사용중
        transactionRequest.setTransactionRef(request.getTransactionRef());
        transactionRequest.setBatchId(request.getBatchId());    // 배치 ID 설정
        
        // 트랜잭션 생성
        FacilityTransactionDTO result = createTransaction(transactionRequest);
        
        // 소유권 이전이 요청된 경우 시설물 소유 회사 업데이트
        if (Boolean.TRUE.equals(request.getTransferOwnership())) {
            Company newOwnerCompany = companyRepository.findById(request.getToCompanyId())
                .orElseThrow(() -> new EntityNotFoundException("소유 회사를 찾을 수 없습니다: " + request.getToCompanyId()));
            
            facility.setOwnerCompany(newOwnerCompany);
            facilityRepository.save(facility);

        }

        
        return result;
    }
    
    /**
     * 이동 트랜잭션 처리 (한 회사에서 다른 회사로 이동)
     */
    @Transactional
    public FacilityTransactionDTO processMove(com.inspection.facility.dto.MoveTransactionRequest request) {
        // 시설물 조회
        Facility facility = facilityRepository.findById(request.getFacilityId())
                .orElseThrow(() -> new EntityNotFoundException("시설물을 찾을 수 없습니다: " + request.getFacilityId()));
        
        // 출발지와 도착지가 같으면 안됨
        if (request.getFromCompanyId().equals(request.getToCompanyId())) {
            throw new IllegalArgumentException("출발지와 도착지가 같을 수 없습니다");
        }
        
        // 기본 요청 객체 생성
        FacilityTransactionRequest transactionRequest = new FacilityTransactionRequest();
        transactionRequest.setFacilityId(request.getFacilityId());
        transactionRequest.setTransactionTypeCode(TRANSACTION_TYPE_MOVE);
        transactionRequest.setTransactionDate(LocalDateTime.now());
        transactionRequest.setFromCompanyId(request.getFromCompanyId()); // 출발지 필수
        transactionRequest.setToCompanyId(request.getToCompanyId());     // 도착지 필수
        transactionRequest.setNotes(request.getNotes());
        transactionRequest.setStatusAfterCode(request.getStatusAfterCode() != null ? 
                request.getStatusAfterCode() : STATUS_NORMAL);  // 기본 상태는 사용중
        transactionRequest.setTransactionRef(request.getTransactionRef());
        transactionRequest.setBatchId(request.getBatchId());    // 배치 ID 설정
        
        // 트랜잭션 생성
        FacilityTransactionDTO result = createTransaction(transactionRequest);
        
        return result;
    }

    /**
     * 트랜잭션 조회
     * @param transactionId 트랜잭션 ID
     * @return 조회된 트랜잭션
     */
    public Optional<FacilityTransaction> getTransaction(Long transactionId) {
        return transactionRepository.findById(transactionId);
    }
    
    /**
     * 트랜잭션 수정
     * @param transactionId 수정할 트랜잭션 ID
     * @param updateDTO 수정 데이터
     * @param userId 수정 요청자 ID
     * @return 수정된 트랜잭션
     */
    @Transactional
    public FacilityTransaction updateTransaction(Long transactionId, TransactionUpdateDTO updateDTO, String userId) {
        // 1. 트랜잭션 조회
        FacilityTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("트랜잭션을 찾을 수 없습니다: " + transactionId));
        
        // 2. 수정자 정보 조회
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다: " + userId));
        
        // 3. 수정 내역 기록 (감사 추적용)
        String updateHistory = String.format(
                "수정 전: 날짜=%s, 메모=%s, 출발회사=%s, 도착회사=%s | 사유: %s",
                transaction.getTransactionDate(),
                transaction.getNotes(),
                transaction.getFromCompany() != null ? transaction.getFromCompany().getId() : "없음",
                transaction.getToCompany() != null ? transaction.getToCompany().getId() : "없음",
                updateDTO.getUpdateReason()
        );
        log.info("트랜잭션 수정 이력: {}", updateHistory);
        
        // 4. 필드 업데이트
        // 트랜잭션 일시 업데이트
        if (updateDTO.getTransactionDate() != null) {
            transaction.setTransactionDate(updateDTO.getTransactionDate());
        }
        
        // 메모 업데이트
        if (updateDTO.getNotes() != null) {
            transaction.setNotes(updateDTO.getNotes());
        }
        
        // 출발 회사 업데이트
        if (updateDTO.getFromCompanyId() != null) {
            Company fromCompany = companyRepository.findById(updateDTO.getFromCompanyId())
                    .orElseThrow(() -> new RuntimeException("출발 회사를 찾을 수 없습니다: " + updateDTO.getFromCompanyId()));
            transaction.setFromCompany(fromCompany);
        }
        
        // 도착 회사 업데이트
        if (updateDTO.getToCompanyId() != null) {
            Company toCompany = companyRepository.findById(updateDTO.getToCompanyId())
                    .orElseThrow(() -> new RuntimeException("도착 회사를 찾을 수 없습니다: " + updateDTO.getToCompanyId()));
            transaction.setToCompany(toCompany);
        }
        
        // 5. 수정 시간 및 기타 정보 업데이트
        transaction.setUpdatedAt(LocalDateTime.now());
        
        // 6. 저장 및 결과 반환
        return transactionRepository.save(transaction);
    }

    /**
     * 배치 ID로 트랜잭션 목록 조회
     * @param batchId 배치 ID
     * @return 트랜잭션 목록
     */
    public List<FacilityTransactionDTO> getTransactionsByBatchId(String batchId) {
        List<FacilityTransaction> transactions = transactionRepository.findByBatchIdAndIsCancelledFalseOrderByTransactionDateDesc(batchId);
        return transactions.stream()
                .map(FacilityTransactionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 특정 날짜의 모든 배치 ID 조회
     * @param date 날짜
     * @return 배치 ID 목록
     */
    public List<String> getDistinctBatchIdsByDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59, 999999999);
        
        return transactionRepository.findDistinctBatchIdsByTransactionDate(startOfDay);
    }

    /**
     * 배치 요약 정보 조회
     * @param batchId 배치 ID
     * @return 배치 요약 정보 (트랜잭션 유형, 개수, 시설물 유형, 회사 등)
     */
    public Map<String, Object> getBatchSummary(String batchId) {
        List<FacilityTransaction> transactions = transactionRepository.findByBatchIdAndIsCancelledFalseOrderByTransactionDateDesc(batchId);
        
        if (transactions.isEmpty()) {
            return Collections.emptyMap();
        }
        
        // 대표 트랜잭션 (첫 번째)
        FacilityTransaction representative = transactions.get(0);
        
        // 트랜잭션 유형 카운트
        Map<String, Long> typeCount = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTransactionType().getCodeName(),
                        Collectors.counting()));
        
        // 시설물 유형 카운트
        Map<String, Long> facilityTypeCount = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getFacility().getFacilityType().getCodeName(),
                        Collectors.counting()));
        
        // 결과 맵 생성
        Map<String, Object> summary = new HashMap<>();
        summary.put("batchId", batchId);
        summary.put("transactionCount", transactions.size());
        summary.put("transactionDate", representative.getTransactionDate());
        summary.put("transactionType", representative.getTransactionType().getCodeName());
        summary.put("typeCount", typeCount);
        summary.put("facilityTypeCount", facilityTypeCount);
        
        // 회사 정보 (출발지/도착지)
        if (representative.getFromCompany() != null) {
            summary.put("fromCompany", Map.of(
                    "id", representative.getFromCompany().getId(),
                    "name", representative.getFromCompany().getStoreName()
            ));
        }
        
        if (representative.getToCompany() != null) {
            summary.put("toCompany", Map.of(
                    "id", representative.getToCompany().getId(),
                    "name", representative.getToCompany().getStoreName()
            ));
        }
        
        return summary;
    }

    /**
     * 특정 날짜의 배치 목록 조회
     * @param date 날짜
     * @return 배치 요약 정보 목록
     */
    public List<Map<String, Object>> getBatchesByDate(LocalDate date) {
        List<String> batchIds = getDistinctBatchIdsByDate(date);
        
        return batchIds.stream()
                .map(this::getBatchSummary)
                .filter(summary -> !summary.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 트랜잭션 취소
     * @param transactionId 취소할 트랜잭션 ID
     * @param reason 취소 사유
     * @param userId 취소 요청자 ID
     * @return 취소된 트랜잭션
     */
    @Transactional
    public FacilityTransactionDTO cancelTransaction(Long transactionId, String reason, String userId) {
        FacilityTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("트랜잭션을 찾을 수 없습니다: " + transactionId));
        
        if (transaction.getIsCancelled()) {
            throw new RuntimeException("이미 취소된 트랜잭션입니다: " + transactionId);
        }
        
        // 취소 전 시설물의 상태와 위치 복구를 위해 시설물 정보 가져오기
        Facility facility = transaction.getFacility();
        String transactionTypeCode = transaction.getTransactionType().getCodeId();
        
        // 취소 상태로 변경
        transaction.setIsCancelled(true);
        transaction.setCancellationReason(reason);
        transaction.setUpdatedAt(LocalDateTime.now());
        
        // 임대중 상태 코드 조회
        Code rentalStatus = codeRepository.findById(STATUS_RENTAL)
            .orElseThrow(() -> new EntityNotFoundException("임대중 상태 코드를 찾을 수 없습니다: " + STATUS_RENTAL));
        
        // 트랜잭션 유형에 따른 시설물 상태/위치 복구
        switch (transactionTypeCode) {
            case TRANSACTION_TYPE_MOVE:
                // 이동 취소: 원래 위치(fromCompany)로 시설물 위치 복구
                log.info("이동 트랜잭션 취소: 시설물 ID={}, 원래 위치(회사 ID)={}로 복구", 
                        facility.getFacilityId(), transaction.getFromCompany().getId());
                facility.setLocationCompany(transaction.getFromCompany());
                break;
                
            case TRANSACTION_TYPE_DISPOSE:
                // 폐기 취소: 이전 상태(statusBefore)로 시설물 상태 복구
                log.info("폐기 트랜잭션 취소: 시설물 ID={}, 원래 상태={}로 복구", 
                        facility.getFacilityId(), transaction.getStatusBefore().getCodeId());
                facility.setStatus(transaction.getStatusBefore());
                break;
                
            case TRANSACTION_TYPE_OUTBOUND:
                // 출고 취소: 원래 위치로 복구
                log.info("출고 트랜잭션 취소: 시설물 ID={}, 원래 위치(회사 ID)={}로 복구", 
                        facility.getFacilityId(), transaction.getFromCompany().getId());
                facility.setLocationCompany(transaction.getFromCompany());
                break;
                
            case TRANSACTION_TYPE_INBOUND:
                // 입고 취소: 시설물 상태를 입고취소로 변경하고 비활성화
                log.info("입고 트랜잭션 취소: 시설물 ID={}", facility.getFacilityId());
                
                // 입고취소 상태 코드 조회
                Code inboundCancelledStatus = codeRepository.findById(STATUS_INBOUND_CANCELLED)
                    .orElseThrow(() -> new EntityNotFoundException("입고취소 상태 코드를 찾을 수 없습니다: " + STATUS_INBOUND_CANCELLED));
                
                // 시설물 상태 변경 및 비활성화
                facility.setActive(false);
                facility.setStatus(inboundCancelledStatus);
                facility.setDiscardReason("입고 트랜잭션 취소");
                facility.setDiscardedAt(LocalDateTime.now());
                facility.setDiscardedBy(userId);
                break;
                
            case TRANSACTION_TYPE_SERVICE:
                // AS 트랜잭션 취소: 원래 위치와 상태로 복구
                log.info("AS 트랜잭션 취소: 시설물 ID={}, 원래 위치(회사 ID)={}로 복구", 
                        facility.getFacilityId(), transaction.getFromCompany().getId());
                facility.setLocationCompany(transaction.getFromCompany());
                facility.setStatus(transaction.getStatusBefore());
                break;
                
            case TRANSACTION_TYPE_RENTAL:
                // 대여 트랜잭션 취소: 원래 위치와 상태로 복구
                log.info("대여 트랜잭션 취소: 시설물 ID={}, 원래 위치(회사 ID)={}로 복구", 
                        facility.getFacilityId(), transaction.getFromCompany().getId());
                facility.setLocationCompany(transaction.getFromCompany());
                
                // 상태를 임대중으로 변경
                facility.setStatus(rentalStatus);
                break;
                
            case TRANSACTION_TYPE_RETURN:
                // 반납 트랜잭션 취소: 대여 상태와 위치로 복구
                log.info("반납 트랜잭션 취소: 시설물 ID={}, 대여 위치(회사 ID)={}로 복구", 
                        facility.getFacilityId(), transaction.getFromCompany().getId());
                facility.setLocationCompany(transaction.getFromCompany());
                
                // 상태를 임대중으로 변경
                facility.setStatus(rentalStatus);
                break;
                
            default:
                log.info("기타 트랜잭션 취소: 시설물 ID={}, 트랜잭션 유형={}", 
                        facility.getFacilityId(), transactionTypeCode);
                break;
        }
        
        // 시설물 변경사항 저장
        facilityRepository.save(facility);
        
        // 트랜잭션 저장 및 결과 반환
        FacilityTransaction savedTransaction = transactionRepository.save(transaction);
        
        // 감사 로그 기록
        log.info("트랜잭션 취소 완료: ID={}, 유형={}, 사유={}, 사용자={}", 
                transactionId, transactionTypeCode, reason, userId);
        
        return FacilityTransactionDTO.fromEntity(savedTransaction);
    }

    /**
     * 배치 내 트랜잭션 일괄 취소
     * @param batchId 배치 ID
     * @param reason 취소 사유
     * @param userId 취소 요청자 ID
     * @return 취소된 트랜잭션 수
     */
    @Transactional
    public int cancelBatchTransactions(String batchId, String reason, String userId) {
        List<FacilityTransaction> transactions = transactionRepository.findByBatchIdAndIsCancelledFalseOrderByTransactionDateDesc(batchId);
        
        if (transactions.isEmpty()) {
            return 0;
        }
        
        // 각 트랜잭션에 대해 개별적으로 취소 처리를 수행
        for (FacilityTransaction transaction : transactions) {
            // 취소 처리
            cancelTransaction(transaction.getTransactionId(), reason, userId);
        }
        
        // 감사 로그 기록
        log.info("배치 트랜잭션 일괄 취소 완료: batchId={}, 건수={}, 사유={}, 사용자={}", 
                batchId, transactions.size(), reason, userId);
        
        return transactions.size();
    }

    /**
     * 분실 트랜잭션 처리 (시설물 상태를 분실로 변경)
     */
    @Transactional
    public FacilityTransactionDTO processLost(com.inspection.facility.dto.LostTransactionRequest request) {
        // 시설물 조회
        Facility facility = facilityRepository.findById(request.getFacilityId())
                .orElseThrow(() -> new EntityNotFoundException("시설물을 찾을 수 없습니다: " + request.getFacilityId()));
        
        // 이미 분실 상태인지 확인
        if ((facility.getStatus() != null && facility.getStatus().getCodeId().equals(STATUS_LOST)) || 
            !facility.isActive()) {
            throw new IllegalStateException("이미 분실 처리된 시설물입니다: " + facility.getSerialNumber());
        }
        
        // 현재 사용자 정보 가져오기
        String userId = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !"anonymousUser".equals(authentication.getPrincipal())) {
            userId = authentication.getName();
        } else {
            throw new IllegalStateException("인증된 사용자만 분실 처리할 수 있습니다.");
        }
        
        // 기본 요청 객체 생성
        FacilityTransactionRequest transactionRequest = new FacilityTransactionRequest();
        transactionRequest.setFacilityId(request.getFacilityId());
        transactionRequest.setTransactionTypeCode(TRANSACTION_TYPE_LOST);
        transactionRequest.setTransactionDate(LocalDateTime.now());
        
        // 현재 위치 회사 설정
        if (request.getLocationCompanyId() != null) {
            transactionRequest.setFromCompanyId(request.getLocationCompanyId());
        } else if (facility.getLocationCompany() != null) {
            transactionRequest.setFromCompanyId(facility.getLocationCompany().getId());
        }
        
        transactionRequest.setNotes(request.getNotes());
        transactionRequest.setStatusAfterCode(STATUS_LOST); // 분실 시에는 분실 상태로 변경
        transactionRequest.setTransactionRef(request.getTransactionRef());
        transactionRequest.setBatchId(request.getBatchId());
        
        // 시설물 비활성화 처리
        facility.setActive(false);
        facility.setDiscardReason("분실: " + request.getNotes());
        facility.setDiscardedAt(LocalDateTime.now()); // 항상 현재 시간으로 폐기 시간 설정
        facility.setDiscardedBy(userId);
        
        // 트랜잭션 생성
        FacilityTransactionDTO result = createTransaction(transactionRequest);
        
        // 변경된 시설물 저장
        facilityRepository.save(facility);
        
        log.info("시설물 분실 처리 완료: ID={}, 시리얼번호={}, 사용자={}", 
                 facility.getFacilityId(), facility.getSerialNumber(), userId);
        
        return result;
    }
    
    /**
     * 기타 트랜잭션 처리 (재고 조정 등의 이유로 시설물 상태 변경)
     */
    @Transactional
    public FacilityTransactionDTO processMisc(com.inspection.facility.dto.MiscTransactionRequest request) {
        // 시설물 조회
        Facility facility = facilityRepository.findById(request.getFacilityId())
                .orElseThrow(() -> new EntityNotFoundException("시설물을 찾을 수 없습니다: " + request.getFacilityId()));
        
        // 이미 비활성화된 시설물인지 확인
        if (!facility.isActive()) {
            throw new IllegalStateException("이미 비활성화된 시설물입니다: " + facility.getSerialNumber());
        }
        
        // 현재 사용자 정보 가져오기
        String userId = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !"anonymousUser".equals(authentication.getPrincipal())) {
            userId = authentication.getName();
        } else {
            throw new IllegalStateException("인증된 사용자만 기타 처리할 수 있습니다.");
        }
        
        // 상세 메모 생성 (사유 + 비고)
        String detailedNotes = "사유: " + request.getReason() + (request.getNotes() != null ? "\n비고: " + request.getNotes() : "");
        
        // 기본 요청 객체 생성
        FacilityTransactionRequest transactionRequest = new FacilityTransactionRequest();
        transactionRequest.setFacilityId(request.getFacilityId());
        transactionRequest.setTransactionTypeCode(TRANSACTION_TYPE_MISC);
        transactionRequest.setTransactionDate(request.getTransactionDate() != null ? request.getTransactionDate() : LocalDateTime.now());
        
        // 현재 위치 회사 설정
        if (request.getLocationCompanyId() != null) {
            transactionRequest.setFromCompanyId(request.getLocationCompanyId());
        } else if (facility.getLocationCompany() != null) {
            transactionRequest.setFromCompanyId(facility.getLocationCompany().getId());
        }
        
        transactionRequest.setNotes(detailedNotes);
        transactionRequest.setStatusAfterCode(STATUS_MISC); // 기타 상태로 변경
        transactionRequest.setTransactionRef(request.getTransactionRef());
        transactionRequest.setBatchId(request.getBatchId());
        
        // 시설물 비활성화 처리
        facility.setActive(false);
        facility.setDiscardReason("기타: " + request.getReason());
        facility.setDiscardedAt(request.getTransactionDate() != null ? request.getTransactionDate() : LocalDateTime.now());
        facility.setDiscardedBy(userId);
        
        // 트랜잭션 생성
        FacilityTransactionDTO result = createTransaction(transactionRequest);
        
        // 변경된 시설물 저장
        facilityRepository.save(facility);
        
        log.info("시설물 기타 처리 완료: ID={}, 시리얼번호={}, 사유={}, 사용자={}", 
                 facility.getFacilityId(), facility.getSerialNumber(), request.getReason(), userId);
        
        return result;
    }
} 