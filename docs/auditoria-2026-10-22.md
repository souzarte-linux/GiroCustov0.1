# Relatório de Auditoria Completa do Projeto GiroCusto
**Data:** 22 de Outubro de 2026  
**Auditor Responsável:** Equipe de Especialistas (Orquestrador, Arquiteto, Backend, Designer, QA, DevOps, Segurança, Integrações, Acessibilidade, Performance e Revisor Independente)  
**Escopo da Tarefa:** Auditoria estrita em modo somente leitura contra os padrões de engenharia, integridade de testes, segurança e regras da equipe.

---

## 1. Resumo para Leigo (Estado Real do Aplicativo)

O GiroCusto possui uma base de código funcional e com uma suíte de 159 testes unitários aprovados, demonstrando cálculo correto de diárias, consumo de combustível e sincronização com Supabase (Last-Write-Wins). No entanto, o aplicativo apresenta falhas graves de segurança que impedem seu lançamento seguro em produção: credenciais e tokens de acesso à nuvem estão salvos em texto puro sem criptografia, e o recurso de backup do Android está aberto, permitindo que qualquer pessoa com acesso físico ao aparelho ou cabo USB copie a sessão e o banco de dados. Além disso, existe um simulador visual de celular no código principal que deforma e encolhe o aplicativo em tablets, aparelhos dobráveis e telas em modo paisagem, travando a tela em 390 pixels. Foi identificado também um botão de login por SMS visível ao motorista que é falso (exibe mensagem de "homologação"). Em termos de usabilidade para entregadores na rua, faltam avisos de confirmação ao apagar veículos e peças (o que pode causar perda acidental de dados com luvas ou trepidação), telas não informam quando estão offline e quase todos os textos estão fixos no código em vez de centralizados. Por fim, o banco de dados local não possui índices de busca, tornando o aplicativo progressivamente lento à medida que o histórico de trabalho cresce.

---

## 2. Placar Geral da Auditoria

### 2.1 Por Severidade
| Severidade | Quantidade | Descrição do Risco |
| :--- | :---: | :--- |
| 🔴 **Crítico** | **3** | Risco imediato de vazamento de credenciais, perda de dados ou quebra estrutural de visualização em dispositivos reais. |
| 🟠 **Alto** | **13** | Violação direta das regras do projeto, falhas de segurança local, falta de conformidade legal (LGPD), stubs na interface ou lentidão de dados. |
| 🟡 **Médio** | **6** | Tratamento incompleto de estados visuais, inconsistências de timeout de rede, dependências desnecessárias ou ausência de testes de migrations antigas. |
| 🔵 **Baixo** | **3** | Textos fixos não migrados para `strings.xml`, avisos de lint de recursos sem uso ou dependências desatualizadas. |
| **TOTAL** | **25** | **Achados identificados e comprovados com evidência no código.** |

### 2.2 Por Área Técnica
| Área Técnica | Crítico | Alto | Médio | Baixo | Total |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **A. Integridade e Funcionalidades Falsas** | 0 | 2 | 1 | 0 | **3** |
| **B. Segurança e Privacidade** | 2 | 4 | 1 | 0 | **7** |
| **C. Dados e Arquitetura** | 1 | 3 | 2 | 0 | **6** |
| **D. UX/UI e Acessibilidade** | 0 | 2 | 1 | 2 | **5** |
| **E. Integrações e Performance** | 0 | 2 | 1 | 1 | **4** |
| **TOTAL** | **3** | **13** | **6** | **3** | **25** |

---

## 3. Tabela de Achados (Ordenada por Severidade e Risco)

