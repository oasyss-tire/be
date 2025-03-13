package com.inspection.dto;

import com.inspection.entity.User;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserResponseDTO {
    private Long id;           // PK
    private String userId;     // 로그인 아이디
    private String userName;   // 사용자 이름
    private String email;      // 이메일
    private String phoneNumber; // 전화번호
    private String role;       // 권한
    private boolean active;    // 활성화 여부

    public UserResponseDTO(User user) {
        this.id = user.getId();
        this.userId = user.getUserId();
        this.userName = user.getUserName();
        this.email = user.getEmail();
        this.phoneNumber = user.getPhoneNumber();
        this.role = user.getRole().name();
        this.active = user.isActive();
    }
} 