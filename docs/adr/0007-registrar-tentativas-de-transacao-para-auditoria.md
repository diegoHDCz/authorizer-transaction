# ADR-0007: Registrar todas as tentativas de transação na tabela `transactions` para auditoria

- **Status:** Aceito
- **Data:** 2026-10-15
- **Relacionados:** [ADR-0001](0001-usar-mysql-para-persistencia.md), [ADR-0004](0004-adotar-liquibase-para-versionamento-de-schema.md), [ADR-0005](0005-armazenar-senha-do-cartao-com-bcrypt.md), [ADR-0006](0006-autorizar-transacoes-com-regras-e-lock-pessimista.md)

## Contexto

Os requisitos não exigem persistir a transação, mas também não proíbem. Sem registro, não há como auditar o que aconteceu com um cartão: só resta o saldo atual. Foi pedida uma tabela `transactions` que mantenha os pagamentos registrados e auditáveis. Decisões de produto: gravar **todas** as tentativas (aprovadas e recusadas) e **não** criar endpoint de leitura (auditoria via SQL), portanto os contratos da API não mudam. A regra de concorrência do AGENTS.md (ADR-0006) deve continuar valendo.

## Decisão

**Tabela.** Criada pelo changeset Liquibase `002-create-transactions`:

| Coluna | Tipo | Observação |
|---|---|---|
| `id` | `CHAR(36)` PK | UUID versão 7 |
| `card_number` | `VARCHAR(19)` NOT NULL | sem foreign key para `card` |
| `amount` | `DECIMAL(19,2)` NOT NULL | |
| `status` | `VARCHAR(10)` NOT NULL | `APPROVED` ou `REJECTED` |
| `rejection_reason` | `VARCHAR(20)` NULL | `CARTAO_INEXISTENTE`, `SENHA_INVALIDA`, `SALDO_INSUFICIENTE` |
| `balance_after` | `DECIMAL(19,2)` NULL | saldo após o débito; `null` em recusas, pois o saldo não mudou |
| `created_at` | `TIMESTAMP(6)` NOT NULL | `@CreationTimestamp` |

Há um índice em `(card_number, created_at)`. A senha nunca é gravada.

**Entidade.** `CardTransaction` é `@Immutable` (append-only: o Hibernate ignora alterações) e é criada pelas factories `approved(transaction, balanceAfter)` e `rejected(transaction, reason)`. Não há foreign key para o cartão, para que tentativas em cartão inexistente também sejam registradas.

**Id.** `@UuidGenerator(style = VERSION_7)` com `@JdbcTypeCode(SqlTypes.CHAR)`: UUIDv7 é ordenado pelo tempo, então os inserts ficam no fim do índice clusterizado do InnoDB; é gravado como `CHAR(36)` legível porque o MySQL 5.7 não tem tipo UUID nativo.

**Enums** (`status`, `rejection_reason`) são gravados como `VARCHAR` (`@JdbcTypeCode(SqlTypes.VARCHAR)`), e não como o `ENUM` nativo do MySQL que o Hibernate usaria, para que novos valores não exijam `ALTER TABLE`.

**Transação.** O registro é feito dentro da mesma transação do lock do cartão:

```java
@Transactional(noRollbackFor = TransactionRejectedException.class)
public void authorize(Transaction transaction) {
    try {
        // lock + regras + debit
        cardTransactionRepository.save(CardTransaction.approved(transaction, card.getBalance()));
    } catch (TransactionRejectedException exception) {
        cardTransactionRepository.save(CardTransaction.rejected(transaction, exception.getReason()));
        throw exception;
    }
}
```

O registro aprovado é commitado atomicamente com o débito. A recusa também é commitada (`noRollbackFor`): as regras rodam antes do débito, então a única mudança a manter é o próprio registro de recusa.

**Entrada.** `AuthorizeTransactionRequestDTO.cardNumber` ganhou `@Size(max = 19)` para caber na coluna; número maior responde `400` em vez de erro `500` ao gravar a tentativa.

**Leitura.** Sem endpoint. `CardTransactionRepository.findByCardNumberOrderByCreatedAtAscIdAsc` existe e é usado nos testes.

## Alternativas consideradas

- **Não persistir (só o saldo)** — o mínimo exigido, mas sem trilha de auditoria.
- **Gravar só as aprovadas** — descartado: as recusas (senha inválida, cartão inexistente) são justamente o que mais interessa auditar.
- **`id` `BIGINT AUTO_INCREMENT`** (versão inicial) — previsível/enumerável e acoplado a uma única base.
- **UUIDv4 aleatório** — fragmenta o índice clusterizado do InnoDB.
- **`BINARY(16)`** — mais compacto, porém ilegível em consultas SQL de auditoria.
- **`REQUIRES_NEW` em outro bean** (gravação independente da recusa) — mais complexo e usa uma conexão a mais por recusa enquanto o lock do cartão está retido.
- **Eventos / `@TransactionalEventListener`** — a recusa poderia ser perdida (a transação principal é a que decide o commit).
- **Foreign key para `card`** — impediria registrar tentativas em cartão inexistente.

## Consequências

- Positivas: trilha completa e imutável de aprovações e recusas, com saldo após cada débito; recusas não sofrem rollback; contratos da API inalterados; ids não enumeráveis e ordenados no tempo.
- Negativas / trade-offs: a tabela cresce sem limite (retenção/particionamento fora do escopo); há um `INSERT` extra por transação dentro do lock do cartão; a auditoria só é possível via SQL.
- Testes: `CardTransactionRepositoryTest` (persistência de aprovadas e recusadas, `createdAt`, id UUID v7, cartão inexistente sem FK); `TransactionServiceTest` (cada desfecho grava o registro certo, com `balanceAfter`); `TransactionIntegrationTest` (o fluxo completo gera 4 registros no cartão e 1 `CARTAO_INEXISTENTE`, provando que as recusas não sofrem rollback; na concorrência, 10 `APPROVED` e 10 `REJECTED` e `balance_after` sem repetição); `TransactionControllerTest` (número com 20 dígitos resulta em `400`).
