package com.inspection.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "code_history")
@Getter @Setter
public class CodeHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 이력 ID
    
    @Column(nullable = false)
    private String codeId;  // 코드 ID
    
    @Column(nullable = false)
    private String codeName;  // 코드 이름
    
    @Column(nullable = false)
    private String groupId;  // 소속 그룹 ID
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType actionType;  // 작업 유형 (CREATE, UPDATE, DELETE)
    
    @Column(nullable = false)
    private String changedBy;  // 변경자
    
    @Column(nullable = false)
    private LocalDateTime changedAt;  // 변경일
    
    @Column(columnDefinition = "TEXT")
    private String changeDetails;  // 변경 상세 내용 (JSON 형태로 저장)
    
    // 작업 유형 열거형
    public enum ActionType {
        CREATE, UPDATE, DELETE
    }
} 