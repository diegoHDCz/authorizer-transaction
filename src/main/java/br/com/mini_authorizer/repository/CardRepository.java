package br.com.mini_authorizer.repository;

import br.com.mini_authorizer.domain.Card;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, String> {

    /**
     * {@code SELECT ... FOR UPDATE}: the row stays locked until the surrounding transaction ends - ADR-0006.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Card c WHERE c.number = :number")
    Optional<Card> findByNumberForUpdate(@Param("number") String number);
}
