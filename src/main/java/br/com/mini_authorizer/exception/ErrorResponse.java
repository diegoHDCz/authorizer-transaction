package br.com.mini_authorizer.exception;

import java.time.Instant;

/**
 * Corpo JSON padrão para erros que o contrato não define (ex.: falha de autenticação).
 */
public record ErrorResponse(Instant timestamp, int status, String error, String message, String path) {
}
