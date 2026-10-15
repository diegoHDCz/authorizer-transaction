package br.com.mini_authorizer.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private static final String PATH = "/cartoes/6549873025634501";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", PATH);

    @Test
    void shouldAskForCredentialsWhenTheyAreMissing() {
        ResponseEntity<ErrorResponse> response = handler.handleMissingCredentials(request);

        assertUnauthorized(response, "Autenticação obrigatória: informe usuário e senha via HTTP Basic.");
    }

    @Test
    void shouldReportInvalidCredentialsWhenUsernameOrPasswordIsWrong() {
        ResponseEntity<ErrorResponse> response = handler.handleBadCredentials(request);

        assertUnauthorized(response, "Usuário ou senha inválidos.");
    }

    @Test
    void shouldFallBackToGenericMessageForOtherAuthenticationFailures() {
        ResponseEntity<ErrorResponse> response = handler.handleAuthenticationFailure(request);

        assertUnauthorized(response, "Falha na autenticação.");
    }

    private void assertUnauthorized(ResponseEntity<ErrorResponse> response, String expectedMessage) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE))
                .isEqualTo("Basic realm=\"mini-autorizador\"");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(401);
        assertThat(response.getBody().error()).isEqualTo("Unauthorized");
        assertThat(response.getBody().message()).isEqualTo(expectedMessage);
        assertThat(response.getBody().path()).isEqualTo(PATH);
        assertThat(response.getBody().timestamp()).isNotNull();
    }
}