| ID | Severidade | Área | Arquivo:Linha | Evidência no Código | Impacto para o Usuário | Correção Recomendada | Esforço | Risco Regressão |
| :--- | :---: | :---: | :--- | :--- | :--- | :--- | :---: | :---: |
| **SEC-01** | 🔴 Crítico | B | `SupabaseClient.kt:20, 68-81` | `prefs?.edit()?.putString(KEY_ACCESS_TOKEN, session.accessToken)` | Se o aparelho for roubado ou infectado por app malicioso, o token de login na nuvem pode ser lido em texto puro, permitindo roubo de conta. | Migrar a persistência de sessão para Android Keystore com criptografia AES-GCM em arquivo protegido. | M | Baixo |
| **SEC-02** | 🔴 Crítico | B | `AndroidManifest.xml:10-12` e `data_extraction_rules.xml:8` | `android:allowBackup="true"` e `<cloud-backup><!-- TODO: Use <include>... --></cloud-backup>` | O backup padrão do Android copia senhas, tokens e dados bancários para a nuvem da Google ou extração local via `adb backup` por USB sem proteção. | Configurar regras de exclusão estritas em `data_extraction_rules.xml` e `backup_rules.xml` excluindo SharedPreferences de auth e banco Room. | P | Baixo |
| **UI-01** | 🔴 Crítico | C | `GiroCustoMainApp.kt:118` e `SmartphoneSimulator.kt:37-53` | `if (isDesktop) { Box(modifier = Modifier.width(390.dp).height(820.dp)...) }` | Se o motorista usar tablet, celular dobrável ou colocar o celular no suporte na horizontal (paisagem > 480dp), o app encolhe para uma caixinha minúscula preta inútil. | Remover o `SmartphoneSimulator` do fluxo principal de produção em `GiroCustoMainApp.kt`, deixando-o estritamente em `@Preview`. | P | Baixo |
| **INT-01** | 🟠 Alto | A | `GiroCustoViewModel.kt:486-487` e `AuthGateScreen.kt:616, 644` | `_authUiState.value = AuthUiState.Error("Login por SMS em homologação.")` | O entregador clica no botão "Entrar com Celular/SMS" e o app dá erro dizendo que está em homologação. Viola regra de proibição de stubs acionáveis. | Desabilitar visualmente o botão com rótulo "Em breve" ou ocultar o formulário de SMS até que o serviço de telefonia esteja integrado. | P | Baixo |
| **SEC-03** | 🟠 Alto | B | `SupabaseClient.kt:105-107` | `HttpLoggingInterceptor().apply { level = if (BuildConfig.DEBUG) Level.BODY else Level.NONE }` | Em versões de teste ou ao conectar o celular no computador, as senhas digitadas e os tokens trafegados são impressos abertamente no Logcat do Android. | Adicionar `.redactHeader("Authorization")` e `.redactHeader("apikey")`, e não logar corpos que contenham senhas. | P | Baixo |
| **SEC-04** | 🟠 Alto | B | `BiometricHelper.kt:24, 62-72` | `putString(KEY_BIOMETRIC_EMAIL, email)` e `showBiometricPrompt` sem `CryptoObject` | A biometria é meramente visual: um aplicativo invasor ou modificação de memória pode burlar a trava, pois ela não descriptografa nenhuma chave criptográfica real. | Utilizar `BiometricPrompt` associado a `BiometricPrompt.CryptoObject` com chave gerada no Android Keystore. | M | Médio |
| **SEC-05** | 🟠 Alto | B | `app/build.gradle.kts:45` e `proguard-rules.pro:1-22` | `isMinifyEnabled = false` e arquivo `proguard-rules.pro` vazio | O código compilado para produção fica desprotegido contra engenharia reversa e o pacote APK fica muito maior do que o necessário. | Ativar `isMinifyEnabled = true` no build de release e cadastrar as regras R8 de Room, Moshi e Retrofit. | M | Médio |
| **SEC-06** | 🟠 Alto | B | Código geral do app (ausente) | `grep -rniE "export|excluir.*conta" app/src/` (0 resultados) | O usuário não consegue exportar seu histórico financeiro nem solicitar a exclusão de sua conta e dados na nuvem, violando a LGPD. | Criar fluxo de exportação de dados (CSV/JSON) e botão de exclusão permanente de conta com expurgo no Supabase. | G | Médio |
| **ARQ-01** | 🟠 Alto | A | `GoogleAuthHelper.kt:37`, `BiometricHelper.kt:27`, `SupabaseClient.kt:137, 139`, `FipeNetworkClient.kt:41`, `ViaCepNetworkClient.kt:37`, `GiroCustoDatabase.kt:203` | `@Volatile var testAuthHandler: ... = null`, `@Volatile var testHardwareAvailable: ... = null` | Código de produção possui variáveis públicas mutáveis que alteram o comportamento real da aplicação para simular testes. Viola regra de separação. | Remover variáveis `test...` públicas de produção; utilizar injeção de dependência ou instâncias de teste limpas. | M | Médio |
| **DAT-01** | 🟠 Alto | C | `Entities.kt:19, 41, 65, 69, 71, 74, 191, 194, 214` | `val grossEarnings: Double`, `val netProfit: Double`, `val pricePerLiter: Double` | Cálculos com `Double` sofrem de imprecisão binária (ex: centavos sumindo ou frações como R$ 0,0000000004), distorcendo faturamento e metas. | Planejar migração de campos financeiros para centavos em `Long` ou `BigDecimal`, mantendo compatibilidade de migração Room. | G | Alto |
| **DAT-02** | 🟠 Alto | C | `app/schemas` e `GiroCustoDatabase.kt:28-187` | Schemas 1, 4, 5, 6, 7 ausentes em `app/schemas/` | Se um usuário estiver com uma versão antiga do app instalada, a migração não foi validada contra o schema original pelo `MigrationTestHelper`. | Exportar e versionar schemas históricos completos de 1 a 12 e adicionar testes com `MigrationTestHelper` cobrindo toda a cadeia 1->12. | M | Baixo |
| **ARQ-02** | 🟠 Alto | C | `ProfileScreen.kt` (2.480 linhas), `FuelRefillsScreen.kt` (1.611), `HistoryScreen.kt` (1.370), `GiroCustoViewModel.kt` (1.323), `LaunchScreen.kt` (1.242), `CloudSyncScreen.kt` (1.043), `PlatformsScreen.kt` (943), `ReportsScreen.kt` (910), `DashboardScreen.kt` (861) | 9 arquivos excedendo o limite de 800 linhas | Manutenção arriscada: telas realizam validações, formatações, consumo de rede e persistência no mesmo arquivo, propiciando bugs. | Decompor arquivos em subcomposables e delegar regras de negócio aos ViewModels/UseCases dedicados. | G | Médio |
| **UX-01** | 🟠 Alto | D | `ProfileScreen.kt:240-244, 261-265` | `onDeleteVehicle = { vehicle -> viewModel.deleteVehicle(vehicle); Toast... }` | Se o motorista estiver na rua e esbarrar acidentalmente no botão de excluir veículo ou peça, o registro é apagado na hora sem confirmação e sem chance de desfazer. | Adicionar diálogo de confirmação com `AlertDialog` e `Snackbar` com botão "Desfazer". | P | Baixo |
| **ACC-01** | 🟠 Alto | D | `FuelRefillsScreen.kt:765` e outros 76 pontos | `IconButton(...) { Icon(..., contentDescription = null) }` | Usuários com deficiência visual usando o TalkBack ouvem apenas "botão não rotulado", sem saber o que a ação faz. | Inserir rótulos acessíveis descritivos em todos os botões e ícones interativos via `contentDescription`. | M | Baixo |
| **PERF-01** | 🟠 Alto | E | `Entities.kt:13, 36, 59, 161, 183, 206` | `@Entity(tableName = "daily_records")` (sem `indices = [...]`) | Nenhuma tabela do Room tem índice. Toda consulta por data, veículo ou status de sincronização varre o banco inteiro, travando a interface em bancos grandes. | Adicionar `@Index` nas colunas de filtro frequente (`dateTimestamp`, `vehicleId`, `syncStatus`, `remoteId`). | M | Médio |
| **PERF-02** | 🟠 Alto | E | `MaintenanceScreen.kt:255`, `PlatformsScreen.kt:196`, `HistoryScreen.kt:393, 440`, `FuelRefillsScreen.kt:300` | `items(records) { record -> ... }` (sem chave estável) | Ao rolar listas longas ou adicionar um novo abastecimento, a tela engasga e pisca porque o Compose reconstrói a lista inteira do zero. | Adicionar parâmetro estável `key = { it.id }` em todas as chamadas de `items(...)`. | P | Baixo |
| **UX-02** | 🟡 Médio | D | `DashboardScreen.kt`, `ReportsScreen.kt`, `HistoryScreen.kt` | Telas sem estados declarados de Carregando / Erro / Offline | Se houver demora de leitura no banco ou erro, o usuário vê telas em branco sem saber o que aconteceu ou se a internet caiu. | Implementar estados visuais padronizados (Loading, Vazio amigável com ação, Erro com retry e Offline banner). | M | Baixo |
| **SEC-07** | 🟡 Médio | B | `FuelRefillsScreen.kt:859-869` | `permissionLauncher.launch(permission)` sem rationale | O app pede acesso à localização imediatamente sem explicar ao entregador que é para encontrar o posto mais barato próximo dele, gerando recusa. | Exibir diálogo prévio explicando o benefício da localização antes de disparar o pedido do sistema. | P | Baixo |
| **INT-02** | 🟡 Médio | E | `OverpassApiService.kt:22-23` e `GasStationRepository.kt:13` | OkHttp timeout = 8 segundos, mas a query do Overpass pede `[timeout:25]` | Em redes móveis lentas na rua, a busca de postos falhará aos 8 segundos por timeout do app, mesmo que o servidor ainda fosse responder. | Sincronizar o timeout do OkHttp para no mínimo 25 segundos na chamada do Overpass. | P | Baixo |
| **ARQ-03** | 🟡 Médio | C | `LaunchScreen.kt:381-382`, `FuelRefillsScreen.kt:657-720`, `ReportsScreen.kt:99-109` | Cálculos de médias, custos de peças e recálculo de preços dentro dos Composables | A interface recalcula regras financeiras pesadas a cada toque na tela ou abertura de teclado, gastando processamento e bateria desnecessários. | Mover toda a lógica de cálculo de totais e projeções para dentro do `GiroCustoViewModel`. | M | Baixo |
| **TST-01** | 🟡 Médio | A | `ExampleUnitTest.kt`, `ExampleRobolectricTest.kt`, `GreetingScreenshotTest.kt`, `ExampleInstrumentedTest.kt` | `assertEquals(4, 2 + 2)` e `Text("GiroCusto")` | Testes de modelo sem valor para a aplicação inflam a contagem de testes sem proteger nenhuma regra de negócio real. | Remover ou substituir por testes reais que validem regras do app. | P | Baixo |
| **SEC-08** | 🟡 Médio | B | `.gitignore:1-19` | Falta exclusão genérica de `*.jks`, `*.keystore` e `google-services.json` | Risco de um desenvolvedor acidentalmente comitar a chave de publicação do app ou arquivo de configuração no GitHub. | Incluir regras completas no `.gitignore` para arquivos de credenciais e certificados. | P | Baixo |
| **ACC-02** | 🔵 Baixo | D | Todas as telas em `app/src/main/java/com/example/ui/screens` | Mais de 600 ocorrências de `Text("...")` sem `stringResource` | Impossibilita internacionalização, dificulta correções ortográficas e viola a convenção Android. | Migrar gradativamente os textos para `strings.xml`. | M | Baixo |
| **ARQ-04** | 🔵 Baixo | C | `app/build.gradle.kts:114-115` e `.env.example:1-2` | `implementation(libs.firebase.ai)` e `GEMINI_API_KEY` sem uso | Dependências mortas incluídas no aplicativo aumentam o tamanho final do APK sem entregar valor. | Remover dependências e variáveis de ambiente não utilizadas ou documentar sua futura integração. | P | Baixo |
| **DEV-01** | 🔵 Baixo | E | `app/src/main/res/values/colors.xml:3-9` | Cores `purple_200`, `teal_200`, etc. sem uso (detectado pelo Lint) | Resquícios do template inicial do Android Studio poluindo os recursos do projeto. | Remover recursos não utilizados indicados no relatório de Lint. | P | Baixo |

