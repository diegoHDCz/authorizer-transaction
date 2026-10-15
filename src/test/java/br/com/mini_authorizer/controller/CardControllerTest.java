package br.com.mini_authorizer.controller;

import br.com.mini_authorizer.config.SecurityConfig;
import br.com.mini_authorizer.domain.Card;
import br.com.mini_authorizer.domain.CardPasswordHasher;
import br.com.mini_authorizer.exception.CardAlreadyExistsException;
import br.com.mini_authorizer.exception.CardNotFoundException;
import br.com.mini_authorizer.service.CardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CardController.class)
@Import(SecurityConfig.class)
class CardControllerTest {

    private static final String CARD_NUMBER = "6549873025634501";
    private static final String PASSWORD = "1234";
    private static final String BODY = """
            {"cardNumber":"6549873025634501","password":"1234"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardService cardService;

    @Test
    void shouldReturnCreatedWithRequestDataWhenCardIsCreated() throws Exception {
        when(cardService.create(CARD_NUMBER, PASSWORD)).thenReturn(Card.create(CARD_NUMBER, PASSWORD, new CardPasswordHasher()));

        mockMvc.perform(createCard(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardNumber").value(CARD_NUMBER))
                .andExpect(jsonPath("$.password").value(PASSWORD));
    }

    @Test
    void shouldReturnUnprocessableEntityWithRequestDataWhenCardAlreadyExists() throws Exception {
        when(cardService.create(CARD_NUMBER, PASSWORD)).thenThrow(new CardAlreadyExistsException(CARD_NUMBER, PASSWORD));

        mockMvc.perform(createCard(BODY))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.cardNumber").value(CARD_NUMBER))
                .andExpect(jsonPath("$.password").value(PASSWORD));
    }

    @Test
    void shouldAcceptOriginalContractFieldNamesAsAliases() throws Exception {
        when(cardService.create(CARD_NUMBER, PASSWORD)).thenReturn(Card.create(CARD_NUMBER, PASSWORD, new CardPasswordHasher()));

        mockMvc.perform(createCard("{\"numeroCartao\":\"6549873025634501\",\"senha\":\"1234\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardNumber").value(CARD_NUMBER))
                .andExpect(jsonPath("$.password").value(PASSWORD));
    }

    @Test
    void shouldEchoRequestDataInEnglishWhenCardAlreadyExists() throws Exception {
        when(cardService.create(CARD_NUMBER, PASSWORD)).thenThrow(new CardAlreadyExistsException(CARD_NUMBER, PASSWORD));

        mockMvc.perform(createCard("{\"numeroCartao\":\"6549873025634501\",\"senha\":\"1234\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.cardNumber").value(CARD_NUMBER))
                .andExpect(jsonPath("$.numeroCartao").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"password\":\"1234\"}",
            "{\"cardNumber\":\"6549873025634501\"}",
            "{\"cardNumber\":\"\",\"password\":\"1234\"}",
            "{\"cardNumber\":\"6549873025634501\",\"password\":\" \"}",
            "{\"cardNumber\":\"6549-8730-2563-4501\",\"password\":\"1234\"}",
            "{\"cardNumber\":\"123\",\"password\":\"1234\"}",
            "{\"cardNumber\":\"12345678901234567890\",\"password\":\"1234\"}",
            "{\"cardNumber\":\"6549873025634501\",\"password\":\"%s\"}"
    })
    void shouldReturnBadRequestWithoutCallingServiceWhenPayloadIsInvalid(String body) throws Exception {
        // %s: password longer than the 72 bytes BCrypt can hash
        mockMvc.perform(createCard(body.formatted("1".repeat(73))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cardService);
    }

    @Test
    void shouldReturnUnauthorizedWhenCreatingCardWithoutCredentials() throws Exception {
        mockMvc.perform(post("/cartoes").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(cardService);
    }

    @Test
    void shouldReturnBalanceAsPlainNumberWhenCardExists() throws Exception {
        when(cardService.getBalance(CARD_NUMBER)).thenReturn(new BigDecimal("495.15"));

        mockMvc.perform(get("/cartoes/{cardNumber}", CARD_NUMBER).with(httpBasic("username", "password")))
                .andExpect(status().isOk())
                .andExpect(content().string("495.15"));
    }

    @Test
    void shouldReturnNotFoundWithoutBodyWhenCardDoesNotExist() throws Exception {
        when(cardService.getBalance(anyString())).thenThrow(new CardNotFoundException(CARD_NUMBER));

        mockMvc.perform(get("/cartoes/{cardNumber}", CARD_NUMBER).with(httpBasic("username", "password")))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    void shouldReturnUnauthorizedWhenQueryingBalanceWithoutCredentials() throws Exception {
        mockMvc.perform(get("/cartoes/{cardNumber}", CARD_NUMBER))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(cardService);
    }

    private MockHttpServletRequestBuilder createCard(String body) {
        return post("/cartoes")
                .with(httpBasic("username", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
