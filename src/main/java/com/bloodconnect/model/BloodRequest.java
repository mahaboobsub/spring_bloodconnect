package com.bloodconnect.model;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "blood_requests")
public class BloodRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private int requestId;

    @Column(name = "requester_id", nullable = false)
    private int requesterId;

    @Column(name = "patient_name", length = 100)
    private String patientName;

    @Column(name = "blood_group_needed", nullable = false)
    private String bloodGroupNeeded;

    @Column(name = "units_required", nullable = false)
    private int unitsRequired;

    @Column(name = "hospital_name", nullable = false, length = 100)
    private String hospitalName;

    @Column(name = "city", nullable = false, length = 50)
    private String city;

    @Column(name = "urgency")
    private String urgency; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "status")
    private String status;  // OPEN, MATCHED, FULFILLED, CLOSED

    @Column(name = "is_verified")
    private boolean isVerified;

    @Column(name = "verified_by")
    private Integer verifiedBy; // Admin user ID

    @Column(name = "created_at", insertable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp createdAt;

    // Transient fields (not mapped to columns in the blood_requests table, populated programmatically)
    @Transient
    private String requesterName;

    @Transient
    private List<DonorMatch> matches;

    public BloodRequest() {
        this.unitsRequired = 1;
        this.urgency = "MEDIUM";
        this.status = "OPEN";
        this.isVerified = false;
    }

    // --- Getters and Setters ---

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(int requesterId) {
        this.requesterId = requesterId;
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

    public int getUnitsRequired() {
        return unitsRequired;
    }

    public void setUnitsRequired(int unitsRequired) {
        this.unitsRequired = unitsRequired;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public Integer getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(Integer verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public List<DonorMatch> getMatches() {
        return matches;
    }

    public void setMatches(List<DonorMatch> matches) {
        this.matches = matches;
    }
}
