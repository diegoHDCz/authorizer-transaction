package br.com.mini_authorizer.controller.dto;

/**
 * Why a request field was rejected. The rejected value is never echoed back, since it may be a password.
 */
public record FieldErrorDTO(String field, String message) {
}
