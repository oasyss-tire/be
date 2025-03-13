package com.inspection.dto;

import com.inspection.entity.Role;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserUpdateDTO {
    private String userName;    // 사용자 이름
    private String email;       // 이메일
    private String phoneNumber; // 전화번호
    private Role role;         // 권한
    private boolean active;     // 활성화 여부
} 