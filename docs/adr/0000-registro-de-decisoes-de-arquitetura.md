# ADR-0000: Registrar decisões de arquitetura

- **Status:** Aceito
- **Data:** 2026-10-15

## Contexto

No mini-autorizador, o histórico e as decisões de projeto fazem parte da documentação. Decisões importantes (banco, concorrência, organização do código) precisam ficar rastreáveis, com o motivo e as alternativas descartadas.

## Decisão

Usaremos ADRs (Architecture Decision Records) no formato de Michael Nygard, em `docs/adr/`, um arquivo por decisão (`NNNN-titulo-em-kebab-case.md`), com as seções Contexto, Decisão, Alternativas consideradas e Consequências. O índice fica em [README.md](README.md).

ADRs aceitos são imutáveis: para mudar uma decisão, cria-se um novo ADR que a substitui e o antigo passa a `Substituído por [ADR-NNNN]`.

## Alternativas consideradas

- **Apenas o README do projeto** — simples, mas mistura documentação de uso com decisões e perde o histórico de mudanças.
- **Wiki externa** — fora do repositório, sem versionamento junto ao código.

## Consequências

- Positivas: decisões versionadas junto ao código e revisáveis em PR.
- Negativas / trade-offs: custo pequeno de manter o índice e os ADRs atualizados.
- Sem impacto em testes ou contratos da API.
