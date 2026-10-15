# AGENTS.md

Guia para agentes de IA (Claude Code, Codex, Copilot etc.) e pessoas que trabalham neste repositório.


## Visão geral

Mini-autorizador de transações de cartões benefício (teste técnico VR Benefícios).
API REST em Spring Boot que permite:

- criar cartões (saldo inicial **R$ 500,00**);
- consultar o saldo de um cartão;
- autorizar transações (cartão existe → senha correta → saldo suficiente).

## Stack

| Item | Versão / escolha |
|---|---|
| Java | 25 |
| Spring Boot | 4.1.1 (`spring-boot-starter-webmvc`) |
| Build | Maven via wrapper (`./mvnw`) |
| Banco | MySQL 5.7 (serviço `mysql` do `docker-compose.yml`) |
| Utilitários | Lombok |
| Testes | JUnit 5, Spring Boot Test, MockMvc |

Pacote base: `br.com.mini_authorizer` (o hífen não é válido em nome de pacote Java).

## Comandos

```bash
docker compose up -d          # sobe o MySQL (localhost:3306, banco miniautorizador, root sem senha)
./mvnw spring-boot:run        # roda a aplicação em http://localhost:8080
./mvnw test                   # testes unitários e de integração
./mvnw verify                 # build completo — rodar antes de todo commit/PR
docker compose down           # derruba o banco
```

Smoke test manual (credenciais Basic `username:password`):

```bash
curl -u username:password -X POST localhost:8080/cartoes \
  -H 'Content-Type: application/json' -d '{"numeroCartao":"6549873025634501","senha":"1234"}'
curl -u username:password localhost:8080/cartoes/6549873025634501
curl -u username:password -X POST localhost:8080/transacoes \
  -H 'Content-Type: application/json' -d '{"numeroCartao":"6549873025634501","senhaCartao":"1234","valor":10.00}'
```

## Regras invioláveis

1. **Contratos da API são fixos.** URLs, métodos, status codes, nomes de campos JSON (`numeroCartao`, `senha`,
   `senhaCartao`, `valor`) e corpos de resposta (`OK`, `SALDO_INSUFICIENTE`, `SENHA_INVALIDA`, `CARTAO_INEXISTENTE`)
2. **Não alterar o serviço `mysql` declarado no `docker-compose.yml`.** O serviço `mongo` pode permanecer comentado.
3. **Autenticação HTTP Basic** com `username` / `password` em todos os endpoints; sem credencial válida → `401`.
4. **Persistir o cartão e o saldo.** A transação em si não precisa ser persistida.
5. **Concorrência:** duas transações simultâneas (inclusive em instâncias diferentes) não podem deixar o saldo
   negativo nem perder débito. Resolver no banco (lock pessimista/otimista ou `UPDATE ... WHERE saldo >= valor`),
   nunca apenas com `synchronized` em memória.
6. **Valores monetários** sempre em `BigDecimal` (nunca `double`/`float`), com escala 2.


## Arquitetura e convenções

Organização sugerida por responsabilidade, dentro de `br.com.mini_authorizer`:

```
controller/   # endpoints REST, DTOs de entrada/saída (records), sem regra de negócio
service/      # casos de uso (criar cartão, consultar saldo, autorizar transação)
domain/       # entidades e regras de autorização (Cartao, regras, exceções de negócio)
repository/   # Spring Data
config/       # segurança, beans de infraestrutura
exception/    # @RestControllerAdvice mapeando exceções → status/corpo do contrato
```

- Código (classes, métodos, variáveis) em inglês ou português, **mas consistente**; termos do contrato
  (`numeroCartao`, `CARTAO_INEXISTENTE`…)
- DTOs como `record`; validação com Bean Validation quando fizer sentido.
- Regras de autorização como objetos (ex.: cadeia/lista de regras), favorecendo polimorfismo — o desafio
  opcional propõe a solução **sem `if`** (sem `break`/`continue`). Preferir `Optional`, streams, enums com
  comportamento e exceções de domínio.
- Erros de negócio viram exceções específicas, traduzidas em um único `@RestControllerAdvice`.
- Injeção por construtor (`@RequiredArgsConstructor` do Lombok ou construtor explícito); sem `@Autowired` em campo.
- Configuração em `application.properties` (ou `.yml`), sem credenciais hardcoded no código Java.

## Testes

O enunciado exige **alta cobertura e testes que realmente verifiquem comportamento**.

- **Unitários** para domínio e serviços (regras de autorização, débito de saldo, cada motivo de recusa).
- **Web/slice** (`@WebMvcTest` + MockMvc) para validar contratos: status, corpo e autenticação (401).
- **Integração** (`@SpringBootTest`) cobrindo o roteiro da avaliação: criar cartão → consultar saldo →
  transações até `SALDO_INSUFICIENTE` → senha inválida → cartão inexistente.
- **Concorrência:** ao menos um teste disparando transações simultâneas e verificando o saldo final.
- Asserções sobre resultado/efeito (saldo, status, corpo), não apenas `verify()` de mocks.
- Nomes descritivos: `shouldReturnSaldoInsuficienteWhenBalanceIsLowerThanAmount`.
- Testes devem rodar com `./mvnw verify` sem passos manuais (usar H2/Testcontainers se precisar de banco).

## Workflow de desenvolvimento

O histórico de commits é avaliado — ele deve contar a história da implementação.

1. Criar branch a partir de `main`: `feat/<descricao>`, `fix/<descricao>`, `test/<descricao>`, `docs/<descricao>`.
2. Desenvolver em passos pequenos, de preferência **TDD** (teste falhando → implementação → refatoração).
3. Rodar `./mvnw verify` localmente; tudo verde antes de commitar.
4. Commits pequenos e atômicos no padrão **Conventional Commits**:
   `feat: cria endpoint de criação de cartão`, `test: cobre transação com senha inválida`,
   `fix: evita saldo negativo em transações concorrentes`, `docs: documenta suposições no README`.
5. Abrir PR para `main` usando o template em `.github/pull_request_template.md`; o CI deve passar.
6. Atualizar `README.md` sempre que uma decisão de projeto ou suposição for tomada.

## Checklist antes de concluir uma tarefa

- [ ] `./mvnw verify` passa.
- [ ] Contratos do README respeitados (URL, status, corpo, campos).
- [ ] Testes novos cobrem o caminho feliz **e** os de erro.
- [ ] Nenhum `double`/`float` para dinheiro; nenhuma credencial nova hardcoded.
- [ ] Suposições/decisões registradas no README.
- [ ] Sem arquivos de IDE/SO no commit (`.idea/`, `.DS_Store`, `target/`).
