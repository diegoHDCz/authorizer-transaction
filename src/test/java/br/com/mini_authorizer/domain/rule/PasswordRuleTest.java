package br.com.mini_authorizer.domain.rule;

import br.com.mini_authorizer.domain.Card;
import br.com.mini_authorizer.domain.CardPasswordHasher;
import br.com.mini_authorizer.domain.RejectionReason;
import br.com.mini_authorizer.domain.Transaction;
import br.com.mini_authorizer.exception.TransactionRejectedException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordRuleTest {

    private static final String CARD_NUMBER = "6549873025634501";

    private final CardPasswordHasher hasher = new CardPasswordHasher();
    private final PasswordRule rule = new PasswordRule(hasher);
    private final Card card = Card.create(CARD_NUMBER, "1234", hasher);

    @Test
    void shouldBeSatisfiedWhenPasswordMatches() {
        Transaction transaction = new Transaction(CARD_NUMBER, "1234", BigDecimal.TEN);

        assertThat(rule.isSatisfiedBy(card, transaction)).isTrue();
        assertThatCode(() -> rule.validate(card, transaction)).doesNotThrowAnyException();
    }

    @Test
    void shouldReturnSenhaInvalidaWhenPasswordDoesNotMatch() {
        Transaction transaction = new Transaction(CARD_NUMBER, "4321", BigDecimal.TEN);

        assertThat(rule.isSatisfiedBy(card, transaction)).isFalse();
        assertThatThrownBy(() -> rule.validate(card, transaction))
                .isInstanceOfSatisfying(TransactionRejectedException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(RejectionReason.SENHA_INVALIDA));
    }
}
