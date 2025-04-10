package com.inspection.finance.util;

/**
 * 계정과목 코드 상수 정의
 * 회계 처리에 필요한 주요 계정과목 코드 관리
 */
public class AccountCodeConstants {
    
    // 자산 계정 코드
    public static final String ASSET_FACILITY = "101";         // 유형자산(시설물)
    public static final String ASSET_ACCUM_DEPRECIATION = "102";  // 감가상각누계액
    public static final String ASSET_CASH = "103";             // 현금
    public static final String ASSET_BANK_DEPOSIT = "104";     // 예금
    public static final String ASSET_ACCOUNTS_RECEIVABLE = "105";  // 매출채권
    
    // 부채 계정 코드
    public static final String LIABILITY_ACCOUNTS_PAYABLE = "201";  // 매입채무
    public static final String LIABILITY_SHORT_TERM_LOAN = "202";   // 단기차입금
    
    // 자본 계정 코드
    public static final String EQUITY_CAPITAL = "301";            // 자본금
    public static final String EQUITY_RETAINED_EARNINGS = "302";  // 이익잉여금
    
    // 수익 계정 코드
    public static final String REVENUE_SALES = "401";          // 매출
    public static final String REVENUE_INTEREST = "402";       // 이자수익
    
    // 비용 계정 코드
    public static final String EXPENSE_COST_OF_SALES = "501";  // 매출원가
    public static final String EXPENSE_DEPRECIATION = "502";   // 감가상각비
    public static final String EXPENSE_INTEREST = "503";       // 이자비용
    public static final String EXPENSE_DISPOSAL_LOSS = "504";  // 자산처분손실
} 