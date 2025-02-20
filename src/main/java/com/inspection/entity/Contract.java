package com.inspection.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Contract extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;                    // 계약서 제목
    private String contracteeName;           // 피계약자 이름
    private String contracteeEmail;          // 피계약자 이메일
    private String contracteePhoneNumber;    // 피계약자 연락처
    private String contractType;             // 계약 종류
    
    private String contractorName;           // 계약자 이름
    private String contractorEmail;          // 계약자 이메일
    private String contractorPhoneNumber;    // 계약자 연락처
    
    @Column(columnDefinition = "TEXT")
    private String description;              // 계약 설명/비고
    
    @Column(columnDefinition = "TEXT")
    private String pdfUrl;                   // PDF 파일 경로
    
    @Column(columnDefinition = "TEXT")
    private String signedPdfUrl;             // 서명된 PDF 경로
    
    private LocalDateTime expirationDate;    // 계약 만료일
    private LocalDateTime signedDate;        // 서명 완료일
    
    @Enumerated(EnumType.STRING)
    private ContractStatus status = ContractStatus.PENDING;

    private String originalFileName;         // 원본 파일명
    private Long fileSize;                   // 파일 크기
    
    @Column(columnDefinition = "TEXT")
    private String signaturePosition;        // 서명 위치 정보 (JSON 형식으로 저장: x, y, page)
    
    @Column(name = "email_sent")
    private boolean emailSent = false;
    
    private LocalDateTime emailSentDate;
    
    private String contractNumber;          // 계약 번호 (자동생성)
    @Column(name = "deleted")
    private boolean deleted = false;
} 