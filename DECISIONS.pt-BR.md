# Decisões do projeto

[English](DECISIONS.md) · **Português (Brasil)**

As decisões importantes do time, na ordem em que foram tomadas. Cada entrada traz o que foi
decidido, as alternativas consideradas e o motivo da escolha. Este arquivo é o roteiro do bloco de
decisões da apresentação.

## 23/11/2026 — Tech Lead

- **Decisão:** Ana Carolina é a Tech Lead.
- **Alternativas consideradas:** Raphael.
- **Motivo:** Ana tem mais conhecimento.

## 23/11/2026 — Responsável pela apresentação

- **Decisão:** Victor apresenta o projeto.
- **Alternativas consideradas:** Yuri.
- **Motivo:** Victor é comunicativo e tem conhecimento de inglês.

## 24/11/2026 — Stack do back-end

- **Decisão:** Java com Spring Boot.
- **Alternativas consideradas:** Node.js.
- **Motivo:** todos os integrantes são familiarizados com Java.

## 24/11/2026 — Stack do front-end

- **Decisão:** HTML, CSS e JavaScript, sem framework.
- **Alternativas consideradas:** nenhuma.
- **Motivo:** facilidade; o projeto não é tão grande.

## 25/11/2026 — Esqueleto do projeto

- **Decisão:** gerar o esqueleto do projeto e um MVP funcionando com apoio de IA.
- **Alternativas consideradas:** Claude.
- **Motivo:** a ferramenta de IA escolhida na época era mais funcional para o time.

## 27/11/2026 — Refazer a aplicação do zero

- **Decisão:** descartar o primeiro protótipo e refazer back-end e front-end, mantendo a stack.
- **Alternativas consideradas:** corrigir o protótipo onde estava.
- **Motivo:** o protótipo tinha pastas aninhadas, uma pipeline apontando para uma pasta que não
  existia, documentos fora da raiz do repositório, um único script de 1.700 linhas no front-end e
  nenhuma IA no produto. Corrigir cada ponto custaria mais do que refazer usando o checklist do
  briefing como guia, e o resultado ainda seria mais difícil de explicar.

## 27/11/2026 — Recorte do MVP

- **Decisão:** entregar os seis requisitos do briefing (cadastro, disponibilidade, geração,
  visualização, conclusão e ajuste, "o que fazer hoje") mais a explicação do plano, e nada além.
- **Ficou de fora:** integração com o sistema acadêmico, notificações, importação de dados e
  sessões de estudo em grupo.
- **Motivo:** o briefing coloca o primeiro fora de escopo, e os outros não mudam o fluxo
  principal. As contas também ficaram de fora no início e entraram mais tarde no mesmo dia (veja
  abaixo).

## 27/11/2026 — O agendador é determinístico; a IA só explica

- **Decisão:** um conjunto de regras fixas, escrito em Java puro, decide o cronograma. A IA recebe
  o resultado e escreve a explicação e as dicas.
- **Alternativas consideradas:** pedir ao modelo de linguagem que montasse o cronograma.
- **Motivo:** um modelo de linguagem pode colocar um bloco fora do horário livre do estudante,
  marcar a mesma hora duas vezes ou mudar a resposta entre uma execução e outra, e nada disso
  pode ser testado. Com regras fixas, "um bloco nunca cai fora da disponibilidade" é uma garantia
  coberta por testes, e a resposta para "como vocês evitam um calendário bonito, porém
  impossível?" é o próprio código. A IA faz o que faz bem: transformar números em uma explicação
  clara e humana.

## 27/11/2026 — Regras do agendamento

- **Decisão:** sessões de foco de 50 minutos com pausas de 10; prazo mais próximo primeiro, com
  desempate pelo peso da disciplina; troca de matéria depois de duas sessões seguidas quando a
  mais urgente ainda tem folga; não se estuda para a prova no dia da prova, e a última sessão é
  de revisão; prazos além da semana ocupam só uma parte proporcional dela.
- **Alternativas consideradas:** distribuir todos os eventos igualmente pela semana.
- **Motivo:** "prazo mais próximo primeiro" é a regra que garante que, se tudo cabe, tudo entra.
  A distribuição igual parece equilibrada, mas pode deixar a prova de sexta com estudo de menos.
  A troca de matéria evita quatro horas seguidas do mesmo conteúdo sem colocar prazo em risco.

