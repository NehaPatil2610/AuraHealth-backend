package com.aurahealth.monolith.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * PasswordEncoder lives in its own config — NOT in SecurityConfig — to avoid a
 * bean-creation cycle:
 *   SecurityConfig -> OAuth2SuccessHandler -> PasswordEncoder
 * If PasswordEncoder were a @Bean method on SecurityConfig, Spring would need a
 * fully-built SecurityConfig to produce it, but SecurityConfig can't finish
 * building until that same PasswordEncoder exists.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
