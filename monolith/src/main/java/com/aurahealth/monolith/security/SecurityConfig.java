package com.aurahealth.monolith.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider,
                          OAuth2SuccessHandler oAuth2SuccessHandler,
                          OAuth2FailureHandler oAuth2FailureHandler) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.oAuth2FailureHandler = oAuth2FailureHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                // IF_REQUIRED (not STATELESS): the OAuth2 authorization-code handshake needs a
                // short-lived session to hold the authorization request between the redirect to
                // Google and the /login/oauth2/code/google callback. API requests still create no
                // session — auth comes from the AURA_SESSION JWT cookie via JwtTokenFilter.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        // Public auth endpoints (register, login, me, logout, mock-bypass)
                        .requestMatchers("/api/auth/**").permitAll()

                        // Google OAuth2 entrypoint + callback (Spring Security defaults, at ROOT).
                        .requestMatchers("/oauth2/**", "/login/**", "/error").permitAll()

                        // Admin-only endpoints
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")

                        // Doctor endpoints
                        .requestMatchers("/api/doctors/add").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/doctors").hasAnyAuthority("ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_PATIENT")
                        .requestMatchers("/api/doctors/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_PATIENT")

                        // Patient endpoints
                        .requestMatchers("/api/patients/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_PATIENT")

                        // Appointment endpoints (privacy-scoped by service layer)
                        .requestMatchers("/api/appointments/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_PATIENT")

                        // Billing endpoints (patient sees own invoices, admin sees all)
                        .requestMatchers("/api/billing/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_PATIENT")

                        // Feedback endpoints
                        .requestMatchers("/api/feedback/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_PATIENT")

                        // Notification endpoints
                        .requestMatchers("/api/notifications/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_PATIENT")

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                // "Continue with Google": /oauth2/authorization/google -> Google -> /login/oauth2/code/google.
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )
                .addFilterBefore(new JwtTokenFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS for the SPA at the frontend origin with credentials enabled, so the
     * AURA_SESSION cookie is sent/accepted on /api/* XHR (credentials: 'include').
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                "https://aura-health-frontend-xi.vercel.app",
                "https://aura-health-frontend-*.vercel.app",
                "http://localhost:5174"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Set-Cookie"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
