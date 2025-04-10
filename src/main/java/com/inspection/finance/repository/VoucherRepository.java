package com.inspection.finance.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.inspection.finance.entity.Voucher;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    
    /**
     * 전표번호로 전표 조회
     */
    Optional<Voucher> findByVoucherNumber(String voucherNumber);
    
    /**
     * 특정 거래일자 범위의 전표 조회
     */
    List<Voucher> findByTransactionDateBetweenOrderByTransactionDateDesc(
            LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 특정 거래일자 범위의 전표 페이징 조회
     */
    Page<Voucher> findByTransactionDateBetweenOrderByTransactionDateDesc(
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    /**
     * 특정 시설물의 전표 조회
     */
    List<Voucher> findByFacilityFacilityIdOrderByTransactionDateDesc(Long facilityId);
    
    /**
     * 특정 시설물 트랜잭션의 전표 조회
     */
    Optional<Voucher> findByFacilityTransactionTransactionId(Long transactionId);
    
    /**
     * 특정 전표 유형의 전표 조회
     */
    List<Voucher> findByVoucherTypeCodeIdOrderByTransactionDateDesc(String voucherTypeCode);
    
    /**
     * 가장 최근에 생성된 전표번호 조회 (접두사로 시작하는)
     */
    @Query("SELECT v.voucherNumber FROM Voucher v WHERE v.voucherNumber LIKE :prefix% ORDER BY v.voucherNumber DESC")
    List<String> findLatestVoucherNumberByPrefix(@Param("prefix") String prefix, Pageable pageable);
    
    /**
     * 모든 전표 조회 (거래일자 내림차순)
     */
    List<Voucher> findAllByOrderByTransactionDateDesc();
} 