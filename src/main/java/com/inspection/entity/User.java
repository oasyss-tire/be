package com.inspection.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter @Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String userId;  // 로그인 아이디

    @Column(nullable = false)
    private String password;  // 비밀번호

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;        // 권한 (ADMIN, MANAGER)

    @Column(nullable = false)
    private String userName;     // 사용자 이름

    private String phoneNumber;  // 전화번호
    private String email;        // 이메일

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;     // 소속 회사

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;  // 사용 여부 (기본값 true)
} 