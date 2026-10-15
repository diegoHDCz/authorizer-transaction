package br.com.mini_authorizer.exception;

import br.com.mini_authorizer.controller.dto.ErrorResponseDTO;
import br.com.mini_authorizer.controller.dto.FieldErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

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

    /**
     * Deliberate deviation from the contract (which echoes the request body): {@code 422} with an
     * {@link ErrorResponseDTO} telling the client why — see README.
     */
    @ExceptionHandler(CardAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDTO> handleCardAlreadyExists(CardAlreadyExistsException exception,
                                                                    HttpServletRequest request) {
        HttpStatus status = HttpStatus.UNPROCESSABLE_CONTENT;
        return ResponseEntity.status(status).body(new ErrorResponseDTO(
                Instant.now(), status.value(), status.getReasonPhrase(), exception.getMessage(),
                request.getRequestURI()));
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

    /**
     * Bean Validation failures: {@code 400} listing each rejected field and why, sorted for a stable body.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidPayload(MethodArgumentNotValidException exception,
                                                                 HttpServletRequest request) {
        List<FieldErrorDTO> fields = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDTO(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldErrorDTO::field).thenComparing(FieldErrorDTO::message))
                .toList();
        return badRequest(request, "Invalid request body.", fields);
    }

    /**
     * Body that is not valid JSON or has a value of the wrong type (e.g. {@code "valor": "abc"}).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnreadablePayload(HttpServletRequest request) {
        return badRequest(request, "Malformed JSON request body or field with wrong type.", List.of());
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

    private ResponseEntity<ErrorResponseDTO> badRequest(HttpServletRequest request, String message,
                                                        List<FieldErrorDTO> fields) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(new ErrorResponseDTO(
                Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI(), fields));
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
