package com.inspection.finance.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.inspection.finance.dto.VoucherDTO;
import com.inspection.finance.dto.VoucherItemDTO;
import com.inspection.finance.entity.VoucherItem;
import com.inspection.finance.repository.VoucherItemRepository;
import com.inspection.finance.service.VoucherService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 전표 컨트롤러
 * 전표와 전표 항목 조회를 위한 REST API를 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;
    private final VoucherItemRepository voucherItemRepository;
    
    /**
     * 전표 ID로 전표 조회
     */
    @GetMapping("/{voucherId}")
    public ResponseEntity<VoucherDTO> getVoucherById(@PathVariable Long voucherId) {
        log.info("전표 조회 요청 (ID): {}", voucherId);
        VoucherDTO voucher = voucherService.getVoucherById(voucherId);
        return ResponseEntity.ok(voucher);
    }
    
    /**
     * 전표번호로 전표 조회
     */
    @GetMapping("/number/{voucherNumber}")
    public ResponseEntity<VoucherDTO> getVoucherByNumber(@PathVariable String voucherNumber) {
        log.info("전표 조회 요청 (전표번호): {}", voucherNumber);
        VoucherDTO voucher = voucherService.getVoucherByNumber(voucherNumber);
        return ResponseEntity.ok(voucher);
    }
    
    /**
     * 기간별 전표 목록 조회 (페이징)
     */
    @GetMapping
    public ResponseEntity<Page<VoucherDTO>> getVouchersWithPaging(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable) {
        
        // 기본 기간 설정 (시작일이 없으면 한 달 전, 종료일이 없으면 현재)
        if (startDate == null) {
            startDate = LocalDateTime.now().minusMonths(1);
        }
        
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        
        log.info("전표 목록 조회 요청 (기간): {} ~ {}", startDate, endDate);
        Page<VoucherDTO> vouchers = voucherService.getVouchersWithPaging(startDate, endDate, pageable);
        return ResponseEntity.ok(vouchers);
    }
    
    /**
     * 전체 전표 목록 조회 (페이징 없이, 최신순)
     */
    @GetMapping("/all")
    public ResponseEntity<List<VoucherDTO>> getAllVouchers() {
        log.info("전체 전표 목록 조회 요청");
        List<VoucherDTO> vouchers = voucherService.getAllVouchers();
        return ResponseEntity.ok(vouchers);
    }
    
    /**
     * 시설물 ID로 관련 전표 목록 조회
     */
    @GetMapping("/facility/{facilityId}")
    public ResponseEntity<List<VoucherDTO>> getVouchersByFacilityId(@PathVariable Long facilityId) {
        log.info("전표 목록 조회 요청 (시설물 ID): {}", facilityId);
        List<VoucherDTO> vouchers = voucherService.getVouchersByFacilityId(facilityId);
        return ResponseEntity.ok(vouchers);
    }
    
    /**
     * 시설물 트랜잭션 ID로 전표 조회
     */
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<VoucherDTO> getVoucherByTransactionId(@PathVariable Long transactionId) {
        log.info("전표 조회 요청 (트랜잭션 ID): {}", transactionId);
        VoucherDTO voucher = voucherService.getVoucherByFacilityTransactionId(transactionId);
        return ResponseEntity.ok(voucher);
    }
    
    /**
     * 전표 유형 코드로 전표 목록 조회
     */
    @GetMapping("/type/{typeCode}")
    public ResponseEntity<List<VoucherDTO>> getVouchersByType(@PathVariable String typeCode) {
        log.info("전표 목록 조회 요청 (유형 코드): {}", typeCode);
        List<VoucherDTO> vouchers = voucherService.getVouchersByType(typeCode);
        return ResponseEntity.ok(vouchers);
    }
    
    /**
     * 전표 ID로 전표 항목 목록 조회
     */
    @GetMapping("/{voucherId}/items")
    public ResponseEntity<List<VoucherItemDTO>> getVoucherItems(@PathVariable Long voucherId) {
        log.info("전표 항목 목록 조회 요청 (전표 ID): {}", voucherId);
        
        // 전표가 존재하는지 확인
        voucherService.getVoucherById(voucherId);
        
        // 전표 항목 조회
        List<VoucherItem> items = voucherItemRepository.findByVoucherVoucherIdOrderByLineNumber(voucherId);
        List<VoucherItemDTO> itemDTOs = items.stream()
                .map(VoucherItemDTO::fromEntity)
                .toList();
        
        return ResponseEntity.ok(itemDTOs);
    }
    
    /**
     * 전표 항목 ID로 단일 전표 항목 조회
     */
    @GetMapping("/items/{itemId}")
    public ResponseEntity<VoucherItemDTO> getVoucherItemById(@PathVariable Long itemId) {
        log.info("전표 항목 조회 요청 (항목 ID): {}", itemId);
        
        VoucherItem item = voucherItemRepository.findById(itemId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("전표 항목을 찾을 수 없습니다: " + itemId));
        
        return ResponseEntity.ok(VoucherItemDTO.fromEntity(item));
    }
    
    /**
     * 계정 코드별 전표 항목 조회
     */
    @GetMapping("/accounts/{accountCode}/items")
    public ResponseEntity<List<VoucherItemDTO>> getVoucherItemsByAccountCode(@PathVariable String accountCode) {
        log.info("전표 항목 목록 조회 요청 (계정 코드): {}", accountCode);
        
        List<VoucherItem> items = voucherItemRepository.findByAccountCode(accountCode);
        List<VoucherItemDTO> itemDTOs = items.stream()
                .map(VoucherItemDTO::fromEntity)
                .toList();
        
        return ResponseEntity.ok(itemDTOs);
    }
} 