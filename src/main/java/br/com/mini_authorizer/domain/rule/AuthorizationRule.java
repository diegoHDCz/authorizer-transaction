package br.com.mini_authorizer.domain.rule;

import br.com.mini_authorizer.domain.Card;
import br.com.mini_authorizer.domain.RejectionReason;
import br.com.mini_authorizer.domain.Transaction;
import br.com.mini_authorizer.exception.TransactionRejectedException;

import java.util.Optional;


public interface AuthorizationRule {

    boolean isSatisfiedBy(Card card, Transaction transaction);

    RejectionReason rejectionReason();

    default void validate(Card card, Transaction transaction) {
        Optional.of(card)
                .filter(candidate -> isSatisfiedBy(candidate, transaction))
                .orElseThrow(() -> new TransactionRejectedException(rejectionReason()));
    }
}
