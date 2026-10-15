package br.com.mini_authorizer.controller.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public record CreateCardRequestDTO(
        @JsonAlias("numeroCartao") @NotBlank @Pattern(regexp = "\\d{13,19}") String cardNumber,
        @JsonAlias("senha") @NotBlank @Size(max = 72) String password) {
}