## 27/11/2026 — O que não cabe é mostrado, não escondido

- **Decisão:** quando o tempo disponível é menor do que o esforço estimado, o plano agenda o que
  cabe e mostra um aviso com o tempo que falta em cada evento.
- **Alternativas consideradas:** encolher as sessões ou passar da disponibilidade para caber tudo.
- **Motivo:** quem pode decidir o que cortar, antecipar ou reestimar é o estudante. Um plano que
  lota a agenda em silêncio repetiria o problema que o produto existe para resolver.

## 27/11/2026 — Ajuste do plano: concluído, travado, adiado

- **Decisão:** refazer o plano mantém os blocos concluídos, os travados e os horários adiados, e
  redistribui o resto. Blocos cujo horário passou sem serem feitos voltam para a fila.
- **Alternativas consideradas:** sempre refazer a semana inteira do zero.
- **Motivo:** responde a três perguntas do briefing: o estudante pode travar blocos que não quer
  que sejam movidos, pode corrigir um plano otimista adiando, e nunca perde o que já fez quando um
  prazo muda. A interface lista o que mudou depois de cada ajuste.

## 27/11/2026 — Provedor de IA e plano B (substituída)

- **Decisão:** Claude (Anthropic) pelo SDK oficial para Java, com saída estruturada. A chave vem
  só da variável de ambiente `ANTHROPIC_API_KEY`. Sem chave, ou em qualquer falha, responde um
  orientador por regras.
- **Alternativas consideradas:** tornar a IA obrigatória; chamar a API HTTP manualmente.
- **Motivo:** a demo não pode depender de rede nem de cota, e a pipeline precisa rodar sem
  segredos. O SDK oficial dá requisições tipadas e um formato de resposta validado.
- **Situação:** substituída por "Sem serviço pago de IA no produto", abaixo.

## 27/11/2026 — SQLite com SQL explícito e Flyway

- **Decisão:** SQLite acessado com o `JdbcClient` do Spring, com o schema versionado pelo Flyway.
- **Alternativas consideradas:** PostgreSQL; JPA/Hibernate.
- **Motivo:** o SQLite não precisa de instalação, então qualquer pessoa do time (e a banca) roda
  o projeto com um comando. Com cinco tabelas pequenas, o SQL explícito é mais fácil de ler e de
  explicar do que mapeamentos de ORM, e o Hibernate não tem dialeto oficial para SQLite.

## 27/11/2026 — Uma aplicação, um comando

- **Decisão:** o back-end serve o front-end a partir do mesmo jar e da mesma origem.
- **Alternativas consideradas:** um servidor separado para o front-end.
- **Motivo:** não há CORS para configurar nem um segundo processo para subir na demo, e as pastas
  continuam separadas (`backend/`, `frontend/`), cada uma com lint e testes próprios.

## 27/11/2026 — Direção visual

- **Decisão:** um visual de "papel e tinta": superfícies quentes de baixo contraste com uma
  textura sutil de papel, um único verde calmo como destaque, serifa nos títulos e temas claro e
  escuro. As animações são curtas e desligam quando o sistema pede movimento reduzido.
- **Alternativas consideradas:** o visual padrão de uma biblioteca de componentes; uma paleta
  viva, de alto contraste.
- **Motivo:** um planejador é aberto várias vezes por dia, muitas delas à noite. Ele precisa ser
  calmo e não cansar a vista. As ilustrações e os ícones foram gerados com um modelo de imagem
  local a partir de uma única direção de arte, para o conjunto ficar coerente.

## 27/11/2026 — Padrão de commits cobrado pela pipeline

- **Decisão:** Conventional Commits em inglês, validados pelo commitlint em todo pull request, com
  as regras escritas no `AGENTS.md`.
- **Alternativas consideradas:** confiar na revisão para pegar mensagens ruins.
- **Motivo:** os primeiros dias do histórico tinham mensagens como "updating file". Uma checagem
  automática não custa nada e mantém o histórico legível para a banca técnica.

