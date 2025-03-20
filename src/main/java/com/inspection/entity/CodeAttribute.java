package com.inspection.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "code_attribute")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public class CodeAttribute {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 속성 ID
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code_id", nullable = false)
    private Code code;  // 코드 참조
    
    @Column(nullable = false)
    private String attributeKey;  // 속성 키 (예: 'color', 'icon')
    
    @Column(nullable = false)
    private String attributeValue;  // 속성 값 (예: 'red', 'file-icon')
    
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
} 