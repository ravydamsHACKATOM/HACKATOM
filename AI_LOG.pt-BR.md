# Registro de uso de IA (AI_LOG)

[English](AI_LOG.md) · **Português (Brasil)**

Como o time usou inteligência artificial no projeto: as ferramentas e, para cada uso relevante, o
prompt principal, o que a IA gerou, o que o time aproveitou e o que foi corrigido ou rejeitado, e
por quê. Este arquivo é o roteiro do bloco "IA na prática" da apresentação.

Chaves de API, senhas e outros segredos nunca entram no repositório. Eles ficam em variáveis de
ambiente e em GitHub Secrets.

## Ferramentas

- **ChatGPT**: planejamento, configuração do ambiente e análise do projeto nos primeiros dias.
- **Codex**: falhou com a nossa conta em 26/11; foi usado em 27/11 para revisar o primeiro
  protótipo em relação ao briefing.
- **Claude Code**: reconstrução da aplicação em 27/11 (código, testes, CI e documentação).
- **Z-Image Turbo no ComfyUI**, rodando localmente: ilustrações, ícones e texturas de papel.

## Registro

### 25/11/2026 — Configuração do GitHub Actions

- **Prompt principal:** criar um workflow de CI com GitHub Actions para executar o build em PRs e
  pushes na branch main, utilizando Java 21 e Maven.
- **O que a IA gerou:** arquivo `ci.yml` com a configuração do Java, do Maven e a execução do
  build.
- **O que o time aproveitou:** a estrutura do workflow e as orientações de configuração.
- **O que o time corrigiu ou rejeitou, e por quê:** retiramos temporariamente os testes para
  seguir a ordem solicitada. Também corrigimos a permissão de execução do `mvnw`, o que fez o
  build funcionar.

### 25/11/2026 — Planejamento e desenvolvimento do StudyFlow

- **Prompt principal:** pedir ajuda para transformar o protótipo do StudyFlow em um aplicativo
  funcional, respeitando as telas do Figma e usando HTML, CSS, Java, Spring Boot e SQLite.
- **O que a IA gerou:** proposta de organização do desenvolvimento, funcionalidades necessárias e
  etapas de implementação.
- **O que o time aproveitou:** os requisitos de funcionalidades, a orientação para integrar
  front-end e back-end e a organização por etapas.
- **O que o time corrigiu ou rejeitou, e por quê:** reforçamos a necessidade de manter o design
  original do Figma, evitando alterações visuais desnecessárias e soluções técnicas complexas demais.

### 26/11/2026 — Orientação sobre ferramentas de IA para desenvolvimento

- **Prompt principal:** pedir orientação sobre como usar o ChatGPT Work e o Codex para acessar,
  analisar e ajudar no desenvolvimento do StudyFlow, com o VS Code e o IntelliJ IDEA.
- **O que a IA gerou:** explicações sobre o funcionamento do Work, suas possibilidades de
  interação com arquivos e projetos, e orientações para usar o Codex no ambiente de
  desenvolvimento.
- **O que o time aproveitou:** as orientações para usar ferramentas de IA como apoio à análise do
  código, à identificação de erros e à implementação de melhorias.
- **O que o time corrigiu ou rejeitou, e por quê:** o Codex apresentou um erro de
  incompatibilidade de modelo com a conta do ChatGPT. Por isso, consideramos usar o VS Code com
  os recursos de IA dele.

### 26/11/2026 — Configuração do ambiente de desenvolvimento e Git

- **Prompt principal:** pedir ajuda para trabalhar com o projeto no VS Code, incluindo
  orientações para gerenciar branches pelo terminal.
- **O que a IA gerou:** instruções para identificar a branch atual, listar as branches e trocar
  de branch com comandos Git.
- **O que o time aproveitou:** as orientações para organizar o trabalho com Git e continuar o
  desenvolvimento na branch adequada.
- **O que o time corrigiu ou rejeitou, e por quê:** foi necessário adaptar o ambiente, por causa
  das dificuldades com o IntelliJ IDEA e o Codex.

### 26/11/2026 — Análise da estrutura e do estado do StudyFlow

- **Prompt principal:** pedir uma análise do projeto a partir do arquivo ZIP do repositório,
  comparando a estrutura com os requisitos do checklist do professor.
