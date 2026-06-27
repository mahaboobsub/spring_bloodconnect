package com.bloodconnect.controller;

import com.bloodconnect.model.DonorProfile;
import com.bloodconnect.model.User;
import com.bloodconnect.repository.DonorProfileRepository;
import com.bloodconnect.repository.UserRepository;
import com.bloodconnect.util.CityList;
import com.bloodconnect.util.PasswordUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @GetMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> checkSession(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (session != null && session.getAttribute("userId") != null) {
            result.put("success", true);
            result.put("userId", session.getAttribute("userId"));
            result.put("role", session.getAttribute("role"));
            result.put("userName", session.getAttribute("userName"));
        } else {
            result.put("success", false);
        }
        return result;
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> login(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            HttpSession session) {

        Map<String, Object> result = new HashMap<>();

        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            result.put("success", false);
            result.put("message", "Email and password are required.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }

        Optional<User> userOpt = userRepository.findByEmail(email.trim());

        if (userOpt.isEmpty() || !PasswordUtil.checkPassword(password, userOpt.get().getPasswordHash())) {
            result.put("success", false);
            result.put("message", "Invalid email or password.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }

        User user = userOpt.get();

        // Authentication successful — create session
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("role", user.getRole());
        session.setAttribute("userName", user.getFullName());

        result.put("success", true);
        result.put("role", user.getRole());
        result.put("userName", user.getFullName());
        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/logout", method = {RequestMethod.GET, RequestMethod.POST}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Logged out successfully.");
        return result;
    }

    @GetMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> getCities() {
        return CityList.CITIES;
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> register(
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            @RequestParam("role") String role,
            @RequestParam(value = "bloodGroup", required = false) String bloodGroup,
            @RequestParam(value = "city", required = false) String city) {

        fullName = trim(fullName);
        email = trim(email);
        phone = trim(phone);
        role = trim(role);
        bloodGroup = trim(bloodGroup);
        city = trim(city);

        Map<String, Object> result = new HashMap<>();
        StringBuilder errors = new StringBuilder();

        if (isEmpty(fullName)) {
            errors.append("Full name is required. ");
        }
        if (isEmpty(email) || !email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            errors.append("Valid email is required. ");
        }
        if (isEmpty(phone) || !phone.matches("^[0-9]{10,15}$")) {
            errors.append("Valid phone number (10-15 digits) is required. ");
        }
        if (password == null || password.length() < 8) {
            errors.append("Password must be at least 8 characters. ");
        }
        if (password != null && !password.equals(confirmPassword)) {
            errors.append("Passwords do not match. ");
        }
        if (!"DONOR".equals(role) && !"REQUESTER".equals(role)) {
            errors.append("Invalid role selected. ");
        }
        if ("DONOR".equals(role)) {
            if (isEmpty(bloodGroup)) {
                errors.append("Blood group is required for donors. ");
            }
            if (isEmpty(city)) {
                errors.append("City is required for donors. ");
            }
        }

        if (errors.length() > 0) {
            result.put("success", false);
            result.put("message", errors.toString().trim());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }

        // Check email uniqueness
        if (userRepository.findByEmail(email).isPresent()) {
            result.put("success", false);
            result.put("message", "An account with this email already exists.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }

        // Hash password
        String hashedPassword = PasswordUtil.hashPassword(password);

        // Create user
        User user = new User(fullName, email, hashedPassword, phone, role);
        user = userRepository.save(user);

        // If donor, create donor profile
        if ("DONOR".equals(role)) {
            DonorProfile dp = new DonorProfile();
            dp.setDonorId(user.getUserId());
            dp.setBloodGroup(bloodGroup);
            dp.setCity(city);
            dp.setAvailable(true);
            donorProfileRepository.save(dp);
        }

        result.put("success", true);
        result.put("message", "Registration successful!");
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    private String trim(String s) {
        return s != null ? s.trim() : null;
    }

    private boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }
}
