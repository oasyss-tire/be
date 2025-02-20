package com.inspection.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignaturePositionRequest {
    private int pageNumber;
    private float x;
    private float y;
    private float width;
    private float height;
    private float normX;    // 정규화된 X 좌표 (0~1)
    private float normY;    // 정규화된 Y 좌표 (0~1)
    private float displayX; // 화면 표시용 X 좌표
    private float displayY; // 화면 표시용 Y 좌표
} 