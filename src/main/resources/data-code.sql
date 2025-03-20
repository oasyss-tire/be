-- -- 대분류 그룹
-- INSERT INTO code_group (group_id, group_name, level, active, created_by, created_at, updated_by, updated_at) VALUES
-- ('001', '계약', 1, true, 'system', NOW(), 'system', NOW()),
-- ('002', '시설물', 1, true, 'system', NOW(), 'system', NOW()),
-- ('003', '사용자', 1, true, 'system', NOW(), 'system', NOW()),
-- ('004', '회사', 1, true, 'system', NOW(), 'system', NOW()),
-- ('005', '게시판', 1, true, 'system', NOW(), 'system', NOW());

-- -- 중분류 그룹 (계약 관리 하위)
-- INSERT INTO code_group (group_id, group_name, level, parent_group_id, active, created_by, created_at, updated_by, updated_at) VALUES
-- ('001001', '계약 유형', 2, '001', true, 'system', NOW(), 'system', NOW()),
-- ('001002', '계약 상태', 2, '001', true, 'system', NOW(), 'system', NOW()),
-- ('001003', '서명 유형', 2, '001', true, 'system', NOW(), 'system', NOW());

-- -- 중분류 그룹 (시설물 관리 하위)
-- INSERT INTO code_group (group_id, group_name, level, parent_group_id, active, created_by, created_at, updated_by, updated_at) VALUES
-- ('002001', '시설물 유형', 2, '002', true, 'system', NOW(), 'system', NOW()),
-- ('002002', '시설물 상태', 2, '002', true, 'system', NOW(), 'system', NOW()),
-- ('002003', 'A/S 유형', 2, '002', true, 'system', NOW(), 'system', NOW()),
-- ('002004', 'A/S 상태', 2, '002', true, 'system', NOW(), 'system', NOW());

-- -- 중분류 그룹 (사용자 관리 하위)
-- INSERT INTO code_group (group_id, group_name, level, parent_group_id, active, created_by, created_at, updated_by, updated_at) VALUES
-- ('003001', '사용자 역할', 2, '003', true, 'system', NOW(), 'system', NOW()),
-- ('003002', '사용자 상태', 2, '003', true, 'system', NOW(), 'system', NOW());

-- -- 중분류 그룹 (회사 관리 하위)
-- INSERT INTO code_group (group_id, group_name, level, parent_group_id, active, created_by, created_at, updated_by, updated_at) VALUES
-- ('004001', '회사 유형', 2, '004', true, 'system', NOW(), 'system', NOW()),
-- ('004002', '회사 상태', 2, '004', true, 'system', NOW(), 'system', NOW());

-- -- 중분류 그룹 (게시판 관리 하위)
-- INSERT INTO code_group (group_id, group_name, level, parent_group_id, active, created_by, created_at, updated_by, updated_at) VALUES
-- ('005001', '게시판 유형', 2, '005', true, 'system', NOW(), 'system', NOW()),
-- ('005002', '게시물 상태', 2, '005', true, 'system', NOW(), 'system', NOW());

-- -- 소분류 그룹 (계약 유형 하위)
-- INSERT INTO code_group (group_id, group_name, level, parent_group_id, active, created_by, created_at, updated_by, updated_at) VALUES
-- ('001001001', '위수탁계약 유형', 3, '001001', true, 'system', NOW(), 'system', NOW()),
-- ('001001002', '근로계약 유형', 3, '001001', true, 'system', NOW(), 'system', NOW());

-- -- 코드 데이터 (계약 유형)
-- INSERT INTO code (code_id, code_name, group_id, sort_order, active, created_by, created_at, updated_by, updated_at) VALUES
-- ('CONTRACT_TYPE_100', '위수탁계약', '001001', 1, true, 'system', NOW(), 'system', NOW()),
-- ('CONTRACT_TYPE_200', '근로계약', '001001', 2, true, 'system', NOW(), 'system', NOW()),
-- ('CONTRACT_TYPE_300', '위탁계약', '001001', 3, true, 'system', NOW(), 'system', NOW());

