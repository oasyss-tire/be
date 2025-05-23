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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import com.inspection.dto.CompanyDTO;
import com.inspection.dto.CreateCompanyRequest;
import com.inspection.dto.UserResponseDTO;
import com.inspection.dto.CompanyBatchRequest;
import com.inspection.dto.BatchResponseDTO;
import com.inspection.dto.PageResponseDTO;
import com.inspection.service.CompanyService;
import com.inspection.service.CompanyImageStorageService;
import com.inspection.entity.Code;
import com.inspection.service.CodeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Slf4j
public class CompanyController {
    
    private final CompanyService companyService;
    private final CompanyImageStorageService companyImageStorageService;
    private final CodeService codeService;
    
    // 회사 생성 API (JSON 방식)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createCompany(@RequestBody CreateCompanyRequest request) {
        
        try {
            // 현재 인증된 사용자 정보 가져오기
            String userId = getCurrentUserId();
            if (StringUtils.hasText(userId)) {
                request.setCreatedBy(userId);
            }
            
            // 회사 생성 (내부적으로 사용자 계정도 함께 생성)
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

        
        try {
            // 현재 인증된 사용자 정보 가져오기
            String userId = getCurrentUserId();
            if (StringUtils.hasText(userId)) {
                request.setCreatedBy(userId);
            }
            
            // 1. 회사 생성 (내부적으로 사용자 계정도 함께 생성)
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

        CompanyDTO updatedCompany = companyService.updateCompanyImages(
                companyId, frontImage, backImage, leftSideImage, rightSideImage, fullImage);
        return ResponseEntity.ok(updatedCompany);
    }
    
    // 특정 이미지 삭제 API
    @DeleteMapping("/{companyId}/images/{imageType}")
    public ResponseEntity<CompanyDTO> deleteCompanyImage(
            @PathVariable Long companyId,
            @PathVariable String imageType) {

        CompanyDTO updatedCompany = companyService.deleteCompanyImage(companyId, imageType);
        return ResponseEntity.ok(updatedCompany);
    }

    // 회사 목록 조회 API (페이징 지원)
    @GetMapping
    public ResponseEntity<?> getAllCompanies(
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "sort", required = false) String sort) {
        
        // 페이징 파라미터가 없는 경우 기존 로직 실행 (하위 호환성 유지)
        if (page == null && size == null && sort == null) {
            List<CompanyDTO> companies;
            if (active != null && active) {
                companies = companyService.getActiveCompanies();
            } else {
                companies = companyService.getAllCompanies();
            }
            return ResponseEntity.ok(companies);
        }
        
        // 페이징 파라미터가 있는 경우 페이징 처리
        int pageNum = (page != null) ? page : 0;
        int sizeNum = (size != null) ? size : 10;
        
        // 정렬 설정
        Pageable pageable;
        if (sort != null && !sort.isEmpty()) {
            String[] sortParams = sort.split(",");
            String sortField = sortParams[0];
            Direction direction = (sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")) 
                    ? Direction.ASC : Direction.DESC;
            pageable = PageRequest.of(pageNum, sizeNum, Sort.by(direction, sortField));
        } else {
            pageable = PageRequest.of(pageNum, sizeNum, Sort.by(Direction.DESC, "id"));
        }
        
        // 페이징된 데이터 조회
        Page<CompanyDTO> companyPage;
        if (active != null && active) {
            companyPage = companyService.getActiveCompaniesWithPaging(pageable);
        } else {
            companyPage = companyService.getAllCompaniesWithPaging(pageable);
        }
        
        return ResponseEntity.ok(PageResponseDTO.fromPage(companyPage));
    }
    
    // 페이징 처리된 회사 목록 조회 API (별도 엔드포인트)
    @GetMapping("/page")
    public ResponseEntity<PageResponseDTO<CompanyDTO>> getCompaniesWithPaging(
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "id") String sort,
            @RequestParam(value = "direction", defaultValue = "DESC") String direction) {
        
        Sort.Direction sortDirection = direction.equalsIgnoreCase("ASC") ? Direction.ASC : Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        Page<CompanyDTO> companyPage;
        if (active != null && active) {
            companyPage = companyService.getActiveCompaniesWithPaging(pageable);
        } else {
            companyPage = companyService.getAllCompaniesWithPaging(pageable);
        }
        
        return ResponseEntity.ok(PageResponseDTO.fromPage(companyPage));
    }
    
    // 회사 ID로 회사 정보 조회 API
    @GetMapping("/{companyId}")
    public ResponseEntity<CompanyDTO> getCompanyById(@PathVariable Long companyId) {
        CompanyDTO company = companyService.getCompanyById(companyId);
        return ResponseEntity.ok(company);
    }
    

    // 회사 정보 수정 API (JSON 방식)
    @PutMapping(value = "/{companyId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateCompany(
            @PathVariable Long companyId,
            @RequestBody CreateCompanyRequest request) {
        
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
        companyService.deleteCompany(companyId);
        return ResponseEntity.ok(Map.of("message", "회사가 성공적으로 삭제되었습니다."));
    }
    

    // 회사 상태 변경 API
    @PutMapping("/{companyId}/toggle-status")
    public ResponseEntity<CompanyDTO> toggleCompanyStatus(@PathVariable Long companyId) {
        CompanyDTO updatedCompany = companyService.toggleCompanyStatus(companyId);
        return ResponseEntity.ok(updatedCompany);
    }
    

