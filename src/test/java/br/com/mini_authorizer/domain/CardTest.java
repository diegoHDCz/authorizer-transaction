package br.com.mini_authorizer.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CardTest {

    private final CardPasswordHasher hasher = new CardPasswordHasher();

    @Test
    void shouldCreateCardWithInitialBalanceOfFiveHundred() {
        Card card = Card.create("6549873025634501", "1234", hasher);

        assertThat(card.getNumber()).isEqualTo("6549873025634501");
        assertThat(card.getId()).isEqualTo("6549873025634501");
        assertThat(card.getBalance()).isEqualTo(new BigDecimal("500.00"));
    }

    @Test
    void shouldKeepMonetaryScaleOfTwoOnInitialBalance() {
        assertThat(Card.create("6549873025634501", "1234", hasher).getBalance().scale()).isEqualTo(2);
    }

    @Test
    void shouldMatchOnlyThePasswordItWasCreatedWith() {
        Card card = Card.create("6549873025634501", "1234", hasher);

        assertThat(card.passwordMatches("1234", hasher)).isTrue();
        assertThat(card.passwordMatches("4321", hasher)).isFalse();
    }

    @Test
    void shouldBeNewUntilPersisted() {
        Card card = Card.create("6549873025634501", "1234", hasher);

        assertThat(card.isNew()).isTrue();

        card.markAsPersisted();

        assertThat(card.isNew()).isFalse();
    }
}
