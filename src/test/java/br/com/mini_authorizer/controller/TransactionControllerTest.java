package br.com.mini_authorizer.controller;

import br.com.mini_authorizer.config.SecurityConfig;
import br.com.mini_authorizer.domain.RejectionReason;
import br.com.mini_authorizer.domain.Transaction;
import br.com.mini_authorizer.exception.TransactionRejectedException;
import br.com.mini_authorizer.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import(SecurityConfig.class)
class TransactionControllerTest {

    private static final String CARD_NUMBER = "6549873025634501";
    private static final String BODY = """
            {"cardNumber":"6549873025634501","password":"1234","amount":10.00}""";
    private static final Transaction TRANSACTION = new Transaction(CARD_NUMBER, "1234", new BigDecimal("10.00"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void shouldReturnCreatedWithOkBodyWhenTransactionIsAuthorized() throws Exception {
        mockMvc.perform(authorize(BODY))
                .andExpect(status().isCreated())
                .andExpect(content().string("OK"));

        verify(transactionService).authorize(TRANSACTION);
    }

    @Test
    void shouldAcceptOriginalContractFieldNamesAsAliases() throws Exception {
        mockMvc.perform(authorize("{\"numeroCartao\":\"6549873025634501\",\"senhaCartao\":\"1234\",\"valor\":10.00}"))
                .andExpect(status().isCreated())
                .andExpect(content().string("OK"));

        verify(transactionService).authorize(TRANSACTION);
    }

    @ParameterizedTest
    @EnumSource(RejectionReason.class)
    void shouldReturnUnprocessableEntityWithRuleThatBarredTheTransaction(RejectionReason reason) throws Exception {
        doThrow(new TransactionRejectedException(reason)).when(transactionService).authorize(any());

        mockMvc.perform(authorize(BODY))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().string(reason.name()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"password\":\"1234\",\"amount\":10.00}",
            "{\"cardNumber\":\" \",\"password\":\"1234\",\"amount\":10.00}",
            "{\"cardNumber\":\"12345678901234567890\",\"password\":\"1234\",\"amount\":10.00}",
            "{\"cardNumber\":\"6549873025634501\",\"amount\":10.00}",
            "{\"cardNumber\":\"6549873025634501\",\"password\":\"\",\"amount\":10.00}",
            "{\"cardNumber\":\"6549873025634501\",\"password\":\"1234\"}",
            "{\"cardNumber\":\"6549873025634501\",\"password\":\"1234\",\"amount\":0}",
            "{\"cardNumber\":\"6549873025634501\",\"password\":\"1234\",\"amount\":-10.00}",
            "{\"cardNumber\":\"6549873025634501\",\"password\":\"1234\",\"amount\":10.001}"
    })
    void shouldReturnBadRequestWithoutCallingServiceWhenPayloadIsInvalid(String body) throws Exception {
        mockMvc.perform(authorize(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    @Test
    void shouldTellWhichFieldIsInvalidWhenAmountIsNotPositive() throws Exception {
        mockMvc.perform(authorize("{\"numeroCartao\":\"6549873025634501\",\"senhaCartao\":\"1234\",\"valor\":-10.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.length()").value(1))
                .andExpect(jsonPath("$.fields[0].field").value("amount"))
                .andExpect(jsonPath("$.fields[0].message").isNotEmpty());

        verifyNoInteractions(transactionService);
    }

    @Test
    void shouldReturnBadRequestWithReasonWhenAmountHasWrongType() throws Exception {
        mockMvc.perform(authorize("{\"numeroCartao\":\"6549873025634501\",\"senhaCartao\":\"1234\",\"valor\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request body or field with wrong type."));

        verifyNoInteractions(transactionService);
    }

    @Test
    void shouldReturnUnauthorizedWhenAuthorizingTransactionWithoutCredentials() throws Exception {
        mockMvc.perform(post("/transacoes").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(transactionService);
    }

    @Test
    void shouldReturnUnauthorizedWhenCredentialsAreWrong() throws Exception {
        mockMvc.perform(post("/transacoes")
                        .with(httpBasic("username", "wrong"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(transactionService);
    }

    private MockHttpServletRequestBuilder authorize(String body) {
        return post("/transacoes")
                .with(httpBasic("username", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
