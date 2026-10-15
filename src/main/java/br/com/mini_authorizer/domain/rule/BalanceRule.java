package br.com.mini_authorizer.domain.rule;

import br.com.mini_authorizer.domain.Card;
import br.com.mini_authorizer.domain.RejectionReason;
import br.com.mini_authorizer.domain.Transaction;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class BalanceRule implements AuthorizationRule {

    @Override
    public boolean isSatisfiedBy(Card card, Transaction transaction) {
        return card.hasBalanceFor(transaction.amount());
    }

    @Override
    public RejectionReason rejectionReason() {
        return RejectionReason.SALDO_INSUFICIENTE;
    }
}
