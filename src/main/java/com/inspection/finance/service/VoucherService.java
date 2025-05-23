package com.inspection.finance.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inspection.entity.Code;
import com.inspection.facility.entity.Facility;
import com.inspection.facility.entity.FacilityTransaction;
import com.inspection.facility.repository.FacilityRepository;
import com.inspection.facility.repository.FacilityTransactionRepository;
import com.inspection.finance.dto.CreateVoucherRequest;
import com.inspection.finance.dto.CreateVoucherRequest.CreateVoucherItemRequest;
import com.inspection.finance.dto.VoucherDTO;
import com.inspection.finance.entity.Voucher;
import com.inspection.finance.entity.VoucherItem;
import com.inspection.finance.repository.VoucherItemRepository;
import com.inspection.finance.repository.VoucherRepository;
import com.inspection.repository.CodeRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherService {
    
    private final VoucherRepository voucherRepository;
    private final VoucherItemRepository voucherItemRepository;
    private final CodeRepository codeRepository;
    private final FacilityRepository facilityRepository;
    private final FacilityTransactionRepository facilityTransactionRepository;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");
    
    /**
     * 전표 생성
     */
    @Transactional
    public VoucherDTO createVoucher(CreateVoucherRequest request) {
        // 전표 유형 코드 검증
        Code voucherType = codeRepository.findById(request.getVoucherTypeCode())
                .orElseThrow(() -> new EntityNotFoundException("전표 유형 코드를 찾을 수 없습니다: " + request.getVoucherTypeCode()));
        
        // 관련 시설물 검증 (있는 경우)
        Facility facility = null;
        if (request.getFacilityId() != null) {
            facility = facilityRepository.findById(request.getFacilityId())
                    .orElseThrow(() -> new EntityNotFoundException("시설물을 찾을 수 없습니다: " + request.getFacilityId()));
        }
        
        // 관련 시설물 트랜잭션 검증 (있는 경우)
        FacilityTransaction facilityTransaction = null;
        if (request.getFacilityTransactionId() != null) {
            facilityTransaction = facilityTransactionRepository.findById(request.getFacilityTransactionId())
                    .orElseThrow(() -> new EntityNotFoundException("시설물 트랜잭션을 찾을 수 없습니다: " + request.getFacilityTransactionId()));
        }
        
        // 현재 사용자 ID 가져오기
        String userId = getCurrentUserId();
        
        // 전표번호 생성 (전표 유형 + 날짜 + 일련번호)
        String voucherNumber = generateVoucherNumber(request.getVoucherTypeCode());
        
        // 전표 엔티티 생성
        Voucher voucher = new Voucher();
        voucher.setVoucherNumber(voucherNumber);
        voucher.setVoucherType(voucherType);
        voucher.setTransactionDate(request.getTransactionDate());
        voucher.setDescription(request.getDescription());
        voucher.setFacility(facility);
        voucher.setFacilityTransaction(facilityTransaction);
        voucher.setTotalAmount(request.getTotalAmount());
        voucher.setCreatedBy(userId);
        voucher.setAutoGenerated(false);  // 수동 생성
        
        // 전표 항목 생성 및 추가
        for (CreateVoucherItemRequest itemRequest : request.getItems()) {
            VoucherItem item = new VoucherItem();
            item.setVoucher(voucher);
            item.setAccountCode(itemRequest.getAccountCode());
            item.setAccountName(itemRequest.getAccountName());
            item.setDebit(itemRequest.getIsDebit());
            item.setAmount(itemRequest.getAmount());
            item.setDescription(itemRequest.getDescription());
            
            // 라인번호 설정 (제공되지 않은 경우 자동 할당)
            item.setLineNumber(itemRequest.getLineNumber() != null ? 
                    itemRequest.getLineNumber() : voucher.getItems().size() + 1);
            
            voucher.addItem(item);
        }
        
        // 차변/대변 합계 검증
        validateDebitCreditBalance(voucher);
        
        // 전표 저장
        Voucher savedVoucher = voucherRepository.save(voucher);
        log.info("전표가 생성되었습니다. 전표번호: {}, ID: {}", voucherNumber, savedVoucher.getVoucherId());
        
        return VoucherDTO.fromEntity(savedVoucher);
    }
    
    /**
     * 전표번호 생성 (전표 유형 약어 + 날짜(YYMMDD) + 5자리 랜덤숫자)
     */
    private String generateVoucherNumber(String voucherTypeCode) {
        // 전표 유형 약어 설정
        String prefix;
        switch (voucherTypeCode) {
            case "002012_0001":  // 매입전표
                prefix = "PUR";
                break;
            case "002012_0002":  // 감가상각전표
                prefix = "DEP";
                break;
            case "002012_0003":  // 폐기전표
                prefix = "DIS";
                break;
            case "002012_0004":  // 매출전표
                prefix = "SAL";
                break;
            case "002012_0005":  // 이동전표
                prefix = "MOV";
                break;
            default:
                prefix = "ETC";
                break;
        }
        
        // 현재 날짜 포맷 (YYMMDD)
        String dateStr = LocalDateTime.now().format(DATE_FORMATTER);
        
        // 동일 유형 및 날짜의 마지막 번호 확인
        String searchPrefix = prefix + "-" + dateStr;
        List<String> lastNumbers = voucherRepository.findLatestVoucherNumberByPrefix(
                searchPrefix, PageRequest.of(0, 1));
        
        int sequence = 1;
        if (!lastNumbers.isEmpty()) {
            String lastNumber = lastNumbers.get(0);
            String seqPart = lastNumber.substring(lastNumber.lastIndexOf('-') + 1);
            try {
                sequence = Integer.parseInt(seqPart) + 1;
            } catch (NumberFormatException e) {
                // 파싱 실패 시 1부터 시작
                sequence = 1;
            }
        }
        
        // 최종 전표번호 생성
        return String.format("%s-%s-%05d", prefix, dateStr, sequence);
    }
    
    /**
     * 전표의 차변/대변 합계 검증
     */
    private void validateDebitCreditBalance(Voucher voucher) {
        BigDecimal debitSum = BigDecimal.ZERO;
        BigDecimal creditSum = BigDecimal.ZERO;
        
        for (VoucherItem item : voucher.getItems()) {
            if (item.isDebit()) {
                debitSum = debitSum.add(item.getAmount());
            } else {
                creditSum = creditSum.add(item.getAmount());
            }
        }
        
        // 차변과 대변의 합계가 일치하는지 확인
        if (debitSum.compareTo(creditSum) != 0) {
            throw new IllegalArgumentException(
                    "차변 합계와 대변 합계가 일치하지 않습니다. 차변: " + debitSum + ", 대변: " + creditSum);
        }
    }
    
    /**
     * 현재 사용자 ID 가져오기
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return "system";  // 인증 정보가 없는 경우 system으로 설정
    }
    
    /**
     * 전표 조회 (ID)
     */
    @Transactional(readOnly = true)
    public VoucherDTO getVoucherById(Long voucherId) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new EntityNotFoundException("전표를 찾을 수 없습니다: " + voucherId));
        
        return VoucherDTO.fromEntity(voucher);
    }
    
    /**
     * 전표 조회 (전표번호)
     */
    @Transactional(readOnly = true)
    public VoucherDTO getVoucherByNumber(String voucherNumber) {
        Voucher voucher = voucherRepository.findByVoucherNumber(voucherNumber)
                .orElseThrow(() -> new EntityNotFoundException("전표를 찾을 수 없습니다: " + voucherNumber));
        
        return VoucherDTO.fromEntity(voucher);
    }
    
    /**
     * 전표 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<VoucherDTO> getVouchersWithPaging(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Page<Voucher> voucherPage = voucherRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(
                startDate, endDate, pageable);
        
        List<VoucherDTO> voucherDtoList = voucherPage.getContent().stream()
                .map(VoucherDTO::fromEntity)
                .collect(Collectors.toList());
        
        return new PageImpl<>(voucherDtoList, pageable, voucherPage.getTotalElements());
    }
    
    /**
     * 시설물 관련 전표 목록 조회
     */
    @Transactional(readOnly = true)
    public List<VoucherDTO> getVouchersByFacilityId(Long facilityId) {
        List<Voucher> vouchers = voucherRepository.findByFacilityFacilityIdOrderByTransactionDateDesc(facilityId);
        
        return vouchers.stream()
                .map(VoucherDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 시설물 트랜잭션 관련 전표 조회
     */
    @Transactional(readOnly = true)
    public VoucherDTO getVoucherByFacilityTransactionId(Long transactionId) {
        Optional<Voucher> voucher = voucherRepository.findByFacilityTransactionTransactionId(transactionId);
        
        return voucher.map(VoucherDTO::fromEntity).orElse(null);
    }
    
    /**
     * 전표 유형별 전표 목록 조회
     */
    @Transactional(readOnly = true)
    public List<VoucherDTO> getVouchersByType(String voucherTypeCode) {
        List<Voucher> vouchers = voucherRepository.findByVoucherTypeCodeIdOrderByTransactionDateDesc(voucherTypeCode);
        
        return vouchers.stream()
                .map(VoucherDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 전체 전표 목록 조회 (최신순)
     */
    @Transactional(readOnly = true)
    public List<VoucherDTO> getAllVouchers() {
        List<Voucher> vouchers = voucherRepository.findAllByOrderByTransactionDateDesc();
        
        return vouchers.stream()
                .map(VoucherDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 시설물 등록 시 자동 전표 생성
     */
    @Transactional
    public VoucherDTO createFacilityRegistrationVoucher(Facility facility, FacilityTransaction transaction) {
        // 전표 유형 코드 (매입전표)
        Code voucherType = codeRepository.findById("002012_0001")
                .orElseThrow(() -> new EntityNotFoundException("전표 유형 코드를 찾을 수 없습니다: 002012_0001"));
        
        // 전표번호 생성
        String voucherNumber = generateVoucherNumber("002012_0001");
        
        // 전표 생성
        Voucher voucher = new Voucher();
        voucher.setVoucherNumber(voucherNumber);
        voucher.setVoucherType(voucherType);
        voucher.setTransactionDate(transaction.getTransactionDate());
        voucher.setDescription(" 시설물 등록");
        voucher.setFacility(facility);
        voucher.setFacilityTransaction(transaction);
        voucher.setTotalAmount(facility.getAcquisitionCost());
        voucher.setCreatedBy(transaction.getPerformedBy().getUserId());
        voucher.setAutoGenerated(true);  // 자동 생성
        
        // 전표 항목 생성 - 차변 (유형자산 증가)
        VoucherItem debitItem = new VoucherItem();
        debitItem.setVoucher(voucher);
        debitItem.setAccountCode("101");  // 유형자산 계정코드
        debitItem.setAccountName("유형자산");
        debitItem.setDebit(true);  // 차변
        debitItem.setAmount(facility.getAcquisitionCost());
        debitItem.setDescription("시설물 자산 취득");
        debitItem.setLineNumber(1);
        
        // 전표 항목 생성 - 대변 (현금 감소)
        VoucherItem creditItem = new VoucherItem();
        creditItem.setVoucher(voucher);
        creditItem.setAccountCode("103");  // 현금 계정코드
        creditItem.setAccountName("현금");
        creditItem.setDebit(false);  // 대변
        creditItem.setAmount(facility.getAcquisitionCost());
        creditItem.setDescription("시설물 구매 대금");
        creditItem.setLineNumber(2);
        
        // 전표항목 추가
        voucher.addItem(debitItem);
        voucher.addItem(creditItem);
        
        // 전표 저장
        Voucher savedVoucher = voucherRepository.save(voucher);
        log.info("시설물 등록 전표가 자동 생성되었습니다. 전표번호: {}, ID: {}, 시설물: {}", 
                voucherNumber, savedVoucher.getVoucherId(), facility.getSerialNumber());
        
        return VoucherDTO.fromEntity(savedVoucher);
    }
    
    /**
     * 감가상각 전표 자동 생성
     */
    @Transactional
    public VoucherDTO createDepreciationVoucher(Facility facility, BigDecimal depreciationAmount) {
        // 전표 유형 코드 (감가상각 전표)
        Code voucherType = codeRepository.findById("002012_0002")
                .orElseThrow(() -> new EntityNotFoundException("전표 유형 코드를 찾을 수 없습니다: 002012_0002"));
        
        // 전표번호 생성
        String voucherNumber = generateVoucherNumber("002012_0002");
        
        // 전표 생성
        Voucher voucher = new Voucher();
        voucher.setVoucherNumber(voucherNumber);
        voucher.setVoucherType(voucherType);
        voucher.setTransactionDate(LocalDateTime.now());
        voucher.setDescription(" 시설물 감가상각");
        voucher.setFacility(facility);
        voucher.setTotalAmount(depreciationAmount);
        voucher.setCreatedBy(getCurrentUserId());  // 현재 로그인한 사용자 ID 설정
        voucher.setAutoGenerated(true);  // 자동 생성
        
        // 전표 항목 생성 - 차변 (감가상각비 증가)
        VoucherItem debitItem = new VoucherItem();
        debitItem.setVoucher(voucher);
        debitItem.setAccountCode("501");  // 감가상각비 계정코드
        debitItem.setAccountName("감가상각비");
        debitItem.setDebit(true);  // 차변
        debitItem.setAmount(depreciationAmount);
        debitItem.setDescription(" 시설물 감가상각비");
        debitItem.setLineNumber(1);
        
        // 전표 항목 생성 - 대변 (감가상각누계액 증가)
        VoucherItem creditItem = new VoucherItem();
        creditItem.setVoucher(voucher);
        creditItem.setAccountCode("102");  // 감가상각누계액 계정코드
        creditItem.setAccountName("감가상각누계액");
        creditItem.setDebit(false);  // 대변
        creditItem.setAmount(depreciationAmount);
        creditItem.setDescription(" 시설물 감가상각누계액");
        creditItem.setLineNumber(2);
        
        // 전표항목 추가
        voucher.addItem(debitItem);
        voucher.addItem(creditItem);
        
        // 전표 저장
        Voucher savedVoucher = voucherRepository.save(voucher);
        log.info("감가상각 전표가 자동 생성되었습니다. 전표번호: {}, ID: {}, 시설물: {}", 
                voucherNumber, savedVoucher.getVoucherId(), facility.getSerialNumber());
        
        return VoucherDTO.fromEntity(savedVoucher);
    }
    
    /**
     * 시설물 폐기 전표 자동 생성
     */
    @Transactional
    public VoucherDTO createDisposalVoucher(Facility facility, FacilityTransaction transaction) {
        // 전표 유형 코드 (폐기 전표)
        Code voucherType = codeRepository.findById("002012_0003")
                .orElseThrow(() -> new EntityNotFoundException("전표 유형 코드를 찾을 수 없습니다: 002012_0003"));
        
        // 전표번호 생성
        String voucherNumber = generateVoucherNumber("002012_0003");
        
        // 전표 생성
        Voucher voucher = new Voucher();
        voucher.setVoucherNumber(voucherNumber);
        voucher.setVoucherType(voucherType);
        voucher.setTransactionDate(transaction.getTransactionDate());
        voucher.setDescription(" 시설물 폐기");
        voucher.setFacility(facility);
        voucher.setFacilityTransaction(transaction);
        voucher.setTotalAmount(facility.getCurrentValue());  // 현재 가치
        voucher.setCreatedBy(transaction.getPerformedBy().getUserId());
        voucher.setAutoGenerated(true);  // 자동 생성
        
        // 각 항목의 금액은 시설물의 현재 가치를 기준으로 계산
        BigDecimal currentValue = facility.getCurrentValue();
        BigDecimal acquisitionCost = facility.getAcquisitionCost();
        BigDecimal accumulatedDepreciation = acquisitionCost.subtract(currentValue);
        
        // 차변 항목 1: 감가상각누계액 (전체 감가상각액)
        VoucherItem debitItem1 = new VoucherItem();
        debitItem1.setVoucher(voucher);
        debitItem1.setAccountCode("102");  // 감가상각누계액 계정코드
        debitItem1.setAccountName("감가상각누계액");
        debitItem1.setDebit(true);  // 차변
        debitItem1.setAmount(accumulatedDepreciation);
        debitItem1.setDescription("감가상각누계액 제거");
        debitItem1.setLineNumber(1);
        voucher.addItem(debitItem1);
        
        // 차변 항목 2: 자산처분손실 (있는 경우)
        if (currentValue.compareTo(BigDecimal.ZERO) > 0) {
            VoucherItem debitItem2 = new VoucherItem();
            debitItem2.setVoucher(voucher);
            debitItem2.setAccountCode("502");  // 자산처분손실 계정코드
            debitItem2.setAccountName("자산처분손실");
            debitItem2.setDebit(true);  // 차변
            debitItem2.setAmount(currentValue);
            debitItem2.setDescription("시설물 폐기 손실");
            debitItem2.setLineNumber(2);
            voucher.addItem(debitItem2);
        }
        
        // 대변 항목: 유형자산 (취득가액 전체)
        VoucherItem creditItem = new VoucherItem();
        creditItem.setVoucher(voucher);
        creditItem.setAccountCode("101");  // 유형자산 계정코드
        creditItem.setAccountName("유형자산");
        creditItem.setDebit(false);  // 대변
        creditItem.setAmount(acquisitionCost);
        creditItem.setDescription("시설물 자산 폐기");
        creditItem.setLineNumber(voucher.getItems().size() + 1);
        voucher.addItem(creditItem);
        
        // 전표 저장
        Voucher savedVoucher = voucherRepository.save(voucher);
        log.info("시설물 폐기 전표가 자동 생성되었습니다. 전표번호: {}, ID: {}, 시설물: {}", 
                voucherNumber, savedVoucher.getVoucherId(), facility.getSerialNumber());
        
        return VoucherDTO.fromEntity(savedVoucher);
    }
} 