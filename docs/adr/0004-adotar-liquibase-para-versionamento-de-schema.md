# ADR-0004: Adotar Liquibase para versionamento de schema

- **Status:** Aceito
- **Data:** 2026-10-15
- **Substitui:** [ADR-0002](0002-adotar-flyway-para-versionamento-de-schema.md)
- **Relacionados:** [ADR-0001](0001-usar-mysql-para-persistencia.md)

## Contexto

O [ADR-0002](0002-adotar-flyway-para-versionamento-de-schema.md) escolheu o Flyway e deixou em aberto se ele era compatível com o MySQL 5.7. A verificação mostrou que não é. O Spring Boot 4.1.1 gerencia o Flyway 12.4.0, e o `MySQLDatabase.ensureSupported()` do `flyway-mysql` exige MySQL 8.0 ou superior na edição Community (o mesmo vale para as versões 10.22 e 11.x). No MySQL 5.7, a aplicação não sobe e mostra "Flyway Teams Edition or MySQL upgrade required". O `docker-compose.yml` usa `mysql:5.7` e não pode ser alterado (regra 2 do AGENTS.md).

A necessidade continua a mesma: um schema versionado, reproduzível e aplicado de forma igual em todos os ambientes, sem `ddl-auto=update`.

## Decisão

Usaremos o **Liquibase**, com a versão gerenciada pelo Spring Boot (`spring-boot-starter-liquibase`, Liquibase 5.0.3). O changelog é aplicado na inicialização da aplicação.

- O changelog mestre fica em `src/main/resources/db/changelog/db.changelog-master.yaml` e apenas inclui arquivos.
- Cada mudança fica em `db/changelog/changes/NNN-descricao.sql`, no formato **formatted SQL** do Liquibase (`--changeset autor:id`, `--rollback`). Assim mantemos o SQL puro que motivou o ADR-0002.
- O Hibernate só valida o mapeamento (`spring.jpa.hibernate.ddl-auto=validate`).
- O SQL deve rodar tanto no MySQL 5.7 quanto no H2 em `MODE=MySQL` usado nos testes ([ADR-0001](0001-usar-mysql-para-persistencia.md)).

## Alternativas consideradas

- **Flyway na versão gerenciada pelo Boot (ADR-0002)**: não suporta MySQL 5.7 na edição Community.
- **Fixar o Flyway 10.0.0**, a última versão encontrada que aceita o 5.7 na Community: sobrescreve a versão gerenciada pelo Boot, provavelmente é incompatível com a autoconfiguração do Spring Boot 4 e não recebe mais manutenção.
- **Flyway Teams**: é pago.
- **`spring.sql.init` com `schema.sql`**: é simples, mas não guarda histórico nem controla quais mudanças já foram aplicadas.

## Consequências

- Positivas: funciona com o MySQL 5.7 do compose sem mudar a infraestrutura. Versão gerenciada pelo Boot. Histórico de mudanças em `DATABASECHANGELOG` e lock de execução em `DATABASECHANGELOGLOCK`, o que é seguro com várias instâncias subindo juntas.
- Negativas / trade-offs: duas tabelas de controle a mais no banco. Um changeset já aplicado não deve ser editado, porque o checksum muda; para corrigir, cria-se um novo changeset.
- Testes: o `./mvnw verify` aplica o changelog no H2 (perfil `test`, ativado pelo Surefire), o que valida o SQL e o mapeamento JPA a cada build. Isso não prova a compatibilidade com o MySQL 5.7; ela continua dependendo do smoke test contra o compose.
- Contratos da API não são afetados.
