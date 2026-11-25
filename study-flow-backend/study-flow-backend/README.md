# Study Flow — Backend

Backend inicial do Study Flow, desenvolvido com Java 21, Spring Boot, Spring Data JPA e SQLite.

## Requisitos

- JDK 21
- Maven 3.9+ (ou Maven integrado à IDE)

## Executar

No terminal, dentro desta pasta:

```bash
mvn spring-boot:run
```

A API ficará disponível em `http://localhost:8080`.

Para executar os testes:

```bash
mvn test
```

Para gerar o pacote:

```bash
mvn clean package
```

## Endpoints iniciais

### Matérias

- `GET /api/subjects` — listar matérias
- `GET /api/subjects/{id}` — buscar matéria
- `POST /api/subjects` — criar matéria
- `PUT /api/subjects/{id}` — atualizar matéria
- `DELETE /api/subjects/{id}` — excluir matéria

Exemplo de criação:

```json
{
  "name": "Matemática",
  "description": "Estudos de álgebra",
  "color": "#4A90E2"
}
```

### Tarefas

- `GET /api/tasks` — listar tarefas
- `GET /api/tasks?completed=false` — listar tarefas pendentes
- `POST /api/tasks` — criar tarefa
- `PUT /api/tasks/{id}` — atualizar tarefa
- `PATCH /api/tasks/{id}/complete` — marcar como concluída
- `DELETE /api/tasks/{id}` — excluir tarefa

Exemplo de criação:

```json
{
  "title": "Revisar funções",
  "description": "Estudar os exercícios da aula",
  "dueAt": "2026-12-01T18:00:00",
  "subjectId": 1
}
```

## Banco de dados

O SQLite será criado automaticamente no arquivo `study-flow.db`, na raiz do backend. Esse arquivo está no `.gitignore` para não ser enviado ao repositório.

## Observações

- Esta é uma estrutura inicial para o MVP, não uma aplicação finalizada.
- Lembretes e notificações ainda precisam ser implementados.
- Os endpoints ainda não incluem autenticação.
- Configure a origem do front-end em `CorsConfig.java` caso ele use outra porta.
- Para integrar o front-end, utilize os endpoints descritos acima.
