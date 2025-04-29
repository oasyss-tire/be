package com.inspection.facility.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;

import com.inspection.facility.dto.CancellationRequest;
import com.inspection.facility.dto.DisposeTransactionRequest;
import com.inspection.facility.dto.FacilityTransactionDTO;
import com.inspection.facility.dto.InboundTransactionRequest;
import com.inspection.facility.dto.MoveTransactionRequest;
import com.inspection.facility.dto.OutboundTransactionRequest;
import com.inspection.facility.dto.RentalTransactionRequest;
import com.inspection.facility.dto.ReturnTransactionRequest;
import com.inspection.facility.dto.ServiceTransactionRequest;
import com.inspection.facility.dto.TransactionUpdateDTO;
import com.inspection.facility.entity.FacilityTransaction;
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

        FacilityTransactionDTO result = transactionService.processInbound(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    /**
     * 출고 트랜잭션 처리
     */
    @PostMapping("/outbound")
    public ResponseEntity<FacilityTransactionDTO> processOutbound(@Valid @RequestBody OutboundTransactionRequest request) {

        FacilityTransactionDTO result = transactionService.processOutbound(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    /**
     * 이동 트랜잭션 처리
     */
    @PostMapping("/move")
    public ResponseEntity<FacilityTransactionDTO> processMove(@Valid @RequestBody MoveTransactionRequest request) {

        FacilityTransactionDTO result = transactionService.processMove(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    /**
     * 대여 트랜잭션 처리
     */
    @PostMapping("/rental")
    public ResponseEntity<FacilityTransactionDTO> processRental(@Valid @RequestBody RentalTransactionRequest request) {

        FacilityTransactionDTO result = transactionService.processRental(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    /**
     * 반납 트랜잭션 처리
     */
    @PostMapping("/return")
    public ResponseEntity<FacilityTransactionDTO> processReturn(@Valid @RequestBody ReturnTransactionRequest request) {

        FacilityTransactionDTO result = transactionService.processReturn(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    /**
     * AS 트랜잭션 처리
     */
    @PostMapping("/service")
    public ResponseEntity<FacilityTransactionDTO> processService(@Valid @RequestBody ServiceTransactionRequest request) {
        String operationType = request.getIsReturn() ? "복귀" : "이동";

        FacilityTransactionDTO result = transactionService.processService(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    /**
     * 폐기 트랜잭션 처리
     */
    @PostMapping("/dispose")
    public ResponseEntity<FacilityTransactionDTO> processDispose(@Valid @RequestBody DisposeTransactionRequest request) {
        FacilityTransactionDTO result = transactionService.processDispose(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    /**
     * 트랜잭션 삭제 (테스트 환경 또는 잘못 입력된 경우에만 사용)
     */
    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long transactionId) {

        transactionService.deleteTransaction(transactionId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 트랜잭션 상세 조회 API
     * @param transactionId 트랜잭션 ID
     * @return 트랜잭션 정보
     */
    @GetMapping("/detail/{transactionId}")
    public ResponseEntity<FacilityTransaction> getTransactionDetail(@PathVariable Long transactionId) {
        log.info("트랜잭션 상세 조회 요청: ID={}", transactionId);
        
        Optional<FacilityTransaction> transaction = transactionService.getTransaction(transactionId);
        
        if (transaction.isPresent()) {
            return ResponseEntity.ok(transaction.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 트랜잭션 수정 API
     * @param transactionId 수정할 트랜잭션 ID
     * @param updateDTO 수정 데이터
     * @return 처리 결과
     */
    @PutMapping("/{transactionId}")
    public ResponseEntity<Map<String, Object>> updateTransaction(
            @PathVariable Long transactionId,
            @RequestBody TransactionUpdateDTO updateDTO) {
        
        log.info("트랜잭션 수정 요청: ID={}, 데이터={}", transactionId, updateDTO);
        
        // 현재 로그인한 사용자 ID 가져오기
        String userId = getCurrentUserId();
        
        FacilityTransaction updatedTransaction = transactionService.updateTransaction(
                transactionId, updateDTO, userId);
        
        Map<String, Object> response = Map.of(
                "success", true,
                "message", "트랜잭션이 성공적으로 수정되었습니다.",
                "transactionId", updatedTransaction.getTransactionId()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 트랜잭션 취소 API
     * @param transactionId 취소할 트랜잭션 ID
     * @param request 취소 요청 정보
     * @return 처리 결과
     * /api/facility-transactions/cancel/{transactionId}
     */
    @PostMapping("/cancel/{transactionId}")
    public ResponseEntity<Map<String, Object>> cancelTransaction(
            @PathVariable Long transactionId,
            @Valid @RequestBody CancellationRequest request) {
        
        log.info("트랜잭션 취소 요청: ID={}, 사유={}", transactionId, request.getReason());
        
        // 현재 로그인한 사용자 ID 가져오기
        String userId = getCurrentUserId();
        
        FacilityTransactionDTO cancelledTransaction = transactionService.cancelTransaction(
                transactionId, request.getReason(), userId);
        
        Map<String, Object> response = Map.of(
                "success", true,
                "message", "트랜잭션이 취소되었습니다.",
                "transactionId", cancelledTransaction.getTransactionId(),
                "transactionDate", cancelledTransaction.getTransactionDate()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 배치 내 모든 트랜잭션 일괄 취소 API
     * @param batchId 취소할 배치 ID
     * @param request 취소 요청 정보
     * @return 처리 결과
     * /api/facility-transactions/batch/cancel/{batchId}
     */
    @PostMapping("/batch/cancel/{batchId}")
    public ResponseEntity<Map<String, Object>> cancelBatchTransactions(
            @PathVariable String batchId,
            @Valid @RequestBody CancellationRequest request) {
        
        log.info("배치 트랜잭션 일괄 취소 요청: batchId={}, 사유={}", batchId, request.getReason());
        
        // 현재 로그인한 사용자 ID 가져오기
        String userId = getCurrentUserId();
        
        int cancelledCount = transactionService.cancelBatchTransactions(
                batchId, request.getReason(), userId);
        
        Map<String, Object> response = Map.of(
                "success", true,
                "message", cancelledCount + "개의 트랜잭션이 취소되었습니다.",
                "cancelledCount", cancelledCount,
                "batchId", batchId
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 특정 날짜의 배치 목록 조회 API
     * @param date 조회할 날짜
     * @return 배치 목록
     * /api/facility-transactions/batch/date/{date}
     */
    @GetMapping("/batch/date/{date}")
    public ResponseEntity<List<Map<String, Object>>> getBatchesByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        log.info("날짜별 배치 목록 조회: {}", date);
        List<Map<String, Object>> batches = transactionService.getBatchesByDate(date);
        
        return ResponseEntity.ok(batches);
    }
    
    /**
     * 배치 ID로 트랜잭션 목록 조회 API
     * @param batchId 배치 ID
     * @return 트랜잭션 목록
     * /api/facility-transactions/batch/{batchId}
     */
    @GetMapping("/batch/{batchId}")
    public ResponseEntity<List<FacilityTransactionDTO>> getTransactionsByBatchId(
            @PathVariable String batchId) {
        
        log.info("배치 ID별 트랜잭션 조회: {}", batchId);
        List<FacilityTransactionDTO> transactions = transactionService.getTransactionsByBatchId(batchId);
        
        return ResponseEntity.ok(transactions);
    }
    
    /**
     * 현재 로그인한 사용자 ID를 가져오는 헬퍼 메서드
     * @return 사용자 ID
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "SYSTEM"; // 기본값
    }
} 