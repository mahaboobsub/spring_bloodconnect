package com.bloodconnect.repository;

import com.bloodconnect.model.BloodRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BloodRequestRepository extends JpaRepository<BloodRequest, Integer> {

    List<BloodRequest> findByRequesterIdOrderByCreatedAtDesc(int requesterId);

    @Query(value = "SELECT * FROM blood_requests " +
                   "ORDER BY FIELD(urgency, 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'), created_at DESC", 
           nativeQuery = true)
    List<BloodRequest> findAllOrderedByUrgency();
}
