# 🎡 Roleta do Saber

Sistema web de **roleta de prêmios com perguntas geradas por IA**, desenvolvido com **Spring Boot + Spring AI**.

O jogador escolhe o prêmio que quer ganhar na roleta, compra um ticket e responde a uma pergunta de
múltipla escolha (A, B, C, D) criada pela IA. Se acertar, ganha o direito de girar a roleta com uma
**chance maior** de cair no prêmio escolhido. Se errar, perde o valor do ticket.

## Funcionalidades

- **Cadastro e login** de usuários (senha com hash BCrypt). Todo usuário novo começa com **R$ 1.000**.
- **Roleta interativa** com 20 prêmios (dinheiro, fichas, XP, roupas, veículo, tickets e prêmio mistério).
- **Ticket de R$ 200** por tentativa.
- **Pergunta gerada pela IA** (GenAI via Spring AI), com tema escolhido pelo jogador.
- **A IA decide a dificuldade** (Fácil, Médio, Difícil, Muito difícil) com base no valor do prêmio escolhido
  em relação aos demais: prêmio mais valioso → pergunta mais difícil.
- **A IA decide o bônus de chance** (de 5 a 40 pontos percentuais) que o jogador ganha se acertar.
  A chance do prêmio escolhido **sempre aumenta** quando o jogador acerta.
- **Explicação da resposta** gerada pela IA após responder.
- **Cronômetro** de 60 segundos por pergunta (validado no servidor).
- **Histórico** paginado de rodadas e prêmios ganhos, salvo no banco de dados.

## Como a IA é usada

| Funcionalidade | Onde |
|---|---|
| Gera a pergunta e as 4 alternativas | `GeradorPerguntaIA` (Spring AI `ChatClient`) |
| Decide a dificuldade pelo valor do prêmio | prompt recebe a tabela de prêmios e o ranking do prêmio escolhido |
| Decide o bônus de chance ao acertar | campo `bonusChance` da resposta estruturada (limitado a 5–40 no servidor) |
| Explica a resposta correta | campo `explicacao` |
| Evita repetir perguntas | as últimas perguntas do jogador vão no prompt |

A resposta da IA é pedida em JSON (`BeanOutputConverter`) e **validada no servidor** antes do uso.
As alternativas são embaralhadas no servidor e a resposta correta **nunca é enviada ao navegador**
antes do jogador responder.

## Tecnologias

- Java 25, Spring Boot 4.1, **Spring AI 2.0** (starter OpenAI apontado para o **OpenRouter**)
- Spring Security, Spring Data JPA, Bean Validation
- Banco **H2** (em arquivo, na pasta `./data`)
- Thymeleaf + HTML/CSS/JavaScript puro (roleta desenhada em SVG)

## Como rodar

### 1. Crie uma chave gratuita no OpenRouter

1. Crie uma conta em <https://openrouter.ai>.
2. Vá em **Keys** → **Create Key** e copie a chave (`sk-or-...`).

O projeto usa por padrão o modelo `openrouter/free`, que escolhe automaticamente um **modelo gratuito**
disponível. Para fixar um modelo específico, defina `OPENROUTER_MODEL` (ex.: `google/gemma-4-31b-it:free`).

### 2. Defina a chave como variável de ambiente

> ⚠️ A chave **nunca** deve ir para o código nem para o GitHub. Ela é lida só da variável de ambiente.

**Jeito mais fácil (Windows):** rode o script abaixo e cole a chave quando ele pedir. Ele salva a chave
como variável de ambiente do seu usuário, sem gravá-la no projeto:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\configurar-chave.ps1
```

Depois feche e abra o terminal. Ou, só para o terminal atual:

**PowerShell (Windows):**
```powershell
$env:OPENROUTER_API_KEY = "sk-or-sua-chave-aqui"
```

**Bash (Linux/macOS/Git Bash):**
```bash
export OPENROUTER_API_KEY="sk-or-sua-chave-aqui"
```

### 3. Suba a aplicação

**Jeito mais fácil (Windows):** dê dois cliques em **`rodar.bat`**. Ele lê a chave salva, sobe o sistema e
abre o navegador sozinho. Para usar outra porta: `rodar.bat 8085`.

Ou pelo terminal:

```bash
./mvnw spring-boot:run        # Linux/macOS/Git Bash
mvnw.cmd spring-boot:run      # Windows (cmd/PowerShell)
```

Acesse <http://localhost:8080>, crie uma conta e jogue.

### Variáveis de ambiente

| Variável | Obrigatória | Padrão | Descrição |
|---|---|---|---|
| `OPENROUTER_API_KEY` | sim | — | Chave da API do OpenRouter |
| `OPENROUTER_MODEL` | não | `openrouter/free` | Modelo usado |
| `DB_USER` / `DB_PASSWORD` | não | `sa` / vazio | Credenciais do H2 |
| `H2_CONSOLE` | não | `false` | Liga o console web do H2 em `/h2-console` (só aceita localhost) |
| `COOKIE_SECURE` | não | `false` | Use `true` quando rodar com HTTPS |

### Ver o banco de dados (console H2)

```powershell
$env:H2_CONSOLE = "true"; ./mvnw spring-boot:run
```

Abra <http://localhost:8080/h2-console> e use a JDBC URL `jdbc:h2:file:./data/roleta`, usuário `sa`,
senha vazia (ou o valor de `DB_PASSWORD`). Tabelas: `USUARIOS` e `RODADAS`.

## Testes

```bash
./mvnw test
```

Os testes cobrem o sorteio da roleta (probabilidade aumentada), a validação da resposta da IA, as regras
do jogo (acerto, erro, tempo esgotado, saldo insuficiente, falha da IA, acesso à rodada de outro
jogador) e a segurança web (login obrigatório, CSRF, cabeçalhos e cadastro). A IA é simulada nos
testes, então eles não precisam de chave.

## Estrutura

```
src/main/java/br/com/roleta
├── config/    Segurança (Spring Security) e beans gerais
├── usuario/   Cadastro, login, limite de tentativas
├── jogo/      Prêmios, rodadas, regras do jogo, sorteio, API REST
├── ia/        Integração com GenAI (Spring AI) e validação das respostas
└── web/       Tratamento de erros da API
src/main/resources
├── templates/ Páginas Thymeleaf (login, cadastro, jogo)
└── static/    CSS e JavaScript (roleta em SVG)
```

## Segurança

- Senhas com BCrypt; sessão com cookie `HttpOnly` e `SameSite=Lax`.
- Proteção CSRF em formulários e na API.
- Limite de tentativas de login (5 a cada 15 min por usuário + IP) e de tentativas de cadastro por IP.
- Cada rodada só pode ser acessada pelo próprio dono (sem IDOR).
- Débito de saldo atômico e controle de concorrência otimista (não dá para girar duas vezes).
- Sorteio feito no servidor com `SecureRandom`; o navegador só anima a roleta.
- Content-Security-Policy restritiva e mensagens de erro genéricas para o cliente.
- Chave da IA apenas em variável de ambiente.
