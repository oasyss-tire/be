package com.inspection.controller;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.inspection.dto.CompanyDTO;
import com.inspection.dto.CreateCompanyRequest;
import com.inspection.dto.UserResponseDTO;
import com.inspection.service.CompanyService;
import com.inspection.service.CompanyImageStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Slf4j
public class CompanyController {
    
    private final CompanyService companyService;
    private final CompanyImageStorageService companyImageStorageService;
    
    // 회사 생성 API (JSON 방식)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createCompany(@RequestBody CreateCompanyRequest request) {
        log.info("회사 생성 요청: {}", request.getStoreName());
        
        try {
            // 현재 인증된 사용자 정보 가져오기
            String userId = getCurrentUserId();
            if (StringUtils.hasText(userId)) {
                request.setCreatedBy(userId);
            }
            
            CompanyDTO createdCompany = companyService.createCompany(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCompany);
        } catch (ResponseStatusException e) {
            // ResponseStatusException의 메시지에서 불필요한 부분 제거
            String cleanMessage = e.getReason() != null ? e.getReason() : "회사 생성 중 오류가 발생했습니다";
            log.error("회사 생성 중 오류 발생: {}", cleanMessage);
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", "입력 오류", "message", cleanMessage));
        } catch (IllegalArgumentException e) {
            // IllegalArgumentException 처리
            log.error("회사 생성 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "입력 오류", "message", e.getMessage()));
        } catch (Exception e) {
            // 기타 서버 오류 처리
            log.error("회사 생성 중 서버 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "서버 오류", "message", "회사 생성 중 오류가 발생했습니다"));
        }
    }
    
    // 회사 생성 API (이미지 포함)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createCompanyWithImages(
            @RequestPart("company") CreateCompanyRequest request,
            @RequestPart(value = "frontImage", required = false) MultipartFile frontImage,
            @RequestPart(value = "backImage", required = false) MultipartFile backImage,
            @RequestPart(value = "leftSideImage", required = false) MultipartFile leftSideImage,
            @RequestPart(value = "rightSideImage", required = false) MultipartFile rightSideImage,
            @RequestPart(value = "fullImage", required = false) MultipartFile fullImage) {
        
        log.info("회사 생성 요청 (이미지 포함): {}", request.getStoreName());
        
        try {
            // 현재 인증된 사용자 정보 가져오기
            String userId = getCurrentUserId();
            if (StringUtils.hasText(userId)) {
                request.setCreatedBy(userId);
            }
            
            // 1. 회사 생성
            CompanyDTO createdCompany = companyService.createCompany(request);
            
            // 2. 이미지가 하나라도 있으면 이미지 업로드
            if (frontImage != null || backImage != null || leftSideImage != null || 
                rightSideImage != null || fullImage != null) {
                
                createdCompany = companyService.uploadCompanyImages(
                    createdCompany.getId(), frontImage, backImage, leftSideImage, rightSideImage, fullImage);
            }
            
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCompany);
        } catch (ResponseStatusException e) {
            // ResponseStatusException의 메시지에서 불필요한 부분 제거
            String cleanMessage = e.getReason() != null ? e.getReason() : "회사 생성 중 오류가 발생했습니다";
            log.error("회사 생성 중 오류 발생 (이미지 포함): {}", cleanMessage);
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", "입력 오류", "message", cleanMessage));
        } catch (IllegalArgumentException e) {
            // IllegalArgumentException 처리
            log.error("회사 생성 중 오류 발생 (이미지 포함): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "입력 오류", "message", e.getMessage()));
        } catch (Exception e) {
            // 기타 서버 오류 처리
            log.error("회사 생성 중 서버 오류 발생 (이미지 포함): {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "서버 오류", "message", "회사 생성 중 오류가 발생했습니다"));
        }
    }
    
    /**
     * 현재 인증된 사용자의 ID를 가져옵니다.
     * JWT 토큰에서 사용자 정보를 추출합니다.
     * 
     * @return 현재 인증된 사용자 ID
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName(); // JWT에서 추출된 사용자 ID (일반적으로 username 또는 sub 클레임)
        }
        return null;
    }
    
    // 회사 이미지 업로드 API
    @PostMapping(value = "/{companyId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CompanyDTO> uploadCompanyImages(
            @PathVariable Long companyId,
            @RequestPart(value = "frontImage", required = false) MultipartFile frontImage,
            @RequestPart(value = "backImage", required = false) MultipartFile backImage,
            @RequestPart(value = "leftSideImage", required = false) MultipartFile leftSideImage,
            @RequestPart(value = "rightSideImage", required = false) MultipartFile rightSideImage,
            @RequestPart(value = "fullImage", required = false) MultipartFile fullImage) {
        
        log.info("회사 이미지 업로드 요청: 회사 ID {}", companyId);
        CompanyDTO updatedCompany = companyService.uploadCompanyImages(
                companyId, frontImage, backImage, leftSideImage, rightSideImage, fullImage);
        return ResponseEntity.ok(updatedCompany);
    }
    
    // 회사 이미지 수정 API
    @PutMapping(value = "/{companyId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CompanyDTO> updateCompanyImages(
            @PathVariable Long companyId,
            @RequestPart(value = "frontImage", required = false) MultipartFile frontImage,
            @RequestPart(value = "backImage", required = false) MultipartFile backImage,
            @RequestPart(value = "leftSideImage", required = false) MultipartFile leftSideImage,
            @RequestPart(value = "rightSideImage", required = false) MultipartFile rightSideImage,
            @RequestPart(value = "fullImage", required = false) MultipartFile fullImage) {
        
        log.info("회사 이미지 수정 요청: 회사 ID {}", companyId);
        CompanyDTO updatedCompany = companyService.updateCompanyImages(
                companyId, frontImage, backImage, leftSideImage, rightSideImage, fullImage);
        return ResponseEntity.ok(updatedCompany);
    }
    
    // 특정 이미지 삭제 API
    @DeleteMapping("/{companyId}/images/{imageType}")
    public ResponseEntity<CompanyDTO> deleteCompanyImage(
            @PathVariable Long companyId,
            @PathVariable String imageType) {
        
        log.info("회사 이미지 삭제 요청: 회사 ID {}, 이미지 타입 {}", companyId, imageType);
        CompanyDTO updatedCompany = companyService.deleteCompanyImage(companyId, imageType);
        return ResponseEntity.ok(updatedCompany);
    }

    // 모든 회사 목록 조회 API
    @GetMapping
    public ResponseEntity<List<CompanyDTO>> getAllCompanies(
            @RequestParam(value = "active", required = false) Boolean active) {
        
        List<CompanyDTO> companies;
        if (active != null && active) {
            log.info("활성화된 회사 목록 조회 요청");
            companies = companyService.getActiveCompanies();
        } else {
            log.info("모든 회사 목록 조회 요청");
            companies = companyService.getAllCompanies();
        }
        
        return ResponseEntity.ok(companies);
    }
    

    // 회사 ID로 회사 정보 조회 API
    @GetMapping("/{companyId}")
    public ResponseEntity<CompanyDTO> getCompanyById(@PathVariable Long companyId) {
        log.info("회사 정보 조회 요청: 회사 ID {}", companyId);
        CompanyDTO company = companyService.getCompanyById(companyId);
        return ResponseEntity.ok(company);
    }
    

    // 회사 정보 수정 API (JSON 방식)
    @PutMapping(value = "/{companyId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateCompany(
            @PathVariable Long companyId,
            @RequestBody CreateCompanyRequest request) {
        
        log.info("회사 정보 수정 요청: 회사 ID {}", companyId);
        
        try {
            CompanyDTO updatedCompany = companyService.updateCompany(companyId, request);
            return ResponseEntity.ok(updatedCompany);
        } catch (ResponseStatusException e) {
            // ResponseStatusException의 메시지에서 불필요한 부분 제거
            String cleanMessage = e.getReason() != null ? e.getReason() : "회사 정보 수정 중 오류가 발생했습니다";
            log.error("회사 정보 수정 중 오류 발생: {}", cleanMessage);
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", "입력 오류", "message", cleanMessage));
        } catch (IllegalArgumentException e) {
            // IllegalArgumentException 처리
            log.error("회사 정보 수정 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "입력 오류", "message", e.getMessage()));
        } catch (Exception e) {
            // 기타 서버 오류 처리
            log.error("회사 정보 수정 중 서버 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "서버 오류", "message", "회사 정보 수정 중 오류가 발생했습니다"));
        }
    }
    
    // 회사 정보와 이미지 함께 수정 API
    @PutMapping(value = "/{companyId}/with-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateCompanyWithImages(
            @PathVariable Long companyId,
            @RequestPart("company") CreateCompanyRequest request,
            @RequestPart(value = "frontImage", required = false) MultipartFile frontImage,
            @RequestPart(value = "backImage", required = false) MultipartFile backImage,
            @RequestPart(value = "leftSideImage", required = false) MultipartFile leftSideImage,
            @RequestPart(value = "rightSideImage", required = false) MultipartFile rightSideImage,
            @RequestPart(value = "fullImage", required = false) MultipartFile fullImage) {
        
        log.info("회사 정보 및 이미지 수정 요청: 회사 ID {}", companyId);
        
        try {
            // 1. 회사 정보 수정
            CompanyDTO updatedCompany = companyService.updateCompany(companyId, request);
            
            // 2. 이미지가 하나라도 있으면 이미지 수정
            if (frontImage != null || backImage != null || leftSideImage != null || 
                rightSideImage != null || fullImage != null) {
                
                updatedCompany = companyService.updateCompanyImages(
                    companyId, frontImage, backImage, leftSideImage, rightSideImage, fullImage);
            }
            
            return ResponseEntity.ok(updatedCompany);
        } catch (ResponseStatusException e) {
            // ResponseStatusException의 메시지에서 불필요한 부분 제거
            String cleanMessage = e.getReason() != null ? e.getReason() : "회사 정보 수정 중 오류가 발생했습니다";
            log.error("회사 정보 및 이미지 수정 중 오류 발생: {}", cleanMessage);
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", "입력 오류", "message", cleanMessage));
        } catch (IllegalArgumentException e) {
            // IllegalArgumentException 처리
            log.error("회사 정보 및 이미지 수정 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "입력 오류", "message", e.getMessage()));
        } catch (Exception e) {
            // 기타 서버 오류 처리
            log.error("회사 정보 및 이미지 수정 중 서버 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "서버 오류", "message", "회사 정보 수정 중 오류가 발생했습니다"));
        }
    }
    
    // 회사 삭제 API
    @DeleteMapping("/{companyId}")
    public ResponseEntity<Map<String, String>> deleteCompany(@PathVariable Long companyId) {
        log.info("회사 삭제 요청: 회사 ID {}", companyId);
        companyService.deleteCompany(companyId);
        return ResponseEntity.ok(Map.of("message", "회사가 성공적으로 삭제되었습니다."));
    }
    

    // 회사 상태 변경 API
    @PutMapping("/{companyId}/toggle-status")
    public ResponseEntity<CompanyDTO> toggleCompanyStatus(@PathVariable Long companyId) {
        log.info("회사 상태 변경 요청: 회사 ID {}", companyId);
        CompanyDTO updatedCompany = companyService.toggleCompanyStatus(companyId);
        return ResponseEntity.ok(updatedCompany);
    }
    

    // 매장명으로 회사 검색 API
    @GetMapping("/search")
    public ResponseEntity<List<CompanyDTO>> searchCompaniesByName(
            @RequestParam("name") String storeName) {
        
        log.info("회사 검색 요청: 매장명 {}", storeName);
        List<CompanyDTO> companies = companyService.searchCompaniesByName(storeName);
        return ResponseEntity.ok(companies);
    }

    // 이미지 파일 조회 API
    @GetMapping("/images/**")
    public ResponseEntity<Resource> getImage(HttpServletRequest request) {
        try {
            // 전체 경로에서 /api/companies/images/ 부분을 제외한 나머지를 파일 경로로 사용
            String requestURL = request.getRequestURL().toString();
            String contextPath = request.getContextPath();
            String baseUrl = contextPath + "/api/companies/images/";
            
            // URL에서 실제 파일 경로 추출
            String filePath = requestURL.substring(requestURL.indexOf(baseUrl) + baseUrl.length());
            
            // URL 디코딩 (한글 파일명 처리)
            filePath = URLDecoder.decode(filePath, StandardCharsets.UTF_8.name());
            
            log.info("이미지 파일 요청 경로: {}", filePath);
            
            // 이미지 파일을 Resource로 로드
            Resource resource = companyImageStorageService.loadImageAsResource(filePath);
            
            // 파일의 Content-Type 확인
            String contentType = Files.probeContentType(resource.getFile().toPath());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
                
        } catch (IOException e) {
            log.error("이미지 파일 로드 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 회사에 소속된 사용자 목록 조회
     */
    @GetMapping("/{companyId}/users")
    public ResponseEntity<List<UserResponseDTO>> getCompanyUsers(@PathVariable Long companyId) {
        try {
            List<UserResponseDTO> users = companyService.getUsersByCompanyId(companyId);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("회사 사용자 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
} 