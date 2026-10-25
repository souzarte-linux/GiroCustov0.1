# RELATÓRIO DE CONCLUSÃO DA TAREFA T-06c (FASE B)

---

### 1. Resultado
🟡 **IMPLEMENTADO, NÃO VERIFICADO NO APARELHO**: O isolamento estrito de dados entre contas no mesmo dispositivo físico foi totalmente implementado e validado por 182 testes unitários automatizados (100% de aprovação), com mutação manual comprovada nas travas de PUSH e PULL e análise de integridade e lint limpos. O app **NÃO** foi instalado no aparelho físico nem testado em tela real, pois o usuário deve primeiro realizar a limpeza de registros misturados na nuvem (Supabase) conforme o guia da Seção 7.

---

### 2. O que mudou

#### Explicação Leiga:
Anteriormente, quando dois motoristas utilizavam o mesmo smartphone ou quando você alternava de uma conta para outra (por exemplo, de `c***@h***.com` para `s***@g***.com`), o aplicativo mantinha no aparelho os veículos e corridas gravados pela primeira conta. Ao acionar a sincronização na segunda conta, o sistema enviava os dados do motorista anterior para a nuvem da conta recém-conectada, misturando as informações financeiras.

Com esta alteração:
1. **Guarda de Dono:** O aplicativo agora registra e memoriza quem é o proprietário legítimo dos dados armazenados no celular. Esse registro fica guardado em um arquivo de preferências próprio (`giro_custo_data_owner.xml`), que não altera a estrutura interna do banco de dados (sem risco de quebras em futuras migrações) e permanece protegido no backup do sistema Android junto com o banco de dados.
2. **Travas de Segurança:** Tanto o envio (PUSH) quanto o recebimento (PULL) de dados pelo Supabase possuem travas rígidas de segurança. Se o dono dos dados locais for diferente da conta atualmente logada, qualquer sincronização é imediatamente interrompida antes mesmo de enviar qualquer dado pela internet.
3. **Ponto Central de Troca de Conta:** Qualquer tentativa de sincronização (automática ou manual) passa por uma verificação central antes de agir. Se você trocar de conta e **não houver dados pendentes**, o aplicativo limpa a memória local da conta antiga e baixa os dados da conta nova com uma tela de carregamento, sem nunca exibir os dados anteriores.
4. **Proteção Contra Perda de Dados:** Se você tiver corridas ou abastecimentos salvos no celular que ainda não foram sincronizados e tentar trocar de conta ou sair do aplicativo:
   - Ao sair da conta, o app avisa que há lançamentos locais e oferece a opção de sincronizar antes de sair.
   - Ao entrar com outra conta, o app bloqueia a sincronização e exibe uma tela com opções claras:
     - *Voltar e entrar com a conta anterior* (para não perder nada);
     - *Descartar dados* (exige confirmação dupla antes de apagar);
     - *Estes dados são meus, usar nesta conta* (caso seja a primeira vez que você está usando o app atualizado e queira assumir aqueles lançamentos para a conta nova).
5. **Preservação no Logout e Biometria:** Sair da conta (logout), falha biométrica ou cancelamento de biometria apagam **apenas** a sessão de acesso. Eles **nunca** apagam as corridas locais nem o registro de dono.

#### Lista de Arquivos Criados ou Alterados:
- `docs/design-isolamento-contas.md`: Atualizado com a incorporação formal de todas as 10 condições obrigatórias e nomes exatos das tabelas do Supabase.
- `app/src/main/java/com/example/data/DataOwnerStorage.kt`: Nova interface e implementação persistente (`SharedPreferencesDataOwnerStorage`) com gravação síncrona imediata (`commit()`).
- `app/src/main/java/com/example/network/supabase/DataOwnerMismatchException.kt`: Nova exceção tipada de domínio para falhas de divergência de dono.
- `app/src/main/java/com/example/network/supabase/SupabaseSyncManager.kt`: Adição das travas defensivas atômicas no PUSH e no PULL antes de qualquer chamada REST.
- `app/src/main/java/com/example/data/Repository.kt`: Métodos auxiliares para contagem de pendências, verificação de dados locais, limpeza atômica e injeção do `DataOwnerStorage`.
- `app/src/main/java/com/example/ui/GiroCustoViewModel.kt`: Ponto centralizado de decisão de troca de conta, serialização com `Mutex`, controle do estado de conflito (`AccountConflictState`), limpeza de preferências específicas da conta e proteção no logout.
- `app/src/main/java/com/example/ui/screens/CloudSyncScreen.kt`: Interceptação de clique no botão de logout quando houver pendências não sincronizadas.
- `app/src/main/java/com/example/ui/navigation/GiroCustoMainApp.kt`: Camada de bloqueio visual durante troca de conta e diálogos de conflito, descarte com dupla confirmação e reivindicação de dados.
- `app/src/test/java/com/example/network/supabase/AccountIsolationAndSyncTest.kt`: Suíte com 10 testes cobrindo todas as regras de isolamento.

---

### 3. Evidências

#### A. Execução da Suíte Completa de Testes (`testDebugUnitTest` com `--rerun-tasks`):
- **Comando executado:**
  ```bash
  export JAVA_HOME=/home/nando/.jdk17 && export PATH=$JAVA_HOME/bin:$PATH && ./gradlew testDebugUnitTest --rerun-tasks
  ```
- **Contagem real obtida:**
  - **Executados:** 182
  - **Aprovados:** 182
  - **Falhas:** 0
  - **Ignorados / Skipped:** 0
  - **Taxa de Sucesso:** 100%
  - **Duração:** 41.508s (tempo total de build: 3m 38s)
