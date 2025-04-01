package com.inspection.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.inspection.dto.ContractDTO;
import com.inspection.service.MyPageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
@Slf4j
public class MyPageController {
    
    private final MyPageService myPageService;
    
    /**
     * 로그인한 사용자가 참여한 계약 목록 조회
     * 
     * @param userDetails 현재 인증된 사용자 정보
     * @return 사용자가 참여한 계약 목록
     */
    @GetMapping("/contracts")
    public ResponseEntity<List<ContractDTO>> getMyContracts(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        if (userDetails == null) {
            return ResponseEntity.badRequest().build();
        }
        
        log.info("사용자 계약 목록 조회 요청: {}", userDetails.getUsername());
        
        List<ContractDTO> contracts = myPageService.getMyContracts(userDetails.getUsername());
        return ResponseEntity.ok(contracts);
    }
} 