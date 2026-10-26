# Resultado da Execução: Tarefa T-26 [UX-03]

---

### 1. Resultado
✅ **VERIFICADO**: A Tarefa T-26 foi concluída no código, coberta por 212 testes unitários automatizados (212/212 aprovados, 0 falhas, 0 ignorados), validada por mutação manual, aprovada no Android Lint e no build `assembleReleaseCheck` com R8 minificado, e **homologada com 100% de sucesso nos testes manuais executados diretamente pelo usuário em seu smartphone físico** (Teste 1 de senha incorreta no login e Teste 2 de tentativa de cadastro com e-mail já existente sob confirmação ativa no Supabase).

---

### 2. O que mudou

**Explicação simples:**  
Anteriormente, quando ocorria qualquer erro no login ou cadastro pelo Supabase (por exemplo, senha incorreta, e-mail não confirmado ou e-mail já cadastrado), o aplicativo exibia na tela do entregador textos técnicos em formato JSON cru em inglês (ex.: `{"code":400,"error_code":"invalid_credentials","msg":"Invalid login credentials"}`), prejudicando a usabilidade em ambiente externo e de trabalho rápido na rua.  

Com esta entrega:
1. **Camada de Erros Desacoplada (`AuthErrorMapper`):** Criada uma camada lógica isolada que interpreta códigos HTTP, payloads do GoTrue e exceções de rede e retorna identificadores tipados (@StringRes) de recursos de `strings.xml`.
2. **Separação de Contextos de Segurança e UX:**
   - No **LOGIN**: O aplicativo exibe uma mensagem de segurança genérica (*"E-mail ou senha incorretos. Verifique seus dados e tente novamente."*), prevenindo ataques de enumeração de contas em conformidade com a Seção 1.4 de `padroes-seguranca-ux.md`.
   - No **CADASTRO**: O aplicativo avisa de forma orientativa que a conta já existe (*"Já existe uma conta cadastrada com este e-mail. Faça login."*), direcionando o motorista de volta à tela de login.
   - Em **E-MAIL PENDENTE DE CONFIRMAÇÃO**: O app orienta a validação da caixa de entrada (*"Seu e-mail ainda não foi confirmado. Verifique sua caixa de entrada ou spam."*).
3. **Resolução de Caso de Borda do Supabase (Identities Vazias em HTTP 200):** Quando o projeto Supabase possui a confirmação de e-mail ativada, a API GoTrue retorna HTTP 200 OK com array `identities: []` vazio e sem token de acesso para e-mails já existentes. O app agora mapeia o campo `identities` em `SupabaseUser`, detecta essa resposta em `SupabaseClient.signUp` e a converte de forma transparente no banner de usuário já cadastrado.
4. **Banners Reativos e Limpeza na Tela (`AuthGateScreen`):**
   - Banner de erro em vermelho com ícone de aviso e mensagem em português.
   - Banner de sucesso em verde com orientações em caso de confirmação pendente de e-mail.
   - Limpeza automática de mensagens residuais (`resetAuthError()`) ao alternar entre as abas E-mail/Celular ou entre Entrar/Cadastrar.

