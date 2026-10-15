package br.com.mini_authorizer;

import br.com.mini_authorizer.domain.CardTransaction;
import br.com.mini_authorizer.domain.RejectionReason;
import br.com.mini_authorizer.domain.Transaction;
import br.com.mini_authorizer.domain.TransactionStatus;
import br.com.mini_authorizer.exception.TransactionRejectedException;
import br.com.mini_authorizer.repository.CardRepository;
import br.com.mini_authorizer.repository.CardTransactionRepository;
import br.com.mini_authorizer.service.TransactionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reproduces the evaluation script of the README against the real application (H2), plus concurrent debits.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TransactionIntegrationTest {

    private static final String CARD_NUMBER = "4444555566667777";
    private static final String PASSWORD = "1234";
    private static final String UNKNOWN_CARD_NUMBER = "1111222233334444";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CardTransactionRepository cardTransactionRepository;

    @Autowired
    private TransactionService transactionService;

    @AfterEach
    void deleteCardAndItsTransactions() {
        cardTransactionRepository.deleteAll(cardTransactionRepository.findByCardNumberOrderByCreatedAtAscIdAsc(CARD_NUMBER));
        cardTransactionRepository.deleteAll(cardTransactionRepository.findByCardNumberOrderByCreatedAtAscIdAsc(UNKNOWN_CARD_NUMBER));
        cardRepository.deleteById(CARD_NUMBER);
    }

    @Test
    void shouldFollowTheEvaluationScriptUntilEachRejectionReason() throws Exception {
        mockMvc.perform(post("/cartoes")
                        .with(httpBasic("username", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroCartao\":\"%s\",\"senha\":\"%s\"}".formatted(CARD_NUMBER, PASSWORD)))
                .andExpect(status().isCreated());
        assertBalance("500.00");

        authorize(CARD_NUMBER, PASSWORD, "200.00").andExpect(status().isCreated()).andExpect(content().string("OK"));
        assertBalance("300.00");
        authorize(CARD_NUMBER, PASSWORD, "295.15").andExpect(status().isCreated()).andExpect(content().string("OK"));
        assertBalance("4.85");

        authorize(CARD_NUMBER, PASSWORD, "4.86")
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().string("SALDO_INSUFICIENTE"));
        assertBalance("4.85");

        authorize(CARD_NUMBER, "9999", "1.00")
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().string("SENHA_INVALIDA"));
        assertBalance("4.85");

        authorize(UNKNOWN_CARD_NUMBER, PASSWORD, "1.00")
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().string("CARTAO_INEXISTENTE"));

        // Every attempt is recorded, rejections included (they are not rolled back)
        assertThat(cardTransactionRepository.findByCardNumberOrderByCreatedAtAscIdAsc(CARD_NUMBER))
                .extracting(CardTransaction::getStatus, CardTransaction::getRejectionReason,
                        CardTransaction::getAmount, CardTransaction::getBalanceAfter)
                .containsExactly(
                        tuple(TransactionStatus.APPROVED, null, new BigDecimal("200.00"), new BigDecimal("300.00")),
                        tuple(TransactionStatus.APPROVED, null, new BigDecimal("295.15"), new BigDecimal("4.85")),
                        tuple(TransactionStatus.REJECTED, RejectionReason.SALDO_INSUFICIENTE, new BigDecimal("4.86"), null),
                        tuple(TransactionStatus.REJECTED, RejectionReason.SENHA_INVALIDA, new BigDecimal("1.00"), null));
        assertThat(cardTransactionRepository.findByCardNumberOrderByCreatedAtAscIdAsc(UNKNOWN_CARD_NUMBER))
                .singleElement()
                .extracting(CardTransaction::getRejectionReason)
                .isEqualTo(RejectionReason.CARTAO_INEXISTENTE);
    }

    @Test
    void shouldNeverDebitMoreThanTheBalanceUnderConcurrentTransactions() throws Exception {
        mockMvc.perform(post("/cartoes")
                        .with(httpBasic("username", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardNumber\":\"%s\",\"password\":\"%s\"}".formatted(CARD_NUMBER, PASSWORD)))
                .andExpect(status().isCreated());

        int attempts = 20;
        Transaction transaction = new Transaction(CARD_NUMBER, PASSWORD, new BigDecimal("50.00"));
        CountDownLatch start = new CountDownLatch(1);
        Callable<String> authorizeAfterStart = () -> {
            start.await();
            try {
                transactionService.authorize(transaction);
                return "OK";
            } catch (TransactionRejectedException exception) {
                return exception.getReason().name();
            }
        };

        List<String> results;
        try (ExecutorService executor = Executors.newFixedThreadPool(attempts)) {
            List<Future<String>> futures = IntStream.range(0, attempts)
                    .mapToObj(i -> executor.submit(authorizeAfterStart))
                    .toList();
            start.countDown();
            results = futures.stream().map(TransactionIntegrationTest::await).toList();
        }

        // 500.00 / 50.00: exactly 10 debits fit, the other 10 must see the updated balance
        assertThat(results).filteredOn("OK"::equals).hasSize(10);
        assertThat(results).filteredOn(RejectionReason.SALDO_INSUFICIENTE.name()::equals).hasSize(10);
        assertThat(cardRepository.findById(CARD_NUMBER).orElseThrow().getBalance())
                .isEqualByComparingTo(BigDecimal.ZERO);
        List<CardTransaction> recorded = cardTransactionRepository.findByCardNumberOrderByCreatedAtAscIdAsc(CARD_NUMBER);
        assertThat(recorded).filteredOn(t -> t.getStatus() == TransactionStatus.APPROVED).hasSize(10);
        assertThat(recorded).filteredOn(t -> t.getStatus() == TransactionStatus.REJECTED).hasSize(10);
        // Serialized debits: the approved records carry 450.00, 400.00, ..., 0.00 - no balance seen twice
        assertThat(recorded).filteredOn(t -> t.getStatus() == TransactionStatus.APPROVED)
                .extracting(CardTransaction::getBalanceAfter)
                .map(BigDecimal::stripTrailingZeros)
                .doesNotHaveDuplicates();
    }

    private ResultActions authorize(String cardNumber, String password, String amount) throws Exception {
        return mockMvc.perform(post("/transacoes")
                .with(httpBasic("username", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"numeroCartao\":\"%s\",\"senhaCartao\":\"%s\",\"valor\":%s}"
                        .formatted(cardNumber, password, amount)));
    }

    private void assertBalance(String expected) throws Exception {
        mockMvc.perform(get("/cartoes/{cardNumber}", CARD_NUMBER).with(httpBasic("username", "password")))
                .andExpect(status().isOk())
                .andExpect(content().string(expected));
    }

    private static String await(Future<String> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