-- -- 코드 데이터 (계약 상태)
-- INSERT INTO code (code_id, code_name, group_id, sort_order, active, created_by, created_at, updated_by, updated_at) VALUES
-- ('CONTRACT_STATUS_DRAFT', '임시저장', '001002', 1, true, 'system', NOW(), 'system', NOW()),
-- ('CONTRACT_STATUS_SIGNING', '서명중', '001002', 2, true, 'system', NOW(), 'system', NOW()),
-- ('CONTRACT_STATUS_COMPLETED', '완료', '001002', 3, true, 'system', NOW(), 'system', NOW());

-- -- 코드 데이터 (서명 유형)
-- INSERT INTO code (code_id, code_name, group_id, sort_order, active, created_by, created_at, updated_by, updated_at) VALUES
-- ('SIGN_TYPE_DIGITAL', '디지털 서명', '001003', 1, true, 'system', NOW(), 'system', NOW()),
-- ('SIGN_TYPE_IMAGE', '이미지 서명', '001003', 2, true, 'system', NOW(), 'system', NOW());

-- -- 코드 데이터 (시설물 유형)
-- INSERT INTO code (code_id, code_name, group_id, sort_order, active, created_by, created_at, updated_by, updated_at) VALUES
-- ('FACILITY_TYPE_BUILDING', '건물', '002001', 1, true, 'system', NOW(), 'system', NOW()),
-- ('FACILITY_TYPE_EQUIPMENT', '장비', '002001', 2, true, 'system', NOW(), 'system', NOW());

-- -- 코드 데이터 (시설물 상태)
-- INSERT INTO code (code_id, code_name, group_id, sort_order, active, created_by, created_at, updated_by, updated_at) VALUES
-- ('FACILITY_STATUS_NORMAL', '정상', '002002', 1, true, 'system', NOW(), 'system', NOW()),
-- ('FACILITY_STATUS_WARNING', '주의', '002002', 2, true, 'system', NOW(), 'system', NOW()),
-- ('FACILITY_STATUS_CRITICAL', '심각', '002002', 3, true, 'system', NOW(), 'system', NOW());

-- -- 코드 데이터 (A/S 유형)
-- INSERT INTO code (code_id, code_name, group_id, sort_order, active, created_by, created_at, updated_by, updated_at) VALUES
-- ('AS_TYPE_REPAIR', '수리', '002003', 1, true, 'system', NOW(), 'system', NOW()),
-- ('AS_TYPE_REPLACE', '교체', '002003', 2, true, 'system', NOW(), 'system', NOW());

-- -- 코드 데이터 (A/S 상태)
-- INSERT INTO code (code_id, code_name, group_id, sort_order, active, created_by, created_at, updated_by, updated_at) VALUES
-- ('AS_STATUS_REQUESTED', '요청됨', '002004', 1, true, 'system', NOW(), 'system', NOW()),
-- ('AS_STATUS_INPROGRESS', '처리중', '002004', 2, true, 'system', NOW(), 'system', NOW()),
-- ('AS_STATUS_COMPLETED', '완료', '002004', 3, true, 'system', NOW(), 'system', NOW());

-- -- 코드 데이터 (사용자 역할)
-- INSERT INTO code (code_id, code_name, group_id, sort_order, active, created_by, created_at, updated_by, updated_at) VALUES
-- ('USER_ROLE_ADMIN', '관리자', '003001', 1, true, 'system', NOW(), 'system', NOW()),
-- ('USER_ROLE_MANAGER', '매니저', '003001', 2, true, 'system', NOW(), 'system', NOW()),
-- ('USER_ROLE_USER', '일반 사용자', '003001', 3, true, 'system', NOW(), 'system', NOW());

