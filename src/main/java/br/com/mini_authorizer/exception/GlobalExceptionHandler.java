package br.com.mini_authorizer.exception;

import br.com.mini_authorizer.controller.dto.CardResponseDTO;
import br.com.mini_authorizer.controller.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * Single place that translates exceptions into HTTP status and body.
 * <p>
 * Authentication exceptions are raised in the Spring Security filters and reach this class because the
 * {@code AuthenticationEntryPoint} configured in {@code SecurityConfig} forwards them to Spring MVC's
 * {@code HandlerExceptionResolver}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASIC_REALM = "Basic realm=\"mini-autorizador\"";

    @ExceptionHandler(CardAlreadyExistsException.class)
    public ResponseEntity<CardResponseDTO> handleCardAlreadyExists(CardAlreadyExistsException exception) {
        return ResponseEntity.unprocessableContent()
                .body(new CardResponseDTO(exception.getCardNumber(), exception.getPassword()));
    }

    /**
     * Balance query contract: {@code 404} without a body.
     */
    @ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<Void> handleCardNotFound() {
        return ResponseEntity.notFound().build();
    }

    /**
     * Transaction contract: {@code 422} with the rule that barred it ({@code SALDO_INSUFICIENTE},
     * {@code SENHA_INVALIDA} or {@code CARTAO_INEXISTENTE}) as a plain body.
     */
    @ExceptionHandler(TransactionRejectedException.class)
    public ResponseEntity<String> handleTransactionRejected(TransactionRejectedException exception) {
        return ResponseEntity.unprocessableContent().body(exception.getReason().name());
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingCredentials(HttpServletRequest request) {
        return unauthorized(request, "Authentication required: provide username and password via HTTP Basic.");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadCredentials(HttpServletRequest request) {
        return unauthorized(request, "Invalid username or password.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthenticationFailure(HttpServletRequest request) {
        return unauthorized(request, "Authentication failed.");
    }

    private ResponseEntity<ErrorResponseDTO> unauthorized(HttpServletRequest request, String message) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        ErrorResponseDTO body = new ErrorResponseDTO(
                Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        return ResponseEntity.status(status)
                .header(HttpHeaders.WWW_AUTHENTICATE, BASIC_REALM)
                .body(body);
    }
}
