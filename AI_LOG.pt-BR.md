# Registro de uso de IA (AI_LOG)

[English](AI_LOG.md) · **Português (Brasil)**

Registrem aqui como o time usou inteligência artificial no projeto. O uso de IA é liberado e incentivado, desde que documentado: este arquivo é entregável obrigatório e serve de roteiro para o bloco “IA na prática” da apresentação.

A documentação do projeto deve ser escrita **em inglês** (a versão principal é o [`AI_LOG.md`](AI_LOG.md)), com uma **versão em português** ao lado. Mantenham as duas versões alinhadas.

Mantenham este arquivo na **raiz** do repositório. Incluam as ferramentas utilizadas e, para cada uso relevante, o prompt principal, o que a IA gerou, o que o time aproveitou e o que foi corrigido ou rejeitado — e por quê.

Nunca commitem chaves de API, senhas ou outros segredos. Esses valores vão em `.env` (fora do repositório) e em GitHub Secrets.

Copiem o modelo abaixo sempre que precisarem de uma entrada nova.

## Ferramentas

- (ex.: ChatGPT, GitHub Copilot, Cursor, outro)

## Modelo de registro

```markdown
### [data] — [tarefa ou trecho]
- **Prompt principal:**
- **O que a IA gerou:**
- **O que o time aproveitou:**
- **O que o time corrigiu ou rejeitou, e por quê:**
```

## Registro

### 24/11/2026 — Autenticação JWT (exemplo)

- **Prompt principal:** "Gere um middleware de autenticação JWT em Node.js/Express, com tratamento de token expirado e uma rota protegida de exemplo."
- **O que a IA gerou:** um middleware e um exemplo de rota, com uma dependência desatualizada e sem validação de variáveis de ambiente.
- **O que o time aproveitou:** a estrutura do middleware e o fluxo de verificação do token.
- **O que o time corrigiu ou rejeitou, e por quê:** atualizamos a biblioteca, movemos o segredo para variável de ambiente e incluímos testes. O exemplo de rota foi reescrito para se adequar ao nosso modelo de usuário.

### [25/11] — [Configuração do GitHub Actions]

- **Prompt principal:**Criar um workflow de CI com GitHub Actions para executar o build em PRs e pushes na branch main, utilizando Java 21 e Maven.
- **O que a IA gerou:**Arquivo ci.yml com configuração do Java, Maven e execução do build.
- **O que o time aproveitou:**A estrutura do workflow e as orientações para configuração e execução.
- **O que o time corrigiu ou rejeitou, e por quê:**Retiramos temporariamente os testes para seguir a ordem solicitada. Também corrigimos a permissão de execução do mvnw, fazendo o build funcionar corretamente.


### [25/11] — [Planejamento e desenvolvimento do StudyFlow]

- **Prompt principal:** Solicitar ajuda para transformar o protótipo do StudyFlow em um aplicativo funcional, respeitando as telas do Figma e utilizando HTML, CSS, Java, Spring Boot e SQLite.
- **O que a IA gerou:** Proposta de organização do desenvolvimento, funcionalidades necessárias e etapas de implementação do sistema.
- **O que o time aproveitou:** Os requisitos de funcionalidades, a orientação para integrar front-end e back-end e a organização do desenvolvimento por etapas.
- **O que o time corrigiu ou rejeitou, e por quê:** Foi reforçada a necessidade de manter fielmente o design original do Figma, evitando alterações visuais desnecessárias e soluções técnicas excessivamente complexas.


### [26/11] — [Orientação sobre ferramentas de IA para desenvolvimento]

- **Prompt principal:** Solicitar orientação sobre como utilizar o ChatGPT Work e o Codex para acessar, analisar e auxiliar diretamente no desenvolvimento do projeto StudyFlow, utilizando o VS Code e o IntelliJ IDEA.
- **O que a IA gerou:** Explicações sobre o funcionamento do Work, suas possibilidades de interação com arquivos e projetos, além de orientações para utilizar o Codex no ambiente de desenvolvimento.
- **O que o time aproveitou:** As orientações para utilizar ferramentas de IA como apoio à análise do código, identificação de erros e implementação de melhorias.
- **O que o time corrigiu ou rejeitou, e por quê:** A utilização do Codex apresentou um erro de incompatibilidade de modelo com a conta do ChatGPT. Por isso, foi considerada a alternativa de utilizar o VS Code com seus recursos de IA.

### [26/11] — [Configuração do ambiente de desenvolvimento e Git]

- **Prompt principal:** Solicitar ajuda para trabalhar com o projeto StudyFlow no VS Code, incluindo orientações para gerenciar branches pelo terminal.
- **O que a IA gerou:** Instruções para identificar a branch atual, visualizar as branches disponíveis e realizar a troca de branch utilizando comandos Git.
- **O que o time aproveitou:** As orientações para organizar o trabalho com Git e continuar o desenvolvimento na branch adequada.
- **O que o time corrigiu ou rejeitou, e por quê:** Foi necessário adaptar o ambiente de desenvolvimento, considerando as dificuldades encontradas na utilização do IntelliJ IDEA e do Codex.


### [26/11] — [Análise da estrutura e do estado atual do StudyFlow]

- **Prompt principal:** Solicitar uma análise do projeto StudyFlow a partir do arquivo ZIP do repositório, sem depender do envio de capturas de tela, comparando sua estrutura com os requisitos do checklist do professor.
- **O que a IA gerou:** Um levantamento da estrutura do projeto, identificando arquivos existentes, documentação, configuração do GitHub Actions, testes, organização do back-end e possíveis pendências.
- **O que o time aproveitou:** A identificação dos pontos que precisam de atenção para preparar o projeto para a apresentação, principalmente a implementação do fluxo principal do MVP, os testes automatizados, a pipeline e a documentação.
- **O que o time corrigiu ou rejeitou, e por quê:** A análise foi utilizada como diagnóstico, sem considerar itens parcialmente implementados como concluídos. Foi identificada a necessidade de verificar e atualizar arquivos de documentação e configurações que ainda não representam completamente o estado esperado do projeto.

### [26/11] — [Planejamento das pendências para a apresentação do MVP]

- **Prompt principal:** Solicitar a conferência do checklist do Dia 4 do Hack com IA, comparando as exigências do professor com o estado atual do projeto StudyFlow.
- **O que a IA gerou:** Uma lista de prioridades dividida entre implementação do sistema, testes e pipeline, documentação, segurança e preparação da apresentação.
- **O que o time aproveitou:** A organização das tarefas por prioridade, dando preferência à implementação de um fluxo funcional de ponta a ponta antes de investir em funcionalidades secundárias e melhorias visuais.
- **O que o time corrigiu ou rejeitou, e por quê:**