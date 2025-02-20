package com.inspection.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
public class KakaoAlertDTO {
    private Long id;
    private Long userId;
    private String username;
    private String receiverPhone;
    private String message;
    private String cpId;
    private LocalDateTime sentAt;
} 