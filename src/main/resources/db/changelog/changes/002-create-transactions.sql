--liquibase formatted sql

--changeset mini-authorizer:002-create-transactions
CREATE TABLE transactions (
    id               CHAR(36)       NOT NULL,
    card_number      VARCHAR(19)    NOT NULL,
    amount           DECIMAL(19, 2) NOT NULL,
    status           VARCHAR(10)    NOT NULL,
    rejection_reason VARCHAR(20),
    balance_after    DECIMAL(19, 2),
    created_at       TIMESTAMP(6)   NOT NULL,
    CONSTRAINT pk_transactions PRIMARY KEY (id)
);
CREATE INDEX idx_transactions_card_number_created_at ON transactions (card_number, created_at);
--rollback DROP TABLE transactions;
