package com.inspection.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 배치 처리 결과를 저장하는 DTO
 */
@Getter
@Setter
public class BatchResponseDTO {
    private int totalCount;      // 전체 처리 항목 수
    private int successCount;    // 성공한 항목 수
    private int failCount;       // 실패한 항목 수
    private List<BatchItemResult> results = new ArrayList<>();  // 개별 처리 결과
    
    /**
     * 개별 항목의 처리 결과
     */
    @Getter
    @Setter
    public static class BatchItemResult {
        private String identifier;    // 식별자 (회사명, 매장명 등)
        private boolean success;      // 성공 여부
        private String message;       // 메시지 (성공 시 ID, 실패 시 오류 메시지)
        
        public BatchItemResult(String identifier, boolean success, String message) {
            this.identifier = identifier;
            this.success = success;
            this.message = message;
        }
    }
    
    /**
     * 성공 항목 추가
     */
    public void addSuccess(String identifier, Long id) {
        results.add(new BatchItemResult(identifier, true, id.toString()));
        successCount++;
        totalCount++;
    }
    
    /**
     * 실패 항목 추가
     */
    public void addFailure(String identifier, String errorMessage) {
        results.add(new BatchItemResult(identifier, false, errorMessage));
        failCount++;
        totalCount++;
    }
} 