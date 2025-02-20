package com.inspection.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;



@Entity
@Getter @Setter
@NoArgsConstructor
public class FacilityStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id")
    private Facility facility;

    @Enumerated(EnumType.STRING)
    private FacilityStatus status;

    private String currentLocation;
    
    private LocalDateTime statusChangeDate;

    // 생성자
    public static FacilityStatusHistory createHistory(Facility facility, FacilityStatus status, String currentLocation) {
        FacilityStatusHistory history = new FacilityStatusHistory();
        history.setFacility(facility);
        history.setStatus(status);
        history.setCurrentLocation(currentLocation);
        history.setStatusChangeDate(LocalDateTime.now());
        return history;
    }
}