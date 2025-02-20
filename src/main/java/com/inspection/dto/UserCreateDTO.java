package com.inspection.dto;

import com.inspection.entity.Role;
import com.inspection.entity.User;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserCreateDTO {
    private String username;
    private String password;
    private String fullName;
    private Long companyId;
    private Role role = Role.MANAGER;
    private String phoneNumber;
    private String email;

    public User toEntity() {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setFullName(fullName);
        user.setRole(role);
        user.setPhoneNumber(phoneNumber);
        user.setEmail(email);
        return user;
    }
} 