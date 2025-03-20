package com.inspection.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.inspection.dto.CodeDTO;
import com.inspection.dto.CodeGroupDTO;
import com.inspection.dto.CodeHistoryDTO;
import com.inspection.entity.Code;
import com.inspection.entity.CodeAttribute;
import com.inspection.entity.CodeGroup;
import com.inspection.entity.CodeHistory;
import com.inspection.service.CodeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/codes")
@RequiredArgsConstructor
@Slf4j
public class CodeController {
    
    private final CodeService codeService;
    
    //=== 코드 그룹 API ===//
    

    // 모든 코드 그룹 조회
    @GetMapping("/groups")
    public ResponseEntity<List<CodeGroupDTO>> getAllCodeGroups() {
        List<CodeGroup> codeGroups = codeService.getAllCodeGroups();
        return ResponseEntity.ok(CodeGroupDTO.fromEntities(codeGroups));
    }
    
    // 최상위 코드 그룹 조회(대분류)
    @GetMapping("/groups/top")
    public ResponseEntity<List<CodeGroupDTO>> getTopLevelCodeGroups() {
        List<CodeGroup> codeGroups = codeService.getTopLevelCodeGroups();
        return ResponseEntity.ok(CodeGroupDTO.fromEntities(codeGroups));
    }
    
    //특정 레벨의 코드 그룹 조회(대분류, 중분류, 소분류)
    @GetMapping("/groups/level/{level}")
    public ResponseEntity<List<CodeGroupDTO>> getCodeGroupsByLevel(@PathVariable Integer level) {
        List<CodeGroup> codeGroups = codeService.getCodeGroupsByLevel(level);
        return ResponseEntity.ok(CodeGroupDTO.fromEntities(codeGroups));
    }
    
    //특정 그룹의 하위 그룹 조회
    @GetMapping("/groups/{parentGroupId}/children")
    public ResponseEntity<List<CodeGroupDTO>> getChildCodeGroups(@PathVariable String parentGroupId) {
        List<CodeGroup> codeGroups = codeService.getChildCodeGroups(parentGroupId);
        return ResponseEntity.ok(CodeGroupDTO.fromEntities(codeGroups));
    }
    
    //코드 그룹 상세 조회
    @GetMapping("/groups/{groupId}")
    public ResponseEntity<CodeGroupDTO> getCodeGroupById(@PathVariable String groupId) {
        CodeGroup codeGroup = codeService.getCodeGroupById(groupId);
        return ResponseEntity.ok(CodeGroupDTO.fromEntity(codeGroup));
    }
    