---

## 4. Plano de Correção em Ondas (Formato de Tarefas da Equipe)

O plano foi estruturado em 3 ondas sequenciais e atômicas. Cada tarefa possui critérios verificáveis e escopo delimitado.

### 4.1 ONDA 1: Correções Críticas e de Alto Risco (Segurança, Usabilidade Crítica e Integridade)

#### Tarefa T-001 (Segurança de Sessão e Backups)
```json
{
  "task_id": "T-001",
  "to_agent": "seguranca",
  "task_type": "seguranca",
  "risk": "alto",
  "title": "Criptografia de sessão e isolamento de backup",
  "context": "Tokens de autenticação estão em SharedPreferences sem criptografia e allowBackup expõe credenciais.",
  "root_cause_evidence": "SupabaseClient.kt:20, 68-81 e AndroidManifest.xml:10",
  "acceptance_criteria": [
    "Tokens de sessão gravados através de armazenamento seguro com Keystore",
    "data_extraction_rules.xml e backup_rules.xml excluem explicitamente SharedPreferences e bancos de dados",
    "Testes unitários de restauração e limpeza de sessão continuam 100% aprovados"
  ],
  "files_expected": [
    "app/src/main/java/com/example/network/supabase/SupabaseClient.kt",
    "app/src/main/res/xml/data_extraction_rules.xml",
    "app/src/main/res/xml/backup_rules.xml"
  ],
  "out_of_scope": ["Refatoração de telas ou mudanças na API do Supabase"],
  "verification": [
    "./gradlew testDebugUnitTest --tests \"com.example.SupabaseGoogleAuthMockWebServerTest*\"",
    "Verificação de leitura segura de tokens em teste automatizado"
  ],
  "required_reviewers": ["revisor", "arquiteto"],
  "depends_on": []
}
```

