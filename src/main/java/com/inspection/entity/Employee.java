package com.inspection.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "employees")
@Getter @Setter
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long employeeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;  // 소속 회사와의 관계

    @Column(nullable = false)
    private String name;      // 직원 이름

    @Column(nullable = false)
    private String phone;     // 연락처

    @Column
    private Boolean active = true; // 재직 여부
} 