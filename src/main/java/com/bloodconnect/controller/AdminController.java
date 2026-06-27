package com.bloodconnect.controller;

import com.bloodconnect.model.BloodRequest;
import com.bloodconnect.model.DonorMatch;
import com.bloodconnect.model.User;
import com.bloodconnect.repository.BloodRequestRepository;
import com.bloodconnect.repository.DonorMatchRepository;
import com.bloodconnect.repository.UserRepository;
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
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorMatchRepository donorMatchRepository;

    @GetMapping(value = "/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getDashboard(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        String role = (String) session.getAttribute("role");

        if (!"ADMIN".equals(role)) {
            result.put("success", false);
            result.put("message", "Forbidden");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }

        try {
            List<BloodRequest> requests = bloodRequestRepository.findAllOrderedByUrgency();
            int pendingVerifications = 0;

            for (BloodRequest req : requests) {
                // Populate requester name if not already set or fetch from user repository
                Optional<User> requesterOpt = userRepository.findById(req.getRequesterId());
                requesterOpt.ifPresent(user -> req.setRequesterName(user.getFullName()));

                List<Object[]> rawMatches = donorMatchRepository.getMatchesByRequestNative(req.getRequestId());
                req.setMatches(mapDonorMatches(rawMatches));

                if (!req.isVerified()) {
                    pendingVerifications++;
                }
            }

            List<User> users = userRepository.findAll();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalAccounts", users.size());
            stats.put("activeRequests", requests.size());
            stats.put("pendingVerifications", pendingVerifications);

            result.put("success", true);
            result.put("stats", stats);
            result.put("requests", requests);
            result.put("users", users);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Error loading dashboard details.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @PostMapping(value = "/verify", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> handleVerifyAction(
            @RequestParam("action") String action,
            @RequestParam("requestId") Integer requestId,
            @RequestParam(value = "status", required = false) String status,
            HttpSession session) {

        Map<String, Object> result = new HashMap<>();
        String role = (String) session.getAttribute("role");
        Integer adminId = (Integer) session.getAttribute("userId");

        if (!"ADMIN".equals(role) || adminId == null) {
            result.put("success", false);
            result.put("message", "Forbidden. Insufficient permissions.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }

        if (requestId == null) {
            result.put("success", false);
            result.put("message", "Missing request ID.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }

        Optional<BloodRequest> reqOpt = bloodRequestRepository.findById(requestId);
        if (reqOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Blood request not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }

        BloodRequest req = reqOpt.get();

        if ("verify".equals(action)) {
            req.setVerified(true);
            req.setVerifiedBy(adminId);
            bloodRequestRepository.save(req);
            result.put("success", true);
            result.put("message", "Blood request verified successfully. Contact details are now public for matching donors.");
            return ResponseEntity.ok(result);
        } else if ("updateStatus".equals(action)) {
            if ("OPEN".equals(status) || "MATCHED".equals(status) || "FULFILLED".equals(status) || "CLOSED".equals(status)) {
                req.setStatus(status);
                bloodRequestRepository.save(req);
                result.put("success", true);
                result.put("message", "Blood request status updated to " + status + ".");
                return ResponseEntity.ok(result);
            } else {
                result.put("success", false);
                result.put("message", "Invalid status value.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
        } else {
            result.put("success", false);
            result.put("message", "Invalid action.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
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
}
