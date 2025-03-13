package com.inspection.service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.inspection.dto.CompanyDTO;
import com.inspection.dto.EmployeeDTO;
import com.inspection.entity.Company;
import com.inspection.entity.CompanyStatus;
import com.inspection.entity.Employee;
import com.inspection.repository.CompanyRepository;
import com.inspection.util.EncryptionUtil;
import com.inspection.util.FileUploadUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final EncryptionUtil aesEncryption;
    private final FileUploadUtil fileUploadUtil;
    

    @Transactional(readOnly = true)
    public List<CompanyDTO> getAllCompanies() {
        return companyRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional
    public CompanyDTO createCompany(CompanyDTO companyDTO,
            MultipartFile businessLicenseImage,
            MultipartFile exteriorImage,
            MultipartFile entranceImage,
            MultipartFile mainPanelImage,
            MultipartFile etcImage1,
            MultipartFile etcImage2,
            MultipartFile etcImage3,
            MultipartFile etcImage4) throws IOException {
        
        log.info("파일 업로드 시작: businessLicenseImage = {}", 
            businessLicenseImage != null ? businessLicenseImage.getOriginalFilename() : "null");
        
        Company company = new Company();
        updateCompanyFromDTO(company, companyDTO);

        // 이미지 처리 추가
        handleImages(company, businessLicenseImage, exteriorImage, entranceImage, mainPanelImage, 
            etcImage1, etcImage2, etcImage3, etcImage4);

        Company savedCompany = companyRepository.save(company);
        log.info("저장된 회사 정보: businessLicenseImage = {}", savedCompany.getBusinessLicenseImage());
        
        return convertToDTO(savedCompany);
    }

    @Transactional
    public Company getOrCreateCompany(String companyName) {
        return companyRepository.findByCompanyName(companyName)
            .orElseGet(() -> {
                Company newCompany = new Company();
                newCompany.setCompanyName(companyName);
                newCompany.setActive(true);
                return companyRepository.save(newCompany);
            });
    }

    @Transactional(readOnly = true)
    public Company getCompanyById(Long companyId) {
        return companyRepository.findById(companyId)
            .orElseThrow(() -> new RuntimeException("업체를 찾을 수 없습니다. ID: " + companyId));
    }

    @Transactional
    public CompanyDTO updateCompany(Long companyId, CompanyDTO companyDTO,
            MultipartFile exteriorImage,
            MultipartFile entranceImage,
            MultipartFile mainPanelImage,
            MultipartFile etcImage1,
            MultipartFile etcImage2,
            MultipartFile etcImage3,
            MultipartFile etcImage4,
            MultipartFile businessLicenseImage) throws IOException {
        
        Company company = getCompanyById(companyId);
        updateCompanyFromDTO(company, companyDTO);

        // 이미지 처리 추가
        handleImages(company, exteriorImage, entranceImage, mainPanelImage, 
            etcImage1, etcImage2, etcImage3, etcImage4, businessLicenseImage);

        Company savedCompany = companyRepository.save(company);
        return convertToDTO(savedCompany);
    }

    // 이미지 처리를 위한 헬퍼 메서드
    private void handleImages(Company company,
            MultipartFile businessLicenseImage,
            MultipartFile exteriorImage,
            MultipartFile entranceImage,
            MultipartFile mainPanelImage,
            MultipartFile etcImage1,
            MultipartFile etcImage2,
            MultipartFile etcImage3,
            MultipartFile etcImage4) throws IOException {
        
        if (businessLicenseImage != null && !businessLicenseImage.isEmpty()) {
            log.info("사업자등록증 파일 처리 시작: {}", businessLicenseImage.getOriginalFilename());
            String savedFileName = fileUploadUtil.saveFile(businessLicenseImage);
            log.info("저장된 파일명: {}", savedFileName);
            company.setBusinessLicenseImage(savedFileName);
        } else {
            log.info("사업자등록증 파일이 없거나 비어있음");
        }
        
        if (exteriorImage != null && !exteriorImage.isEmpty()) {
            fileUploadUtil.deleteFile(company.getExteriorImage());
            company.setExteriorImage(fileUploadUtil.saveFile(exteriorImage));
        }
        if (entranceImage != null && !entranceImage.isEmpty()) {
            fileUploadUtil.deleteFile(company.getEntranceImage());
            company.setEntranceImage(fileUploadUtil.saveFile(entranceImage));
        }
        if (mainPanelImage != null && !mainPanelImage.isEmpty()) {
            fileUploadUtil.deleteFile(company.getMainPanelImage());
            company.setMainPanelImage(fileUploadUtil.saveFile(mainPanelImage));
        }
        if (etcImage1 != null && !etcImage1.isEmpty()) {
            fileUploadUtil.deleteFile(company.getEtcImage1());
            company.setEtcImage1(fileUploadUtil.saveFile(etcImage1));
        }
        if (etcImage2 != null && !etcImage2.isEmpty()) {
            fileUploadUtil.deleteFile(company.getEtcImage2());
            company.setEtcImage2(fileUploadUtil.saveFile(etcImage2));
        }
        if (etcImage3 != null && !etcImage3.isEmpty()) {
            fileUploadUtil.deleteFile(company.getEtcImage3());
            company.setEtcImage3(fileUploadUtil.saveFile(etcImage3));
        }
        if (etcImage4 != null && !etcImage4.isEmpty()) {
            fileUploadUtil.deleteFile(company.getEtcImage4());
            company.setEtcImage4(fileUploadUtil.saveFile(etcImage4));
        }
    }

    // 계약 정보 수정 메서드
    @Transactional
    public CompanyDTO updateContractInfo(Long companyId, CompanyDTO companyDTO) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new RuntimeException("회사를 찾을 수 없습니다."));

        company.setContractDate(companyDTO.getContractDate());
        company.setStartDate(companyDTO.getStartDate());
        company.setExpiryDate(companyDTO.getExpiryDate());
        company.setMonthlyFee(companyDTO.getMonthlyFee());
        company.setStatus(companyDTO.getStatus());
        company.setTerminationDate(companyDTO.getTerminationDate());

        if (companyDTO.getStatus() == CompanyStatus.TERMINATED) {
            company.setTerminationDate(LocalDate.now());
        }

        Company savedCompany = companyRepository.save(company);
        return convertToDTO(savedCompany);
    }

    private void deleteExistingImage(String fileName) {
        if (fileName != null && !fileName.isEmpty()) {
            fileUploadUtil.deleteFile(fileName);
        }
    }

    // ✅ 업체 정보를 DTO로 변환
    private CompanyDTO convertToDTO(Company company) {
        CompanyDTO dto = new CompanyDTO();
        dto.setCompanyId(company.getCompanyId());
        dto.setCompanyName(company.getCompanyName());

        // 전화번호 복호화
        if (company.getPhoneNumber() != null && !company.getPhoneNumber().isEmpty()) {
            try {
                dto.setPhoneNumber(aesEncryption.decrypt(company.getPhoneNumber()));
            } catch (Exception e) {
                dto.setPhoneNumber(company.getPhoneNumber());
            }
        }

        // 팩스번호 복호화
        if (company.getFaxNumber() != null && !company.getFaxNumber().isEmpty()) {
            try {
                dto.setFaxNumber(aesEncryption.decrypt(company.getFaxNumber()));
            } catch (Exception e) {
                dto.setFaxNumber(company.getFaxNumber());
            }
        }

        dto.setNotes(company.getNotes());
        dto.setActive(company.isActive());

        // 주소 정보 추가
        dto.setAddress(company.getAddress());

        // 사업자번호 추가
        dto.setBusinessNumber(company.getBusinessNumber());

        // 계약일자 추가
        dto.setContractDate(company.getContractDate());

        // 시작일자 추가
        dto.setStartDate(company.getStartDate());

        // 만기일자 추가
        dto.setExpiryDate(company.getExpiryDate());

        // 월 비용 추가
        dto.setMonthlyFee(company.getMonthlyFee());

        // 상태 추가
        dto.setStatus(company.getStatus());

        // 해지일자 추가
        dto.setTerminationDate(company.getTerminationDate());

        // 외관사진 추가
        dto.setExteriorImage(company.getExteriorImage());

        // 입구사진 추가
        dto.setEntranceImage(company.getEntranceImage());

        // 메인 분전함사진 추가
        dto.setMainPanelImage(company.getMainPanelImage());

        // 기타1 추가
        dto.setEtcImage1(company.getEtcImage1());

        // 기타2 추가
        dto.setEtcImage2(company.getEtcImage2());

        // 기타3 추가
        dto.setEtcImage3(company.getEtcImage3());

        // 기타4 추가
        dto.setEtcImage4(company.getEtcImage4());
        
        // 사업자등록증 이미지 추가
        dto.setBusinessLicenseImage(company.getBusinessLicenseImage());
        
        
        
        
        

        // ✅ 건축물 정보 추가
        dto.setBuildingPermitDate(company.getBuildingPermitDate());
        dto.setOccupancyDate(company.getOccupancyDate());
        dto.setTotalFloorArea(company.getTotalFloorArea());
        dto.setBuildingArea(company.getBuildingArea());

        dto.setNumberOfUnits(company.getNumberOfUnits());
        dto.setFloorCount(company.getFloorCount());
        dto.setBuildingHeight(company.getBuildingHeight());
        dto.setNumberOfBuildings(company.getNumberOfBuildings());
        dto.setBuildingStructure(company.getBuildingStructure());
        dto.setRoofStructure(company.getRoofStructure());
        dto.setRamp(company.getRamp());
        dto.setStairsType(company.getStairsType());
        dto.setStairsCount(company.getStairsCount());
        dto.setElevatorType(company.getElevatorType());
        dto.setElevatorCount(company.getElevatorCount());
        dto.setParkingLot(company.getParkingLot());

        // 직원 정보 변환 추가
        dto.setEmployees(company.getEmployees().stream()
            .map(emp -> {
                EmployeeDTO empDTO = new EmployeeDTO();
                empDTO.setName(emp.getName());
                empDTO.setPhone(emp.getPhone());
                empDTO.setActive(emp.getActive());
                return empDTO;
            })
            .collect(Collectors.toList()));

        return dto;
    }



    // ✅ 업체 정보를 DTO로 변환
    private void updateCompanyFromDTO(Company company, CompanyDTO dto) {
        company.setCompanyName(dto.getCompanyName());
        
        // 전화번호 암호화
        if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().isEmpty()) {
            company.setPhoneNumber(aesEncryption.encrypt(dto.getPhoneNumber()));
        }
        
        // 팩스번호 암호화
        if (dto.getFaxNumber() != null && !dto.getFaxNumber().isEmpty()) {
            company.setFaxNumber(aesEncryption.encrypt(dto.getFaxNumber()));
        }
        
        company.setNotes(dto.getNotes());
        company.setActive(true);  // ✅ 저장할 때 항상 true로 설정 (주의 필요)

        // 주소 정보 추가
        company.setAddress(dto.getAddress());

        // 사업자번호 추가
        company.setBusinessNumber(dto.getBusinessNumber());

        // 계약일자 추가
        company.setContractDate(dto.getContractDate());

        // 시작일자 추가
        company.setStartDate(dto.getStartDate());

        // 만기일자 추가
        company.setExpiryDate(dto.getExpiryDate());

        // 월 비용 추가
        company.setMonthlyFee(dto.getMonthlyFee());

        // 상태 추가
        company.setStatus(dto.getStatus());

        // 해지일자 추가
        company.setTerminationDate(dto.getTerminationDate());

        // 외관사진 추가
        company.setExteriorImage(dto.getExteriorImage());

        // 입구사진 추가
        company.setEntranceImage(dto.getEntranceImage());

        // 메인 분전함사진 추가
        company.setMainPanelImage(dto.getMainPanelImage());

        // 기타1 추가       
        company.setEtcImage1(dto.getEtcImage1());

        // 기타2 추가
        company.setEtcImage2(dto.getEtcImage2());

        // 기타3 추가   
        company.setEtcImage3(dto.getEtcImage3());

        // 기타4 추가
        company.setEtcImage4(dto.getEtcImage4());

        // 사업자등록증 이미지 추가
        company.setBusinessLicenseImage(dto.getBusinessLicenseImage());


        // ✅ 건축물 정보 추가
        company.setBuildingPermitDate(dto.getBuildingPermitDate());
        company.setOccupancyDate(dto.getOccupancyDate());
        company.setTotalFloorArea(dto.getTotalFloorArea());
        company.setBuildingArea(dto.getBuildingArea());

        company.setNumberOfUnits(dto.getNumberOfUnits());
        company.setFloorCount(dto.getFloorCount());
        company.setBuildingHeight(dto.getBuildingHeight());
        company.setNumberOfBuildings(dto.getNumberOfBuildings());
        company.setBuildingStructure(dto.getBuildingStructure());
        company.setRoofStructure(dto.getRoofStructure());
        company.setRamp(dto.getRamp());
        company.setStairsType(dto.getStairsType());
        company.setStairsCount(dto.getStairsCount());
        company.setElevatorType(dto.getElevatorType());
        company.setElevatorCount(dto.getElevatorCount());
        company.setParkingLot(dto.getParkingLot());

        // 직원 정보 처리 수정
        if (dto.getEmployees() != null) {
            // 기존 직원 목록 유지를 위한 임시 리스트
            List<Employee> updatedEmployees = new ArrayList<>();
            
            // 새로운/수정된 직원 정보 처리
            dto.getEmployees().forEach(empDTO -> {
                // 기존 직원 찾기 (전화번호로 매칭)
                Employee existingEmployee = company.getEmployees().stream()
                    .filter(emp -> emp.getPhone().equals(empDTO.getPhone()))
                    .findFirst()
                    .orElse(null);

                if (existingEmployee != null) {
                    // 기존 직원 정보 업데이트
                    existingEmployee.setName(empDTO.getName());
                    existingEmployee.setActive(empDTO.getActive());
                    updatedEmployees.add(existingEmployee);
                } else {
                    // 새로운 직원 추가
                    Employee newEmployee = new Employee();
                    newEmployee.setName(empDTO.getName());
                    newEmployee.setPhone(empDTO.getPhone());
                    newEmployee.setActive(empDTO.getActive());
                    newEmployee.setCompany(company);
                    updatedEmployees.add(newEmployee);
                }
            });
            
            // 기존 직원 목록 초기화 후 업데이트된 목록으로 교체
            company.getEmployees().clear();
            company.getEmployees().addAll(updatedEmployees);
        }
    }





    @Transactional
    public void deleteCompany(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new RuntimeException("업체를 찾을 수 없습니다. ID: " + companyId));
        companyRepository.delete(company);
    }

    @Transactional(readOnly = true)
    public CompanyDTO getCompanyDTOById(Long companyId) {
        Company company = getCompanyById(companyId);
        return convertToDTO(company);
    }

    @Transactional
    public void addEmployee(Long companyId, Employee employee) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new RuntimeException("회사를 찾을 수 없습니다. ID: " + companyId));
        
        employee.setCompany(company);
        company.getEmployees().add(employee);
        companyRepository.save(company);
    }

    @Transactional
    public void addEmployees(Long companyId, List<Employee> employees) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new RuntimeException("회사를 찾을 수 없습니다. ID: " + companyId));
        
        employees.forEach(employee -> {
            employee.setCompany(company);
            company.getEmployees().add(employee);
        });
        
        companyRepository.save(company);
    }

    @Transactional
    public void removeEmployee(Long companyId, Long employeeId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new RuntimeException("회사를 찾을 수 없습니다. ID: " + companyId));
        
        company.getEmployees().removeIf(employee -> employee.getEmployeeId().equals(employeeId));
        companyRepository.save(company);
    }

    @Transactional(readOnly = true)
    public List<Employee> getCompanyEmployees(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new RuntimeException("회사를 찾을 수 없습니다. ID: " + companyId));
        
        return company.getEmployees();
    }

    @Transactional
    public void updateEmployeeStatus(Long companyId, Long employeeId, boolean active) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new RuntimeException("회사를 찾을 수 없습니다. ID: " + companyId));
        
        company.getEmployees().stream()
            .filter(employee -> employee.getEmployeeId().equals(employeeId))
            .findFirst()
            .ifPresent(employee -> employee.setActive(active));
        
        companyRepository.save(company);
    }
} 