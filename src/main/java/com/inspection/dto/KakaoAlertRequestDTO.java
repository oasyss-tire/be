package com.inspection.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KakaoAlertRequestDTO {
    private String phoneNumber;
    private String name;
    private String title;
    private String requester;
    private String contractDate;
    private String url;
} 