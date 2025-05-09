package com.inspection.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 여러 회사 정보를 일괄 등록하기 위한 배치 요청 DTO
 */
@Getter
@Setter
@ToString
public class CompanyBatchRequest {
    private List<CreateCompanyRequest> companies;
} 