# Decisões do projeto

[English](DECISIONS.md) · **Português (Brasil)**

Este registro reúne decisões que aparecem na documentação e na implementação. A equipe deve revisá-lo antes da apresentação e completar ou corrigir as decisões tomadas fora do repositório.

## 24/11/2026 — Tecnologia do back-end

- **Decisão:** Usar Java com Spring Boot.
- **Alternativas consideradas:** Node.js; as anotações existentes da equipe citam Node como alternativa.
- **Motivo:** A equipe informou ter familiaridade com Java, e o Spring Boot fornece a API REST e serve os arquivos web.

## 24/11/2026 — Tecnologia do front-end

- **Decisão:** Usar HTML, CSS e JavaScript sem um servidor de desenvolvimento separado para o front-end.
- **Alternativas consideradas:** Um framework ou servidor de front-end separado; nenhuma alternativa específica foi registrada.
- **Motivo:** Manter o MVP enxuto e servir a interface estática pela aplicação Spring Boot.

## 24/11/2026 — Persistência de dados

- **Decisão:** Usar SQLite para persistência local.
- **Alternativas consideradas:** Nenhuma alternativa consta nas anotações existentes da equipe.
- **Motivo:** Um banco local simplifica a configuração do MVP.

## 25/11/2026 — Recorte do MVP

- **Decisão:** Construir o fluxo principal de planejamento com disciplinas, eventos, disponibilidade semanal, blocos gerados, visão semanal/diária e acompanhamento de conclusão.
- **Alternativas consideradas:** Um aplicativo de produtividade mais amplo; a equipe restringiu o escopo ao tema de agenda de estudos e a um MVP demonstrável.
- **Motivo:** Concluir o fluxo central durante a imersão de uma semana.

## Item em aberto — Planejamento com apoio de IA

O código atual tem um gerador determinístico e não está integrado a um provedor de IA. A equipe ainda precisa decidir e registrar se implementará sugestões ou remanejamento com IA para a apresentação e como explicará ao estudante o esforço que não coube no cronograma.
