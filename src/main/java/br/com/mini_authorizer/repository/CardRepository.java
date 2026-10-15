package br.com.mini_authorizer.repository;

import br.com.mini_authorizer.domain.Card;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, String> {
}
