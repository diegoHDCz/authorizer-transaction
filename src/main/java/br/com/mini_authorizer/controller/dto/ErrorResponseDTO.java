package br.com.mini_authorizer.controller.dto;

import java.time.Instant;

/**
 * Default JSON body for errors the contract does not define (e.g. authentication failures).
 */
public record ErrorResponseDTO(Instant timestamp, int status, String error, String message, String path) {
}
