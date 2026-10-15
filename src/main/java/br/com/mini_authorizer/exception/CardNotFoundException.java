package br.com.mini_authorizer.exception;

import lombok.Getter;

/**
 * No card registered with the given number.
 */
@Getter
public class CardNotFoundException extends RuntimeException {

    private final String cardNumber;

    public CardNotFoundException(String cardNumber) {
        super("Card not found: " + cardNumber);
        this.cardNumber = cardNumber;
    }
}
