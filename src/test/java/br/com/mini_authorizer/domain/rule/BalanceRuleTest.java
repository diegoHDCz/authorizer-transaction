package br.com.mini_authorizer.domain.rule;

import br.com.mini_authorizer.domain.Card;
import br.com.mini_authorizer.domain.CardPasswordHasher;
import br.com.mini_authorizer.domain.RejectionReason;
import br.com.mini_authorizer.domain.Transaction;
import br.com.mini_authorizer.exception.TransactionRejectedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BalanceRuleTest {

    private static final String CARD_NUMBER = "6549873025634501";

    private final BalanceRule rule = new BalanceRule();
    private final Card card = Card.create(CARD_NUMBER, "1234", new CardPasswordHasher());

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "10.00", "500.00"})
    void shouldBeSatisfiedWhenBalanceCoversAmount(String amount) {
        Transaction transaction = new Transaction(CARD_NUMBER, "1234", new BigDecimal(amount));

        assertThat(rule.isSatisfiedBy(card, transaction)).isTrue();
        assertThatCode(() -> rule.validate(card, transaction)).doesNotThrowAnyException();
    }

    @Test
    void shouldReturnSaldoInsuficienteWhenBalanceIsLowerThanAmount() {
        Transaction transaction = new Transaction(CARD_NUMBER, "1234", new BigDecimal("500.01"));

        assertThat(rule.isSatisfiedBy(card, transaction)).isFalse();
        assertThatThrownBy(() -> rule.validate(card, transaction))
                .isInstanceOfSatisfying(TransactionRejectedException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(RejectionReason.SALDO_INSUFICIENTE));
    }
}
