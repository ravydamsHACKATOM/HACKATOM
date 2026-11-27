# Registro de uso de IA

[English](AI_LOG.md) · **Português (Brasil)**

Este registro documenta o uso de IA no projeto. As entradas abaixo resumem usos anotados pela equipe e a assistência atual do Codex; a equipe deve confirmar se os resumos refletem suas próprias decisões antes da apresentação.

## Ferramentas

- ChatGPT / Codex
- GitHub Copilot ou outras ferramentas: não constam nas anotações de projeto disponíveis

## 25/11/2026 — Planejamento e desenvolvimento do MVP

- **Prompt principal:** Pedir ajuda para transformar o protótipo do StudyFlow em uma aplicação funcional com HTML, CSS, Java, Spring Boot e SQLite.
- **O que a IA gerou:** Um roteiro de desenvolvimento, sugestões de funcionalidades e etapas de integração.
- **O que a equipe aproveitou:** O escopo funcional, a direção de integrar front-end e back-end e o plano de implementação em etapas, conforme o registro anterior da equipe.
- **O que a equipe corrigiu ou rejeitou, e por quê:** A equipe reforçou a fidelidade ao design do Figma e rejeitou complexidade visual ou técnica desnecessária.

## 25/11/2026 — Configuração do GitHub Actions

- **Prompt principal:** Criar um workflow do Actions para o build do backend em pull requests e pushes, usando Java 21 e Maven.
- **O que a IA gerou:** Uma estrutura de workflow e orientações para configurar o Maven.
- **O que a equipe aproveitou:** A estrutura da CI e as orientações sobre o Maven Wrapper, conforme o registro anterior da equipe.
- **O que a equipe corrigiu ou rejeitou, e por quê:** A equipe omitiu temporariamente os testes na etapa inicial e corrigiu a permissão de execução do Maven Wrapper. O workflow atual executa a verificação; a equipe deve atualizar este registro após revisar a pipeline atual.

## 27/11/2026 — Revisão dos requisitos e geração semanal

- **Prompt principal:** Comparar o projeto com o briefing do professor e implementar melhorias viáveis.
- **O que a IA gerou:** Uma análise de requisitos, a documentação bilíngue canônica na raiz, uma etapa de Checkstyle na CI e uma alteração local para limitar os blocos ao domingo da semana escolhida e executar a geração em uma transação. Também foi adicionado um teste de regressão específico.
- **O que a equipe aproveitou:** Ainda depende de revisão da equipe. As alterações estão sem commit. O Checkstyle local e `mvnw clean verify` passaram com Java 21.0.7; o teste de regressão do cronograma passou.
- **O que a equipe corrigiu ou rejeitou, e por quê:** As primeiras verificações locais apontaram um import não utilizado e depois imports explícitos de DTOs ausentes; os problemas foram corrigidos. A equipe ainda deve revisar as alterações antes de aceitá-las.
