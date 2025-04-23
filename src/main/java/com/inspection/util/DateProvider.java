package com.inspection.util;

import org.springframework.stereotype.Component;
import java.time.LocalDate;

/**
 * 날짜 제공자 클래스
 * 현재 날짜를 제공하고 테스트를 위해 날짜를 오버라이드할 수 있는 기능 제공
 */
@Component
public class DateProvider {
    private LocalDate overrideDate = null;
    
    /**
     * 현재 날짜 반환
     * 오버라이드된 날짜가 있으면 해당 날짜 반환, 없으면 실제 현재 날짜 반환
     */
    public LocalDate getCurrentDate() {
        return overrideDate != null ? overrideDate : LocalDate.now();
    }
    
    /**
     * 테스트용 날짜 설정
     */
    public void setOverrideDate(LocalDate date) {
        this.overrideDate = date;
    }
    
    /**
     * 테스트용 날짜 초기화
     */
    public void resetOverrideDate() {
        this.overrideDate = null;
    }
} 