# Padrão: DTOs de request e response

## Regra

Todo objeto que representa o **corpo de uma requisição ou de uma resposta HTTP** (DTO) segue as regras abaixo:

1. **Local:** pacote `br.com.mini_authorizer.controller.dto` (`src/main/java/br/com/mini_authorizer/controller/dto/`).
2. **Nome:** termina com o sufixo **`DTO`** (em maiúsculas), em inglês, como o resto do código:
   - request: `<Ação><Recurso>RequestDTO` — ex.: `CreateCardRequestDTO`, `AuthorizeTransactionRequestDTO`;
   - response: `<Recurso>ResponseDTO` — ex.: `CardResponseDTO`, `ErrorResponseDTO`.
3. **Tipo:** sempre `record` (imutável, sem Lombok).
4. **Nomes dos campos JSON:** em inglês, iguais aos componentes do record (`cardNumber`, `password`), sem
   `@JsonProperty`. Nos **requests**, os nomes originais do contrato (`numeroCartao`, `senha`, `senhaCartao`,
   `valor`) continuam aceitos via `@JsonAlias`, para que clientes do contrato original sigam funcionando.
   **Responses** saem só com os nomes em inglês.
5. **Validação:** Bean Validation nos componentes do request (`@NotBlank`, `@Pattern`, `@Size`, `@Positive`…); o
   controller recebe com `@Valid`. Payload inválido resulta em `400` sem chamar o `service`.
6. **Sem regra de negócio:** DTO só transporta dados. Não é entidade, não é passado para o `domain` nem para o
   `repository` — o controller extrai os valores e chama o `service` com tipos simples ou objetos de domínio.

## Exemplo

```java
package br.com.mini_authorizer.controller.dto;

public record CreateCardRequestDTO(
        @JsonAlias("numeroCartao") @NotBlank @Pattern(regexp = "\\d{13,19}") String cardNumber,
        @JsonAlias("senha") @NotBlank @Size(max = 72) String password) {
}
```

```java
package br.com.mini_authorizer.controller.dto;

public record CardResponseDTO(String cardNumber, String password) {
}
```

`POST /cartoes` aceita qualquer um dos dois requests:

```json
{"cardNumber": "6549873025634501", "password": "1234"}
```

```json
{"numeroCartao": "6549873025634501", "senha": "1234"}
```

A resposta (`201` ou `422`) sempre usa os nomes em inglês:

```json
{"cardNumber": "6549873025634501", "password": "1234"}
```

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public CardResponseDTO create(@Valid @RequestBody CreateCardRequestDTO request) {
    cardService.create(request.cardNumber(), request.password());
    return new CardResponseDTO(request.cardNumber(), request.password());
}
```

## Não fazer

- DTO fora de `controller/dto` (ex.: no pacote `controller`, `exception` ou `domain`).
- Nome sem o sufixo ou com outra grafia: `CardRequest`, `CardDto`, `CardResponseDto`.
- Classe mutável com getters/setters no lugar de `record`.
- Expor a entidade JPA (`Card`) diretamente como corpo de resposta.
- Nomes de campo JSON em português via `@JsonProperty` (use o nome em inglês e, no request, `@JsonAlias` para o
  nome do contrato).
- `@JsonAlias` em response: ele só afeta a leitura (desserialização), não muda o nome gerado na saída.

## Exceções

Respostas cujo corpo o contrato define como valor simples, e não como objeto JSON, não ganham DTO:
`GET /cartoes/{card_number}` devolve `BigDecimal` e `POST /transacoes` devolve o texto `OK` / motivo da recusa.
