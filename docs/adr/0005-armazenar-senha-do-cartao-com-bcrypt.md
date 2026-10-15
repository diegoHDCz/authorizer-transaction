# ADR-0005: Armazenar a senha do cartão com hash BCrypt

- **Status:** Aceito
- **Data:** 2026-10-15
- **Relacionados:** [ADR-0001](0001-usar-mysql-para-persistencia.md), [ADR-0004](0004-adotar-liquibase-para-versionamento-de-schema.md)

## Contexto

Todo cartão tem uma senha (ex.: `1234`) que a autorização de transações vai conferir (`SENHA_INVALIDA`). Até aqui ela era gravada em texto puro em `card.password`: quem tivesse acesso de leitura ao banco (dump, backup, réplica vazada) teria a senha de todos os cartões pronta para uso.

Forças em jogo:

- A senha é curta (4 dígitos no exemplo do contrato: só 10.000 combinações), então qualquer hash dela sozinha pode ser quebrado por força bruta offline se o banco vazar.
- O contrato devolve a senha nas respostas `201` / `422` do `POST /cartoes`, então ela trafega em texto puro pela API independentemente de como é armazenada.
- O projeto já tem o Spring Security no classpath (HTTP Basic), que traz o `spring-security-crypto`.
- O Spring Boot usa qualquer bean `PasswordEncoder` para o usuário em memória do HTTP Basic (`spring.security.user.password`); registrar um faria a senha `password` da API, em texto puro, deixar de bater e toda requisição voltaria `401`.

## Decisão

Gravaremos apenas o **hash BCrypt da senha do cartão** (`BCryptPasswordEncoder`, força padrão 10, salt aleatório por hash).

- `domain/CardPasswordHasher` (um `@Component` simples, **não** um bean `PasswordEncoder`) encapsula o encoder com `hash` e `matches`, sem afetar o HTTP Basic.
- `Card.create(number, rawPassword, hasher)` gera o hash na criação; `Card.passwordMatches(rawPassword, hasher)` é o que a regra de senha da autorização vai usar. A entidade nunca expõe o hash armazenado.
- O **número** do cartão continua em texto puro: é a chave primária e a chave de busca de `GET /cartoes/{cardNumber}` e `POST /transacoes`.
- `CreateCardRequestDTO.password` é limitado a 72 caracteres (`@Size(max = 72)`), o limite de entrada do BCrypt.

## Alternativas consideradas

- **Texto puro (situação anterior)** — mais simples, mas um vazamento do banco expõe todas as senhas diretamente. Rejeitada.
- **Card hash (hash do número do cartão)** — considerado para proteger também o número do cartão (PAN) em repouso. Um hash com salt como o BCrypt impossibilita a busca pelo número, então exigiria um hash determinístico com chave (HMAC-SHA256 com segredo no servidor) como chave primária, além de gestão de chave e migração da coluna. Também não acrescenta proteção prática hoje, já que o número trafega em texto puro em toda requisição. Não adotado por ora; é o próximo passo natural se o número do cartão precisar ser protegido em repouso (PCI DSS).
- **HMAC-SHA256 da senha com segredo no servidor (pepper)** — mais forte que o BCrypt contra força bruta offline de um PIN de 4 dígitos, porque o atacante também precisa da chave, e rápido. Exige gerenciar e rotacionar um segredo (variável de ambiente / cofre). Considerado exagero para o escopo atual.
- **Hash rápido sem salt (SHA-256)** — com 10.000 PINs possíveis, uma tabela pré-calculada quebra todos os cartões instantaneamente. Rejeitada.
- **Registrar um bean `PasswordEncoder` (`BCryptPasswordEncoder` ou `DelegatingPasswordEncoder`)** — mudaria também a verificação da senha do usuário do HTTP Basic, obrigando `spring.security.user.password` a virar `{noop}...` ou um hash BCrypt. Rejeitada para manter a configuração de autenticação independente.
- **Abordagem do mundo real (PIN block verificado por HSM)** — como as bandeiras realmente tratam PINs; fora do escopo deste projeto.

## Consequências

- Positivas: o banco não guarda mais senhas utilizáveis; com salt, senhas iguais geram valores diferentes; nenhuma dependência nova e nenhuma mudança no HTTP Basic.
- Negativas / trade-offs: com só 10.000 PINs de 4 dígitos possíveis, um hash vazado ainda pode ser quebrado por força bruta offline (o BCrypt torna isso minutos por cartão em vez de instantâneo). Cada verificação custa cerca de 50–100 ms de CPU, o que adiciona latência a cada autorização de transação e deixa o teste de concorrência mais lento.
- A API continua devolvendo a senha em texto puro nas respostas do `POST /cartoes`, como o contrato exige; o hash protege apenas o dado em repouso.
- Linhas criadas antes desta mudança têm senha em texto puro e não vão bater; bancos locais precisam ser recriados (`docker compose down -v`).
- Testes: `CardPasswordHasherTest` (hash ≠ senha, com salt, match / mismatch), `CardTest` (`passwordMatches`), `CardServiceTest` (o cartão salvo bate com a senha original) e `CardIntegrationTest` (a coluna `password` guarda um hash BCrypt `$2…`, lido via JDBC).
- Contratos da API não são afetados.
