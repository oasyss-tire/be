package com.inspection.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EmployeeDTO {
    private Long employeeId;
    private String name;
    private String phone;
    private Boolean active;
} 