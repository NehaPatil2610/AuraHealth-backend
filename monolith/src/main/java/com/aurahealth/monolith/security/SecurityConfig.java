package com.aurahealth.monolith.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
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

    /**
     * Optional: only present when GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET
     * are non-empty. When null, oauth2Login() is skipped so the app still
     * starts and all non-OAuth endpoints work.
     */
    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

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
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/**", "/error").permitAll()
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/doctors/add").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/doctors").hasAnyAuthority("ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_PATIENT")
                        .requestMatchers("/api/doctors/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_PATIENT")
                        .requestMatchers("/api/patients/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_PATIENT")
                        .requestMatchers("/api/appointments/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_PATIENT")
                        .requestMatchers("/api/billing/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_PATIENT")
                        .requestMatchers("/api/feedback/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_PATIENT")
                        .requestMatchers("/api/notifications/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_PATIENT")
                        .anyRequest().authenticated()
                );

<<<<<<< HEAD
        // Only wire Google OAuth2 login when credentials are actually provided.
        // Without valid GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET, Spring's
        // auto-config won't create a ClientRegistrationRepository, so calling
        // .oauth2Login() unconditionally would crash the app at startup.
=======
>>>>>>> 9b5e9a8e70bbfa073a6454f8feda8df9183f79ca
        if (clientRegistrationRepository != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .successHandler(oAuth2SuccessHandler)
                    .failureHandler(oAuth2FailureHandler)
            );
        }

        http.addFilterBefore(new JwtTokenFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

<<<<<<< HEAD
    /**
     * CORS for the SPA at the frontend origin with credentials enabled, so the
     * AURA_SESSION cookie is sent/accepted on /api/* XHR (credentials: 'include').
     *
     * Uses setAllowedOriginPatterns (not setAllowedOrigins) so we can use wildcards
     * while still setting allowCredentials=true — Spring rejects wildcard+credentials
     * with the plain setAllowedOrigins API.
     *
     * Covered origins:
     *  - https://aura-health-frontend-xi.vercel.app      (production canonical URL)
     *  - https://aura-health-frontend-*.vercel.app       (Vercel preview deployments)
     *  - http://localhost:5174                            (Vite dev server)
     */
=======
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
>>>>>>> 9b5e9a8e70bbfa073a6454f8feda8df9183f79ca
}

