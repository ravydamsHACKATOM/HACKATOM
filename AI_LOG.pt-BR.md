# Registro de uso de IA

## 2026-11-26 — Integração e confiabilidade do StudyFlow
- **Resumo do prompt:** Analisar os requisitos enviados, inspecionar o repositório antes de editar e depois integrar a interface à API REST Spring, aprimorando persistência, validações, cronograma, testes, CI e documentação.
- **Trabalho gerado:** Refatoração dos contratos REST e serviços, separação dos arquivos estáticos, migrações Liquibase, tratamento de erros, testes de integração e documentação bilíngue.
- **Aproveitado:** As mudanças foram guiadas pelo projeto Maven real; os testes ajudaram a encontrar e corrigir diferenças de tipos entre SQLite e JPA e problemas na geração do cronograma.
- **Corrigido ou rejeitado:** A dependência Flyway para SQLite foi descartada porque o Maven Central não conseguiu resolver o artefato neste ambiente; Liquibase foi adotado. Os bancos locais foram retirados do Git e suas cópias locais foram preservadas.
- **Revisão humana necessária:** Confirmar a necessidade de autenticação antes de qualquer hospedagem compartilhada/pública. O MVP atual é local e de usuário único.
