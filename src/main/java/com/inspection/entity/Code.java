package com.inspection.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "code")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public class Code {
    
    @Id
    private String codeId;  // 코드 ID (예: '001001_0001')
    
    @Column(nullable = false)
    private String codeName;  // 코드 이름 (예: '신규 계약', '재계약')
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private CodeGroup codeGroup;  // 소속 그룹
    
    @Column(nullable = false)
    private Integer sortOrder;  // 정렬 순서
    
    @Column(nullable = false)
    private boolean active = true;  // 활성화 여부
    
    private String description;  // 설명
    
    @Column(length = 50)
    private String createdBy;  // 등록자
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;  // 등록일
    
    @Column(length = 50)
    private String updatedBy;  // 수정자
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;  // 수정일
    
    // 코드 속성 관계
    @OneToMany(mappedBy = "code", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CodeAttribute> attributes = new ArrayList<>();
} 