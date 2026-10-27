# Relatório de Execução da Tarefa T-28B [DAT-03]
## Sincronização de Metas por Conta na Nuvem, Room v14 e Desacoplamento de Tema

---

### 1. Resultado

🟡 **IMPLEMENTADO, NÃO VERIFICADO NO APARELHO**  
O código de produção foi totalmente implementado com persistência Room na versão 14 (`14.json`), migração sequencial não destrutiva `MIGRATION_13_14`, modelo e DTO de sincronização com resolução Last-Write-Wins (LWW), API REST Supabase, desacoplamento seguro da preferência de tema em `app_preferences`, suporte a entrada numérica com vírgula padrão brasileiro e testes unitários e de integração 100% aprovados.  
O status permanece como 🟡 pois a execução do script DDL da tabela `user_goals` no PostgreSQL remoto depende de intervenção do usuário no console web do Supabase, e a validação do comportamento em dispositivo móvel sob condições de uso real exige homologação manual no aparelho físico.

---

### 2. O que mudou

**Explicação em linguagem simples (para o dono do produto):**
Antes desta atualização, as metas de trabalho (quanto o entregador/motorista quer faturar, lucrar, rodar e quantas entregas pretende fazer no dia, na semana e no mês) ficavam salvas em um arquivo temporário de preferências do aparelho (`giro_custo_goals`). Se o motorista trocasse de celular, perdesse o aparelho ou trocasse de conta, essas metas não eram sincronizadas com a nuvem. Além disso, a opção de Tema Escuro ou Claro ficava misturada no mesmo arquivo de metas, fazendo com que o tema visual fosse alterado ou resetado se o usuário limpasse as metas.

Com a Tarefa T-28B:
1. **Metas no Banco de Dados Local (Room v14):** As metas agora são salvas como uma tabela oficial do banco de dados local (`user_goals`), com chaves, índices de busca rápida, controle de data de modificação e status de sincronização (`PENDING_UPDATE`, `SYNCED`).
2. **Sincronização com o Supabase:** Criamos a comunicação que envia as metas locais para o servidor do Supabase e baixa as metas mais recentes salvas na nuvem. Em caso de alteração em dois aparelhos diferentes, o aplicativo adota a regra Last-Write-Wins (a alteração com data e hora mais recente prevalece), preservando com segurança dados inseridos offline.
3. **Isolamento por Conta:** Cada usuário logado possui suas próprias metas exclusivas vinculadas ao seu identificador (`user_id`). Ao sair da conta ou trocar de usuário, as metas da conta anterior são zeradas do aparelho para proteger a privacidade, e as metas da nova conta são baixadas da nuvem.
4. **Desacoplamento do Tema:** A escolha de Tema Escuro/Claro foi transferida para um arquivo dedicado de preferências gerais do aplicativo (`app_preferences`), garantindo que o tema visual nunca mais seja alterado ao limpar ou trocar de conta.
5. **Formatação Brasileira:** A tela do Painel (Dashboard) agora aceita valores digitados com vírgula (ex.: `250,00` ou `160,00`) e exibe feedback imediato via mensagem na parte inferior (Snackbar) ao salvar as metas.

**Lista exata de arquivos modificados e criados:**
- **Esquema de Banco de Dados:**
  - `app/schemas/com.example.data.GiroCustoDatabase/14.json` *(Novo arquivo: contrato de esquema oficial exportado do Room v14)*
  - `app/src/main/java/com/example/data/Entities.kt` *(Modificado: entidade `UserGoals` com chave primária singleton `id = 1`, 12 campos de metas, timestamps e enum `SyncStatus`)*
  - `app/src/main/java/com/example/data/Daos.kt` *(Modificado: interface `UserGoalsDao` com consultas reativas em Flow, upsert e limpeza)*
  - `app/src/main/java/com/example/data/GiroCustoDatabase.kt` *(Modificado: versão do banco elevada para 14, registro de `MIGRATION_13_14` e adição do DAO)*
  - `app/src/main/java/com/example/data/Repository.kt` *(Modificado: métodos de leitura, atualização, migração transparente de preferências legadas e verificação de metas pendentes)*