#### Tarefa T-002 (Remoção do Simulador de Celular da Produção)
```json
{
  "task_id": "T-002",
  "to_agent": "frontend",
  "task_type": "implementacao",
  "risk": "alto",
  "title": "Remoção do SmartphoneSimulator do fluxo principal",
  "context": "SmartphoneSimulator força a tela em 390dp x 820dp quando maxWidth > 480dp, quebrando renderização em tablets e celulares na horizontal.",
  "root_cause_evidence": "GiroCustoMainApp.kt:118 e SmartphoneSimulator.kt:37",
  "acceptance_criteria": [
    "GiroCustoMainApp renderiza diretamente o Scaffold adaptável ocupando toda a tela",
    "SmartphoneSimulator restringe-se a arquivos de @Preview sem interferir na UI de produção"
  ],
  "files_expected": [
    "app/src/main/java/com/example/ui/navigation/GiroCustoMainApp.kt"
  ],
  "out_of_scope": ["Alterações de paleta de cores ou navegação"],
  "verification": [
    "./gradlew testDebugUnitTest",
    "Inspeção visual em emulador tablet ou rotação de tela"
  ],
  "required_reviewers": ["designer", "revisor"],
  "depends_on": []
}
```

#### Tarefa T-003 (Tratamento de Stub de SMS e Redação de Logs)
```json
{
  "task_id": "T-003",
  "to_agent": "frontend",
  "task_type": "implementacao",
  "risk": "alto",
  "title": "Ocultação de stub de SMS e higienização de logs HTTP",
  "context": "signInWithPhone retorna erro de homologação e interceptor HTTP loga tokens e senhas no Logcat.",
  "root_cause_evidence": "GiroCustoViewModel.kt:486 e SupabaseClient.kt:105-107",
  "acceptance_criteria": [
    "Botão de login por SMS na AuthGateScreen desabilitado ou oculto com rótulo 'Em breve'",
    "HttpLoggingInterceptor em modo Debug configurado com redactHeader('Authorization') e redactHeader('apikey')",
    "Nenhuma senha é impressa em texto plano no Logcat"
  ],
  "files_expected": [
    "app/src/main/java/com/example/ui/screens/AuthGateScreen.kt",
    "app/src/main/java/com/example/network/supabase/SupabaseClient.kt"
  ],
  "out_of_scope": ["Implementação completa de provedor SMS Twilio/Supabase"],
  "verification": [
    "./gradlew testDebugUnitTest --tests \"com.example.AuthBiometricsAndViaCepTest*\""
  ],
  "required_reviewers": ["seguranca", "revisor"],
  "depends_on": []
}
```

