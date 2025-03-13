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

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;  // 사용 여부 (기본값 true)
} 