package br.com.mini_authorizer.exception;

import br.com.mini_authorizer.controller.dto.ErrorResponseDTO;
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
        ResponseEntity<ErrorResponseDTO> response = handler.handleMissingCredentials(request);

        assertUnauthorized(response, "Authentication required: provide username and password via HTTP Basic.");
    }

    @Test
    void shouldReportInvalidCredentialsWhenUsernameOrPasswordIsWrong() {
        ResponseEntity<ErrorResponseDTO> response = handler.handleBadCredentials(request);

        assertUnauthorized(response, "Invalid username or password.");
    }

    @Test
    void shouldFallBackToGenericMessageForOtherAuthenticationFailures() {
        ResponseEntity<ErrorResponseDTO> response = handler.handleAuthenticationFailure(request);

        assertUnauthorized(response, "Authentication failed.");
    }

    private void assertUnauthorized(ResponseEntity<ErrorResponseDTO> response, String expectedMessage) {
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
