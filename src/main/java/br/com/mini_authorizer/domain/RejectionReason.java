package br.com.mini_authorizer.domain;

/**
 * Why a transaction was not authorized. The constant names are the {@code 422} bodies of the
 * {@code POST /transacoes} contract.
 */
public enum RejectionReason {
    CARTAO_INEXISTENTE,
    SENHA_INVALIDA,
    SALDO_INSUFICIENTE
}
