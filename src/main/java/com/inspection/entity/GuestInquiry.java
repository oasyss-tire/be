package com.inspection.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "guest_inquiries")
@Getter @Setter
public class GuestInquiry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String guestName;  // 문의자 이름

    @Column(nullable = false)
    private String phoneNumber;  // 문의자 번호

    @Column(nullable = false)
    private String password;  // 게스트용 비밀번호

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;  // 문의 내용

    @Column(columnDefinition = "TEXT")
    private String answer;  // 답변 내용

    private LocalDateTime answerTime;  // 답변 작성시간

    @Column(nullable = false)
    private Boolean answered = false;  // 답변 여부

    @Column(nullable = false)
    private LocalDateTime createdAt;  // 문의글 작성시간

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
} 