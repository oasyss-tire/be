package com.inspection.finance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.inspection.finance.entity.VoucherItem;

@Repository
public interface VoucherItemRepository extends JpaRepository<VoucherItem, Long> {
    
    /**
     * 특정 전표의 모든 항목 조회 (라인번호순)
     */
    List<VoucherItem> findByVoucherVoucherIdOrderByLineNumber(Long voucherId);
    
    /**
     * 특정 계정과목 코드의 모든 항목 조회
     */
    List<VoucherItem> findByAccountCode(String accountCode);
    
    /**
     * 특정 계정과목 코드별 합계 금액 조회 (차변/대변 구분)
     */
    @Query("SELECT vi.accountCode, vi.accountName, vi.isDebit, SUM(vi.amount) " +
           "FROM VoucherItem vi " +
           "WHERE vi.voucher.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY vi.accountCode, vi.accountName, vi.isDebit")
    List<Object[]> getSumByAccountAndDebitCredit(
            @Param("startDate") java.time.LocalDateTime startDate, 
            @Param("endDate") java.time.LocalDateTime endDate);
    
    /**
     * 특정 전표의 최대 라인 번호 조회
     */
    @Query("SELECT MAX(vi.lineNumber) FROM VoucherItem vi WHERE vi.voucher.voucherId = :voucherId")
    Integer findMaxLineNumberByVoucherId(@Param("voucherId") Long voucherId);
} 