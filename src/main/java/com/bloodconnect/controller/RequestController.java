package com.bloodconnect.controller;

import com.bloodconnect.model.BloodRequest;
import com.bloodconnect.model.DonorMatch;
import com.bloodconnect.model.DonorProfile;
import com.bloodconnect.repository.BloodRequestRepository;
import com.bloodconnect.repository.DonorMatchRepository;
import com.bloodconnect.repository.DonorProfileRepository;
import com.bloodconnect.util.CityList;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class RequestController {

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private DonorMatchRepository donorMatchRepository;

    @GetMapping(value = "/request/new", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> getCities() {
        return CityList.CITIES;
    }

    @PostMapping(value = "/request/new", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createRequest(
            @RequestParam("patientName") String patientName,
            @RequestParam("bloodGroupNeeded") String bloodGroupNeeded,
            @RequestParam("unitsRequired") String unitsRequiredStr,
            @RequestParam("hospitalName") String hospitalName,
            @RequestParam("city") String city,
            @RequestParam("urgency") String urgency,
            HttpSession session) {

        Map<String, Object> result = new HashMap<>();
        Integer requesterId = (Integer) session.getAttribute("userId");
        if (requesterId == null) {
            result.put("success", false);
            result.put("message", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }

        patientName = trim(patientName);
        bloodGroupNeeded = trim(bloodGroupNeeded);
        hospitalName = trim(hospitalName);
        city = trim(city);
        urgency = trim(urgency);

        StringBuilder errors = new StringBuilder();

        if (isEmpty(patientName)) {
            errors.append("Patient name is required. ");
        }
        if (isEmpty(bloodGroupNeeded)) {
            errors.append("Blood group needed is required. ");
        }
        if (isEmpty(hospitalName)) {
            errors.append("Hospital name is required. ");
        }
        if (isEmpty(city)) {
            errors.append("City is required. ");
        }
        if (isEmpty(urgency)) {
            errors.append("Urgency level is required. ");
        }

        int unitsRequired = 0;
        if (isEmpty(unitsRequiredStr)) {
            errors.append("Units required is required. ");
        } else {
            try {
                unitsRequired = Integer.parseInt(unitsRequiredStr.trim());
                if (unitsRequired <= 0) {
                    errors.append("Units required must be a positive number. ");
                }
            } catch (NumberFormatException e) {
                errors.append("Units required must be a valid number. ");
            }
        }

        if (errors.length() > 0) {
            result.put("success", false);
            result.put("message", errors.toString().trim());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }

        // Create and save request
        BloodRequest req = new BloodRequest();
        req.setRequesterId(requesterId);
        req.setPatientName(patientName);
        req.setBloodGroupNeeded(bloodGroupNeeded);
        req.setUnitsRequired(unitsRequired);
        req.setHospitalName(hospitalName);
        req.setCity(city);
        req.setUrgency(urgency);
        req.setStatus("OPEN");
        req.setVerified(false);

        req = bloodRequestRepository.save(req);

        // Run auto-matching logic
        List<DonorProfile> eligibleDonors = donorProfileRepository.findEligibleDonors(bloodGroupNeeded, city);
        int matchCount = 0;
        for (DonorProfile donor : eligibleDonors) {
            if (!donorMatchRepository.existsByRequestIdAndDonorId(req.getRequestId(), donor.getDonorId())) {
                DonorMatch match = new DonorMatch();
                match.setRequestId(req.getRequestId());
                match.setDonorId(donor.getDonorId());
                match.setStatus("PENDING");
                donorMatchRepository.save(match);
                matchCount++;
            }
        }

        result.put("success", true);
        result.put("message", "Blood request posted successfully!");
        result.put("matchCount", matchCount);
        result.put("requestId", req.getRequestId());

        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/request/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> listRequests(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Integer requesterId = (Integer) session.getAttribute("userId");
        if (requesterId == null) {
            result.put("success", false);
            result.put("message", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }

        List<BloodRequest> requests = bloodRequestRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId);
        List<Map<String, Object>> requestList = new ArrayList<>();

        for (BloodRequest req : requests) {
            Map<String, Object> reqMap = new HashMap<>();
            reqMap.put("requestId", req.getRequestId());
            reqMap.put("patientName", req.getPatientName());
            reqMap.put("bloodGroupNeeded", req.getBloodGroupNeeded());
            reqMap.put("unitsRequired", req.getUnitsRequired());
            reqMap.put("hospitalName", req.getHospitalName());
            reqMap.put("city", req.getCity());
            reqMap.put("urgency", req.getUrgency());
            reqMap.put("status", req.getStatus());
            reqMap.put("isVerified", req.isVerified());
            reqMap.put("createdAt", req.getCreatedAt() != null ? req.getCreatedAt().toString() : "");

            int count = donorMatchRepository.findByRequestId(req.getRequestId()).size();
            reqMap.put("matchCount", count);
            requestList.add(reqMap);
        }

        result.put("success", true);
        result.put("requests", requestList);
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/match/find", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> findMatches(
            @RequestParam("requestId") Integer requestId,
            HttpSession session) {

        Map<String, Object> result = new HashMap<>();
        Integer userId = (Integer) session.getAttribute("userId");
        String role = (String) session.getAttribute("role");

        if (userId == null) {
            result.put("success", false);
            result.put("message", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }

        if (requestId == null) {
            result.put("success", false);
            result.put("message", "Request ID is required.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }

        Optional<BloodRequest> reqOpt = bloodRequestRepository.findById(requestId);
        if (reqOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Blood request not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }

        BloodRequest bloodRequest = reqOpt.get();

        // Authorization: Requester who posted or Admin
        if (!"ADMIN".equals(role) && bloodRequest.getRequesterId() != userId) {
            result.put("success", false);
            result.put("message", "Forbidden. You are not authorized to view these matches.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }

        List<Object[]> rawMatches = donorMatchRepository.getMatchesByRequestNative(requestId);
        List<DonorMatch> matches = mapDonorMatches(rawMatches);

        // Mask phone numbers if not verified
        for (DonorMatch match : matches) {
            if (!bloodRequest.isVerified()) {
                String phone = match.getDonorPhone();
                if (phone != null && phone.length() >= 4) {
                    match.setDonorPhone(phone.substring(0, 2) + "******" + phone.substring(phone.length() - 2));
                } else {
                    match.setDonorPhone("********");
                }
            }
        }

        result.put("success", true);
        result.put("bloodRequest", bloodRequest);
        result.put("matches", matches);

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
