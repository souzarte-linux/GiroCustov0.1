# Desenho Técnico: Isolamento de Dados entre Contas no Mesmo Aparelho (T-06c)

**Data:** 24 de Outubro de 2026  
**Status:** Aprovado com Condições — Versão Consolidada para Implementação (Fase B)  
**Classificação de Risco:** Alto (Privacidade de Dados, Integridade Offline-First, LGPD)  
**Diretiva do Dono do Projeto:** Opção B (Guarda de Dono + Limpeza e Re-sincronização na Troca de Conta), sem alteração do formato das tabelas do Room e sem banco por usuário.

---

## 1. Diagnóstico do Caminho do Dado e Pontos Exatos de Mistura

### 1.1 Fluxo do Dado: Do Lançamento Local até a Nuvem
1. **Lançamento Local:** O motorista insere um registro na interface (ex.: diária em `LaunchScreen.kt`, abastecimento em `FuelRefillsScreen.kt`, veículo em `VehiclesScreen.kt` ou perfil em `ProfileScreen.kt`).
2. **Gravação no Room:** O ViewModel invoca o repositório (`GiroCustoRepository`), que grava a entidade via DAO no banco SQLite local (`giro_custo_database`) com `syncStatus = "PENDING_INSERT"` e `remoteId = ""`.
3. **Disparo de Sincronização:** O app aciona `SupabaseSyncManager.sync(userId)`.
4. **Envio à Nuvem (PUSH):** O `SupabaseSyncManager` busca as pendências no Room, converte cada entidade para DTO injetando o `userId` fornecido na chamada (`.toDto(userId)`), e faz upsert no Supabase via REST API.
5. **Recebimento da Nuvem (PULL):** O `SupabaseSyncManager` busca os registros remotos no Supabase onde `user_id = eq.$userId` e executa upsert diretamente nas tabelas locais do Room.

### 1.2 Pontos Exatos de Mistura no Código Real (Arquivo:Linha)

