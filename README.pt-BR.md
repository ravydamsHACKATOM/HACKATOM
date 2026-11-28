# StudyFlow

[English](README.md) · **Português (Brasil)**

O StudyFlow é um planejador que transforma provas e entregas em um cronograma de estudos
realista. O estudante informa o que vem pela frente e quando pode estudar de verdade; o
StudyFlow distribui blocos de foco até cada prazo, mostra o que fazer hoje e refaz o plano quando
a semana muda.

Documentos relacionados: [DECISIONS.pt-BR.md](DECISIONS.pt-BR.md) ·
[AI_LOG.pt-BR.md](AI_LOG.pt-BR.md) · [Briefing](docs/BRIEFING.pt-BR.md) · [AGENTS.md](AGENTS.md)

## Equipe

- **Turma:** TDS — turmas mistas 6570, 6756 e 6863
- **Tech Lead:** Ana Carolina Dutra
- **Responsável pela apresentação:** Victor Fanti Antonietti
- **Integrantes:**
  - Ana Carolina Dutra — [@dutranacarolina](https://github.com/dutranacarolina)
  - Victor Fanti Antonietti
  - Raphael Vieira Lutkemeyer Sausen
  - Yuri José Machado Ribeiro

## Tema / área

Agenda de estudos — vida no campus e comunidade.

## Problema

No curso técnico a semana se enche de provas, entregas e projetos. O calendário oficial diz
quando algo vence, mas não diz quando estudar nem quanto tempo reservar. Muita gente improvisa na
véspera, subestima o esforço ou fica com duas entregas pesadas no mesmo dia. Quando existe um
plano, ele raramente sobrevive à primeira mudança.

Isso atinge quem tem várias entregas na mesma semana, quem trava na hora de começar e quem teve
um imprevisto e precisa remanejar tudo.

## Solução

O estudante cadastra disciplinas, provas e entregas (com data e estimativa de esforço em horas) e
as janelas da semana em que pode estudar. A partir disso, o StudyFlow:

1. **Gera a semana**: blocos de foco de 50 minutos com pausas de 10, sempre dentro dos horários
   livres do estudante, com o prazo mais urgente primeiro.
2. **Mostra o que fazer hoje**, com o progresso do dia, todas as provas e entregas do dia e os
   próximos prazos. Sessões concluídas saem da lista de pendentes e continuam registradas.
3. **Mostra a semana inteira** em um calendário simples, com os prazos de cada dia. As sessões
   podem mudar de dia (pelo botão ou arrastando) sem mudar nenhum prazo real, e a semana pode ser
   espalhada para estudar um pouco em cada dia.
4. **Ajusta o plano**: um bloco pode ser concluído, travado no horário ou adiado. Adiar um bloco
   ou mudar um prazo refaz o resto da semana e lista o que mudou. Quando há mais de uma prova
   ou entrega, o estudante escolhe quantas horas cada uma recebe.
5. **Avisa quando o esforço não cabe** antes de um prazo, em vez de montar um cronograma bonito
   e impossível de cumprir. O estudante decide o que cortar ou antecipar.
6. **Explica o plano** em linguagem simples: como ficou a semana, o que mudou e dicas práticas.
7. **Avisa o estudante** com um cartão no canto inferior direito da página quando chega a hora
   de uma sessão de estudo.

### Onde a IA entra

- **No processo:** código, testes, imagens e documentação foram produzidos com apoio de IA e
  revisados pelo time. As ilustrações, os ícones e as texturas de papel foram gerados com um
  modelo de imagem rodando no nosso próprio notebook. Tudo está registrado no
  [AI_LOG.pt-BR.md](AI_LOG.pt-BR.md).
- **No produto:** o plano é montado e explicado por regras fixas, sem serviço pago de IA por trás,
  então o app funciona offline e não precisa de chave de API. Testamos uma explicação escrita por
  um modelo de linguagem e retiramos; os motivos estão no [DECISIONS.pt-BR.md](DECISIONS.pt-BR.md).

### Recorte do MVP

Entra: os pontos acima, contas com cadastro e login (cada estudante vê só os próprios
dados), temas claro e escuro, layout responsivo e uma semana de exemplo que pode ser carregada
com um clique. Fica de fora, de propósito: integração com o sistema acadêmico, notificações por
push ou e-mail (o aviso aparece na página aberta),
sessões de estudo em grupo, importação de dados e recuperação de senha por e-mail. Os motivos
estão no [DECISIONS.pt-BR.md](DECISIONS.pt-BR.md).

## Stack

| Camada | Tecnologia |
| --- | --- |
| Back-end | Java 21, Spring Boot 3.5 (Web, Validation, JDBC) |
| Contas | Senhas com hash BCrypt, sessões no servidor em cookie HttpOnly |
| Banco de dados | SQLite, com migrações Flyway |
| Front-end | HTML, CSS e módulos JavaScript, sem etapa de build |
| Testes | JUnit 5, AssertJ, MockMvc; test runner do Node no front-end |
| Lint | Spotless (google-java-format), ESLint, Prettier, commitlint |
| CI | GitHub Actions |

## Estrutura de pastas

```text
/
├── README.md / README.pt-BR.md
├── DECISIONS.md / DECISIONS.pt-BR.md
├── AI_LOG.md / AI_LOG.pt-BR.md
├── AGENTS.md                  convenções para pessoas e agentes de IA que trabalham aqui
├── backend/                   aplicação Spring Boot
│   └── src/main/java/com/studyflow/
│       ├── auth/              contas, sessões e a proteção da API
│       ├── discipline/        disciplinas (CRUD)
│       ├── event/             provas e entregas (CRUD)
│       ├── availability/      horários livres da semana (CRUD)
│       ├── plan/              plano semanal: serviço, endpoints, persistência
│       │   └── engine/        o agendador, em Java puro, sem framework
│       ├── advisor/           explicação do plano, escrita por regras fixas
│       └── common/            erros, relógio, health check
├── frontend/                  interface estática servida pelo back-end
│   ├── index.html
│   ├── styles/                tokens de design, base, componentes, telas
│   ├── js/                    cliente da API, templates, controles de formulário, um módulo por tela
│   ├── assets/                ilustrações, ícones e texturas de papel
│   └── tests/                 testes unitários dos formatadores
├── docs/                      briefing original do hackathon
└── .github/workflows/ci.yml   pipeline
```

## Como rodar

### Pré-requisitos

- Java 21 (JDK)
- Nada mais para rodar o app: o Maven vem pelo wrapper e o SQLite é embutido.
- Node 22 ou mais novo, só para o lint e os testes do front-end.

### Back-end e front-end juntos

O back-end serve o front-end, então um comando sobe tudo:

```bash
cd backend
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

Abra <http://localhost:8080>. O banco é criado em `backend/data/studyflow.db`.
Crie uma conta na primeira tela e escolha **"Preencher com um exemplo"** para carregar uma semana
de demonstração. O `.env.example` lista as configurações opcionais.

### Testes e lint

```bash
cd backend && ./mvnw verify          # compila, 44 testes, checagem de formato, jar
cd frontend && npm ci && npm run lint && npm test
```

`./mvnw spotless:apply` e `npm run format` corrigem a formatação.

### Endpoints principais

| Método | Endpoint | Para que serve |
| --- | --- | --- |
| POST | `/api/auth/register`, `/api/auth/login`, `/api/auth/logout` | Cadastro, login e logout |
| GET | `/api/auth/me` | O estudante logado |
| GET, POST, PUT, DELETE | `/api/disciplines` | Disciplinas |
| GET, POST, PUT, DELETE | `/api/events` | Provas e entregas, com progresso |
| GET, POST, PUT, DELETE | `/api/availability` | Horários livres da semana |
| GET | `/api/plan?date=` | Plano da semana que contém a data |
| POST | `/api/plan/generate?date=` | Gera ou refaz a semana |
| PUT | `/api/plan/settings?date=` | Horas por prova ou entrega e modo espalhado; refaz a semana |
| PATCH | `/api/blocks/{id}/done` | Conclui um bloco ou desfaz |
| PATCH | `/api/blocks/{id}/locked` | Trava um bloco no horário |
| POST | `/api/blocks/{id}/postpone` | Adia um bloco e refaz a semana |
| POST | `/api/blocks/{id}/move` | Move um bloco para outro dia ou horário da semana |
| GET | `/api/health` | Health check |

Todas as rotas, menos as três de autenticação acima e o health check, exigem sessão.

## CI/CD

A pipeline em [`.github/workflows/ci.yml`](.github/workflows/ci.yml) roda em todo pull request e
em todo push na `main` e na `develop`:

- **Backend:** `./mvnw verify`: build, testes unitários e de API, checagem de formato com
  Spotless e o jar executável. Os relatórios de teste sobem como artefato.
- **Frontend:** ESLint, checagem do Prettier e testes unitários.
- **Conventional Commits:** nos pull requests, o commitlint valida o título do PR e a mensagem de
  cada commit conforme o [`commitlint.config.js`](commitlint.config.js).

## Deploy

O deploy automático ainda não foi configurado. A aplicação roda localmente a partir de um único
jar (`backend/target/studyflow-1.0.0.jar`), que é como ela é demonstrada.

## Links

- **Repositório:** <https://github.com/ravydamsHACKATOM/HACKATOM>
- **Deploy (se houver):** não publicado
- **Board / projeto no GitHub (se houver):** não há
