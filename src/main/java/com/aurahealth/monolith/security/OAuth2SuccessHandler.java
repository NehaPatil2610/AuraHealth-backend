package com.aurahealth.monolith.security;

import com.aurahealth.monolith.PatientRepository;
import com.aurahealth.monolith.UserRepository;
import com.aurahealth.monolith.entity.Patient;
import com.aurahealth.monolith.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Runs after Google finishes the OAuth2 authorization-code exchange.
 *
 * <p>Bridges Google's identity into the app's existing JWT-cookie session:
 * find-or-create the {@link User} by email (new users default to
 * {@link User.Role#PATIENT}), then issue the <em>exact same</em> AURA_SESSION
 * cookie the email/password login issues — via the shared {@link JwtTokenProvider}
 * and {@link SessionCookieFactory} — so {@code /api/auth/me} validates it
 * identically. Finally redirect to the SPA root; the frontend calls
 * {@code /api/auth/me} on mount to pick up the session.
 */
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository users;
    private final PatientRepository patients;
    private final PasswordEncoder passwords;
    private final JwtTokenProvider tokens;
    private final SessionCookieFactory cookies;
    private final String frontendUrl;

    public OAuth2SuccessHandler(UserRepository users, PatientRepository patients, PasswordEncoder passwords,
                                JwtTokenProvider tokens, SessionCookieFactory cookies,
                                @Value("${aura.frontend-url}") String frontendUrl) {
        this.users = users;
        this.patients = patients;
        this.passwords = passwords;
        this.tokens = tokens;
        this.cookies = cookies;
        this.frontendUrl = frontendUrl;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String email = null;
        String name = null;

        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser) {           // Google + "openid" scope -> OIDC user
            email = oidcUser.getEmail();
            name = oidcUser.getFullName();
        } else if (principal instanceof OAuth2User oAuth2User) { // fallback: plain OAuth2 user
            email = oAuth2User.getAttribute("email");
            name = oAuth2User.getAttribute("name");
        }

        if (email == null || email.isBlank()) {
            // Google returned no email — treat as an OAuth failure.
            response.sendRedirect(frontendUrl + "/?error=oauth");
            return;
        }

        final String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        final String displayName = (name == null || name.isBlank()) ? "AuraHealth Member" : name.trim();

        User user = users.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> createGoogleUser(normalizedEmail, displayName));

        // Same token + same cookie as email/password login (see AuthController#authenticated).
        String token = tokens.generateToken(user.getEmail(), List.of("ROLE_" + user.getRole().name()));
        cookies.issue(response, token);

        response.sendRedirect(frontendUrl + "/");
    }

    /** First-time Google sign-in: create the user (PATIENT) + patient profile, mirroring registration. */
    private User createGoogleUser(String email, String name) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(name);
        // Random hash: the column is non-null, but Google users never authenticate via password.
        user.setPasswordHash(passwords.encode(UUID.randomUUID().toString()));
        user.setRole(User.Role.PATIENT);
        user.setProvider(User.AuthProvider.GOOGLE);
        user = users.save(user);

        Patient patient = new Patient();
        patient.setUserId(user.getId());
        patient.setName(user.getFullName());
        patient.setEmail(user.getEmail());
        patient.setSubscriptionTier("FREE");
        patients.save(patient);

        return user;
    }
}
