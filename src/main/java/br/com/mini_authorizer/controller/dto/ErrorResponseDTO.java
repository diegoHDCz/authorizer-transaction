package br.com.mini_authorizer.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Default JSON body for errors the contract does not define (e.g. authentication failures, invalid payloads).
 * {@code fields} lists what is wrong with each request field and is omitted when empty.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponseDTO(Instant timestamp, int status, String error, String message, String path,
                               List<FieldErrorDTO> fields) {

    public ErrorResponseDTO(Instant timestamp, int status, String error, String message, String path) {
        this(timestamp, status, error, message, path, List.of());
    }
}