#### Tarefa T-004 (Confirmação em Exclusões e Proteção contra Toque Acidental)
```json
{
  "task_id": "T-004",
  "to_agent": "frontend",
  "task_type": "implementacao",
  "risk": "alto",
  "title": "Diálogo de confirmação e Desfazer em ações destrutivas",
  "context": "Exclusão de veículos e peças na ProfileScreen ocorre instantaneamente sem confirmação prévia.",
  "root_cause_evidence": "ProfileScreen.kt:241 e 262",
  "acceptance_criteria": [
    "Diálogo AlertDialog solicitando confirmação explícita antes de apagar veículo ou peça",
    "Snackbar com opção 'Desfazer' exibida logo após a confirmação"
  ],
  "files_expected": [
    "app/src/main/java/com/example/ui/screens/ProfileScreen.kt"
  ],
  "out_of_scope": ["Refatoração de outras partes da ProfileScreen"],
  "verification": [
    "./gradlew testDebugUnitTest"
  ],
  "required_reviewers": ["designer", "revisor"],
  "depends_on": []
}
```

#### Tarefa T-005 (Índices nas Tabelas Room e Chaves Estáveis em Listas)
```json
{
  "task_id": "T-005",
  "to_agent": "backend",
  "task_type": "implementacao",
  "risk": "alto",
  "title": "Criação de índices no Room e adição de keys estáveis em LazyColumn",
  "context": "Tabelas Room sem índices geram varreduras lentas e LazyColumn sem key gera recomposições pesadas.",
  "root_cause_evidence": "Entities.kt:13-220 e MaintenanceScreen.kt:255",
  "acceptance_criteria": [
    "Entidades Room com @Index em dateTimestamp, vehicleId, syncStatus e remoteId",
    "Migração Room 12->13 implementada e testada com MigrationTestHelper",
    "Todas as LazyColumn com parâmetro key = { it.id }"
  ],
  "files_expected": [
    "app/src/main/java/com/example/data/Entities.kt",
    "app/src/main/java/com/example/data/GiroCustoDatabase.kt",
    "app/src/main/java/com/example/ui/screens/MaintenanceScreen.kt",
    "app/src/main/java/com/example/ui/screens/PlatformsScreen.kt",
    "app/src/main/java/com/example/ui/screens/HistoryScreen.kt",
    "app/src/main/java/com/example/ui/screens/FuelRefillsScreen.kt"
  ],
  "out_of_scope": ["Alterações de schema remoto no Supabase"],
  "verification": [
    "./gradlew testDebugUnitTest"
  ],
  "required_reviewers": ["arquiteto", "performance", "revisor"],
  "depends_on": []
}
```

---

### 4.2 ONDA 2: Confiabilidade, UX e Conformidade Legal

#### Tarefa T-006 (Conformidade LGPD: Exportação e Exclusão de Conta)
```json
{
  "task_id": "T-006",
  "to_agent": "seguranca",
  "task_type": "implementacao",
  "risk": "alto",
  "title": "Implementação dos direitos LGPD de portabilidade e exclusão",
  "context": "Aplicativo não oferece opção de exportar histórico nem apagar os dados do usuário.",
  "root_cause_evidence": "Ausência de endpoints/funções em ViewModel e UI",
  "acceptance_criteria": [
    "Opção 'Exportar Meus Dados' gerando relatório CSV local",
    "Opção 'Excluir Minha Conta' com confirmação dupla apagando dados locais e disparando exclusão na nuvem"
  ],
  "files_expected": [
    "app/src/main/java/com/example/ui/screens/ProfileScreen.kt",
    "app/src/main/java/com/example/ui/GiroCustoViewModel.kt",
    "app/src/main/java/com/example/network/supabase/SupabaseClient.kt"
  ],
  "out_of_scope": ["Alteração das regras de negócio de cálculo"],
  "verification": [
    "./gradlew testDebugUnitTest"
  ],
  "required_reviewers": ["seguranca", "revisor"],
  "depends_on": ["T-001"]
}
```

