package com.inspection.facility.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.inspection.facility.dto.DisposeTransactionRequest;
import com.inspection.facility.dto.FacilityTransactionDTO;
import com.inspection.facility.dto.FacilityTransactionRequest;
import com.inspection.facility.dto.InboundTransactionRequest;
import com.inspection.facility.dto.MoveTransactionRequest;
import com.inspection.facility.dto.OutboundTransactionRequest;
import com.inspection.facility.dto.RentalTransactionRequest;
import com.inspection.facility.dto.ReturnTransactionRequest;
import com.inspection.facility.dto.ServiceTransactionRequest;
import com.inspection.facility.service.FacilityTransactionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/facility-transactions")
@RequiredArgsConstructor
public class FacilityTransactionController {
    
    private final FacilityTransactionService transactionService;
    
    /**
     * 트랜잭션 전체 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<FacilityTransactionDTO>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }
    
    /**
     * 페이징된 트랜잭션 목록 조회
     */
    @GetMapping("/paging")
    public ResponseEntity<Page<FacilityTransactionDTO>> getTransactionsWithPaging(Pageable pageable) {
        return ResponseEntity.ok(transactionService.getTransactionsWithPaging(pageable));
    }
    
    /**
     * ID로 특정 트랜잭션 조회
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<FacilityTransactionDTO> getTransactionById(@PathVariable Long transactionId) {
        return ResponseEntity.ok(transactionService.getTransactionById(transactionId));
    }
    
    /**
     * 시설물 ID로 트랜잭션 목록 조회
     */
    @GetMapping("/facility/{facilityId}")
    public ResponseEntity<List<FacilityTransactionDTO>> getTransactionsByFacilityId(@PathVariable Long facilityId) {
        return ResponseEntity.ok(transactionService.getTransactionsByFacilityId(facilityId));
    }
    
    /**
     * 회사 ID로 트랜잭션 목록 조회
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<FacilityTransactionDTO>> getTransactionsByCompanyId(@PathVariable Long companyId) {
        return ResponseEntity.ok(transactionService.getTransactionsByCompanyId(companyId));
    }
    
    /**
     * 트랜잭션 유형별 조회
     */
    @GetMapping("/type/{transactionTypeCode}")
    public ResponseEntity<List<FacilityTransactionDTO>> getTransactionsByType(@PathVariable String transactionTypeCode) {
        return ResponseEntity.ok(transactionService.getTransactionsByType(transactionTypeCode));
    }
    
    /**
     * 기간별 트랜잭션 조회
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<FacilityTransactionDTO>> getTransactionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(transactionService.getTransactionsByDateRange(startDate, endDate));
    }
    
    /**
     * 입고 트랜잭션 처리
     */
    @PostMapping("/inbound")
    public ResponseEntity<FacilityTransactionDTO> processInbound(@Valid @RequestBody InboundTransactionRequest request) {
        log.info("입고 트랜잭션 요청: 시설물 ID: {}, 입고 회사: {}", request.getFacilityId(), request.getToCompanyId());
        FacilityTransactionDTO result = transactionService.processInbound(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    /**
     * 출고 트랜잭션 처리
     */
    @PostMapping("/outbound")
    public ResponseEntity<FacilityTransactionDTO> processOutbound(@Valid @RequestBody OutboundTransactionRequest request) {
        log.info("출고 트랜잭션 요청: 시설물 ID: {}, 출발 회사: {}, 도착 회사: {}", 
                request.getFacilityId(), request.getFromCompanyId(), request.getToCompanyId());
        FacilityTransactionDTO result = transactionService.processOutbound(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    /**
     * 이동 트랜잭션 처리
     */
    @PostMapping("/move")
    public ResponseEntity<FacilityTransactionDTO> processMove(@Valid @RequestBody MoveTransactionRequest request) {
        log.info("이동 트랜잭션 요청: 시설물 ID: {}, 출발 회사: {}, 도착 회사: {}", 
                request.getFacilityId(), request.getFromCompanyId(), request.getToCompanyId());
        FacilityTransactionDTO result = transactionService.processMove(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    /**
     * 대여 트랜잭션 처리
     */
    @PostMapping("/rental")
    public ResponseEntity<FacilityTransactionDTO> processRental(@Valid @RequestBody RentalTransactionRequest request) {
        log.info("대여 트랜잭션 요청: 시설물 ID: {}, 대여자: {}, 반납 예정일: {}", 
                request.getFacilityId(), request.getToCompanyId(), request.getExpectedReturnDate());
        FacilityTransactionDTO result = transactionService.processRental(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    /**
     * 반납 트랜잭션 처리
     */
    @PostMapping("/return")
    public ResponseEntity<FacilityTransactionDTO> processReturn(@Valid @RequestBody ReturnTransactionRequest request) {
        log.info("반납 트랜잭션 요청: 시설물 ID: {}, 대여 트랜잭션 ID: {}", 
                request.getFacilityId(), request.getRentalTransactionId());
        FacilityTransactionDTO result = transactionService.processReturn(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    /**
     * AS 트랜잭션 처리
     */
    @PostMapping("/service")
    public ResponseEntity<FacilityTransactionDTO> processService(@Valid @RequestBody ServiceTransactionRequest request) {
        String operationType = request.getIsReturn() ? "복귀" : "이동";
        log.info("AS 트랜잭션({}) 요청: 시설물 ID: {}, AS 요청 ID: {}", 
                operationType, request.getFacilityId(), request.getServiceRequestId());
        FacilityTransactionDTO result = transactionService.processService(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    /**
     * 폐기 트랜잭션 처리
     */
    @PostMapping("/dispose")
    public ResponseEntity<FacilityTransactionDTO> processDispose(@Valid @RequestBody DisposeTransactionRequest request) {
        log.info("폐기 트랜잭션 요청: 시설물 ID: {}, 사유: {}", request.getFacilityId(), request.getNotes());
        FacilityTransactionDTO result = transactionService.processDispose(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    /**
     * 트랜잭션 삭제 (테스트 환경 또는 잘못 입력된 경우에만 사용)
     */
    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long transactionId) {
        log.info("트랜잭션 삭제 요청: ID: {}", transactionId);
        transactionService.deleteTransaction(transactionId);
        return ResponseEntity.noContent().build();
    }
} 