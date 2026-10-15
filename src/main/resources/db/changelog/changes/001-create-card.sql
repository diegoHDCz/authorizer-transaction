--liquibase formatted sql

--changeset mini-authorizer:001-create-card
CREATE TABLE card (
    card_number VARCHAR(19)    NOT NULL,
    password    VARCHAR(255)   NOT NULL,
    balance     DECIMAL(19, 2) NOT NULL,
    CONSTRAINT pk_card PRIMARY KEY (card_number)
);
--rollback DROP TABLE card;