- **Comunicação em Rede e Sincronização Supabase:**
  - `app/src/main/java/com/example/network/supabase/SupabaseModels.kt` *(Modificado: DTO `UserGoalsDto` com anotações Moshi para serialização JSON)*
  - `app/src/main/java/com/example/network/supabase/SupabaseMappers.kt` *(Modificado: conversores bidirecionais entre `UserGoals` e `UserGoalsDto`)*
  - `app/src/main/java/com/example/network/supabase/SupabaseApi.kt` *(Modificado: endpoints REST `getUserGoals` e `upsertUserGoals`)*
  - `app/src/main/java/com/example/network/supabase/SupabaseSyncManager.kt` *(Modificado: rotinas de upload `pushUserGoals` e download `pullUserGoals` com reconciliação Last-Write-Wins e inclusão nas rotinas de sincronização completa)*
- **Interface com o Usuário e Recursos:**
  - `app/src/main/java/com/example/ui/GiroCustoViewModel.kt` *(Modificado: observação reativa de metas via Flow, método `updateGoals`, desacoplamento da preferência `is_dark_theme` para `app_preferences` e migração silenciosa)*
  - `app/src/main/java/com/example/ui/screens/DashboardScreen.kt` *(Modificado: parsing robusto de vírgula decimal pt-BR e Snackbar de confirmação ao salvar)*
  - `app/src/main/res/values/strings.xml` *(Modificado: recursos de texto centralizados para metas salvas e feedbacks visuais)*
- **Scripts de Banco de Dados Remoto:**
  - `supabase/migrations/20261026_user_goals.sql` *(Novo arquivo: DDL PostgreSQL com chave primária em `user_id`, RLS estrita por `auth.uid() = user_id`, índices e trigger de atualização)*
  - `supabase/README.md` *(Modificado: documentação da tabela `user_goals`, políticas de segurança e instruções de migração)*
- **Suíte de Testes Automatizados:**
  - `app/src/test/java/com/example/UserGoalsDatabaseAndSyncTest.kt` *(Novo arquivo: 10 testes unitários cobrindo Room v14, MigrationTestHelper, DAO, Repository e SyncManager)*
  - `app/src/test/java/com/example/UserGoalsSyncTest.kt` *(Novo arquivo: 5 testes de integração com MockWebServer e despacho HTTP realista)*
  - `app/src/test/java/com/example/UserGoalsViewModelAndUiTest.kt` *(Novo arquivo: 7 testes de ViewModel, preferências de tema e parsing pt-BR)*
  - `app/src/test/java/com/example/SupabaseSyncAndMigrationTest.kt` *(Modificado: adição de `MIGRATION_13_14` nas cadeias completas de teste e suporte a metas no `FakeSupabaseRestApi`)*
  - `app/src/test/java/com/example/VehicleAndFipeIntegrationTest.kt` *(Modificado: adição de `MIGRATION_13_14` no encadeamento de teste de banco)*
  - `app/src/test/java/com/example/AuthBiometricsAndViaCepTest.kt` *(Modificado: implementação de stubs no fake api de teste)*
  - `app/src/test/java/com/example/network/supabase/AccountIsolationAndSyncTest.kt` *(Modificado: implementação de stubs de metas no mock de contagem)*

---

### 3. Evidências

