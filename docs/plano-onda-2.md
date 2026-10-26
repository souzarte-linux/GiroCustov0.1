# Plano de Execução da Onda 2 e Revisão da Onda 3 (GiroCusto)

**Data:** 25 de Outubro de 2026  
**Autor:** Agente Orquestrador (com deliberação técnica de Arquitetura, Segurança, Dados, UX/UI, QA e DevOps)  
**Documentos de Referência:**
- [docs/plano-correcao-v2.md](file:///home/nando/Documentos/GiroCustov0.1/docs/plano-correcao-v2.md) (Plano v2.1)
- [docs/auditoria-2026-10-22.md](file:///home/nando/Documentos/GiroCustov0.1/docs/auditoria-2026-10-22.md)
- [docs/design-sessao-segura.md](file:///home/nando/Documentos/GiroCustov0.1/docs/design-sessao-segura.md)
- [docs/design-isolamento-contas.md](file:///home/nando/Documentos/GiroCustov0.1/docs/design-isolamento-contas.md)
- Relatórios de conclusão e conformidade das Tarefas T-01 a T-07 em [docs/](file:///home/nando/Documentos/GiroCustov0.1/docs/)

---

## 1. Quadro de Situação da Onda 1 (T-01 a T-07, T-06b, T-06c, T-06d)

A Onda 1 teve como meta primordial a blindagem imediata contra riscos críticos de segurança, proteção de credenciais, correção de usabilidade crítica de tela e ativação de minificação R8. Todas as tarefas planejadas foram concluídas no código, validadas por testes unitários automatizados com 100% de sucesso e testadas no aparelho físico pelo usuário, alcançando o status oficial ✅ **VERIFICADO**. Resta apenas o teste de 70 minutos da renovação do token da T-06, a ser confirmado pelo usuário.

| ID Tarefa | Achado Auditado | Risco | Status Oficial | Hash Commit | Mensagem do Commit | Verificação / Observações |
| :--- | :---: | :---: | :---: | :---: | :--- | :--- |
| **T-01** | SEC-08 | Baixo | ✅ **VERIFICADO** | `69c9a71` | `Segurança: adiciona padrões de certificados e credenciais ao .gitignore (T-01)` | Blindagem do `.gitignore` contra chaves, keystores e credenciais; verificado com `git check-ignore`. |
| **T-02** | SEC-02 | Alto | ✅ **VERIFICADO** | `077bd66` | `Segurança: exclui preferências de autenticação e biometria do backup Android (T-02)` | Exclusão cirúrgica de tokens e biometria do backup em nuvem e USB; Room preservado. |
| **T-03** | UI-01 | Crítico | ✅ **VERIFICADO** | `11cf935` | `UI: remove moldura de SmartphoneSimulator da produção em GiroCustoMainApp (T-03)` | Testado e homologado pelo usuário no aparelho físico; rotação de tela em modo paisagem verificada sem barras pretas laterais ou moldura de 390dp. |
| **T-04** | INT-01 + SEC-03 | Alto | ✅ **VERIFICADO** | `fa60196` | `Segurança: oculta login por SMS e higieniza logs HTTP com Level.BASIC (T-04)` | Testado no aparelho real com Logcat (`Level.BASIC`, redação de `Authorization`/`apikey`); stub de SMS com rótulo "Em breve". |
| **T-05** | UX-01 | Alto | ✅ **VERIFICADO** | `b2714f0` | `UX: adiciona diálogo de confirmação e desfazer com proteção LWW ao excluir veículos e peças (T-05)` | Testado e homologado no aparelho físico pelo usuário; diálogo de confirmação, snackbar com Desfazer e proteção LWW. |
| **T-06** | SEC-01 + SEC-04 | Alto | ✅ **VERIFICADO** | `8d2641d`<br>`78f88d2`<br>`abfdda2`<br>`fa80a8e` | `Segurança: implementa sessão segura em repouso e biometria com Keystore AES-GCM (T-06)`<br>`UI/Acessibilidade: corrige layout vertical do botão Consultar Correios em Meus Dados (T-06)`<br>`Fix(Segurança): adiciona switch de biometria no perfil e força reautenticação no fallback (T-06)`<br>`Docs: atualiza hash de commit no relatório da Tarefa T-06` | Sessão segura em repouso com Keystore AES-GCM, CryptoObject real em BiometricPrompt, switch no perfil e contingência segura testados no aparelho. O commit `78f88d2` (layout do botão Consultar Correios) foi uma alteração extra realizada durante o ciclo. Resta apenas o teste de 70 minutos da renovação do token da T-06, a ser confirmado pelo usuário. |
| **T-06b** | SEC-01 (Conformidade) | Alto | ✅ **VERIFICADO** | `86aa696` | `Segurança: remove fallback em memória e implementa conformidade estrita de sessão segura (T-06b)` | Remoção de fallback de chaves em RAM na produção; contrato `SessionCryptoEngine` implementado pelo Keystore e fakes restritos a `src/test`. |
| **T-06c** | SEC-01 (Isolamento) | Alto | ✅ **VERIFICADO** | `e1c0742` | `feat(sync): isolamento estrito de dados entre contas no mesmo dispositivo (T-06c)` | Testado e homologado pelo usuário no aparelho físico; isolamento entre contas, máquina de estados de troca de conta, travas defensivas de sync e persistência no DataOwnerStorage validadas com sucesso no dispositivo. |
| **T-06d** | Integridade Nuvem | Alto | ✅ **VERIFICADO** | `5da367d` | `Segurança: remove dados pessoais e senha dos documentos` | Removidos dados pessoais, senhas e arquivos de exportação CSV do repositório, blindando a documentação e histórico conforme a Regra 8; base Supabase limpa de registros inconsistentes. (Nota: o commit `bc7453b` foi o que enviou exportações ao repositório e NÃO é o commit da T-06d). |
| **T-07** | SEC-05 | Alto | ✅ **VERIFICADO** | `129cd7a` | `build(release): ativa R8 em release, cria buildType releaseCheck e define regras ProGuard (T-07)` | Testado e homologado pelo usuário no aparelho físico; APK `releaseCheck` instalado com sucesso por cima da versão existente com R8 e regras ProGuard ativas, fluxos críticos validados sem travamentos. |
| **T-27** | Limpeza | Baixo | ✅ **VERIFICADO** | `2ea2387` | `Limpeza: remove signInWithPhone e mensagem proibida de homologação (T-27)` | Remoção de código morto e mensagem proibida de homologação no ViewModel. |
| **T-30** | SEC-09 | Alto | ✅ **VERIFICADO** | `bbde9ee` | `Segurança: torna DataOwnerStorage obrigatório e adiciona trava fail-safe (T-30)` | Testado e homologado; trava fail-safe de dono nulo no SupabaseSyncManager com teste de instalação nova. |
| **T-26** | UX-03 | Médio | ✅ **VERIFICADO** | `209e83d`<br>`24132cf` | `UX/Rede: traduz erros da API Supabase Auth para mensagens amigáveis em português (T-26)`<br>`UX/Rede: detecta usuario ja existente em signup HTTP 200 com identities vazias (T-26)` | Testado e homologado no celular físico pelo usuário; tradução amigável em pt-BR de erros Supabase Auth com identificador tipado e detecção de identities vazias em HTTP 200 no signup. |
| **T-14a** | ARQ-01A | Alto | ✅ **VERIFICADO** | `0676ec8` | `Arquitetura/Segurança: desacopla ganchos de teste e reflexão em Auth e Sessão (T-14a)` | Testado e homologado no celular físico pelo usuário; desacoplamento de ganchos de teste em Auth e Sessão (`GoogleAuthHelper`, `BiometricHelper`, `SupabaseClient`), Login com Google via Credential Manager e desbloqueio por biometria 100% verificados no aparelho. |
| **T-14b** | ARQ-01B | Alto | ✅ **VERIFICADO** | `0d62142` | `Arquitetura: desacopla ganchos de teste e provê injeção em clientes de rede e banco (T-14b)` | Testado e homologado no celular físico pelo usuário; remoção de ganchos de teste em clientes de rede e banco (`FipeNetworkClient`, `ViaCepNetworkClient`, `GiroCustoDatabase`), consulta de CEP e busca no catálogo FIPE 100% verificadas no aparelho. |
| **T-29** | SEC-05B | Alto | ✅ **VERIFICADO** | `42dbb17`<br>`cc36d56` | `build(r8): aperto cirurgico das regras ProGuard e validacao em releaseCheck (T-29)` | Testado e homologado no celular físico pelo usuário; keeps genéricos amplos removidos de `proguard-rules.pro`, regras cirúrgicas mínimas implementadas, ofuscação R8 comprovada em `mapping.txt`, APK `releaseCheck` instalado por cima da versão anterior e 9 fluxos essenciais validados com 100% de sucesso sem erros de reflexão no logcat. |
| **T-28A** | ARQ-06A | Médio | ✅ **VERIFICADO** | `1172c41`<br>`c125047` | `docs(metas): diagnóstico, inventário e desenho técnico de metas por conta na nuvem (T-28A)` | Diagnóstico aprofundado no código real, inventário completo de SharedPreferences e desenho técnico completo formalizados em `docs/design-metas-por-conta.md`; aprovado formalmente pelo usuário em 26/10/2026. |
| **T-08** | PERF-01 | Alto | ✅ **VERIFICADO** | `f8e4a73` | `perf(room): índices nas 7 entidades Room e migração v13 com MigrationTestHelper (T-08)` | 20 índices criados nas 7 entidades do Room baseados em consultas reais dos DAOs, migração MIGRATION_12_13, schema v13 exportado (`13.json`) e 100% validado com MigrationTestHelper e mutação manual; 228 testes unitários passando. |



---

## 2. Reavaliação das Tarefas Restantes da Onda 2 e Onda 3 (Plano v2.1)

### 2.1 O que mudou e o impacto nas tarefas futuras

1. **Adição do `DataOwnerStorage` e Decisão Central de Troca de Conta (T-06c):**
   - Foi criada a camada `DataOwnerStorage` persistida em `giro_custo_data_owner.xml` com gravação síncrona imediata (`commit()`).
   - O `SupabaseSyncManager` recebeu travas defensivas atômicas antes do PUSH e do PULL para barrar sincronizações se `ownerId != userId`.
   - O `GiroCustoViewModel` passou a gerenciar estados de conflito de conta (`AccountConflictState`), diálogos de confirmação de descarte de dados e reivindicação de propriedade local.
   - O `GiroCustoRepository` recebeu métodos atômicos de contagem de pendências (`countPendingChanges`), verificação de dados locais (`hasAnyLocalData`) e limpeza de tabelas (`clearAllLocalData`).
   - **Impacto direto:** 
     - A tarefa **T-10 (Exclusão de Conta)** agora precisa interagir compulsoriamente com o `DataOwnerStorage`, a limpeza de tabelas locais e a exclusão de metas (T-28B), além de implementar proteção fail-safe (se o servidor falhar, nada local é apagado), confirmação reforçada e oferta de exportação prévia para não deixar o aparelho em estado inconsistente ou bloqueado.
     - A tarefa **T-08 (Índices Room e Migration v13)** coordena a estrutura do banco Room com a introdução das metas persistidas na nuvem (T-28B, v14 com schema 14.json) e com a futura migração monetária para centavos (T-18B, v15 com schema 15.json).
     - A tarefa **T-28 (Metas por Conta)** passa a ter implementação (T-28B) vinculada à Opção B (sincronização na nuvem), precedida de desenho e inventário de preferências (T-28A).
     - A tarefa **T-14** é desacoplada em **T-14a** (Auth/Biometria/Sessão) e **T-14b** (Clientes de Rede/Room) para menor risco e foco cirúrgico na auditoria.

2. **Ativação do R8 e Criação do buildType `releaseCheck` (T-07):**
   - O compilador R8 agora ofusca e otimiza compilações de release.
   - Todas as alterações estruturais que envolvam reflection, Room, Moshi, Retrofit ou injeção de dependência precisam obrigatoriamente ser validadas não apenas com `./gradlew testDebugUnitTest lintDebug`, mas também com `./gradlew assembleReleaseCheck`.
   - As regras atuais em `proguard-rules.pro` incluíram regras `-keep class com.example.data.** { *; }`, `-keep class com.example.network.** { *; }` e `-keep class com.example.util.** { *; }`, que são excessivamente permissivas e anulam parte dos ganhos do R8 (objeto da nova tarefa T-29).

3. **Crescimento do `GiroCustoViewModel.kt`:**
   - Com as regras de biometria, fallback seguro, mitigação de concorrência e máquina de estados de troca de conta, o ViewModel alcançou **1.572 linhas**. A tarefa de decomposição estrutural (**T-20**) ganhou urgência arquitetural aumentada para a Onda 3.

> [!IMPORTANT]
> **Nota Global de Versionamento e Sequenciamento do Room:** O número de versão de cada migração deve ser estritamente conferido contra o número real da versão atual do banco de dados no momento da execução. A ordem sequencial obrigatória de execução das tarefas que alteram o banco Room é: **T-08** (v12 -> v13, schema `13.json`), depois **T-28B** (v13 -> v14, schema `14.json`), e depois **T-18B** (v14 -> v15, schema `15.json`). Nenhuma tarefa pode pular versões ou duplicar números de schema.

---

### 2.2 Catálogo de Tarefas Restantes da Onda 2 e Onda 3 (Formato JSON)

#### Tarefa T-08 [PERF-01]: Índices no Room Baseados em Consultas Reais e Migração v13
```json
{
  "task_id": "T-08",
  "to_agent": "backend",
  "task_type": "implementacao",
  "risk": "alto",
  "title": "Definir índices nas entidades Room baseados em consultas reais e migração v13 com runMigrationsAndValidate",
  "context": "Nenhuma das 7 entidades Room possui índices definidos, gerando varreduras completas de tabela em filtros frequentes. A limpeza e sincronização introduzidas na T-06c também realizam contagens que se beneficiam de índices específicos.",
  "root_cause_evidence": "Entities.kt:13, 36, 59, 161, 183, 206 e Daos.kt:8, 18, 27, 63, 75, 78, 101, 111, 114, 168, 171, 189, 192, 203, 206, 218, 224, 227",
  "acceptance_criteria": [
    "Cada índice adicionado cita a consulta exata (DAO e linha de código):",
    "- daily_records: índice em (isDeleted, dateTimestamp) [Daos.kt:101], índice em remoteId [Daos.kt:111], índice em syncStatus [Daos.kt:114]",
    "- vehicle_parts: índice em vehicleId [Daos.kt:63, 69], índice em remoteId [Daos.kt:75], índice em syncStatus [Daos.kt:78]",
    "- fuel_refills: índice em (vehicleId, dateTimestamp) [Daos.kt:189], índice em (vehicleId, odometer) [Daos.kt:192, 195], índice em remoteId [Daos.kt:203], índice em syncStatus [Daos.kt:206]",
    "- maintenance_records: índice em (vehicleId, dateTimestamp) [Daos.kt:218], índice em remoteId [Daos.kt:224], índice em syncStatus [Daos.kt:227]",
    "- vehicle: índice em (active, isDeleted) [Daos.kt:8, 11], índice em remoteId [Daos.kt:18], índice em syncStatus [Daos.kt:27]",
    "- platforms: índice em remoteId [Daos.kt:168], índice em syncStatus [Daos.kt:171]",
    "Migração Room MIGRATION_12_13 criada com CREATE INDEX IF NOT EXISTS e exportação do schema 13.json gerada e versionada",
    "Testar compulsoriamente a migração utilizando runMigrationsAndValidate do MigrationTestHelper para garantir conformidade de 100% dos nomes e ordenação dos índices",
    "Nenhum teste existente alterado ou enfraquecido"
  ],
  "files_expected": [
    "app/src/main/java/com/example/data/Entities.kt",
    "app/src/main/java/com/example/data/GiroCustoDatabase.kt",
    "app/schemas/com.example.data.GiroCustoDatabase/13.json",
    "app/src/test/java/com/example/SupabaseSyncAndMigrationTest.kt"
  ],
  "out_of_scope": ["Alterações de schema remoto no Supabase (já indexado)"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug",
    "./gradlew assembleReleaseCheck"
  ],
  "required_reviewers": ["backend", "performance", "revisor"],
  "depends_on": []
}
```

#### Tarefa T-09 [PERF-02]: Chaves Estáveis em Listas LazyColumn
```json
{
  "task_id": "T-09",
  "to_agent": "frontend",
  "task_type": "implementacao",
  "risk": "alto",
  "title": "Adicionar key estável em todas as chamadas items() de listas Compose",
  "context": "Listas de manutenções, plataformas, histórico e abastecimentos usam items() sem o parâmetro key, provocando recomposições custosas de toda a árvore a cada mudança de estado.",
  "root_cause_evidence": "MaintenanceScreen.kt:255, PlatformsScreen.kt:196, HistoryScreen.kt:393, 440 e FuelRefillsScreen.kt:300",
  "acceptance_criteria": [
    "Parâmetro key = { it.id } (ou identificador único estável equivalente para agrupamentos) adicionado a todos os blocos items()",
    "Nenhuma lista rolável LazyColumn opera sem key explícita",
    "Comportamento visual e ordenação mantidos rigorosamente idênticos",
    "Entregar roteiro de teste manual no aparelho, passo a passo completo, para validação de rolagem suave e sem cintilações (status máximo 🟡 sem teste no aparelho)"
  ],
  "files_expected": [
    "app/src/main/java/com/example/ui/screens/MaintenanceScreen.kt",
    "app/src/main/java/com/example/ui/screens/PlatformsScreen.kt",
    "app/src/main/java/com/example/ui/screens/HistoryScreen.kt",
    "app/src/main/java/com/example/ui/screens/FuelRefillsScreen.kt"
  ],
  "out_of_scope": ["Redesenho visual dos cartões das listas"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug",
    "./gradlew assembleReleaseCheck"
  ],
  "required_reviewers": ["performance", "revisor"],
  "depends_on": []
}
```

#### Tarefa T-10 [SEC-06]: Portabilidade de Dados e Exclusão Segura de Conta (LGPD) com Limpeza de Dono e Metas
```json
{
  "task_id": "T-10",
  "to_agent": "seguranca",
  "task_type": "implementacao",
  "risk": "alto",
  "title": "Implementar exportação de dados via SAF e exclusão definitiva de conta no Supabase com expurgo do DataOwnerStorage, metas e Room local",
  "context": "O app não atende aos requisitos de exclusão e portabilidade da LGPD. Além de deletar o usuário na nuvem por RPC SECURITY DEFINER sem usar chave service_role no cliente, o processo deve compulsoriamente expurgar os dados locais (incluindo metas sincronizadas), limpar o DataOwnerStorage e revogar sessões locais. Se a chamada remota falhar, nada local pode ser apagado. A interface deve exigir confirmação reforçada e oferecer a exportação dos dados previamente.",
  "root_cause_evidence": "Ausência de rotinas de exportação e exclusão de conta em GiroCustoViewModel.kt e ProfileScreen.kt",
  "acceptance_criteria": [
    "(a) Criar migração SQL remota em supabase/migrations com a RPC delete_user_account() configurada como SECURITY DEFINER, com search_path = public fixo, permitida exclusivamente ao papel authenticated (REVOKE ALL FROM anon, public; GRANT EXECUTE TO authenticated;)",
    "(b) A RPC remota deve garantir a exclusão em cascata de todas as tabelas do usuário (incluindo as metas na nuvem da T-28B) antes de expurgar a conta em auth.users",
    "(c) Proteção Fail-Safe Local: Se a chamada ao servidor falhar (erro HTTP, timeout ou sem internet), NADA local é apagado (banco Room, preferências, metas, sessão e DataOwnerStorage permanecem rigorosamente intactos); incluir teste automatizado específico cobrindo a falha do servidor e a preservação total dos dados locais",
    "(d) Confirmação reforçada antes da exclusão (ação irreversível): diálogo de confirmação com aviso explícito de destruição definitiva e digitação obrigatória de palavra de segurança (ex.: 'EXCLUIR') ou confirmação em duas etapas para impedir cliques acidentais",
    "(e) Oferecer proativamente a exportação de dados locais via SAF (CreateDocument) ou ShareSheet nativo em formato JSON/CSV antes de permitir a exclusão definitiva, sem exigir permissão intrusiva de armazenamento",
    "(f) Ao confirmar a exclusão com sucesso na RPC remota: (1) acionar a RPC remota; (2) executar repository.clearAllLocalData() (incluindo dados de metas da T-28B); (3) executar dataOwnerStorage.clearDataOwner(); (4) executar supabaseClient.signOut() e clearSavedSession(); (5) executar BiometricHelper.clearBiometrics(); (6) resetar preferências locais e transitar para RootNavState.Unauthenticated",
    "(g) Criar teste automatizado validando o expurgo completo dos dados locais e a limpeza do DataOwnerStorage após sucesso na exclusão",
    "(h) Script SQL entregue com documentação detalhada em supabase/README.md. Status da etapa de servidor até execução pelo usuário no painel: ⛔ BLOQUEADO",
    "(i) Entregar roteiro de teste manual no aparelho, passo a passo completo, orientando expressamente a realização do teste APENAS com conta de teste descartável (nunca com a conta real do usuário) (status máximo 🟡 sem teste no aparelho)"
  ],
  "files_expected": [
    "app/src/main/java/com/example/ui/screens/ProfileScreen.kt",
    "app/src/main/java/com/example/ui/GiroCustoViewModel.kt",
    "app/src/main/java/com/example/network/supabase/SupabaseClient.kt",
    "app/src/main/java/com/example/data/DataOwnerStorage.kt",
    "app/src/main/res/values/strings.xml",
    "supabase/migrations/20261026_user_deletion_rpc.sql",
    "supabase/README.md",
    "app/src/test/java/com/example/UserAccountDeletionTest.kt"
  ],
  "out_of_scope": ["Exclusão de backups manuais do usuário em seu Google Drive pessoal"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug",
    "./gradlew assembleReleaseCheck"
  ],
  "required_reviewers": ["seguranca", "backend", "revisor"],
  "depends_on": ["T-06c", "T-30", "T-28B"]
}
```

#### Tarefa T-11 [SEC-07]: Rationale Prévia para Acesso à Localização (GPS)
```json
{
  "task_id": "T-11",
  "to_agent": "frontend",
  "task_type": "implementacao",
  "risk": "medio",
  "title": "Adicionar diálogo explicativo prévio antes de solicitar permissão de GPS",
  "context": "Ao clicar no botão de busca de postos na FuelRefillsScreen, o app dispara diretamente o launcher de permissões do sistema operacional sem explicar a finalidade ao motorista, induzindo à recusa.",
  "root_cause_evidence": "FuelRefillsScreen.kt:859-869",
  "acceptance_criteria": [
    "Diálogo explicativo prévio em português simples informando que a localização é utilizada exclusivamente para encontrar postos próximos e calcular o menor custo de combustível, com textos via stringResource",
    "O pedido de permissão do sistema operacional só é acionado após clique positivo no botão de confirmação do diálogo",
    "Opção de dispensar o diálogo mantendo a busca manual por texto ativa",
    "Entregar roteiro de teste manual no aparelho, passo a passo completo, para validação visual (status máximo 🟡 sem teste no aparelho)"
  ],
  "files_expected": [
    "app/src/main/java/com/example/ui/screens/FuelRefillsScreen.kt",
    "app/src/main/res/values/strings.xml"
  ],
  "out_of_scope": ["Modificações na lógica de cálculo de geolocalização em LocationHelper.kt"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug",
    "./gradlew assembleReleaseCheck"
  ],
  "required_reviewers": ["designer", "revisor"],
  "depends_on": []
}
```

#### Tarefa T-12 [UX-02]: Estados de Carregando, Vazio, Erro e Offline nas Telas Principais
```json
{
  "task_id": "T-12",
  "to_agent": "designer",
  "task_type": "implementacao",
  "risk": "medio",
  "title": "Padronização de estados de carregamento, lista vazia com ação, erro recuperável e aviso offline",
  "context": "DashboardScreen, ReportsScreen e HistoryScreen deixam o motorista sem orientação visual quando vazias, em processamento ou em falha.",
  "root_cause_evidence": "DashboardScreen.kt:510 e ReportsScreen.kt:99-109",
  "acceptance_criteria": [
    "Componente visual de estado Vazio (Empty State) informativo com botão de ação rápida direcionando para lançamento de corrida ou abastecimento, utilizando stringResource",
    "Tratamento explícito de estado de Erro com botão 'Tentar novamente'",
    "Banner informativo discreto quando o aplicativo opera offline (preservando o uso local irrestrito do app)",
    "Entregar roteiro de teste manual no aparelho, passo a passo completo, para o usuário executar (sem confirmação no aparelho, status máximo 🟡)"
  ],
  "files_expected": [
    "app/src/main/java/com/example/ui/screens/DashboardScreen.kt",
    "app/src/main/java/com/example/ui/screens/ReportsScreen.kt",
    "app/src/main/java/com/example/ui/screens/HistoryScreen.kt",
    "app/src/main/res/values/strings.xml"
  ],
  "out_of_scope": ["Alteração das fórmulas matemáticas de relatórios"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug",
    "./gradlew assembleReleaseCheck"
  ],
  "required_reviewers": ["designer", "revisor"],
  "depends_on": ["T-03"]
}
```

#### Tarefa T-13 [INT-02]: Sincronização de Timeouts da Overpass API
```json
{
  "task_id": "T-13",
  "to_agent": "integracoes",
  "task_type": "implementacao",
  "risk": "medio",
  "title": "Alinhar timeout do cliente OkHttp com a query do Overpass (25 segundos)",
  "context": "O timeout configurado no OkHttpClient de OverpassApiService é de 8 segundos, enquanto a consulta enviada ao servidor Overpass solicita [timeout:25], causando cancelamento prematuro no 4G antes da resposta do servidor.",
  "root_cause_evidence": "OverpassApiService.kt:22-23 e GasStationRepository.kt:13",
  "acceptance_criteria": [
    "connectTimeout e readTimeout no OkHttpClient de OverpassApiService ajustados para 25 segundos",
    "Tratamento explícito de SocketTimeoutException com tentativa no próximo endpoint da lista de espelhos",
    "Testes de integração com MockWebServer mantidos 100% verdes"
  ],
  "files_expected": [
    "app/src/main/java/com/example/network/OverpassApiService.kt"
  ],
  "out_of_scope": ["Alteração da lista de servidores públicos do Overpass"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug",
    "./gradlew assembleReleaseCheck"
  ],
  "required_reviewers": ["integracoes", "revisor"],
  "depends_on": []
}
```

#### Tarefa T-14a [ARQ-01A]: Remoção de Ganchos Públicos e Reflexão de Autenticação e Sessão em Produção
```json
{
  "task_id": "T-14a",
  "to_agent": "arquiteto",
  "task_type": "implementacao",
  "risk": "alto",
  "title": "Remover ganchos públicos 'test...', resetForTesting e reflexão de classes de teste em GoogleAuthHelper, BiometricHelper e SupabaseClient",
  "context": "GoogleAuthHelper (testAuthHandler), BiometricHelper (testHardwareAvailable) e SupabaseClient (resetForTesting, testStorage e reflexão de InMemorySessionStorage) contêm ganchos de teste em código de produção que violam a separação entre produção e ambiente de testes.",
  "root_cause_evidence": "GoogleAuthHelper.kt:37, BiometricHelper.kt:27 e SupabaseClient.kt:253, 289-298",
  "acceptance_criteria": [
    "Remover GoogleAuthHelper.testAuthHandler de produção, provendo ponto de injeção limpo via construtor ou visibilidade restrita a testes",
    "Remover BiometricHelper.testHardwareAvailable de produção, utilizando injeção de dependência limpa",
    "Remover SupabaseClient.resetForTesting, @Volatile var testStorage e o carregamento por reflexão dinâmica de InMemorySessionStorage via Class.forName",
    "Substituir acoplamentos de teste por injeção de dependência via construtores ou factories limpas",
    "As asserções de TODOS os testes unitários existentes permanecem RIGOROSAMENTE IDÊNTICAS linha por linha (somente a inicialização dos objetos nos testes é adaptada)",
    "O Revisor Independente deve conferir o diff de teste linha por linha comprovando que nenhuma asserção foi enfraquecida ou removida",
    "Entregar roteiro manual no aparelho: testar login Google e biometria após a alteração (status máximo 🟡 sem teste no aparelho)"
  ],
  "files_expected": [
    "app/src/main/java/com/example/util/GoogleAuthHelper.kt",
    "app/src/main/java/com/example/util/BiometricHelper.kt",
    "app/src/main/java/com/example/network/supabase/SupabaseClient.kt",
    "app/src/test/java/com/example/AuthBiometricsAndViaCepTest.kt",
    "app/src/test/java/com/example/SecureSessionAndCryptoTest.kt"
  ],
  "out_of_scope": ["Ganchos de teste de FIPE, ViaCep e Room (tratados na T-14b)"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug",
    "./gradlew assembleReleaseCheck"
  ],
  "required_reviewers": ["arquiteto", "seguranca", "revisor"],
  "depends_on": ["T-06b", "T-06c"]
}
```

#### Tarefa T-14b [ARQ-01B]: Remoção de Ganchos Públicos de Teste em Clientes de Rede e Banco
```json
{
  "task_id": "T-14b",
  "to_agent": "arquiteto",
  "task_type": "implementacao",
  "risk": "alto",
  "title": "Remover ganchos públicos de teste em FipeNetworkClient, ViaCepNetworkClient e GiroCustoDatabase",
  "context": "FipeNetworkClient (setClientForTesting/testEngine), ViaCepNetworkClient (testEngine/mock) e GiroCustoDatabase (resetForTesting) contêm variáveis e métodos públicos para testes que poluem o código de produção.",
  "root_cause_evidence": "FipeNetworkClient.kt:41, ViaCepNetworkClient.kt:37 e GiroCustoDatabase.kt:203",
  "acceptance_criteria": [
    "Remover todas as variáveis públicas de mock/teste em FipeNetworkClient e ViaCepNetworkClient, provendo injeção via construtor nos testes",
    "Remover GiroCustoDatabase.resetForTesting de produção",
    "As asserções de TODOS os testes unitários existentes permanecem RIGOROSAMENTE IDÊNTICAS linha por linha",
    "O Revisor Independente deve conferir o diff de teste linha por linha comprovando que nenhuma asserção foi enfraquecida ou removida",
    "Entregar roteiro manual no aparelho: consulta de CEP em Meus Dados e busca na Tabela FIPE (status máximo 🟡 sem teste no aparelho)"
  ],
  "files_expected": [
    "app/src/main/java/com/example/network/fipe/FipeNetworkClient.kt",
    "app/src/main/java/com/example/network/viacep/ViaCepNetworkClient.kt",
    "app/src/main/java/com/example/data/GiroCustoDatabase.kt",
    "app/src/test/java/com/example/FipeNetworkAndViewModelTest.kt",
    "app/src/test/java/com/example/AuthBiometricsAndViaCepTest.kt"
  ],
  "out_of_scope": ["Alterações nas regras de negócio das APIs ou do Room"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug",
    "./gradlew assembleReleaseCheck"
  ],
  "required_reviewers": ["arquiteto", "revisor"],
  "depends_on": ["T-14a"]
}
```

#### Tarefa T-15 [ACC-01]: contentDescription em Elementos Interativos
```json
{
  "task_id": "T-15",
  "to_agent": "acessibilidade",
  "task_type": "implementacao",
  "risk": "alto",
  "title": "Auditar e corrigir contentDescription = null em botões e ícones clicáveis",
  "context": "Foram identificadas dezenas de ocorrências de contentDescription = null em elementos interativos clicáveis, impedindo a usabilidade por motoristas com deficiência visual via TalkBack.",
  "root_cause_evidence": "FuelRefillsScreen.kt:765, ProfileScreen.kt, HistoryScreen.kt e DashboardScreen.kt",
  "acceptance_criteria": [
    "Todo IconButton, botão com ícone ou elemento com Modifier.clickable possui contentDescription claro em português utilizando stringResource",
    "Ícones estritamente decorativos adjacentes a textos explicativos podem manter contentDescription = null desde que o container forneça semântica completa",
    "TalkBack navega e anuncia corretamente o propósito de cada elemento interativo",
    "Área de toque mínima de 48dp respeitada em todos os elementos corrigidos",
    "Entregar roteiro de teste manual no aparelho, passo a passo completo, com passos para navegação via TalkBack (status máximo 🟡 sem teste no aparelho)"
  ],
  "files_expected": [
    "app/src/main/java/com/example/ui/screens/FuelRefillsScreen.kt",
    "app/src/main/java/com/example/ui/screens/ProfileScreen.kt",
    "app/src/main/java/com/example/ui/screens/HistoryScreen.kt",
    "app/src/main/java/com/example/ui/screens/DashboardScreen.kt",
    "app/src/main/res/values/strings.xml"
  ],
  "out_of_scope": ["Redesenho de telas ou fluxos de navegação"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug",
    "./gradlew assembleReleaseCheck"
  ],
  "required_reviewers": ["acessibilidade", "revisor"],
  "depends_on": []
}
```

#### Tarefa T-16 [ARQ-03]: Elevação de Regras de Negócio de Composables para ViewModel
```json
{
  "task_id": "T-16",
  "to_agent": "frontend",
  "task_type": "implementacao",
  "risk": "medio",
  "title": "Mover cálculos matemáticos, formatações e regras de negócio de Composables para o ViewModel",
  "context": "LaunchScreen, FuelRefillsScreen e ReportsScreen executam recálculo de médias, preços dinâmicos e faturamento no corpo de funções @Composable a cada recomposição, desperdiçando CPU e bateria.",
  "root_cause_evidence": "LaunchScreen.kt:381-382, FuelRefillsScreen.kt:657-720 e ReportsScreen.kt:99-109",
  "acceptance_criteria": [
    "Cálculos de médias de consumo, totais acumulados e projeções financeiras movidos para StateFlow ou métodos do ViewModel",
    "Composables passam a atuar estritamente como renderizadores de estado",
    "Criar testes unitários para cada rotina de cálculo migrada para o ViewModel",
    "Textos novos utilizam stringResource",
    "Entregar roteiro de teste manual no aparelho, passo a passo completo, para validação visual (status máximo 🟡 sem teste no aparelho)"
  ],
  "files_expected": [
    "app/src/main/java/com/example/ui/screens/LaunchScreen.kt",
    "app/src/main/java/com/example/ui/screens/FuelRefillsScreen.kt",
    "app/src/main/java/com/example/ui/screens/ReportsScreen.kt",
    "app/src/main/java/com/example/ui/GiroCustoViewModel.kt",
    "app/src/test/java/com/example/ViewModelCalculationsTest.kt"
  ],
  "out_of_scope": ["Alteração visual de layouts ou paleta de cores"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug",
    "./gradlew assembleReleaseCheck"
  ],
  "required_reviewers": ["arquiteto", "revisor"],
  "depends_on": []
}
```

#### Tarefa T-17 [TST-01]: Substituição de Testes de Template por Testes de Regra Real (com Parada Obrigatória)
```json
{
  "task_id": "T-17",
  "to_agent": "qa",
  "task_type": "teste",
  "risk": "medio",
  "title": "Substituir testes de template (2+2) por testes reais de regras de negócio, com autorização explícita do usuário",
  "context": "ExampleUnitTest (2+2), ExampleRobolectricTest, GreetingScreenshotTest e ExampleInstrumentedTest são templates gerados pelo assistente sem validação do negócio.",
  "root_cause_evidence": "ExampleUnitTest.kt:14, ExampleRobolectricTest.kt:19 e GreetingScreenshotTest.kt:24",
  "acceptance_criteria": [
    "PARADA OBRIGATÓRIA: Antes de tocar em qualquer arquivo de teste, o agente deve parar e solicitar autorização explícita por escrito do usuário no chat, listando os testes afetados. Não prosseguir sem resposta afirmativa",
    "GreetingScreenshotTest adaptado para Roborazzi cobrindo componente real (ex.: DashboardSummaryCard)",
    "ExampleUnitTest e ExampleRobolectricTest substituídos por testes reais de validação de campos (hodômetro, litros, preços)",
    "Contagem total de testes mantida ou expandida, com asserções reais sobre regras do app"
  ],
  "files_expected": [
    "app/src/test/java/com/example/ExampleUnitTest.kt",
    "app/src/test/java/com/example/ExampleRobolectricTest.kt",
    "app/src/test/java/com/example/GreetingScreenshotTest.kt"
  ],
  "out_of_scope": ["Remoção de qualquer teste funcional de autenticação, Room ou sync"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug",
    "./gradlew assembleReleaseCheck"
  ],
  "required_reviewers": ["qa", "revisor"],
  "depends_on": []
}
```

---

### 2.3 Tarefas da Onda 3 (Revisadas e Prontas para Planejamento Futuro)

#### Tarefa T-18A [DAT-01A]: Diagnóstico e Planejamento da Migração Monetária para Centavos
```json
{
  "task_id": "T-18A",
  "to_agent": "backend",
  "task_type": "diagnostico",
  "risk": "medio",
  "title": "Diagnosticar e mapear impacto da migração de Double para centavos (Long), sem alterar código",
  "context": "Valores monetários utilizam Double em diversas entidades, gerando riscos de arredondamento e frações residuais. É mandatório mapear todos os pontos antes de alterar código.",
  "root_cause_evidence": "Entities.kt:19, 41, 65, 69, 71, 74, 191, 194, 214",
  "acceptance_criteria": [
    "Mapeamento exaustivo de todos os campos monetários no Room, DTOs Supabase, ViewModels e telas",
    "Análise de impacto em relatórios e sincronização com Supabase (LWW)",
    "Entregar relatório técnico em docs/plano-migracao-monetaria.md com proposta de transição para centavos (Long)",
    "NENHUMA linha de código ou banco alterada nesta etapa",
    "PARADA OBRIGATÓRIA: Aguardar aprovação formal do usuário antes de iniciar a T-18B"
  ],
  "files_expected": [
    "docs/plano-migracao-monetaria.md"
  ],
  "out_of_scope": ["Alterações de código, build ou banco de dados"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
  ],
  "required_reviewers": ["arquiteto", "backend", "revisor"],
  "depends_on": ["T-08"]
}
```

#### Tarefa T-18B [DAT-01B]: Implementação da Migração Monetária para Centavos
```json
{
  "task_id": "T-18B",
  "to_agent": "backend",
  "task_type": "implementacao",
  "risk": "alto",
  "title": "Implementar migração dos campos monetários para centavos (Long) com base no plano aprovado (Room v15)",
  "context": "Execução da migração planejada em T-18A para eliminar erros de ponto flutuante em valores monetários.",
  "root_cause_evidence": "Entities.kt:19, 41, 65, 69, 71, 74, 191, 194, 214",
  "acceptance_criteria": [
    "Modelo de centavos (Long) implementado nas entidades Room com utilitários de conversão segura (CurrencyUtils.kt)",
    "Migração Room MIGRATION_14_15 criada e testada com runMigrationsAndValidate do MigrationTestHelper",
    "Mappers do Supabase compatibilizados sem quebra de registros remotos",
    "Testes unitários cobrindo operações matemáticas financeiras sem perda de centavos",
    "Entregar roteiro manual no aparelho para validação de relatórios e lançamentos (status máximo 🟡 sem teste no aparelho)"
  ],
  "files_expected": [
    "app/src/main/java/com/example/data/Entities.kt",
    "app/src/main/java/com/example/data/GiroCustoDatabase.kt",
    "app/src/main/java/com/example/network/supabase/SupabaseMappers.kt",
    "app/src/main/java/com/example/util/CurrencyUtils.kt",
    "app/src/main/java/com/example/ui/GiroCustoViewModel.kt",
    "app/schemas/com.example.data.GiroCustoDatabase/15.json"
  ],
  "out_of_scope": ["Conversão de odômetro e consumo médio (permanecem Float/Double)"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug",
    "./gradlew assembleReleaseCheck"
  ],
  "required_reviewers": ["arquiteto", "backend", "revisor"],
  "depends_on": ["T-18A", "T-08", "T-28B"]
}
```

#### Tarefa T-19 [DAT-02]: Versionamento de Schemas e Bloqueio de Migração Destrutiva
```json
{
  "task_id": "T-19",
  "to_agent": "arquiteto",
  "task_type": "diagnostico",
  "risk": "baixo",
  "title": "Formalizar integridade de schemas Room (versões 13, 14 e 15) e documentar histórico de banco",
  "context": "Garantir que todo schema futuro seja versionado compulsoriamente (incluindo as versões 13 da T-08, 14 da T-28B e 15 da T-18B) e proibir em definitivo fallbackToDestructiveMigration.",
  "root_cause_evidence": "app/schemas/com.example.data.GiroCustoDatabase/ (ausência de 1, 4, 5, 6, 7 no Git)",
  "acceptance_criteria": [
    "Documentação em docs/database-schemas.md explicando a evolução histórica completa das versões do banco (1->12 legadas, 12->13 na T-08, 13->14 na T-28B e 14->15 na T-18B)",
    "Regra automatizada impedindo build caso schemaVersion != schemas JSON exportados",
    "Garantia absoluta de manutenção de exportSchema = true"
  ],
  "files_expected": [
    "docs/database-schemas.md",
    "app/src/main/java/com/example/data/GiroCustoDatabase.kt"
  ],
  "out_of_scope": ["Criação de schemas sintéticos retroativos"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
  ],
  "required_reviewers": ["arquiteto", "revisor"],
  "depends_on": ["T-08", "T-28B", "T-18B"]
}
```

#### Tarefa T-20 [ARQ-02]: Decomposição Modular de Arquivos com Mais de 800 Linhas
```json
{
  "task_id": "T-20",
  "to_agent": "arquiteto",
  "task_type": "implementacao",
  "risk": "alto",
  "title": "Decompor ProfileScreen, FuelRefillsScreen, HistoryScreen e GiroCustoViewModel",
  "context": "ProfileScreen (2.480 linhas), FuelRefillsScreen (1.611 linhas), HistoryScreen (1.370 linhas) e GiroCustoViewModel (1.572 linhas) ultrapassam massivamente o teto de 800 linhas, acumulando responsabilidades concorrentes.",
  "root_cause_evidence": "ProfileScreen.kt, FuelRefillsScreen.kt, HistoryScreen.kt e GiroCustoViewModel.kt",
  "acceptance_criteria": [
    "Quebra modular de ProfileScreen em componentes isolados (Veículos, Peças, Endereço, Fipe)",
    "Quebra de FuelRefillsScreen em lista, formulário e mapa",
    "Decomposição de responsabilidades do ViewModel em delegates ou casos de uso focados",
    "Nenhum arquivo novo resultante acima de 800 linhas",
    "100% dos testes unitários mantidos verdes com comportamento idêntico",
    "Entregar roteiro manual no aparelho para validação visual (status máximo 🟡 sem teste no aparelho)"
  ],
  "files_expected": [
    "app/src/main/java/com/example/ui/screens/ProfileScreen.kt",
    "app/src/main/java/com/example/ui/screens/FuelRefillsScreen.kt",
    "app/src/main/java/com/example/ui/screens/HistoryScreen.kt",
    "app/src/main/java/com/example/ui/GiroCustoViewModel.kt"
  ],
  "out_of_scope": ["Novas funcionalidades visuais"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug",
    "./gradlew assembleReleaseCheck"
  ],
  "required_reviewers": ["arquiteto", "revisor"],
  "depends_on": ["T-05", "T-09", "T-16"]
}
```

#### Tarefa T-21 [ACC-02]: Migração Gradual de Textos para strings.xml (Dashboard e Launch)
```json
{
  "task_id": "T-21",
  "to_agent": "acessibilidade",
  "task_type": "implementacao",
  "risk": "baixo",
  "title": "Migrar textos estáticos de DashboardScreen e LaunchScreen para strings.xml",
  "context": "Mais de 600 chamadas Text() possuem strings literais no código Kotlin sem uso de stringResource.",
  "root_cause_evidence": "app/src/main/res/values/strings.xml e telas principais",
  "acceptance_criteria": [
    "Textos de títulos, botões, métricas e cartões de DashboardScreen e LaunchScreen centralizados em strings.xml",
    "Formatação monetária e de data respeitando Locale pt-BR",
    "Entregar roteiro manual no aparelho para validação visual (status máximo 🟡 sem teste no aparelho)"
  ],
  "files_expected": [
    "app/src/main/res/values/strings.xml",
    "app/src/main/java/com/example/ui/screens/DashboardScreen.kt",
    "app/src/main/java/com/example/ui/screens/LaunchScreen.kt"
  ],
  "out_of_scope": ["Telas secundárias"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug",
    "./gradlew assembleReleaseCheck"
  ],
  "required_reviewers": ["acessibilidade", "revisor"],
  "depends_on": []
}
```

#### Tarefa T-22 [ARQ-04]: Remoção de Dependências Mortas (Firebase AI / Gemini)
```json
{
  "task_id": "T-22",
  "to_agent": "devops",
  "task_type": "implementacao",
  "risk": "baixo",
  "title": "Remover libs não utilizadas de IA do build.gradle.kts e catálogo de versões",
  "context": "firebase.ai e GEMINI_API_KEY constam nos arquivos de build e ambiente sem nenhuma utilização no código.",
  "root_cause_evidence": "app/build.gradle.kts:114-115 e .env.example:1-2",
  "acceptance_criteria": [
    "Remoção de implementation(libs.firebase.ai) e appcheck do build.gradle.kts",
    "Limpeza das entradas em libs.versions.toml e .env.example",
    "Compilação limpa e redução de tamanho do binário"
  ],
  "files_expected": [
    "app/build.gradle.kts",
    "gradle/libs.versions.toml",
    ".env.example"
  ],
  "out_of_scope": ["Outras dependências ativas"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug",
    "./gradlew assembleReleaseCheck"
  ],
  "required_reviewers": ["devops", "revisor"],
  "depends_on": []
}
```

#### Tarefa T-23 [DEV-01]: Limpeza de Recursos Órfãos do Template
```json
{
  "task_id": "T-23",
  "to_agent": "devops",
  "task_type": "implementacao",
  "risk": "baixo",
  "title": "Remover cores e recursos órfãos de template apontados pelo Lint",
  "context": "Android Lint aponta recursos de cor sem uso em colors.xml herdados do template original.",
  "root_cause_evidence": "app/src/main/res/values/colors.xml:3-9",
  "acceptance_criteria": [
    "Remoção das cores não utilizadas do colors.xml",
    "Avisos de UnusedResources eliminados no Lint"
  ],
  "files_expected": [
    "app/src/main/res/values/colors.xml"
  ],
  "out_of_scope": ["Cores do tema Compose em Color.kt"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
  ],
  "required_reviewers": ["devops", "revisor"],
  "depends_on": []
}
```

#### Tarefa T-24 [ARQ-02B]: Modularização de Telas Secundárias (> 800 linhas)
```json
{
  "task_id": "T-24",
  "to_agent": "arquiteto",
  "task_type": "implementacao",
  "risk": "alto",
  "title": "Modularizar LaunchScreen, CloudSyncScreen, PlatformsScreen e ReportsScreen",
  "context": "Telas secundárias remanescentes ultrapassam o limite de 800 linhas com lógica mista.",
  "root_cause_evidence": "LaunchScreen.kt, CloudSyncScreen.kt, PlatformsScreen.kt e ReportsScreen.kt",
  "acceptance_criteria": [
    "Extração de cartões e diálogos em componentes reutilizáveis",
    "Nenhum arquivo resultante acima de 800 linhas",
    "Textos novos utilizam stringResource",
    "Entregar roteiro manual no aparelho para validação visual (status máximo 🟡 sem teste no aparelho)"
  ],
  "files_expected": [
    "app/src/main/java/com/example/ui/screens/LaunchScreen.kt",
    "app/src/main/java/com/example/ui/screens/CloudSyncScreen.kt",
    "app/src/main/java/com/example/ui/screens/PlatformsScreen.kt",
    "app/src/main/java/com/example/ui/screens/ReportsScreen.kt"
  ],
  "out_of_scope": ["Alterações em ViewModels ou Repositórios"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug",
    "./gradlew assembleReleaseCheck"
  ],
  "required_reviewers": ["arquiteto", "revisor"],
  "depends_on": ["T-20"]
}
```

#### Tarefa T-25 [ACC-02B]: Conclusão da Migração de Textos Secundários para strings.xml
```json
{
  "task_id": "T-25",
  "to_agent": "acessibilidade",
  "task_type": "implementacao",
  "risk": "baixo",
  "title": "Concluir migração de textos das telas secundárias para strings.xml",
  "context": "Finalizar a centralização de textos em HistoryScreen, FuelRefillsScreen, PlatformsScreen e ProfileScreen.",
  "root_cause_evidence": "Telas em app/src/main/java/com/example/ui/screens",
  "acceptance_criteria": [
    "Strings estáticas restantes centralizadas em strings.xml",
    "Avisos de HardcodedText zerados nas telas afetadas",
    "Entregar roteiro manual no aparelho para validação visual (status máximo 🟡 sem teste no aparelho)"
  ],
  "files_expected": [
    "app/src/main/res/values/strings.xml",
    "app/src/main/java/com/example/ui/screens/HistoryScreen.kt",
    "app/src/main/java/com/example/ui/screens/FuelRefillsScreen.kt",
    "app/src/main/java/com/example/ui/screens/PlatformsScreen.kt",
    "app/src/main/java/com/example/ui/screens/ProfileScreen.kt"
  ],
  "out_of_scope": ["Alterações de layout"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug",
    "./gradlew assembleReleaseCheck"
  ],
  "required_reviewers": ["acessibilidade", "revisor"],
  "depends_on": ["T-21"]
}
```

---

## 3. Catálogo de Tarefas Novas

As sete novas tarefas identificadas durante a auditoria e os testes da Onda 1 foram estruturadas individualmente com severidade, responsável, critérios mensuráveis e arquivos esperados.

---

### Tarefa Nova 3.a [T-26 / UX-03]: Tradução em Português dos Erros de Autenticação do Supabase

- **Status:** ✅ **VERIFICADO** (Homologado no aparelho físico pelo usuário)
- **Severidade:** 🟡 **Média** (UX e Clareza de Mensagens ao Motorista)
- **Responsável:** Agente Integrador / Frontend
- **Revisores:** Agente Revisor Independente e Agente Designer
- **Contexto:** Em `SupabaseClient.kt:155, 177, 206, 232`, as falhas da API Supabase Auth são capturadas diretamente como `response.errorBody()?.string()` e repassadas sem tratamento para o `GiroCustoViewModel.kt`. Quando ocorre um erro como e-mail não confirmado ou taxa de envio excedida, o banner da `AuthGateScreen.kt` exibe JSON cru em inglês na tela do motorista (ex.: `{"code":400,"error_code":"email_not_confirmed","msg":"Email not confirmed"}` ou `{"error":"over_email_send_rate_limit"}`).
- **Definição de Critério de Segurança vs. UX:** 
  - **No fluxo de LOGIN:** Adota-se estritamente mensagem genérica para qualquer falha de credenciais (`invalid_credentials`), sem revelar se o e-mail existe ou não no sistema (*"E-mail ou senha incorretos. Verifique seus dados e tente novamente."*), prevenindo ataques de enumeração de contas em conformidade com a seção 1.4 de `padroes-seguranca-ux.md`.
  - **No fluxo de CADASTRO e E-mail Não Confirmado:** Adotam-se mensagens específicas (*"Já existe uma conta cadastrada com este e-mail. Faça login."* e *"Seu e-mail ainda não foi confirmado. Verifique sua caixa de entrada ou spam."*). Essa concessão é um compromisso deliberado de usabilidade (UX), formalmente documentado, indispensável para orientar entregadores que esquecem se já se cadastraram ou que aguardam a validação por link.
- **Arquitetura Desacoplada de Texto:** `AuthErrorMapper.kt` pertence à camada lógica e **NUNCA** deve chamar `stringResource` diretamente (função restrita a `@Composable`). O mapper deve retornar um identificador tipado de recurso (`@StringRes Int` ou `AuthErrorMessage` sealed) associado ao arquivo `strings.xml`. A tela Compose (`AuthGateScreen`) é a única responsável por resolver o identificador em texto visual através de `stringResource`.
- **Root Cause Evidence:** `SupabaseClient.kt:155, 177, 206, 232` e `GiroCustoViewModel.kt:479-482, 509-512, 534-537, 554-557`.

```json
{
  "task_id": "T-26",
  "to_agent": "integracoes",
  "task_type": "implementacao",
  "risk": "medio",
  "title": "Traduzir erros da API Supabase Auth para mensagens amigáveis em português com identificadores tipados",
  "context": "Falhas de login e cadastro exibem payloads JSON brutos em inglês no banner da tela AuthGateScreen. As respostas devem ser tratadas por um mapper desacoplado e convertidas para mensagens claras ao usuário brasileiro.",
  "root_cause_evidence": "SupabaseClient.kt:155, 177, 206, 232 e GiroCustoViewModel.kt:479-482, 509-512",
  "acceptance_criteria": [
    "Criar utilitário/mapper de erro de autenticação (AuthErrorMapper.kt) que analisa o código HTTP e o payload JSON da resposta, retornando um identificador tipado (@StringRes Int ou sealed type de erro)",
    "PROIBIDO usar stringResource dentro de AuthErrorMapper.kt ou fora de funções @Composable; a tela Compose resolve o texto a partir do identificador",
    "Critério de Segurança e UX formalmente aplicado:",
    "- No LOGIN: invalid_credentials / Invalid login credentials -> mensagem genérica R.string.auth_error_invalid_credentials ('E-mail ou senha incorretos. Verifique seus dados e tente novamente.') sem revelar se o e-mail existe no banco",
    "- No CADASTRO: user_already_exists / User already registered -> mensagem específica R.string.auth_error_user_already_exists ('Já existe uma conta cadastrada com este e-mail. Faça login.') aceita como compromisso de usabilidade documentado",
    "- No LOGIN ou CADASTRO: email_not_confirmed / Email not confirmed -> mensagem específica R.string.auth_error_email_not_confirmed ('Seu e-mail ainda não foi confirmado. Verifique sua caixa de entrada ou spam.') aceita como compromisso de usabilidade documentado",
    "- over_email_send_rate_limit / email rate limit exceeded -> R.string.auth_error_rate_limit ('Muitas tentativas em pouco tempo. Aguarde alguns minutos antes de tentar novamente.')",
    "- weak_password -> R.string.auth_error_weak_password ('A senha escolhida é muito fraca. Utilize ao menos 6 caracteres com números ou símbolos.')",
    "- Falhas de timeout ou conexão de rede -> R.string.auth_error_network ('Sem conexão com a internet. Verifique seu sinal e tente novamente.')",
    "- Erros desconhecidos -> mensagem amigável genérica sem vazar chaves JSON cruas, códigos de erro em inglês ou stack traces",
    "Criar testes unitários em AuthErrorMapperTest validando o mapeamento correto de cada payload e código HTTP para seu respectivo identificador de recurso",
    "Entregar roteiro de teste manual no aparelho, passo a passo completo, para validação visual (status máximo 🟡 sem teste no aparelho)"
  ],
  "files_expected": [
    "app/src/main/java/com/example/network/supabase/AuthErrorMapper.kt",
    "app/src/main/java/com/example/network/supabase/SupabaseClient.kt",
    "app/src/main/java/com/example/ui/screens/AuthGateScreen.kt",
    "app/src/main/java/com/example/ui/GiroCustoViewModel.kt",
    "app/src/main/res/values/strings.xml",
    "app/src/test/java/com/example/network/supabase/AuthErrorMapperTest.kt"
  ],
  "out_of_scope": ["Mudanças na arquitetura de navegação de AuthGateScreen"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug",
    "./gradlew assembleReleaseCheck"
  ],
  "required_reviewers": ["integracoes", "designer", "revisor"],
  "depends_on": []
}
```

---

### Tarefa Nova 3.b [T-27 / ARQ-05]: Remoção de signInWithPhone e Mensagem de Homologação Residual

- **Severidade:** 🟡 **Média** (Conformidade com a Regra 8 de Integridade e Remoção de Código Morto)
- **Responsável:** Agente Frontend / Arquiteto
- **Revisores:** Agente Revisor Independente e Agente de Segurança
- **Contexto:** Na Tarefa T-04, o botão de SMS na `AuthGateScreen.kt` foi desabilitado e rotulado como "Em breve". Contudo, o método `signInWithPhone` permaneceu em `GiroCustoViewModel.kt:517-523` contendo a mensagem proibida `"Login por SMS em homologação."`. Nenhuma tela ou componente chama este método, caracterizando código morto com mensagem de homologação em desacordo com as regras de integridade.
- **Root Cause Evidence:** `GiroCustoViewModel.kt:517-523` (`_authUiState.value = AuthUiState.Error("Login por SMS em homologação.")`).

```json
{
  "task_id": "T-27",
  "to_agent": "frontend",
  "task_type": "implementacao",
  "risk": "baixo",
  "title": "Remover método signInWithPhone e mensagem de homologação residual do GiroCustoViewModel",
  "context": "O método signInWithPhone no ViewModel não é chamado por nenhum componente e emite mensagem de homologação em produção, violando as regras de integridade.",
  "root_cause_evidence": "GiroCustoViewModel.kt:517-523",
  "acceptance_criteria": [
    "Remover o método signInWithPhone de GiroCustoViewModel.kt",
    "Confirmar com varredura pré-commit que a string 'Login por SMS em homologação' foi 100% eliminada do projeto",
    "Nenhum teste existente quebrado e compilação limpa"
  ],
  "files_expected": [
    "app/src/main/java/com/example/ui/GiroCustoViewModel.kt"
  ],
  "out_of_scope": ["Implementação de provedor real de SMS"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug",
    "./gradlew assembleReleaseCheck"
  ],
  "required_reviewers": ["arquiteto", "revisor"],
  "depends_on": []
}
```

---

### Tarefa Nova 3.c [Integrada à T-14a e T-14b / ARQ-01A e ARQ-01B]: Remoção de Ganchos de Teste em Produção com Asserções Idênticas

- **Severidade:** 🟠 **Alta** (Separação Rígida entre Código de Produção e Testes)
- **Responsável:** Agente Arquiteto
- **Revisores:** Agente Revisor Independente e Agente de Segurança
- **Contexto:** Durante as correções da T-06 e T-06b, foram introduzidos em `SupabaseClient.kt` os ganchos `@Volatile var testStorage: SecureSessionStorage? = null`, o método `resetForTesting` e o carregamento por reflexão dinâmica de `InMemorySessionStorage` via `Class.forName("com.example.network.supabase.InMemorySessionStorage")`. Adicionalmente, `GoogleAuthHelper.kt:37` mantém `var testAuthHandler`, `BiometricHelper.kt:27` mantém `testHardwareAvailable`, e os clientes `FipeNetworkClient`, `ViaCepNetworkClient` e `GiroCustoDatabase` mantêm ganchos de mock/reset. Essas construções violam a Seção 1 (Item 5) de `integridade-de-testes.md`.
- **Decisão:** Este escopo foi **formalmente dividido em duas etapas de menor risco (vide Seção 2.2)**: **T-14a** (focado no núcleo crítico de Autenticação, Biometria e Sessão Segura) e **T-14b** (focado em Clientes de Rede e Room), mantendo 100% das asserções dos testes existentes RIGOROSAMENTE IDÊNTICAS linha por linha.

---

### Tarefa Nova 3.d.1 [T-28A / ARQ-06A]: Diagnóstico, Inventário de Preferências e Desenho de Metas na Nuvem (Opção B)

- **Status:** ✅ **VERIFICADO** (Desenho técnico em `docs/design-metas-por-conta.md` formalmente aprovado pelo usuário em 26/10/2026)
- **Severidade:** 🟡 **Média** (Integridade de Dados e Desenho Arquitetural)
- **Responsável:** Agente Arquiteto / Backend
- **Revisores:** Agente Revisor Independente e Agente Designer
- **Contexto & Decisão do Usuário:** O proprietário do projeto selecionou formalmente a **Opção B** (sincronizar as metas na nuvem Supabase para acompanharem o motorista caso ele troque de celular). Em `GiroCustoViewModel.kt:926-941`, o método `resetAccountSpecificPreferences()` executa `prefs.edit().clear()`, apagando metas personalizadas e misturando preferências de tema. Para garantir que nenhuma meta seja perdida e que o esquema de banco seja robusto, a tarefa foi bifurcada:
  - **T-28A:** Diagnóstico técnico, inventário de preferências e elaboração da especificação técnica em `docs/design-metas-por-conta.md` (estritamente SEM CÓDIGO).
  - **T-28B:** Implementação no Room, Supabase e ViewModel (que só inicia após aprovação formal do desenho pelo usuário).
- **Inventário Mandatório de Preferências no Diagnóstico:**
  - O diagnóstico deve documentar exaustivamente tudo o que reside no arquivo `giro_custo_goals.xml` afetado por `prefs.edit().clear()`:
    1. As 12 metas do motorista: `daily_goal_gross`, `daily_goal_net`, `daily_goal_km`, `daily_goal_deliveries`, `weekly_goal_gross`, `weekly_goal_net`, `weekly_goal_km`, `weekly_goal_deliveries`, `monthly_goal_gross`, `monthly_goal_net`, `monthly_goal_km`, `monthly_goal_deliveries`.
    2. A preferência global de tema visual `is_dark_theme` (que estava indevidamente acoplada no mesmo arquivo de metas, exigindo leitura e regravação manual para não ser perdida).
    3. Mapear o que mais é apagado ou afetado sem querer ao alternar de conta no fluxo `resetAccountSpecificPreferences()` (ex.: `last_sync_timestamp` em `giro_custo_sync_prefs` e quaisquer preferências residuais).
- **Especificações Obrigatórias do Desenho (`docs/design-metas-por-conta.md`):**
  - **(a) Onde guardar as metas:** Decidir e justificar se as metas devem ser colunas adicionais na tabela `user_profile` ou uma tabela dedicada `user_goals` (análise de cardinalidade 1:1, performance de sync e concorrência); definir o schema no Room (migration MIGRATION_13_14 e schema 14.json, coordenada com a versão 13 da T-08 e preparando o banco para a futura conversão monetária em T-18B na versão 15); definir o schema no Supabase (script SQL com políticas RLS restritas ao dono `auth.uid() = user_id`, status inicial ⛔ BLOQUEADO até execução no painel); e desenhar o fluxo no `SupabaseSyncManager`.
  - **(b) Regra de conflito e offline:** Definir regra de resolução Last-Write-Wins (LWW) baseada em timestamp `updatedAt` / `updated_at`; garantir que o motorista possa alterar suas metas 100% offline, enfileirando para sincronização posterior com status pendente.
  - **(c) Valores padrão:** Especificar valores padrão de fábrica caso a conta nunca tenha configurado metas (R$ 200 bruto, R$ 130 líquido, 100 km, 15 entregas diárias; proporcionalidade semanal e mensal).
  - **(d) Migração transparente de preferências locais:** Projetar a migração segura das metas atualmente armazenadas em `giro_custo_goals.xml` para a nova entidade no primeiro boot após o update, garantindo que nenhum motorista perca suas metas atuais.
  - **(e) Interação com a troca de conta (T-06c):** As metas passam a ser propriedade da conta ativa (`userId`); ao trocar de conta, as metas da conta logada são carregadas do Room/nuvem e o reset de preferências deixa de apagá-las.
  - **(f) Impacto na exclusão de conta (T-10) e sincronização (T-30):** Incluir o expurgo de metas na RPC remota e no expurgo local da T-10; e incluir as metas nas travas fail-safe de dono da T-30.
  - **(g) Plano de testes e roteiro manual:** Definir plano de testes automatizados e roteiro passo a passo para validação manual no aparelho.
- **Root Cause Evidence:** `GiroCustoViewModel.kt:407-420, 922-944, 1141-1154`.

```json
{
  "task_id": "T-28A",
  "to_agent": "arquiteto",
  "task_type": "diagnostico",
  "risk": "medio",
  "title": "Diagnóstico, inventário de preferências e especificação técnica para sincronização de metas na nuvem (Opção B)",
  "context": "Metas de faturamento são resetadas ao trocar de conta. O usuário escolheu a Opção B (sincronizar na nuvem). É necessário inventariar SharedPreferences e especificar detalhadamente a arquitetura de metas antes de codificar.",
  "root_cause_evidence": "GiroCustoViewModel.kt:407-420, 922-944",
  "acceptance_criteria": [
    "Entregar documento técnico completo em docs/design-metas-por-conta.md sem alterar nenhuma linha de código",
    "Incluir inventário completo de giro_custo_goals.xml e giro_custo_sync_prefs",
    "Decidir e justificar os 7 tópicos mandatórios (a) a (g): localização no Room/Supabase, regra de conflito LWW, valores padrão, migração de prefs legadas, isolamento por conta, impacto na T-10/T-30 e plano de testes",
    "PARADA OBRIGATÓRIA: Submeter o desenho para aprovação formal do proprietário do projeto antes de liberar a T-28B"
  ],
  "files_expected": [
    "docs/design-metas-por-conta.md"
  ],
  "out_of_scope": ["Alterações em arquivos Kotlin, build ou banco de dados nesta etapa"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
  ],
  "required_reviewers": ["arquiteto", "backend", "revisor"],
  "depends_on": ["T-06c"]
}
```

---

### Tarefa Nova 3.d.2 [T-28B / DAT-03]: Implementação da Sincronização de Metas na Nuvem (Opção B) no Room e Supabase

- **Severidade:** 🟠 **Alta** (Integridade de Dados e Sincronização Multi-dispositivo)
- **Responsável:** Agente Backend / Integrações
- **Revisores:** Agente Revisor Independente, Agente de Segurança e Agente Arquiteto
- **Contexto:** Execução da implementação prática das metas persistidas e sincronizadas, conforme a arquitetura aprovada em T-28A.
- **Root Cause Evidence:** `GiroCustoViewModel.kt:407-420, 922-944, 1141-1154`.

```json
{
  "task_id": "T-28B",
  "to_agent": "backend",
  "task_type": "implementacao",
  "risk": "alto",
  "title": "Implementar persistência de metas no Room (v14) e sincronização Supabase com resolução LWW",
  "context": "Implementar as entidades, DAOs, sincronização e mappers de metas com base no desenho técnico aprovado na T-28A, eliminando a dependência volátil do SharedPreferences.",
  "root_cause_evidence": "GiroCustoViewModel.kt:407-420, 922-944, 1141-1154",
  "acceptance_criteria": [
    "Implementar modelo de persistência de metas no Room conforme especificação aprovada em T-28A",
    "Criar migração Room MIGRATION_13_14 e testar compulsoriamente com runMigrationsAndValidate do MigrationTestHelper para assegurar conformidade do schema 14",
    "Exportação do schema 14.json gerada e versionada em app/schemas/com.example.data.GiroCustoDatabase/14.json",
    "Criar script SQL em supabase/migrations/20261026_user_goals.sql com políticas RLS restritas ao dono (auth.uid() = user_id) e documentar em supabase/README.md (Status do script: ⛔ BLOQUEADO até execução no painel web)",
    "Implementar rotinas de PUSH e PULL no SupabaseSyncManager com resolução de conflitos Last-Write-Wins (LWW) via updatedAt",
    "Migrar automaticamente as metas salvas anteriormente em giro_custo_goals.xml para o novo modelo persistido no primeiro boot, garantindo perda zero",
    "Desacoplar a preferência de tema visual is_dark_theme para armazenamento independente",
    "Atualizar resetAccountSpecificPreferences() em GiroCustoViewModel para não mais expurgar as metas ao trocar de conta, carregando os dados da conta ativa",
    "Criar testes unitários em UserGoalsSyncTest cobrindo persistência, conflito LWW, migração de SharedPreferences legados e suporte offline",
    "Entregar roteiro de teste manual no aparelho, passo a passo completo, para validação da sincronização de metas entre contas (status máximo 🟡 sem teste no aparelho)"
  ],
  "files_expected": [
    "app/src/main/java/com/example/data/Entities.kt",
    "app/src/main/java/com/example/data/Daos.kt",
    "app/src/main/java/com/example/data/GiroCustoDatabase.kt",
    "app/src/main/java/com/example/network/supabase/SupabaseClient.kt",
    "app/src/main/java/com/example/network/supabase/SupabaseMappers.kt",
    "app/src/main/java/com/example/network/supabase/SupabaseSyncManager.kt",
    "app/src/main/java/com/example/ui/GiroCustoViewModel.kt",
    "app/schemas/com.example.data.GiroCustoDatabase/14.json",
    "supabase/migrations/20261026_user_goals.sql",
    "supabase/README.md",
    "app/src/test/java/com/example/UserGoalsSyncTest.kt"
  ],
  "out_of_scope": ["Alteração visual dos layouts de configuração de metas"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug",
    "./gradlew assembleReleaseCheck"
  ],
  "required_reviewers": ["backend", "integracoes", "revisor", "seguranca"],
  "depends_on": ["T-28A", "T-08"]
}
```

---

### Tarefa Nova 3.e [T-29 / SEC-05B]: Aperto Cirúrgico das Regras ProGuard/R8

- **Severidade:** 🟠 **Alta** (Minificação Efetiva e Proteção de Binário)
- **Responsável:** Agente DevOps / Segurança
- **Revisores:** Agente Revisor Independente e Agente Arquiteto
- **Contexto:** Na Tarefa T-07, para assegurar a primeira compilação do R8 sem interrupções, foram adicionadas em `app/proguard-rules.pro:70-72` regras abrangentes que preservam integralmente todos os pacotes do app (`-keep class com.example.data.** { *; }`, `-keep class com.example.network.** { *; }`, `-keep class com.example.util.** { *; }`). Essas regras impedem a minificação e ofuscação da quase totalidade das classes do app, anulando os ganhos de segurança contra engenharia reversa.
- **Root Cause Evidence:** `app/proguard-rules.pro:70-72`.

```json
{
  "task_id": "T-29",
  "to_agent": "devops",
  "task_type": "seguranca",
  "risk": "alto",
  "title": "Apertar as regras de keep em proguard-rules.pro para regras cirúrgicas mínimas e validar em releaseCheck",
  "context": "As regras atuais mantêm todos os membros de data, network e util sem ofuscação. As regras devem ser reduzidas ao mínimo indispensável (apenas DTOs Moshi e contratos do Room), permitindo que repositórios e utilitários sejam ofuscados e otimizados pelo R8.",
  "root_cause_evidence": "app/proguard-rules.pro:70-72",
  "acceptance_criteria": [
    "Remover as regras catch-all: -keep class com.example.data.**, network.** e util.**",
    "Substituir por regras cirúrgicas mínimas estritamente necessárias:",
    "- Room: entidades (@Entity) e DAOs (@Dao) já possuem regras genéricas ativas;",
    "- Moshi: classes anotadas com @JsonClass(generateAdapter = true) e adapters gerados *JsonAdapter;",
    "- Retrofit: interfaces de serviço com anotações @retrofit2.http.* e classes de retorno genéricas;",
    "- DataOwnerStorage e SecureSessionStorage: apenas métodos contratuais essenciais caso haja chamada externa;",
    "Compilação com ./gradlew assembleReleaseCheck executada com sucesso sem avisos não tratados de compilação",
    "Suíte completa de testes unitários automatizados 100% verde",
    "Entregar roteiro manual no aparelho executando os seguintes 9 fluxos essenciais na versão releaseCheck minificada:",
    "  1. Login com Google;",
    "  2. Login com e-mail e senha;",
    "  3. Autenticação biométrica;",
    "  4. Consulta de CEP (botão 'Consultar Correios');",
    "  5. Sincronização com Supabase (PUSH e PULL);",
    "  6. Consulta à Tabela FIPE (marcas, modelos e anos);",
    "  7. Mapa de postos de combustível (busca Overpass);",
    "  8. Exclusão e Desfazer de lançamento/veículo (Snackbar com Desfazer);",
    "  9. Instalação e abertura imediata do app logo após atualizar por cima da versão anterior (sem desinstalar antes, preservando banco Room e Keystore);",
    "Monitorar a execução manual no dispositivo com o comando: adb logcat -d | grep -E \"FATAL EXCEPTION|ClassNotFoundException|NoSuchMethodError|NoSuchFieldException|JsonDataException\" (status máximo 🟡 sem teste no aparelho)"
  ],
  "files_expected": [
    "app/proguard-rules.pro"
  ],
  "out_of_scope": ["Assinatura com chave de produção (upload-key.jks)"],
  "verification": [
    "./gradlew assembleReleaseCheck",
    "./gradlew testDebugUnitTest lintDebug"
  ],
  "required_reviewers": ["seguranca", "devops", "revisor"],
  "depends_on": ["T-07"]
}
```

---

### Tarefa Nova 3.f [T-30 / SEC-09]: Blindagem Fail-Safe na Trava de Dono em SupabaseSyncManager

- **Severidade:** 🟠 **Alta** (Segurança e Integridade de Dados Multi-usuário)
- **Responsável:** Agente de Segurança / Integrações
- **Revisores:** Agente Revisor Independente e Agente Arquiteto
- **Contexto:** Em `SupabaseSyncManager.kt:53-60, 193-200`, as travas de verificação de dono executam:
  ```kotlin
  val ownerId = dataOwnerStorage?.getDataOwnerId()
  if (ownerId != null && ownerId != userId) {
      throw DataOwnerMismatchException(...)
  }
  ```
  Se `dataOwnerStorage` for nulo (injeção opcional) OU se `ownerId` for nulo (dono desconhecido), a condição avalia para falso e o `SupabaseSyncManager` libera o envio e recebimento de dados sem restrição! Embora o `GiroCustoViewModel` possua uma camada de proteção prévia, a classe de rede `SupabaseSyncManager` deve operar em modelo **fail-safe**: nunca permitir sincronização com banco sem dono confirmado quando existirem dados locais, prevenindo vazamento de dados caso o sync seja acionado diretamente por workers em segundo plano.
- **Root Cause Evidence:** `SupabaseSyncManager.kt:19, 22, 53-60, 193-200`.

```json
{
  "task_id": "T-30",
  "to_agent": "seguranca",
  "task_type": "implementacao",
  "risk": "alto",
  "title": "Tornar DataOwnerStorage obrigatório e implementar trava fail-safe quando dono for nulo em SupabaseSyncManager",
  "context": "SupabaseSyncManager permite injeção nula de DataOwnerStorage e libera envio quando ownerId é nulo, gerando risco de sincronização indevida se acionado diretamente fora do ViewModel.",
  "root_cause_evidence": "SupabaseSyncManager.kt:19, 22, 53-60, 193-200",
  "acceptance_criteria": [
    "Tornar DataOwnerStorage dependência obrigatória e não-nula (remover valor default null no construtor primário de SupabaseSyncManager)",
    "Trava Fail-Safe no PUSH e PULL: se ownerId == null mas o banco local possuir registros (hasAnyLocalData == true), lançar compulsoriamente DataOwnerMismatchException impedindo qualquer envio REST à nuvem",
    "Adicionar teste automatizado cobrindo o cenário de nova instalação do app: banco Room vazio, dono desconhecido/nulo no DataOwnerStorage, sincronização PULL permitida normalmente e o dono autenticado gravado no DataOwnerStorage após a conclusão",
    "Garantir alinhamento com a máquina de estados do ViewModel: quando o usuário optar por 'Estes dados são meus', o ViewModel define o dono no DataOwnerStorage ANTES de disparar o sync",
    "Nos testes existentes (SupabaseSyncAndMigrationTest.kt e AccountIsolationAndSyncTest.kt), SOMENTE a forma de construir os objetos pode mudar (passando o DataOwnerStorage); as asserções de validação de sync e migration permanecem RIGOROSAMENTE IDÊNTICAS, com conferência linha a linha do Revisor Independente",
    "Nenhum teste existente quebrado, enfraquecido ou removido"
  ],
  "files_expected": [
    "app/src/main/java/com/example/network/supabase/SupabaseSyncManager.kt",
    "app/src/main/java/com/example/data/Repository.kt",
    "app/src/test/java/com/example/network/supabase/AccountIsolationAndSyncTest.kt",
    "app/src/test/java/com/example/SupabaseSyncAndMigrationTest.kt"
  ],
  "out_of_scope": ["Alterações nas rotas REST do Supabase"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug",
    "./gradlew assembleReleaseCheck"
  ],
  "required_reviewers": ["seguranca", "integracoes", "revisor"],
  "depends_on": ["T-06c"]
}
```

---

### Tarefa Nova 3.g [Integrada à T-10 / SEC-06]: Exclusão Segura com DataOwnerStorage, Metas e Proteção Local

- **Severidade:** 🟠 **Alta** (Conformidade LGPD, Integridade de Metas e Consistência Multi-conta)
- **Responsável:** Agente de Segurança / Backend
- **Revisores:** Agente Revisor Independente e Agente Arquiteto
- **Contexto:** Ao solicitar a exclusão definitiva da conta, o aplicativo não pode apenas chamar o servidor Supabase: se o `DataOwnerStorage` não for limpo e as tabelas locais (incluindo as metas sincronizadas na nuvem) não forem expurgadas, o aparelho permanecerá com o ID do usuário excluído registrado como dono, bloqueando qualquer sincronização ou novo login no mesmo aparelho. Além disso, se a chamada remota falhar, nada local pode ser destruído, e a interface deve oferecer exportação prévia e confirmação reforçada.
- **Decisão:** Este requisito foi **formalmente incorporado aos critérios de aceite da Tarefa T-10 [SEC-06]** (vide Seção 2.2), abrangendo proteção fail-safe local contra falha remota, confirmação reforçada, oferta de exportação prévia via SAF e o ciclo completo de expurgo local (`clearAllLocalData` com metas, `clearDataOwner`, `signOut`, `clearBiometrics` e reset de preferências), com roteiro manual exclusivo em conta descartável.

---

## 4. Proposta de Ordem de Execução, Dependências e Análise de Risco

### 4.1 Princípios da Ordenação Proposta

A ordem proposta para a execução da Onda 2 foi estruturada respeitando:
1. **Segurança e Higienização Imediata Primeiro:** Eliminar código morto com mensagens de homologação (T-27) e blindar travas de dados multi-conta (T-30 e T-26).
2. **Desacoplamento e Testes Limpos:** Limpar os ganchos de teste em produção (T-14) e apertar as regras do R8 (T-29) antes de fazer grandes alterações de dados.
3. **Integridade de Dados e Performance:** Índices no Room (T-08) e chaves estáveis em listas (T-09).
4. **LGPD e Acessibilidade:** Exclusão de conta com limpeza de dono (T-10), rationale de GPS (T-11), acessibilidade TalkBack (T-15) e estados visuais (T-12, T-13, T-16).
5. **Decisões e Paradas Planejadas:** T-28 (desenho de metas) e T-17 (testes de template) antes de abrir a Onda 3.

---

### 4.2 Matriz Sequencial de Execução da Onda 2

| Ordem | ID Tarefa | Tipo | Risco | Responsável | Depende de | Impacto Principal |
| :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **1º** | **T-27** | Limpeza | Baixo | Frontend | - | Remove `signInWithPhone` e mensagem proibida de homologação no ViewModel. (Status: ✅ **VERIFICADO**) |
| **2º** | **T-30** | Segurança | Alto | Segurança | T-06c | Blindagem fail-safe de dono nulo no `SupabaseSyncManager` com teste de instalação nova e testes existentes idênticos. (Status: ✅ **VERIFICADO**) |
| **3º** | **T-26** | UX/Rede | Médio | Integrações | - | Tradução amigável em pt-BR de erros Supabase Auth com identificador tipado; login genérico e cadastro/não-confirmado específicos. (Status: ✅ **VERIFICADO**) |
| **4º** | **T-14a** | Arquitetura/Segurança | Alto | Arquiteto | T-06b, T-06c | Remoção de ganchos `test...`, `resetForTesting` e reflexão em Auth e Sessão (`GoogleAuthHelper`, `BiometricHelper`, `SupabaseClient`) com asserções idênticas e roteiro manual de login Google e biometria. (Status: ✅ **VERIFICADO**) |
| **5º** | **T-14b** | Arquitetura | Alto | Arquiteto | T-14a | Remoção de ganchos de teste em clientes de rede e banco (`FipeNetworkClient`, `ViaCepNetworkClient`, `GiroCustoDatabase`) com asserções idênticas. (Status: ✅ **VERIFICADO**) |
| **6º** | **T-29** | DevOps/R8 | Alto | DevOps | T-07 | Aperto cirúrgico das regras de keep do ProGuard/R8, teste em `releaseCheck` e roteiro manual de 9 fluxos com comando de logcat. (Status: ✅ **VERIFICADO**) |
| **7º** | **T-28A** | Diagnóstico | Médio | Arquiteto | T-06c | Diagnóstico, inventário de preferências e desenho técnico para sincronização de metas na nuvem (Opção B) em `docs/design-metas-por-conta.md`. (Status: ✅ **VERIFICADO** — Aprovado formalmente pelo usuário em 26/10/2026) |
| **8º** | **T-08** | Dados | Alto | Backend | - | Índices Room nas 7 tabelas e Migration v13 com `runMigrationsAndValidate`. (Status: ✅ **VERIFICADO**) |
| **9º** | **T-28B** | Implementação/Dados | Alto | Backend | T-28A (Aprovado), T-08 | Implementação das metas sincronizadas na nuvem (Room v14 com MIGRATION_13_14 e schema 14.json, Supabase SQL, Sync, ViewModel), preservando preferências legadas (Status SQL: ⛔ BLOQUEADO até execução no painel). |
| **10º** | **T-09** | Performance | Alto | Frontend | - | Chaves estáveis (`key`) em todas as LazyColumns. |
| **11º** | **T-10** | Segurança/LGPD | Alto | Segurança | T-06c, T-30, T-28B | Exclusão de conta via RPC Supabase com limpeza local, `DataOwnerStorage` e metas (falha no servidor não apaga nada local, confirmação reforçada, oferta de exportação e teste com conta descartável) (Status SQL: ⛔ BLOQUEADO até execução no painel). |
| **12º** | **T-11** | UX/Privacidade | Médio | Frontend | - | Diálogo explicativo de rationale prévia antes de solicitar permissão de GPS. |
| **13º** | **T-13** | Integração | Médio | Integrações | - | Alinhamento de timeouts do OkHttp para 25s na Overpass API. |
| **14º** | **T-15** | Acessibilidade | Alto | Acessibilidade | - | Inserção de `contentDescription` acessível em todos os botões e ícones clicáveis. |
| **15º** | **T-12** | UX/Design | Médio | Designer | T-03 | Padronização dos estados de Carregando, Vazio, Erro e Offline nas telas. |
| **16º** | **T-16** | Arquitetura/UI | Médio | Frontend | - | Elevação de cálculos e regras pesadas dos Composables para o ViewModel. |
| **17º** | **T-17** | Testes | Médio | QA | - | Substituição de testes de modelo (2+2) por regras reais (**PARADA OBRIGATÓRIA** para autorização do usuário). |

---

### 4.3 Transição para a Onda 3 e Tratamento Especial de T-18 (Centavos)

A **Tarefa T-18** permanece estritamente dividida em duas etapas com controle de decisão:

1. **T-18A [DAT-01A] (Diagnóstico e Planejamento):**
   - Mapeamento completo e elaboração de `docs/plano-migracao-monetaria.md` sem alteração de nenhuma linha de código.
2. **PARADA OBRIGATÓRIA:**
   - O Orquestrador submete o plano monetário ao proprietário do projeto e aguarda sua aprovação expressa.
3. **T-18B [DAT-01B] (Implementação da Migração Monetária):**
   - Somente após a aprovação formal do usuário, a migração do Room (v15 com MIGRATION_14_15 e schema 15.json) e conversão para centavos (`Long`) é implementada.

As demais tarefas da Onda 3 seguem a ordem: **T-19** (schemas Room), **T-20** (decomposição de Profile, FuelRefills, History e ViewModel), **T-21** (strings Dashboard/Launch), **T-22** (remoção de libs de IA), **T-23** (limpeza de colors.xml), **T-24** (telas secundárias) e **T-25** (conclusão de strings).

---

## 5. Lista de Decisões que Dependem do Proprietário do Projeto

As validações manuais das tarefas da Onda 1 no smartphone físico (T-03, T-06c e T-07) foram concluídas com sucesso pelo usuário e todas alcançaram o status oficial ✅ **VERIFICADO** (restando apenas o teste de 70 minutos da renovação do token da T-06, a ser confirmado pelo usuário). Além disso, a definição sobre as metas foi formalmente resolvida com a sua escolha pela **Opção B** (sincronização na nuvem).

Abaixo estão listadas, de forma transparente e objetiva, as decisões e aprovações que dependem exclusivamente de você antes ou durante a execução da Onda 2:

### Decisão 1: Aprovação do Desenho Técnico de Metas por Conta (T-28A) Antes da Implementação (T-28B) — [RESOLVIDA]
- **Status:** ✅ **APROVADO FORMALMENTE PELO PROPRIETÁRIO DO PROJETO EM 26/10/2026**
- **Contexto:** A Opção B (sincronizar metas na nuvem) foi escolhida para que as metas acompanhem o motorista ao trocar de aparelho. A tarefa T-28A elaborou o documento técnico completo `docs/design-metas-por-conta.md` (SEM CÓDIGO) contemplando o inventário das preferências, a escolha entre colunas em `user_profile` vs. tabela `user_goals`, migration Room MIGRATION_13_14 (schema 14.json), scripts Supabase com RLS, regras de conflito LWW, valores padrão, migração de dados legados e impacto na T-10 e T-30.
- **Resultado:** Desenho técnico aprovado formalmente no chat. A Tarefa T-28B está autorizada para execução respeitando a ordem sequencial de versionamento do Room (após a conclusão da T-08).

---

### Decisão 2: Execução de Scripts SQL no Painel Web do Supabase (T-10 e T-28B)
- **Contexto:** Para manter o app seguro sem chaves `service_role` no cliente e respeitar a LGPD e o isolamento de metas, duas funções/tabelas dependem de scripts no banco Supabase:
  1. **T-10 (Exclusão definitiva de conta):** RPC `delete_user_account()` via arquivo `supabase/migrations/20261026_user_deletion_rpc.sql`.
  2. **T-28B (Metas sincronizadas):** Schema e RLS de metas via arquivo `supabase/migrations/20261026_user_goals.sql`.
- **Ação necessária:** O agente criará os scripts e documentará passo a passo no README. Você precisará abrir o painel web do Supabase, colar cada script no **SQL Editor** e clicar em **Run**. Cada uma dessas tarefas permanecerá em ⛔ **BLOQUEADO** para a etapa remota até a sua confirmação.

---

### Decisão 3: Autorização Explícita para Substituição dos Testes de Template (T-17)
- **Contexto:** A Seção 1 de `integridade-de-testes.md` proíbe remover qualquer teste sem autorização prévia por escrito no chat. Os testes herdados de templates de projeto (`assertEquals(4, 2 + 2)` e similares) precisam ser substituídos por testes úteis de regras de validação real.
- **Ação necessária:** Quando a tarefa T-17 for iniciada, o agente fará uma parada mandatória no chat listando os testes afetados para que você responda autorizando a substituição (**PARADA OBRIGATÓRIA**).

---

### Decisão 4: Aprovação do Diagnóstico Monetário (T-18A) Antes da Implementação (T-18B)
- **Contexto:** A conversão de `Double` para centavos (`Long`) mexe nos cálculos financeiros de todo o aplicativo e no banco de dados Room.
- **Ação necessária:** O agente entregará em T-18A o documento completo de diagnóstico em `docs/plano-migracao-monetaria.md`. Você precisará ler e aprovar formalmente o plano antes de autorizar o início da codificação em T-18B (**PARADA OBRIGATÓRIA**).

---

## 6. Registro de Ajustes v2.2

Esta seção consolida e rastreia formalmente todos os 11 ajustes efetuados no Plano de Execução da Onda 2 na versão v2.2, em estrito alinhamento com as deliberações e comandos do usuário:

1. **Atualização do Quadro de Situação da Onda 1 (Seção 1):**
   - As tarefas **T-03** (remoção do SmartphoneSimulator da produção), **T-06c** (isolamento estrito de dados e travas de sync) e **T-07** (ativação de R8 e regras ProGuard) foram testadas pelo usuário no aparelho físico, passaram com sucesso e foram atualizadas para o status oficial ✅ **VERIFICADO**.
   - A antiga Decisão 1 da Seção 5 (validação da Onda 1 no smartphone) foi removida por estar integralmente resolvida.
   - Registrado formalmente que resta apenas o teste de 70 minutos da renovação do token da T-06, a ser confirmado pelo usuário no aparelho.

2. **Rastreabilidade e Retificação Histórica da Tarefa T-06d (Seção 1):**
   - Corrigido o commit oficial da T-06d para `5da367d` (*"Segurança: remove dados pessoais e senha dos documentos"*).
   - Registrado que o commit `bc7453b` (*"docs: salvar backups CSV pre-limpeza da base Supabase"*) foi o commit anterior que indevidamente enviou exportações de dados ao repositório e NÃO corresponde à entrega da T-06d.
   - Retificada a descrição da tarefa, eliminando a alegação incorreta de mascaramento de CSV e documentando a remoção total de dados pessoais, senhas e arquivos CSV do histórico do repositório.

3. **Rastreabilidade Completa dos Commits da Tarefa T-06 (Seção 1):**
   - Incluídos no quadro da Seção 1 os 4 commits executados no ciclo da T-06: `8d2641d`, `78f88d2`, `abfdda2` e `fa80a8e`.
   - Registrado de forma transparente que o commit `78f88d2` (*"UI/Acessibilidade: corrige layout vertical do botão Consultar Correios em Meus Dados"*) representou uma alteração de layout extra realizada durante o ciclo.

4. **Definição de Critério de Segurança vs. UX e Arquitetura de Erros na Tarefa T-26 (Seção 3.a):**
   - Resolvida a contradição entre proteção anti-enumeração de usuários e mensagens claras ao entregador:
     - No **LOGIN**: Mantida mensagem rigorosamente genérica para credenciais incorretas (*"E-mail ou senha incorretos. Verifique seus dados e tente novamente."*), sem revelar se o e-mail existe no banco, em estrita conformidade com a Seção 1.4 de `padroes-seguranca-ux.md`.
     - No **CADASTRO** e em **E-MAIL NÃO CONFIRMADO**: Adotadas mensagens específicas (*"Já existe uma conta cadastrada com este e-mail. Faça login."* e *"Seu e-mail ainda não foi confirmado. Verifique sua caixa de entrada ou spam."*), aceitas como compromisso justificado de usabilidade e documentadas.
   - Arquitetura desacoplada: `AuthErrorMapper.kt` deve retornar identificadores tipados de texto (`@StringRes Int` ou sealed type de erro), sendo expressamente proibido invocar `stringResource` fora de funções `@Composable`; a tela Compose (`AuthGateScreen`) é quem resolve e obtém a string localizada.

5. **Blindagem e Suíte de Testes da Trava de Dono na Tarefa T-30 (Seção 3.f):**
   - Inclusão formal de `SupabaseSyncAndMigrationTest.kt` e dos demais testes que constroem `SupabaseSyncManager` na lista de `files_expected`.
   - Regra estrita de integridade: nos testes existentes, é permitida exclusivamente a adaptação da construção dos objetos (passando a instância obrigatória e não-nula de `DataOwnerStorage`); as asserções de teste permanecem RIGOROSAMENTE IDÊNTICAS linha por linha, com auditoria obrigatória do Agente Revisor Independente.
   - Adicionado novo teste unitário cobrindo o cenário de nova instalação (banco de dados Room vazio, dono nulo/desconhecido no `DataOwnerStorage`, sincronização PULL permitida e o `ownerId` registrado no storage após o sucesso).

6. **Blindagem contra Perda de Dados, Confirmação e Exportação na Tarefa T-10 (Seção 2.2 e 3.g):**
   - Incorporada proteção fail-safe: se a chamada à RPC remota falhar (erro HTTP, timeout ou sem rede), NADA local é apagado (tabelas Room, preferências, metas e `DataOwnerStorage` intactos), com teste automatizado obrigatório cobrindo a falha e a preservação local.
   - Implementação de confirmação reforçada na interface para ação irreversível (diálogo de alerta com exigência de digitação de palavra de segurança como 'EXCLUIR' ou confirmação em duas etapas).
   - Oferta proativa de exportação de dados via SAF (JSON/CSV) antes de prosseguir com a exclusão definitiva.
   - Roteiro de teste manual no aparelho físico restrito obrigatoriamente ao uso de contas de teste descartáveis (proibido testar com a conta real do usuário).

7. **Divisão Granular da Tarefa T-14 em T-14a e T-14b (Seções 2.2 e 3.c):**
   - Dividida em duas tarefas focadas e isoladas:
     - **T-14a [ARQ-01A]:** Núcleo de Autenticação, Biometria e Sessão Segura (`GoogleAuthHelper`, `BiometricHelper`, `SupabaseClient`), com remoção de `testAuthHandler`, `testHardwareAvailable`, `resetForTesting`, `@Volatile var testStorage` e reflexão de `InMemorySessionStorage`, acompanhada de roteiro manual de login Google e biometria no aparelho.
     - **T-14b [ARQ-01B]:** Clientes de Rede e Banco (`FipeNetworkClient`, `ViaCepNetworkClient`, `GiroCustoDatabase`), com remoção de variáveis públicas de mock e `resetForTesting`.
   - Dependências e ordem ajustadas: T-14a executa primeiro e T-14b depende de T-14a; asserções de testes 100% idênticas conferidas pelo Revisor.

8. **Inventário Exaustivo de Preferências no Diagnóstico da Tarefa T-28A (Seção 3.d.1):**
   - Inclusão no diagnóstico do mapeamento minucioso de todo o conteúdo do arquivo `giro_custo_goals.xml` afetado por `prefs.edit().clear()` no método `resetAccountSpecificPreferences()` (as 12 metas diárias, semanais e mensais e a preferência global de tema `is_dark_theme`).
   - Mapeamento de efeitos colaterais ao trocar de conta, incluindo a remoção de `last_sync_timestamp` em `giro_custo_sync_prefs` e quaisquer preferências residuais.

9. **Roteiro Manual Completo de 9 Fluxos e Monitoramento de Logcat na Tarefa T-29 (Seção 3.e):**
   - Especificação detalhada dos 9 fluxos essenciais a serem validados no APK `releaseCheck` minificado: (1) Login Google; (2) Login e-mail/senha; (3) Biometria; (4) Consulta de CEP (Consultar Correios); (5) Sincronização Supabase; (6) Tabela FIPE; (7) Mapa de postos; (8) Exclusão e Desfazer; (9) Instalação/abertura logo após atualização sobre a versão anterior sem desinstalação prévia.
   - Inclusão do comando obrigatório de monitoramento no terminal durante os testes:
     `adb logcat -d | grep -E "FATAL EXCEPTION|ClassNotFoundException|NoSuchMethodError|NoSuchFieldException|JsonDataException"`.

10. **Reestruturação da Matriz Sequencial e Governança de Paradas Obrigatórias (Seção 4):**
    - Atualizada a matriz de execução de 17 tarefas refletindo a separação de T-14a/T-14b, a inserção do diagnóstico T-28A e a implementação T-28B.
    - Mantidas com destaque explícito as paradas obrigatórias que exigem autorização expressa do usuário antes de prosseguir: **T-17** (autorização para substituição de testes de modelo) e **T-18A** (aprovação formal do diagnóstico de migração monetária).

11. **Formalização da Decisão do Usuário sobre Metas e Bifurcação em T-28A e T-28B (Seção 3.d e 4.2):**
    - Registrada a escolha formal da **Opção B** pelo usuário (sincronizar metas na nuvem Supabase).
    - Transformação da T-28 em:
      - **T-28A [ARQ-06A]:** Diagnóstico, inventário e desenho técnico em `docs/design-metas-por-conta.md` (SEM CÓDIGO), decidindo e justificando os pontos (a) localização no Room/Supabase, (b) regra de conflito LWW e offline, (c) valores padrão, (d) migração de preferências locais legadas sem perdas, (e) isolamento por conta na troca de usuário, (f) impacto na exclusão de conta T-10 e na trava T-30, e (g) plano de testes. PARADA OBRIGATÓRIA para aprovação do desenho.
      - **T-28B [DAT-03]:** Implementação no Room, Supabase e ViewModel, com script SQL com RLS (status ⛔ BLOQUEADO até execução no painel), dependente da conclusão da T-08 e da aprovação prévia do desenho da T-28A.

12. **Resolução de Conflito de Versão do Banco Room no Plano (T-08, T-28B, T-18B, T-19):**
    - Identificado e corrigido o conflito de versão do banco: a tarefa T-28B (implementação de metas, ordem 9) altera o Room antes da T-18B (migração monetária na Onda 3), não podendo ambas utilizarem a versão 14.
    - **T-08:** Mantida na versão 13 (`MIGRATION_12_13`, schema `13.json`) para índices nas 7 tabelas.
    - **T-28B:** Alocada para a versão 14 do Room (`MIGRATION_13_14`, schema `14.json`). Inclusão nos critérios da migração e validação com `runMigrationsAndValidate`, e inclusão de `app/schemas/com.example.data.GiroCustoDatabase/14.json` em `files_expected`.
    - **T-18B:** Atualizada para a versão 15 do Room (`MIGRATION_14_15`, schema `15.json`), atualizando critérios de aceite, `files_expected` (`15.json`), Seção 4.3 e dependências formais (`depends_on: ["T-18A", "T-08", "T-28B"]`).
    - **T-19:** Ajustada a documentação de histórico de schemas para abranger explicitamente as versões 13, 14 e 15 (`1->15`), dependendo de T-08, T-28B e T-18B.
    - **Nota Global de Versionamento:** Inserida nota destacada na Seção 2.1 determinando que o número de cada migração seja conferido contra o número real da versão atual do banco no momento da execução, sendo a ordem sequencial estrita de execução das tarefas de banco: T-08, depois T-28B, depois T-18B.

