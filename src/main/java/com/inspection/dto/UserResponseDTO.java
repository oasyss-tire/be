package com.inspection.dto;

import com.inspection.entity.User;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserResponseDTO {
    private Long id;           // PK
    private String userId;     // 로그인 아이디
    private String userName;   // 사용자 이름
    private String email;      // 이메일
    private String phoneNumber; // 전화번호
    private String role;       // 권한
    private boolean active;    // 활성화 여부
    
    // Company 관련 필드
    private Long companyId;    // 회사 ID
    private String companyName; // 회사명
    private String storeCode;   // 매장코드
    
    // 지부 그룹 관련 필드
    private String branchGroupId;   // 담당 지부 그룹 코드
    private String branchGroupName; // 담당 지부 그룹명
    
    // 담당 부서 관련 필드
    private String departmentTypeId;   // 담당 부서 코드
    private String departmentTypeName; // 담당 부서명

    public UserResponseDTO(User user) {
        this.id = user.getId();
        this.userId = user.getUserId();
        this.userName = user.getUserName();
        this.email = user.getEmail();
        this.phoneNumber = user.getPhoneNumber();
        this.role = user.getRole().name();
        this.active = user.isActive();
        
        // Company 정보 설정
        if (user.getCompany() != null) {
            this.companyId = user.getCompany().getId();
            this.companyName = user.getCompany().getStoreName();
            this.storeCode = user.getCompany().getStoreCode();
        }
        
        // 지부 그룹 정보 설정
        if (user.getBranchGroup() != null) {
            this.branchGroupId = user.getBranchGroup().getCodeId();
            this.branchGroupName = user.getBranchGroup().getCodeName();
        }
        
        // 담당 부서 정보 설정
        if (user.getDepartmentType() != null) {
            this.departmentTypeId = user.getDepartmentType().getCodeId();
            this.departmentTypeName = user.getDepartmentType().getCodeName();
        }
    }
} 