    // 매장명으로 회사 검색 API (페이징 지원)
    @GetMapping("/search")
    public ResponseEntity<?> searchCompaniesByName(
            @RequestParam("name") String storeName,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "sort", required = false) String sort) {

        // 페이징 파라미터가 없는 경우 기존 로직 실행 (하위 호환성 유지)
        if (page == null && size == null && sort == null) {
            List<CompanyDTO> companies = companyService.searchCompaniesByName(storeName);
            return ResponseEntity.ok(companies);
        }
        
        // 페이징 파라미터가 있는 경우 페이징 처리
        int pageNum = (page != null) ? page : 0;
        int sizeNum = (size != null) ? size : 10;
        
        // 정렬 설정
        Pageable pageable;
        if (sort != null && !sort.isEmpty()) {
            String[] sortParams = sort.split(",");
            String sortField = sortParams[0];
            Direction direction = (sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")) 
                    ? Direction.ASC : Direction.DESC;
            pageable = PageRequest.of(pageNum, sizeNum, Sort.by(direction, sortField));
        } else {
            pageable = PageRequest.of(pageNum, sizeNum, Sort.by(Direction.DESC, "id"));
        }
        
        Page<CompanyDTO> companyPage = companyService.searchCompaniesByNameWithPaging(storeName, pageable);
        return ResponseEntity.ok(PageResponseDTO.fromPage(companyPage));
    }

    // 페이징 처리된 회사 검색 API (별도 엔드포인트)
    @GetMapping("/search/page")
    public ResponseEntity<PageResponseDTO<CompanyDTO>> searchCompaniesByNameWithPaging(
            @RequestParam("name") String storeName,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "id") String sort,
            @RequestParam(value = "direction", defaultValue = "DESC") String direction) {
        
        Sort.Direction sortDirection = direction.equalsIgnoreCase("ASC") ? Direction.ASC : Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        Page<CompanyDTO> companyPage = companyService.searchCompaniesByNameWithPaging(storeName, pageable);
        
        return ResponseEntity.ok(PageResponseDTO.fromPage(companyPage));
    }
    
    // 키워드로 회사 검색 API (통합 검색)
    @GetMapping("/search/keyword")
    public ResponseEntity<PageResponseDTO<CompanyDTO>> searchCompaniesByKeyword(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "id") String sort,
            @RequestParam(value = "direction", required = false) String direction) {
        
        // 정렬 설정
        Pageable pageable;
        if (direction == null) {
            // sort 파라미터에 방향이 포함된 경우 (예: "id,desc")
            if (sort.contains(",")) {
                String[] sortParams = sort.split(",");
                String sortField = sortParams[0];
                Direction sortDirection = (sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")) 
                        ? Direction.ASC : Direction.DESC;
                pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));
            } else {
                // 기본 내림차순 정렬
                pageable = PageRequest.of(page, size, Sort.by(Direction.DESC, sort));
            }
        } else {
            // sort와 direction이 별도 파라미터로 제공된 경우
            Direction sortDirection = direction.equalsIgnoreCase("ASC") ? Direction.ASC : Direction.DESC;
            pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        }
        
        Page<CompanyDTO> companyPage = companyService.searchCompaniesByKeywordWithPaging(keyword, pageable);
        
        return ResponseEntity.ok(PageResponseDTO.fromPage(companyPage));
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

    /**
     * 회사 등록과 동시에 수탁사업자 계정을 자동으로 생성하는 API
     */
    @PostMapping("/with-user")
    public ResponseEntity<?> createCompanyWithUser(@RequestBody CreateCompanyRequest request) {
        try {
            // 현재 인증된 사용자 정보 가져오기
            String userId = getCurrentUserId();
            if (StringUtils.hasText(userId)) {
                request.setCreatedBy(userId);
            }
            
            // 회사 생성 (내부적으로 사용자 계정도 함께 생성)
            CompanyDTO createdCompany = companyService.createCompany(request);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCompany);
        } catch (ResponseStatusException e) {
            // ResponseStatusException 처리
            String cleanMessage = e.getReason() != null ? e.getReason() : "회사 및 사용자 생성 중 오류가 발생했습니다";
            log.error("회사 및 사용자 생성 중 오류 발생: {}", cleanMessage);
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", "입력 오류", "message", cleanMessage));
        } catch (IllegalArgumentException e) {
            // IllegalArgumentException 처리
            log.error("회사 및 사용자 생성 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "입력 오류", "message", e.getMessage()));
        } catch (Exception e) {
            // 기타 서버 오류 처리
            log.error("회사 및 사용자 생성 중 서버 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "서버 오류", "message", "회사 및 사용자 생성 중 오류가 발생했습니다"));
        }
    }

    // 회사 일괄 등록 API
    @PostMapping("/batch")
    public ResponseEntity<?> createCompanyBatch(@RequestBody CompanyBatchRequest batchRequest) {
        try {
            // 현재 인증된 사용자 정보 가져오기
            String userId = getCurrentUserId();
            
            // 사용자 정보 설정
            if (StringUtils.hasText(userId)) {
                for (CreateCompanyRequest request : batchRequest.getCompanies()) {
                    request.setCreatedBy(userId);
                }
            }
            
            // 배치 처리 실행
            BatchResponseDTO result = companyService.createCompanyBatch(batchRequest.getCompanies());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            log.error("회사 일괄 등록 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "서버 오류", "message", "회사 일괄 등록 중 오류가 발생했습니다."));
        }
    }

    /**
     * 지부 그룹 코드 목록 조회 API
     * 회사 등록/수정 시 지부 그룹 선택에 사용됨
     */
    @GetMapping("/branch-groups")
    public ResponseEntity<?> getBranchGroups() {
        try {
            // 003002 코드그룹은 지부 그룹을 의미함
            List<Code> branchGroups = codeService.getActiveCodesByGroupId("003002");
            return ResponseEntity.ok(branchGroups);
        } catch (Exception e) {
            log.error("지부 그룹 코드 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "서버 오류", "message", "지부 그룹 코드 조회 중 오류가 발생했습니다."));
        }
    }
} 