#### 3.1 Compilação do APK Minificado com R8 (`assembleReleaseCheck`)
Comando executado:
```bash
./gradlew assembleReleaseCheck
```
Últimas linhas da saída do terminal:
```text
> Task :app:compileReleaseCheckJavaWithJavac UP-TO-DATE
> Task :app:mergeReleaseCheckGeneratedProguardFiles
> Task :app:generateReleaseCheckLintVitalReportModel
> Task :app:mergeReleaseCheckJavaResource
> Task :app:expandReleaseCheckArtProfileWildcards
> Task :app:lintVitalAnalyzeReleaseCheck
> Task :app:minifyReleaseCheckWithR8
> Task :app:lintVitalReportReleaseCheck
> Task :app:lintVitalReleaseCheck
> Task :app:compileReleaseCheckArtProfile
> Task :app:packageReleaseCheck
> Task :app:assembleReleaseCheck
> Task :app:createReleaseCheckApkListingFileRedirect UP-TO-DATE

BUILD SUCCESSFUL in 8m 44s
50 actionable tasks: 19 executed, 4 from cache, 27 up-to-date
Configuration cache entry reused.
```
*Evidência:* As tarefas `:app:minifyReleaseCheckWithR8` e `:app:assembleReleaseCheck` foram concluídas com sucesso. O R8 processou com precisão as classes `UserGoals`, `UserGoalsDto`, Moshi e Room sem avisos impeditivos e sem quebra de regras.

---

#### 3.2 Execução Completa da Suíte de Testes Unitários (`testDebugUnitTest`)
Comando executado (execução fresca forçada):
```bash
./gradlew testDebugUnitTest --rerun-tasks
```
Contagem real de testes:
- **Executados:** 250
- **Aprovados:** 250
- **Falhas:** 0
- **Ignorados / Pulados:** 0
- **Taxa de Sucesso:** 100%
- **Tempo de Execução:** 41.551s

Caminho do relatório HTML detalhado:
`app/build/reports/tests/testDebugUnitTest/index.html`

Últimas linhas da saída do terminal:
```text
> Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
> Task :app:processDebugUnitTestJavaRes
> Task :app:testDebugUnitTest
> Task :app:finalizeTestRoborazziDebug SKIPPED

BUILD SUCCESSFUL in 4m 18s
33 actionable tasks: 33 executed
Configuration cache entry reused.
```

---

#### 3.3 Verificação de Lint do Android (`lintDebug`)
Comando executado:
```bash
./gradlew lintDebug
```
Últimas linhas da saída do terminal:
```text
> Task :app:lintReportDebug UP-TO-DATE
> Task :app:lintDebug

BUILD SUCCESSFUL in 2s
31 actionable tasks: 1 executed, 30 up-to-date
Configuration cache entry reused.
```

---

#### 3.4 Varreduras Obrigatórias de Integridade Pré-Commit

1. **Varredura de Stubs, Mocks e Pendências em Código de Produção:**
   ```bash
   grep -rniE "mock|stub|fake|homologa|TODO|FIXME|em breve" app/src/main/java
   ```
   *Resultado e Justificativa:*
   - `GiroCustoMainApp.kt:1`: Comentário legado remanescente sobre implementação de gaveta lateral (`// TODO: Implementar menu lateral...`). Não há código stub nem funcionalidade falsa.
   - `SmartphoneSimulator.kt:44`: Comentário técnico explicativo de componente de visualização em telas largas (`// Desenha um mock realista de smartphone centralizado na tela`). Não se trata de mock de testes nem funcionalidade simulada em produção.
   - Ocorrências contendo a sequência `todo` em `LaunchScreen.kt`, `FuelRefillsScreen.kt` e `GiroCustoViewModel.kt` decorrem de substrings case-insensitive de nomes de variáveis legítimas como `startOdo`, `startOdometer` e da função padrão Kotlin `toDoubleOrNull()`. Nenhuma função falsa ou stub foi introduzida.

2. **Varredura de Testes Desativados ou com Asserções Triviais:**
   ```bash
   grep -rnE "@Ignore|assertTrue\(true\)|assertEquals\(([0-9]+), *\1\)" app/src/test app/src/androidTest
   ```
   *Resultado:* Retorno código 1 (saída completamente vazia). Nenhum teste desativado, nenhum teste ignorado e nenhuma asserção enfraquecida.

