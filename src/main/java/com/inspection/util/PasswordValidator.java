package com.inspection.util;

import java.util.regex.Pattern;

/**
 * 비밀번호 정책을 검증하는 유틸리티 클래스
 */
public class PasswordValidator {
    
    // 최소 8자, 최대 20자, 최소 하나의 소문자, 하나의 숫자, 하나의 특수문자
    private static final String PASSWORD_PATTERN = 
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,20}$";
    
    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);
    
    /**
     * 비밀번호가 정책에 맞는지 검증합니다.
     * 
     * @param password 검증할 비밀번호
     * @return 정책에 맞으면 true, 아니면 false
     */
    public static boolean isValid(String password) {
        if (password == null) {
            return false;
        }
        return pattern.matcher(password).matches();
    }
    
    /**
     * 비밀번호 정책 설명을 반환합니다.
     * 
     * @return 비밀번호 정책 설명
     */
    public static String getPasswordPolicy() {
        return "비밀번호는 8~20자 사이이며, 최소 하나의 소문자, 숫자, 특수문자(@#$%^&+=!)를 포함해야 합니다.";
    }
} 