# ADR-0003: Adotar arquitetura em camadas simples (controller / service / repository)

- **Status:** Aceito
- **Data:** 2026-10-15

## Contexto

O projeto é um microsserviço pequeno com três casos de uso (criar cartão, consultar saldo, autorizar transação). O AGENTS.md sugere organizar `br.com.mini_authorizer` por responsabilidade, e o desafio opcional propõe regras de autorização sem `if`.

## Decisão

Usaremos arquitetura em camadas simples, com os pacotes:

```
controller/   # endpoints REST e DTOs (records), sem regra de negócio
service/      # casos de uso
domain/       # entidades, regras de autorização e exceções de negócio
repository/   # Spring Data
config/       # segurança e beans de infraestrutura
exception/    # @RestControllerAdvice
```

Dependências fluem de controller para service, e daí para repository/domain.

## Alternativas consideradas

- **Hexagonal (ports & adapters)** — isola o domínio da infraestrutura, mas introduz portas, adaptadores e mapeamentos desnecessários para o tamanho do serviço (overengineering).
- **Vertical slices** — coesão por funcionalidade, porém com apenas três casos de uso pouco acrescenta e foge da organização sugerida no AGENTS.md.

## Consequências

- Positivas: estrutura familiar, pouca cerimônia, fácil de testar (unitário em domínio/serviço, slice em controller).
- Negativas / trade-offs: entidades JPA ficam no domínio, acopladas ao Spring Data/JPA; se o serviço crescer, pode ser necessário reavaliar (novo ADR).
- Estado atual: só existe `MiniAuthorizerApplication`; os pacotes ainda não foram criados.
- Contratos da API não são afetados.
