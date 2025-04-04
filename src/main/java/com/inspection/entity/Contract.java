package com.inspection.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;                   // 계약 제목
    
    // 계약 상태 코드와 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_code_id")
    private Code statusCode;               // 계약 상태 코드 (코드 관리 시스템 연결)
    
    private Integer progressRate;           // 계약 진행률 (%)
    // private String contractType;         // 계약 구분 (추후 코드로 관리)
    
    private LocalDateTime createdAt;        // 계약 작성일
    private LocalDateTime startDate;        // 계약 시작일
    private LocalDateTime expiryDate;       // 계약 만료일
    private LocalDateTime completedAt;      // 계약 완료일
    private LocalDateTime lastModifiedAt;   // 최종 수정일
    
    // 추가: 관리자 승인 관련 필드
    private LocalDateTime approvedAt;      // 관리자 승인 일시
    private String approvedBy;             // 승인자 (관리자 ID 또는 이름)
    private String rejectionReason;        // 반려 사유
    
    // 다중 템플릿 관계 추가
    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContractTemplateMapping> templateMappings = new ArrayList<>();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = true)
    private Company company;                // 계약 회사
    
    private String contractPdfId;           // 실제 계약서 PDF ID (대표 PDF, 첫 번째 템플릿의 PDF)
    
    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL)
    private List<ContractParticipant> participants = new ArrayList<>();  // 서명 참여자 목록
    
    private String createdBy;               // 계약 작성자 (담당자)
    private String description;             // 계약 설명/비고
    private String cancelReason;            // 계약 취소/거절 사유
    private boolean active;  // isActive -> active로 변경
    
    // 계약 관련 추가 필드들
    private LocalDateTime deadlineDate;     // 서명 마감 기한
    private String contractNumber;          // 계약 번호 (관리용)
    private String department;              // 담당 부서
    
    // 편의 메서드
    public void addParticipant(ContractParticipant participant) {
        this.participants.add(participant);
        participant.setContract(this);
    }
    
    // 템플릿 매핑 추가 메서드
    public void addTemplateMapping(ContractTemplate template, int sortOrder, String processedPdfId) {
        ContractTemplateMapping mapping = ContractTemplateMapping.create(this, template, sortOrder, processedPdfId);
        this.templateMappings.add(mapping);
    }
    
    // 진행률 계산 메서드
    public void calculateProgressRate() {
        if (participants.isEmpty()) {
            this.progressRate = 0;
            return;
        }
        long signedCount = participants.stream()
            .filter(ContractParticipant::isSigned)
            .count();
        this.progressRate = (int) ((signedCount * 100) / participants.size());
    }
    
    // 서명 상태 초기화 메서드
    public void resetSignStatus() {
        for (ContractParticipant participant : participants) {
            // 현재 계약에 속하는 참여자만 처리 (방어적 코딩)
            if (!participant.getContract().getId().equals(this.getId())) {
                continue;
            }
            
            participant.setSigned(false);
            participant.setSignedAt(null);
            
            // 템플릿 매핑에 대한 서명 상태도 초기화
            if (participant.getTemplateMappings() != null) {
                for (var templateMapping : participant.getTemplateMappings()) {
                    templateMapping.setSigned(false);
                    templateMapping.setSignedAt(null);
                    templateMapping.setSignedPdfId(null);
                }
            }
        }
        calculateProgressRate();
    }
    
    // 모든 참여자가 서명했는지 확인하는 메서드
    public boolean isAllSigned() {
        if (participants.isEmpty()) {
            return false;
        }
        return participants.stream().allMatch(ContractParticipant::isSigned);
    }
    
    // 계약 상태 설정 메서드
    public void setStatus(Code statusCode) {
        this.statusCode = statusCode;
        // 상태에 따른 추가 로직 수행
    }
    
    // 계약 승인 메서드
    public void approve(String approvedBy) {
        this.approvedBy = approvedBy;
        this.approvedAt = LocalDateTime.now();
        // 승인 시 상태 변경 등의 추가 로직 필요
    }
    
    // 계약 반려 메서드
    public void reject(String rejectionReason) {
        this.rejectionReason = rejectionReason;
        // 반려 시 상태 변경 등의 추가 로직 필요
    }
} 