- **O que a IA gerou:** um levantamento da estrutura do projeto: arquivos existentes,
  documentação, GitHub Actions, testes, organização do back-end e pendências.
- **O que o time aproveitou:** a identificação dos pontos que precisavam de atenção:
  implementação do fluxo principal do MVP, testes automatizados, pipeline e documentação.
- **O que o time corrigiu ou rejeitou, e por quê:** usamos a análise como diagnóstico, sem tratar
  itens parcialmente implementados como concluídos. Vimos que arquivos de documentação e
  configurações ainda não representavam o estado esperado do projeto.

### 26/11/2026 — Planejamento das pendências para a apresentação

- **Prompt principal:** pedir a conferência do checklist do Dia 4 do Hack com IA, comparando as
  exigências do professor com o estado do projeto.
- **O que a IA gerou:** uma lista de prioridades dividida entre implementação, testes e pipeline,
  documentação, segurança e preparação da apresentação.
- **O que o time aproveitou:** a ordem das tarefas, com preferência para um fluxo funcional de
  ponta a ponta antes de funcionalidades secundárias e melhorias visuais.
- **O que o time corrigiu ou rejeitou, e por quê:** não registrado na época.

### 27/11/2026 — Revisão dos requisitos do primeiro protótipo (Codex)

- **Prompt principal:** comparar o projeto com o briefing do professor e implementar as melhorias
  viáveis.
- **O que a IA gerou:** uma análise de requisitos, a documentação bilíngue na raiz do repositório,
  uma etapa de Checkstyle na pipeline, e uma alteração para limitar os blocos gerados ao domingo
  da semana escolhida e executar a geração em uma transação, com um teste de regressão.
- **O que o time aproveitou:** a análise, que confirmou as lacunas que levaram à reconstrução, e
  as duas regras, que o agendador refeito também segue: os blocos nunca passam da semana
  escolhida e a geração roda em uma única transação.
- **O que o time corrigiu ou rejeitou, e por quê:** as primeiras verificações locais apontaram um
  import não utilizado e depois imports ausentes, que foram corrigidos. As alterações de código
  foram feitas no primeiro protótipo em paralelo com a reconstrução; como a reconstrução
  substitui esse protótipo, elas não foram levadas adiante, e a documentação dele foi unida aos
  arquivos da reconstrução.

### 27/11/2026 — Diagnóstico e plano de reconstrução (Claude Code)

- **Prompt principal:** "Leia todos os arquivos .md. O projeto atual não serve, vamos precisar
  refazer do zero."
- **O que a IA gerou:** a lista do que o briefing exige, os problemas do protótipo (pipeline
  apontando para uma pasta inexistente, documentos fora da raiz, sem IA no produto, pastas de IDE
  commitadas) e um plano de reconstrução com perguntas em aberto para o time.
- **O que o time aproveitou:** o diagnóstico e a estrutura proposta (`backend/`, `frontend/`,
  documentos na raiz).
- **O que o time corrigiu ou rejeitou, e por quê:** mantivemos Java e Spring Boot, que o time
  conhece, em vez de trocar de stack. Exigimos que todos os commits fossem em inglês e seguissem
  Conventional Commits à risca, e que a interface tivesse foco em design, com animações sutis.

### 27/11/2026 — Back-end e agendador (Claude Code)

- **Prompt principal:** refazer o back-end em Java 21 e Spring Boot cobrindo os seis requisitos
  do briefing, com a IA ajudando a propor e a refazer o plano.
- **O que a IA gerou:** a API REST (disciplinas, provas e entregas, horários livres, plano), o
  agendador em Java puro, o schema com Flyway e 35 testes automatizados (44 depois das contas).
- **O que o time aproveitou:** a separação entre o agendador determinístico e a explicação por IA,
  e as regras do agendamento (prazo mais próximo primeiro, sessões de 50 minutos, revisão antes da
  prova, aviso quando não cabe).
- **O que o time corrigiu ou rejeitou, e por quê:** um teste de API mostrou que, na primeira
  geração de uma semana, a explicação saía sem o aviso de "não coube", porque o resumo era montado
  antes de o plano ser marcado como gerado; o erro foi corrigido. A formatação do código passou a
  ser cobrada pelo build (Spotless), para não depender de revisão manual.

