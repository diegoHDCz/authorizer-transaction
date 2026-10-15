package br.com.mini_authorizer.exception;

import br.com.mini_authorizer.domain.RejectionReason;
import lombok.Getter;

/**
 * An authorization rule barred the transaction; {@link #getReason()} is the {@code 422} body.
 */
@Getter
public class TransactionRejectedException extends RuntimeException {

    private final RejectionReason reason;

    public TransactionRejectedException(RejectionReason reason) {
        super("Transaction rejected: " + reason);
        this.reason = reason;
    }
}
