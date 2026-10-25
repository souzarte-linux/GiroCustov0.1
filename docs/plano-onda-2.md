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

A Onda 1 teve como meta primordial a blindagem imediata contra riscos críticos de segurança, proteção de credenciais, correção de usabilidade crítica de tela e ativação de minificação R8. Todas as tarefas planejadas foram concluídas no código e validadas por testes unitários automatizados com 100% de sucesso.

| ID Tarefa | Achado Auditado | Risco | Status Oficial | Hash Commit | Mensagem do Commit | Verificação / Observações |
| :--- | :---: | :---: | :---: | :---: | :--- | :--- |
| **T-01** | SEC-08 | Baixo | ✅ **VERIFICADO** | `69c9a71` | `Segurança: adiciona padrões de certificados e credenciais ao .gitignore (T-01)` | Blindagem do `.gitignore` contra chaves, keystores e credenciais; verificado com `git check-ignore`. |
| **T-02** | SEC-02 | Alto | ✅ **VERIFICADO** | `077bd66` | `Segurança: exclui preferências de autenticação e biometria do backup Android (T-02)` | Exclusão cirúrgica de tokens e biometria do backup em nuvem e USB; Room preservado. |
| **T-03** | UI-01 | Crítico | 🟡 **IMPLEMENTADO, NÃO VERIFICADO NO APARELHO** | `11cf935` | `UI: remove moldura de SmartphoneSimulator da produção em GiroCustoMainApp (T-03)` | Removida moldura restritiva de 390dp da produção; pendente prova manual do usuário em tablet ou rotação paisagem. |
| **T-04** | INT-01 + SEC-03 | Alto | ✅ **VERIFICADO** | `fa60196` | `Segurança: oculta login por SMS e higieniza logs HTTP com Level.BASIC (T-04)` | Testado no aparelho real com Logcat (`Level.BASIC`, redação de `Authorization`/`apikey`); stub de SMS com rótulo "Em breve". |
| **T-05** | UX-01 | Alto | ✅ **VERIFICADO** | `b2714f0` | `UX: adiciona diálogo de confirmação e desfazer com proteção LWW ao excluir veículos e peças (T-05)` | Testado e homologado no aparelho físico pelo usuário; diálogo de confirmação, snackbar com Desfazer e proteção LWW. |
| **T-06** | SEC-01 + SEC-04 | Alto | ✅ **VERIFICADO** | `8d2641d`<br>`abfdda2` | `Segurança: implementa sessão segura em repouso e biometria com Keystore AES-GCM (T-06)`<br>`Fix(Segurança): adiciona switch de biometria no perfil e força reautenticação no fallback (T-06)` | Sessão segura em repouso com Keystore AES-GCM, CryptoObject real em BiometricPrompt, switch no perfil e contingência segura testados no aparelho. |
| **T-06b** | SEC-01 (Conformidade) | Alto | ✅ **VERIFICADO** | `86aa696` | `Segurança: remove fallback em memória e implementa conformidade estrita de sessão segura (T-06b)` | Remoção de fallback de chaves em RAM na produção; contrato `SessionCryptoEngine` implementado pelo Keystore e fakes restritos a `src/test`. |
| **T-06c** | SEC-01 (Isolamento) | Alto | 🟡 **IMPLEMENTADO, NÃO VERIFICADO NO APARELHO** | `e1c0742` | `feat(sync): isolamento estrito de dados entre contas no mesmo dispositivo (T-06c)` | Implementação do `DataOwnerStorage`, travas PUSH/PULL no `SupabaseSyncManager` e decisão central no ViewModel; 182 testes unitários aprovados; aguarda teste manual no aparelho. |
| **T-06d** | Integridade Nuvem | Alto | ✅ **VERIFICADO** | `bc7453b` | `docs: salvar backups CSV pre-limpeza da base Supabase` | Exportação de backups CSV com mascaramento estrito de dados pessoais (`user_profile.csv`) e limpeza de registros inconsistentes no Supabase. |
| **T-07** | SEC-05 | Alto | 🟡 **IMPLEMENTADO, NÃO VERIFICADO NO APARELHO** | `129cd7a` | `build(release): ativa R8 em release, cria buildType releaseCheck e define regras ProGuard (T-07)` | R8 ativado em release; criado buildType `releaseCheck`; ProGuard configurado; SHA-1 conferido; aguarda instalação e homologação do APK no celular. |

