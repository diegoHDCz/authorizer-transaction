package br.com.mini_authorizer.service;

import br.com.mini_authorizer.domain.Card;
import br.com.mini_authorizer.domain.CardPasswordHasher;
import br.com.mini_authorizer.exception.CardAlreadyExistsException;
import br.com.mini_authorizer.exception.CardNotFoundException;
import br.com.mini_authorizer.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final CardPasswordHasher cardPasswordHasher;

    public Card create(String cardNumber, String password) {
        try {
            return cardRepository.saveAndFlush(Card.create(cardNumber, password, cardPasswordHasher));
        } catch (DataIntegrityViolationException e) {
            throw new CardAlreadyExistsException(cardNumber, password);
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(String cardNumber) {
        return cardRepository.findById(cardNumber)
                .map(Card::getBalance)
                .orElseThrow(() -> new CardNotFoundException(cardNumber));
    }
}
