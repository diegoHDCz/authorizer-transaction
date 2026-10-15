package br.com.mini_authorizer.repository;

import br.com.mini_authorizer.domain.Card;
import br.com.mini_authorizer.domain.CardPasswordHasher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class CardRepositoryTest {

    private static final String CARD_NUMBER = "6549873025634501";

    private final CardPasswordHasher hasher = new CardPasswordHasher();

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldPersistCardAndBalance() {
        cardRepository.saveAndFlush(Card.create(CARD_NUMBER, "1234", hasher));
        entityManager.clear();

        Card found = cardRepository.findById(CARD_NUMBER).orElseThrow();

        assertThat(found.getBalance()).isEqualTo(new BigDecimal("500.00"));
        assertThat(found.isNew()).isFalse();
        assertThat(found.passwordMatches("1234", hasher)).isTrue();
    }

    @Test
    void shouldRejectDuplicateCardInsteadOfOverwritingIt() {
        cardRepository.saveAndFlush(Card.create(CARD_NUMBER, "1234", hasher));
        entityManager.clear();

        assertThatThrownBy(() -> cardRepository.saveAndFlush(Card.create(CARD_NUMBER, "9999", hasher)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
