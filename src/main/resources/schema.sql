-- BloodConnect Database Schema
-- Run this script once to set up the database


-- ============================================================
-- USERS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(15) NOT NULL,
    role ENUM('DONOR', 'REQUESTER', 'ADMIN') NOT NULL DEFAULT 'DONOR',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- DONOR PROFILES TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS donor_profiles (
    donor_id INT PRIMARY KEY,
    blood_group ENUM('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-') NOT NULL,
    age INT,
    gender ENUM('M', 'F', 'OTHER'),
    city VARCHAR(50) NOT NULL,
    pincode VARCHAR(10),
    last_donation_date DATE,
    is_available BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (donor_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ============================================================
-- BLOOD REQUESTS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS blood_requests (
    request_id INT AUTO_INCREMENT PRIMARY KEY,
    requester_id INT NOT NULL,
    patient_name VARCHAR(100),
    blood_group_needed ENUM('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-') NOT NULL,
    units_required INT NOT NULL DEFAULT 1,
    hospital_name VARCHAR(100) NOT NULL,
    city VARCHAR(50) NOT NULL,
    urgency ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') DEFAULT 'MEDIUM',
    status ENUM('OPEN', 'MATCHED', 'FULFILLED', 'CLOSED') DEFAULT 'OPEN',
    is_verified BOOLEAN DEFAULT FALSE,
    verified_by INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (requester_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (verified_by) REFERENCES users(user_id) ON DELETE SET NULL
);

-- ============================================================
-- DONOR MATCHES TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS donor_matches (
    match_id INT AUTO_INCREMENT PRIMARY KEY,
    request_id INT NOT NULL,
    donor_id INT NOT NULL,
    status ENUM('PENDING', 'ACCEPTED', 'DECLINED') DEFAULT 'PENDING',
    matched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (request_id) REFERENCES blood_requests(request_id) ON DELETE CASCADE,
    FOREIGN KEY (donor_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ============================================================
-- INDEXES for matching query performance
-- ============================================================
CREATE INDEX idx_donor_blood_group ON donor_profiles(blood_group);
CREATE INDEX idx_donor_city ON donor_profiles(city);
CREATE INDEX idx_donor_available ON donor_profiles(is_available);
CREATE INDEX idx_request_status ON blood_requests(status);
CREATE INDEX idx_request_verified ON blood_requests(is_verified);
CREATE INDEX idx_match_request ON donor_matches(request_id);
CREATE INDEX idx_match_donor ON donor_matches(donor_id);

-- ============================================================
-- SEED: Admin account
-- Email: admin@bloodconnect.com
-- Password: Admin@123
-- BCrypt hash generated with 10 salt rounds
-- This admin logs in through LoginServlet like every other user.
-- ============================================================
INSERT INTO users (full_name, email, password_hash, phone, role)
VALUES (
    'System Admin',
    'admin@bloodconnect.com',
    '$2a$10$UE6oFLim8BTEoFqO/JW3VeTpt1hMd.lsFeqL2HtVUq2a9krwaRJIq',
    '9999999999',
    'ADMIN'
) ON DUPLICATE KEY UPDATE full_name='System Admin';