- **Localização do Relatório HTML:**
  `app/build/reports/tests/testDebugUnitTest/index.html`

#### B. Execução do Lint (`lintDebug`):
- **Comando executado:**
  ```bash
  export JAVA_HOME=/home/nando/.jdk17 && export PATH=$JAVA_HOME/bin:$PATH && ./gradlew lintDebug
  ```
- **Resultado:**
  ```text
  BUILD SUCCESSFUL in 1s
  42 actionable tasks: 1 executed, 41 up-to-date
  Configuration cache entry reused.
  ```
  *(0 erros de lint).*

#### C. Varreduras Automáticas de Integridade:

1. **Varredura 1: Stubs, mocks e pendências em código de produção (`app/src/main/java`)**
   - **Comando:**
     ```bash
     grep -rniE "\b(mock|stub|fake|homologa|TODO|FIXME|em breve)\b" app/src/main/java
     ```
   - **Saída obtida:**
     ```text
     app/src/main/java/com/example/ui/navigation/GiroCustoMainApp.kt:1:// TODO: Implementar menu lateral (drawer) com acesso ao perfil de usuário, substituindo a navegação da aba Veículo.
     app/src/main/java/com/example/ui/components/SmartphoneSimulator.kt:44:            // Desenha um mock realista de smartphone centralizado na tela
     ```
   - **Explicação:** Comentários explicativos existentes; nenhuma função stub adicionada em produção.

2. **Varredura 2: Testes desativados, ignorados ou vazios (`app/src/test` e `app/src/androidTest`)**
   - **Comando:**
     ```bash
     grep -rnE "@Ignore|assertTrue\(true\)|assertEquals\(([0-9]+), *\1\)" app/src/test app/src/androidTest
     ```
   - **Saída:** 0 ocorrências.

3. **Varredura 3: Busca por segredos, senhas e chaves privadas vazadas**
   - **Comando:**
     ```bash
     grep -rniE "service_role|BEGIN (RSA|PRIVATE)" .
     ```
   - **Saída:** 0 ocorrências em arquivos de código e configuração.

---

### 4. Testes

- **Testes adicionados (10 novos testes em `AccountIsolationAndSyncTest.kt`):**
  1. `testSwitchAccount_withoutPendingChanges_clearsLocalDataAndDownloadsRemoteData`
  2. `testSwitchAccount_withPendingChanges_blocksSyncAndSignalsConflict`
  3. `testPushLocalChanges_whenDataOwnerMismatch_blocksPushAndThrowsError`
  4. `testPullRemoteChanges_whenDataOwnerMismatch_blocksPullAndThrowsError`
  5. `testUnknownOwner_withPendingChanges_blocksSyncAndSignalsConflict`
  6. `testUnknownOwner_withoutPendingChanges_clearsAndSyncsWithNewOwner`
  7. `testDiscardData_requiresTwoStepConfirmation_andClearsOnlyAfterConfirmation`
  8. `testClaimDataOption_bindsLocalDataToCurrentAccountAndSyncs`
  9. `testLogoutAndBiometricFallback_clearsOnlyAuthSession_preservesRoomAndOwner`
  10. `testTriggerSync_whenDataOwnerMismatch_setsUserFriendlyErrorMessage`

- **Testes existentes alterados ou removidos:** Nenhum teste existente foi alterado ou removido.
- **Mutação manual:** Comprovada em falha imediata dos testes de trava de PUSH e PULL ao comentar as checagens e restauração com sucesso.

---

### 5. O que NÃO foi feito ou está simulado

- Nada está simulado.
- O app não foi instalado no smartphone nem testado no aparelho físico nesta etapa, aguardando a limpeza da nuvem no Supabase pelo usuário.

---

### 6. Riscos e dívidas encontrados

1. Registros antigos misturados no Supabase exigem limpeza manual pelo usuário.
2. Metas de faturamento redefinidas para o padrão local ao alternar contas.

---

### 7. O que o usuário precisa fazer

#### PARTE A: Guia Passo a Passo para Limpeza no Supabase (Table Editor)
1. Acesse o painel do Supabase no computador.
2. Exporte o CSV de backup de cada tabela: `daily_records`, `fuel_refills`, `maintenance_records`, `vehicle_parts`, `vehicle`, `platforms`, `user_profile`.
3. Na tabela `daily_records`, ordene por `created_at` descendente.
4. Identifique as corridas criadas durante o teste da T-06b vinculadas à conta errada.
5. Selecione a linha e clique em **Delete 1 row**.
6. Repita para `fuel_refills` e `vehicle` se tiver adicionado dados de teste.

#### PARTE B: Roteiro de Testes Manuais no Aparelho (Após Limpeza)
1. Abra o app, vá na aba **Nuvem** e use o botão **Sair da Conta**.
2. Faça login com a Conta A (`s***@g***.com`) e sincronize.
3. Saia e faça login com a Conta B (`c***@h***.com`). Confirme que os dados da Conta A somem e não aparecem.
4. Crie uma corrida sem sincronizar e tente sair da conta. Confirme que o diálogo de lançamentos pendentes aparece.
5. Ao tentar sincronizar em outra conta com dados pendentes, confirme a trava e teste o fluxo de descarte seguro com dupla confirmação ou "Estes dados são meus".

---

### 8. Pareceres Formais de Revisão e Segurança

- **Revisor Independente:** APROVADO.
- **Agente de Segurança e Privacidade:** APROVADO.

---

### 9. Commit e Sincronização

- Commit de código: `e1c07427a963b12930e809193c7f3f0928ff10a9`
