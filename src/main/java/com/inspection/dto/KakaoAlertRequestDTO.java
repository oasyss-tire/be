package com.inspection.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter

public class KakaoAlertRequestDTO {
    private String phoneNumber;  // 수신자 전화번호
    private String message;      // 전송할 메시지
} 