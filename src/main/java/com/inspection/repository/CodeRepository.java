package com.inspection.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.inspection.entity.Code;
import com.inspection.entity.CodeGroup;

public interface CodeRepository extends JpaRepository<Code, String> {
    // 그룹 ID로 코드 조회
    List<Code> findByCodeGroupGroupId(String groupId);
    
    // 그룹 ID로 활성화된 코드만 조회
    List<Code> findByCodeGroupGroupIdAndActiveTrue(String groupId);
    
    // 코드 ID로 활성화된 코드 조회
    Optional<Code> findByCodeIdAndActiveTrue(String codeId);
    
    // 자동생성 관련 메서드
    // 특정 그룹 내에서 코드 ID가 가장 큰 코드 조회
    Optional<Code> findTopByCodeGroupGroupIdOrderByCodeIdDesc(String groupId);

    // 그룹 ID로 시작하는 모든 코드 조회 (수정)
    List<Code> findByCodeGroupGroupIdStartingWith(String groupIdPrefix);
    
    // 설명으로 코드 조회 (수정)
    Code findByDescription(String description);
    
    // 부모 그룹 ID로 코드 조회
    @Query("SELECT c FROM Code c WHERE c.codeGroup.groupId = :parentGroupId")
    List<Code> findByParentGroupId(@Param("parentGroupId") String parentGroupId);
    
    // 역할 ID로 접근 가능한 메뉴 ID 목록 조회
    @Query(value = "SELECT menu_id FROM role_menu_permissions WHERE role_id = :roleId", nativeQuery = true)
    List<String> findMenuIdsByRoleId(@Param("roleId") String roleId);
    
    // 역할에 연결된 모든 메뉴 권한 삭제
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM role_menu_permissions WHERE role_id = :roleId", nativeQuery = true)
    void deleteRoleMenuPermissions(@Param("roleId") String roleId);
    
    // 역할에 메뉴 권한 추가
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO role_menu_permissions (role_id, menu_id, created_at, updated_at, created_by) VALUES (:roleId, :menuId, NOW(), NOW(), 'system')", nativeQuery = true)
    void addRoleMenuPermission(@Param("roleId") String roleId, @Param("menuId") String menuId);
    
    // 모든 역할 코드 조회
    @Query("SELECT c FROM Code c WHERE c.codeGroup.groupId = '008001'")
    List<Code> findAllRoles();
    
    // 코드 ID 목록으로 코드 조회
    List<Code> findByCodeIdIn(List<String> codeIds);
    
    // 역할 코드 ID로 접근 가능한 메뉴 코드 ID 목록 조회
    @Query(value = "SELECT menu_id FROM role_menu_permissions WHERE role_id = :roleId", nativeQuery = true)
    List<String> findMenuCodeIdsByRoleCodeId(@Param("roleId") String roleId);
    
    // 코드 ID가 존재하는지 확인
    boolean existsByCodeId(String codeId);
    
    // 그룹 ID로 코드 조회 (간소화된 버전)
    @Query("SELECT c FROM Code c WHERE c.codeGroup.groupId = :groupId")
    List<Code> findByGroupId(@Param("groupId") String groupId);
    
    // 그룹 ID로 CodeGroup 엔티티 조회
    @Query("SELECT cg FROM CodeGroup cg WHERE cg.groupId = :groupId")
    Optional<CodeGroup> findCodeGroupByGroupId(@Param("groupId") String groupId);
    
    // 다수의 메뉴 권한을 한번에 추가
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO role_menu_permissions (role_id, menu_id, created_at, updated_at, created_by) " +
           "VALUES (:roleId, :menuId, NOW(), NOW(), 'system')", nativeQuery = true)
    void addRoleMenuPermissions(@Param("roleId") String roleId, @Param("menuId") List<String> menuIds);
} 