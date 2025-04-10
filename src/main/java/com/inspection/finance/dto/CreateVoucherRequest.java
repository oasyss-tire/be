package com.inspection.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 전표 생성 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVoucherRequest {
    
    @NotBlank(message = "전표 유형 코드는 필수입니다.")
    private String voucherTypeCode;
    
    @NotNull(message = "거래일자는 필수입니다.")
    private LocalDateTime transactionDate;
    
    @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다.")
    private String description;
    
    private Long facilityId;  // 관련 시설물 ID (선택)
    
    private Long facilityTransactionId;  // 관련 시설물 트랜잭션 ID (선택)
    
    @NotNull(message = "총액은 필수입니다.")
    private BigDecimal totalAmount;
    
    @NotEmpty(message = "최소 1개 이상의 전표 항목이 필요합니다.")
    @Valid
    private List<CreateVoucherItemRequest> items = new ArrayList<>();
    
    /**
     * 전표 항목 생성 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateVoucherItemRequest {
        
        @NotBlank(message = "계정과목 코드는 필수입니다.")
        private String accountCode;
        
        @NotBlank(message = "계정과목명은 필수입니다.")
        private String accountName;
        
        @NotNull(message = "차변/대변 구분은 필수입니다.")
        private Boolean isDebit;
        
        @NotNull(message = "금액은 필수입니다.")
        private BigDecimal amount;
        
        @Size(max = 200, message = "설명은 200자를 초과할 수 없습니다.")
        private String description;
        
        private Integer lineNumber;  // 자동 할당 가능
    }
} 