-- -- 코드 데이터 (사용자 상태)
-- INSERT INTO code (code_id, code_name, group_id, sort_order, active, created_by, created_at, updated_by, updated_at) VALUES
-- ('USER_STATUS_ACTIVE', '활성', '003002', 1, true, 'system', NOW(), 'system', NOW()),
-- ('USER_STATUS_INACTIVE', '비활성', '003002', 2, true, 'system', NOW(), 'system', NOW()),
-- ('USER_STATUS_BLOCKED', '차단됨', '003002', 3, true, 'system', NOW(), 'system', NOW());

-- -- 코드 데이터 (회사 유형)
-- INSERT INTO code (code_id, code_name, group_id, sort_order, active, created_by, created_at, updated_by, updated_at) VALUES
-- ('COMPANY_TYPE_MAIN', '본사', '004001', 1, true, 'system', NOW(), 'system', NOW()),
-- ('COMPANY_TYPE_BRANCH', '지점', '004001', 2, true, 'system', NOW(), 'system', NOW()),
-- ('COMPANY_TYPE_PARTNER', '협력사', '004001', 3, true, 'system', NOW(), 'system', NOW());

-- -- 코드 데이터 (회사 상태)
-- INSERT INTO code (code_id, code_name, group_id, sort_order, active, created_by, created_at, updated_by, updated_at) VALUES
-- ('COMPANY_STATUS_ACTIVE', '활성', '004002', 1, true, 'system', NOW(), 'system', NOW()),
-- ('COMPANY_STATUS_INACTIVE', '비활성', '004002', 2, true, 'system', NOW(), 'system', NOW());

-- -- 코드 데이터 (게시판 유형)
-- INSERT INTO code (code_id, code_name, group_id, sort_order, active, created_by, created_at, updated_by, updated_at) VALUES
-- ('BOARD_TYPE_NOTICE', '공지사항', '005001', 1, true, 'system', NOW(), 'system', NOW()),
-- ('BOARD_TYPE_QNA', '질문과 답변', '005001', 2, true, 'system', NOW(), 'system', NOW()),
-- ('BOARD_TYPE_FAQ', '자주 묻는 질문', '005001', 3, true, 'system', NOW(), 'system', NOW());

-- -- 코드 데이터 (게시물 상태)
-- INSERT INTO code (code_id, code_name, group_id, sort_order, active, created_by, created_at, updated_by, updated_at) VALUES
-- ('POST_STATUS_DRAFT', '임시저장', '005002', 1, true, 'system', NOW(), 'system', NOW()),
-- ('POST_STATUS_PUBLISHED', '게시됨', '005002', 2, true, 'system', NOW(), 'system', NOW()),
-- ('POST_STATUS_HIDDEN', '숨김', '005002', 3, true, 'system', NOW(), 'system', NOW());

-- -- 코드 속성 예시 (색상 속성)
-- INSERT INTO code_attribute (code_id, attribute_key, attribute_value, created_by, created_at, updated_by, updated_at) VALUES
-- ('FACILITY_STATUS_NORMAL', 'color', 'green', 'system', NOW(), 'system', NOW()),
-- ('FACILITY_STATUS_WARNING', 'color', 'yellow', 'system', NOW(), 'system', NOW()),
-- ('FACILITY_STATUS_CRITICAL', 'color', 'red', 'system', NOW(), 'system', NOW());

-- -- 코드 속성 예시 (아이콘 속성)
-- INSERT INTO code_attribute (code_id, attribute_key, attribute_value, created_by, created_at, updated_by, updated_at) VALUES
-- ('CONTRACT_TYPE_100', 'icon', 'file-contract', 'system', NOW(), 'system', NOW()),
-- ('CONTRACT_TYPE_200', 'icon', 'file-signature', 'system', NOW(), 'system', NOW()),
-- ('CONTRACT_TYPE_300', 'icon', 'file-alt', 'system', NOW(), 'system', NOW()); 