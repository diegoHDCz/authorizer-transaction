package br.com.mini_authorizer.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    private static final String VALID_USERNAME = "username";
    private static final String VALID_PASSWORD = "password";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnUnauthorizedWhenRequestHasNoCredentials() throws Exception {
        mockMvc.perform(get("/cartoes/6549873025634501"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Basic")));
    }

    @Test
    void shouldReturnFriendlyJsonBodyWhenRequestHasNoCredentials() throws Exception {
        mockMvc.perform(get("/cartoes/6549873025634501"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(
                        "Authentication required: provide username and password via HTTP Basic."))
                .andExpect(jsonPath("$.path").value("/cartoes/6549873025634501"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void shouldReturnUnauthorizedWhenPasswordIsWrong() throws Exception {
        mockMvc.perform(get("/cartoes/6549873025634501").with(httpBasic(VALID_USERNAME, "wrong")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Basic")))
                .andExpect(jsonPath("$.message").value("Invalid username or password."));
    }

    @Test
    void shouldReturnUnauthorizedWhenUsernameIsWrong() throws Exception {
        mockMvc.perform(get("/cartoes/6549873025634501").with(httpBasic("wrong", VALID_PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnUnauthorizedOnPostWithoutCredentials() throws Exception {
        mockMvc.perform(post("/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldLetAuthenticatedGetReachTheApplication() throws Exception {
        // Unknown card: getting past security means the contract's 404 instead of 401
        mockMvc.perform(get("/cartoes/6549873025634501").with(httpBasic(VALID_USERNAME, VALID_PASSWORD)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldLetAuthenticatedPostReachTheApplicationWithoutCsrfToken() throws Exception {
        // Stateless REST API: CSRF is disabled, so an authenticated POST must not get a 403
        mockMvc.perform(post("/cartoes")
                        .with(httpBasic(VALID_USERNAME, VALID_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardNumber\":\"4000000000000002\",\"password\":\"1234\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldNotCreateSessionCookieOnAuthenticatedRequest() throws Exception {
        mockMvc.perform(get("/cartoes/6549873025634501").with(httpBasic(VALID_USERNAME, VALID_PASSWORD)))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }
}
