# ADR-0001: Usar MySQL (banco relacional) para persistência

- **Status:** Aceito
- **Data:** 2026-10-15 (revisado em 2026-10-15: inclui a decisão sobre o banco dos testes)
- **Relacionados:** [ADR-0002](0002-adotar-flyway-para-versionamento-de-schema.md)

## Contexto

É preciso persistir cartão e saldo (AGENTS.md). O débito de saldo exige que duas transações simultâneas, inclusive em instâncias diferentes, não deixem o saldo negativo nem percam débito, resolvido no banco (regra 5). O `docker-compose.yml` oferece `mysql` (5.7) e um `mongo` comentado; o serviço `mysql` não pode ser alterado (regra 2).

## Decisão

Usaremos o MySQL do `docker-compose.yml` (imagem `mysql:5.7`, banco `miniautorizador`) como banco de dados. Um banco relacional oferece transações ACID e mecanismos maduros de controle de concorrência (lock pessimista, otimista, `UPDATE ... WHERE saldo >= valor`), adequados ao débito de saldo.

Configuração atual em `application.properties` (URL, usuário e senha via `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, com padrão para o compose) e dependência `mysql-connector-j` no `pom.xml`.

Para os **testes automatizados** usaremos **H2 em memória**, de modo que `./mvnw verify` rode sem Docker nem passos manuais (AGENTS.md). A aplicação em execução continua usando o MySQL 5.7 do compose. Os testes apontarão para o H2 em modo de compatibilidade MySQL, por exemplo:

```properties
spring.datasource.url=jdbc:h2:mem:miniautorizador;MODE=MySQL;DB_CLOSE_DELAY=-1
```

O SQL (migrations e consultas) deve permanecer portável entre H2 e MySQL. A dependência do H2 (escopo `test`) ainda precisa ser confirmada no `pom.xml`.

## Alternativas consideradas

- **MongoDB (comentado no compose)** — modelo flexível, mas o modelo de dados aqui é simples e fortemente relacional/transacional; garantias multi-operação e atomicidade do débito são menos diretas. Não escolhido.
- **Outro banco relacional (ex.: PostgreSQL)** — exigiria alterar o compose, o que é vedado.
- **Testcontainers com MySQL 5.7 nos testes** — fidelidade maior ao ambiente real, mas exige Docker local e no CI e é mais lento. Descartado em favor do H2.
- **Testes contra o MySQL do `docker-compose.yml`** — exige passo manual (`docker compose up`), contrariando o requisito de `./mvnw verify` autônomo.

## Consequências

- Positivas: atomicidade e isolamento no débito; já provisionado no compose; driver já no `pom.xml`.
- Negativas / trade-offs: MySQL 5.7 é antigo (fim de vida), o que pode limitar versões de ferramentas (ver ADR-0002); acoplamento a SQL do MySQL.
- Testes (positivo): `./mvnw verify` roda sem Docker e sem passos manuais; execução rápida.
- Testes (risco): H2 difere do MySQL 5.7 em dialeto e comportamento, por exemplo no lock pessimista (`SELECT ... FOR UPDATE`), no `UPDATE ... WHERE saldo >= valor` usado na concorrência e em tipos como `DECIMAL` com escala 2. Mitigação: `MODE=MySQL`, SQL portável e smoke test manual contra o MySQL do compose. O teste de concorrência no H2 não comprova o comportamento de isolamento do MySQL 5.7.
- Contratos da API não são afetados.
