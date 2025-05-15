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
    @Column(nullable = false, length = 20)
    private Role role;        // 권한 (ADMIN, FINANCE_MANAGER, CONTRACT_MANAGER, FACILITY_MANAGER, AS_MANAGER, MANAGER, USER)

    @Column(nullable = false)
    private String userName;     // 사용자 이름

    private String phoneNumber;  // 전화번호
    
    private String email;        // 이메일

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;     // 소속 회사
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_group_id")
    private Code branchGroup;    // 담당 지부 그룹 (AS 매니저가 담당하는 지부, Company의 branchGroup과 연결)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_type_code")
    private Code departmentType; // 담당 부서 (AS 매니저가 담당하는 부서, ServiceRequest의 departmentType과 연결)

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;  // 사용 여부 (기본값 true)
} 