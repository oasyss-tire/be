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
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.inspection.facility.dto.CancellationRequest;
import com.inspection.facility.dto.DisposeTransactionRequest;
import com.inspection.facility.dto.FacilityTransactionDTO;
import com.inspection.facility.dto.FacilityTransactionWithImagesDTO;
import com.inspection.facility.dto.InboundTransactionRequest;
import com.inspection.facility.dto.LostTransactionRequest;
import com.inspection.facility.dto.MiscTransactionRequest;
import com.inspection.facility.dto.MoveTransactionRequest;
import com.inspection.facility.dto.OutboundTransactionRequest;
import com.inspection.facility.dto.RentalTransactionRequest;
import com.inspection.facility.dto.ReturnTransactionRequest;
import com.inspection.facility.dto.ServiceTransactionRequest;
import com.inspection.facility.dto.TransactionUpdateDTO;
import com.inspection.facility.entity.FacilityTransaction;
import com.inspection.facility.service.FacilityTransactionImageService;
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
    private final FacilityTransactionImageService transactionImageService;
    
    /**
     * 트랜잭션 전체 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<FacilityTransactionDTO>> getAllTransactions(
            @RequestParam(defaultValue = "desc") String sort) {
        return ResponseEntity.ok(transactionService.getAllTransactions(sort));
    }
    
    /**
     * 페이징된 트랜잭션 목록 조회
     */
    @GetMapping("/paging")
    public ResponseEntity<Page<FacilityTransactionDTO>> getTransactionsWithPaging(Pageable pageable) {
        return ResponseEntity.ok(transactionService.getTransactionsWithPaging(pageable));
    }
    
    /**
     * 페이징된 트랜잭션 목록 조회 (이미지 포함)
     */
    @GetMapping("/paging-with-images")
    public ResponseEntity<Page<FacilityTransactionWithImagesDTO>> getTransactionsWithImagesWithPaging(Pageable pageable) {
        return ResponseEntity.ok(transactionService.getTransactionsWithImagesWithPaging(pageable));
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
     * 이동 트랜잭션 처리 (이미지 첨부 지원)
     */
    @PostMapping(value = "/move-with-images")
    public ResponseEntity<FacilityTransactionDTO> processMoveWithImages(
            @RequestParam("request") String requestJson,
            @RequestParam(value = "images", required = false) MultipartFile[] images) {
        
        try {
            // DTO 필드명에 맞게 JSON을 전송해야 함 (fromCompanyId, toCompanyId)
            ObjectMapper objectMapper = new ObjectMapper();
            MoveTransactionRequest request = objectMapper.readValue(requestJson, MoveTransactionRequest.class);
            
            log.info("이미지 첨부 이동 트랜잭션 요청: 시설물 ID={}, 출발 회사 ID={}, 도착 회사 ID={}", 
                    request.getFacilityId(), request.getFromCompanyId(), request.getToCompanyId());
            
            // 기본 이동 트랜잭션 처리
            FacilityTransactionDTO result = transactionService.processMove(request);
            
            // 이미지가 제공된 경우 업로드 처리
            if (images != null && images.length > 0) {
                for (MultipartFile image : images) {
                    if (!image.isEmpty()) {
                        try {
                            transactionImageService.uploadTransactionImage(
                                    result.getTransactionId(), 
                                    image, 
                                    "002005_0006", // 기본 이미지 타입
                                    getCurrentUserId());
                        } catch (Exception e) {
                            log.error("이동 트랜잭션 이미지 업로드 중 오류 발생: {}", e.getMessage(), e);
                            // 이미지 업로드 실패가 트랜잭션 자체를 실패시키지 않도록 예외 처리
                        }
                    }
                }
            }
            
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            log.error("이동 트랜잭션 요청 처리 중 오류: {}", e.getMessage(), e);
            throw new RuntimeException("이동 트랜잭션 처리 중 오류가 발생했습니다.", e);
        }
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
     * 폐기 트랜잭션 처리 (이미지 첨부 지원)
     */
    @PostMapping(value = "/dispose-with-images")
    public ResponseEntity<FacilityTransactionDTO> processDisposeWithImages(
            @RequestParam("request") String requestJson,
            @RequestParam(value = "images", required = false) MultipartFile[] images) {
        
        try {
            // DTO 필드명에 맞게 JSON을 전송해야 함 (facilityId, notes)
            ObjectMapper objectMapper = new ObjectMapper();
            DisposeTransactionRequest request = objectMapper.readValue(requestJson, DisposeTransactionRequest.class);
            
            log.info("이미지 첨부 폐기 트랜잭션 요청: 시설물 ID={}, 폐기 사유={}", 
                    request.getFacilityId(), request.getNotes());
            
            // 기본 폐기 트랜잭션 처리
            FacilityTransactionDTO result = transactionService.processDispose(request);
            
            // 이미지가 제공된 경우 업로드 처리
            if (images != null && images.length > 0) {
                for (MultipartFile image : images) {
                    if (!image.isEmpty()) {
                        try {
                            transactionImageService.uploadTransactionImage(
                                    result.getTransactionId(), 
                                    image, 
                                    "002005_0007", // 기본 이미지 타입
                                    getCurrentUserId());
                        } catch (Exception e) {
                            log.error("폐기 트랜잭션 이미지 업로드 중 오류 발생: {}", e.getMessage(), e);
                            // 이미지 업로드 실패가 트랜잭션 자체를 실패시키지 않도록 예외 처리
                        }
                    }
                }
            }
            
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            log.error("폐기 트랜잭션 요청 처리 중 오류: {}", e.getMessage(), e);
            throw new RuntimeException("폐기 트랜잭션 처리 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 분실 트랜잭션 처리
     * POST /api/facility-transactions/lost
        {
        "facilityId": 1,
        "locationCompanyId": 1,
        "lostDate": "2025-05-20T10:00:00",
        "notes": "현장 확인 중 분실 발견"
        }
     */
    @PostMapping("/lost")
    public ResponseEntity<FacilityTransactionDTO> processLost(@Valid @RequestBody LostTransactionRequest request) {
        FacilityTransactionDTO result = transactionService.processLost(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    /**
     * 기타 트랜잭션 처리 (재고 조정 등)
     * http://localhost:8080/api/facility-transactions/misc
     * {
        "facilityId": 8,
        "locationCompanyId": 1,
        "reason": "재고 확인 결과 불일치",
        "notes": "시스템상 존재하지만 실제로 확인 불가능"
        }
     */
    @PostMapping("/misc")
    public ResponseEntity<FacilityTransactionDTO> processMisc(@Valid @RequestBody MiscTransactionRequest request) {
        FacilityTransactionDTO result = transactionService.processMisc(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    /**
     * 분실 트랜잭션 처리 (이미지 첨부 지원)
     * /api/facility-transactions/lost-with-images
     * {
        "facilityId": 2,
        "lostDate": "2025-05-16T14:40:00",
        "locationCompanyId": 1,
        "notes": "현장 점검 중 분실된 것으로 확인됨"
        }
     */
    @PostMapping(value = "/lost-with-images")
    public ResponseEntity<FacilityTransactionDTO> processLostWithImages(
            @RequestParam("request") String requestJson,
            @RequestParam(value = "images", required = false) MultipartFile[] images) {
        
        try {
            // JSON을 LostTransactionRequest 객체로 변환 (Java 8 시간 모듈 추가)
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            LostTransactionRequest request = objectMapper.readValue(requestJson, LostTransactionRequest.class);
            
            log.info("이미지 첨부 분실 트랜잭션 요청: 시설물 ID={}", request.getFacilityId());
            
            // 분실 트랜잭션 처리
            FacilityTransactionDTO result = transactionService.processLost(request);
            
            // 이미지가 제공된 경우 업로드 처리
            if (images != null && images.length > 0) {
                for (MultipartFile image : images) {
                    if (!image.isEmpty()) {
                        try {
                            transactionImageService.uploadTransactionImage(
                                    result.getTransactionId(), 
                                    image, 
                                    "002005_0010", // 기본 이미지 타입
                                    getCurrentUserId());
                        } catch (Exception e) {
                            log.error("분실 트랜잭션 이미지 업로드 중 오류 발생: {}", e.getMessage(), e);
                        }
                    }
                }
            }
            
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            log.error("분실 트랜잭션 요청 처리 중 오류: {}", e.getMessage(), e);
            throw new RuntimeException("분실 트랜잭션 처리 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 기타 트랜잭션 처리 (이미지 첨부 지원)
     * 
     *  {
        "facilityId": 8,
        "locationCompanyId": 1,
        "reason": "재고 확인 결과 불일치",
        "notes": "시스템상 존재하지만 실제로 확인 불가능"
        }
     */
    @PostMapping(value = "/misc-with-images")
    public ResponseEntity<FacilityTransactionDTO> processMiscWithImages(
            @RequestParam("request") String requestJson,
            @RequestParam(value = "images", required = false) MultipartFile[] images) {
        
        try {
            // JSON을 MiscTransactionRequest 객체로 변환
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            MiscTransactionRequest request = objectMapper.readValue(requestJson, MiscTransactionRequest.class);
            
            log.info("이미지 첨부 기타 트랜잭션 요청: 시설물 ID={}, 사유={}", 
                request.getFacilityId(), request.getReason());
            
            // 기타 트랜잭션 처리
            FacilityTransactionDTO result = transactionService.processMisc(request);
            
            // 이미지가 제공된 경우 업로드 처리
            if (images != null && images.length > 0) {
                for (MultipartFile image : images) {
                    if (!image.isEmpty()) {
                        try {
                            transactionImageService.uploadTransactionImage(
                                    result.getTransactionId(), 
                                    image, 
                                    "002005_0011", // 기본 이미지 타입
                                    getCurrentUserId());
                        } catch (Exception e) {
                            log.error("기타 트랜잭션 이미지 업로드 중 오류 발생: {}", e.getMessage(), e);
                        }
                    }
                }
            }
            
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            log.error("기타 트랜잭션 요청 처리 중 오류: {}", e.getMessage(), e);
            throw new RuntimeException("기타 트랜잭션 처리 중 오류가 발생했습니다.", e);
        }
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