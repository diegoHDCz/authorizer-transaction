package br.com.mini_authorizer.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CardPasswordHasherTest {

    private final CardPasswordHasher hasher = new CardPasswordHasher();

    @Test
    void shouldProduceBcryptHashInsteadOfRawPassword() {
        String hash = hasher.hash("1234");

        assertThat(hash).isNotEqualTo("1234").doesNotContain("1234").startsWith("$2");
    }

    @Test
    void shouldSaltEachHashSoEqualPasswordsProduceDifferentHashes() {
        assertThat(hasher.hash("1234")).isNotEqualTo(hasher.hash("1234"));
    }

    @Test
    void shouldMatchRawPasswordAgainstItsHash() {
        assertThat(hasher.matches("1234", hasher.hash("1234"))).isTrue();
    }

    @Test
    void shouldNotMatchWrongPassword() {
        assertThat(hasher.matches("4321", hasher.hash("1234"))).isFalse();
    }
}
