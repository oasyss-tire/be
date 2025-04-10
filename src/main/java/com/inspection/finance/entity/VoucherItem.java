package com.inspection.finance.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 전표 항목 엔티티
 * 하나의 전표에 포함된 차변/대변 항목을 나타냅니다.
 */
@Entity
@Table(name = "voucher_items")
@Getter @Setter
public class VoucherItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;  // 항목 ID (PK)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;  // 소속 전표
    
    @Column(name = "account_code", nullable = false, length = 20)
    private String accountCode;  // 계정과목 코드
    
    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;  // 계정과목명
    
    @Column(name = "is_debit", nullable = false)
    private boolean isDebit;  // 차변(true) / 대변(false) 구분
    
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;  // 금액
    
    @Column(name = "description", length = 200)
    private String description;  // 항목 설명
    
    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;  // 라인번호 (정렬용)
} 