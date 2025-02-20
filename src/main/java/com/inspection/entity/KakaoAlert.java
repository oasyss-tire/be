package com.inspection.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "kakao_alerts")
@Getter @Setter
public class KakaoAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;  // 발신자 ID

    @Column(nullable = false)
    private String receiverPhone;  // 수신자 번호

    @Column(nullable = false)
    private String message;  // 메시지 내용

    @Column(nullable = false)
    private String cpId;  // 알림톡 번호 (CPID)

    @Column(nullable = false)
    private LocalDateTime sentAt;  // 발송 시간
} 