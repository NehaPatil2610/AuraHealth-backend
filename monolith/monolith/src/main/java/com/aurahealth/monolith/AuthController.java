package com.aurahealth.monolith;

import com.aurahealth.monolith.entity.Doctor;
import com.aurahealth.monolith.entity.Patient;
import com.aurahealth.monolith.model.User;
import com.aurahealth.monolith.security.JwtTokenProvider;
import com.aurahealth.monolith.security.SessionCookieFactory;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final DoctorRepository doctors;
    private final PatientRepository patients;
    private final JwtTokenProvider tokens;
    private final PasswordEncoder passwords;
    private final SessionCookieFactory cookies;
    private final String internalOauthKey;
    private final boolean mockAuthEnabled;

    public AuthController(UserRepository users, DoctorRepository doctors, PatientRepository patients,
                          JwtTokenProvider tokens, PasswordEncoder passwords, SessionCookieFactory cookies,
                          @Value("${aura.internal.oauth-key}") String internalOauthKey,
                          @Value("${aura.mock-auth.enabled:false}") boolean mockAuthEnabled) {
        this.users = users; this.doctors = doctors; this.patients = patients;
        this.tokens = tokens; this.passwords = passwords; this.cookies = cookies;
        this.internalOauthKey = internalOauthKey;
        this.mockAuthEnabled = mockAuthEnabled;
    }

    public record RegisterRequest(@NotBlank @Size(max = 160) String name, @NotBlank @Email @Size(max = 320) String email,
                                  @NotBlank @Size(min = 8, max = 128) String password,
                                  @NotBlank @Pattern(regexp = "PATIENT|DOCTOR|ADMIN") String role,
                                  @Size(max = 80) String licenseId, @Size(max = 120) String specialty) {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    @PostMapping("/register") @Transactional
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) return error(HttpStatus.CONFLICT, "An account with this email already exists.");
        User.Role role = User.Role.valueOf(request.role());
        if (role == User.Role.DOCTOR && (blank(request.licenseId()) || blank(request.specialty())))
            return error(HttpStatus.BAD_REQUEST, "License ID and specialty are required for a doctor account.");
        User user = new User();
        user.setFullName(request.name().trim()); user.setEmail(email); user.setPasswordHash(passwords.encode(request.password()));
        user.setRole(role); user.setProvider(User.AuthProvider.LOCAL); user = users.save(user);
        createProfile(user, request.licenseId(), request.specialty());
        return authenticated(user, response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        User user = users.findByEmailIgnoreCase(request.email().trim()).orElse(null);
        if (user == null || user.getProvider() != User.AuthProvider.LOCAL || !passwords.matches(request.password(), user.getPasswordHash()))
            return error(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        return authenticated(user, response, HttpStatus.OK);
    }

    @PostMapping("/oauth-provision") @Transactional
    public ResponseEntity<?> provisionGoogle(@RequestHeader(value = "X-Aura-Internal-Key", required = false) String internalKey, @RequestBody OAuthProvisionRequest request) {
        if (!java.security.MessageDigest.isEqual(internalOauthKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), String.valueOf(internalKey).getBytes(java.nio.charset.StandardCharsets.UTF_8)))
            return error(HttpStatus.FORBIDDEN, "Invalid internal credential.");
        if (blank(request.email())) return error(HttpStatus.BAD_REQUEST, "Google did not return an email address.");
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User user = users.findByEmailIgnoreCase(email).orElseGet(() -> {
            User created = new User(); created.setEmail(email); created.setFullName(blank(request.name()) ? "AuraHealth Member" : request.name().trim());
            created.setPasswordHash(passwords.encode(java.util.UUID.randomUUID().toString())); created.setRole(User.Role.PATIENT);
            created.setProvider(User.AuthProvider.GOOGLE); created = users.save(created); createProfile(created, null, null); return created;
        });
        return ResponseEntity.ok(profile(user));
    }
    public record OAuthProvisionRequest(String email, String name) {}

    // ── Dev-Only Mock Auth Bypass ──────────────────────────────────
    // Allows developers to quickly test any role without a real OAuth flow.
    // Gated behind aura.mock-auth.enabled (defaults to false).
    @PostMapping("/oauth-mock-bypass") @Transactional
    public ResponseEntity<?> mockAuthBypass(@RequestBody MockBypassRequest request, HttpServletResponse response) {
        if (!mockAuthEnabled) {
            return error(HttpStatus.FORBIDDEN, "Mock auth bypass is disabled. Set aura.mock-auth.enabled=true to enable.");
        }
        if (blank(request.email())) return error(HttpStatus.BAD_REQUEST, "Email is required.");
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String name = blank(request.name()) ? "Dev User" : request.name().trim();
        String roleStr = blank(request.role()) ? "PATIENT" : request.role().trim().toUpperCase(Locale.ROOT);

        // Strip ROLE_ prefix if provided
        if (roleStr.startsWith("ROLE_")) roleStr = roleStr.substring(5);

        User.Role role;
        try {
            role = User.Role.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, "Invalid role: " + roleStr + ". Must be PATIENT, DOCTOR, or ADMIN.");
        }

        User user = users.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setFullName(name);
            user.setPasswordHash(passwords.encode(java.util.UUID.randomUUID().toString()));
            user.setRole(role);
            user.setProvider(User.AuthProvider.LOCAL);
            user = users.save(user);
            createProfile(user, role == User.Role.DOCTOR ? "DEV-LICENSE-001" : null,
                          role == User.Role.DOCTOR ? "General Practice" : null);
        }

        return authenticated(user, response, HttpStatus.OK);
    }
    public record MockBypassRequest(String email, String name, String role) {}

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) return error(HttpStatus.UNAUTHORIZED, "Not authenticated.");
        return users.findByEmailIgnoreCase(authentication.getName()).<ResponseEntity<?>>map(user -> ResponseEntity.ok(profile(user)))
                .orElseGet(() -> error(HttpStatus.UNAUTHORIZED, "Account no longer exists."));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) { clearCookie(response); return ResponseEntity.ok(Map.of("message", "Logged out")); }

    private void createProfile(User user, String licenseId, String specialty) {
        if (user.getRole() == User.Role.DOCTOR) {
            Doctor doctor = new Doctor();
            doctor.setUserId(user.getId()); doctor.setName(user.getFullName()); doctor.setEmail(user.getEmail());
            doctor.setLicenseNumber(licenseId != null ? licenseId.trim() : "PENDING");
            doctor.setSpecialization(specialty != null ? specialty.trim() : "General");
            doctor.setApprovalStatus(false); doctor.setAvailable(false); doctors.save(doctor);
        } else if (user.getRole() == User.Role.PATIENT) {
            Patient patient = new Patient();
            patient.setUserId(user.getId()); patient.setName(user.getFullName()); patient.setEmail(user.getEmail());
            patient.setSubscriptionTier("FREE"); patients.save(patient);
        }
        // ADMIN role does not require a profile record in doctors/patients tables
    }

    private ResponseEntity<?> authenticated(User user, HttpServletResponse response, HttpStatus status) {
        String token = tokens.generateToken(user.getEmail(), List.of("ROLE_" + user.getRole().name()));
        cookies.issue(response, token);
        return ResponseEntity.status(status).body(Map.of("message", "Authenticated", "user", profile(user)));
    }

    private Map<String, Object> profile(User user) {
        java.util.LinkedHashMap<String, Object> value = new java.util.LinkedHashMap<>();
        value.put("id", user.getId());
        value.put("name", user.getFullName());
        value.put("email", user.getEmail());
        value.put("role", user.getRole().name());
        value.put("provider", user.getProvider().name());
        if (user.getRole() == User.Role.PATIENT) patients.findByEmail(user.getEmail()).ifPresent(p -> value.put("subscription", p.getSubscriptionTier()));
        return value;
    }
    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) { return ResponseEntity.status(status).body(Map.of("message", message)); }
    private void clearCookie(HttpServletResponse response) { cookies.clear(response); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}