package com.inspection.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CompanyStatus {
    ACTIVE("사용"),
    TERMINATED("해지");
    
    private final String description;
    
    CompanyStatus(String description) {
        this.description = description;
    }
    
    @JsonValue  // enum -> JSON 변환 시 사용
    public String getValue() {
        return this.name();  // "ACTIVE" 또는 "TERMINATED" 반환
    }
    
    @JsonCreator  // JSON -> enum 변환 시 사용
    public static CompanyStatus fromValue(String value) {
        try {
            return CompanyStatus.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }
    
    public String getDescription() {
        return description;
    }
}