package com.inspection.dto;


import com.inspection.entity.User;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserResponseDTO {
    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String companyName;
    private String role;
    private boolean active;
    private Long companyId;

    public UserResponseDTO(User user) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.phoneNumber = user.getPhoneNumber();
        this.companyName = user.getCompany() != null ? user.getCompany().getCompanyName() : null;
        this.role = user.getRole().name();
        this.active = user.isActive();
        this.companyId = user.getCompany() != null ? user.getCompany().getCompanyId() : null;
    }
} 