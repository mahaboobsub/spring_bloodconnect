package com.bloodconnect.repository;

import com.bloodconnect.model.DonorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonorProfileRepository extends JpaRepository<DonorProfile, Integer> {

    @Query(value = "SELECT d.* FROM donor_profiles d " +
                   "JOIN users u ON d.donor_id = u.user_id " +
                   "WHERE d.blood_group = :bloodGroup " +
                   "AND LOWER(d.city) = LOWER(:city) " +
                   "AND d.is_available = TRUE " +
                   "AND (d.last_donation_date IS NULL OR DATEDIFF(CURDATE(), d.last_donation_date) > 90)", 
           nativeQuery = true)
    List<DonorProfile> findEligibleDonors(@Param("bloodGroup") String bloodGroup, @Param("city") String city);
}
