package com.inspection.dto;

import com.inspection.entity.Role;
import com.inspection.entity.User;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserCreateDTO {
    private String userId;      // 로그인 아이디
    private String password;    // 비밀번호
    private String userName;    // 사용자 이름
    private Role role = Role.MANAGER;  // 권한
    private String phoneNumber; // 전화번호
    private String email;       // 이메일

    public User toEntity() {
        User user = new User();
        user.setUserId(userId);
        user.setPassword(password);
        user.setUserName(userName);
        user.setRole(role);
        user.setPhoneNumber(phoneNumber);
        user.setEmail(email);
        return user;
    }
} 