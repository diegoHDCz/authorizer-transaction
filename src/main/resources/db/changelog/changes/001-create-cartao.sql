--liquibase formatted sql

--changeset mini-authorizer:001-create-cartao
CREATE TABLE cartao (
    numero_cartao VARCHAR(19)    NOT NULL,
    senha         VARCHAR(255)   NOT NULL,
    saldo         DECIMAL(19, 2) NOT NULL,
    CONSTRAINT pk_cartao PRIMARY KEY (numero_cartao)
);
--rollback DROP TABLE cartao;
