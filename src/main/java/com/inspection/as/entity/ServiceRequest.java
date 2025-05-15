package com.inspection.as.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.inspection.entity.Code;
import com.inspection.entity.User;
import com.inspection.facility.entity.Facility;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "service_request")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ServiceRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_request_id")
    private Long serviceRequestId;   // AS 고유 접수 번호
    
    @Column(name = "request_number", length = 20, nullable = false, unique = true)
    private String requestNumber;   // AS 접수 번호
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;  // 시설물 정보
    
    @Column(name = "request_date", nullable = false)
    private LocalDateTime requestDate;  // 접수 일자
    
    @Column(name = "is_received", nullable = false)
    private Boolean isReceived;  // 접수 여부
    
    @Column(name = "request_content", length = 2000, nullable = false)
    private String requestContent;  // 요청 내용
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;  // 요청자
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;  // 담당자
    
    @Column(name = "expected_completion_date")
    private LocalDateTime expectedCompletionDate;  // 수리 예정일
    
    @Column(name = "completion_date")
    private LocalDateTime completionDate;  // 수리 완료일
    
    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted;  // 완료 여부
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_type_code", nullable = false)
    private Code serviceType;   // AS유형 코드 (일반수리, 긴급수리, 정기점검)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "priority_code", nullable = false)
    private Code priority;  // 우선순위 코드 (긴급, 일반)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_code")
    private Code status;  // 서비스 요청 상태 코드 (접수중, 접수완료, 수리중, 수리완료)
    
    @Column(name = "cost")
    private Double cost;  // 수리 비용
    
    @Column(name = "notes", length = 500)
    private String notes;  // 비고
    
    @Column(name = "repair_comment", length = 2000)
    private String repairComment;  // 수리 코멘트 (수리 내용, 문제 원인 등)
    
    @Column(name = "original_location_company_id")
    private Long originalLocationCompanyId;  // 원래 시설물 위치 회사 ID (AS 접수 시 저장)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_type_code")
    private Code departmentType;  // 담당 부서 유형 (메인장비팀, 전기팀, 시설팀)
    
    @OneToMany(mappedBy = "serviceRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceHistory> serviceHistories;
    
    @OneToMany(mappedBy = "serviceRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceRequestImage> images;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;  // 생성일자
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;  // 수정일자
}   