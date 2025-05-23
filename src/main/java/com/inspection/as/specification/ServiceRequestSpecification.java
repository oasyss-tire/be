package com.inspection.as.specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.inspection.as.entity.ServiceRequest;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public class ServiceRequestSpecification {

    /**
     * 통합 키워드 검색 (매장명, 시설물 유형, 브랜드명을 OR 조건으로 검색)
     */
    public static Specification<ServiceRequest> hasKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return null;
            }
            
            String likePattern = "%" + keyword.toLowerCase() + "%";
            
            // 필요한 조인 생성
            Join<?, ?> facilityJoin = root.join("facility", JoinType.LEFT);
            Join<?, ?> locationCompanyJoin = facilityJoin.join("locationCompany", JoinType.LEFT);
            Join<?, ?> facilityTypeJoin = facilityJoin.join("facilityType", JoinType.LEFT);
            Join<?, ?> brandJoin = facilityJoin.join("brand", JoinType.LEFT);
            
            // 중복 제거 설정
            query.distinct(true);
            
            return criteriaBuilder.or(
                // 매장명에서 검색
                criteriaBuilder.like(
                    criteriaBuilder.lower(locationCompanyJoin.get("storeName")), 
                    likePattern
                ),
                // 시설물 유형에서 검색
                criteriaBuilder.like(
                    criteriaBuilder.lower(facilityTypeJoin.get("codeName")), 
                    likePattern
                ),
                // 브랜드명에서 검색
                criteriaBuilder.like(
                    criteriaBuilder.lower(brandJoin.get("codeName")), 
                    likePattern
                )
            );
        };
    }

    /**
     * 매장명으로 필터링
     */
    public static Specification<ServiceRequest> hasCompanyName(String companyName) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(companyName)) {
                return null;
            }
            
            // Facility -> LocationCompany -> StoreName 조인
            Join<?, ?> facilityJoin = root.join("facility", JoinType.LEFT);
            Join<?, ?> locationCompanyJoin = facilityJoin.join("locationCompany", JoinType.LEFT);
            
            return criteriaBuilder.like(
                criteriaBuilder.lower(locationCompanyJoin.get("storeName")),
                "%" + companyName.toLowerCase() + "%"
            );
        };
    }

    /**
     * 시설물 항목(유형)으로 필터링
     */
    public static Specification<ServiceRequest> hasFacilityTypeName(String facilityTypeName) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(facilityTypeName)) {
                return null;
            }
            
            // Facility -> FacilityType -> CodeName 조인
            Join<?, ?> facilityJoin = root.join("facility", JoinType.LEFT);
            Join<?, ?> facilityTypeJoin = facilityJoin.join("facilityType", JoinType.LEFT);
            
            return criteriaBuilder.like(
                criteriaBuilder.lower(facilityTypeJoin.get("codeName")),
                "%" + facilityTypeName.toLowerCase() + "%"
            );
        };
    }

    /**
     * 품목(브랜드명)으로 필터링
     */
    public static Specification<ServiceRequest> hasBrandName(String brandName) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(brandName)) {
                return null;
            }
            
            // Facility -> Brand -> CodeName 조인
            Join<?, ?> facilityJoin = root.join("facility", JoinType.LEFT);
            Join<?, ?> brandJoin = facilityJoin.join("brand", JoinType.LEFT);
            
            return criteriaBuilder.like(
                criteriaBuilder.lower(brandJoin.get("codeName")),
                "%" + brandName.toLowerCase() + "%"
            );
        };
    }

    /**
     * 지부그룹 ID로 필터링
     */
    public static Specification<ServiceRequest> hasBranchGroupId(String branchGroupId) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(branchGroupId)) {
                return null;
            }
            
            // Facility -> LocationCompany -> BranchGroup -> CodeId 조인
            Join<?, ?> facilityJoin = root.join("facility", JoinType.LEFT);
            Join<?, ?> locationCompanyJoin = facilityJoin.join("locationCompany", JoinType.LEFT);
            Join<?, ?> branchGroupJoin = locationCompanyJoin.join("branchGroup", JoinType.LEFT);
            
            return criteriaBuilder.equal(branchGroupJoin.get("codeId"), branchGroupId);
        };
    }

    /**
     * 지부그룹명으로 필터링
     */
    public static Specification<ServiceRequest> hasBranchGroupName(String branchGroupName) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(branchGroupName)) {
                return null;
            }
            
            // Facility -> LocationCompany -> BranchGroup -> CodeName 조인
            Join<?, ?> facilityJoin = root.join("facility", JoinType.LEFT);
            Join<?, ?> locationCompanyJoin = facilityJoin.join("locationCompany", JoinType.LEFT);
            Join<?, ?> branchGroupJoin = locationCompanyJoin.join("branchGroup", JoinType.LEFT);
            
            return criteriaBuilder.like(
                criteriaBuilder.lower(branchGroupJoin.get("codeName")),
                "%" + branchGroupName.toLowerCase() + "%"
            );
        };
    }

    /**
     * AS 상태 코드로 필터링
     */
    public static Specification<ServiceRequest> hasServiceStatusCode(String serviceStatusCode) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(serviceStatusCode)) {
                return null;
            }
            
            // ServiceRequest -> Status -> CodeId 조인
            Join<?, ?> statusJoin = root.join("status", JoinType.LEFT);
            
            return criteriaBuilder.equal(statusJoin.get("codeId"), serviceStatusCode);
        };
    }

    /**
     * AS 상태명으로 필터링
     */
    public static Specification<ServiceRequest> hasServiceStatusName(String serviceStatusName) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(serviceStatusName)) {
                return null;
            }
            
            // ServiceRequest -> Status -> CodeName 조인
            Join<?, ?> statusJoin = root.join("status", JoinType.LEFT);
            
            return criteriaBuilder.like(
                criteriaBuilder.lower(statusJoin.get("codeName")),
                "%" + serviceStatusName.toLowerCase() + "%"
            );
        };
    }

    /**
     * 담당부서 코드로 필터링
     */
    public static Specification<ServiceRequest> hasDepartmentTypeCode(String departmentTypeCode) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(departmentTypeCode)) {
                return null;
            }
            
            // ServiceRequest -> DepartmentType -> CodeId 조인
            Join<?, ?> departmentTypeJoin = root.join("departmentType", JoinType.LEFT);
            
            return criteriaBuilder.equal(departmentTypeJoin.get("codeId"), departmentTypeCode);
        };
    }

    /**
     * 담당부서명으로 필터링
     */
    public static Specification<ServiceRequest> hasDepartmentTypeName(String departmentTypeName) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(departmentTypeName)) {
                return null;
            }
            
            // ServiceRequest -> DepartmentType -> CodeName 조인
            Join<?, ?> departmentTypeJoin = root.join("departmentType", JoinType.LEFT);
            
            return criteriaBuilder.like(
                criteriaBuilder.lower(departmentTypeJoin.get("codeName")),
                "%" + departmentTypeName.toLowerCase() + "%"
            );
        };
    }

    /**
     * 요청일자 범위로 필터링
     */
    public static Specification<ServiceRequest> hasRequestDateBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("requestDate"), startDate));
            }
            
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("requestDate"), endDate));
            }
            
            return predicates.isEmpty() ? null : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 모든 필터 조건을 결합
     */
    public static Specification<ServiceRequest> withFilters(
            String search,
            String companyName,
            String facilityTypeName,
            String brandName,
            String branchGroupId,
            String branchGroupName,
            String serviceStatusCode,
            String serviceStatusName,
            String departmentTypeCode,
            String departmentTypeName,
            LocalDateTime requestDateStart,
            LocalDateTime requestDateEnd) {
        
        return Specification.where(hasKeyword(search))
                .and(hasCompanyName(companyName))
                .and(hasFacilityTypeName(facilityTypeName))
                .and(hasBrandName(brandName))
                .and(hasBranchGroupId(branchGroupId))
                .and(hasBranchGroupName(branchGroupName))
                .and(hasServiceStatusCode(serviceStatusCode))
                .and(hasServiceStatusName(serviceStatusName))
                .and(hasDepartmentTypeCode(departmentTypeCode))
                .and(hasDepartmentTypeName(departmentTypeName))
                .and(hasRequestDateBetween(requestDateStart, requestDateEnd));
    }
} 