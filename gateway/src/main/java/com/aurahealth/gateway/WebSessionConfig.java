package com.aurahealth.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;
import reactor.core.publisher.Mono;

/**
 * Explicit WebSession configuration for Spring Cloud Gateway.
 *
 * ROOT CAUSE FIX for the OAuth2 500 error:
 * Without this config, the default InMemoryWebSessionStore works but the
 * session ID cookie is not set with SameSite=Lax. During the OAuth2 redirect
 * flow (frontend → gateway → Google → gateway callback), the browser drops
 * the session cookie on the callback because it's a cross-site redirect.
 * This causes the OAuth2AuthorizationRequestRepository to fail to find the
 * saved authorization request, resulting in a 500 error.
 *
 * The fix: explicitly configure a CookieWebSessionIdResolver with SameSite=Lax.
 */
@Configuration
public class WebSessionConfig {

    @Bean
    public CookieWebSessionIdResolver cookieWebSessionIdResolver() {
        CookieWebSessionIdResolver resolver = new CookieWebSessionIdResolver();
        resolver.setCookieName("AURA_GATEWAY_SESSION");
        resolver.addCookieInitializer(builder ->
                builder.sameSite("Lax")
                        .httpOnly(true)
                        .path("/")
                        .secure(false) // Set to true in production with HTTPS
        );
        return resolver;
    }
}
