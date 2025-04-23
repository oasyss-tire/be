package com.inspection.controller;

import com.inspection.dto.CompanyDTO;
import com.inspection.dto.TrusteeChangeRequest;
import com.inspection.service.CompanyService;
import com.inspection.service.TrusteeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.inspection.dto.TrusteeHistoryDTO;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Slf4j
public class TrusteeController {

    private final CompanyService companyService;
    private final TrusteeService trusteeService;

    /**
     * 수탁자 정보 변경 API
     */
    @PutMapping("/{companyId}/trustee")
    public ResponseEntity<?> changeTrustee(@PathVariable Long companyId, @RequestBody TrusteeChangeRequest request) {
        try {
            // 현재 인증된 사용자 정보 가져오기
            String userId = getCurrentUserId();
            if (StringUtils.hasText(userId)) {
                request.setModifiedBy(userId);
            }
            
            CompanyDTO updatedCompany = trusteeService.changeTrustee(companyId, request);
            return ResponseEntity.ok(updatedCompany);
        } catch (ResponseStatusException e) {
            log.error("수탁자 정보 변경 중 오류 발생: {}", e.getReason());
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", "오류", "message", e.getReason()));
        } catch (Exception e) {
            log.error("수탁자 정보 변경 중 서버 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "서버 오류", "message", "수탁자 정보 변경 중 오류가 발생했습니다"));
        }
    }
    
    /**
     * 재계약 API
     * 기존 수탁자와 같은 정보로 재계약 진행
     */
    @PostMapping("/{companyId}/renew-contract")
    public ResponseEntity<?> renewContract(
            @PathVariable Long companyId,
            @RequestBody Map<String, Object> request) {
        try {
            // 현재 인증된 사용자 정보 가져오기
            String userId = getCurrentUserId();
            
            // 재계약 처리
            Map<String, Object> result = trusteeService.renewContract(companyId, request, userId);
            return ResponseEntity.ok(result);
        } catch (ResponseStatusException e) {
            log.error("재계약 처리 중 오류 발생: {}", e.getReason());
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", "오류", "message", e.getReason()));
        } catch (Exception e) {
            log.error("재계약 처리 중 서버 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "서버 오류", "message", "재계약 처리 중 오류가 발생했습니다"));
        }
    }
    
    /**
     * 현재 인증된 사용자의 ID를 가져옵니다.
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * 회사의 수탁자 이력 목록 조회 (계약 생성 시 선택용)
     * 활성 및 미래 계약 이력을 모두 포함
     */
    @GetMapping("/{companyId}/histories-for-contract")
    public ResponseEntity<?> getTrusteeHistoriesForContract(@PathVariable Long companyId) {
        try {
            List<TrusteeHistoryDTO> histories = trusteeService.getTrusteeHistoriesForContract(companyId);
            return ResponseEntity.ok(histories);
        } catch (ResponseStatusException e) {
            log.error("수탁자 이력 조회 중 오류 발생: {}", e.getReason());
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", "오류", "message", e.getReason()));
        } catch (Exception e) {
            log.error("수탁자 이력 조회 중 서버 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "서버 오류", "message", "수탁자 이력 조회 중 오류가 발생했습니다"));
        }
    }
}