#### Tarefa T-007 (Padronização de Estados Visuais e Rationale de Localização)
```json
{
  "task_id": "T-007",
  "to_agent": "designer",
  "task_type": "implementacao",
  "risk": "medio",
  "title": "Tratamento de estados e diálogo prévio para GPS",
  "context": "Telas não tratam carregamento e erro; permissão de GPS é requisitada sem explicação ao entregador.",
  "root_cause_evidence": "DashboardScreen.kt:510 e FuelRefillsScreen.kt:860",
  "acceptance_criteria": [
    "Diálogo explicativo em português simples antes de solicitar permissão de localização",
    "Componentes de estado Vazio amigável e Erro com botão 'Tentar novamente' no Dashboard e Relatórios"
  ],
  "files_expected": [
    "app/src/main/java/com/example/ui/screens/DashboardScreen.kt",
    "app/src/main/java/com/example/ui/screens/ReportsScreen.kt",
    "app/src/main/java/com/example/ui/screens/FuelRefillsScreen.kt"
  ],
  "out_of_scope": ["Cálculos matemáticos de relatórios"],
  "verification": [
    "./gradlew testDebugUnitTest"
  ],
  "required_reviewers": ["acessibilidade", "revisor"],
  "depends_on": ["T-002"]
}
```

#### Tarefa T-008 (Sincronização de Timeouts e Remoção de Test Hooks em Produção)
```json
{
  "task_id": "T-008",
  "to_agent": "arquiteto",
  "task_type": "diagnostico",
  "risk": "medio",
  "title": "Higienização de singletons e alinhamento de timeout Overpass",
  "context": "Variáveis públicas 'testApi' poluem singletons de produção e timeout de 8s aborta queries de 25s.",
  "root_cause_evidence": "GoogleAuthHelper.kt:37 e OverpassApiService.kt:22",
  "acceptance_criteria": [
    "Remoção de variáveis @Volatile var test... em código de produção",
    "Timeout de OkHttp do Overpass ajustado para 25 segundos",
    "Testes unitários atualizados para injetar fakes via construtor ou factory"
  ],
  "files_expected": [
    "app/src/main/java/com/example/network/OverpassApiService.kt",
    "app/src/main/java/com/example/util/GoogleAuthHelper.kt",
    "app/src/main/java/com/example/network/fipe/FipeNetworkClient.kt",
    "app/src/main/java/com/example/network/viacep/ViaCepNetworkClient.kt"
  ],
  "out_of_scope": ["Mudanças de endpoints de rede"],
  "verification": [
    "./gradlew testDebugUnitTest"
  ],
  "required_reviewers": ["revisor"],
  "depends_on": []
}
```

---

### 4.3 ONDA 3: Refatoração Estrutural e Higienização Técnica

#### Tarefa T-009 (Decomposição de Arquivos Gigantes)
```json
{
  "task_id": "T-009",
  "to_agent": "arquiteto",
  "task_type": "implementacao",
  "risk": "medio",
  "title": "Quebra de telas com mais de 800 linhas em subcomponentes",
  "context": "ProfileScreen, FuelRefillsScreen e HistoryScreen possuem mais de 1.300 linhas cada.",
  "root_cause_evidence": "ProfileScreen.kt (2.480 linhas) e FuelRefillsScreen.kt (1.611 linhas)",
  "acceptance_criteria": [
    "Nenhum arquivo resultante com mais de 800 linhas",
    "Comportamento idêntico preservado com 100% dos testes unitários passando antes e depois"
  ],
  "files_expected": [
    "app/src/main/java/com/example/ui/screens/ProfileScreen.kt",
    "app/src/main/java/com/example/ui/screens/FuelRefillsScreen.kt",
    "app/src/main/java/com/example/ui/screens/HistoryScreen.kt"
  ],
  "out_of_scope": ["Alterações de lógica de negócio ou novos recursos visuais"],
  "verification": [
    "./gradlew testDebugUnitTest"
  ],
  "required_reviewers": ["arquiteto", "revisor"],
  "depends_on": ["T-004", "T-005"]
}
```

#### Tarefa T-010 (Centralização de Strings e Remoção de Dependências Mortas)
```json
{
  "task_id": "T-010",
  "to_agent": "acessibilidade",
  "task_type": "implementacao",
  "risk": "baixo",
  "title": "Migração de textos para strings.xml e limpeza de libs não utilizadas",
  "context": "Mais de 600 chamadas Text() sem stringResource e dependências não utilizadas como firebase.ai.",
  "root_cause_evidence": "strings.xml:2 e app/build.gradle.kts:114",
  "acceptance_criteria": [
    "Textos principais de botões, títulos e avisos migrados para strings.xml",
    "Remoção de dependências mortas do build.gradle.kts e libs.versions.toml",
    "Limpeza das cores de template não utilizadas do colors.xml"
  ],
  "files_expected": [
    "app/src/main/res/values/strings.xml",
    "app/src/main/res/values/colors.xml",
    "app/build.gradle.kts"
  ],
  "out_of_scope": ["Alteração no fluxo de telas"],
  "verification": [
    "./gradlew testDebugUnitTest lintDebug"
  ],
  "required_reviewers": ["revisor", "devops"],
  "depends_on": []
}
```