3. **Varredura de Segredos e Chaves no Repositório:**
   ```bash
   grep -rniE "(api[_-]?key|secret|password|service_role|BEGIN (RSA|PRIVATE))" --include=*.kt --include=*.kts --include=*.xml --include=*.json --include=*.properties . | grep -v ".env.example"
   ```
   *Resultado:* Apenas referências estruturais de APIs oficiais do Android (`javax.crypto.SecretKey`, `KeyboardType.Password`, `PasswordVisualTransformation`), cabeçalhos ofuscados no cliente (`redactHeader("apikey")`) e o recurso textual de validação de interface em `strings.xml` (`auth_error_weak_password`). Nenhuma chave privada, token ou senha exposta.

4. **Varredura de Privacidade e Mascaramento na Pasta `docs/`:**
   - E-mails não mascarados: `grep -rnE "[A-Za-z0-9._%+-]{2,}@[A-Za-z0-9.-]+\.[A-Za-z]{2,}" docs | grep -v "\*\*\*"` -> **0 ocorrências**.
   - Telefones reais: `grep -rnE "\b[0-9]{8,11}\b" docs` -> Apenas exemplos de CEPs públicos (`01310100`, `99999999`) e valores decimais históricos.
   - Senhas reais: `grep -rn -iE "(senha|password)" docs` -> Apenas termos conceituais em relatórios anteriores e descrições de rotinas de autenticação.
   - Tokens JWT: `grep -rn "eyJ" docs` -> Apenas citações da expressão do comando de varredura em relatórios históricos.

---

#### 3.5 Registro da Verificação por Mutação Manual (Regra 4)
Conforme preconizado em `.agents/rules/integridade-de-testes.md`, o Agente de QA submeteu a lógica de sincronização a mutação proposital no código de produção para assegurar a efetividade dos testes:

- **Arquivo e Linha Modificados:** `app/src/main/java/com/example/network/supabase/SupabaseSyncManager.kt`, linha 426.
- **Quebra Injetada:** A condição de reconciliação Last-Write-Wins (LWW) do download de metas:
  ```kotlin
  // Código original:
  if (existing == null || existing.syncStatus == SyncStatus.SYNCED || remoteUpdated >= existing.updatedAt) {
      repository.saveGoalsFromRemote(remote)
  }
  // Mutação aplicada:
  if (false) {
      repository.saveGoalsFromRemote(remote)
  }
  ```
- **Execução do Teste Sob Mutação:**
  Comando: `./gradlew testDebugUnitTest --tests "com.example.UserGoalsSyncTest"`
- **Detecção Comprovada:** A suíte acusou imediatamente 2 falhas pontuais:
  - `org.junit.ComparisonFailure at Assert.java:117` no teste `testPullUserGoals_remoteNewerLWW_replacesLocalAndUpdatesRoom`.
  - `java.lang.AssertionError` no teste `testAccountSwitch_confirmDiscardData_clearsUserAGoals_preservesTheme_andPullsUserBGoals`.
- **Restauração e Revalidação:** A quebra foi revertida, retornando a condição correta de reconciliação temporal. Os testes foram re-executados e atingiram 100% de aprovação.

---

### 4. Testes

#### 4.1 Testes Novos Adicionados (Total: 22 testes)

