package com.bloodconnect.model;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "donor_matches")
public class DonorMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_id")
    private int matchId;

    @Column(name = "request_id", nullable = false)
    private int requestId;

    @Column(name = "donor_id", nullable = false)
    private int donorId;

    @Column(name = "status")
    private String status; // PENDING, ACCEPTED, DECLINED

    @Column(name = "matched_at", insertable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp matchedAt;

    // Transient fields for display (populated via custom queries, not persisted)
    @Transient
    private String donorName;

    @Transient
    private String donorPhone;

    @Transient
    private String donorBloodGroup;

    @Transient
    private String donorCity;

    // Transient request fields for donor view
    @Transient
    private String patientName;

    @Transient
    private String bloodGroupNeeded;

    @Transient
    private String hospitalName;

    @Transient
    private String requestCity;

    @Transient
    private String urgency;

    @Transient
    private Integer unitsRequired;

    public DonorMatch() {
        this.status = "PENDING";
    }

    // --- Getters and Setters ---

    public int getMatchId() {
        return matchId;
    }

    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getDonorId() {
        return donorId;
    }

    public void setDonorId(int donorId) {
        this.donorId = donorId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getMatchedAt() {
        return matchedAt;
    }

    public void setMatchedAt(Timestamp matchedAt) {
        this.matchedAt = matchedAt;
    }

    public String getDonorName() {
        return donorName;
    }

    public void setDonorName(String donorName) {
        this.donorName = donorName;
    }

    public String getDonorPhone() {
        return donorPhone;
    }

    public void setDonorPhone(String donorPhone) {
        this.donorPhone = donorPhone;
    }

    public String getDonorBloodGroup() {
        return donorBloodGroup;
    }

    public void setDonorBloodGroup(String donorBloodGroup) {
        this.donorBloodGroup = donorBloodGroup;
    }

    public String getDonorCity() {
        return donorCity;
    }

    public void setDonorCity(String donorCity) {
        this.donorCity = donorCity;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getBloodGroupNeeded() {
        return bloodGroupNeeded;
    }

    public void setBloodGroupNeeded(String bloodGroupNeeded) {
        this.bloodGroupNeeded = bloodGroupNeeded;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getRequestCity() {
        return requestCity;
    }

    public void setRequestCity(String requestCity) {
        this.requestCity = requestCity;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public Integer getUnitsRequired() {
        return unitsRequired;
    }

    public void setUnitsRequired(Integer unitsRequired) {
        this.unitsRequired = unitsRequired;
    }
}
