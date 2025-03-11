package com.inspection.enums;

import lombok.Getter;

@Getter
public enum NotificationType {
    EMAIL("이메일"),
    SMS("문자메시지"),
    KAKAO("카카오톡");
    
    private final String description;
    
    NotificationType(String description) {
        this.description = description;
    }
} 