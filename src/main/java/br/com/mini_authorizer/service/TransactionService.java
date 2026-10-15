package br.com.mini_authorizer.service;

import br.com.mini_authorizer.domain.Card;
import br.com.mini_authorizer.domain.CardTransaction;
import br.com.mini_authorizer.domain.RejectionReason;
import br.com.mini_authorizer.domain.Transaction;
import br.com.mini_authorizer.domain.rule.AuthorizationRule;
import br.com.mini_authorizer.exception.TransactionRejectedException;
import br.com.mini_authorizer.repository.CardRepository;
import br.com.mini_authorizer.repository.CardTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final CardRepository cardRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final List<AuthorizationRule> rules;

    /**
     * Locks the card row ({@code SELECT ... FOR UPDATE}) until commit, so concurrent transactions on the same
     * card - even from other instances - are evaluated one after the other against the up-to-date balance.
     * The debit is flushed by dirty checking on commit - ADR-0006.
     * <p>
     * Every attempt is recorded in {@code transactions}: the approved record commits atomically with the debit;
     * a rejection still commits ({@code noRollbackFor}) because the rules run before the debit, so the only
     * change to keep is the rejected record itself.
     */
    @Transactional(noRollbackFor = TransactionRejectedException.class)
    public void authorize(Transaction transaction) {
        try {
            Card card = cardRepository.findByNumberForUpdate(transaction.cardNumber())
                    .orElseThrow(() -> new TransactionRejectedException(RejectionReason.CARTAO_INEXISTENTE));
            rules.forEach(rule -> rule.validate(card, transaction));
            card.debit(transaction.amount());
            cardTransactionRepository.save(CardTransaction.approved(transaction, card.getBalance()));
        } catch (TransactionRejectedException exception) {
            cardTransactionRepository.save(CardTransaction.rejected(transaction, exception.getReason()));
            throw exception;
        }
    }
}