---

## 5. Itens Verificados e Aprovados (OK)

Todos os itens listados abaixo foram inspecionados no código-fonte e validados por execução ou testes:

1. **Autenticação Real com Google (Credential Manager + Supabase OAuth):**
   - **Evidência:** `GoogleAuthHelper.kt:57-100` e `SupabaseClient.kt:197-230`. O app utiliza `GetGoogleIdOption`, gera nonce SHA-256 e repassa para `signInWithGoogleIdToken` no Supabase com sucesso. Testado por `SupabaseGoogleAuthMockWebServerTest` (3 testes aprovados).
2. **Cadeia de Migração Room Completa e sem Migração Destrutiva:**
   - **Evidência:** `GiroCustoDatabase.kt:28-187`. Versão atual 12 possui migrations incrementais declaradas (`MIGRATION_1_2` até `MIGRATION_11_12`). Não há presença de `fallbackToDestructiveMigration`.
3. **Resolução de Conflitos Last-Write-Wins (LWW) e Transações Atômicas:**
   - **Evidência:** `SupabaseSyncManager.kt:49-208`. Todas as atualizações locais ocorrem em blocos `db.withTransaction { ... }`. A regra de LWW preserva dados mais novos comparando `updatedAt` e trata deleções lógicas com soft delete (`deletedAt != null`). Testado por `SupabaseSyncAndMigrationTest` (25 testes aprovados).
4. **Isolamento de Segurança RLS no Supabase:**
   - **Evidência:** `supabase/migrations/20261021_init_girocusto_schema.sql:54-61`. Todas as tabelas têm `ENABLE ROW LEVEL SECURITY` e políticas `USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id)`.
5. **Nenhum Segredo ou Chave Privada Rastreada no Git:**
   - **Evidência:** Busca executada com `git log -p -S` para chaves sensíveis, `.env`, `*.jks` e `google-services.json` retornou 0 ocorrências.
6. **Ausência de Testes Desativados ou Ignorados:**
   - **Evidência:** Execução de busca por `@Ignore`, testes vazios ou filtros no Gradle retornou 0 ocorrências. Toda a suíte executa sem exclusões.
7. **Detecção Eficaz por Mutação:**
   - **Evidência:** Três testes de mutação propositais em cópia isolada (`/tmp/girocusto-auditoria`) foram detectados e reprovados com precisão pela suíte de testes existente:
     - Mutação 1 (Inversão no cálculo de lucro líquido): detectada e reprovada por `RepositoryAndEstimationTest > testInsertDailyRecordCalculationsAndOdometerAndWear`.
     - Mutação 2 (Coluna quebrada na migração Room 11->12): detectada e reprovada por `SupabaseSyncAndMigrationTest > testMigration11To12_addsVehicleIdAndLinksToActiveVehicle`.
     - Mutação 3 (Desativação da atualização remota no LWW): detectada e reprovada por `SupabaseSyncAndMigrationTest > testPullRemoteChanges_LWW_remoteNewerOverwritesPendingLocal`.

---

## 6. Achados Derrubados pelo Revisor Independente

Conforme a Regra 5 desta auditoria, o Revisor Independente inspecionou as suspeitas preliminares e derrubou formalmente três apontamentos que não se sustentavam diante do código real:

1. **Suspeita:** `signInWithGoogleMockOrOAuth` e `signUpWithGoogleMockOrOAuth` no `GiroCustoViewModel` seriam stubs que apenas exibem "em homologação".
   - **Parecer do Revisor:** **DERRUBADO.** Esses métodos foram totalmente substituídos por `signInWithGoogle(context)` em `GiroCustoViewModel.kt:491`, implementando o fluxo nativo com Credential Manager (`GoogleAuthHelper.kt`) e validação via Supabase API (`SupabaseClient.kt:197`). O apontamento foi derrubado. (Ressalva mantida apenas para o método de SMS, registrado no ID `INT-01`).
2. **Suspeita:** O banco Room estaria utilizando `fallbackToDestructiveMigration()`.
   - **Parecer do Revisor:** **DERRUBADO.** O arquivo `GiroCustoDatabase.kt` não contém nenhuma invocação a migrações destrutivas. Toda a evolução da versão 1 para a 12 possui objetos de migração `Migration(n, n+1)` explícitos. (Ressalva mantida apenas para a falta de testes automatizados para as migrações 1 a 8, registrada no ID `DAT-02`).
