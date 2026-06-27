package com.bloodconnect.controller;

import com.bloodconnect.model.DonorMatch;
import com.bloodconnect.model.DonorProfile;
import com.bloodconnect.model.User;
import com.bloodconnect.repository.DonorMatchRepository;
import com.bloodconnect.repository.DonorProfileRepository;
import com.bloodconnect.repository.UserRepository;
import com.bloodconnect.util.CityList;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/donor")
public class DonorController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private DonorMatchRepository donorMatchRepository;

    @GetMapping(value = "/profile", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getProfile(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            result.put("success", false);
            result.put("message", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }

        User user = userOpt.get();
        DonorProfile profile = donorProfileRepository.findById(userId).orElse(null);

        // If profile does not exist, create a default one
        if (profile == null) {
            profile = new DonorProfile();
            profile.setDonorId(userId);
            profile.setBloodGroup("O+");
            profile.setCity("Mumbai");
            profile.setAvailable(true);
            profile = donorProfileRepository.save(profile);
        }

        // Fetch matches native
        List<Object[]> rawMatches = donorMatchRepository.getMatchesByDonorNative(userId);
        List<DonorMatch> matches = mapDonorMatches(rawMatches);

        // Compute eligibility
        long daysSinceLastDonation = -1;
        boolean isEligible = true;
        if (profile.getLastDonationDate() != null) {
            long diffMs = System.currentTimeMillis() - profile.getLastDonationDate().getTime();
            daysSinceLastDonation = diffMs / (1000L * 60 * 60 * 24);
            if (daysSinceLastDonation < 0) daysSinceLastDonation = 0;
            isEligible = daysSinceLastDonation > 90;
        }

        result.put("success", true);
        result.put("user", user);
        result.put("profile", profile);
        result.put("matches", matches);
        result.put("cities", CityList.CITIES);
        result.put("daysSinceLastDonation", daysSinceLastDonation);
        result.put("isEligible", isEligible);

        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/profile", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> handlePost(
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "isAvailable", required = false) String isAvailableStr,
            @RequestParam(value = "matchId", required = false) Integer matchId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "bloodGroup", required = false) String bloodGroup,
            @RequestParam(value = "age", required = false) String ageStr,
            @RequestParam(value = "gender", required = false) String gender,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "pincode", required = false) String pincode,
            @RequestParam(value = "lastDonationDate", required = false) String lastDonationDateStr,
            HttpSession session) {

        Map<String, Object> result = new HashMap<>();
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            result.put("success", false);
            result.put("message", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }

        // Action 1: toggleAvailability
        if ("toggleAvailability".equals(action)) {
            boolean available = Boolean.parseBoolean(isAvailableStr);
            Optional<DonorProfile> dpOpt = donorProfileRepository.findById(userId);
            if (dpOpt.isPresent()) {
                DonorProfile dp = dpOpt.get();
                dp.setAvailable(available);
                donorProfileRepository.save(dp);
            }
            result.put("success", true);
            result.put("message", "Availability updated successfully.");
            return ResponseEntity.ok(result);
        }

        // Action 2: updateMatchStatus
        if ("updateMatchStatus".equals(action)) {
            if (matchId == null || status == null) {
                result.put("success", false);
                result.put("message", "Missing matchId or status.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            if ("ACCEPTED".equals(status) || "DECLINED".equals(status)) {
                Optional<DonorMatch> dmOpt = donorMatchRepository.findById(matchId);
                if (dmOpt.isPresent()) {
                    DonorMatch dm = dmOpt.get();
                    if (dm.getDonorId() == userId) {
                        dm.setStatus(status);
                        donorMatchRepository.save(dm);
                        result.put("success", true);
                        result.put("message", "Match request " + status.toLowerCase() + ".");
                        return ResponseEntity.ok(result);
                    }
                }
                result.put("success", false);
                result.put("message", "Match request not found or not authorized.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
            } else {
                result.put("success", false);
                result.put("message", "Invalid status update.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
        }

        // Action 3: Profile Update
        bloodGroup = trim(bloodGroup);
        gender = trim(gender);
        city = trim(city);
        pincode = trim(pincode);
        lastDonationDateStr = trim(lastDonationDateStr);

        StringBuilder errors = new StringBuilder();

        if (isEmpty(bloodGroup)) {
            errors.append("Blood group is required. ");
        }
        if (isEmpty(city)) {
            errors.append("City is required. ");
        }
        if (!isEmpty(pincode) && !pincode.matches("^[0-9]{6}$")) {
            errors.append("Pincode must be exactly 6 digits. ");
        }

        Integer age = null;
        if (!isEmpty(ageStr)) {
            try {
                age = Integer.parseInt(ageStr.trim());
                if (age < 18 || age > 65) {
                    errors.append("Donor age must be between 18 and 65. ");
                }
            } catch (NumberFormatException e) {
                errors.append("Age must be a valid number. ");
            }
        }

        Date lastDonationDate = null;
        if (!isEmpty(lastDonationDateStr)) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                sdf.setLenient(false);
                java.util.Date parsedDate = sdf.parse(lastDonationDateStr);
                lastDonationDate = new Date(parsedDate.getTime());

                if (lastDonationDate.after(new java.util.Date())) {
                    errors.append("Last donation date cannot be in the future. ");
                }
            } catch (ParseException e) {
                errors.append("Invalid date format. ");
            }
        }

        if (errors.length() > 0) {
            result.put("success", false);
            result.put("message", errors.toString().trim());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }

        // Save profile
        DonorProfile dp = donorProfileRepository.findById(userId).orElse(new DonorProfile());
        dp.setDonorId(userId);
        dp.setBloodGroup(bloodGroup);
        dp.setAge(age);
        dp.setGender(gender);
        dp.setCity(city);
        dp.setPincode(pincode);
        dp.setLastDonationDate(lastDonationDate);
        if (isAvailableStr != null) {
            dp.setAvailable(Boolean.parseBoolean(isAvailableStr));
        }

        donorProfileRepository.save(dp);

        result.put("success", true);
        result.put("message", "Profile updated successfully.");
        return ResponseEntity.ok(result);
    }

    private List<DonorMatch> mapDonorMatches(List<Object[]> rows) {
        List<DonorMatch> matches = new ArrayList<>();
        for (Object[] row : rows) {
            DonorMatch dm = new DonorMatch();
            dm.setMatchId(((Number) row[0]).intValue());
            dm.setRequestId(((Number) row[1]).intValue());
            dm.setDonorId(((Number) row[2]).intValue());
            dm.setStatus((String) row[3]);
            dm.setMatchedAt((Timestamp) row[4]);

            dm.setDonorName((String) row[5]);
            dm.setDonorPhone((String) row[6]);
            dm.setDonorBloodGroup((String) row[7]);
            dm.setDonorCity((String) row[8]);

            if (row.length > 9) {
                dm.setPatientName((String) row[9]);
                dm.setBloodGroupNeeded((String) row[10]);
                dm.setUnitsRequired(((Number) row[11]).intValue());
                dm.setHospitalName((String) row[12]);
                dm.setRequestCity((String) row[13]);
                dm.setUrgency((String) row[14]);
            }
            matches.add(dm);
        }
        return matches;
    }

    private String trim(String s) {
        return s != null ? s.trim() : null;
    }

    private boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }
}
