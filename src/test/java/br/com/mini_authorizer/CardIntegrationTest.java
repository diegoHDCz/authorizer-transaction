package br.com.mini_authorizer;

import br.com.mini_authorizer.repository.CardRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CardIntegrationTest {

    private static final String CARD_NUMBER = "5555666677778888";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteCard() {
        cardRepository.deleteById(CARD_NUMBER);
    }

    @Test
    void shouldCreateCardAndQueryItsInitialBalance() throws Exception {
        mockMvc.perform(createCard("1234"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardNumber").value(CARD_NUMBER))
                .andExpect(jsonPath("$.password").value("1234"));

        mockMvc.perform(get("/cartoes/{cardNumber}", CARD_NUMBER).with(httpBasic("username", "password")))
                .andExpect(status().isOk())
                .andExpect(content().string("500.00"));
    }

    @Test
    void shouldStoreOnlyTheBcryptHashOfThePassword() throws Exception {
        mockMvc.perform(createCard("1234")).andExpect(status().isCreated());

        String storedPassword = jdbcTemplate.queryForObject(
                "SELECT password FROM card WHERE card_number = ?", String.class, CARD_NUMBER);

        assertThat(storedPassword).isNotEqualTo("1234").startsWith("$2");
    }

    @Test
    void shouldRejectDuplicateCardWithoutChangingTheExistingOne() throws Exception {
        mockMvc.perform(createCard("1234")).andExpect(status().isCreated());

        mockMvc.perform(createCard("9999"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("Card already exists: " + CARD_NUMBER))
                .andExpect(jsonPath("$.password").doesNotExist());

        assertThat(cardRepository.findById(CARD_NUMBER).orElseThrow().getBalance())
                .isEqualTo(new BigDecimal("500.00"));
    }

    @Test
    void shouldCreateCardSentWithOriginalContractFieldNames() throws Exception {
        mockMvc.perform(post("/cartoes")
                        .with(httpBasic("username", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroCartao\":\"%s\",\"senha\":\"1234\"}".formatted(CARD_NUMBER)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardNumber").value(CARD_NUMBER));

        assertThat(cardRepository.findById(CARD_NUMBER)).isPresent();
    }

    @Test
    void shouldReturnNotFoundWithoutBodyForUnknownCard() throws Exception {
        mockMvc.perform(get("/cartoes/{cardNumber}", "1111222233334444").with(httpBasic("username", "password")))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    private MockHttpServletRequestBuilder createCard(String password) {
        return post("/cartoes")
                .with(httpBasic("username", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cardNumber\":\"%s\",\"password\":\"%s\"}".formatted(CARD_NUMBER, password));
    }
}