**Em `app/src/test/java/com/example/UserGoalsDatabaseAndSyncTest.kt` (10 testes):**
1. `testMigration13To14_createsUserGoalsTableAndIndexesWithMigrationTestHelper`: Valida via `MigrationTestHelper` a criação da tabela `user_goals` com todas as 15 colunas, tipos SQLite e índice único em `(id)`.
2. `testUserGoalsDao_singletonOperationsAndFlow`: Testa inserção inicial, leitura contínua reativa via `Flow` e atualização atômica do registro singleton no Room.
3. `testRepository_goalsMethodsAndPendingChecks`: Testa métodos `getUserGoalsFlow`, `updateUserGoals`, `hasPendingGoals` e transição de status para `PENDING_UPDATE`.
4. `testRepository_migrateLegacyGoalsIfNeeded_withLegacyCustomGoals`: Valida a leitura de metas personalizadas pré-existentes em `giro_custo_goals` e a migração transparente para o Room v14.
5. `testRepository_migrateLegacyGoalsIfNeeded_freshInstallDefaults`: Valida a inicialização automática com metas socioeconômicas padrão do entregador em instalações novas (sem preferências legadas).
6. `testRepository_migrateLegacyGoalsIfNeeded_idempotentWhenAlreadyPresent`: Garante idempotência: se o Room já contiver registro, a migração não sobrescreve os dados.
7. `testSyncManager_pushLocalGoals_whenPendingUpdate`: Valida envio ao Supabase de metas com status `PENDING_UPDATE` e transição para `SYNCED`.
8. `testSyncManager_pullRemoteGoals_remoteNewerLWW`: Valida que metas com `updated_at` remoto superior sobrescrevem metas locais sincronizadas.
9. `testSyncManager_pullRemoteGoals_localNewerOfflineLWW_preservesLocal`: Valida que metas editadas offline mais recentes que o servidor são preservadas localmente e continuam pendentes de envio.
10. `testSyncManager_hasAnyLocalData_failsafeBlocksPushIfUnowned`: Valida que a existência de metas locais é contabilizada na checagem de dados do aparelho, protegendo contra envio não autorizado se o dono local não corresponder ao usuário logado.

**Em `app/src/test/java/com/example/UserGoalsSyncTest.kt` (5 testes):**
11. `testPushUserGoals_pendingSerializedToJson_postRequestSent_roomTransitionsToSynced`: Teste de contrato com MockWebServer validando formato JSON enviado, cabeçalhos de autorização e atualização do Room para `SYNCED`.
12. `testPullUserGoals_remoteNewerLWW_replacesLocalAndUpdatesRoom`: Valida download via MockWebServer e reconciliação temporal LWW no banco local.
13. `testPullUserGoals_localNewerOfflineLWW_preservesLocalAndPendingStatus`: Valida preservação de dados locais offline durante resposta do servidor.
14. `testPullUserGoals_emptyRemoteArray_preservesLocalGoalsIntact`: Garante que retorno vazio do servidor (usuário sem metas na nuvem) não apaga as metas locais.
15. `testAccountSwitch_confirmDiscardData_clearsUserAGoals_preservesTheme_andPullsUserBGoals`: Cenário completo de ponta a ponta: troca da Conta A para a Conta B, confirmação de descarte de dados, limpeza das metas da Conta A, preservação rigorosa do tema escuro/claro e carregamento das metas da Conta B via rede.

**Em `app/src/test/java/com/example/UserGoalsViewModelAndUiTest.kt` (7 testes):**
16. `testInitialDarkTheme_readsFromDedicatedPreferences`: Valida que a inicialização do ViewModel consulta o arquivo desacoplado `app_preferences`.
17. `testInitialDarkTheme_fallsBackToLegacyGoals_andMigratesToAppPrefs`: Valida a migração retrocompatível do tema salvo anteriormente em `giro_custo_goals` para `app_preferences`.
18. `testToggleTheme_persistsToDedicatedPreferences`: Valida a alternância de tema e sua persistência direta em `app_preferences`.
19. `testGoalsCollection_updatesStateFlowsFromDatabaseFlow`: Valida que alterações salvas no banco Room propagam automaticamente para os `StateFlow` observados pela UI.
20. `testUpdateGoals_updatesStateFlowsImmediatelyAndPersistsToRoom`: Valida a gravação imediata na UI e disparo de persistência assíncrona ao invocar `updateGoals`.
21. `testPtBrCommaDecimalParsing_handlesCommasAndDefaults`: Valida a conversão correta de strings contendo vírgula (`"250,00"`) para valores de ponto flutuante válidos (`250.0`), prevenindo erros de digitação e garantindo fallback seguro para campos em branco.
22. `testResetAccountSpecificPreferences_preservesTheme`: Valida que a rotina de limpeza de preferências por troca de conta apaga o arquivo `giro_custo_goals` mas mantém intocado o `app_preferences` com o tema do motorista.