| Ponto de Falha | Arquivo e Linhas | Causa Técnica |
| :--- | :--- | :--- |
| **Ausência de Dono nas Entidades** | [`app/src/main/java/com/example/data/Entities.kt:13-221`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/data/Entities.kt) | As tabelas do Room (`vehicle`, `vehicle_parts`, `daily_records`, `fuel_refills`, `maintenance_records`, `platforms`, `user_profile`) não possuem a coluna `user_id`. Os registros locais são unitenant por aparelho físico. |
| **Apropriação Indevida no PUSH** | [`app/src/main/java/com/example/network/supabase/SupabaseSyncManager.kt:49-176`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/network/supabase/SupabaseSyncManager.kt#L49-L176) | `pushLocalChanges(userId)` lê os registros pendentes de todos os DAOs e chama `.toDto(userId)` (linhas 55, 79, 97, 114, 124, 144, 164), atribuindo cegamente os registros ao `userId` conectado no momento, mesmo que pertençam a outro usuário. |
| **Mesclagem Indevida no PULL** | [`app/src/main/java/com/example/network/supabase/SupabaseSyncManager.kt:181-362`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/network/supabase/SupabaseSyncManager.kt#L181-L362) | `pullRemoteChanges(userId)` baixa os registros remotos da nova conta logada e os insere sobre a mesma tabela Room que já continha dados da conta anterior, sobrepondo ou misturando os dois conjuntos de dados. |
| **Logout sem Limpeza Local** | [`app/src/main/java/com/example/ui/GiroCustoViewModel.kt:573-584`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/GiroCustoViewModel.kt#L573-L584) | `logout()` desconecta a sessão remota e limpa a biometria, mas preserva o banco Room intacto. O próximo usuário que logar vê os dados do motorista anterior. |
| **Disparo de Sync sem Validação de Dono** | [`app/src/main/java/com/example/ui/GiroCustoViewModel.kt:417, 441, 470, 516, 538, 810, 1044, 1055`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/GiroCustoViewModel.kt) | Múltiplos pontos chamam `triggerSync()` sem verificar previamente se o proprietário dos dados locais coincide com o usuário autenticado. |

---

## 2. Arquitetura da Guarda de Dono (Owner Guard)

### 2.1 Armazenamento Próprio Fora do Room (Sem Migrations)
Para manter o banco Room na versão 12 intacto (sem migrations adicionais e sem quebra de compatibilidade com a T-08):
- **Arquivo de Armazenamento:** `giro_custo_data_owner.xml` (`SharedPreferences` privado via `Context.MODE_PRIVATE`).
- **Política de Backup (Condição 4):** Este arquivo **NÃO É EXCLUÍDO DO BACKUP**. Ele permanece incluído nas regras de backup (`backup_rules.xml` e `data_extraction_rules.xml`), garantindo perfeita coerência com o banco SQLite do Room (`giro_custo_database`), que também é restaurado pelo Google Cloud Backup. Assim, se o motorista restaurar o aparelho a partir do backup na nuvem, o vínculo do proprietário dos dados locais permanece preservado.
- **Contrato de Interface:**
  ```kotlin
  package com.example.data

  interface DataOwnerStorage {
      fun getDataOwnerId(): String?
      fun setDataOwnerId(userId: String)
      fun clearDataOwner()
      fun hasOwner(): Boolean
  }
  ```
- **Implementação:** `SharedPreferencesDataOwnerStorage(context: Context) : DataOwnerStorage`.

### 2.2 Ciclo de Vida: Logout, Fallback Biométrico e Chave Invalidada (Condição 6)
- **Regra Rígida:** Os métodos de saída (`logout()`, `handleBiometricKeyInvalidated()`, `dismissBiometricLockForFallback()`):
  1. Apagam **SOMENTE** a sessão remota de autenticação (`supabaseClient.signOut()` / `clearSavedSession()`) e as credenciais biométricas (`BiometricHelper.clearBiometrics()`).
  2. **NUNCA** apagam o banco Room local.
  3. **NUNCA** apagam o registro de dono em `giro_custo_data_owner.xml`.
- **Racional Técnico:** O motorista que usa o app na rua precisa continuar vendo seus dados se estiver offline, ou se o app fechar em segundo plano. Os dados locais só são substituídos ou limpos mediante processo formal e controlado de **troca de conta**.

---

## 3. Salvaguardas Obrigatórias e Resolução de Conflitos

### 3.1 Salvaguarda 1 & Condição 1: Trava Rígida no PUSH e no PULL
No `SupabaseSyncManager`:
- **Trava no PUSH (`pushLocalChanges`):**
  Antes de inspecionar registros ou chamar qualquer endpoint do Supabase:
  ```kotlin
  val ownerId = dataOwnerStorage.getDataOwnerId()
  if (ownerId != null && ownerId != userId) {
      throw DataOwnerMismatchException(
          ownerId = ownerId,
          currentUserId = userId,
          operation = "PUSH"
      )
  }
  ```
- **Trava no PULL (`pullRemoteChanges` - Condição 1):**
  Antes de realizar **qualquer requisição de rede** (ex.: `restApi.getVehicles()`):
  ```kotlin
  val ownerId = dataOwnerStorage.getDataOwnerId()
  if (ownerId != null && ownerId != userId) {
      throw DataOwnerMismatchException(
          ownerId = ownerId,
          currentUserId = userId,
          operation = "PULL"
      )
  }
  ```
  Se o dono for divergente, a sincronização é abortada imediatamente com erro tipado, impedindo a sobreposição de dados locais com remotos.

### 3.2 Salvaguarda 2 & Condição 2: Ponto Central Único e Serialização via Mutex
- Toda tentativa de sincronização passa por um ponto central: `prepareAndExecuteAccountSync(userId: String)`.
- **Serialização por Mutex:** Utiliza um `Mutex` compartilhado no repositório / sync manager (`syncMutex`). Nenhuma sincronização, consulta ou rotina de limpeza pode rodar simultaneamente com uma transição de conta.
- **Isolamento Visual:** Durante a limpeza e o pull inicial da nova conta, o ViewModel emite estado de carregamento isolado (`_isAccountSwitching.value = true`). A interface permanece em loading e **NUNCA** projeta na tela dados residuais da conta anterior.
- **Cobertura Completa:** Centraliza o controle para todos os pontos que disparam `triggerSync()` no `GiroCustoViewModel`:
  - Linha 417: Restauração de sessão salva na inicialização do app (`init`);
  - Linha 441: Conclusão de login com e-mail e senha (`signInWithEmail`);
  - Linha 470: Conclusão de registro com e-mail e senha (`signUpWithEmail`);
  - Linha 516: Conclusão de login com o Google (`signInWithGoogle`);
  - Linha 538: Desbloqueio e restauração via biometria (`unlockWithBiometrics`);
  - Linhas 810, 1044 e 1055: Salvamento de perfil de usuário (`saveUserProfile`);
  - Ação manual do usuário no botão "Sincronizar Agora".

### 3.3 Salvaguarda 3 & Condição 3: Tratamento de Dono Desconhecido (Primeira Execução pós-Atualização)
Quando `dataOwnerStorage.getDataOwnerId()` retorna `null`:
1. **Se o Banco Room estiver vazio:**
   - Define o novo dono: `dataOwnerStorage.setDataOwnerId(newUserId)`.
   - Executa sincronização normal (PULL para puxar dados da nuvem).
2. **Se o Banco Room contiver dados e NÃO houver pendências (`hasPendingSyncChanges() == false`):**
   - Limpa o banco local de forma segura e atômica: `clearAllLocalData()`.
   - Zera o timestamp de última sincronização em `giro_custo_sync_prefs`.
   - Define o novo dono: `dataOwnerStorage.setDataOwnerId(newUserId)`.
   - Executa PULL da conta autenticada.
3. **Se o Banco Room contiver dados e HOUVER pendências (`hasPendingSyncChanges() == true`):**
   - O app **NÃO** apaga nada e **NÃO** envia nada.
   - Pausa o fluxo de sincronização e exibe diálogo emergencial com **TRÊS OPÇÕES**:
     - **Opção 1 (Recomendada):** *"Voltar e entrar com a conta anterior"*  
       Desconecta a sessão atual, mantendo os registros locais intactos, e retorna para a tela de autenticação para que o usuário entre com a conta dona dos dados e sincronize.
     - **Opção 2 (Destrutiva com Confirmação Dupla):** *"Descartar dados"*  
       Abre um diálogo secundário de confirmação estrita: *"Tem certeza absoluta? Os dados não sincronizados serão apagados permanentemente deste celular."*. Somente se o usuário confirmar novamente, executa a limpeza das tabelas, define o novo dono e faz PULL da nova conta.
     - **Opção 3 (Apropriação Consentida com Confirmação):** *"Estes dados são meus, usar nesta conta"*  
       Exibe confirmação: *"Deseja vincular os lançamentos locais à conta atual e enviá-los para a sua nuvem?"*. Se confirmado, registra `dataOwnerStorage.setDataOwnerId(newUserId)` e aciona o PUSH, salvando as pendências sob o UID da nova conta.

### 3.4 Salvaguarda 4 & Condição 5: Troca de Conta entre Donos Conhecidos e Limpeza de Preferências
Quando `ownerId != null && ownerId != newUserId`:
1. **Sem pendências:**
   - Limpa atomicamente todas as tabelas Room (`db.clearAllTables()`).
   - **Gestão de Preferências Locais (Condição 5):**
     - Zera `last_sync_timestamp` em `giro_custo_sync_prefs` para 0L;
     - Zera as metas financeiras diárias, semanais e mensais em `giro_custo_goals`, pois representam metas financeiras particulares de um indivíduo específico.
     - **Preserva** `is_dark_theme` em `giro_custo_goals`, pois o tema escuro/claro é preferência de ergonomia visual do hardware do aparelho, não dado pessoal da conta.
     - Limpa cache de CEP em memória (`clearViaCepState()`) e FIPE (`clearFipe()`).
     - Limpa credenciais biométricas (`BiometricHelper.clearBiometrics()`).
   - Define `dataOwnerStorage.setDataOwnerId(newUserId)`.
   - Executa `pullRemoteChanges(newUserId)`.
2. **Com pendências:**
   - Bloqueio imediato. Apresenta diálogo com duas opções:
     - *"Voltar e sincronizar na conta anterior"*;
     - *"Descartar dados"* (com confirmação dupla obrigatória).

### 3.5 Salvaguarda 5: Aviso Prévio no Logout
Ao acionar "Desconectar / Sair da Conta" no menu ou tela de Nuvem:
- Se `hasPendingSyncChanges() == true`:
  - Exibe alerta: *"Você possui lançamentos que ainda não foram sincronizados com a nuvem. Se outro usuário acessar este aparelho, eles poderão ser descartados. Deseja sincronizar agora antes de sair?"*
  - Opções: *"Sincronizar agora"*, *"Sair mesmo assim"* e *"Cancelar"*.

---

## 4. Guia Passo a Passo: Identificação e Limpeza no Supabase Table Editor

Este procedimento deve ser executado **ANTES** de instalar a nova versão no celular físico, sanando a contaminação cruzada que ocorreu nos testes da T-06b.

### Sequência Segura Global (Do Primeiro ao Último Passo)
1. **No Celular:** Não utilize o app durante a limpeza. Deixe-o fechado.
2. **No Navegador Web:** Faça a exportação de segurança (backup CSV) de todas as tabelas no Supabase.
3. **No Navegador Web:** Identifique e remova os registros que foram enviados incorretamente para a conta cruzada.
4. **No Computador:** Compile e instale a nova versão do GiroCusto (com as salvaguardas da T-06c).
5. **No Celular:** Abra o app e realize o roteiro manual de validação.

---

### Passo 1: Localizar os UUIDs dos Usuários
1. Acesse o [Supabase Dashboard](https://supabase.com/dashboard) e abra o projeto **GiroCusto**.
2. No menu lateral esquerdo, vá em **Authentication** -> **Users**.
3. Localize as duas contas envolvidas e copie seus respectivos **UIDs**:
   - Conta A (`c***@h***.com`) -> UID A (ex.: `aaaaaaaa-1111-2222-3333-444444444444`)
   - Conta B (`s***@g***.com`) -> UID B (ex.: `bbbbbbbb-5555-6666-7777-888888888888`)

---

### Passo 2: Exportar Backup (CSV) de TODAS as Tabelas ANTES de Apagar
No menu lateral esquerdo, clique em **Table Editor**. Para cada uma das 7 tabelas abaixo:
- Clique sobre a tabela;
- No canto superior direito, clique em **Export** -> **Export as CSV**;
- Guarde os arquivos baixados como cópia de segurança preventiva.

---

### Passo 3: Inspeção e Limpeza Tabela por Tabela

Para cada tabela listada, aplique o filtro:
- Clique em **Filter** -> **Add filter**;
- Coluna: `user_id` | Operador: `eq` | Valor: cole o **UID B** (`s***@g***.com`);
- Clique no cabeçalho da coluna `created_at` (ou `updated_at`) para ordenar pelos mais recentes;
- Compare os registros encontrados com o horário em que os testes foram executados;
- **Preferência de Ação:** Selecione as linhas referentes aos testes e clique no botão vermelho **Delete row(s)**. Excluir os registros de teste é a prática recomendada, pois não corrompe referências estrangeiras nem mantém lixo de homologação.

#### Tabelas Reais e Colunas Disponíveis (Conforme `supabase/migrations`):

1. **`vehicle`** (atenção: nome no singular)
   - *Colunas principais:* `id`, `user_id`, `model`, `brand`, `vehicle_type`, `tank_capacity`, `current_odometer`, `created_at`, `updated_at`, `deleted_at`.
   - *Ação:* Se houver veículo criado pelo Usuário A registrado com o `user_id` do Usuário B, exclua a linha indevida.
2. **`vehicle_parts`**
   - *Colunas principais:* `id`, `user_id`, `vehicle_id`, `name`, `price`, `lifespan_km`, `run_km_since_change`, `created_at`, `updated_at`, `deleted_at`.
   - *Ação:* Exclua peças associadas ao teste indevido.
3. **`daily_records`**
   - *Colunas principais:* `id`, `user_id`, `date_string`, `gross_earnings`, `deliveries_count`, `fuel_cost`, `wear_cost`, `net_profit`, `created_at`, `updated_at`, `deleted_at`.
   - *Ação:* Exclua os lançamentos diários que foram atribuídos erroneamente à Conta B.
4. **`fuel_refills`**
   - *Colunas principais:* `id`, `user_id`, `vehicle_id`, `date_string`, `fuel_type`, `liters`, `price_per_liter`, `total_paid`, `odometer`, `full_tank`, `created_at`, `updated_at`, `deleted_at`.
   - *Ação:* Exclua abastecimentos cruzados.
5. **`maintenance_records`**
   - *Colunas principais:* `id`, `user_id`, `vehicle_id`, `date_string`, `description`, `value`, `odometer`, `created_at`, `updated_at`, `deleted_at`.
   - *Ação:* Exclua manutenções de teste indevidas.
6. **`user_profile`**
   - *Colunas principais:* `id`, `user_id`, `name`, `phone`, `city`, `email`, `cep`, `street`, `address_number`, `neighborhood`, `state_uf`, `onboarding_completed`, `created_at`, `updated_at`, `deleted_at`.
   - *Ação:* Verifique se o perfil do Usuário B tem seu próprio e-mail (`s***@g***.com`) e dados corretos. Se contiver o nome/e-mail do Usuário A, corrija os campos de texto ou exclua o registro para que o Usuário B possa refazer seu onboarding.
7. **`platforms`**
   - *Colunas principais:* `id`, `user_id`, `name`, `segment`, `payment_model`, `color_hex`, `created_at`, `updated_at`, `deleted_at`.
   - *Ação:* Exclua plataformas de teste se houver duplicidade ou vínculo incorreto.

---

## 5. Plano de Testes Automatizados e Roteiro Manual

### 5.1 Suíte de Testes Automatizados (`AccountIsolationAndSyncTest.kt`)
Localizada em `src/test/java/com/example/network/supabase/AccountIsolationAndSyncTest.kt`:

1. **`testPushLocalChanges_whenDataOwnerMismatch_blocksPushAndThrowsError`**:
   - Prepara registros locais pendentes com dono "user_A".
   - Tenta invocar `pushLocalChanges("user_B")`.
   - **Resultado:** Lança `DataOwnerMismatchException(operation="PUSH")`, 0 chamadas de rede à REST API e dados mantidos locais.
2. **`testPullRemoteChanges_whenDataOwnerMismatch_blocksPullAndThrowsError`**:
   - Prepara dados com dono "user_A".
   - Tenta invocar `pullRemoteChanges("user_B")`.
   - **Resultado:** Lança `DataOwnerMismatchException(operation="PULL")`, 0 chamadas de rede à REST API e dados protegidos.
3. **`testUnknownOwner_emptyDatabase_assignsOwnerAndSyncs`**:
   - Banco Room vazio e `dataOwnerId == null`.
   - Executa sincronização com "user_A".
   - **Resultado:** Atribui "user_A" como dono em `giro_custo_data_owner.xml` e conclui o PULL.
4. **`testUnknownOwner_withDataAndNoPending_clearsLocalDatabaseAndPulls`**:
   - Banco Room com registros sincronizados (`SYNCED`) e dono nulo.
   - Executa transição para "user_B".
   - **Resultado:** Banco Room é zerado (`clearAllTables`), timestamp de sync é resetado, novo dono vira "user_B" e dados de "user_B" são puxados da nuvem.
5. **`testUnknownOwner_withPendingChanges_blocksSyncAndSignalsConflict`**:
   - Banco com registro `PENDING_INSERT` e dono nulo.
   - Tenta transição para "user_B".
   - **Resultado:** Sincronização bloqueada, nenhum registro apagado, emite estado de conflito com 3 opções.
6. **`testConflictResolution_claimData_assignsOwnerAndPushes`**:
   - Usuário escolhe "Estes dados são meus, usar nesta conta".
   - **Resultado:** Dono local vira "user_B", pendências continuam no Room e `pushLocalChanges` envia com sucesso para "user_B".
7. **`testConflictResolution_discardRequiresDoubleConfirmation_thenClears`**:
   - Usuário solicita descarte: 1ª confirmação abre confirmação estrita;
   - Somente após 2ª confirmação executa `clearAllLocalData()`, reseta sync prefs, define dono e faz pull.
8. **`testAccountSwitch_duringActiveSync_isSerializedViaMutex`**:
   - Executa sincronização longa concorrente com tentativa de troca de conta.
   - **Resultado:** O Mutex garante execução estritamente sequencial, impedindo condições de corrida ou mistura de dados.
9. **`testLogoutAndBiometricKeyInvalidated_preserveRoomDatabaseAndOwner`**:
   - Registra dados no Room e define dono.
   - Executa `logout()`, `handleBiometricKeyInvalidated()` e `dismissBiometricLockForFallback()`.
   - **Resultado:** Sessão remota e biometria são limpas, mas tabelas do Room e arquivo de dono continuam 100% íntegros.
10. **`testDataOwnerMismatchError_isTypedAndSurfacedToUi`**:
    - Dispara sync com dono divergente.
    - **Resultado:** O erro é capturado e exibe mensagem compreensível de orientação na UI.

### 5.2 Roteiro de Teste Manual no Aparelho Real (Textos Idênticos ao App)

#### Cenário 1: Troca sem Pendências (Transição Limpa)
1. No aparelho, logado com a **Conta A**, acesse a tela de Sincronização tocando no ícone de nuvem no topo da tela (ou no Menu Lateral -> **"Nuvem"**).
2. Verifique se o status está **"Sincronizado"** (ou toque no botão **"Sincronizar Agora"** e aguarde ficar verde).
3. Na parte inferior da tela, toque no botão vermelho **"Desconectar / Sair da Conta"**.
4. No diálogo "Desconectar Conta?", toque em **"Desconectar"**.
5. Na tela de login, faça login com a **Conta B**.
6. **Resultado Esperado:** O aplicativo abre com indicador de carregamento. As tabelas da Conta A foram limpas (a tela nunca mostra os dados da Conta A). Em seguida, a sincronização baixa e exibe exclusivamente os dados da Conta B.

#### Cenário 2: Tentativa de Troca com Pendências (Proteção contra Perda)
1. Logado na **Conta B**, ative o modo avião (sem internet).
2. Lance uma nova diária ou abastecimento no app.
3. Observe na tela de Nuvem que o status indica pendências locais não enviadas.
4. Tente desconectar a conta tocando em **"Desconectar / Sair da Conta"**.
5. **Resultado Esperado:** O app exibe o diálogo de aviso alertando que há lançamentos pendentes não sincronizados.
6. Toque em **"Cancelar"**, desative o modo avião e toque em **"Sincronizar Agora"**.
7. Após o envio bem-sucedido, desconecte com segurança.

#### Cenário 3: Descarte de Dados com Confirmação Dupla
1. Com lançamentos pendentes de teste de outra conta e dono divergente, ao tentar autenticar:
2. O app exibe o diálogo de conflito: escolha **"Descartar dados"**.
3. O app exibe o segundo diálogo: *"Tem certeza absoluta? Os dados não sincronizados serão excluídos permanentemente deste aparelho."*.
4. Ao tocar em **"Sim, descartar"**, os dados locais são limpos e a nova conta é sincronizada.

#### Cenário 4: Voltar à Conta Anterior e Restaurar Dados
1. No diálogo de conflito, escolha **"Voltar e entrar com a conta anterior"**.
2. O login atual é desfeito.
3. Entre novamente com a Conta A: os dados locais continuam íntegros no aparelho, prontos para sincronização imediata.

---

## 6. Pareceres das Especialidades

- **Arquiteto de Software:** APROVADO. A introdução do `DataOwnerStorage` fora do Room e do `Mutex` de transição de conta protege a base de código sem exigir migrations arriscadas, respeitando integralmente as camadas de arquitetura.
- **Especialista em Segurança e Privacidade:** APROVADO. A trava dupla (PUSH e PULL) elimina qualquer possibilidade de vazamento de dados locais entre contas distintas no mesmo dispositivo físico, atendendo às exigências da LGPD.
- **Especialista em UX/UI:** APROVADO. As salvaguardas de interface impedem perda acidental de lançamentos através de confirmação dupla e oferecem comunicação compreensível para entregadores na rua.
