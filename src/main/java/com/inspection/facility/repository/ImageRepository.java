package com.inspection.facility.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.inspection.facility.entity.Image;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    
    /**
     * 특정 시설물의 모든 활성화된 이미지 목록 조회
     */
    List<Image> findByFacilityFacilityIdAndActiveTrue(Long facilityId);
    
    /**
     * 특정 시설물의 모든 이미지 목록 조회 (비활성화 이미지 포함)
     */
    List<Image> findByFacilityFacilityId(Long facilityId);
    
    /**
     * 특정 시설물의 이미지를 모두 삭제
     */
    void deleteByFacilityFacilityId(Long facilityId);
    
    /**
     * 특정 이미지 타입의 활성화된 이미지 목록 조회
     */
    @Query("SELECT i FROM Image i WHERE i.imageType.codeId = :imageTypeCode AND i.active = true")
    List<Image> findByImageTypeCodeIdAndActiveTrue(@Param("imageTypeCode") String imageTypeCode);
    
    /**
     * 특정 시설물 ID 목록과 이미지 타입에 해당하는 활성화된 이미지 목록 조회
     */
    @Query("SELECT i FROM Image i WHERE i.facility.facilityId IN :facilityIds AND i.imageType.codeId = :imageTypeCode AND i.active = true")
    List<Image> findByFacilityIdInAndImageTypeCodeAndActiveTrue(
            @Param("facilityIds") List<Long> facilityIds,
            @Param("imageTypeCode") String imageTypeCode);
} 