package com.aurahealth.monolith.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for the AURA_SESSION cookie.
 *
 * <p>Used by BOTH the email/password {@code AuthController} and the Google
 * OAuth2 success handler so a session issued by either path is byte-for-byte
 * identical (name, flags, path, max-age, SameSite) and therefore validated the
 * same way by {@link JwtTokenFilter}. Keeping this in one place prevents the
 * "OAuth succeeds but the user lands logged out" class of bug caused by the two
 * paths drifting apart.
 */
@Component
public class SessionCookieFactory {

    /** Cookie name the JwtTokenFilter reads the JWT from. */
    public static final String COOKIE_NAME = "AURA_SESSION";

    /** 24h — matches the JWT validity in JwtTokenProvider. */
    private static final int MAX_AGE_SECONDS = 86_400;

    private final boolean secure;

    public SessionCookieFactory(@Value("${aura.cookie.secure:false}") boolean secure) {
        this.secure = secure;
    }

    /** Attach the session cookie carrying the signed JWT to the response. */
    public void issue(HttpServletResponse response, String token) {
        response.addCookie(build(token, MAX_AGE_SECONDS));
    }

    /** Expire the session cookie (logout). */
    public void clear(HttpServletResponse response) {
        response.addCookie(build("", 0));
    }

    private Cookie build(String value, int maxAge) {
        Cookie cookie = new Cookie(COOKIE_NAME, value);
        cookie.setHttpOnly(true);
        // Secure only in prod (https); false in local dev (http). Driven by aura.cookie.secure.
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        // Lax lets the cookie ride the top-level OAuth redirect back from Google.
        // Do NOT use Strict — it would be dropped on that cross-site navigation.
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }
}
