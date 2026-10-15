package br.com.mini_authorizer.exception;

import lombok.Getter;

/**
 * Attempt to create a card whose number is already registered.
 * <p>
 * Carries the password only because the {@code 422} response echoes the request body;
 * it never goes into the exception message (logs).
 */
@Getter
public class CardAlreadyExistsException extends RuntimeException {

    private final String cardNumber;
    private final transient String password;

    public CardAlreadyExistsException(String cardNumber, String password) {
        super("Card already exists: " + cardNumber);
        this.cardNumber = cardNumber;
        this.password = password;
    }
}
