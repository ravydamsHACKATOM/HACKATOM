# StudyFlow

[English](README.md) · **Português (Brasil)**

O StudyFlow é uma agenda de estudos para estudantes. Ele permite cadastrar disciplinas, provas e trabalhos, registrar horários semanais disponíveis e gerar blocos de estudo que podem ser visualizados e marcados como concluídos.

## Funcionalidades do MVP

- Cadastrar, editar, listar e excluir disciplinas e eventos de estudo.
- Informar janelas recorrentes de disponibilidade semanal.
- Gerar blocos de estudo de 50 minutos, com intervalos de 10 minutos, respeitando a disponibilidade e os prazos.
- Visualizar o cronograma da semana e os blocos do dia; concluir ou reabrir blocos.
- Salvar os dados em um banco SQLite local.

O planejador atual usa regras determinísticas e não está conectado a um provedor de IA. Se o tempo disponível for insuficiente, ele cria os blocos que couberem, mas ainda não informa o esforço que ficou sem alocação. Ao editar um prazo ou disponibilidade, é preciso gerar o cronograma novamente.

## Tecnologias

- Java 21.0.7 e Spring Boot 3.5.7
- HTML, CSS e JavaScript servidos pelo Spring Boot
- SQLite via JDBC
- Maven Wrapper

## Estrutura do repositório

```text
.
├── .github/workflows/ci.yml
├── docs/                                 # Briefings e materiais arquivados
├── studyflow-springboot/studyflow-springboot/
│   ├── pom.xml
│   └── src/
│       ├── main/java/                    # API REST e regras de negócio
│       ├── main/resources/static/        # Front-end web
│       └── test/java/                   # Testes automatizados
├── README.md
├── README.pt-BR.md
├── DECISIONS.md
├── DECISIONS.pt-BR.md
├── AI_LOG.md
└── AI_LOG.pt-BR.md
```

## Executar localmente

Requisito: Java 21.0.7. O Maven Wrapper baixa o Maven quando necessário.

No PowerShell, dentro de `studyflow-springboot/studyflow-springboot`:

```powershell
.\mvnw.cmd spring-boot:run
```

No macOS ou Linux:

```bash
bash ./mvnw spring-boot:run
```

Abra <http://localhost:8080>. O banco é criado em `data/studyflow.db`.

## Verificações de qualidade

Na pasta do projeto backend:

```bash
bash ./mvnw checkstyle:check
bash ./mvnw clean verify
```

O GitHub Actions executa Checkstyle e a verificação do Maven em pushes e pull requests.

## Situação da entrega

- Publicação: ainda não configurada.
- Repositório: <https://github.com/ravydamsHACKATOM/HACKATOM>
- É preciso confirmar nas configurações do GitHub o colaborador professor, a proteção da branch `main` e as regras de revisão obrigatória.
