package com.inspection.facility.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.inspection.entity.Code;
import com.inspection.entity.Company;
import com.inspection.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "facility_transactions", 
    indexes = {
        @Index(name = "idx_ft_transaction_date", columnList = "transaction_date"),
        @Index(name = "idx_ft_facility_transaction_date", columnList = "facility_id,transaction_date"),
        @Index(name = "idx_ft_from_company", columnList = "from_company_id,transaction_type_code,transaction_date"),
        @Index(name = "idx_ft_to_company", columnList = "to_company_id,transaction_type_code,transaction_date"),
        @Index(name = "idx_ft_facility_type_transaction", columnList = "facility_id,transaction_type_code,transaction_date"),
        @Index(name = "idx_ft_batch_id", columnList = "batch_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public class FacilityTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId; // 트랜잭션 ID (PK)
    
    @Column(name = "batch_id", length = 36)
    private String batchId; // 배치 ID (UUID 형식) - 같은 작업 단위 트랜잭션 그룹화
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility; // 트랜잭션 대상 시설물
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_type_code", nullable = false)
    private Code transactionType; // 트랜잭션 유형 (입고, 출고, 이동, 대여, 반납, AS, 폐기 등)
    
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate; // 트랜잭션 발생일시
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_company_id")
    private Company fromCompany; // 출발 회사/위치
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_company_id")
    private Company toCompany; // 도착 회사/위치
    
    @Column(name = "notes", length = 500)
    private String notes; // 비고/메모
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_before_code")
    private Code statusBefore; // 트랜잭션 전 시설물 상태
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_after_code")
    private Code statusAfter; // 트랜잭션 후 시설물 상태
    
    @Column(name = "expected_return_date")
    private LocalDateTime expectedReturnDate; // 반납 예정일 (대여 시)
    
    @Column(name = "actual_return_date")
    private LocalDateTime actualReturnDate; // 실제 반납일 (반납 시)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_transaction_id")
    private FacilityTransaction relatedTransaction; // 연관 트랜잭션 (대여-반납, 출고-입고 등의 쌍)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id")
    private com.inspection.as.entity.ServiceRequest serviceRequest; // 관련 AS 요청 (있는 경우)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by", nullable = false)
    private User performedBy; // 트랜잭션 수행자
    
    @Column(name = "transaction_ref", length = 100)
    private String transactionRef; // 트랜잭션 참조 번호 (외부 시스템 연동용)
    
    @Column(name = "is_cancelled", nullable = false)
    private Boolean isCancelled = false; // 취소 여부
    
    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason; // 취소 사유
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 등록날짜
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 수정날짜
} 