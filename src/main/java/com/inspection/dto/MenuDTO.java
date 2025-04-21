package com.inspection.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MenuDTO {
    private String id;        // 메뉴 코드 ID
    private String name;      // 메뉴 이름
    private String path;      // 메뉴 경로 (React 라우트)
    private String category;  // 메뉴 카테고리
    private String icon;      // 메뉴 아이콘
    private int displayOrder; // 표시 순서
    private boolean visible = true; // 메뉴에 표시 여부
}