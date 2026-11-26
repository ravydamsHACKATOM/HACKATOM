# StudyFlow

O StudyFlow é um organizador de estudos para quem precisa planejar matérias, provas, entregas, horários disponíveis e sessões de estudo. O MVP transforma a disponibilidade semanal e os prazos cadastrados em um cronograma persistido e acompanha os blocos concluídos.

## Tecnologias

- Java 21, Spring Boot 4.2, Spring MVC, Jakarta Validation, Spring Data JPA/Hibernate
- SQLite com migrações versionadas por Liquibase
- HTML, CSS e JavaScript sem framework, servidos pelo Spring Boot
- Maven Wrapper e GitHub Actions

## Estrutura do repositório

```text
/
├── README.md, README.pt-BR.md
├── DECISIONS.md, DECISIONS.pt-BR.md
├── AI_LOG.md, AI_LOG.pt-BR.md
├── .github/workflows/ci.yml
├── docs/archive/                 # Frontend antigo em arquivo único, preservado como referência
└── studyFlow/studyFlow/
    ├── src/main/java/com/example/studyFlow/
    │   ├── config/               # Caminho do banco e CORS
    │   ├── controller/           # API REST
    │   ├── dto/                  # Contratos validados de entrada e saída
    │   ├── entity/               # Entidades JPA
    │   ├── exception/            # Erros da API e tratamento de exceções
    │   ├── repository/           # Repositórios Spring Data
    │   └── service/              # Regras de negócio e cronograma
    ├── src/main/resources/
    │   ├── db/                   # Changelog Liquibase e migrações SQL
    │   └── static/               # index.html, CSS e JavaScript
    └── src/test/                 # Testes de integração da API e persistência
```

## Executar localmente

Instale um JDK Java 21. A partir de `studyFlow/studyFlow`:

```bash
./mvnw clean verify
./mvnw spring-boot:run
```

No PowerShell do Windows, use `.\mvnw.cmd clean verify` e `.\mvnw.cmd spring-boot:run`.
Acesse [http://localhost:8080](http://localhost:8080) ou `/index.html`. A aplicação serve o frontend e a API em `/api` na mesma origem.
Com JavaScript desativado, `/no-js` oferece formulários renderizados pelo servidor para as mesmas ações principais de matérias, prazos, disponibilidade, cronograma e conclusão.

Por padrão, o SQLite fica em `${user.home}/.studyflow/study-flow.db`, fora do repositório e independente da pasta de execução. Defina `STUDYFLOW_DB_PATH` para escolher outro caminho absoluto. Defina `STUDYFLOW_CORS_ORIGINS` com origens confiáveis separadas por vírgula se o frontend estiver em outro endereço. O log SQL fica desligado; use `STUDYFLOW_SQL_LOG=true` para diagnóstico local.

O Liquibase cria o schema e registra as migrações aplicadas. O Hibernate valida o schema ao iniciar. Faça backup do arquivo do banco antes de atualizar uma instalação existente. Para continuar usando um banco da versão anterior, defina `STUDYFLOW_DB_PATH` com o caminho absoluto desse arquivo; o novo caminho padrão não move nem mescla silenciosamente bancos antigos.

## API REST

Todos os endpoints usam JSON, exceto quando retornam `204 No Content`.

| Método | Caminho | Finalidade |
| --- | --- | --- |
| GET | `/api/health` | Estado da aplicação |
| GET, POST | `/api/subjects` | Listar ou cadastrar matérias |
| PUT, DELETE | `/api/subjects/{id}` | Atualizar ou excluir matéria e seus eventos/blocos |
| GET, POST | `/api/events` | Listar ou cadastrar provas e entregas |
| PUT, DELETE | `/api/events/{id}` | Atualizar ou excluir evento e seus blocos |
| GET | `/api/availability` | Consultar disponibilidade semanal agrupada |
| PUT, DELETE | `/api/availability` | Substituir ou limpar toda a disponibilidade |
| GET | `/api/blocks` | Consultar blocos salvos |
| GET | `/api/today` | Consultar blocos de hoje |
| POST | `/api/schedule/generate` | Recalcular blocos pendentes e retornar avisos |
| PATCH | `/api/blocks/{id}/completion` | Definir se um bloco está concluído |

Exemplo de matéria: `{"name":"Cálculo","color":"#2858c8"}`.

Exemplo de evento: `{"title":"Prova parcial","type":"PROVA","subjectId":1,"dueDate":"2026-12-10","estimatedHours":3.5}`. Os tipos aceitos são `PROVA` e `ENTREGA`; a estimativa deve ser maior que zero e não pode exceder 100 horas.

Exemplo para substituir a disponibilidade: `{"daysOfWeek":[1,3,5],"startTime":"18:00","endTime":"21:00"}`. Os dias seguem ISO: segunda `1` até domingo `7`. Nesta versão, um mesmo intervalo é aplicado aos dias selecionados.

Exemplo para concluir um bloco: `{"completed":true}`. Erros seguem a estrutura `status`, `message`, `code`, `fields` e `timestamp`; validação retorna HTTP 422, recursos inexistentes 404 e conflitos 409.

## Testes e CI

Execute `./mvnw clean test` para os testes de integração com SQLite isolado e `./mvnw clean verify` para a validação completa. O GitHub Actions executa `clean verify` em pushes e pull requests e armazena os relatórios Surefire. Os testes usam bancos SQLite temporários, nunca o banco local da aplicação.

## Segurança e limites do MVP

O MVP não tem login e foi pensado para uma pessoa usando a aplicação localmente. Não o publique como serviço multiusuário: os registros não pertencem a contas e não há uma barreira de autorização. Antes de qualquer hospedagem compartilhada, implemente autenticação e verificação de propriedade dos dados. O CORS aceita apenas origens configuradas e, por padrão, localhost. A aplicação ainda não tem hospedagem pública configurada.

O cronograma considera o dia anterior ao prazo como último dia válido para estudo, cria sessões de 50 minutos quando possível com pausa de 10 minutos, permite uma última sessão menor, preserva blocos concluídos e informa esforço que não conseguiu distribuir. O editor atual aplica um intervalo comum aos dias selecionados.