    //코드 그룹 생성
    @PostMapping("/groups")
    public ResponseEntity<CodeGroupDTO> createCodeGroup(@RequestBody CodeGroupDTO codeGroupDTO) {
        String currentUser = getCurrentUserId();
        
        // codeGroupDTO를 CodeGroup으로 변환
        CodeGroup codeGroup = new CodeGroup();
        codeGroup.setGroupName(codeGroupDTO.getGroupName());
        codeGroup.setDescription(codeGroupDTO.getDescription());
        codeGroup.setActive(codeGroupDTO.isActive());
        
        // 상위 그룹 설정 (있는 경우)
        if (codeGroupDTO.getParentGroupId() != null && !codeGroupDTO.getParentGroupId().isEmpty()) {
            CodeGroup parentGroup = new CodeGroup();
            parentGroup.setGroupId(codeGroupDTO.getParentGroupId());
            codeGroup.setParentGroup(parentGroup);
        }
        
        // 코드 그룹 ID와 레벨은 서비스에서 자동 생성
        CodeGroup createdCodeGroup = codeService.createCodeGroup(codeGroup, currentUser);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CodeGroupDTO.fromEntity(createdCodeGroup));
    }
    
    //코드 그룹 수정
    @PutMapping("/groups/{groupId}")
    public ResponseEntity<CodeGroupDTO> updateCodeGroup(
            @PathVariable String groupId, 
            @RequestBody CodeGroupDTO codeGroupDTO) {
        String currentUser = getCurrentUserId();
        
        CodeGroup codeGroup = codeGroupDTO.toEntity();
        CodeGroup updatedCodeGroup = codeService.updateCodeGroup(groupId, codeGroup, currentUser);
        
        return ResponseEntity.ok(CodeGroupDTO.fromEntity(updatedCodeGroup));
    }
    
    //코드 그룹 삭제(비활성화)
    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<Map<String, String>> deleteCodeGroup(@PathVariable String groupId) {
        String currentUser = getCurrentUserId();
        
        codeService.deleteCodeGroup(groupId, currentUser);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "코드 그룹이 성공적으로 비활성화되었습니다.");
        return ResponseEntity.ok(response);
    }
    
    //=== 코드 API ===//
    
    //그룹에 속한 모든 코드 조회
    @GetMapping("/groups/{groupId}/codes")
    public ResponseEntity<List<CodeDTO>> getCodesByGroupId(@PathVariable String groupId) {
        List<Code> codes = codeService.getCodesByGroupId(groupId);
        return ResponseEntity.ok(CodeDTO.fromEntities(codes));
    }
    
    //그룹에 속한 활성화된 코드만 조회
    @GetMapping("/groups/{groupId}/codes/active")
    public ResponseEntity<List<CodeDTO>> getActiveCodesByGroupId(@PathVariable String groupId) {
        List<Code> codes = codeService.getActiveCodesByGroupId(groupId);
        return ResponseEntity.ok(CodeDTO.fromEntities(codes));
    }
    
    //코드 상세 조회
    @GetMapping("/codes/{codeId}")
    public ResponseEntity<CodeDTO> getCodeById(@PathVariable String codeId) {
        Code code = codeService.getCodeById(codeId);
        return ResponseEntity.ok(CodeDTO.fromEntity(code));
    }
    
    //코드 생성
    @PostMapping("/codes")
    public ResponseEntity<CodeDTO> createCode(@RequestBody Map<String, Object> request) {
        String currentUser = getCurrentUserId();
        
        // 요청에서 코드와 속성 추출
        Code code = extractCodeFromRequest(request);
        List<CodeAttribute> attributes = extractAttributesFromRequest(request);
        
        // 코드 ID는 서비스에서 자동 생성
        Code createdCode = codeService.createCode(code, attributes, currentUser);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CodeDTO.fromEntity(createdCode));
    }
    
    //코드 수정
    @PutMapping("/codes/{codeId}")
    public ResponseEntity<CodeDTO> updateCode(
            @PathVariable String codeId,
            @RequestBody Map<String, Object> request) {
        String currentUser = getCurrentUserId();
        
        // 요청에서 코드와 속성 추출
        Code code = extractCodeFromRequest(request);
        List<CodeAttribute> attributes = extractAttributesFromRequest(request);
        
        Code updatedCode = codeService.updateCode(codeId, code, attributes, currentUser);
        
        return ResponseEntity.ok(CodeDTO.fromEntity(updatedCode));
    }
    
    //코드 삭제(비활성화)
    @DeleteMapping("/codes/{codeId}")
    public ResponseEntity<Map<String, String>> deleteCode(@PathVariable String codeId) {
        String currentUser = getCurrentUserId();
        
        codeService.deleteCode(codeId, currentUser);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "코드가 성공적으로 비활성화되었습니다.");
        return ResponseEntity.ok(response);
    }
    
    //코드 이력 조회
    @GetMapping("/codes/{codeId}/history")
    public ResponseEntity<List<CodeHistoryDTO>> getCodeHistory(@PathVariable String codeId) {
        List<CodeHistory> history = codeService.getCodeHistory(codeId);
        return ResponseEntity.ok(CodeHistoryDTO.fromEntities(history));
    }
    
    //=== 헬퍼 메서드 ===//
    
    //현재 로그인한 사용자 ID 조회
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "system";
    }
    
    //요청에서 코드 정보 추출
    @SuppressWarnings("unchecked")
    private Code extractCodeFromRequest(Map<String, Object> request) {
        Code code = new Code();
        
        Map<String, Object> codeData = (Map<String, Object>) request.get("code");
        if (codeData != null) {
            // 코드 ID는 자동생성 로직에서 처리하기 위해 명시적으로 지정된 경우에만 설정
            if (codeData.containsKey("codeId") && codeData.get("codeId") != null 
                    && !((String)codeData.get("codeId")).isEmpty()) {
                code.setCodeId((String) codeData.get("codeId"));
            }
            
            code.setCodeName((String) codeData.get("codeName"));
            
            // 코드 그룹 설정
            String groupId = (String) codeData.get("groupId");
            if (groupId != null) {
                CodeGroup codeGroup = new CodeGroup();
                codeGroup.setGroupId(groupId);
                code.setCodeGroup(codeGroup);
            }
            
            // 정렬 순서 설정
            Integer sortOrder = (Integer) codeData.get("sortOrder");
            code.setSortOrder(sortOrder != null ? sortOrder : 0);
            
            // 활성화 여부 설정
            Boolean isActive = (Boolean) codeData.get("active");
            code.setActive(isActive != null ? isActive : true);
            
            // 설명 설정
            code.setDescription((String) codeData.get("description"));
        }
        
        return code;
    }
    
    //요청에서 속성 정보 추출
    @SuppressWarnings("unchecked")
    private List<CodeAttribute> extractAttributesFromRequest(Map<String, Object> request) {
        List<CodeAttribute> attributes = new ArrayList<>();
        
        List<Map<String, Object>> attributesData = (List<Map<String, Object>>) request.get("attributes");
        if (attributesData != null) {
            for (Map<String, Object> attrData : attributesData) {
                CodeAttribute attribute = new CodeAttribute();
                attribute.setAttributeKey((String) attrData.get("attributeKey"));
                attribute.setAttributeValue((String) attrData.get("attributeValue"));
                attributes.add(attribute);
            }
        }
        
        return attributes;
    }
} 