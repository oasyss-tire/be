package com.inspection.as.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.inspection.as.entity.ServiceRequest;
import com.inspection.as.entity.ServiceRequestImage;
import com.inspection.facility.entity.Facility;
import com.inspection.util.EncryptionUtil;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequestDTO {
    
    private Long serviceRequestId;        // AS 접수 ID
    private String requestNumber;         // AS 접수 번호
    private Long facilityId;              // 시설물 ID
    private String facilityName;          // 시설물명
    private String brandName;             // 브랜드명
    
    // 추가 필드 - 시설물 관련 정보
    private String companyName;           // 매장명
    private String facilityTypeName;      // 시설물 항목
    private Integer usefulLifeMonths;     // 사용연한(개월)
    private LocalDateTime installationDate;   // 최초 설치일
    private String statusCode;            // 시설물 상태 코드
    private String statusName;            // 시설물 상태명
    private String currentLocation;       // 현재 위치(주소)
    
    // 서비스 요청 상태 필드
    private String serviceStatusCode;     // 서비스 요청 상태 코드
    private String serviceStatusName;     // 서비스 요청 상태명
    
    private LocalDateTime requestDate;        // 요청일
    private Boolean isReceived;               // 접수 완료 여부
    private String requestContent;            // 요청 내용
    private Long requesterId;                 // 요청자 ID
    private String requesterName;             // 요청자명
    private Long managerId;                   // 담당자 ID
    private String managerName;               // 담당자명
    private LocalDateTime expectedCompletionDate; // 예상 완료일
    private LocalDateTime completionDate;     // 완료일
    private Boolean isCompleted;              // 완료 여부
    private String serviceTypeCode;           // 서비스 유형 코드
    private String serviceTypeName;           // 서비스 유형명
    private String priorityCode;              // 우선순위 코드
    private String priorityName;              // 우선순위명
    private Double cost;                      // 수리 비용
    private String notes;                     // 비고
    private List<ServiceHistoryDTO> serviceHistories; // AS 이력
    private List<ServiceRequestImageDTO> images; // AS 이미지
    private LocalDateTime createdAt;          // 등록일
    private LocalDateTime updatedAt;          // 수정일
    
    /**
     * ServiceRequest 엔티티를 DTO로 변환
     */
    public static ServiceRequestDTO fromEntity(ServiceRequest entity) {
        return ServiceRequestDTO.builder()
                .serviceRequestId(entity.getServiceRequestId())
                .requestNumber(entity.getRequestNumber())
                .facilityId(entity.getFacility().getFacilityId())
                .facilityName(entity.getFacility().getFacilityType().getCodeName())
                .brandName(entity.getFacility().getBrand().getCodeName())
                
                // 추가 필드 - 매장 정보는 상위 계층(서비스)에서 채워줘야 함
                .companyName(getCompanyName(entity.getFacility()))
                .facilityTypeName(entity.getFacility().getFacilityType().getCodeName())
                .usefulLifeMonths(entity.getFacility().getUsefulLifeMonths())
                .installationDate(entity.getFacility().getInstallationDate())
                .statusCode(entity.getFacility().getStatus() != null ? entity.getFacility().getStatus().getCodeId() : null)
                .statusName(entity.getFacility().getStatus() != null ? entity.getFacility().getStatus().getCodeName() : null)
                .currentLocation(entity.getFacility().getLocationCompany() != null ? entity.getFacility().getLocationCompany().getAddress() : null)
                
                // 서비스 요청 상태 필드
                .serviceStatusCode(entity.getStatus() != null ? entity.getStatus().getCodeId() : null)
                .serviceStatusName(entity.getStatus() != null ? entity.getStatus().getCodeName() : null)
                
                .requestDate(entity.getRequestDate())
                .isReceived(entity.getIsReceived())
                .requestContent(entity.getRequestContent())
                .requesterId(entity.getRequester().getId())
                .requesterName(entity.getRequester().getUserName())
                .managerId(entity.getManager() != null ? entity.getManager().getId() : null)
                .managerName(entity.getManager() != null ? entity.getManager().getUserName() : null)
                .expectedCompletionDate(entity.getExpectedCompletionDate())
                .completionDate(entity.getCompletionDate())
                .isCompleted(entity.getIsCompleted())
                .serviceTypeCode(entity.getServiceType().getCodeId())
                .serviceTypeName(entity.getServiceType().getCodeName())
                .priorityCode(entity.getPriority().getCodeId())
                .priorityName(entity.getPriority().getCodeName())
                .cost(entity.getCost())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
    /**
     * ServiceRequest 엔티티를 DTO로 변환 (AS 이력 포함)
     */
    public static ServiceRequestDTO fromEntityWithHistories(ServiceRequest entity) {
        ServiceRequestDTO dto = fromEntity(entity);
        
        if (entity.getServiceHistories() != null) {
            List<ServiceHistoryDTO> historyDtos = entity.getServiceHistories().stream()
                    .map(ServiceHistoryDTO::fromEntity)
                    .toList();
            dto.setServiceHistories(historyDtos);
        }
        
        return dto;
    }
    
    /**
     * ServiceRequest 엔티티를 DTO로 변환 (AS 이력 및 이미지 포함)
     */
    public static ServiceRequestDTO fromEntityWithAll(ServiceRequest entity) {
        ServiceRequestDTO dto = fromEntityWithHistories(entity);
        
        if (entity.getImages() != null) {
            List<ServiceRequestImageDTO> imageDtos = entity.getImages().stream()
                    .filter(image -> image.isActive())
                    .map(ServiceRequestImageDTO::fromEntity)
                    .collect(Collectors.toList());
            dto.setImages(imageDtos);
        }
        
        return dto;
    }
    
    /**
     * 암호화된 currentLocation 필드를 복호화
     * @param encryptionUtil 암호화/복호화 유틸리티
     */
    public void decryptCurrentLocation(EncryptionUtil encryptionUtil) {
        if (this.currentLocation != null && !this.currentLocation.isEmpty()) {
            try {
                this.currentLocation = encryptionUtil.decrypt(this.currentLocation);
            } catch (Exception e) {
                // 복호화 실패 시 원래 값 유지
            }
        }
    }
    
    /**
     * Facility에서 회사 이름 정보를 가져오는 유틸리티 메서드
     */
    private static String getCompanyName(Facility facility) {
        if (facility == null) return null;
        
        // 위치 회사 정보가 있으면 우선 사용
        if (facility.getLocationCompany() != null) {
            return facility.getLocationCompany().getStoreName();
        }
        // 없으면 소유 회사 정보 사용
        else if (facility.getOwnerCompany() != null) {
            return facility.getOwnerCompany().getStoreName();
        }
        
        return null;
    }
} 