package com.inspection.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspection.entity.Code;
import com.inspection.entity.CodeAttribute;
import com.inspection.entity.CodeGroup;
import com.inspection.entity.CodeHistory;
import com.inspection.entity.CodeHistory.ActionType;
import com.inspection.repository.CodeAttributeRepository;
import com.inspection.repository.CodeGroupRepository;
import com.inspection.repository.CodeHistoryRepository;
import com.inspection.repository.CodeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeService {
    
    private final CodeGroupRepository codeGroupRepository;
    private final CodeRepository codeRepository;
    private final CodeAttributeRepository codeAttributeRepository;
    private final CodeHistoryRepository codeHistoryRepository;
    
    //=== 코드 그룹 ID 자동생성 관련 메서드 ===//
    
    /**
     * 코드 그룹 ID 자동 생성
     * @param level 레벨 (1: 대분류, 2: 중분류, 3: 소분류)
     * @param parentGroupId 상위 그룹 ID (대분류인 경우 null)
     * @return 생성된 그룹 ID
     */
    public String generateGroupId(int level, String parentGroupId) {
        // 1. 새로운 일련번호 생성 (001~999)
        int newSequence;
        
        if (level == 1) {
            // 대분류인 경우 - 전체 대분류 중 마지막 번호 조회
            Optional<CodeGroup> lastGroup = codeGroupRepository.findTopByLevelOrderByGroupIdDesc(1);
            if (lastGroup.isPresent()) {
                String lastGroupId = lastGroup.get().getGroupId();
                newSequence = Integer.parseInt(lastGroupId) + 1;
            } else {
                newSequence = 1; // 첫 번째 대분류
            }
        } else {
            // 중분류/소분류인 경우 - 해당 상위 그룹 내의 마지막 번호 조회
            Optional<CodeGroup> lastChildGroup;
            if (level == 2) {
                lastChildGroup = codeGroupRepository.findTopByParentGroupGroupIdAndLevelOrderByGroupIdDesc(parentGroupId, 2);
            } else { // level == 3
                lastChildGroup = codeGroupRepository.findTopByParentGroupGroupIdAndLevelOrderByGroupIdDesc(parentGroupId, 3);
            }
            
            if (lastChildGroup.isPresent()) {
                String lastGroupId = lastChildGroup.get().getGroupId();
                // 마지막 3자리만 추출
                String lastSequenceStr = lastGroupId.substring(lastGroupId.length() - 3);
                newSequence = Integer.parseInt(lastSequenceStr) + 1;
            } else {
                newSequence = 1; // 첫 번째 하위 그룹
            }
        }

        // 최대값 검사
        if (newSequence > 999) {
            throw new IllegalStateException("더 이상 생성할 수 없습니다. 최대값(999)을 초과했습니다.");
        }

        // 2. 새로운 그룹 ID 생성
        String sequencePart = String.format("%03d", newSequence);
        return level == 1 ? sequencePart : parentGroupId + sequencePart;
    }
    
    //=== 코드 ID 자동생성 관련 메서드 ===//
    
    /**
     * 코드 ID 자동 생성
     * @param groupId 그룹 ID
     * @return 생성된 코드 ID
     */
    public String generateCodeId(String groupId) {
        // 해당 그룹의 마지막 코드 ID 조회
        Optional<Code> lastCode = codeRepository.findTopByCodeGroupGroupIdOrderByCodeIdDesc(groupId);
        
        int newSequence;
        if (lastCode.isPresent()) {
            String lastCodeId = lastCode.get().getCodeId();
            // ID 형식이 groupId_XXXX 형태인지 확인
            if (lastCodeId.contains("_")) {
                String[] parts = lastCodeId.split("_");
                if (parts.length == 2) {
                    try {
                        newSequence = Integer.parseInt(parts[1]) + 1;
                    } catch (NumberFormatException e) {
                        // 마지막 코드가 숫자 형식이 아닌 경우
                        newSequence = 1;
                    }
                } else {
                    newSequence = 1;
                }
            } else {
                // 기존 형식이 다른 경우 새로 시작
                newSequence = 1;
            }
        } else {
            // 해당 그룹의 첫 번째 코드
            newSequence = 1;
        }
        
        // 최대값 검사
        if (newSequence > 9999) {
            throw new IllegalStateException("더 이상 생성할 수 없습니다. 최대값(9999)을 초과했습니다.");
        }
        
        // 새로운 코드 ID 생성 (groupId_XXXX 형식)
        return groupId + "_" + String.format("%04d", newSequence);
    }
    
    //=== 코드 그룹 관련 메서드 ===//
    
    /**
     * 모든 코드 그룹 조회
     */
    public List<CodeGroup> getAllCodeGroups() {
        return codeGroupRepository.findAll();
    }
    
    /**
     * 최상위 코드 그룹 조회 (대분류)
     */
    public List<CodeGroup> getTopLevelCodeGroups() {
        return codeGroupRepository.findByLevel(1);
    }
    
    /**
     * 특정 레벨의 코드 그룹 조회
     */
    public List<CodeGroup> getCodeGroupsByLevel(Integer level) {
        return codeGroupRepository.findByLevel(level);
    }
    
    /**
     * 특정 그룹의 하위 그룹 조회
     */
    public List<CodeGroup> getChildCodeGroups(String parentGroupId) {
        return codeGroupRepository.findByParentGroupGroupId(parentGroupId);
    }
    
    /**
     * 코드 그룹 상세 조회
     */
    public CodeGroup getCodeGroupById(String groupId) {
        return codeGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("코드 그룹을 찾을 수 없습니다: " + groupId));
    }
    
    /**
     * 코드 그룹 생성 (자동 ID 생성 적용)
     */
    @Transactional
    public CodeGroup createCodeGroup(CodeGroup codeGroup, String currentUser) {
        // 그룹 ID가 없는 경우 자동 생성
        if (codeGroup.getGroupId() == null || codeGroup.getGroupId().isEmpty()) {
            int level;
            String parentGroupId = null;
            
            // 상위 그룹 설정 및 레벨 결정
            if (codeGroup.getParentGroup() != null && codeGroup.getParentGroup().getGroupId() != null) {
                final String parentId = codeGroup.getParentGroup().getGroupId();
                parentGroupId = parentId;
                CodeGroup parentGroup = codeGroupRepository.findById(parentId)
                        .orElseThrow(() -> new RuntimeException("상위 그룹을 찾을 수 없습니다: " + parentId));
                codeGroup.setParentGroup(parentGroup);
                
                // 상위 그룹 레벨에 따라 현재 그룹 레벨 결정
                level = parentGroup.getLevel() + 1;
                if (level > 3) {
                    throw new IllegalArgumentException("최대 3단계까지만 그룹을 생성할 수 있습니다.");
                }
            } else {
                // 상위 그룹이 없으면 대분류(level 1)
                level = 1;
            }
            
            // 레벨 설정
            codeGroup.setLevel(level);
            
            // 그룹 ID 자동 생성
            String groupId = generateGroupId(level, parentGroupId);
            codeGroup.setGroupId(groupId);
            
            log.info("코드 그룹 ID 자동 생성: {}, 레벨: {}", groupId, level);
        } else {
            // 그룹 ID 중복 체크
            if (codeGroupRepository.existsById(codeGroup.getGroupId())) {
                throw new RuntimeException("이미 존재하는 그룹 ID입니다: " + codeGroup.getGroupId());
            }
        
            // 상위 그룹 설정 (있는 경우)
            if (codeGroup.getParentGroup() != null && codeGroup.getParentGroup().getGroupId() != null) {
                final String parentId = codeGroup.getParentGroup().getGroupId();
                CodeGroup parentGroup = codeGroupRepository.findById(parentId)
                        .orElseThrow(() -> new RuntimeException("상위 그룹을 찾을 수 없습니다: " + parentId));
                codeGroup.setParentGroup(parentGroup);
            }
        }
        
        // 등록자 정보 설정
        codeGroup.setCreatedBy(currentUser);
        codeGroup.setUpdatedBy(currentUser);
        
        log.info("코드 그룹 생성: {}, 레벨: {}, 등록자: {}", 
                codeGroup.getGroupName(), codeGroup.getLevel(), currentUser);
        
        return codeGroupRepository.save(codeGroup);
    }
    
    /**
     * 코드 그룹 수정
     */
    @Transactional
    public CodeGroup updateCodeGroup(String groupId, CodeGroup updatedGroup, String currentUser) {
        CodeGroup existingGroup = codeGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("코드 그룹을 찾을 수 없습니다: " + groupId));
        
        // 그룹 정보 업데이트
        existingGroup.setGroupName(updatedGroup.getGroupName());
        existingGroup.setActive(updatedGroup.isActive());
        existingGroup.setDescription(updatedGroup.getDescription());
        existingGroup.setUpdatedBy(currentUser);
        
        log.info("코드 그룹 수정: {}, 수정자: {}", groupId, currentUser);
        
        return codeGroupRepository.save(existingGroup);
    }
    
    /**
     * 코드 그룹 삭제 (비활성화)
     */
    @Transactional
    public void deleteCodeGroup(String groupId, String currentUser) {
        CodeGroup codeGroup = codeGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("코드 그룹을 찾을 수 없습니다: " + groupId));
        
        // 실제 삭제 대신 비활성화
        codeGroup.setActive(false);
        codeGroup.setUpdatedBy(currentUser);
        codeGroupRepository.save(codeGroup);
        
        log.info("코드 그룹 비활성화: {}, 처리자: {}", groupId, currentUser);
    }
    
    //=== 코드 관련 메서드 ===//
    
    /**
     * 그룹에 속한 모든 코드 조회
     */
    public List<Code> getCodesByGroupId(String groupId) {
        return codeRepository.findByCodeGroupGroupId(groupId);
    }
    
    /**
     * 그룹에 속한 활성화된 코드만 조회
     */
    public List<Code> getActiveCodesByGroupId(String groupId) {
        return codeRepository.findByCodeGroupGroupIdAndActiveTrue(groupId);
    }
    
    /**
     * 코드 상세 조회
     */
    public Code getCodeById(String codeId) {
        return codeRepository.findById(codeId)
                .orElseThrow(() -> new RuntimeException("코드를 찾을 수 없습니다: " + codeId));
    }
    
    /**
     * 코드 생성 (자동 ID 생성 적용)
     */
    @Transactional
    public Code createCode(Code code, List<CodeAttribute> attributes, String currentUser) {
        // 코드 ID가 없는 경우 자동 생성
        if (code.getCodeId() == null || code.getCodeId().isEmpty()) {
            // 코드 그룹 확인
            if (code.getCodeGroup() == null || code.getCodeGroup().getGroupId() == null) {
                throw new IllegalArgumentException("코드 그룹 정보가 필요합니다.");
            }
            
            final String groupId = code.getCodeGroup().getGroupId();
            CodeGroup codeGroup = codeGroupRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("코드 그룹을 찾을 수 없습니다: " + groupId));
            code.setCodeGroup(codeGroup);
            
            // 코드 ID 자동 생성
            String codeId = generateCodeId(groupId);
            code.setCodeId(codeId);
            
            log.info("코드 ID 자동 생성: {}, 그룹: {}", codeId, groupId);
        } else {
            // 코드 ID 중복 체크
            if (codeRepository.existsById(code.getCodeId())) {
                throw new RuntimeException("이미 존재하는 코드 ID입니다: " + code.getCodeId());
            }
            
            // 코드 그룹 확인
            final String groupId = code.getCodeGroup().getGroupId();
            CodeGroup codeGroup = codeGroupRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("코드 그룹을 찾을 수 없습니다: " + groupId));
            code.setCodeGroup(codeGroup);
        }
        
        // 등록자 정보 설정
        code.setCreatedBy(currentUser);
        code.setUpdatedBy(currentUser);
        
        // 코드 저장
        Code savedCode = codeRepository.save(code);
        
        // 속성 저장 (있는 경우)
        if (attributes != null && !attributes.isEmpty()) {
            for (CodeAttribute attr : attributes) {
                attr.setCode(savedCode);
                attr.setCreatedBy(currentUser);
                attr.setUpdatedBy(currentUser);
                codeAttributeRepository.save(attr);
            }
        }
        
        // 이력 저장
        saveCodeHistory(savedCode, ActionType.CREATE, currentUser, null);
        
        log.info("코드 생성: {}, 그룹: {}, 등록자: {}", 
                code.getCodeId(), code.getCodeGroup().getGroupId(), currentUser);
        
        return savedCode;
    }
    
    /**
     * 코드 수정
     */
    @Transactional
    public Code updateCode(String codeId, Code updatedCode, List<CodeAttribute> updatedAttributes, String currentUser) {
        Code existingCode = codeRepository.findById(codeId)
                .orElseThrow(() -> new RuntimeException("코드를 찾을 수 없습니다: " + codeId));
        
        // 변경 전 상태 저장
        Map<String, Object> beforeChanges = new HashMap<>();
        beforeChanges.put("codeName", existingCode.getCodeName());
        beforeChanges.put("active", existingCode.isActive());
        beforeChanges.put("sortOrder", existingCode.getSortOrder());
        beforeChanges.put("description", existingCode.getDescription());
        
        // 코드 업데이트
        existingCode.setCodeName(updatedCode.getCodeName());
        existingCode.setActive(updatedCode.isActive());
        existingCode.setSortOrder(updatedCode.getSortOrder());
        existingCode.setDescription(updatedCode.getDescription());
        existingCode.setUpdatedBy(currentUser);
        
        // 속성 업데이트 (있는 경우)
        if (updatedAttributes != null) {
            // 기존 속성 삭제
            List<CodeAttribute> existingAttrs = codeAttributeRepository.findByCodeCodeId(codeId);
            for (CodeAttribute attr : existingAttrs) {
                codeAttributeRepository.delete(attr);
            }
            
            // 새 속성 추가
            for (CodeAttribute attr : updatedAttributes) {
                attr.setCode(existingCode);
                attr.setCreatedBy(currentUser);
                attr.setUpdatedBy(currentUser);
                codeAttributeRepository.save(attr);
            }
        }
        
        // 변경 후 상태
        Map<String, Object> afterChanges = new HashMap<>();
        afterChanges.put("codeName", updatedCode.getCodeName());
        afterChanges.put("active", updatedCode.isActive());
        afterChanges.put("sortOrder", updatedCode.getSortOrder());
        afterChanges.put("description", updatedCode.getDescription());
        
        // 이력 저장
        Map<String, Object> changes = new HashMap<>();
        changes.put("before", beforeChanges);
        changes.put("after", afterChanges);
        
        saveCodeHistory(existingCode, ActionType.UPDATE, currentUser, changes);
        
        log.info("코드 수정: {}, 수정자: {}", codeId, currentUser);
        
        return codeRepository.save(existingCode);
    }
    
    /**
     * 코드 삭제 (비활성화)
     */
    @Transactional
    public void deleteCode(String codeId, String currentUser) {
        Code code = codeRepository.findById(codeId)
                .orElseThrow(() -> new RuntimeException("코드를 찾을 수 없습니다: " + codeId));
        
        // 실제 삭제 대신 비활성화
        code.setActive(false);
        code.setUpdatedBy(currentUser);
        codeRepository.save(code);
        
        // 이력 저장
        saveCodeHistory(code, ActionType.DELETE, currentUser, null);
        
        log.info("코드 비활성화: {}, 처리자: {}", codeId, currentUser);
    }
    
    //=== 코드 속성 관련 메서드 ===//
    
    /**
     * 코드 속성 조회
     */
    public List<CodeAttribute> getCodeAttributes(String codeId) {
        return codeAttributeRepository.findByCodeCodeId(codeId);
    }
    
    //=== 코드 이력 관련 메서드 ===//
    
    /**
     * 코드 이력 조회
     */
    public List<CodeHistory> getCodeHistory(String codeId) {
        return codeHistoryRepository.findByCodeIdOrderByChangedAtDesc(codeId);
    }
    
    /**
     * 코드 이력 저장
     */
    private void saveCodeHistory(Code code, ActionType actionType, String changedBy, Map<String, Object> changes) {
        CodeHistory history = new CodeHistory();
        history.setCodeId(code.getCodeId());
        history.setCodeName(code.getCodeName());
        history.setGroupId(code.getCodeGroup().getGroupId());
        history.setActionType(actionType);
        history.setChangedBy(changedBy);
        history.setChangedAt(LocalDateTime.now());
        
        if (changes != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                history.setChangeDetails(mapper.writeValueAsString(changes));
            } catch (Exception e) {
                log.error("이력 변경 내용 저장 중 오류 발생", e);
                history.setChangeDetails("변경 내용 저장 실패: " + e.getMessage());
            }
        }
        
        codeHistoryRepository.save(history);
    }
} 