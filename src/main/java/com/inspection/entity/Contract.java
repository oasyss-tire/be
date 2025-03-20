package com.inspection.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;                   // 계약 제목
    
    // private ContractStatus status;       // 계약 상태 (추후 코드로 관리)
    private Integer progressRate;           // 계약 진행률 (%)
    // private String contractType;         // 계약 구분 (추후 코드로 관리)
    
    private LocalDateTime createdAt;        // 계약 작성일
    private LocalDateTime startDate;        // 계약 시작일
    private LocalDateTime expiryDate;       // 계약 만료일
    private LocalDateTime completedAt;      // 계약 완료일
    private LocalDateTime lastModifiedAt;   // 최종 수정일
    
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
} 