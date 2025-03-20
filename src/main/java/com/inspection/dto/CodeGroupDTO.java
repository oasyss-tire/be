package com.inspection.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.inspection.entity.CodeGroup;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CodeGroupDTO {
    private String groupId;          // 그룹 ID
    private String groupName;        // 그룹 이름
    private Integer level;           // 레벨 (1: 대분류, 2: 중분류, 3: 소분류)
    private String parentGroupId;    // 상위 그룹 ID
    private boolean active;          // 활성화 여부
    private String description;      // 설명
    private List<CodeGroupDTO> childGroups;  // 하위 그룹 목록
    private String createdBy;        // 등록자
    private LocalDateTime createdAt; // 등록일
    private String updatedBy;        // 수정자
    private LocalDateTime updatedAt; // 수정일
    
    /**
     * CodeGroup 엔티티를 DTO로 변환
     */
    public static CodeGroupDTO fromEntity(CodeGroup codeGroup) {
        if (codeGroup == null) return null;
        
        CodeGroupDTO dto = new CodeGroupDTO();
        dto.setGroupId(codeGroup.getGroupId());
        dto.setGroupName(codeGroup.getGroupName());
        dto.setLevel(codeGroup.getLevel());
        
        if (codeGroup.getParentGroup() != null) {
            dto.setParentGroupId(codeGroup.getParentGroup().getGroupId());
        }
        
        dto.setActive(codeGroup.isActive());
        dto.setDescription(codeGroup.getDescription());
        dto.setCreatedBy(codeGroup.getCreatedBy());
        dto.setCreatedAt(codeGroup.getCreatedAt());
        dto.setUpdatedBy(codeGroup.getUpdatedBy());
        dto.setUpdatedAt(codeGroup.getUpdatedAt());
        
        return dto;
    }
    
    /**
     * CodeGroup 엔티티 목록을 DTO 목록으로 변환
     */
    public static List<CodeGroupDTO> fromEntities(List<CodeGroup> codeGroups) {
        if (codeGroups == null) return null;
        
        List<CodeGroupDTO> dtos = new ArrayList<>();
        for (CodeGroup codeGroup : codeGroups) {
            dtos.add(fromEntity(codeGroup));
        }
        return dtos;
    }
    
    /**
     * DTO를 CodeGroup 엔티티로 변환
     */
    public CodeGroup toEntity() {
        CodeGroup codeGroup = new CodeGroup();
        codeGroup.setGroupId(this.groupId);
        codeGroup.setGroupName(this.groupName);
        codeGroup.setLevel(this.level);
        
        if (this.parentGroupId != null) {
            CodeGroup parentGroup = new CodeGroup();
            parentGroup.setGroupId(this.parentGroupId);
            codeGroup.setParentGroup(parentGroup);
        }
        
        codeGroup.setActive(this.active);
        codeGroup.setDescription(this.description);
        
        return codeGroup;
    }
} 