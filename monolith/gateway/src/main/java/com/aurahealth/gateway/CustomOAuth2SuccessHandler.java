package com.aurahealth.gateway;

import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.DefaultServerRedirectStrategy;
import org.springframework.security.web.server.ServerRedirectStrategy;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class CustomOAuth2SuccessHandler implements ServerAuthenticationSuccessHandler {

    private final ServerRedirectStrategy redirectStrategy = new DefaultServerRedirectStrategy();
    private final GatewayJwtUtil jwtUtil;
    private final WebClient webClient;
    private static final String FRONTEND_REDIRECT_URL = "http://localhost:5174";

    public CustomOAuth2SuccessHandler(GatewayJwtUtil jwtUtil, @Value("${aura.internal.oauth-key}") String internalOauthKey) {
        this.jwtUtil = jwtUtil;
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:8080")
                .defaultHeader("X-Aura-Internal-Key", internalOauthKey)
                .build();
    }

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        Object principal = authentication.getPrincipal();
        String email = null;
        String name = null;

        if (principal instanceof OidcUser oidcUser) {
            email = oidcUser.getEmail();
            name = oidcUser.getFullName();
        } else if (principal instanceof OAuth2User oAuth2User) {
            email = oAuth2User.getAttribute("email");
            name = oAuth2User.getAttribute("name");
        }

        if (email == null || email.isBlank()) {
            return redirectStrategy.sendRedirect(webFilterExchange.getExchange(), URI.create(FRONTEND_REDIRECT_URL + "?authError=google_email_missing"));
        }

        final String finalEmail = email;
        final String finalName = name != null ? name : "AuraHealth User";
        Map<String, String> body = Map.of("email", finalEmail, "name", finalName);

        return webClient.post()
                .uri("/api/auth/oauth-provision")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(responseMap -> {
                    String role = (String) responseMap.get("role");
                    if (role == null) return Mono.error(new IllegalStateException("OAuth provisioning returned no role"));

                    // Ensure ROLE_ prefix for Spring Security authority matching
                    String authorityRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;

                    String jwt = jwtUtil.generateToken(finalEmail, finalName, List.of(authorityRole));

                    ResponseCookie sessionCookie = ResponseCookie.from("AURA_SESSION", jwt)
                            .httpOnly(true)
                            .secure(false)
                            .path("/")
                            .maxAge(Duration.ofHours(24))
                            .sameSite("Lax")
                            .build();

                    webFilterExchange.getExchange().getResponse().addCookie(sessionCookie);
                    return this.redirectStrategy.sendRedirect(webFilterExchange.getExchange(), URI.create(FRONTEND_REDIRECT_URL));
                })
                .onErrorResume(e -> redirectStrategy.sendRedirect(webFilterExchange.getExchange(), URI.create(FRONTEND_REDIRECT_URL + "?authError=oauth_provisioning_failed")));
    }
}