## 27/11/2026 — Contas com cadastro e login

- **Decisão:** adicionar contas. As senhas são guardadas como hash BCrypt. A sessão é um token
  aleatório em um cookie HttpOnly e SameSite=Lax, e o banco guarda só o resumo SHA-256 dele. Toda
  tabela pertence a um dono, e toda consulta filtra por ele.
- **Alternativas consideradas:** manter a versão de um único estudante; JWT no armazenamento
  local do navegador; o framework Spring Security completo.
- **Motivo:** um planejador guarda dados pessoais de rotina, e com contas vários estudantes podem
  usar a mesma instalação, inclusive na demo. Um cookie que os scripts da página não conseguem ler
  é mais seguro do que um token no armazenamento local, e uma sessão no servidor pode ser
  encerrada no logout. Usamos só o módulo de hash de senhas do Spring Security e escrevemos um
  interceptador pequeno, que o time inteiro consegue ler e explicar, em vez de configurar o
  framework completo.
- **Ficou de fora:** recuperação de senha por e-mail e limite de tentativas de login, que pedem
  infraestrutura fora do MVP.

## 27/11/2026 — Sem serviço pago de IA no produto

- **Decisão:** retirar a explicação escrita por um modelo de linguagem e a chave de API dela. O
  plano é explicado só pelo orientador por regras.
- **Alternativas consideradas:** manter a integração opcional com o plano B.
- **Motivo:** o produto não pode depender de uma chave paga que o time teria de administrar e
  proteger, e a explicação por regras já respondia às mesmas perguntas nos nossos testes. A IA
  continua no processo (código, testes, imagens, documentação), documentada no registro de IA. A
  decisão anterior de que a IA nunca decide o cronograma fez dessa retirada uma mudança pequena:
  só a camada de explicação foi afetada.

## 27/11/2026 — Modais em cartões e select próprio

- **Decisão:** refazer todos os modais como um conjunto de painéis com cartões de escolha, um
  contador e atalhos de preenchimento, e trocar o select nativo por um próprio, que mostra o ícone
  e a cor da disciplina.
- **Alternativas consideradas:** manter os controles nativos com um estilo leve.
- **Motivo:** o select nativo não mostra ícones e muda de aparência em cada sistema. O nosso
  mantém o que torna um controle nativo usável: um valor real de formulário, uso completo pelo
  teclado (setas, Enter, Esc, digitar para buscar) e os papéis de listbox para leitores de tela.

## 28/11/2026 — Organizar a semana: divisão do tempo, modo espalhado, mover sessões e avisos

- **Decisão:** o estudante pode escolher quantas horas cada prova ou entrega recebe na semana,
  pedir que as sessões sejam espalhadas pelos dias, mover uma sessão para outro dia ou horário e
  receber um aviso na página quando uma sessão começa. Sessões concluídas saem da lista de
  pendentes do dia e continuam listadas como concluídas.
- **Como funciona:** as escolhas ficam guardadas por semana (`week_preferences` e
  `week_allocations`, migração V3) e o agendador continua sendo o único lugar que posiciona
  blocos: a divisão escolhida substitui a automática, limitada ao esforço que falta; o modo
  espalhado limita cada dia a uma parte igual da semana e depois usa o tempo livre que sobrar,
  então nunca deixa de fora um estudo que caberia. Uma sessão movida mantém a duração, fica
  travada no novo lugar e precisa ficar na mesma semana, antes do último dia de estudo, no futuro
  e sem sobrepor outra. A data da prova ou entrega nunca muda.
- **Alternativas consideradas:** mudar a data do prazo quando a sessão muda de dia (rejeitada:
  esconderia o prazo real) e notificações do navegador ou push (ficaram de fora: pedem permissão
  ou um servidor que envie mensagens; o aviso na página atende quem está com o StudyFlow aberto).
  Isto atualiza o item "notificações" que ficou fora do recorte do MVP, acima.
- **Motivo:** com vários prazos nos mesmos dias, a ordem automática (o mais urgente primeiro)
  pode encher as noites com uma única matéria. Os estudantes pediram para decidir a divisão e
  estudar um pouco por dia, sem perder de vista as datas reais.