#### 4.2 Testes Existentes Adaptados
- `app/src/test/java/com/example/SupabaseSyncAndMigrationTest.kt`: Encadeamento da migração `MIGRATION_13_14` nos testes de banco e implementação dos métodos de metas na classe utilitária `FakeSupabaseRestApi`.
- `app/src/test/java/com/example/VehicleAndFipeIntegrationTest.kt`: Atualização da versão do banco para 14 incluindo `MIGRATION_13_14`.
- `app/src/test/java/com/example/AuthBiometricsAndViaCepTest.kt`: Implementação dos métodos de metas no mock anônimo do Supabase.
- `app/src/test/java/com/example/network/supabase/AccountIsolationAndSyncTest.kt`: Inclusão dos métodos de metas na classe utilitária `CountingSupabaseRestApi`.

*Nenhum teste existente foi removido, ignorado ou enfraquecido.*

---

### 5. O que NÃO foi feito ou está simulado

- **Script SQL Remoto no Supabase:** O script de criação da tabela [`supabase/migrations/20261026_user_goals.sql`](../supabase/migrations/20261026_user_goals.sql) está redigido, documentado e validado, mas **NÃO foi executado automaticamente no banco de dados de produção do Supabase**. A execução precisa ser feita manualmente pelo dono do projeto via console web do Supabase (passo a passo detalhado na Seção 7).
- **Validação Física no Aparelho Celular:** Os fluxos visuais e de rede foram validados em ambiente de testes unitários e com servidor local MockWebServer; contudo, a prova real no aparelho celular físico (sob luz solar, com toques reais e conexão de dados móveis) ainda deve ser realizada pelo usuário final.
- **Simulação em Código:** **NADA está simulado em código de produção.** Todas as classes, DAOs, chamadas de rede e conversores operam com código definitivo e pronto para uso real.

---

### 6. Riscos e dívidas encontrados

1. **Representação Monetária em `Double` nas Metas:**
   - Em conformidade com o diagnóstico formal da Tarefa T-28A e com o padrão atual das demais entidades do Room (como `DailyRecord`), os campos financeiros de metas (`dailyGoalGross`, `dailyGoalNet`, etc.) foram implementados temporariamente como `Double`.
   - *Mitigação planejada:* A refatoração completa de todos os valores monetários do app para inteiros em centavos (`Long` ou `BigDecimal`) está centralizada e isolada na futura **Tarefa T-18B [DAT-01]** (Onda 3), na migração de banco v15 (`15.json`), evitando alterações precipitadas fora de escopo.
2. **Cardinalidade 1:1 por Conta no PostgreSQL:**
   - No PostgreSQL do Supabase, definimos `user_id UUID NOT NULL UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE`. Isso garante que cada entregador tenha estritamente uma única linha de metas. Caso uma tentativa de inserção ocorra para o mesmo usuário, a API REST utiliza o recurso de `upsert` com resolução de conflito em `user_id`.

---

### 7. O que o usuário precisa fazer

#### Parte A: Executar o Script SQL no Painel Web do Supabase (Passo a Passo Completo Clique a Clique)

Para criar a nova tabela `user_goals` com segurança e isolamento por usuário no Supabase:

1. Abra o navegador no seu computador ou celular e acesse o painel oficial: [https://supabase.com/dashboard](https://supabase.com/dashboard).
2. Faça login com sua conta do Supabase e clique no projeto **GiroCusto**.
3. Na barra de navegação lateral esquerda, clique no ícone do **SQL Editor** (ícone com o símbolo `>_` ou terminal de código).
4. No topo da página do SQL Editor, clique no botão verde **"+ New query"** (ou "Create new query").
5. Abra no seu computador ou editor o arquivo [`supabase/migrations/20261026_user_goals.sql`](../supabase/migrations/20261026_user_goals.sql) do projeto, selecione **todo o conteúdo** e copie (Ctrl+C).
6. Volte ao navegador e cole o texto dentro do campo de texto do SQL Editor (Ctrl+V).
7. Clique no botão verde **"Run"** no canto inferior direito da caixa de consulta (ou use o atalho de teclado `Ctrl + Enter`).
8. Na aba inferior "Results", certifique-se de que apareceu a mensagem:  
   `Success. No rows returned`.
9. *(Opcional de conferência)*: No menu lateral esquerdo, clique em **Table Editor** e confirme que a tabela **`user_goals`** aparece na lista com um ícone de cadeado verde indicando que o RLS (*Row Level Security*) está ativado.

---

#### Parte B: Roteiro de Homologação no Aparelho Celular Físico

Após a compilação e instalação do novo APK no aparelho celular, siga os 3 roteiros abaixo para homologar a entrega:

##### Roteiro 1: Persistência Local com Vírgula e Modo Avião
1. Abra o GiroCusto no seu celular.
2. Na tela inicial (Painel / Dashboard), role até a seção **"Metas de Trabalho"** e toque no botão **"Editar Metas"** (ícone de lápis).
3. No diálogo que se abrirá, digite valores usando a vírgula do teclado brasileiro:
   - Meta Bruta Diária: `250,00`
   - Meta Líquida Diária: `160,00`
   - Meta Km Diária: `100,0`
   - Entregas Diárias: `15`
4. Toque no botão **"Salvar Metas"**.
5. **Verificação imediata:** Observe que na parte inferior da tela surge a mensagem: *"Metas atualizadas com sucesso!"* e os cartões do painel passam a exibir imediatamente os novos valores calculados.
6. Ative o **Modo Avião** no celular (desconectando Wi-Fi e dados móveis).
7. Feche completamente o aplicativo (abra a tela de aplicativos recentes do Android e deslize o GiroCusto para encerrar o processo).
8. Reabra o GiroCusto com o Modo Avião ainda ligado.
9. **Resultado esperado:** O aplicativo abre sem nenhum travamento e as metas salvas (`R$ 250,00` / `R$ 160,00`) continuam visíveis e intactas no painel, comprovando que o banco Room v14 gravou os dados com sucesso no aparelho.

##### Roteiro 2: Sincronização com a Nuvem
1. Desative o Modo Avião no celular e aguarde a reconexão à internet.
2. Se ainda não estiver logado, acesse o menu de **Sincronização em Nuvem** e faça login com sua conta de teste.
3. Se já estiver logado, toque no botão **"Sincronizar Agora"** (ou faça uma pequena edição de metas).
4. Acesse o painel web do Supabase no computador, vá em **Table Editor -> `user_goals`**.
5. **Resultado esperado:** Uma linha aparecerá na tabela contendo o seu `user_id` e os valores exatos de metas que você preencheu no celular.

##### Roteiro 3: Troca de Conta e Preservação de Tema
1. No aplicativo, com o Tema Escuro ativado (ou alterne o tema para testar), entre na tela de **Sincronização em Nuvem**.
2. Toque no botão **"Sair da Conta"**.
3. Na janela de diálogo que pergunta sobre os dados locais, confirme o descarte para prosseguir com a troca de conta.
4. **Resultado esperado:** O visual do aplicativo (Tema Claro ou Escuro) **NÃO se altera**, permanecendo rigorosamente no tema que você escolheu. As metas exibidas no painel retornam para o valor padrão do aplicativo e, se você fizer login com uma segunda conta de teste, as metas dessa segunda conta serão puxadas da nuvem automaticamente.

---

### 8. Commit e sincronização

- **Mensagem do commit:** `feat(metas): sincronização de metas na nuvem, Room v14 e desacoplamento de tema (T-28B)`
- **Hash do commit:** `7af3a40`
- **Status do push:** Sincronizado com sucesso com `origin/main` (`c4d9078..7af3a40`).