### 27/11/2026 — IA dentro do produto: explicação do plano (testada e retirada)

- **Prompt principal (instrução de sistema do produto):** "Você é o orientador de estudos do
  StudyFlow. Você recebe, em JSON, os fatos de um plano semanal já calculado por um algoritmo.
  Explique o plano em português, com tom calmo e direto. Use apenas os fatos recebidos. Não
  invente horários, datas, disciplinas ou conteúdos de prova."
- **O que a IA gerou:** um resumo de uma a três frases e até quatro dicas práticas, em formato
  estruturado (JSON validado pelo SDK).
- **O que o time aproveitou:** a IA como explicadora: o que mudou depois de um ajuste, qual é o
  dia mais cheio, o que fazer quando falta tempo.
- **O que o time corrigiu ou rejeitou, e por quê:** rejeitamos deixar a IA montar o cronograma,
  porque ela poderia marcar blocos fora dos horários livres e isso não é testável. Também
  rejeitamos tornar a IA obrigatória: sem chave ou em caso de falha, um orientador por regras
  respondia, e a interface mostrava de onde vinha o texto. Mais tarde, no mesmo dia, retiramos a
  integração inteira: o produto não deve depender de uma chave paga, e a explicação por regras
  cobria o mesmo terreno. A integração nunca chegou a ser exercitada contra a API real.

### 27/11/2026 — Interface (Claude Code)

- **Prompt principal:** "Quero que o app seja bonito, profissional, sutilmente animado, com foco
  no design", com um fundo de papel texturizado que não canse a vista.
- **O que a IA gerou:** um sistema de design em tokens CSS (papel e tinta, temas claro e escuro),
  seis telas em HTML, CSS e módulos JavaScript, e as animações.
- **O que o time aproveitou:** a direção visual, a tela "Hoje" como entrada e a semana de exemplo
  que pode ser carregada com um clique na demo.
- **O que o time corrigiu ou rejeitou, e por quê:** na revisão no navegador, as colunas da semana
  estavam apertadas e quebravam palavras no meio; a grade ganhou largura mínima e rolagem
  horizontal. O rótulo "sem bloco" aparecia em vermelho até para prazos de semanas futuras, o que
  assustava sem motivo; agora só fica em destaque o que o plano atual realmente não conseguiu
  encaixar. A ilustração da capa tinha um tom de papel diferente do fundo e foi mesclada por CSS.

### 27/11/2026 — Ilustrações, ícones e textura de papel (modelo de imagem local)

- **Prompt principal:** direção de arte única para todas as imagens: "visual calmo, editorial, de
  papel e tinta; paleta creme, verde sálvia, terracota, mostarda e azul acinzentado; minimalista,
  muito espaço vazio, sem texto, sem logotipos, sem rostos", mais a descrição de cada peça.
- **O que a IA gerou:** vários candidatos por imagem com o Z-Image Turbo no ComfyUI, rodando no
  próprio notebook: textura de papel, ilustração da capa, quatro ilustrações de estado vazio e dez
  ícones de disciplina.
- **O que o time aproveitou:** 17 imagens finais em WebP, escolhidas entre três ou quatro
  candidatos cada.
- **O que o time corrigiu ou rejeitou, e por quê:** candidatos com texto ilegível ou bordas sujas
  foram descartados. O ícone de História foi refeito duas vezes, porque o recorte automático do
  fundo abria buracos no vidro da ampulheta. A textura de papel gerada tinha manchas e emendas
  visíveis ao repetir; ela foi tratada por código (filtro, mistura das bordas e contraste
  reduzido a cerca de 3%) até repetir sem emendas. Limites conhecidos: o verde das ilustrações
  varia um pouco em relação ao da interface, e o ícone de Geografia é mais detalhado que os outros.

### 27/11/2026 — Pipeline e padrão de commits (Claude Code)

- **Prompt principal:** "Todos os commits têm que ser em inglês e seguir o melhor padrão
  possível. Veja se algum documento menciona isso."
