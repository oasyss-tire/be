package com.inspection.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.inspection.entity.Company;
import com.inspection.entity.CompanyStatus;
import com.inspection.dto.EmployeeDTO;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CompanyDTO {
    private Long companyId;
    private String companyName;
    private String phoneNumber;
    private String faxNumber;
    private String notes;
    private boolean active;
    private String address;
    
    // 건축물 정보 필드 추가
    private LocalDate buildingPermitDate;     // 건축허가일
    private LocalDate occupancyDate;          // 사용승인일
    private BigDecimal totalFloorArea;        // 연면적
    private BigDecimal buildingArea;          // 건축면적
    private Integer numberOfUnits;            // 세대수
    private String floorCount;                // 층수
    private BigDecimal buildingHeight;        // 높이
    private Integer numberOfBuildings;        // 건물동수
    private String buildingStructure;         // 건축물구조
    private String roofStructure;             // 지붕구조
    private String ramp;                      // 경사로
    private String stairsType;  
    private Integer stairsCount;
    private String elevatorType;
    private Integer elevatorCount;
    private String parkingLot;                // 주차장
    
    // 추가된 필드들
    private String businessNumber;
    private LocalDate contractDate;
    private LocalDate startDate;
    private LocalDate expiryDate;
    private BigDecimal monthlyFee;
    private CompanyStatus status;
    private LocalDate terminationDate;
    private String exteriorImage;
    private String entranceImage;
    private String mainPanelImage;
    private String etcImage1;
    private String etcImage2;
    private String etcImage3;
    private String etcImage4;
    private String businessLicenseImage;
    
    private List<EmployeeDTO> employees = new ArrayList<>();  // 직원 목록 추가
    
    public CompanyDTO() {}
    
    public CompanyDTO(Company company) {
        this.companyId = company.getCompanyId();
        this.companyName = company.getCompanyName();
        this.phoneNumber = company.getPhoneNumber();
        this.faxNumber = company.getFaxNumber();
        this.notes = company.getNotes();
        this.active = company.isActive();
        this.address = company.getAddress();
        
        // 건축물 정보 매핑
        this.buildingPermitDate = company.getBuildingPermitDate();
        this.occupancyDate = company.getOccupancyDate();
        this.totalFloorArea = company.getTotalFloorArea();
        this.buildingArea = company.getBuildingArea();
        this.numberOfUnits = company.getNumberOfUnits();
        this.floorCount = company.getFloorCount();
        this.buildingHeight = company.getBuildingHeight();
        this.numberOfBuildings = company.getNumberOfBuildings();
        this.buildingStructure = company.getBuildingStructure();
        this.roofStructure = company.getRoofStructure();
        this.ramp = company.getRamp();
        this.stairsType = company.getStairsType();
        this.stairsCount = company.getStairsCount();
        this.elevatorType = company.getElevatorType();
        this.elevatorCount = company.getElevatorCount();
        this.parkingLot = company.getParkingLot();
        
        // 추가된 필드들 매핑
        this.businessNumber = company.getBusinessNumber();
        this.contractDate = company.getContractDate();
        this.startDate = company.getStartDate();
        this.expiryDate = company.getExpiryDate();
        this.monthlyFee = company.getMonthlyFee();
        this.status = company.getStatus();
        this.terminationDate = company.getTerminationDate();
        this.exteriorImage = company.getExteriorImage();
        this.entranceImage = company.getEntranceImage();
        this.mainPanelImage = company.getMainPanelImage();
        this.etcImage1 = company.getEtcImage1();
        this.etcImage2 = company.getEtcImage2();
        this.etcImage3 = company.getEtcImage3();
        this.etcImage4 = company.getEtcImage4();
        this.businessLicenseImage = company.getBusinessLicenseImage();
    }
}

