package com.inspection.finance.dto;

import java.math.BigDecimal;

import com.inspection.finance.entity.VoucherItem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 전표 항목 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherItemDTO {
    
    private Long itemId;
    private Long voucherId;
    private String accountCode;
    private String accountName;
    private boolean isDebit;  // 차변(true) / 대변(false)
    private BigDecimal amount;
    private String description;
    private Integer lineNumber;
    
    /**
     * 엔티티를 DTO로 변환
     */
    public static VoucherItemDTO fromEntity(VoucherItem entity) {
        if (entity == null) {
            return null;
        }
        
        return VoucherItemDTO.builder()
                .itemId(entity.getItemId())
                .voucherId(entity.getVoucher() != null ? entity.getVoucher().getVoucherId() : null)
                .accountCode(entity.getAccountCode())
                .accountName(entity.getAccountName())
                .isDebit(entity.isDebit())
                .amount(entity.getAmount())
                .description(entity.getDescription())
                .lineNumber(entity.getLineNumber())
                .build();
    }
} 