package br.com.mini_authorizer.domain;

import java.math.BigDecimal;

/**
 * Transaction to be authorized. Not persisted: only its effect on the card balance is.
 */
public record Transaction(String cardNumber, String password, BigDecimal amount) {
}
