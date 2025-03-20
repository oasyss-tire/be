package com.inspection.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "code_group")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public class CodeGroup {
    
    @Id
    private String groupId;  // 그룹 ID (예: '001', '001001')
    
    @Column(nullable = false)
    private String groupName;  // 그룹 이름 (예: '계약', '계약 유형')
    
    @Column(nullable = false)
    private Integer level;  // 레벨 (1: 대분류, 2: 중분류, 3: 소분류)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_group_id")
    private CodeGroup parentGroup;  // 상위 그룹 참조
    
    @OneToMany(mappedBy = "parentGroup", cascade = CascadeType.ALL)
    private List<CodeGroup> childGroups = new ArrayList<>();  // 하위 그룹 목록
    
    @OneToMany(mappedBy = "codeGroup", cascade = CascadeType.ALL)
    private List<Code> codes = new ArrayList<>();  // 이 그룹에 속한 코드 목록
    
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
    
    // 편의 메서드: 하위 그룹 추가
    public void addChildGroup(CodeGroup childGroup) {
        this.childGroups.add(childGroup);
        childGroup.setParentGroup(this);
    }
    
    // 편의 메서드: 코드 추가
    public void addCode(Code code) {
        this.codes.add(code);
        code.setCodeGroup(this);
    }
} 