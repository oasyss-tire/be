package com.inspection.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LoginRequest {
    private String userId;    // 로그인 아이디
    private String password;  // 비밀번호
} 