3. **Suspeita:** Vazamento de chaves privadas e segredos de produção no histórico do Git.
   - **Parecer do Revisor:** **DERRUBADO.** A varredura profunda no repositório com `git log -p -S` confirmou que nenhuma chave real do Supabase (como `service_role`), arquivo `.env` com valores reais, certificados JKS ou credenciais Google vazaram em commits passados.

---

## 7. O que NÃO Foi Possível Verificar e Por Quê

1. **Autenticação Real no Servidor Supabase e Google Play Services em Aparelho Físico:**
   - **Motivo:** O ambiente de auditoria é um contêiner/máquina de desenvolvimento sem aparelho Android físico conectado via USB. Os testes foram realizados via `MockWebServer`, Robolectric e banco de dados SQLite em memória.
   - **Status atribuído:** 🟡 **IMPLEMENTADO, NÃO VERIFICADO NO APARELHO** para a integração externa de autenticação e busca de postos.
2. **Desempenho Real de Bateria e Rolagem a 60/120 FPS:**
   - **Motivo:** Testes de perfilamento térmico, consumo de miliampères do GPS e taxa de quadros (JankStats) exigem instrumentação em dispositivo físico real.
3. **Comportamento do R8 / Minificação em Tempo de Execução:**
   - **Motivo:** A minificação está desativada no Gradle (`isMinifyEnabled = false`). O impacto de eventuais regras faltantes no ProGuard só pode ser avaliado após a geração de um APK de release minificado executado no aparelho.

---

## 8. Resultado Real da Execução de Testes e Lint

### 8.1 Suíte de Testes Unitários
- **Comando executado:**
  ```bash
  export JAVA_HOME=/home/nando/.jdks/jdk-17.0.21+9
  export PATH=$JAVA_HOME/bin:$PATH
  ./gradlew testDebugUnitTest --info
  ```
- **Contagem Real de Testes:**
  - **Total Executado:** 159 testes
  - **Aprovados:** 159 testes
  - **Falhas:** 0
  - **Erros:** 0
  - **Ignorados / Pulados:** 0
- **Detalhamento por Arquivo de Teste:**
  - `AuthBiometricsAndViaCepTest.xml`: 41 testes (Aprovados)
  - `ExampleRobolectricTest.xml`: 1 teste (Aprovado - teste template)
  - `ExampleUnitTest.xml`: 1 teste (Aprovado - teste template 2+2)
  - `FipeNetworkAndViewModelTest.xml`: 16 testes (Aprovados)
  - `FuelIntelligenceAndEstimationTest.xml`: 14 testes (Aprovados)
  - `GreetingScreenshotTest.xml`: 1 teste (Aprovado - screenshot de texto)
  - `HistoryGroupingTest.xml`: 4 testes (Aprovados)
  - `JuneSeedingTest.xml`: 1 teste (Aprovado)
  - `RepositoryAndEstimationTest.xml`: 23 testes (Aprovados)
  - `SupabaseGoogleAuthMockWebServerTest.xml`: 3 testes (Aprovados)
  - `SupabaseSyncAndMigrationTest.xml`: 25 testes (Aprovados)
  - `SyncStateTest.xml`: 2 testes (Aprovados)
  - `VehicleAndFipeIntegrationTest.xml`: 23 testes (Aprovados)
  - `WorkShiftAndPeriodFilterTest.xml`: 4 testes (Aprovados)
- **Caminho dos Relatórios:**
  - XMLs: `app/build/test-results/testDebugUnitTest/`
  - HTML: `app/build/reports/tests/testDebugUnitTest/index.html`
- **Últimas Linhas da Saída do Gradle:**
  ```
  BUILD SUCCESSFUL in 27s
  33 actionable tasks: 1 from cache, 32 up-to-date
  Configuration cache entry reused.
  ```

### 8.2 Android Lint
- **Comando executado:**
  ```bash
  export JAVA_HOME=/home/nando/.jdks/jdk-17.0.21+9
  export PATH=$JAVA_HOME/bin:$PATH
  ./gradlew lintDebug
  ```
- **Resultado do Lint:**
  - **Erros:** 0
  - **Avisos (Warnings):** 69 avisos
- **Caminho do Relatório de Lint:**
  - `app/build/reports/lint-results-debug.html`
  - `app/build/reports/lint-results-debug.txt`
- **Últimas Linhas da Saída do Gradle:**
  ```
  > Task :app:lintReportDebug
  Wrote HTML report to file:///home/nando/Documentos/GiroCustov0.1/app/build/reports/lint-results-debug.html

  > Task :app:lintDebug

  BUILD SUCCESSFUL in 5m 42s
  31 actionable tasks: 9 executed, 22 up-to-date
  Configuration cache entry stored.
  ```

---
*Relatório concluído com estrita observância das regras de integridade e somente leitura.*
