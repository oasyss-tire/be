package com.inspection.dto;

import com.inspection.entity.Role;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserUpdateDTO {
    private String fullName;
    private String email;
    private String phoneNumber;
    private Role role;
    private Long companyId;
    private boolean active;
} 