---

## 2. Reavaliação das Tarefas Restantes da Onda 2 e Onda 3 (Plano v2.1)

### 2.1 O que mudou e o impacto nas tarefas futuras

1. **Adição do `DataOwnerStorage` e Decisão Central de Troca de Conta (T-06c):**
   - Foi criada a camada `DataOwnerStorage` persistida em `giro_custo_data_owner.xml` com gravação síncrona imediata (`commit()`).
   - O `SupabaseSyncManager` recebeu travas defensivas atômicas antes do PUSH e do PULL para barrar sincronizações se `ownerId != userId`.
   - O `GiroCustoViewModel` passou a gerenciar estados de conflito de conta (`AccountConflictState`), diálogos de confirmação de descarte de dados e reivindicação de propriedade local.
   - O `GiroCustoRepository` recebeu métodos atômicos de contagem de pendências (`countPendingChanges`), verificação de dados locais (`hasAnyLocalData`) e limpeza de tabelas (`clearAllLocalData`).
   - **Impacto direto:** 
     - A tarefa **T-10 (Exclusão de Conta)** agora precisa interagir compulsoriamente com o `DataOwnerStorage` e a limpeza de tabelas locais para não deixar o aparelho bloqueado após a exclusão da conta no servidor.
     - A tarefa **T-08 (Índices Room e Migration v13)** precisa considerar a interação entre as operações de limpeza local (`clearAllLocalData`) e o desempenho de deleção em cascata/reconciliação.

2. **Ativação do R8 e Criação do buildType `releaseCheck` (T-07):**
   - O compilador R8 agora ofusca e otimiza compilações de release.
   - Todas as alterações estruturais que envolvam reflection, Room, Moshi, Retrofit ou injeção de dependência precisam obrigatoriamente ser validadas não apenas com `./gradlew testDebugUnitTest lintDebug`, mas também com `./gradlew assembleReleaseCheck`.
   - As regras atuais em `proguard-rules.pro` incluíram regras `-keep class com.example.data.** { *; }`, `-keep class com.example.network.** { *; }` e `-keep class com.example.util.** { *; }`, que são excessivamente permissivas e anulam parte dos ganhos do R8 (objeto da nova tarefa T-29).

3. **Crescimento do `GiroCustoViewModel.kt`:**
   - Com as regras de biometria, fallback seguro, mitigação de concorrência e máquina de estados de troca de conta, o ViewModel alcançou **1.572 linhas**. A tarefa de decomposição estrutural (**T-20**) ganhou urgência arquitetural aumentada para a Onda 3.

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

