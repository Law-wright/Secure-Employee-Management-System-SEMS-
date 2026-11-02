package edu.sems.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderTest {

    @Test
    void passwordEncoder_matchesHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("test-password-1");

        assertThat(encoder.matches("test-password-1", hash)).isTrue();
        assertThat(encoder.matches("other-password-1", hash)).isFalse();
        assertThat(hash).doesNotContain("test-password-1");
    }
}
