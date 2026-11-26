# Registro de uso de IA (AI_LOG)

[English](AI_LOG.md) · **Português (Brasil)**

Registrem aqui como o time usou inteligência artificial no projeto. O uso de IA é liberado e incentivado, desde que
documentado: este arquivo é entregável obrigatório e serve de roteiro para o bloco “IA na prática” da apresentação.

A documentação do projeto deve ser escrita **em inglês** (a versão principal é o [`AI_LOG.md`](AI_LOG.md)), com uma *
*versão em português** ao lado. Mantenham as duas versões alinhadas.

Mantenham este arquivo na **raiz** do repositório. Incluam as ferramentas utilizadas e, para cada uso relevante, o
prompt principal, o que a IA gerou, o que o time aproveitou e o que foi corrigido ou rejeitado — e por quê.

Nunca commitem chaves de API, senhas ou outros segredos. Esses valores vão em `.env` (fora do repositório) e em GitHub
Secrets.

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

- **Prompt principal:** "Gere um middleware de autenticação JWT em Node.js/Express, com tratamento de token expirado e
  uma rota protegida de exemplo."
- **O que a IA gerou:** um middleware e um exemplo de rota, com uma dependência desatualizada e sem validação de
  variáveis de ambiente.
- **O que o time aproveitou:** a estrutura do middleware e o fluxo de verificação do token.
- **O que o time corrigiu ou rejeitou, e por quê:** atualizamos a biblioteca, movemos o segredo para variável de
  ambiente e incluímos testes. O exemplo de rota foi reescrito para se adequar ao nosso modelo de usuário.

# Registro de uso de IA — Projeto StudyFlow

### 25/11/2026 — Desenvolvimento do frontend da agenda de estudos

- **Prompt principal:** Solicitação para criar o frontend de uma aplicação de agenda de estudos, seguindo o briefing
  fornecido pelo professor.
- **O que a IA gerou:** Um arquivo `index.html` completo, reunindo HTML, CSS e JavaScript, com a interface e as
  funcionalidades iniciais do StudyFlow.
- **O que o time aproveitou:** A estrutura inicial da interface, a organização visual e as funcionalidades propostas
  para gerenciamento dos estudos.
- **O que o time corrigiu ou rejeitou, e por quê:** Foram feitos ajustes e adaptações conforme a estrutura e as
  necessidades do projeto.

### 25/11/2026 — Desenvolvimento do backend: Controller

- **Prompt principal:** Solicitação para criar o Controller do StudyFlow em Java com Spring Boot, gerando os arquivos
  individualmente.
- **O que a IA gerou:** O arquivo `StudyFlowController.java`, com rotas REST para matérias, provas, entregas,
  disponibilidades, cronograma e tarefas do dia.
- **O que o time aproveitou:** A estrutura do Controller, os endpoints e a comunicação planejada entre frontend e
  backend.
- **O que o time corrigiu ou rejeitou, e por quê:** O pacote e os imports foram ajustados para corresponder à estrutura
  real do projeto (`com.example.studyFlow`), evitando incompatibilidade entre os arquivos.

### 25/11/2026 — Desenvolvimento do backend: Service

- **Prompt principal:** Solicitação para desenvolver a camada Service responsável pelas regras de negócio da aplicação.
- **O que a IA gerou:** O arquivo `StudyFlowService.java`, com operações de cadastro, consulta, edição e exclusão, além
  da geração de cronogramas e conclusão de blocos de estudo.
- **O que o time aproveitou:** A organização das regras de negócio e a lógica de gerenciamento das sessões de estudo.
- **O que o time corrigiu ou rejeitou, e por quê:** O Service foi atualizado para utilizar os repositories e as
  entidades do projeto, substituindo a implementação inicial baseada em dados armazenados em memória.

### 25/11/2026 — Criação das entidades e repositories

- **Prompt principal:** Solicitação para criar individualmente os arquivos necessários para estruturar os dados do
  StudyFlow.
- **O que a IA gerou:** As entidades `Subject.java`, `StudyEvent.java`, `Availability.java` e `StudyBlock.java`, além
  dos repositories para acesso aos dados.
- **O que o time aproveitou:** A estrutura das entidades, os relacionamentos entre matérias e eventos e os métodos de
  consulta dos repositories.
- **O que o time corrigiu ou rejeitou, e por quê:** Os nomes dos pacotes e imports foram revisados para manter todos os
  arquivos compatíveis com a organização real do projeto.

### 25/11/2026 — Configuração do banco de dados SQLite

- **Prompt principal:** Solicitação de orientação para configurar um banco de dados SQLite no projeto Spring Boot.
- **O que a IA gerou:** Orientações para ajustar o `pom.xml` e o `application.properties`, incluindo as dependências do
  driver SQLite e do dialeto Hibernate.
- **O que o time aproveitou:** A configuração do SQLite como banco de dados, utilizando o arquivo `study-flow.db`, e a
  configuração de criação e atualização automática das tabelas.
- **O que o time corrigiu ou rejeitou, e por quê:** O caminho de conexão com o banco foi ajustado para evitar
  referências a diretórios incorretos. Também foram revisadas as dependências e configurações para adequação ao projeto.