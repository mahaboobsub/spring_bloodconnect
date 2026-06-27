package com.bloodconnect.model;

import jakarta.persistence.*;
import java.sql.Date;

@Entity
@Table(name = "donor_profiles")
public class DonorProfile {

    @Id
    @Column(name = "donor_id")
    private int donorId;

    @Column(name = "blood_group", nullable = false)
    private String bloodGroup;

    @Column(name = "age")
    private Integer age;

    @Column(name = "gender")
    private String gender; // M, F, OTHER

    @Column(name = "city", nullable = false, length = 50)
    private String city;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Column(name = "last_donation_date")
    private Date lastDonationDate;

    @Column(name = "is_available")
    private boolean isAvailable;

    public DonorProfile() {
        this.isAvailable = true;
    }

    // --- Getters and Setters ---

    public int getDonorId() {
        return donorId;
    }

    public void setDonorId(int donorId) {
        this.donorId = donorId;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public Date getLastDonationDate() {
        return lastDonationDate;
    }

    public void setLastDonationDate(Date lastDonationDate) {
        this.lastDonationDate = lastDonationDate;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }
}
