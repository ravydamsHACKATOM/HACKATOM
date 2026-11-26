# Decisões de arquitetura

## 2026-11-26 — Frontend servido pelo Spring Boot
- **Decisão:** Manter o MVP como uma aplicação Spring Boot e servir o frontend por `src/main/resources/static`.
- **Alternativas consideradas:** Abrir um HTML separado ou criar uma etapa de build para outro frontend.
- **Motivo:** Requisições na mesma origem simplificam o uso local e evitam uma segunda implantação.

## 2026-11-26 — SQLite com migrações Liquibase
- **Decisão:** Usar SQLite com changesets Liquibase, caminho externo configurável e validação do schema pelo Hibernate.
- **Alternativas consideradas:** `ddl-auto=update`, extensão SQLite do Flyway ou banco servidor.
- **Motivo:** Migrações versionadas deixam mudanças de schema revisáveis; manter bancos locais fora do Git evita versionar dados pessoais. A dependência Flyway para SQLite inicialmente tentada não estava disponível no Maven Central configurado, então foi adotado Liquibase.

## 2026-11-26 — Disponibilidade com dias ISO
- **Decisão:** Armazenar dias ISO, segunda `1` a domingo `7`, e substituir a semana inteira em uma única requisição.
- **Alternativas consideradas:** Índice de JavaScript começando no domingo ou agrupar registros apenas por terem o mesmo horário.
- **Motivo:** Um contrato explícito evita deslocamento dos dias e torna a atualização atômica e previsível.

## 2026-11-26 — Regras de prazo e cronograma
- **Decisão:** Não agendar no dia do prazo; usar sessões de 50 minutos com pausa de 10 minutos e permitir uma última sessão menor. Preservar blocos concluídos ao recalcular.
- **Alternativas consideradas:** Usar o próprio dia do prazo ou apagar todos os blocos anteriores.
- **Motivo:** Um dia de margem antes de provas e entregas é mais seguro, e o trabalho concluído deve permanecer no histórico.

## 2026-11-26 — MVP sem contas de usuário
- **Decisão:** Manter o uso local por uma pessoa sem autenticação e documentar essa limitação.
- **Alternativas consideradas:** Incluir login, associação de dados e autorização nesta entrega.
- **Motivo:** O projeto não tem modelo de usuário nem requisito de hospedagem compartilhada. Uma implantação multiusuário depende de autenticação e propriedade dos registros.
