package br.com.mini_authorizer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;

/**
 * Benefit card and its available balance.
 * <p>
 * The card number is the primary key, assigned by the application. Implementing {@link Persistable} makes
 * {@code save} of a newly created card always use {@code persist} (INSERT) instead of {@code merge}: a number
 * that is already registered violates the primary key instead of overwriting the existing card — even when two
 * creations race each other.
 */
@Entity
@Table(name = "card")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card implements Persistable<String> {

    public static final BigDecimal INITIAL_BALANCE = new BigDecimal("500.00");

    @Id
    @Column(name = "card_number", length = 19)
    private String number;

    /**
     * BCrypt hash, never the raw password - ADR-0005.
     */
    @Getter(AccessLevel.NONE)
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean newEntity;

    private Card(String number, String password, BigDecimal balance) {
        this.number = number;
        this.password = password;
        this.balance = balance;
        this.newEntity = true;
    }

    public static Card create(String number, String rawPassword, CardPasswordHasher hasher) {
        return new Card(number, hasher.hash(rawPassword), INITIAL_BALANCE);
    }

    public boolean passwordMatches(String rawPassword, CardPasswordHasher hasher) {
        return hasher.matches(rawPassword, password);
    }

    @Override
    public String getId() {
        return number;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markAsPersisted() {
        this.newEntity = false;
    }
}
