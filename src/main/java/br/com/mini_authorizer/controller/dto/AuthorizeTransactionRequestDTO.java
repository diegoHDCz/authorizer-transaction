package br.com.mini_authorizer.controller.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;


public record AuthorizeTransactionRequestDTO(
        @JsonAlias("numeroCartao") @NotBlank @Size(max = 19) String cardNumber,
        @JsonAlias("senhaCartao") @NotBlank String password,
        @JsonAlias("valor") @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount) {
}
