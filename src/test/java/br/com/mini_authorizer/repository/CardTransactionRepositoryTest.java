package br.com.mini_authorizer.repository;

import br.com.mini_authorizer.domain.CardTransaction;
import br.com.mini_authorizer.domain.RejectionReason;
import br.com.mini_authorizer.domain.Transaction;
import br.com.mini_authorizer.domain.TransactionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
class CardTransactionRepositoryTest {

    private static final String CARD_NUMBER = "6549873025634501";

    @Autowired
    private CardTransactionRepository cardTransactionRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldPersistApprovedAndRejectedAttemptsInOrderWithTimestamp() {
        Transaction transaction = new Transaction(CARD_NUMBER, "1234", new BigDecimal("10.00"));
        cardTransactionRepository.saveAndFlush(CardTransaction.approved(transaction, new BigDecimal("490.00")));
        cardTransactionRepository.saveAndFlush(CardTransaction.rejected(transaction, RejectionReason.SENHA_INVALIDA));
        cardTransactionRepository.saveAndFlush(CardTransaction.rejected(
                new Transaction("1111222233334444", "1234", BigDecimal.ONE), RejectionReason.CARTAO_INEXISTENTE));
        entityManager.clear();

        List<CardTransaction> found = cardTransactionRepository.findByCardNumberOrderByCreatedAtAscIdAsc(CARD_NUMBER);

        assertThat(found)
                .extracting(CardTransaction::getStatus, CardTransaction::getRejectionReason,
                        CardTransaction::getAmount, CardTransaction::getBalanceAfter)
                .containsExactly(
                        tuple(TransactionStatus.APPROVED, null, new BigDecimal("10.00"), new BigDecimal("490.00")),
                        tuple(TransactionStatus.REJECTED, RejectionReason.SENHA_INVALIDA, new BigDecimal("10.00"), null));
        assertThat(found).allSatisfy(recorded -> {
            assertThat(recorded.getId()).isNotNull().extracting(UUID::version).isEqualTo(7);
            assertThat(recorded.getCreatedAt()).isNotNull();
        });
    }

    @Test
    void shouldRecordAttemptOnUnknownCardWithoutForeignKeyToCard() {
        cardTransactionRepository.saveAndFlush(CardTransaction.rejected(
                new Transaction("1111222233334444", "1234", BigDecimal.ONE), RejectionReason.CARTAO_INEXISTENTE));
        entityManager.clear();

        assertThat(cardTransactionRepository.findByCardNumberOrderByCreatedAtAscIdAsc("1111222233334444"))
                .singleElement()
                .extracting(CardTransaction::getRejectionReason)
                .isEqualTo(RejectionReason.CARTAO_INEXISTENTE);
    }
}
