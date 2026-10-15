package br.com.mini_authorizer.domain.rule;

import br.com.mini_authorizer.domain.Card;
import br.com.mini_authorizer.domain.CardPasswordHasher;
import br.com.mini_authorizer.domain.RejectionReason;
import br.com.mini_authorizer.domain.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
public class PasswordRule implements AuthorizationRule {

    private final CardPasswordHasher hasher;

    @Override
    public boolean isSatisfiedBy(Card card, Transaction transaction) {
        return card.passwordMatches(transaction.password(), hasher);
    }

    @Override
    public RejectionReason rejectionReason() {
        return RejectionReason.SENHA_INVALIDA;
    }
}
