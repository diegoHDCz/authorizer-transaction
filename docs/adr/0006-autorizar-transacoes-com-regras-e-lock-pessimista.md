# ADR-0006: Autorizar transações com regras polimórficas e lock pessimista no cartão

- **Status:** Aceito
- **Data:** 2026-10-15
- **Relacionados:** [ADR-0007](0007-registrar-tentativas-de-transacao-para-auditoria.md), [ADR-0001](0001-usar-mysql-para-persistencia.md), [ADR-0003](0003-arquitetura-em-camadas-simples.md), [ADR-0005](0005-armazenar-senha-do-cartao-com-bcrypt.md)

## Contexto

`POST /transacoes` deve autorizar uma transação se o cartão existe, a senha confere e há saldo, respondendo `201 OK` ou `422` com `CARTAO_INEXISTENTE`, `SENHA_INVALIDA` ou `SALDO_INSUFICIENTE`. Duas questões guiam o desenho:

- **Regras sem `if`** (objetivo de design): preferir objetos, polimorfismo, `Optional`, streams e exceções de domínio.
- **Concorrência** (regra inviolável do AGENTS.md): duas transações simultâneas, inclusive em instâncias diferentes, não podem deixar o saldo negativo nem perder débito. Exemplo: saldo 10,00 e duas transações de 10,00 ao mesmo tempo.

## Decisão

**Regras.** Cada regra implementa `AuthorizationRule` (`isSatisfiedBy`, `rejectionReason` e um `validate` default que usa `Optional.of(card).filter(...).orElseThrow(...)`, lançando `TransactionRejectedException`). `PasswordRule` (`@Order(1)`, usa `CardPasswordHasher`) e `BalanceRule` (`@Order(2)`, usa `Card.hasBalanceFor`) são `@Component`s; o Spring injeta a `List<AuthorizationRule>` já ordenada. "Cartão existe" não é regra da lista: sem cartão não há o que avaliar, então o serviço faz `findByNumberForUpdate(...).orElseThrow(CARTAO_INEXISTENTE)`. A ordem é cartão, senha, saldo, e a primeira falha define a resposta. `RejectionReason` tem os nomes iguais aos corpos do contrato, e o `GlobalExceptionHandler` responde `422` com `reason.name()`.

**Concorrência.** `TransactionService` é `@Transactional` e carrega o cartão com lock pessimista:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select c from Card c where c.number = :number")
Optional<Card> findByNumberForUpdate(String number); // SELECT ... FOR UPDATE
```

Com a linha travada, o serviço executa `rules.forEach(validate)` e `card.debit(amount)`; o débito é gravado por dirty checking no commit, que também libera o lock. Transações no mesmo cartão (mesmo em instâncias diferentes) são serializadas pelo MySQL/InnoDB; cartões diferentes não competem. Nesse exemplo, uma transação retorna `OK` e a outra `SALDO_INSUFICIENTE`.

A transação (`Transaction`, um `record`) não é persistida como entidade de negócio; o débito altera apenas o saldo do cartão. O registro de auditoria das tentativas (aprovadas e recusadas) é tratado no [ADR-0007](0007-registrar-tentativas-de-transacao-para-auditoria.md), que torna o serviço `noRollbackFor = TransactionRejectedException`.

## Alternativas consideradas

- **`UPDATE card SET balance = balance - :v WHERE number = :n AND balance >= :v`** — maior vazão e sem lock longo, mas tira a regra de saldo do domínio e exige consulta extra para distinguir cartão inexistente de saldo insuficiente.
- **Lock otimista (`@Version`)** — neste cenário os conflitos são frequentes e exigiriam retry; o resultado seria mais complexo.
- **`synchronized` / lock em memória** — não funciona entre instâncias; proibido pelo AGENTS.md.
- **Cadeia de responsabilidade clássica** — mais cerimônia; uma lista ordenada resolve com menos código.
- **Enum de regras** — `PasswordRule` depende de um bean (`CardPasswordHasher`), o que não combina com enum.

## Consequências

- Positivas: regras isoladas, testáveis e extensíveis (nova regra = novo `@Component` com `@Order`); correção de concorrência garantida pelo banco; nenhum `if` no fluxo.
- Negativas / trade-offs: o lock fica retido durante o BCrypt (~50–100 ms), limitando a vazão por cartão a ~10–20 tx/s; aceitável para o volume esperado. A ordem das regras depende de `@Order`.
- Testes: `PasswordRuleTest`, `BalanceRuleTest`, `CardTest` (`hasBalanceFor`, `debit`), `TransactionServiceTest` (cada motivo, ordem das regras, saldo intacto ao recusar), `TransactionControllerTest` (contrato, aliases, `400`, `401`), `CardRepositoryTest` (`findByNumberForUpdate`) e `TransactionIntegrationTest` (fluxo completo; 20 débitos simultâneos de R$ 50,00 em cartão de R$ 500,00 resultam em exatamente 10 `OK`, 10 `SALDO_INSUFICIENTE` e saldo 0). Trocando o lock por `findById`, o teste de concorrência falha.
- O H2 de teste ganhou `LOCK_TIMEOUT=10000` em `application-test.properties`: o padrão (1 s) é curto para 20 transações em fila com BCrypt; o MySQL usa `innodb_lock_wait_timeout` de 50 s.
- Contratos da API respeitados; nenhuma mudança neles.
