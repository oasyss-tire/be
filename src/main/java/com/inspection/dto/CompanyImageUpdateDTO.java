package com.inspection.dto;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;


@Data
public class CompanyImageUpdateDTO {
    private Long companyId;
    private MultipartFile exteriorImage;
    private MultipartFile entranceImage;
    private MultipartFile mainPanelImage;
    private MultipartFile etcImage1;
    private MultipartFile etcImage2;
    private MultipartFile etcImage3;
    private MultipartFile etcImage4;
}