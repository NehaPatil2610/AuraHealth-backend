package com.aurahealth.monolith.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Any OAuth2 failure (user denied consent, token exchange error, missing email)
 * redirects back to the SPA login screen with {@code ?error=oauth}, which the
 * frontend reads to show an error message.
 */
@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    private final String frontendUrl;

    public OAuth2FailureHandler(@Value("${aura.frontend-url}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        response.sendRedirect(frontendUrl + "/?error=oauth");
    }
}