- **O que a IA gerou:** a confirmação de que o briefing exige Conventional Commits, as regras
  escritas no `AGENTS.md`, o workflow com três jobs (back-end, front-end e commits) e a
  configuração do commitlint.
- **O que o time aproveitou:** a validação automática das mensagens em todo pull request.
- **O que o time corrigiu ou rejeitou, e por quê:** a primeira versão do commitlint não achava a
  própria configuração na pipeline; ele passou a ser instalado como dependência do repositório. Um
  commit antigo do histórico não passa na regra e não pode ser reescrito sem alterar o histórico
  compartilhado, então foi listado como exceção, com a justificativa no arquivo de configuração.

### 27/11/2026 — Contas, login e dados por usuário (Claude Code)

- **Prompt principal:** "Cria um sistema de cadastro e login. Não precisa de integração com IA
  (chave da IA)."
- **O que a IA gerou:** as tabelas de usuários e sessões, o hash de senhas com BCrypt, o cookie de
  sessão, um interceptador que protege a API, o filtro por dono em todas as consultas, nove testes
  de API e o cartão de login na capa.
- **O que o time aproveitou:** a sessão em cookie HttpOnly com só o resumo guardado, a mesma
  resposta para senha errada e e-mail desconhecido, e o teste que prova que uma conta não lê nem
  altera os dados de outra.
- **O que o time corrigiu ou rejeitou, e por quê:** rejeitamos o framework Spring Security
  completo e ficamos com o módulo de senhas dele mais um interceptador pequeno, que o time
  consegue explicar. A primeira execução da pipeline no GitHub falhou por motivos que não
  apareciam nas nossas máquinas com Windows: arquivos commitados com quebra de linha do Windows
  reprovavam na checagem de formato no Linux, e o comando de teste do front-end só funcionava em
  um Node mais novo. Os dois foram corrigidos com um arquivo `.gitattributes` e um padrão de
  arquivos no comando de teste, e conferidos no Node 22.

### 27/11/2026 — Modais e select próprio (Claude Code)

- **Prompt principal:** "Melhora todos os modais e selects para ficarem mais premium, sutilmente
  animados. O select deve ter o ícone da disciplina. Usa ícones, quadros com cada cartão, nada
  simples."
- **O que a IA gerou:** um novo layout de modal feito de painéis, cartões de escolha para o tipo,
  o peso e os dias da semana, um contador com atalhos para as horas, e um select próprio com
  ícones, uso pelo teclado e papéis para leitores de tela.
- **O que o time aproveitou:** o layout, as dicas ao vivo ("em 7 dias", "cerca de 3 sessões") e a
  opção de adicionar o mesmo horário livre em vários dias de uma vez.
- **O que o time corrigiu ou rejeitou, e por quê:** no navegador, a lista do select fechava
  sozinha logo depois de abrir, porque rolar até a opção ativa também rolava o modal, e o select
  tratava isso como motivo para fechar. Depois a lista aparecia fora do lugar, porque os efeitos
  visuais do modal mudavam a referência da "posição fixa". Agora a lista é rolada manualmente e
  vai para o fim da página enquanto está aberta.

### 27/11/2026 — Redesenho da tela Hoje e da barra lateral (Claude Code)

- **Prompt principal:** uma captura da tela Hoje com "melhora a tela, deixa mais bonita,
  sutilmente animada e profissional, aproveita e incrementa o design da sidebar".
- **O que a IA gerou:** um cabeçalho de saudação com o progresso do dia, quatro quadros de resumo,
  um cartão em destaque para o bloco da vez com o aviso "começa em" atualizado ao vivo, uma linha
  do tempo do dia, um resumo da semana, e uma barra lateral com o progresso da semana, navegação
  em grupos com contadores, uma chave de tema e o cartão da conta. No celular entrou uma barra no
  topo para a conta.
- **O que o time aproveitou:** o layout inteiro.
- **O que o time corrigiu ou rejeitou, e por quê:** no navegador, a luz suave atrás da saudação
  terminava em uma borda vertical dura no tema escuro, e "Provas e entregas" quebrava em duas
  linhas na barra lateral; os dois foram corrigidos. No celular os quadros de resumo ficavam
  apertados, então os ícones deles somem ali. Antes desta mudança, não havia como sair da conta
  pelo celular.
