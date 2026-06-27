package com.bloodconnect.repository;

import com.bloodconnect.model.DonorMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonorMatchRepository extends JpaRepository<DonorMatch, Integer> {

    boolean existsByRequestIdAndDonorId(int requestId, int donorId);

    List<DonorMatch> findByRequestId(int requestId);

    @Query(value = "SELECT dm.match_id, dm.request_id, dm.donor_id, dm.status, dm.matched_at, " +
                   "u.full_name AS donor_name, u.phone AS donor_phone, " +
                   "d.blood_group AS donor_blood_group, d.city AS donor_city " +
                   "FROM donor_matches dm " +
                   "JOIN users u ON dm.donor_id = u.user_id " +
                   "JOIN donor_profiles d ON dm.donor_id = d.donor_id " +
                   "WHERE dm.request_id = :requestId " +
                   "ORDER BY dm.matched_at DESC", 
           nativeQuery = true)
    List<Object[]> getMatchesByRequestNative(@Param("requestId") int requestId);

    @Query(value = "SELECT dm.match_id, dm.request_id, dm.donor_id, dm.status, dm.matched_at, " +
                   "u.full_name AS donor_name, u.phone AS donor_phone, " +
                   "d.blood_group AS donor_blood_group, d.city AS donor_city, " +
                   "br.patient_name, br.blood_group_needed, br.units_required, " +
                   "br.hospital_name, br.city AS request_city, br.urgency " +
                   "FROM donor_matches dm " +
                   "JOIN users u ON dm.donor_id = u.user_id " +
                   "JOIN donor_profiles d ON dm.donor_id = d.donor_id " +
                   "JOIN blood_requests br ON dm.request_id = br.request_id " +
                   "WHERE dm.donor_id = :donorId " +
                   "ORDER BY dm.matched_at DESC", 
           nativeQuery = true)
    List<Object[]> getMatchesByDonorNative(@Param("donorId") int donorId);
}
