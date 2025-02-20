package com.inspection.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter @Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(unique = true, nullable = false)
    private String username;  // 로그인 아이디

    @Column(nullable = false)
    private String password;  // 비밀번호

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;        // 권한 (ADMIN, MANAGER)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "full_name", nullable = false)
    private String fullName;     // 사용자 실명

    private String phoneNumber;  // 추가
    private String email;        // 추가

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;  // 사용 여부 (기본값 true)
} 