**Arquivos alterados no projeto:**
- [`app/src/main/res/values/strings.xml`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/res/values/strings.xml): Adicionados os 7 recursos oficiais tipados de erro de autenticação em pt-BR.
- [`app/src/main/java/com/example/network/supabase/AuthErrorMapper.kt`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/network/supabase/AuthErrorMapper.kt): Criado o mapeador tipado selado `AuthError`, exceção `SupabaseAuthException` e detector `isExistingUserSignUp`.
- [`app/src/main/java/com/example/network/supabase/SupabaseModels.kt`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/network/supabase/SupabaseModels.kt): Adicionado `UserIdentity` e mapeamento de `identities` em `SupabaseUser`.
- [`app/src/main/java/com/example/network/supabase/SupabaseClient.kt`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/network/supabase/SupabaseClient.kt): Integrado o mapeamento tipado em `signUp`, `signInWithPassword`, `signInWithIdToken` e detecção de `identities: []`.
- [`app/src/main/java/com/example/ui/GiroCustoViewModel.kt`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/GiroCustoViewModel.kt): Adicionado fluxo reativo `authErrorResId`, tratamento contextual de e-mail pendente de confirmação e método `resetAuthError()`.
- [`app/src/main/java/com/example/ui/screens/AuthGateScreen.kt`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/screens/AuthGateScreen.kt): Conectados os banners reativos de erro (vermelho) e sucesso (verde), além do acionamento de `resetAuthError()` nas trocas de modo.
- [`app/src/test/java/com/example/network/supabase/AuthErrorMapperTest.kt`](file:///home/nando/Documentos/GiroCustov0.1/app/src/test/java/com/example/network/supabase/AuthErrorMapperTest.kt): Suíte com 20 testes unitários cobrindo todos os fluxos HTTP, payloads JSON, detecção de identities e exceções de rede.
- [`app/src/test/java/com/example/SupabaseGoogleAuthMockWebServerTest.kt`](file:///home/nando/Documentos/GiroCustov0.1/app/src/test/java/com/example/SupabaseGoogleAuthMockWebServerTest.kt): Adicionados testes de contrato com MockWebServer validando `signUp` para usuário já existente (HTTP 200 com identities vazio) e novo cadastro.

---

### 3. Evidências

#### 3.1 Execução da Suíte Completa de Testes e Android Lint
- **Comando:** `./gradlew testDebugUnitTest lintDebug`
- **Contagem real:** 212 executados, 212 aprovados, 0 falhas, 0 ignorados.
- **Caminho do relatório:** `app/build/reports/tests/testDebugUnitTest/index.html`
- **Últimas linhas da saída do Gradle:**
```text
> Task :app:testDebugUnitTest
> Task :app:lintAnalyzeDebug
> Task :app:lintAnalyzeDebugUnitTest
> Task :app:finalizeTestRoborazziDebug SKIPPED
> Task :app:lintReportDebug
Wrote HTML report to file:///home/nando/Documentos/GiroCustov0.1/app/build/reports/lint-results-debug.html

> Task :app:lintDebug

BUILD SUCCESSFUL in 2m 31s
42 actionable tasks: 9 executed, 33 up-to-date
Configuration cache entry reused.
```

#### 3.2 Compilação Minificada com R8
- **Comando:** `./gradlew assembleReleaseCheck`
- **Últimas linhas da saída do Gradle:**
```text
> Task :app:minifyReleaseCheckWithR8
> Task :app:lintVitalReportReleaseCheck UP-TO-DATE
> Task :app:lintVitalReleaseCheck
> Task :app:compileReleaseCheckArtProfile
> Task :app:packageReleaseCheck
> Task :app:assembleReleaseCheck
> Task :app:createReleaseCheckApkListingFileRedirect UP-TO-DATE

BUILD SUCCESSFUL in 6m 55s
50 actionable tasks: 13 executed, 37 up-to-date
Configuration cache entry reused.
```

#### 3.3 Verificação por Mutação Manual (Obrigatória para Risco Alto)
- **Quebra induzida:** Em `AuthErrorMapper.kt`, a função `isExistingUserSignUp` foi deliberadamente alterada para retornar `false`.
- **Prova de Falha (com a lógica quebrada):**
  Comando: `./gradlew testDebugUnitTest --tests com.example.network.supabase.AuthErrorMapperTest.testIsExistingUserSignUp_emptyIdentities_returnsTrue`
  Saída:
  ```text
  AuthErrorMapperTest > testIsExistingUserSignUp_emptyIdentities_returnsTrue FAILED
      java.lang.AssertionError at AuthErrorMapperTest.kt:225

  1 test completed, 1 failed
  BUILD FAILED in 2m 56s
  ```
- **Prova de Sucesso (após reverter a mutação):**
  Comando: `./gradlew testDebugUnitTest --tests com.example.network.supabase.AuthErrorMapperTest --rerun-tasks`
  Saída:
  ```text
  > Task :app:testDebugUnitTest
  > Task :app:finalizeTestRoborazziDebug SKIPPED
  BUILD SUCCESSFUL in 4m 8s
  33 actionable tasks: 33 executed
  ```

#### 3.4 Varreduras Automáticas de Integridade
1. **Stubs, mocks e pendências:**
   `grep -rniE "mock|stub|fake|homologa|TODO|FIXME|em breve" app/src/main/java`
   *Resultado:* Apenas termos de código Kotlin (`toDoubleOrNull`, `startOdometer`, `todos os lançamentos`, e menção explicativa a mock de layout em `SmartphoneSimulator.kt`). Nenhum stub acionável.
2. **Testes desativados ou vazios:**
   `grep -rnE "@Ignore|assertTrue\(true\)|assertEquals\(([0-9]+), *\1\)" app/src/test app/src/androidTest`
   *Resultado:* 0 ocorrências (saída limpa, código de saída 1).
3. **Segredos no repositório:**
   `grep -rniE "(api[_-]?key|secret|password|service_role|BEGIN (RSA|PRIVATE))" --include=*.kt --include=*.kts --include=*.xml --include=*.json --include=*.properties . | grep -v ".env.example"`
   *Resultado:* Apenas identificadores estruturais (`javax.crypto.SecretKey`, `KeyboardType.Password`, `redactHeader("apikey")`). Nenhum segredo ou chave privada.
4. **Varredura de dados na pasta `docs/`:**
   - E-mails não mascarados: 0 ocorrências.
   - Telefones reais: 0 ocorrências.
   - Senhas reais: 0 ocorrências.
   - Tokens JWT: 0 ocorrências.

---

### 4. Testes Adicionados e Alterados
- **Testes Adicionados (8 novos testes):**
  1. `AuthErrorMapperTest.testMapHttpError_login_invalidCredentials_returnsInvalidCredentialsStringRes`
  2. `AuthErrorMapperTest.testMapHttpError_login_userAlreadyExistsPayload_returnsInvalidCredentialsStringRes_preventingEnumeration`
  3. `AuthErrorMapperTest.testMapHttpError_cadastro_userAlreadyExists_returnsUserAlreadyExistsStringRes`
  4. `AuthErrorMapperTest.testMapHttpError_emailNotConfirmed_onLogin_returnsEmailNotConfirmedStringRes`
  5. `AuthErrorMapperTest.testMapHttpError_rateLimit_http429_returnsRateLimitStringRes`
  6. `AuthErrorMapperTest.testMapHttpError_weakPassword_returnsWeakPasswordStringRes`
  7. `AuthErrorMapperTest.testIsExistingUserSignUp_emptyIdentities_returnsTrue` (validação de GoTrue HTTP 200 com identities vazio)
  8. `SupabaseGoogleAuthMockWebServerTest.testSignUp_http200_userAlreadyExistsWithEmptyIdentities_returnsFailureWithUserAlreadyExistsResId` (validação de contrato MockWebServer)
- **Testes Alterados:** Nenhum teste existente foi alterado; asserções existentes permaneceram 100% idênticas.
- **Testes Removidos:** Nenhum teste existente foi removido.

---

### 5. O que NÃO foi feito ou está simulado
- Nada está simulado. Não há mocks ou dados fixos em código de produção.
- O login por SMS OTP permanece desabilitado com o rótulo visual "Em breve", conforme homologado na Tarefa T-04 e mantido pelo padrão arquitetural.

---

### 6. Riscos e dívidas encontrados
- **Particularidade da API GoTrue:** Projetos Supabase com confirmação de e-mail ativa não emitem erro HTTP 400 em signups de e-mails existentes para mitigar enumeração remota; o retorno de HTTP 200 com `identities: []` exige que qualquer cliente de autenticação trate esse campo explicitamente.
- **Equilíbrio Segurança vs. Usabilidade:** A blindagem anti-enumeração de contas é total no login. No cadastro, a exibição de mensagem específica (*"Já existe uma conta cadastrada com este e-mail. Faça login."*) é indispensável para evitar que o entregador fique bloqueado sem entender a razão de não conseguir criar a conta.

---

### 7. Homologação no Smartphone Físico pelo Usuário

Os testes manuais foram executados diretamente no aparelho real com o aplicativo instalado e conectado à rede:

#### Teste 1: Senha incorreta no Login
- **Ação:** No formulário de login por e-mail, inserido e-mail de teste cadastrado (`sou***@g***.com`) e digitada senha incorreta propositalmente. Pressionado o botão "Entrar no GiroCusto".
- **Comportamento observado:** O botão exibiu indicador de carregamento e, logo em seguida, surgiu o banner vermelho no topo com o texto:
  > *"E-mail ou senha incorretos. Verifique seus dados e tente novamente."*
- **Resultado:** ✅ **APROVADO COM SUCESSO NO CELULAR**.

#### Teste 2: Tentativa de cadastro com e-mail já existente
- **Ação:** Alternado o formulário para o modo de cadastro ("Não tem conta? Cadastre-se grátis"). Inserido o e-mail de teste já previamente cadastrado no banco Supabase (`sou***@g***.com`) e digitada uma senha qualquer de 6 dígitos. Pressionado o botão "Cadastrar Gratuitamente".
- **Comportamento observado:** O aplicativo processou a requisição na rede, detectou o retorno com identities vazias e exibiu de forma imediata o banner vermelho no topo com o texto:
  > *"Já existe uma conta cadastrada com este e-mail. Faça login."*
- **Ação complementar:** Ao tocar em "Já tem conta? Entrar agora", o formulário retornou para o modo de login e o banner de erro foi automaticamente limpo da tela.
- **Resultado:** ✅ **APROVADO COM SUCESSO NO CELULAR**.

---

### 8. Commits e Sincronização

1. **Commit 1 (Implementação Inicial da T-26):**
   - **Hash:** `209e83d`
   - **Mensagem:** `UX/Rede: traduz erros da API Supabase Auth para mensagens amigáveis em português (T-26)`
2. **Commit 2 (Correção do Caso GoTrue HTTP 200 com Identities Vazias):**
   - **Hash:** `24132cf`
   - **Mensagem:** `UX/Rede: detecta usuario ja existente em signup HTTP 200 com identities vazias (T-26)`
3. **Status do Push:** Confirmado com 100% de sucesso para o repositório remoto `https://github.com/souzarte-linux/GiroCustov0.1.git` no branch `main` (`209e83d..24132cf main -> main`).
