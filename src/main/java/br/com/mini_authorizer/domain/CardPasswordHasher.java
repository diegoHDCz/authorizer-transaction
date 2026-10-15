package br.com.mini_authorizer.domain;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Hashes and checks card passwords with BCrypt - ADR-0005.
 * <p>
 * Deliberately not exposed as a {@code PasswordEncoder} bean: Spring Boot would pick it up for the in-memory
 * HTTP Basic user ({@code spring.security.user.password}) and the plain-text API password would stop matching.
 */
@Component
public class CardPasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String hashedPassword) {
        return encoder.matches(rawPassword, hashedPassword);
    }
}
