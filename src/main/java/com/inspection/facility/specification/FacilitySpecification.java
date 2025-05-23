package com.inspection.facility.specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.inspection.facility.entity.Facility;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public class FacilitySpecification {

    /**
     * 통합 키워드 검색 (관리번호, 시리얼번호, 매장명, 품목명 중 하나라도 일치하는 검색 - OR 조건)
     */
    public static Specification<Facility> hasSearchKeyword(String search) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(search)) {
                return null;
            }
            
            String likePattern = "%" + search.toLowerCase() + "%";
            
            // 필요한 조인 생성
            Join<?, ?> locationCompanyJoin = root.join("locationCompany", JoinType.LEFT);
            Join<?, ?> brandJoin = root.join("brand", JoinType.LEFT);
            Join<?, ?> facilityTypeJoin = root.join("facilityType", JoinType.LEFT);
            
            // 중복 제거 설정
            query.distinct(true);
            
            return criteriaBuilder.or(
                // 관리번호에서 검색
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("managementNumber")), 
                    likePattern
                ),
                // 시리얼번호에서 검색
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("serialNumber")), 
                    likePattern
                ),
                // 매장명에서 검색
                criteriaBuilder.like(
                    criteriaBuilder.lower(locationCompanyJoin.get("storeName")), 
                    likePattern
                ),
                // 브랜드명(품목)에서 검색
                criteriaBuilder.like(
                    criteriaBuilder.lower(brandJoin.get("codeName")), 
                    likePattern
                ),
                // 시설물 유형명에서 검색
                criteriaBuilder.like(
                    criteriaBuilder.lower(facilityTypeJoin.get("codeName")), 
                    likePattern
                )
            );
        };
    }
    
    /**
     * 설치일자 범위로 필터링
     */
    public static Specification<Facility> hasInstallationDateBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("installationDate"), startDate));
            }
            
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("installationDate"), endDate));
            }
            
            return predicates.isEmpty() ? null : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * 상태 코드로 필터링
     */
    public static Specification<Facility> hasStatusCode(String statusCode) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(statusCode)) {
                return null;
            }
            
            // 상태 코드 조인
            Join<?, ?> statusJoin = root.join("status", JoinType.LEFT);
            
            return criteriaBuilder.equal(statusJoin.get("codeId"), statusCode);
        };
    }
    
    /**
     * 모든 필터 조건을 결합
     */
    public static Specification<Facility> withFilters(
            String search,
            String statusCode,
            LocalDateTime installationStartDate,
            LocalDateTime installationEndDate) {
        
        return Specification.where(hasSearchKeyword(search))
                .and(hasStatusCode(statusCode))
                .and(hasInstallationDateBetween(installationStartDate, installationEndDate));
    }
} 