#### Tarefa T-10 [SEC-06]: Portabilidade de Dados e Exclusão Segura de Conta (LGPD) com Limpeza de Dono
```json
{
  "task_id": "T-10",
  "to_agent": "seguranca",
  "task_type": "implementacao",
  "risk": "alto",
  "title": "Implementar exportação de dados via SAF e exclusão definitiva de conta no Supabase com expurgo do DataOwnerStorage e Room local",
  "context": "O app não atende aos requisitos de exclusão e portabilidade da LGPD. Além de deletar o usuário na nuvem por RPC SECURITY DEFINER sem usar chave service_role no cliente, o processo deve compulsoriamente expurgar os dados locais, limpar o DataOwnerStorage e revogar sessões locais para evitar bloqueios em logins posteriores no mesmo aparelho.",
  "root_cause_evidence": "Ausência de rotinas de exportação e exclusão de conta em GiroCustoViewModel.kt e ProfileScreen.kt",
  "acceptance_criteria": [
    "(a) Criar migração SQL remota em supabase/migrations com a RPC delete_user_account() configurada como SECURITY DEFINER, com search_path = public fixo, permitida exclusivamente ao papel authenticated (REVOKE ALL FROM anon, public; GRANT EXECUTE TO authenticated;)",
    "(b) A RPC deve garantir a exclusão em cascata de todas as 7 tabelas do usuário antes de expurgar a conta em auth.users",
    "(c) Exportação de dados locais realizada via SAF (CreateDocument) ou ShareSheet nativo em formato JSON/CSV, SEM exigir permissão intrusiva de armazenamento",
    "(d) Ao confirmar a exclusão da conta no app: (1) acionar a RPC remota; (2) executar repository.clearAllLocalData(); (3) executar dataOwnerStorage.clearDataOwner(); (4) executar supabaseClient.signOut() e clearSavedSession(); (5) executar BiometricHelper.clearBiometrics(); (6) resetar preferências locais e transitar para RootNavState.Unauthenticated",
    "(e) Criar teste automatizado validando o expurgo completo dos dados locais e a limpeza do DataOwnerStorage após sucesso na exclusão",
    "(f) Script SQL entregue com documentação detalhada em supabase/README.md. Status da etapa de servidor até execução pelo usuário no painel: ⛔ BLOQUEADO",
    "Entregar roteiro de teste manual no aparelho, passo a passo completo, para o usuário executar (sem confirmação no aparelho, status máximo 🟡)"
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
  "depends_on": ["T-06c"]
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

#### Tarefa T-14 [ARQ-01]: Remoção de Ganchos de Teste em Produção com Asserções Idênticas
```json
{
  "task_id": "T-14",
  "to_agent": "arquiteto",
  "task_type": "implementacao",
  "risk": "alto",
  "title": "Remover ganchos públicos 'test...', resetForTesting e reflexão de classes de teste dos singletons de produção",
  "context": "GoogleAuthHelper (testAuthHandler), BiometricHelper (testHardwareAvailable), SupabaseClient (resetForTesting, testStorage e reflexão de InMemorySessionStorage), FipeNetworkClient, ViaCepNetworkClient e GiroCustoDatabase contêm variáveis e métodos públicos que violam a separação estrita entre código de produção e ambiente de testes.",
  "root_cause_evidence": "GoogleAuthHelper.kt:37, BiometricHelper.kt:27, SupabaseClient.kt:253, 289-298, FipeNetworkClient.kt:41, ViaCepNetworkClient.kt:37 e GiroCustoDatabase.kt:203",
  "acceptance_criteria": [
    "Remover todas as variáveis públicas 'test...' dos objetos de produção",
    "Remover SupabaseClient.resetForTesting e a tentativa de instanciar InMemorySessionStorage via Class.forName / reflexão no código de produção",
    "Remover GoogleAuthHelper.testAuthHandler de produção, provendo ponto de injeção limpo via construtor ou visibilidade restrita a testes",
    "Substituir acoplamentos de teste por injeção de dependência via construtores ou factories limpas",
    "As asserções de TODOS os testes unitários existentes permanecem RIGOROSAMENTE IDÊNTICAS linha por linha (somente a inicialização dos objetos nos testes é adaptada)",
    "O Revisor Independente deve conferir o diff de teste linha por linha comprovando que nenhuma asserção foi enfraquecida ou removida",
    "Entregar roteiro manual no aparelho: testar login Google e biometria após a alteração (status máximo 🟡 sem teste no aparelho)"
  ],
  "files_expected": [
    "app/src/main/java/com/example/util/GoogleAuthHelper.kt",
    "app/src/main/java/com/example/util/BiometricHelper.kt",
    "app/src/main/java/com/example/network/supabase/SupabaseClient.kt",
    "app/src/main/java/com/example/network/fipe/FipeNetworkClient.kt",
    "app/src/main/java/com/example/network/viacep/ViaCepNetworkClient.kt",
    "app/src/main/java/com/example/data/GiroCustoDatabase.kt",
    "app/src/test/java/com/example/AuthBiometricsAndViaCepTest.kt",
    "app/src/test/java/com/example/FipeNetworkAndViewModelTest.kt",
    "app/src/test/java/com/example/SecureSessionAndCryptoTest.kt"
  ],
  "out_of_scope": ["Alteração da lógica das APIs externas"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug",
    "./gradlew assembleReleaseCheck"
  ],
  "required_reviewers": ["arquiteto", "revisor", "seguranca"],
  "depends_on": ["T-06b", "T-06c"]
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
  "title": "Implementar migração dos campos monetários para centavos (Long) com base no plano aprovado",
  "context": "Execução da migração planejada em T-18A para eliminar erros de ponto flutuante em valores monetários.",
  "root_cause_evidence": "Entities.kt:19, 41, 65, 69, 71, 74, 191, 194, 214",
  "acceptance_criteria": [
    "Modelo de centavos (Long) implementado nas entidades Room com utilitários de conversão segura (CurrencyUtils.kt)",
    "Migração Room MIGRATION_13_14 criada e testada com MigrationTestHelper",
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
    "app/schemas/com.example.data.GiroCustoDatabase/14.json"
  ],
  "out_of_scope": ["Conversão de odômetro e consumo médio (permanecem Float/Double)"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug",
    "./gradlew assembleReleaseCheck"
  ],
  "required_reviewers": ["arquiteto", "backend", "revisor"],
  "depends_on": ["T-18A", "T-08"]
}
```

#### Tarefa T-19 [DAT-02]: Versionamento de Schemas e Bloqueio de Migração Destrutiva
```json
{
  "task_id": "T-19",
  "to_agent": "arquiteto",
  "task_type": "diagnostico",
  "risk": "baixo",
  "title": "Formalizar integridade de schemas Room v13+ e documentar histórico de banco",
  "context": "Garantir que todo schema futuro seja versionado compulsoriamente e proibir em definitivo fallbackToDestructiveMigration.",
  "root_cause_evidence": "app/schemas/com.example.data.GiroCustoDatabase/ (ausência de 1, 4, 5, 6, 7 no Git)",
  "acceptance_criteria": [
    "Documentação em docs/database-schemas.md explicando a evolução histórica 1->13",
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
  "depends_on": ["T-08"]
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

- **Severidade:** 🟡 **Média** (UX e Clareza de Mensagens ao Motorista)
- **Responsável:** Agente Integrador / Frontend
- **Revisores:** Agente Revisor Independente e Agente Designer
- **Contexto:** Em `SupabaseClient.kt:155, 177, 206, 232`, as falhas da API Supabase Auth são capturadas diretamente como `response.errorBody()?.string()` e repassadas sem tratamento para o `GiroCustoViewModel.kt`. Quando ocorre um erro como e-mail não confirmado ou taxa de envio excedida, o banner da `AuthGateScreen.kt` exibe JSON cru em inglês na tela do motorista (ex.: `{"code":400,"error_code":"email_not_confirmed","msg":"Email not confirmed"}` ou `{"error":"over_email_send_rate_limit"}`).
- **Root Cause Evidence:** `SupabaseClient.kt:155, 177, 206, 232` e `GiroCustoViewModel.kt:479-482, 509-512, 534-537, 554-557`.

```json
{
  "task_id": "T-26",
  "to_agent": "integracoes",
  "task_type": "implementacao",
  "risk": "medio",
  "title": "Traduzir erros da API Supabase Auth para mensagens amigáveis em português",
  "context": "Falhas de login e cadastro exibem payloads JSON brutos em inglês no banner da tela AuthGateScreen. As respostas devem ser tratadas e convertidas para mensagens claras ao usuário brasileiro.",
  "root_cause_evidence": "SupabaseClient.kt:155, 177, 206, 232 e GiroCustoViewModel.kt:479-482, 509-512",
  "acceptance_criteria": [
    "Criar utilitário/mapper de erro de autenticação (AuthErrorMapper.kt) que analisa o código HTTP e o payload JSON da resposta",
    "Mapear com mensagens claras em português via stringResource:",
    "- email_not_confirmed / Email not confirmed -> 'Seu e-mail ainda não foi confirmado. Verifique sua caixa de entrada ou spam.'",
    "- invalid_credentials / Invalid login credentials -> 'E-mail ou senha incorretos. Verifique seus dados e tente novamente.'",
    "- over_email_send_rate_limit / email rate limit exceeded -> 'Muitas tentativas em pouco tempo. Aguarde alguns minutos antes de tentar novamente.'",
    "- user_already_exists / User already registered -> 'Já existe uma conta cadastrada com este e-mail. Faça login.'",
    "- weak_password -> 'A senha escolhida é muito fraca. Utilize ao menos 6 caracteres com números ou símbolos.'",
    "- Falhas de timeout ou conexão -> 'Sem conexão com a internet. Verifique seu sinal e tente novamente.'",
    "Caso o erro seja desconhecido, exibir mensagem genérica amigável em português sem vazar strings JSON cruas ou stack traces",
    "Mensagens de login não devem revelar se o e-mail específico existe no sistema (em conformidade com a seção 1.4 de padroes-seguranca-ux.md)",
    "Criar testes unitários específicos em AuthErrorMapperTest cobrindo cada payload de erro",
    "Entregar roteiro de teste manual no aparelho, passo a passo completo, para validação visual (status máximo 🟡 sem teste no aparelho)"
  ],
  "files_expected": [
    "app/src/main/java/com/example/network/supabase/AuthErrorMapper.kt",
    "app/src/main/java/com/example/network/supabase/SupabaseClient.kt",
    "app/src/main/java/com/example/ui/GiroCustoViewModel.kt",
    "app/src/main/res/values/strings.xml",
    "app/src/test/java/com/example/network/supabase/AuthErrorMapperTest.kt"
  ],
  "out_of_scope": ["Mudanças no layout visual de AuthGateScreen"],
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

### Tarefa Nova 3.c [Integrada à T-14 / ARQ-01]: Remoção de Ganchos de Teste em Produção com Asserções Idênticas

- **Severidade:** 🟠 **Alta** (Separação Rígida entre Código de Produção e Testes)
- **Responsável:** Agente Arquiteto
- **Revisores:** Agente Revisor Independente e Agente de Segurança
- **Contexto:** Durante as correções da T-06 e T-06b, foram introduzidos em `SupabaseClient.kt` os ganchos `@Volatile var testStorage: SecureSessionStorage? = null`, o método `resetForTesting` e o carregamento por reflexão dinâmica de `InMemorySessionStorage` via `Class.forName("com.example.network.supabase.InMemorySessionStorage")`. Adicionalmente, `GoogleAuthHelper.kt:37` mantém `var testAuthHandler`. Essas construções violam a Seção 1 (Item 5) de `integridade-de-testes.md`.
- **Decisão:** Este escopo foi **formalmente integrado aos critérios de aceite da Tarefa T-14 [ARQ-01]** (vide Seção 2.2), mantendo 100% das asserções dos testes existentes IDÊNTICAS linha por linha.

---

### Tarefa Nova 3.d [T-28 / ARQ-06]: Diagnóstico e Desenho de Persistência de Metas de Faturamento por Conta

- **Severidade:** 🟡 **Média** (Integridade de Dados e UX de Metas)
- **Responsável:** Agente Arquiteto / Backend
- **Revisores:** Agente Revisor Independente e Agente Designer
- **Contexto:** Em `GiroCustoViewModel.kt:926-941`, o método `resetAccountSpecificPreferences()` limpa o SharedPreferences `prefs.edit().clear()` ao alternar de conta ou no descarte de dados locais, resetando as metas diárias, semanais e mensais (`daily_goal_*`, `weekly_goal_*`, `monthly_goal_*`) para os valores estáticos padrão (R$ 200, R$ 130, 100 km, 15 entregas, etc.). Se o motorista A configurou metas personalizadas, e o motorista B loga no aparelho e depois o motorista A retorna, as metas de A foram permanentemente perdidas. É mandatório decidir e desenhar a arquitetura correta antes de codificar.
- **Root Cause Evidence:** `GiroCustoViewModel.kt:407-420, 922-944, 1141-1154`.

```json
{
  "task_id": "T-28",
  "to_agent": "arquiteto",
  "task_type": "diagnostico",
  "risk": "medio",
  "title": "Elaborar diagnóstico e especificação técnica para persistência e isolamento de metas por conta",
  "context": "Metas de faturamento são resetadas para os valores de fábrica ao trocar de conta. É necessário especificar a melhor estratégia de isolamento (SharedPreferences por userId vs. persistência no Room / Supabase user_profile) antes de implementar.",
  "root_cause_evidence": "GiroCustoViewModel.kt:407-420, 922-944",
  "acceptance_criteria": [
    "Entregar documento técnico em docs/design-metas-por-conta.md comparando três alternativas técnicas:",
    "  - Opção 1: SharedPreferences local isolado por conta (ex.: giro_custo_goals_<userId>.xml ou chaves prefixadas goal_<userId>_*)",
    "  - Opção 2: Persistência no banco Room na tabela user_profile com sincronização na nuvem Supabase",
    "  - Opção 3: Metas globais do dispositivo (não resetadas na troca de conta)",
    "Detalhar impacto na sincronização offline-first, backup e migração de banco",
    "Nenhuma alteração de código nesta etapa",
    "Submeter a proposta para escolha do proprietário do projeto"
  ],
  "files_expected": [
    "docs/design-metas-por-conta.md"
  ],
  "out_of_scope": ["Alterações de código ou migração de banco nesta fase"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
  ],
  "required_reviewers": ["arquiteto", "backend", "revisor"],
  "depends_on": ["T-06c"]
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
    "Entregar roteiro manual no aparelho executando os 5 fluxos críticos do app na versão releaseCheck minificada (login Google/email, diária no Room, busca no mapa, FIPE e sync) (status máximo 🟡 sem teste no aparelho)"
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
    "Tornar DataOwnerStorage dependência obrigatória e não-nula (remover valor default null no construtor primário)",
    "Trava Fail-Safe no PUSH e PULL: se ownerId == null mas o banco local possuir registros (hasAnyLocalData == true), lançar compulsoriamente DataOwnerMismatchException impedindo qualquer envio REST à nuvem",
    "Garantir alinhamento com a máquina de estados do ViewModel: quando o usuário optar por 'Estes dados são meus', o ViewModel define o dono no DataOwnerStorage ANTES de disparar o sync",
    "Atualizar testes unitários em AccountIsolationAndSyncTest e SupabaseSyncAndMigrationTest cobrindo o bloqueio fail-safe",
    "Nenhum teste existente quebrado"
  ],
  "files_expected": [
    "app/src/main/java/com/example/network/supabase/SupabaseSyncManager.kt",
    "app/src/main/java/com/example/data/Repository.kt",
    "app/src/test/java/com/example/network/supabase/AccountIsolationAndSyncTest.kt"
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

### Tarefa Nova 3.g [Integrada à T-10 / SEC-06]: Exclusão Segura com DataOwnerStorage e Limpeza Local

- **Severidade:** 🟠 **Alta** (Conformidade LGPD e Consistência Multi-conta)
- **Responsável:** Agente de Segurança / Backend
- **Revisores:** Agente Revisor Independente e Agente Arquiteto
- **Contexto:** Ao solicitar a exclusão definitiva da conta, o aplicativo não pode apenas chamar o servidor Supabase: se o `DataOwnerStorage` não for limpo e as tabelas locais não forem expurgadas, o aparelho permanecerá com o ID do usuário excluído registrado como dono, bloqueando qualquer sincronização ou novo login no mesmo aparelho.
- **Decisão:** Este requisito foi **formalmente incorporado aos critérios de aceite da Tarefa T-10 [SEC-06]** (vide Seção 2.2), abrangendo o ciclo completo de expurgo local (`clearAllLocalData`, `clearDataOwner`, `signOut`, `clearBiometrics` e reset de preferências).

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
| **1º** | **T-27** | Limpeza | Baixo | Frontend | - | Remove `signInWithPhone` e mensagem proibida de homologação no ViewModel. |
| **2º** | **T-30** | Segurança | Alto | Segurança | T-06c | Blindagem fail-safe de dono nulo no `SupabaseSyncManager`. |
| **3º** | **T-26** | UX/Rede | Médio | Integrações | - | Tradução amigável em pt-BR de erros da API Supabase Auth (elimina JSON cru). |
| **4º** | **T-14** | Arquitetura | Alto | Arquiteto | T-06b, T-06c | Remoção de ganchos `test...`, `resetForTesting` e reflexão mantendo asserções idênticas. |
| **5º** | **T-29** | DevOps/R8 | Alto | DevOps | T-07 | Aperto cirúrgico das regras de keep do ProGuard/R8 e teste em `releaseCheck`. |
| **6º** | **T-08** | Dados | Alto | Backend | - | Índices Room nas 7 tabelas e Migration v13 com `runMigrationsAndValidate`. |
| **7º** | **T-09** | Performance | Alto | Frontend | - | Chaves estáveis (`key`) em todas as LazyColumns. |
| **8º** | **T-10** | Segurança/LGPD | Alto | Segurança | T-06c, T-30 | Exclusão de conta via RPC Supabase com limpeza local e `DataOwnerStorage` (Status inicial: ⛔ BLOQUEADO para a parte SQL até o usuário rodar no painel). |
| **9º** | **T-11** | UX/Privacidade | Médio | Frontend | - | Diálogo explicativo de rationale prévia antes de solicitar permissão de GPS. |
| **10º** | **T-13** | Integração | Médio | Integrações | - | Alinhamento de timeouts do OkHttp para 25s na Overpass API. |
| **11º** | **T-15** | Acessibilidade | Alto | Acessibilidade | - | Inserção de `contentDescription` acessível em todos os botões e ícones clicáveis. |
| **12º** | **T-12** | UX/Design | Médio | Designer | T-03 | Padronização dos estados de Carregando, Vazio, Erro e Offline nas telas. |
| **13º** | **T-16** | Arquitetura/UI | Médio | Frontend | - | Elevação de cálculos e regras pesadas dos Composables para o ViewModel. |
| **14º** | **T-28** | Diagnóstico | Médio | Arquiteto | T-06c | Diagnóstico e proposta técnica para isolamento de metas de faturamento por conta. |
| **15º** | **T-17** | Testes | Médio | QA | - | Substituição de testes de modelo (2+2) por regras reais (PARADA OBRIGATÓRIA para autorização do usuário). |

---

### 4.3 Transição para a Onda 3 e Tratamento Especial de T-18 (Centavos)

A **Tarefa T-18** permanece estritamente dividida em duas etapas com controle de decisão:

1. **T-18A [DAT-01A] (Diagnóstico e Planejamento):**
   - Mapeamento completo e elaboração de `docs/plano-migracao-monetaria.md` sem alteração de nenhuma linha de código.
2. **PARADA OBRIGATÓRIA:**
   - O Orquestrador submete o plano monetário ao proprietário do projeto e aguarda sua aprovação expressa.
3. **T-18B [DAT-01B] (Implementação da Migração Monetária):**
   - Somente após a aprovação formal do usuário, a migração do Room (v14) e conversão para centavos (`Long`) é implementada.

As demais tarefas da Onda 3 seguem a ordem: **T-19** (schemas Room), **T-20** (decomposição de Profile, FuelRefills, History e ViewModel), **T-21** (strings Dashboard/Launch), **T-22** (remoção de libs de IA), **T-23** (limpeza de colors.xml), **T-24** (telas secundárias) e **T-25** (conclusão de strings).

---

## 5. Lista de Decisões que Dependem do Proprietário do Projeto

Abaixo estão listadas, de forma transparente e objetiva, todas as decisões e ações que dependem exclusivamente de você antes ou durante a execução:

### Decisão 1: Validação dos Testes Manuais Pendentes da Onda 1 no Smartphone Físico
- **Contexto:** Três tarefas da Onda 1 encontram-se com status 🟡 **IMPLEMENTADO, NÃO VERIFICADO NO APARELHO** aguardando validação manual no seu smartphone:
  - **T-03:** Rotação de tela em modo paisagem (confirmar ausência de bordas pretas de 390dp).
  - **T-06c:** Teste de troca de contas e trava de sincronização (após a limpeza da base Supabase realizada em T-06d).
  - **T-07:** Instalação do APK `app-releaseCheck.apk` por cima da versão existente e teste dos 5 fluxos principais (login Google/e-mail, diária, mapa, FIPE e sync).
- **Ação necessária:** Executar esses testes práticos para que as tarefas possam ser formalmente homologadas como ✅ **VERIFICADO**.

---

### Decisão 2: Definição do Modelo de Metas de Faturamento por Conta (Item 3.d / T-28)
- **Contexto:** Ao trocar de conta, as metas (`daily_goal_*`, `weekly_goal_*`, `monthly_goal_*`) voltam aos valores de fábrica padrão e apagam as preferências customizadas do motorista anterior.
- **Opções disponíveis para sua escolha:**
  - **Opção A (Recomendada para rapidez e simplicidade):** Salvar as metas associadas ao ID de cada conta no armazenamento local do aparelho (ex.: `goal_<userId>_*` em arquivo seguro). *Vantagem:* Rápido, não mexe na nuvem, funciona 100% offline e cada motorista mantém suas metas salvas no celular.
  - **Opção B:** Criar colunas de metas na tabela `user_profile` no Supabase e sincronizar via nuvem. *Vantagem:* As metas acompanham o motorista caso ele troque de celular. *Desvantagem:* Exige migration e script SQL na nuvem.
  - **Opção C:** Metas globais do aparelho (as metas não se resetam ao trocar de conta; o celular mantém as mesmas metas independentemente de quem estiver logado).

---

### Decisão 3: Execução do Script SQL da Exclusão de Conta no Supabase (T-10)
- **Contexto:** Para atender à LGPD sem embutir chaves mestras (`service_role`) no aplicativo, a exclusão da conta exige a criação de uma função SQL no Supabase (`delete_user_account()`).
- **Ação necessária:** O agente de Segurança criará o arquivo `supabase/migrations/20261026_user_deletion_rpc.sql` e a documentação passo a passo. Você precisará abrir o painel web do Supabase, colar o script no **SQL Editor** e clicar em **Run**. A tarefa permanecerá em ⛔ **BLOQUEADO** até sua confirmação de execução no servidor.

---

### Decisão 4: Autorização Explícita para Remoção dos Testes de Template (T-17)
- **Contexto:** A Seção 1 de `integridade-de-testes.md` proíbe remover qualquer teste sem autorização prévia por escrito no chat. Os testes herdados de templates de projeto (`assertEquals(4, 2 + 2)` e similares) precisam ser substituídos por testes úteis de regras de validação.
- **Ação necessária:** Quando a tarefa T-17 for iniciada, o agente fará uma parada mandatória no chat listando os testes afetados para que você responda autorizando a substituição.

---

### Decisão 5: Aprovação do Diagnóstico Monetário (T-18A) Antes da Implementação (T-18B)
- **Contexto:** A conversão de `Double` para centavos (`Long`) mexe nos cálculos financeiros de todo o aplicativo e no banco de dados Room.
- **Ação necessária:** O agente entregará em T-18A o documento completo de diagnóstico em `docs/plano-migracao-monetaria.md`. Você precisará ler e aprovar formalmente o plano antes de autorizar o início da codificação em T-18B.
