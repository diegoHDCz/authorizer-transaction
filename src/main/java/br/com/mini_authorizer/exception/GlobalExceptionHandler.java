package br.com.mini_authorizer.exception;

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
 * Ponto único de tradução de exceções para status e corpo HTTP.
 * <p>
 * As exceções de autenticação nascem nos filtros do Spring Security e chegam aqui porque o
 * {@code AuthenticationEntryPoint} configurado em {@code SecurityConfig} as repassa ao
 * {@code HandlerExceptionResolver} do Spring MVC.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASIC_REALM = "Basic realm=\"mini-autorizador\"";

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleMissingCredentials(HttpServletRequest request) {
        return unauthorized(request, "Autenticação obrigatória: informe usuário e senha via HTTP Basic.");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(HttpServletRequest request) {
        return unauthorized(request, "Usuário ou senha inválidos.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationFailure(HttpServletRequest request) {
        return unauthorized(request, "Falha na autenticação.");
    }

    private ResponseEntity<ErrorResponse> unauthorized(HttpServletRequest request, String message) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        ErrorResponse body = new ErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        return ResponseEntity.status(status)
                .header(HttpHeaders.WWW_AUTHENTICATE, BASIC_REALM)
                .body(body);
    }
}
