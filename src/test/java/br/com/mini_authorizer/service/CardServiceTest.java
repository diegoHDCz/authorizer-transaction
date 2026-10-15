package br.com.mini_authorizer.service;

import br.com.mini_authorizer.domain.Card;
import br.com.mini_authorizer.domain.CardPasswordHasher;
import br.com.mini_authorizer.exception.CardAlreadyExistsException;
import br.com.mini_authorizer.exception.CardNotFoundException;
import br.com.mini_authorizer.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    private static final String CARD_NUMBER = "6549873025634501";
    private static final String PASSWORD = "1234";

    private final CardPasswordHasher hasher = new CardPasswordHasher();

    @Mock
    private CardRepository cardRepository;

    private CardService cardService;

    @BeforeEach
    void setUp() {
        cardService = new CardService(cardRepository, hasher);
    }

    @Test
    void shouldPersistNewCardWithInitialBalanceAndHashedPassword() {
        when(cardRepository.saveAndFlush(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Card created = cardService.create(CARD_NUMBER, PASSWORD);

        ArgumentCaptor<Card> saved = ArgumentCaptor.forClass(Card.class);
        verify(cardRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().isNew()).isTrue();
        assertThat(saved.getValue().passwordMatches(PASSWORD, hasher)).isTrue();
        assertThat(created.getNumber()).isEqualTo(CARD_NUMBER);
        assertThat(created.getBalance()).isEqualTo(new BigDecimal("500.00"));
    }

    @Test
    void shouldThrowCardAlreadyExistsWhenCardNumberIsAlreadyRegistered() {
        when(cardRepository.saveAndFlush(any(Card.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> cardService.create(CARD_NUMBER, PASSWORD))
                .isInstanceOfSatisfying(CardAlreadyExistsException.class, exception -> {
                    assertThat(exception.getCardNumber()).isEqualTo(CARD_NUMBER);
                    assertThat(exception.getPassword()).isEqualTo(PASSWORD);
                    assertThat(exception.getMessage()).doesNotContain(PASSWORD);
                });
    }

    @Test
    void shouldReturnBalanceOfExistingCard() {
        when(cardRepository.findById(CARD_NUMBER))
                .thenReturn(Optional.of(Card.create(CARD_NUMBER, PASSWORD, hasher)));

        assertThat(cardService.getBalance(CARD_NUMBER)).isEqualTo(new BigDecimal("500.00"));
    }

    @Test
    void shouldThrowCardNotFoundWhenCardDoesNotExist() {
        when(cardRepository.findById(CARD_NUMBER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getBalance(CARD_NUMBER))
                .isInstanceOfSatisfying(CardNotFoundException.class,
                        exception -> assertThat(exception.getCardNumber()).isEqualTo(CARD_NUMBER));
    }
}
