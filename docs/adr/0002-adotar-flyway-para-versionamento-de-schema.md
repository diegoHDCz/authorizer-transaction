# ADR-0002: Adotar Flyway para versionamento de schema

- **Status:** Substituído por [ADR-0004](0004-adotar-liquibase-para-versionamento-de-schema.md)
- **Data:** 2026-10-15
- **Relacionados:** [ADR-0001](0001-usar-mysql-para-persistencia.md)

## Contexto

O schema do MySQL ([ADR-0001](0001-usar-mysql-para-persistencia.md)) precisa ser aplicado de forma consistente e reproduzível em todos os ambientes: homologação, produção, máquinas virtuais e desenvolvedores rodando localmente com Docker. Hoje o `application.properties` usa `spring.jpa.hibernate.ddl-auto=update`, que não é versionado nem auditável.

## Decisão

Usaremos Flyway, com migrations SQL versionadas em `src/main/resources/db/migration` (`V1__...sql`), executadas na inicialização da aplicação. O Hibernate deixará de gerar DDL (`ddl-auto=validate` ou `none`).

## Alternativas consideradas

- **`spring.jpa.hibernate.ddl-auto`** — prático, mas sem histórico, sem controle de mudanças e arriscado em produção.
- **Liquibase** — equivalente funcional; mais verboso (XML/YAML/changelogs) para a necessidade, e o time optou por SQL puro.
- **Scripts SQL manuais** — dependem de execução humana e divergem entre ambientes.

## Consequências

- Positivas: schema versionado, repetível e igual em todos os ambientes; histórico em `flyway_schema_history`.
- Negativas / trade-offs: migrations aplicadas não devem ser editadas (corrigir com nova versão).
- **Próximo passo (não implementado):** o `pom.xml` não tem Flyway; `ddl-auto=update` ainda está ativo; não há `db/migration`. Será preciso adicionar a dependência (versão gerenciada pelo Spring Boot, a confirmar o artefato do starter e o módulo específico para MySQL) e verificar a compatibilidade da versão do Flyway com MySQL 5.7, que pode não ser suportado em versões recentes (questão em aberto).
- Testes: o `./mvnw verify` deve aplicar as migrations no banco de testes, validando-as.
- Contratos da API não são afetados.
