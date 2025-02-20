package com.inspection.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.inspection.entity.KakaoAlert;
import java.util.List;

public interface KakaoAlertRepository extends JpaRepository<KakaoAlert, Long> {
    List<KakaoAlert> findByUserId(Long userId);
} 