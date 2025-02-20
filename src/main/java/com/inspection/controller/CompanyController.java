package com.inspection.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspection.dto.CompanyDTO;
import com.inspection.dto.EmployeeDTO;
import com.inspection.entity.Company;
import com.inspection.entity.Employee;
import com.inspection.service.CompanyService;
import com.inspection.util.FileUploadUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Slf4j
public class CompanyController {
    private final CompanyService companyService;
    private final ObjectMapper objectMapper;
    private final FileUploadUtil fileUploadUtil;


    @GetMapping
    public ResponseEntity<List<CompanyDTO>> getAllCompanies() {
        return ResponseEntity.ok(companyService.getAllCompanies());
    }

    @GetMapping("/{companyId}")
    public ResponseEntity<CompanyDTO> getCompanyById(@PathVariable Long companyId) {
        CompanyDTO companyDTO = companyService.getCompanyDTOById(companyId);
        return ResponseEntity.ok(companyDTO);
    }

    @PostMapping
    public ResponseEntity<CompanyDTO> createCompany(
            @RequestParam("company") String companyDtoString,
            @RequestParam(value = "businessLicenseImage", required = false) MultipartFile businessLicenseImage,
            @RequestParam(value = "exteriorImage", required = false) MultipartFile exteriorImage,
            @RequestParam(value = "entranceImage", required = false) MultipartFile entranceImage,
            @RequestParam(value = "mainPanelImage", required = false) MultipartFile mainPanelImage,
            @RequestParam(value = "etcImage1", required = false) MultipartFile etcImage1,
            @RequestParam(value = "etcImage2", required = false) MultipartFile etcImage2,
            @RequestParam(value = "etcImage3", required = false) MultipartFile etcImage3,
            @RequestParam(value = "etcImage4", required = false) MultipartFile etcImage4) {
        try {
            CompanyDTO companyDTO = objectMapper.readValue(companyDtoString, CompanyDTO.class);
            
            // 회사와 직원 정보를 한 번에 저장
            CompanyDTO savedCompany = companyService.createCompany(companyDTO, 
                businessLicenseImage, exteriorImage, entranceImage, mainPanelImage,
                etcImage1, etcImage2, etcImage3, etcImage4);

            return ResponseEntity.ok(savedCompany);
        } catch (Exception e) {
            log.error("회사 생성 실패: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{companyId}")
    public ResponseEntity<CompanyDTO> updateCompany(
            @PathVariable Long companyId,
            @RequestParam("company") String companyDtoString,
            @RequestParam(value = "businessLicenseImage", required = false) MultipartFile businessLicenseImage,
            @RequestParam(value = "exteriorImage", required = false) MultipartFile exteriorImage,
            @RequestParam(value = "entranceImage", required = false) MultipartFile entranceImage,
            @RequestParam(value = "mainPanelImage", required = false) MultipartFile mainPanelImage,
            @RequestParam(value = "etcImage1", required = false) MultipartFile etcImage1,
            @RequestParam(value = "etcImage2", required = false) MultipartFile etcImage2,
            @RequestParam(value = "etcImage3", required = false) MultipartFile etcImage3,
            @RequestParam(value = "etcImage4", required = false) MultipartFile etcImage4) {
        try {
            CompanyDTO companyDTO = objectMapper.readValue(companyDtoString, CompanyDTO.class);
            
            return ResponseEntity.ok(companyService.updateCompany(companyId, companyDTO,
                businessLicenseImage, exteriorImage, entranceImage, mainPanelImage,
                etcImage1, etcImage2, etcImage3, etcImage4));
        } catch (Exception e) {
            log.error("회사 수정 실패: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{companyId}")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long companyId) {
        companyService.deleteCompany(companyId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{companyId}/business-license")
    public ResponseEntity<?> getBusinessLicenseFile(@PathVariable Long companyId) {
        try {
            Company company = companyService.getCompanyById(companyId);
            String fileName = company.getBusinessLicenseImage();
            
            if (fileName == null || fileName.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // 파일 경로 생성
            Path filePath = Paths.get(fileUploadUtil.getUploadDir()).resolve(fileName);
            
            // 파일이 존재하는지 확인
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            // 파일 타입 확인
            String contentType = determineContentType(fileName);
            
            // 파일 데이터 로드
            byte[] fileData = Files.readAllBytes(filePath);

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "inline; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()) + "\"")
                .body(fileData);

        } catch (Exception e) {
            log.error("파일 로드 실패: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String determineContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "pdf":
                return "application/pdf";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            default:
                return "application/octet-stream";
        }
    }

    // 직원 상태 업데이트
    @PutMapping("/{companyId}/employees/{employeeId}")
    public ResponseEntity<?> updateEmployee(
            @PathVariable Long companyId,
            @PathVariable Long employeeId,
            @RequestBody EmployeeDTO employeeDTO) {
        try {
            companyService.updateEmployeeStatus(companyId, employeeId, employeeDTO.getActive());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 직원 삭제
    @DeleteMapping("/{companyId}/employees/{employeeId}")
    public ResponseEntity<?> removeEmployee(
            @PathVariable Long companyId,
            @PathVariable Long employeeId) {
        try {
            companyService.removeEmployee(companyId, employeeId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 직원 추가
    @PostMapping("/{companyId}/employees")
    public ResponseEntity<?> addEmployee(
            @PathVariable Long companyId,
            @RequestBody EmployeeDTO employeeDTO) {
        try {
            Employee employee = new Employee();
            employee.setName(employeeDTO.getName());
            employee.setPhone(employeeDTO.getPhone());
            employee.setActive(true);
            
            companyService.addEmployee(companyId, employee);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{companyId}/employees")
    public ResponseEntity<List<EmployeeDTO>> getCompanyEmployees(@PathVariable Long companyId) {
        List<Employee> employees = companyService.getCompanyEmployees(companyId);
        List<EmployeeDTO> employeeDTOs = employees.stream()
            .map(emp -> {
                EmployeeDTO dto = new EmployeeDTO();
                dto.setEmployeeId(emp.getEmployeeId());  // 고유 ID 포함
                dto.setName(emp.getName());
                dto.setPhone(emp.getPhone());
                dto.setActive(emp.getActive());
                return dto;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(employeeDTOs);
    }
} 