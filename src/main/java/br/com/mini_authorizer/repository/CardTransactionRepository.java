package br.com.mini_authorizer.repository;

import br.com.mini_authorizer.domain.CardTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CardTransactionRepository extends JpaRepository<CardTransaction, UUID> {

    List<CardTransaction> findByCardNumberOrderByCreatedAtAscIdAsc(String cardNumber);
}
