package br.com.mini_authorizer.service;

import br.com.mini_authorizer.domain.Card;
import br.com.mini_authorizer.domain.CardPasswordHasher;
import br.com.mini_authorizer.domain.CardTransaction;
import br.com.mini_authorizer.domain.RejectionReason;
import br.com.mini_authorizer.domain.Transaction;
import br.com.mini_authorizer.domain.TransactionStatus;
import br.com.mini_authorizer.domain.rule.BalanceRule;
import br.com.mini_authorizer.domain.rule.PasswordRule;
import br.com.mini_authorizer.exception.TransactionRejectedException;
import br.com.mini_authorizer.repository.CardRepository;
import br.com.mini_authorizer.repository.CardTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final String CARD_NUMBER = "6549873025634501";
    private static final String PASSWORD = "1234";

    private final CardPasswordHasher hasher = new CardPasswordHasher();

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardTransactionRepository cardTransactionRepository;

    private TransactionService transactionService;
    private Card card;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(cardRepository, cardTransactionRepository,
                List.of(new PasswordRule(hasher), new BalanceRule()));
        card = Card.create(CARD_NUMBER, PASSWORD, hasher);
    }

    @Test
    void shouldDebitAmountAndRecordApprovedTransactionWithBalanceAfter() {
        when(cardRepository.findByNumberForUpdate(CARD_NUMBER)).thenReturn(Optional.of(card));

        transactionService.authorize(new Transaction(CARD_NUMBER, PASSWORD, new BigDecimal("10.00")));

        assertThat(card.getBalance()).isEqualTo(new BigDecimal("490.00"));
        CardTransaction recorded = recordedTransaction();
        assertThat(recorded.getCardNumber()).isEqualTo(CARD_NUMBER);
        assertThat(recorded.getAmount()).isEqualTo(new BigDecimal("10.00"));
        assertThat(recorded.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(recorded.getRejectionReason()).isNull();
        assertThat(recorded.getBalanceAfter()).isEqualTo(new BigDecimal("490.00"));
    }

    @Test
    void shouldAuthorizeTransactionThatUsesTheWholeBalance() {
        when(cardRepository.findByNumberForUpdate(CARD_NUMBER)).thenReturn(Optional.of(card));

        transactionService.authorize(new Transaction(CARD_NUMBER, PASSWORD, new BigDecimal("500.00")));

        assertThat(card.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(recordedTransaction().getStatus()).isEqualTo(TransactionStatus.APPROVED);
    }

    @Test
    void shouldReturnAndRecordCartaoInexistenteWhenCardDoesNotExist() {
        when(cardRepository.findByNumberForUpdate(CARD_NUMBER)).thenReturn(Optional.empty());

        assertRejectedAndRecorded(new Transaction(CARD_NUMBER, PASSWORD, BigDecimal.TEN),
                RejectionReason.CARTAO_INEXISTENTE);
    }

    @Test
    void shouldReturnAndRecordSenhaInvalidaWithoutDebitingWhenPasswordIsWrong() {
        when(cardRepository.findByNumberForUpdate(CARD_NUMBER)).thenReturn(Optional.of(card));

        assertRejectedAndRecorded(new Transaction(CARD_NUMBER, "4321", BigDecimal.TEN), RejectionReason.SENHA_INVALIDA);
        assertThat(card.getBalance()).isEqualTo(new BigDecimal("500.00"));
    }

    @Test
    void shouldReturnAndRecordSaldoInsuficienteWithoutDebitingWhenBalanceIsLowerThanAmount() {
        when(cardRepository.findByNumberForUpdate(CARD_NUMBER)).thenReturn(Optional.of(card));

        assertRejectedAndRecorded(new Transaction(CARD_NUMBER, PASSWORD, new BigDecimal("500.01")),
                RejectionReason.SALDO_INSUFICIENTE);
        assertThat(card.getBalance()).isEqualTo(new BigDecimal("500.00"));
    }

    @Test
    void shouldCheckPasswordBeforeBalanceWhenBothRulesFail() {
        when(cardRepository.findByNumberForUpdate(CARD_NUMBER)).thenReturn(Optional.of(card));

        assertRejectedAndRecorded(new Transaction(CARD_NUMBER, "4321", new BigDecimal("500.01")),
                RejectionReason.SENHA_INVALIDA);
    }

    private void assertRejectedAndRecorded(Transaction transaction, RejectionReason reason) {
        assertThatThrownBy(() -> transactionService.authorize(transaction))
                .isInstanceOfSatisfying(TransactionRejectedException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(reason));

        CardTransaction recorded = recordedTransaction();
        assertThat(recorded.getCardNumber()).isEqualTo(transaction.cardNumber());
        assertThat(recorded.getAmount()).isEqualTo(transaction.amount());
        assertThat(recorded.getStatus()).isEqualTo(TransactionStatus.REJECTED);
        assertThat(recorded.getRejectionReason()).isEqualTo(reason);
        assertThat(recorded.getBalanceAfter()).isNull();
    }

    private CardTransaction recordedTransaction() {
        ArgumentCaptor<CardTransaction> captor = ArgumentCaptor.forClass(CardTransaction.class);
        verify(cardTransactionRepository).save(captor.capture());
        return captor.getValue();
    }
}
