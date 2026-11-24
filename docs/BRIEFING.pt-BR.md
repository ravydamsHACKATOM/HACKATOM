# Briefing do tema: Agenda de estudos

[English](BRIEFING.md) · **Português (Brasil)**

**Hack com IA: Codando com Inteligência**  
Senac na Prática · Técnico em Desenvolvimento de Sistemas  
Área: vida no campus e comunidade  
Imersão de 23/11 a 29/11 · apresentações em 30/11 e 01/12

Este briefing é o ponto de partida do **seu** tema. Leiam com calma, recortem um MVP que caiba em uma semana e registrem as decisões. Vocês vão transformar este problema em um produto real, com front-end e back-end, repositório organizado e uso documentado de IA. Esta é uma imersão colaborativa: a banca avalia o que o time construiu, decidiu e aprendeu.

## Tema e contexto

**Agenda de estudos** — Planejador que monta cronograma de estudos a partir das provas e entregas.

No Técnico em Desenvolvimento de Sistemas, a semana se enche de provas, entregas, projetos e aulas. O calendário oficial diz quando algo vence, mas raramente ajuda a responder quando estudar e quanto tempo reservar. Muita gente improvisa na véspera, subestima o esforço ou deixa duas entregas pesadas no mesmo dia — e o plano, quando existe, não sobrevive à primeira mudança de prazo.

Vocês vão construir um planejador que parte das provas e entregas do próprio estudante e monta um cronograma realista, que dá para ajustar quando a semana muda.

## Público e personas

- **Estudante com várias entregas na mesma semana:** precisa enxergar o volume e distribuir o esforço.
- **Estudante que trava para começar:** se beneficia de blocos concretos (“hoje, 50 minutos nesta disciplina”), não só de uma lista de prazos.
- **Estudante que teve um imprevisto:** precisa remanejar o plano sem começar do zero.

## Objetivo

Entregar um MVP em que o aluno informa provas, entregas e horários disponíveis, o sistema monta um cronograma de estudos para a semana, e ele consegue ver, cumprir e ajustar os blocos — com a IA ajudando a propor e a refazer o plano.

## Requisitos mínimos (MVP)

O recorte abaixo é realista para uma semana. Fechem-no no primeiro dia e registrem no `DECISIONS.md` o que entra, o que fica de fora e por quê. É melhor um fluxo completo e demonstrável do que uma lista longa pela metade.

1. Cadastrar disciplinas e eventos de estudo: prova ou entrega, data, e uma estimativa de esforço (por exemplo, em horas).
2. Informar janelas de disponibilidade (dias e horários em que a pessoa realmente pode estudar).
3. Gerar um cronograma da semana com blocos distribuídos a partir desses prazos e da disponibilidade.
4. Visualizar o plano em lista ou em visão de calendário simples.
5. Marcar um bloco como concluído e ajustar o que ainda falta quando um prazo ou uma disponibilidade mudam.
6. Mostrar, no dia, o que está previsto — um “o que fazer hoje” claro.

## Onde a IA entra

O uso de IA é liberado e incentivado — no produto e no processo — e precisa aparecer no `AI_LOG.md`. Algumas frentes concretas neste tema:

- Propor uma distribuição inicial dos blocos, respeitando prazos e horários livres.
- Refazer o plano quando entra uma prova nova ou quando um bloco é adiado, explicando o que mudou.
- Sugerir sessões curtas de revisão perto da data da prova, sem lotar o dia.
- Avisar quando o volume não cabe no tempo disponível, para o aluno decidir o que cortar ou antecipar.

Não publiquem, sem revisão de vocês, um texto ou uma decisão automática da IA como se já fosse a versão final.

## Extras opcionais

Só depois que o MVP estiver em pé. São ideias, não obrigações:

- Blocos com duração típica de foco (por exemplo, 25 ou 50 minutos).
- Importação simples de uma lista de prazos (arquivo ou formulário em lote).
- Prioridade ou peso diferente por disciplina.
- Espaço para uma sessão de estudo em grupo no plano.

## Fora de escopo

Para proteger o prazo e o foco do produto:

- Integração oficial com o sistema acadêmico do Senac (notas, diário de classe ou calendário institucional).
- Diagnosticar dificuldades de aprendizagem ou indicar conteúdos de uma prova específica.
- Substituir o calendário da disciplina publicado pelo professor.
- Um aplicativo completo de produtividade com dezenas de técnicas de estudo.

## Perguntas para guiar o time

Usem estas perguntas no primeiro dia, ao definir o MVP, e de novo antes da demo:

- O que o sistema faz quando o tempo disponível é menor do que o esforço estimado?
- Como o aluno corrige um plano que a IA deixou otimista demais?
- Como o cronograma reage a uma entrega que foi adiada ou antecipada no meio da semana?
- O plano é só uma sugestão, ou o aluno consegue travar blocos que não quer que sejam movidos?
- Como vocês evitam um calendário bonito, porém impossível de cumprir?

## Entregáveis e como a banca avalia

Esta é uma **imersão colaborativa** de uma semana, com times de 3 ou 4 estudantes do TDS. A banca avalia o trabalho de **vocês**, no próprio projeto — um retorno formativo, não uma comparação com outros times.

**O que precisa estar pronto no repositório** (raiz), em **inglês** (versão principal) e em **português** (`.pt-BR`), com o link entre os idiomas no topo de cada arquivo:

- `README.md` e `README.pt-BR.md`
- `DECISIONS.md` e `DECISIONS.pt-BR.md`
- `AI_LOG.md` e `AI_LOG.pt-BR.md`

A aplicação precisa ter **front-end e back-end**. O back-end pode ser **Java**, **Python** ou **Node.js** (isolados ou combinados, desde que integrados). O front-end é obrigatório (web, desktop ou mobile). Justifiquem a stack no `DECISIONS.md`. Também são obrigatórios o histórico com Conventional Commits, o trabalho via Pull Request com revisão, a proteção da `main` e uma pipeline de **GitHub Actions** verde (build, lint e testes) em PRs e na `main`. Não commitem segredos. No primeiro dia, adicionem o professor como colaborador: **adrianoanschau@gmail.com**.

O **congelamento do código** é no domingo, **29/11, às 23h59**. As apresentações são em **15 minutos + 5 minutos de perguntas**, nos dias 30/11 e 01/12, no Senac de Canoas, Sala 103. Ensaiem a demo e o roteiro (problema e solução, demo ao vivo, fluxo de trabalho, decisões, IA na prática e CI/CD).

A avaliação tem **duas bancas, com o mesmo peso**:

- **Banca Técnica (50%):** qualidade de código e arquitetura; Git, Conventional Commits e PRs; CI/CD; domínio do código nas perguntas.
- **Banca de Inovação e Impacto (50%):** originalidade; impacto (o problema real e para quem); experiência do usuário; clareza do pitch; uso criativo da IA.

Qualquer pessoa do time pode ser perguntada sobre qualquer parte do projeto. Usem a IA com inteligência, registrem o que ela gerou e o que vocês corrigiram, e assumam o código que forem apresentar.

Bom trabalho — e que a semana seja intensa, colaborativa e bem documentada.

