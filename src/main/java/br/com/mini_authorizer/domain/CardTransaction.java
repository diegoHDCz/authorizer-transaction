package br.com.mini_authorizer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Audit record of one authorization attempt, approved or rejected. Append-only: {@link Immutable} makes
 * Hibernate ignore any change to a persisted record. The card password is never recorded.
 * <p>
 * {@code cardNumber} is not a foreign key: attempts on unknown cards ({@code CARTAO_INEXISTENTE}) are recorded too.
 */
@Entity
@Table(name = "transactions")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardTransaction {

    /**
     * UUIDv7 (time-ordered, so inserts stay at the end of the InnoDB clustered index), stored as readable
     * {@code CHAR(36)} for audit queries - MySQL 5.7 has no native UUID type.
     */
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID id;

    @Column(name = "card_number", nullable = false, length = 19)
    private String cardNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    // VARCHAR instead of the native MySQL ENUM Hibernate would expect, so new values need no ALTER TABLE
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "rejection_reason", length = 20)
    private RejectionReason rejectionReason;

    /**
     * Card balance right after the debit; {@code null} when rejected, since the balance did not change.
     */
    @Column(name = "balance_after", precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private CardTransaction(Transaction transaction, TransactionStatus status, RejectionReason rejectionReason,
                            BigDecimal balanceAfter) {
        this.cardNumber = transaction.cardNumber();
        this.amount = transaction.amount();
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.balanceAfter = balanceAfter;
    }

    public static CardTransaction approved(Transaction transaction, BigDecimal balanceAfter) {
        return new CardTransaction(transaction, TransactionStatus.APPROVED, null, balanceAfter);
    }

    public static CardTransaction rejected(Transaction transaction, RejectionReason reason) {
        return new CardTransaction(transaction, TransactionStatus.REJECTED, reason, null);
    }
}
