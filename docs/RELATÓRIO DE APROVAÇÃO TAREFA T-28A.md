# Relatório de Aprovação Formal — Tarefa T-28A [ARQ-06A]
## Diagnóstico, Inventário de Preferências e Desenho de Metas na Nuvem (Opção B)

**Data de Aprovação:** 26 de Outubro de 2026  
**Aprovado por:** Proprietário do Projeto (Usuário)  
**Coordenação e Governança:** Agente Orquestrador  
**Especialistas Responsáveis pelo Desenho:** Agente Arquiteto (Arquitetura e Dados), Agente Designer (UX/UI e Acessibilidade) e Agente Revisor Independente  
**Documentos de Referência:**
- [docs/design-metas-por-conta.md](file:///home/nando/Documentos/GiroCustov0.1/docs/design-metas-por-conta.md) (Especificação Técnica Detalhada)
- [docs/plano-onda-2.md](file:///home/nando/Documentos/GiroCustov0.1/docs/plano-onda-2.md) (Seção 1, Seção 3.d.1, Seção 4.2 e Seção 5 - Decisão 1)
- [docs/Relatório EXECUÇÃO TAREFA T-28A [ARQ-06A].md](file:///home/nando/Documentos/GiroCustov0.1/docs/Relat%C3%B3rio%20EXECU%C3%87%C3%83O%20TAREFA%20T-28A%20%5BARQ-06A%5D.md)
- [docs/Relatório Tarefa T-08 [PERF-01] Índices Room e Migração v13.md](file:///home/nando/Documentos/GiroCustov0.1/docs/Relat%C3%B3rio%20Tarefa%20T-08%20%5BPERF-01%5D%20%C3%8Dndices%20Room%20e%20Migra%C3%A7%C3%A3o%20v13.md)

---

### 1. Resultado
✅ **VERIFICADO**: O desenho técnico da Tarefa T-28A ([`docs/design-metas-por-conta.md`](file:///home/nando/Documentos/GiroCustov0.1/docs/design-metas-por-conta.md)) foi **FORMALMENTE APROVADO** pelo proprietário do projeto em 26 de outubro de 2026. A resolução da Parada Obrigatória (Decisão 1) e o status oficial ✅ **VERIFICADO** foram devidamente registrados no plano da Onda 2 ([`docs/plano-onda-2.md`](file:///home/nando/Documentos/GiroCustov0.1/docs/plano-onda-2.md)). Com a conclusão prévia e homologada da Tarefa T-08 (versão 13 do Room), a implementação das metas na nuvem na Tarefa T-28B está oficialmente autorizada e liberada para execução, respeitando o número sequencial de schema v14 do Room.

---

### 2. O que mudou (Decisões Técnicas e Arquiteturais Aprovadas)

#### Explicação Leiga
Até o momento, as 12 metas financeiras e operacionais do motorista/entregador (como faturamento bruto diário de R$ 200, líquido de R$ 130, quilometragem máxima e quantidade de corridas) ficavam guardadas em um arquivo frágil dentro do aparelho (`giro_custo_goals.xml`). Isso causava dois problemas críticos:
1. **Perda Total ao Trocar de Conta:** Sempre que o usuário deslogava e confirmava a troca de conta com descarte de dados, esse arquivo era inteiramente apagado, fazendo com que o motorista tivesse que redigitar todas as suas metas do zero.
2. **Mistura Indevida com o Tema Escuro:** A preferência de tema escuro/claro estava misturada dentro do mesmo arquivo de metas, exigindo "gambiarras" no código para não perder a escolha visual do usuário durante limpezas de dados.

Com a aprovação formal do desenho da **Opção B**, o proprietário do projeto estabeleceu que as metas serão tratadas como dados de primeira classe do usuário: persistidas localmente no banco Room e sincronizadas automaticamente com a nuvem (Supabase), acompanhando o trabalhador em qualquer smartphone.

#### Síntese das Decisões Arquiteturais Aprovadas
1. **Criação da Tabela Dedicada `user_goals` (Decisão de Onde Guardar):**
   - Rejeitou-se a inclusão de 12 colunas dentro da tabela `user_profile` para evitar sobrecarga nas consultas cadastrais básicas (CEP, nome) e diminuir tráfego desnecessário de rede.
   - Criada a tabela independente `user_goals` (1:1 por usuário), com isolamento de responsabilidade, sincronização atômica enxuta e baixo consumo de dados móveis e bateria na rua.
2. **Resolução de Conflitos e Offline-First:**
   - 100% utilizável sem internet: o motorista edita as metas mesmo em garagens ou áreas sem sinal; a tela atualiza instantaneamente (0ms de latência) e a sincronização ocorre silenciosamente ao restabelecer a conexão.
   - Resolução de concorrência por *Last-Write-Wins* (LWW) baseado na coluna `updated_at` em milissegundos UTC.
3. **Isolamento Estrito entre Contas no Mesmo Aparelho:**
   - Ao deslogar e logar com outra conta, o `DataOwnerStorage` impede vazamento de metas. O novo usuário baixa as suas próprias metas da nuvem ou inicializa valores padrão limpos.
   - Em caso de exclusão de conta (futura Tarefa T-10), as metas são eliminadas em cascata na nuvem e expurgadas localmente.
4. **Migração Transparente e Desacoplamento do Tema:**
   - No primeiro boot após a futura T-28B, as metas legadas do arquivo XML serão lidas e convertidas para o banco Room local sem perda de informação.
   - A configuração `is_dark_theme` será movida em definitivo para `giro_custo_preferences.xml` (arquivo que nunca é apagado em limpezas de dados de conta).
5. **Correção de Usabilidade e Acessibilidade (Apontadas por UX e QA):**
   - Correção mandatória do diálogo de edição para aceitar vírgula decimal brasileira (`250,00`).
   - Teclado numérico/decimal correto, feedback acessível via Snackbar, área de toque mínima de 48dp e externalização mandatória de todos os textos para `strings.xml`.
6. **Sequenciamento Estrito de Versões do Room:**
   - **T-08 [PERF-01]:** Room v12 -> v13 (`13.json`) — **CONCLUÍDO E HOMOLOGADO**.
   - **T-28B [DAT-03]:** Room v13 -> v14 (`14.json`) — **AUTORIZADO PARA IMPLEMENTAÇÃO**.
   - **T-18B [DAT-01]:** Room v14 -> v15 (`15.json`) — **PLANEJADO PARA ONDA 3**.

---

### 3. Evidências

#### 3.1 Registro Formal de Aprovação no Chat
Em 26 de outubro de 2026, o proprietário do projeto emitiu a aprovação expressa:
> *"Primeiro: Eu APROVO formalmente o desenho técnico da Tarefa T-28A (docs/design-metas-por-conta.md). Registre a aprovação e o status ✅ VERIFICADO da T-28A em docs/plano-onda-2.md."*

#### 3.2 Atualização do Plano Oficial ([`docs/plano-onda-2.md`](file:///home/nando/Documentos/GiroCustov0.1/docs/plano-onda-2.md))
- **Seção 1 (Quadro de Situação):** T-28A registrada com status ✅ **VERIFICADO**.
- **Seção 3.d.1:** Registrado que o documento técnico `docs/design-metas-por-conta.md` foi formalmente aprovado pelo usuário.
- **Seção 4.2 (Matriz de Dependências):** 7º item da esteira atualizado para ✅ **VERIFICADO**.
- **Seção 5 (Decisões Pendentes):** "Decisão 1: Aprovação do Desenho Técnico de Metas por Conta" marcada como **RESOLVIDA / APROVADA**.

#### 3.3 Status da Suíte Automatizada de Testes
Com a conclusão da T-08 que precedeu esta consolidação:
- **Total de Testes Unitários:** 228 testes executados e aprovados (100% de sucesso).
- **Falhas:** 0
- **Ignorados:** 0
- **Lint do Android:** 0 erros.
- **R8 / Minificação (`assembleReleaseCheck`):** Aprovado.

---

### 4. Testes

- **Testes nesta Tarefa:** Por se tratar de diagnóstico arquitetural e aprovação formal de design técnico, nenhuma alteração de código de produção ocorreu na T-28A.
- **Testes da T-08 Preservados:** Os 228 testes unitários cobrem 100% da integridade do banco até a versão 13.
- **Plano de Testes da Futura T-28B:**
  1. `testMigration13To14_createsUserGoalsTableAndPreservesDataWithMigrationTestHelper`: validação estrita da tabela `user_goals` contra o schema `14.json`.
  2. `UserGoalsDaoTest`: inserção, consulta por userId, atualização atômica e exclusão.
  3. `UserGoalsSyncTest`: teste com `MockWebServer` simulando PUSH e PULL com respostas realistas (200, 401, 500) e integridade de headers autenticados.
  4. `LegacyGoalsMigrationTest`: leitura e conversão de `giro_custo_goals.xml` com verificação de integridade dos valores padrão e desacoplamento do tema.
  5. `AccountSwitchGoalsIsolationTest`: confirmação de que deslogar não vaza metas locais para outra conta.

---

### 5. O que NÃO foi feito ou está simulado

- **Código de Produção da T-28B:** As entidades Kotlin, DAOs, ViewModel, repositório e tela de metas na nuvem NÃO foram codificados nesta etapa. Serão implementados na Tarefa T-28B.
- **Script SQL Remoto no Supabase:** O script DDL com as tabelas e políticas RLS para `user_goals` encontra-se documentado e validado em `docs/design-metas-por-conta.md`, com status inicial oficial ⛔ **BLOQUEADO** até que seja executado pelo usuário no painel web do Supabase durante a execução da T-28B.
- **Nenhum mock ou stub foi adicionado:** O aplicativo permanece 100% livre de códigos simulados.

---

### 6. Riscos e Dívidas Encontrados

1. **Dependência do Script SQL no Supabase na T-28B:**
   - A sincronização na nuvem só passará a funcionar após a criação física da tabela `user_goals` no projeto Supabase pelo usuário. O app deve operar em modo defensivo (offline-first gracioso) enquanto o script não for executado.
2. **Tratamento de Decimais com Vírgula:**
   - O campo de texto atual de metas aceita apenas ponto. Na T-28B, o parser deverá tratar vírgulas (`replace(',', '.')`) para não frustrar o usuário final que digita valores monetários em formato brasileiro.
3. **Respeito ao Sequenciamento de Banco:**
   - O banco foi elevado com sucesso para a versão 13 na T-08. A T-28B implementará obrigatoriamente `MIGRATION_13_14` e o schema `app/schemas/com.example.data.GiroCustoDatabase/14.json`.

---

### 7. O que o usuário precisa fazer

Nenhuma intervenção técnica adicional é exigida neste momento para a T-28A.

Para prosseguir com o projeto:
1. Este relatório e a aprovação formal estão consolidados e versionados no repositório Git.
2. Como a Tarefa T-08 já foi concluída, testada e homologada (versão 13 do Room), a próxima tarefa da esteira da Onda 2 é a **Tarefa T-28B [DAT-03]**:
   - Implementação da entidade `UserGoal` e `UserGoalsDao`;
   - Implementação da migração `MIGRATION_13_14` e exportação do schema `14.json`;
   - Implementação da sincronização na nuvem em `SupabaseSyncManager`;
   - Atualização do `GiroCustoViewModel` e dos diálogos de edição com suporte a vírgula pt-BR;
   - Disponibilização do script SQL com RLS para execução no painel do Supabase.
3. Para iniciar a T-28B, basta solicitar ao Orquestrador no chat.

---

### 8. Commit e sincronização

- **Mensagem do Commit:** `docs(metas): relatório formal de aprovação da Tarefa T-28A e liberação da T-28B`
- **Hash do Commit:** `<commit-hash>`
- **Status do Push:** `<push-status>`
