# Plano de Correção e Evolução v2.1 (GiroCusto)
**Data:** 22 de Outubro de 2026  
**Autor:** Orquestrador (com parecer do Revisor Independente)  
**Documento Base:** [docs/auditoria-2026-10-22.md](file:///home/nando/Documentos/GiroCustov0.1/docs/auditoria-2026-10-22.md)  
**Diretiva:** Planejamento rigoroso, atômico e sequencial contra as regras de engenharia da equipe.

---

## 1. Reclassificação de Severidade e Correção do Placar

### 1.1 Reavaliação de SEC-01 e SEC-02 (Armazenamento e Backup)
Na auditoria inicial, SEC-01 e SEC-02 haviam sido classificados preliminarmente como **Críticos** sob a suspeita de exposição de credenciais completas (incluindo senhas).
- **Evidência no código real:** Uma busca profunda por todas as chamadas `putString` em `app/src/main/java` revelou que o aplicativo grava estritamente:
  - `KEY_ACCESS_TOKEN` (`session.accessToken`)
  - `KEY_REFRESH_TOKEN` (`session.refreshToken`)
  - `KEY_USER_ID` (`session.userId`)
  - `KEY_EMAIL` (`session.userEmail`) em `SupabaseClient.kt:70-73`
  - `KEY_BIOMETRIC_EMAIL` (`email`) em `BiometricHelper.kt:66`
- **Conclusão:** O aplicativo **NÃO armazena senhas em texto puro em disco**. As senhas existem apenas na memória volátil (`Compose State`) durante o fluxo de entrada e registro nas telas `AuthGateScreen` e `CloudSyncScreen`.
- **Reclassificação:** 
  - **SEC-01** é reclassificado de *Crítico* para **Alto** (tokens de sessão em SharedPreferences sem Keystore geram risco de sequestro de sessão se o dispositivo for comprometido, mas não expõem senhas do usuário nem chaves mestras).
  - **SEC-02** é reclassificado de *Crítico* para **Alto** (o backup na nuvem copia os tokens e preferências locais, mas a exclusão cirúrgica de `giro_custo_supabase_auth.xml` e `giro_custo_biometrics.xml` elimina o risco sem necessidade de desativar o backup do banco Room sem autorização do usuário).

### 1.2 Reavaliação de DAT-02 (Schemas Históricos do Room)
- **Evidência no histórico Git:** O histórico do repositório (`git log --all --full-history -- app/schemas`) comprova que os schemas `1.json`, `4.json`, `5.json`, `6.json` e `7.json` **nunca foram commitados** no repositório. O repositório possui apenas os schemas `2.json`, `3.json`, `8.json`, `9.json`, `10.json`, `11.json` e `12.json`.
- **Conclusão:** Como o GiroCusto é um aplicativo de uso pessoal cujo banco de dados já se encontra consolidado na versão 12, e todas as migrações recentes (9->10, 10->11, 11->12) possuem testes com `MigrationTestHelper` contra schemas reais, tentar recriar os schemas 1 a 7 artificialmente traria schemas sintéticos com baixo valor real.
- **Reclassificação:** **DAT-02** é reclassificado de *Alto* para **Baixo**, com foco na garantia de que todo schema novo (versão 13 em diante) seja compulsoriamente versionado e testado.

### 1.3 Placar Corrigido e Auditado

#### Placar por Severidade
| Severidade | Quantidade | Justificativa Técnica |
| :--- | :---: | :--- |
| 🔴 **Crítico** | **1** | **UI-01**: `SmartphoneSimulator` em `GiroCustoMainApp.kt` quebra totalmente o uso do app em tablets, celulares dobráveis e telas horizontais (>480dp), prendendo a produção em 390dp x 820dp. |
| 🟠 **Alto** | **14** | SEC-01, SEC-02, SEC-03, SEC-04, SEC-05, SEC-06, INT-01, ARQ-01, ARQ-02, DAT-01, UX-01, ACC-01, PERF-01, PERF-02. |
| 🟡 **Médio** | **6** | UX-02, SEC-07, INT-02, ARQ-03, TST-01, SEC-08. |
| 🔵 **Baixo** | **4** | ACC-02, ARQ-04, DEV-01, DAT-02. |
| **TOTAL** | **25** | **Todos os 25 achados mapeados individualmente em tarefas.** |

#### Placar por Área (Auditado e Corrigido)
| Área Técnica | Crítico | Alto | Médio | Baixo | Total |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **A. Integridade e Funcionalidades Falsas** | 0 | 2 | 1 | 0 | **3** |
| **B. Segurança e Privacidade** | 0 | 6 | 2 | 0 | **8** |
| **C. Dados e Arquitetura** | 1 | 2 | 1 | 2 | **6** |
| **D. UX/UI e Acessibilidade** | 0 | 2 | 1 | 1 | **4** |
| **E. Integrações e Performance** | 0 | 2 | 1 | 1 | **4** |
| **TOTAL** | **1** | **14** | **6** | **4** | **25** |

*(Nota de auditoria: A área B contém 8 achados [SEC-01 a SEC-08] e a área D contém 4 achados [UX-01, UX-02, ACC-01, ACC-02], corrigindo a contagem da v1).*

---

## 2. Estratégia de Execução em Ondas e Regras Rígidas

### 2.1 Regras Rígidas de Execução
1. **Uma tarefa por vez:** O agente deve executar estritamente uma única tarefa por ciclo, na ordem numérica estabelecida.
2. **Um commit por tarefa:** Cada tarefa concluída deve gerar exatamente um commit no padrão do projeto, conforme `workflows/commit.md`.
3. **Verificação completa mandatória:** Antes de considerar qualquer tarefa pronta, a suíte completa `./gradlew testDebugUnitTest lintDebug` deve passar sem falhas e sem testes ignorados. É terminantemente proibido usar filtros parciais (`--tests`) como evidência de conclusão.
4. **Tratamento de Interface:** Para toda tarefa que altera a interface, testes unitários não comprovam o resultado visual. O agente deve entregar um roteiro de teste manual no aparelho, passo a passo completo, para o usuário executar. Sem a confirmação de teste em aparelho real pelo usuário, o status máximo da entrega é 🟡 **IMPLEMENTADO, NÃO VERIFICADO NO APARELHO**.
5. **Textos novos em strings.xml:** Todo texto novo adicionado (inclusive mensagens de erro, botões e atributos `contentDescription`) deve obrigatoriamente utilizar `stringResource` apontando para `strings.xml`.
6. **Validação de integração no aparelho:** Após a conclusão de **T-06**, **T-07** e **T-14**, o usuário deve testar manualmente no aparelho real o login com Google e a biometria, uma vez que os testes automatizados utilizam objetos falsos (fakes/mocks) e podem não detectar falhas de integração no runtime do sistema operacional.

### 2.2 Sequência da Onda 1
A **Onda 1** segue estritamente a ordem de precedência estabelecida pelo proprietário do projeto:
1. `SEC-08` (T-01): Blindagem preventiva do `.gitignore` contra vazamento acidental de chaves.
2. `SEC-02` (T-02): Exclusão de credenciais do backup Android (mantendo o banco Room).
3. `UI-01` (T-03): Remoção do `SmartphoneSimulator` da produção.
4. `INT-01 + SEC-03` (T-04): Ocultação do stub de SMS e higienização dos logs HTTP (OkHttp Level.BASIC com redação de tokens e senhas).
5. `UX-01` (T-05): Diálogo de confirmação e "Desfazer" baseado na exclusão lógica (`deletedAt` / `isDeleted`).
6. `SEC-01 + SEC-04` (T-06): Projeto único de segurança de sessão e biometria com Android Keystore e `CryptoObject`.
7. `SEC-05` (T-07): Ativação do R8/minificação com buildType `releaseCheck` e configuração de regras no `proguard-rules.pro`.

---

## 3. Catálogo Completo de Tarefas (26 Tarefas para 25 Achados)

> **Regra Obrigatória:** Toda tarefa abaixo utiliza verificação completa da suíte de testes e lint:  
> `./gradlew testDebugUnitTest lintDebug`. É proibido usar `--tests` parcial como evidência de conclusão.

### ONDA 1: Segurança Imediata, Usabilidade Crítica e Proteção

#### Tarefa T-01 [SEC-08]: Blindagem Preventiva do .gitignore
```json
{
  "task_id": "T-01",
  "to_agent": "devops",
  "task_type": "seguranca",
  "risk": "baixo",
  "title": "Adicionar padrões de certificados e credenciais ao .gitignore",
  "context": "O .gitignore ignora apenas debug.keystore e .env, mas não contém padrões genéricos para *.jks, *.keystore e google-services.json, gerando risco de commit acidental.",
  "root_cause_evidence": ".gitignore:1-19",
  "acceptance_criteria": [
    "Padrões *.jks, *.keystore, google-services.json e secrets.properties presentes no .gitignore",
    "Nenhum arquivo de credencial rastreado pelo git"
  ],
  "files_expected": [
    ".gitignore"
  ],
  "out_of_scope": ["Alterações de build ou código-fonte"],
  "verification": [
    "git check-ignore -v test.jks google-services.json",
    "./gradlew testDebugUnitTest lintDebug"
  ],
  "required_reviewers": ["seguranca", "revisor"],
  "depends_on": []
}
```

#### Tarefa T-02 [SEC-02]: Proteção de Backup sem Comprometer Banco Room
```json
{
  "task_id": "T-02",
  "to_agent": "seguranca",
  "task_type": "seguranca",
  "risk": "alto",
  "title": "Exclusão cirúrgica de preferências de autenticação e biometria do backup Android",
  "context": "data_extraction_rules.xml e backup_rules.xml estão com templates vazios. O backup na nuvem ou extração via USB (adb backup) copia as credenciais de sessão.",
  "root_cause_evidence": "AndroidManifest.xml:10-12, app/src/main/res/xml/data_extraction_rules.xml:8 e backup_rules.xml:8",
  "acceptance_criteria": [
    "data_extraction_rules.xml e backup_rules.xml configurados com exclude específico para giro_custo_supabase_auth.xml, giro_custo_biometrics.xml e o futuro arquivo de sessão segura a ser criado na T-06 (giro_custo_secure_session.xml)",
    "O banco de dados Room (giro_custo_database) NÃO é excluído do backup nesta etapa (permanece preservado; o usuário será consultado antes de qualquer exclusão)",
    "AndroidManifest.xml preserva android:allowBackup='true' de forma segura com as novas regras configuradas",
    "Dependência formal registrada com a tarefa T-06 para que o arquivo criptografado criado tenha o mesmo nome excluído aqui"
  ],
  "files_expected": [
    "app/src/main/res/xml/data_extraction_rules.xml",
    "app/src/main/res/xml/backup_rules.xml"
  ],
  "out_of_scope": ["Exclusão do banco de dados Room do backup (exige aprovação prévia do usuário)"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
  ],
  "required_reviewers": ["seguranca", "revisor"],
  "depends_on": ["T-01"]
}
```

#### Tarefa T-03 [UI-01]: Remoção do SmartphoneSimulator do Fluxo de Produção
```json
{
  "task_id": "T-03",
  "to_agent": "frontend",
  "task_type": "implementacao",
  "risk": "alto",
  "title": "Remover moldura de SmartphoneSimulator da produção em GiroCustoMainApp",
  "context": "GiroCustoMainApp envolve toda a UI no SmartphoneSimulator, que força width 390dp e height 820dp sempre que maxWidth > 480dp, tornando o app inutilizável em tablets e celulares em orientação paisagem.",
  "root_cause_evidence": "GiroCustoMainApp.kt:118 e SmartphoneSimulator.kt:37-53",
  "acceptance_criteria": [
    "GiroCustoMainApp renderiza diretamente AppDrawer e Scaffold sem envolver em SmartphoneSimulator",
    "SmartphoneSimulator é preservado exclusivamente para visualizações @Preview em ferramentas de design",
    "Layout adapta-se a qualquer largura de tela sem barras pretas laterais ou notch falso na produção",
    "Textos novos (inclusive contentDescription) utilizam stringResource",
    "Entregar roteiro de teste manual no aparelho, passo a passo completo, para o usuário executar (sem confirmação no aparelho, status máximo 🟡)"
  ],
  "files_expected": [
    "app/src/main/java/com/example/ui/navigation/GiroCustoMainApp.kt"
  ],
  "out_of_scope": ["Alteração da lógica das abas ou do AppDrawer"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
  ],
  "required_reviewers": ["designer", "revisor"],
  "depends_on": []
}
```

#### Tarefa T-04 [INT-01 e SEC-03]: Ocultação de Stub de SMS e Redação de Logs HTTP
```json
{
  "task_id": "T-04",
  "to_agent": "frontend",
  "task_type": "implementacao",
  "risk": "alto",
  "title": "Ocultar opção de login por SMS e configurar HttpLoggingInterceptor com Level.BASIC e redação de headers",
  "context": "signInWithPhone é um stub em homologação conectado a botões reais; HttpLoggingInterceptor loga Level.BODY imprimindo tokens e payloads no Logcat.",
  "root_cause_evidence": "GiroCustoViewModel.kt:486-487, AuthGateScreen.kt:616, 644 e SupabaseClient.kt:105-107",
  "acceptance_criteria": [
    "Botão de login por SMS na AuthGateScreen desabilitado ou oculto com rótulo 'Em breve' via stringResource",
    "HttpLoggingInterceptor em modo Debug configurado com Level.BASIC (nenhum corpo de requisição é impresso no Logcat)",
    "HttpLoggingInterceptor configurado com redactHeader('Authorization') e redactHeader('apikey')",
    "Nenhuma função acionável pela interface emite mensagem fixa de homologação",
    "Entregar roteiro de teste manual no aparelho, passo a passo completo, para o usuário executar (sem confirmação no aparelho, status máximo 🟡)"
  ],
  "files_expected": [
    "app/src/main/java/com/example/ui/screens/AuthGateScreen.kt",
    "app/src/main/java/com/example/network/supabase/SupabaseClient.kt",
    "app/src/main/res/values/strings.xml"
  ],
  "out_of_scope": ["Implementação completa de provedor SMS externo"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
  ],
  "required_reviewers": ["seguranca", "revisor"],
  "depends_on": []
}
```

#### Tarefa T-05 [UX-01]: Confirmação de Exclusão e Desfazer via Soft Delete
```json
{
  "task_id": "T-05",
  "to_agent": "frontend",
  "task_type": "implementacao",
  "risk": "alto",
  "title": "Adicionar confirmação prévia e ação Desfazer via isDeleted ao excluir veículos e peças",
  "context": "Na ProfileScreen, toques acidentais em excluir veículo ou peça apagam o registro na hora com um simples Toast.",
  "root_cause_evidence": "ProfileScreen.kt:240-244, 261-265 e Repository.kt:94-101, 179-186",
  "acceptance_criteria": [
    "Exibição de diálogo AlertDialog solicitando confirmação do motorista antes de apagar veículo ou peça, com textos em stringResource",
    "Após confirmação, registro é marcado como isDeleted = true e um Snackbar é exibido com ação 'Desfazer'",
    "Se 'Desfazer' for acionado, o registro é restaurado (isDeleted = false, syncStatus = PENDING_UPDATE) e updatedAt é atualizado com System.currentTimeMillis() para que o LWW não reaplique a exclusão remota",
    "Criar teste automatizado cobrindo o fluxo completo: excluir, sincronizar, desfazer, sincronizar",
    "Área de toque dos botões de exclusão respeita o mínimo de 48dp",
    "Entregar roteiro de teste manual no aparelho, passo a passo completo, para o usuário executar (sem confirmação no aparelho, status máximo 🟡)"
  ],
  "files_expected": [
    "app/src/main/java/com/example/ui/screens/ProfileScreen.kt",
    "app/src/main/java/com/example/ui/GiroCustoViewModel.kt",
    "app/src/test/java/com/example/SupabaseSyncAndMigrationTest.kt",
    "app/src/main/res/values/strings.xml"
  ],
  "out_of_scope": ["Modificações no schema do Room ou no Supabase"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
  ],
  "required_reviewers": ["designer", "revisor"],
  "depends_on": []
}
```

#### Tarefa T-06 [SEC-01 e SEC-04]: Projeto Único de Segurança de Sessão e Biometria
```json
{
  "task_id": "T-06",
  "to_agent": "seguranca",
  "task_type": "implementacao",
  "risk": "alto",
  "title": "Criptografia de sessão local e autenticação biométrica com Android Keystore",
  "context": "Tokens de sessão (access/refresh) e e-mail biométrico ficam em SharedPreferences em texto puro; BiometricPrompt opera sem CryptoObject.",
  "root_cause_evidence": "SupabaseClient.kt:20, 68-81 e BiometricHelper.kt:24, 62-100",
  "acceptance_criteria": [
    "(a) Antes de codificar, o Arquiteto entrega um desenho técnico de 1 página em docs/design-sessao-segura.md detalhando qual chave protege os tokens, qual chave a biometria protege e como a sincronização em segundo plano continua funcionando sem pedir digital",
    "(b) A chave criptográfica dos tokens NÃO pode exigir autenticação do usuário a cada leitura (para viabilizar sincronização em background sem interrupção)",
    "(c) Tratar chave invalidada (nova digital cadastrada no Android, restauração em outro aparelho, falha de descriptografia) levando o usuário ao login normal, sem apagar dados locais do Room e sem travar o app",
    "(d) Após migrar os dados com sucesso para o armazenamento seguro, APAGAR o SharedPreferences antigo em texto puro",
    "(e) Criar testes unitários específicos para migração do formato legado, chave invalidada e payload corrompido",
    "(f) O arquivo gerado deve ser nomeado giro_custo_secure_session.xml, conforme regra de exclusão já configurada na T-02",
    "BiometricHelper integrado a BiometricPrompt.CryptoObject para que o desbloqueio opere sobre chave criptográfica real",
    "Todos os testes de sessão e autenticação continuam 100% aprovados na suíte completa",
    "Entregar roteiro manual no aparelho: o usuário deve testar login e biometria no aparelho real após conclusão (sem teste real, status máximo 🟡)"
  ],
  "files_expected": [
    "docs/design-sessao-segura.md",
    "app/src/main/java/com/example/network/supabase/SupabaseClient.kt",
    "app/src/main/java/com/example/util/BiometricHelper.kt",
    "app/src/main/java/com/example/ui/GiroCustoViewModel.kt",
    "app/src/test/java/com/example/AuthBiometricsAndViaCepTest.kt"
  ],
  "out_of_scope": ["Alterações de endpoints na API Supabase"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
  ],
  "required_reviewers": ["seguranca", "revisor", "arquiteto"],
  "depends_on": ["T-02"]
}
```

#### Tarefa T-07 [SEC-05]: Configuração de Minificação R8 e Build releaseCheck
```json
{
  "task_id": "T-07",
  "to_agent": "devops",
  "task_type": "seguranca",
  "risk": "alto",
  "title": "Ativar minificação R8 em release, criar buildType releaseCheck e definir regras ProGuard",
  "context": "isMinifyEnabled está false e proguard-rules.pro está vazio. Testes unitários comuns não executam código minificado, exigindo compilação releaseCheck com assinatura de debug para prova manual no aparelho.",
  "root_cause_evidence": "app/build.gradle.kts:45 e app/proguard-rules.pro:1-22",
  "acceptance_criteria": [
    "isMinifyEnabled = true configurado no buildType release em app/build.gradle.kts",
    "Criado o buildType 'releaseCheck' (com initWith(getByName('release')), isMinifyEnabled = true e signingConfig = signingConfigs.getByName('debugConfig'))",
    "Regras ProGuard para Room, Moshi (adapters e codegen) e Retrofit devidamente configuradas em proguard-rules.pro",
    "Verificação automatizada executando ./gradlew assembleReleaseCheck com sucesso, além de ./gradlew testDebugUnitTest lintDebug",
    "Entregar roteiro manual para o usuário instalar a versão releaseCheck e percorrer: login com e-mail, login com Google, lançamento de diária, sincronização com Supabase, busca de postos no mapa, consulta FIPE e leitura de histórico do banco Room",
    "Sem a confirmação do usuário no aparelho real, o status máximo da entrega é 🟡 IMPLEMENTADO, NÃO VERIFICADO NO APARELHO"
  ],
  "files_expected": [
    "app/build.gradle.kts",
    "app/proguard-rules.pro"
  ],
  "out_of_scope": ["Assinatura com chave de produção real (upload-key.jks)"],
  "verification": [
    "./gradlew assembleReleaseCheck",
    "./gradlew testDebugUnitTest lintDebug"
  ],
  "required_reviewers": ["seguranca", "revisor"],
  "depends_on": []
}
```

---

### ONDA 2: Performance, Confiabilidade, UX e Integrações

#### Tarefa T-08 [PERF-01]: Índices no Room Baseados em Consultas Reais
```json
{
  "task_id": "T-08",
  "to_agent": "backend",
  "task_type": "implementacao",
  "risk": "alto",
  "title": "Definir índices nas entidades Room baseados em consultas reais e migração v13 com runMigrationsAndValidate",
  "context": "Nenhuma das 7 entidades Room possui índices definidos, gerando varreduras completas. Cada índice deve ser estritamente justificado por uma consulta de DAO real.",
  "root_cause_evidence": "Entities.kt:13, 36, 59, 161, 183, 206 e Daos.kt:8, 18, 27, 63, 75, 78, 101, 111, 114, 168, 171, 189, 192, 203, 206, 218, 224, 227",
  "acceptance_criteria": [
    "Cada índice adicionado cita a consulta exata (DAO e linha de código):",
    "- daily_records: índice em (isDeleted, dateTimestamp) [Daos.kt:101], índice em remoteId [Daos.kt:111], índice em syncStatus [Daos.kt:114]",
    "- vehicle_parts: índice em vehicleId [Daos.kt:63, 69], índice em remoteId [Daos.kt:75], índice em syncStatus [Daos.kt:78]",
    "- fuel_refills: índice em (vehicleId, dateTimestamp) [Daos.kt:189], índice em (vehicleId, odometer) [Daos.kt:192, 195], índice em remoteId [Daos.kt:203], índice em syncStatus [Daos.kt:206]",
    "- maintenance_records: índice em (vehicleId, dateTimestamp) [Daos.kt:218], índice em remoteId [Daos.kt:224], índice em syncStatus [Daos.kt:227]",
    "- vehicle: índice em (active, isDeleted) [Daos.kt:8, 11], índice em remoteId [Daos.kt:18], índice em syncStatus [Daos.kt:27]",
    "- platforms: índice em remoteId [Daos.kt:168], índice em syncStatus [Daos.kt:171]",
    "Remover índices sem consulta correspondente no DAO e remover índices exclusivos sobre syncStatus sem justificativa",
    "Migração Room MIGRATION_12_13 criada com CREATE INDEX IF NOT EXISTS e exportação do schema 13.json gerada e versionada",
    "Obrigatório testar a migração utilizando runMigrationsAndValidate do MigrationTestHelper para garantir que os nomes, colunas e ordenação dos índices batem 100% com o esperado pelo Room"
  ],
  "files_expected": [
    "app/src/main/java/com/example/data/Entities.kt",
    "app/src/main/java/com/example/data/GiroCustoDatabase.kt",
    "app/schemas/com.example.data.GiroCustoDatabase/13.json",
    "app/src/test/java/com/example/SupabaseSyncAndMigrationTest.kt"
  ],
  "out_of_scope": ["Alterações de schema remoto no Supabase (que já possui índices)"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
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
  "context": "Listas de manutenções, plataformas, histórico e abastecimentos usam items() sem key, provocando recomposições custosas a cada atualização de estado.",
  "root_cause_evidence": "MaintenanceScreen.kt:255, PlatformsScreen.kt:196, HistoryScreen.kt:393, 440 e FuelRefillsScreen.kt:300",
  "acceptance_criteria": [
    "Parâmetro key = { it.id } (ou chave estável equivalente no caso de cabeçalhos/agrupamentos) adicionado a todos os blocos items()",
    "Nenhuma lista rolável opera sem identificador de chave estável",
    "Comportamento visual e ordenação mantidos rigorosamente idênticos"
  ],
  "files_expected": [
    "app/src/main/java/com/example/ui/screens/MaintenanceScreen.kt",
    "app/src/main/java/com/example/ui/screens/PlatformsScreen.kt",
    "app/src/main/java/com/example/ui/screens/HistoryScreen.kt",
    "app/src/main/java/com/example/ui/screens/FuelRefillsScreen.kt"
  ],
  "out_of_scope": ["Refatoração visual das células das listas"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
  ],
  "required_reviewers": ["performance", "revisor"],
  "depends_on": []
}
```

#### Tarefa T-10 [SEC-06]: Portabilidade de Dados e Exclusão Segura de Conta (LGPD)
```json
{
  "task_id": "T-10",
  "to_agent": "seguranca",
  "task_type": "implementacao",
  "risk": "alto",
  "title": "Implementar exportação de dados via SAF e exclusão de conta por RPC Supabase",
  "context": "O app não atende aos requisitos da LGPD de portabilidade e exclusão definitiva de conta. Exclusão exige RPC no Supabase com auth.uid() para evitar chave service_role no app.",
  "root_cause_evidence": "Ausência de rotinas de exportação e exclusão de conta no app",
  "acceptance_criteria": [
    "(a) A função SQL remota no Supabase deve ser SECURITY DEFINER com search_path = public fixo, permitida exclusivamente ao papel authenticated (REVOKE ALL ON FUNCTION delete_user_account() FROM anon, public; GRANT EXECUTE ON FUNCTION delete_user_account() TO authenticated;)",
    "(b) Verificar se as 7 tabelas possuem ON DELETE CASCADE vinculado a auth.users e, se não possuírem, a função deve apagar os dados explicitamente antes de deletar o usuário em auth.users",
    "(c) A exportação de dados locais deve ser realizada através do seletor de arquivos nativo do sistema (CreateDocument / SAF) ou compartilhamento direto (ShareSheet), SEM exigir permissão de armazenamento (READ/WRITE_EXTERNAL_STORAGE)",
    "(d) O agente não roda SQL no Supabase: entregar ao usuário o script SQL exato e o passo a passo completo de como executar no SQL Editor e como testar. Status até a execução pelo usuário: ⛔ BLOQUEADO",
    "PROIBIDO embutir chave service_role no app ou simular exclusão sem chamada real ao servidor"
  ],
  "files_expected": [
    "app/src/main/java/com/example/ui/screens/ProfileScreen.kt",
    "app/src/main/java/com/example/ui/GiroCustoViewModel.kt",
    "app/src/main/java/com/example/network/supabase/SupabaseClient.kt",
    "supabase/migrations/20261023_user_deletion_rpc.sql",
    "supabase/README.md"
  ],
  "out_of_scope": ["Exclusão automática de backups do Google Drive (gerenciado pelo usuário)"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
  ],
  "required_reviewers": ["seguranca", "revisor"],
  "depends_on": ["T-06"]
}
```

#### Tarefa T-11 [SEC-07]: Rationale Prévia para Acesso à Localização
```json
{
  "task_id": "T-11",
  "to_agent": "frontend",
  "task_type": "implementacao",
  "risk": "medio",
  "title": "Adicionar diálogo explicativo prévio antes de solicitar permissão de GPS",
  "context": "Ao clicar no botão de busca de postos na FuelRefillsScreen, o app dispara diretamente o launcher do sistema sem explicar o motivo.",
  "root_cause_evidence": "FuelRefillsScreen.kt:859-869",
  "acceptance_criteria": [
    "Diálogo em português simples explicando que a localização serve apenas para localizar postos próximos e calcular o menor custo de abastecimento, usando stringResource",
    "Solicitação de permissão do sistema disparada somente após o motorista clicar em 'Entendi' ou 'Permitir'",
    "Opção de dispensar o diálogo mantendo a busca manual por nome do posto ativa",
    "Entregar roteiro de teste manual no aparelho, passo a passo completo, para o usuário executar (sem confirmação no aparelho, status máximo 🟡)"
  ],
  "files_expected": [
    "app/src/main/java/com/example/ui/screens/FuelRefillsScreen.kt",
    "app/src/main/res/values/strings.xml"
  ],
  "out_of_scope": ["Alteração do LocationHelper ou lógica de cálculo do GPS"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
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
  "title": "Padronização de estados de carregamento, lista vazia, erro e offline",
  "context": "DashboardScreen, ReportsScreen e HistoryScreen deixam o usuário sem feedback quando vazias ou em erro.",
  "root_cause_evidence": "DashboardScreen.kt:510 e ReportsScreen.kt:99-109",
  "acceptance_criteria": [
    "Componente visual de estado Vazio (Empty State) informativo com botão de ação rápida (ex: 'Lançar primeira corrida'), com textos em stringResource",
    "Tratamento de estado de Erro com botão 'Tentar novamente'",
    "Banner informativo discreto quando o aplicativo opera offline (mantendo uso normal)",
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
    "./gradlew testDebugUnitTest lintDebug"
  ],
  "required_reviewers": ["designer", "revisor"],
  "depends_on": ["T-03"]
}
```

#### Tarefa T-13 [INT-02]: Sincronização de Timeouts do Overpass API
```json
{
  "task_id": "T-13",
  "to_agent": "integracoes",
  "task_type": "implementacao",
  "risk": "medio",
  "title": "Alinhar timeout do cliente OkHttp com a query do Overpass",
  "context": "OkHttp timeout é de 8 segundos em OverpassApiService, mas o cabeçalho da query pede timeout:25, gerando cancelamento prematuro em redes 4G instáveis.",
  "root_cause_evidence": "OverpassApiService.kt:22-23 e GasStationRepository.kt:13",
  "acceptance_criteria": [
    "connectTimeout e readTimeout no OkHttpClient do Overpass ajustados para 25 segundos",
    "Tratamento de SocketTimeoutException com fallback transparente para o próximo endpoint da lista",
    "Testes de integração com MockWebServer mantidos verdes"
  ],
  "files_expected": [
    "app/src/main/java/com/example/network/OverpassApiService.kt"
  ],
  "out_of_scope": ["Mudança dos endpoints públicos do Overpass"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
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
  "title": "Remover variáveis públicas 'test...' dos singletons de produção",
  "context": "GoogleAuthHelper, BiometricHelper, SupabaseClient, FipeNetworkClient, ViaCepNetworkClient e GiroCustoDatabase possuem variáveis @Volatile var test... públicas para acoplamento de testes.",
  "root_cause_evidence": "GoogleAuthHelper.kt:37, BiometricHelper.kt:27, SupabaseClient.kt:137, FipeNetworkClient.kt:41, ViaCepNetworkClient.kt:37 e GiroCustoDatabase.kt:203",
  "acceptance_criteria": [
    "Variáveis públicas de teste removidas dos objetos de produção",
    "Injeção das instâncias ou clientes fakes realizada através de construtores, factories ou visibilidade interna restrita a testes",
    "As asserções dos 159 testes existentes permanecem RIGOROSAMENTE IDÊNTICAS linha por linha (somente a criação dos objetos muda)",
    "Revisor independente confere o diff dos testes linha a linha confirmando que nenhuma asserção foi enfraquecida",
    "Entregar roteiro manual no aparelho: o usuário repete no aparelho o teste de login com Google e biometria após conclusão (sem teste real, status máximo 🟡)"
  ],
  "files_expected": [
    "app/src/main/java/com/example/util/GoogleAuthHelper.kt",
    "app/src/main/java/com/example/util/BiometricHelper.kt",
    "app/src/main/java/com/example/network/supabase/SupabaseClient.kt",
    "app/src/main/java/com/example/network/fipe/FipeNetworkClient.kt",
    "app/src/main/java/com/example/network/viacep/ViaCepNetworkClient.kt",
    "app/src/main/java/com/example/data/GiroCustoDatabase.kt",
    "app/src/test/java/com/example/AuthBiometricsAndViaCepTest.kt",
    "app/src/test/java/com/example/FipeNetworkAndViewModelTest.kt"
  ],
  "out_of_scope": ["Alteração da lógica das APIs reais"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
  ],
  "required_reviewers": ["arquiteto", "revisor"],
  "depends_on": []
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
  "context": "Foram encontradas 77 ocorrências de contentDescription = null. Ocorrências interativas (como o dropdown de postos em FuelRefillsScreen.kt:765) prejudicam usuários de TalkBack.",
  "root_cause_evidence": "FuelRefillsScreen.kt:765 e ocorrências mapeadas na auditoria",
  "acceptance_criteria": [
    "Todo IconButton, botão com ícone ou elemento com Modifier.clickable possui contentDescription descritivo em português utilizando stringResource",
    "Ícones puramente decorativos ao lado de textos permanentes podem manter contentDescription = null desde que o container forneça semântica",
    "TalkBack lê corretamente a ação de cada elemento interativo",
    "Entregar roteiro de teste manual no aparelho, passo a passo completo, para o usuário executar (sem confirmação no aparelho, status máximo 🟡)"
  ],
  "files_expected": [
    "app/src/main/java/com/example/ui/screens/FuelRefillsScreen.kt",
    "app/src/main/java/com/example/ui/screens/ProfileScreen.kt",
    "app/src/main/java/com/example/ui/screens/HistoryScreen.kt",
    "app/src/main/java/com/example/ui/screens/DashboardScreen.kt",
    "app/src/main/res/values/strings.xml"
  ],
  "out_of_scope": ["Redesenho de telas"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
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
  "title": "Mover cálculos matemáticos e regras de formatação das telas para o ViewModel",
  "context": "LaunchScreen, FuelRefillsScreen e ReportsScreen executam recálculo de médias, preços dinâmicos e custos no corpo de funções @Composable durante recomposição.",
  "root_cause_evidence": "LaunchScreen.kt:381-382, FuelRefillsScreen.kt:657-720 e ReportsScreen.kt:99-109",
  "acceptance_criteria": [
    "Cálculos de médias, totais de abastecimento e faturamento consolidado movidos para StateFlow ou funções de cálculo no ViewModel",
    "Composables atuam estritamente como renderizadores de estado desacoplados de regras pesadas",
    "Testes unitários adicionados para cada método de cálculo migrado para o ViewModel",
    "Textos novos utilizam stringResource",
    "Entregar roteiro de teste manual no aparelho, passo a passo completo, para o usuário executar (sem confirmação no aparelho, status máximo 🟡)"
  ],
  "files_expected": [
    "app/src/main/java/com/example/ui/screens/LaunchScreen.kt",
    "app/src/main/java/com/example/ui/screens/FuelRefillsScreen.kt",
    "app/src/main/java/com/example/ui/screens/ReportsScreen.kt",
    "app/src/main/java/com/example/ui/GiroCustoViewModel.kt"
  ],
  "out_of_scope": ["Mudança no visual das telas"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
  ],
  "required_reviewers": ["arquiteto", "revisor"],
  "depends_on": []
}
```

#### Tarefa T-17 [TST-01]: Substituição de Testes de Template por Testes de Regra Real
```json
{
  "task_id": "T-17",
  "to_agent": "qa",
  "task_type": "teste",
  "risk": "medio",
  "title": "Substituir testes de modelo sem valor por testes reais de regras de negócio",
  "context": "ExampleUnitTest (2+2), ExampleRobolectricTest, GreetingScreenshotTest e ExampleInstrumentedTest são templates gerados pelo assistente sem validação real.",
  "root_cause_evidence": "ExampleUnitTest.kt:14, ExampleRobolectricTest.kt:19 e GreetingScreenshotTest.kt:24",
  "acceptance_criteria": [
    "Antes de remover ou alterar qualquer teste existente, o agente deve PARAR e pedir autorização explícita do usuário no chat, listando os testes afetados. Não prosseguir sem ela",
    "GreetingScreenshotTest adaptado para capturar tela ou componente real (ex: DashboardSummaryCard) com Roborazzi",
    "ExampleUnitTest e ExampleRobolectricTest substituídos por testes unitários reais de validação de formulário ou formatação",
    "Total de testes úteis mantido ou expandido com asserções reais sobre a aplicação"
  ],
  "files_expected": [
    "app/src/test/java/com/example/ExampleUnitTest.kt",
    "app/src/test/java/com/example/ExampleRobolectricTest.kt",
    "app/src/test/java/com/example/GreetingScreenshotTest.kt"
  ],
  "out_of_scope": ["Remoção de qualquer teste funcional das outras suítes"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
  ],
  "required_reviewers": ["qa", "revisor"],
  "depends_on": []
}
```

---

### ONDA 3: Refatoração Estrutural, Padrões Monetários e Higienização

#### Tarefa T-18A [DAT-01A]: Diagnóstico e Planejamento da Migração Monetária
```json
{
  "task_id": "T-18A",
  "to_agent": "backend",
  "task_type": "diagnostico",
  "risk": "medio",
  "title": "Diagnosticar e mapear impacto da migração de Double para centavos",
  "context": "Valores monetários usam Double em diversas entidades, gerando riscos de arredondamento. É necessário mapear todos os pontos antes de alterar código.",
  "root_cause_evidence": "Entities.kt:19, 41, 65, 69, 71, 74, 191, 194, 214",
  "acceptance_criteria": [
    "Listar exaustivamente todo uso dos campos monetários no app (entidades Room, DTOs Supabase, ViewModels, DAOs e telas)",
    "Analisar o impacto nos relatórios e na sincronização com o Supabase (LWW)",
    "Entregar relatório técnico em docs/plano-migracao-monetaria.md com a proposta de migração para centavos (Long), sem alterar nenhuma linha de código",
    "Aguardar aprovação explícita do usuário antes de iniciar qualquer alteração de código"
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
  "title": "Implementar migração dos campos monetários para centavos (Long)",
  "context": "Execução da migração planejada em T-18A para eliminar erros de ponto flutuante em valores monetários.",
  "root_cause_evidence": "Entities.kt:19, 41, 65, 69, 71, 74, 191, 194, 214",
  "acceptance_criteria": [
    "Definição de modelo de centavos (Long) para entidades Room e métodos utilitários de conversão segura (CurrencyUtils.kt)",
    "Migração Room incremental e atualização dos DTOs e mappers do Supabase sem quebra de compatibilidade",
    "Arredondamento e formatação financeira com BigDecimal/Long garantindo que R$ 0,01 seja exato",
    "Testes unitários cobrindo soma, subtração e divisões de centavos sem resíduo de dízima binária"
  ],
  "files_expected": [
    "app/src/main/java/com/example/data/Entities.kt",
    "app/src/main/java/com/example/data/GiroCustoDatabase.kt",
    "app/src/main/java/com/example/network/supabase/SupabaseMappers.kt",
    "app/src/main/java/com/example/util/CurrencyUtils.kt",
    "app/src/main/java/com/example/ui/GiroCustoViewModel.kt"
  ],
  "out_of_scope": ["Conversão de odômetro e consumo médio (permanecem Double/Float)"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
  ],
  "required_reviewers": ["arquiteto", "backend", "revisor"],
  "depends_on": ["T-18A", "T-08"]
}
```

#### Tarefa T-19 [DAT-02]: Versionamento Futuro de Schemas e Bloqueio de Migração Destrutiva
```json
{
  "task_id": "T-19",
  "to_agent": "arquiteto",
  "task_type": "diagnostico",
  "risk": "baixo",
  "title": "Formalizar integridade de schemas Room v13+ e documentar histórico",
  "context": "Schemas 1, 4, 5, 6 e 7 não existem no Git e o app é de uso pessoal já na v12. É mandatório assegurar que todo schema futuro seja rigidamente versionado.",
  "root_cause_evidence": "app/schemas/com.example.data.GiroCustoDatabase/ (ausência de 1, 4, 5, 6, 7 no Git)",
  "acceptance_criteria": [
    "Documentação em docs/database-schemas.md explicando a evolução histórica 1->12",
    "Regra de checagem automatizada no Gradle/CI impedindo build se schemaVersion != arquivos json exportados",
    "Garantia absoluta de manutenção de exportSchema = true e proibição de fallbackToDestructiveMigration"
  ],
  "files_expected": [
    "docs/database-schemas.md",
    "app/src/main/java/com/example/data/GiroCustoDatabase.kt"
  ],
  "out_of_scope": ["Criação de schemas sintéticos retroativos para versões já superadas no aparelho"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
  ],
  "required_reviewers": ["arquiteto", "revisor"],
  "depends_on": ["T-08"]
}
```

#### Tarefa T-20 [ARQ-02]: Decomposição de Arquivos com Mais de 800 Linhas
```json
{
  "task_id": "T-20",
  "to_agent": "arquiteto",
  "task_type": "implementacao",
  "risk": "alto",
  "title": "Decompor ProfileScreen, FuelRefillsScreen, HistoryScreen e GiroCustoViewModel",
  "context": "Existem 9 arquivos acima de 800 linhas (com ProfileScreen em 2.480 linhas e ViewModel em 1.323 linhas), misturando responsabilidades de múltiplas telas.",
  "root_cause_evidence": "ProfileScreen.kt, FuelRefillsScreen.kt, HistoryScreen.kt e GiroCustoViewModel.kt",
  "acceptance_criteria": [
    "Quebra modular de ProfileScreen em seções isoladas (Veículos, Peças, Endereço, Fipe)",
    "Quebra de FuelRefillsScreen em lista, formulário e mapa",
    "Nenhum arquivo novo resultante acima de 800 linhas",
    "100% dos testes passam sem qualquer alteração de comportamento para o usuário",
    "Textos novos utilizam stringResource",
    "Entregar roteiro de teste manual no aparelho, passo a passo completo, para o usuário executar (sem confirmação no aparelho, status máximo 🟡)"
  ],
  "files_expected": [
    "app/src/main/java/com/example/ui/screens/ProfileScreen.kt",
    "app/src/main/java/com/example/ui/screens/FuelRefillsScreen.kt",
    "app/src/main/java/com/example/ui/screens/HistoryScreen.kt",
    "app/src/main/java/com/example/ui/GiroCustoViewModel.kt"
  ],
  "out_of_scope": ["Novas funcionalidades de interface"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
  ],
  "required_reviewers": ["arquiteto", "revisor"],
  "depends_on": ["T-05", "T-09", "T-16"]
}
```

#### Tarefa T-21 [ACC-02]: Migração Gradual de Textos para strings.xml
```json
{
  "task_id": "T-21",
  "to_agent": "acessibilidade",
  "task_type": "implementacao",
  "risk": "baixo",
  "title": "Migrar textos fixos das telas principais para strings.xml",
  "context": "O arquivo strings.xml só possui o app_name. Mais de 600 chamadas Text() contêm strings literais no código Kotlin.",
  "root_cause_evidence": "app/src/main/res/values/strings.xml:2 e telas em app/src/main/java/com/example/ui/screens",
  "acceptance_criteria": [
    "Strings de botões, diálogos, mensagens de erro e títulos de DashboardScreen e LaunchScreen migradas para strings.xml com stringResource",
    "Formatações monetárias e de data respeitando Locale pt-BR",
    "Nenhum texto visual cortado ou truncado em visualização normal",
    "Entregar roteiro de teste manual no aparelho, passo a passo completo, para o usuário executar (sem confirmação no aparelho, status máximo 🟡)"
  ],
  "files_expected": [
    "app/src/main/res/values/strings.xml",
    "app/src/main/java/com/example/ui/screens/DashboardScreen.kt",
    "app/src/main/java/com/example/ui/screens/LaunchScreen.kt"
  ],
  "out_of_scope": ["Telas secundárias não migradas nesta onda"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
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
  "context": "firebase.ai e firebase.appcheck.recaptcha estão no build.gradle.kts e GEMINI_API_KEY no .env.example sem nenhuma linha de código que os utilize.",
  "root_cause_evidence": "app/build.gradle.kts:114-115 e .env.example:1-2",
  "acceptance_criteria": [
    "Remoção de implementation(libs.firebase.ai) e appcheck do build.gradle.kts",
    "Limpeza das entradas no libs.versions.toml e .env.example",
    "Compilação do projeto e testes mantidos 100% verdes com redução no tamanho do APK"
  ],
  "files_expected": [
    "app/build.gradle.kts",
    "gradle/libs.versions.toml",
    ".env.example"
  ],
  "out_of_scope": ["Outras dependências ativas (Room, Retrofit, Moshi, Osmdroid)"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
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
  "title": "Remover cores e atributos não utilizados apontados pelo Lint",
  "context": "O Android Lint apontou 6 recursos de cor não utilizados em colors.xml herdados do template inicial do projeto.",
  "root_cause_evidence": "app/src/main/res/values/colors.xml:3-9 e lint-results-debug.txt:161-182",
  "acceptance_criteria": [
    "Remoção das cores purple_200, purple_500, purple_700, teal_200, teal_700, black, white não referenciadas do colors.xml",
    "Avisos de 'UnusedResources' eliminados no relatório do Lint"
  ],
  "files_expected": [
    "app/src/main/res/values/colors.xml"
  ],
  "out_of_scope": ["Alterações nas cores do tema Compose em Color.kt"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
  ],
  "required_reviewers": ["devops", "revisor"],
  "depends_on": []
}
```

#### Tarefa T-24 [ARQ-02B]: Quebra de Telas Secundárias (LaunchScreen, CloudSync, Platforms, Reports)
```json
{
  "task_id": "T-24",
  "to_agent": "arquiteto",
  "task_type": "implementacao",
  "risk": "alto",
  "title": "Modularizar LaunchScreen, CloudSyncScreen, PlatformsScreen e ReportsScreen",
  "context": "As 4 telas remanescentes ultrapassam o teto de 800 linhas (LaunchScreen com 1.242 linhas, CloudSync com 1.043, Platforms com 943, Reports com 910).",
  "root_cause_evidence": "LaunchScreen.kt, CloudSyncScreen.kt, PlatformsScreen.kt e ReportsScreen.kt",
  "acceptance_criteria": [
    "Extração de diálogos e cartões de métricas para arquivos de componentes reutilizáveis",
    "Todos os arquivos decompostos passam a respeitar o limite máximo de 800 linhas",
    "Textos novos utilizam stringResource",
    "Entregar roteiro de teste manual no aparelho, passo a passo completo, para o usuário executar (sem confirmação no aparelho, status máximo 🟡)",
    "Suíte completa de testes e lint executada com aprovação total"
  ],
  "files_expected": [
    "app/src/main/java/com/example/ui/screens/LaunchScreen.kt",
    "app/src/main/java/com/example/ui/screens/CloudSyncScreen.kt",
    "app/src/main/java/com/example/ui/screens/PlatformsScreen.kt",
    "app/src/main/java/com/example/ui/screens/ReportsScreen.kt"
  ],
  "out_of_scope": ["Alterações em ViewModels ou Repositórios"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
  ],
  "required_reviewers": ["arquiteto", "revisor"],
  "depends_on": ["T-20"]
}
```

#### Tarefa T-25 [ACC-02B]: Conclusão da Migração de Textos para strings.xml
```json
{
  "task_id": "T-25",
  "to_agent": "acessibilidade",
  "task_type": "implementacao",
  "risk": "baixo",
  "title": "Migrar textos remanescentes para strings.xml e formatadores pt-BR",
  "context": "Finalização da centralização de textos das telas de Histórico, Abastecimento, Perfil e Plataformas.",
  "root_cause_evidence": "Telas em app/src/main/java/com/example/ui/screens",
  "acceptance_criteria": [
    "Todas as strings estáticas das telas restantes centralizadas em strings.xml",
    "Avisos de HardcodedText reduzidos a zero nas telas afetadas",
    "Rótulos e placeholders permanentemente legíveis",
    "Entregar roteiro de teste manual no aparelho, passo a passo completo, para o usuário executar (sem confirmação no aparelho, status máximo 🟡)"
  ],
  "files_expected": [
    "app/src/main/res/values/strings.xml",
    "app/src/main/java/com/example/ui/screens/HistoryScreen.kt",
    "app/src/main/java/com/example/ui/screens/FuelRefillsScreen.kt",
    "app/src/main/java/com/example/ui/screens/PlatformsScreen.kt",
    "app/src/main/java/com/example/ui/screens/ProfileScreen.kt"
  ],
  "out_of_scope": ["Alterações estruturais de layout"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
  ],
  "required_reviewers": ["acessibilidade", "revisor"],
  "depends_on": ["T-21"]
}
```

---

## 4. Matriz de Rastreabilidade (25 Achados -> 26 Tarefas)

| ID Achado | Severidade Auditada | Onda | ID Tarefa | Responsável | Resumo da Ação |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **SEC-08** | Médio | Onda 1 | **T-01** | DevOps | Adicionar *.jks, *.keystore e google-services.json ao .gitignore |
| **SEC-02** | Alto (reaval.) | Onda 1 | **T-02** | Segurança | Excluir arquivos de login/biometria e secure_session do backup, preservando Room |
| **UI-01** | Crítico | Onda 1 | **T-03** | Frontend | Remover SmartphoneSimulator da produção em GiroCustoMainApp |
| **INT-01** | Alto | Onda 1 | **T-04** | Frontend | Ocultar/desabilitar botão com stub de login por SMS com stringResource |
| **SEC-03** | Alto | Onda 1 | **T-04** | Frontend/Segurança | Redigir Authorization, apikey e usar OkHttp Level.BASIC sem logar payloads |
| **UX-01** | Alto | Onda 1 | **T-05** | Frontend | Confirmação e Desfazer via soft delete atualizando updatedAt para proteger LWW |
| **SEC-01** | Alto (reaval.) | Onda 1 | **T-06** | Segurança | Criptografar tokens com Keystore (sem exigir digital a cada leitura) |
| **SEC-04** | Alto | Onda 1 | **T-06** | Segurança | Acoplar BiometricPrompt a CryptoObject real do Keystore e desenho em docs/ |
| **SEC-05** | Alto | Onda 1 | **T-07** | DevOps | Minificação R8 em release, buildType releaseCheck e regras ProGuard |
| **PERF-01** | Alto | Onda 2 | **T-08** | Backend | Índices no Room justificados por DAOs e migração v13 com runMigrationsAndValidate |
| **PERF-02** | Alto | Onda 2 | **T-09** | Frontend | Adicionar chaves estáveis key = { it.id } em todas as LazyColumns |
| **SEC-06** | Alto | Onda 2 | **T-10** | Segurança | Exportação SAF/ShareSheet e exclusão de conta via RPC SECURITY DEFINER (Bloqueado) |
| **SEC-07** | Médio | Onda 2 | **T-11** | Frontend | Diálogo prévio de rationale antes de solicitar permissão de GPS |
| **UX-02** | Médio | Onda 2 | **T-12** | Designer | Estados padronizados de Carregando, Vazio, Erro e Offline com stringResource |
| **INT-02** | Médio | Onda 2 | **T-13** | Integrações | Aumentar timeout do OkHttp para 25s na chamada da Overpass API |
| **ARQ-01** | Alto | Onda 2 | **T-14** | Arquiteto | Remover ganchos test... de produção mantendo asserções de teste idênticas |
| **ACC-01** | Alto | Onda 2 | **T-15** | Acessibilidade | Inserir contentDescription em ícones interativos via stringResource |
| **ARQ-03** | Médio | Onda 2 | **T-16** | Frontend | Mover cálculos matemáticos de regras de negócio para o ViewModel |
| **TST-01** | Médio | Onda 2 | **T-17** | QA | Pedir autorização explícita no chat antes de alterar testes legados |
| **DAT-01** | Alto | Onda 3 | **T-18A** | Backend | Diagnosticar e documentar impacto da migração monetária para centavos |
| **DAT-01** | Alto | Onda 3 | **T-18B** | Backend | Implementar migração monetária para centavos (Long) no Room e Supabase |
| **DAT-02** | Baixo (reaval.) | Onda 3 | **T-19** | Arquiteto | Documentar histórico de schemas e blindar integridade a partir da v13 |
| **ARQ-02** | Alto | Onda 3 | **T-20** | Arquiteto | Decompor ProfileScreen, FuelRefillsScreen, HistoryScreen e ViewModel (>800 linhas) |
| **ACC-02** | Baixo | Onda 3 | **T-21** | Acessibilidade | Migrar textos fixos de Dashboard e LaunchScreen para strings.xml |
| **ARQ-04** | Baixo | Onda 3 | **T-22** | DevOps | Remover dependências mortas (firebase.ai e GEMINI_API_KEY) |
| **DEV-01** | Baixo | Onda 3 | **T-23** | DevOps | Excluir cores órfãs de template detectadas pelo Lint no colors.xml |
| **ARQ-02B** | Alto | Onda 3 | **T-24** | Arquiteto | Decompor telas restantes (Launch, CloudSync, Platforms, Reports) (>800 linhas) |
| **ACC-02B** | Baixo | Onda 3 | **T-25** | Acessibilidade | Concluir migração de textos fixos de telas secundárias para strings.xml |

---

## 5. Parecer e Auditoria do Revisor Independente

O Revisor Independente inspecionou todas as seções deste plano e registrou os seguintes apontamentos e correções validadas:

1. **Confirmação de Ausência de Senhas em Disco:**
   - Conferido com busca no código-fonte. O app não salva senhas no SharedPreferences nem no Room. A reclassificação de SEC-01 e SEC-02 de *Crítico* para *Alto* está plenamente justificada e foi homologada.
2. **Correção do Placar por Área:**
   - O placar anterior continha erro material de contagem. Foi retificado para exatamente 8 achados em B (Segurança) e 4 achados em D (UX/Acessibilidade), totalizando rigorosamente os 25 achados.
3. **Divisão e Atomicidade:**
   - A divisão da antiga T-005 em T-08 (Backend / Índices Room) e T-09 (Frontend / Chaves Compose) e a divisão de T-18 em T-18A (Diagnóstico e Plano) e T-18B (Implementação) foram homologadas, eliminando tarefas sobrecarregadas.
4. **Comando de Verificação Global:**
   - Confirmado que 100% das 26 tarefas exigem `./gradlew testDebugUnitTest lintDebug`. Não há brecha para comprovação parcial.
5. **Critérios de Rigor e Não-Regressão:**
   - A exigência de manter as asserções de teste idênticas em T-14 e a obrigação de pedir autorização explícita no chat em T-17 garantem blindagem contra regressões silenciosas.
   - O uso de `runMigrationsAndValidate` em T-08 garante que índices inconsistentes não passem despercebidos.

---

## 6. Adendos v2.1 (Registro das Alterações Homologadas)

Esta seção documenta individualmente cada alteração e refinamento incorporado ao plano nesta versão 2.1:

1. **Correção de ID da Tarefa:**
   - O identificador `"task_id": "T-014"` foi formalmente corrigido para `"task_id": "T-14"`.
2. **Exigência de Roteiro Manual para Mudanças de UI (Status 🟡):**
   - Para todas as tarefas que alteram a interface (T-03, T-04, T-05, T-11, T-12, T-15, T-16, T-20, T-21, T-24 e T-25), foi acrescentado ao critério de aceite a entrega de roteiro de teste manual no aparelho, passo a passo completo, para execução pelo usuário. Sem a confirmação do usuário no aparelho real, o status máximo da entrega é estritamente 🟡 **IMPLEMENTADO, NÃO VERIFICADO NO APARELHO**.
3. **Higienização Avançada de Logs HTTP (T-04):**
   - O nível do `HttpLoggingInterceptor` foi rebaixado de `Level.BODY` para `Level.BASIC` em modo Debug. Dessa forma, nenhum corpo de requisição (contendo senhas ou dados cadastrais) é impresso no Logcat, mantendo-se a redação obrigatória dos cabeçalhos `Authorization` e `apikey`.
4. **Proteção LWW no Desfazer e Teste Automatizado (T-05):**
   - No fluxo de "Desfazer" exclusão de peças e veículos, além de reverter `isDeleted = false` e marcar `syncStatus = PENDING_UPDATE`, foi tornado obrigatório atualizar o campo `updatedAt` com `System.currentTimeMillis()`. Isso impede que uma sincronização subsequente com Supabase interprete o registro como desatualizado e reaplique a deleção remota. Foi incluída a obrigatoriedade de teste automatizado cobrindo: excluir -> sincronizar -> desfazer -> sincronizar.
5. **Exclusão de Backup para Armazenamento Futuro (T-02 e T-06):**
   - Em T-02, além de `giro_custo_supabase_auth.xml` e `giro_custo_biometrics.xml`, foi incluído preventivamente o arquivo `giro_custo_secure_session.xml`, que será gerado na T-06. O banco Room permanece preservado no backup (qualquer exclusão futura do Room exigirá consulta prévia ao usuário).
6. **Projeto de Arquitetura de Sessão e Biometria (T-06):**
   - Adicionada a obrigatoriedade de entrega prévia de documento de arquitetura de 1 página em `docs/design-sessao-segura.md` pelo Arquiteto antes de codificar.
   - Definido que a chave dos tokens não exigirá autenticação do usuário a cada leitura para não travar a sincronização em segundo plano.
   - Definido tratamento de chave invalidada (ex: inclusão de nova biometria no sistema operacional ou restauração em outro aparelho) levando o usuário à tela de login normal, sem apagar dados locais do Room e sem crash do app.
   - Obrigatoriedade de apagar o `SharedPreferences` antigo em texto puro após migração bem-sucedida e criação de testes de migração, chave invalidada e dados corrompidos.
7. **Validação R8 com buildType releaseCheck (T-07):**
   - Criado o requisito do buildType `releaseCheck` (`initWith release`, minificado com R8, assinado com chave de debug).
   - Verificação via terminal com `./gradlew assembleReleaseCheck`.
   - Entrega de roteiro manual cobrindo os fluxos principais (login email/Google, lançamento, sincronização, postos, FIPE e banco Room). Sem teste no aparelho, status máximo é 🟡. A chave de release real fica fora da tarefa.
8. **Justificativa Estrita de Índices e Validação Room (T-08):**
   - Cada índice proposto foi estritamente vinculado à consulta real existente em `Daos.kt` (citando arquivo e linha). Índices sem consulta e índices órfãos em `syncStatus` foram descartados.
   - Obrigatoriedade de teste com `runMigrationsAndValidate` do `MigrationTestHelper` para garantir conformidade de nomenclatura com o compilador do Room.
9. **Segurança Reforçada na Exclusão LGPD (T-10):**
   - A função PostgreSQL no Supabase deve ser `SECURITY DEFINER` com `SET search_path = public` e concedida unicamente ao papel `authenticated` (revogando acesso de `anon` e `public`).
   - Verificação mandatória de integridade referencial `ON DELETE CASCADE` para `auth.users`.
   - Exportação de dados locais via SAF (Storage Access Framework) ou ShareSheet nativo sem pedir permissões intrusivas de armazenamento.
   - Como o agente não executa comandos no console do Supabase, o status da tarefa é mantido como ⛔ **BLOQUEADO** até que o usuário execute o script SQL no SQL Editor do painel remoto.
10. **Parada Mandatória e Consulta ao Usuário em Testes (T-17):**
    - Antes de alterar ou remover qualquer teste legado (mesmo templates como 2+2), o agente deve PARAR a execução e solicitar autorização explícita por escrito do usuário no chat, listando os testes afetados.
11. **Bifurcação da Migração Monetária (T-18A e T-18B):**
    - Dividida a tarefa T-18 em duas etapas atômicas: T-18A (diagnóstico aprofundado, análise de impacto em relatórios/sync e entrega de plano em `docs/plano-migracao-monetaria.md` sem alteração de código) e T-18B (implementação da migração monetária para `Long` nos models e Room após aprovação do usuário).
12. **Padronização Global de Textos em stringResource:**
    - Explicitada a exigência de que qualquer texto novo inserido nas tarefas de interface e acessibilidade (inclusive rótulos de botões e `contentDescription`) utilize `stringResource` vinculado a `strings.xml`.
13. **Protocolo Operacional de Execução:**
    - Estabelecido que as tarefas serão executadas estritamente uma por vez, gerando um commit individual por tarefa após aprovação total da suíte completa de testes e lint.
    - Após a conclusão de T-06, T-07 e T-14, o usuário repetirá obrigatoriamente os testes no aparelho físico real para validar autenticação e biometria fora do ambiente de mocks.

---
*Plano de Correção v2.1 concluído, auditado e pronto para execução estrita sob autorização do usuário.*
