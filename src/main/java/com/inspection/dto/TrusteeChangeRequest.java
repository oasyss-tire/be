package com.inspection.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrusteeChangeRequest {
    private String trustee;                // 수탁자
    private String trusteeCode;            // 수탁코드
    private String representativeName;     // 대표자명
    private String businessNumber;         // 사업자번호
    private String companyName;            // 상호
    private String managerName;            // 담당자
    private String email;                  // 이메일
    private String phoneNumber;            // 휴대폰번호
    private String subBusinessNumber;      // 종사업장번호
    private String storeTelNumber;         // 매장 전화번호
    private String businessType;           // 업태
    private String businessCategory;       // 종목
    
    private LocalDate startDate;           // 계약 시작일
    private LocalDate endDate;             // 계약 종료일
    private LocalDate insuranceStartDate;  // 보증증권 시작일
    private LocalDate insuranceEndDate;    // 보증증권 종료일
    
    private String reason;                 // 변경 사유
    private String modifiedBy;             // 수정자
}
