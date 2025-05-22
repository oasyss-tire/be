package com.inspection.nice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * NICE 본인인증 결과 데이터 DTO
 * 본인인증 완료 후 NICE로부터 받은 사용자 정보를 담습니다.
 */
@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NiceCertificationResultDto {

    // 인증 요청 번호
    private String requestNo;
    
    // 인증 응답 번호
    private String responseNo;
    
    // 인증 방식 (M: 휴대폰인증, C: 카드본인확인, X: 공동인증서, U: 금융인증서 등)
    private String authType;
    
    // 인증 요청자 성명
    private String name;
    
    // 생년월일 (YYYYMMDD)
    private String birthDate;
    
    // 성별 코드 (남:1, 여:0)
    private String gender;
    
    // 내외국인 구분 (내국인:0, 외국인:1)
    private String nationalInfo;
    
    // DI (중복가입확인정보)
    private String di;
    
    // CI (연계정보)
    private String ci;
    
    // 휴대폰 번호
    private String mobileNo;
    
    // 통신사 코드 (SKT:1, KT:2, LGU+:3, SKT알뜰폰:5, KT알뜰폰:6, LGU+알뜰폰:7)
    private String mobileCo;
    
    // 인증 일시 (YYYYMMDDHHmmss)
    private String encTime;
    
    // 결과 코드
    private String resultCode;
    
    // 사이트 코드
    private String siteCode;
} 