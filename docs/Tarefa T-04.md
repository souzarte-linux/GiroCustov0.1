# Relatório Final de Execução: Tarefa T-04 [INT-01 e SEC-03]

## 1. Resultado
✅ **VERIFICADO**: A higienização de logs com `Level.BASIC` e a desativação da opção de SMS foram testadas e comprovadas no aparelho celular real (`IJA67XLBNBCML7NV`). Os logs extraídos diretamente do dispositivo confirmaram que senhas e dados sensíveis de autenticação não são mais impressos no Logcat. Os testes unitários (159/159) e o Android Lint permanecem 100% aprovados, e as intercorrências de configuração de autenticação do Supabase no dispositivo foram diagnosticadas e solucionadas.

---

## 2. O que mudou
1. **Desativação segura de autenticação por SMS (INT-01):**
   - O botão de envio de SMS na tela de login/cadastro ([`AuthGateScreen.kt`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/screens/AuthGateScreen.kt)) foi desabilitado (`enabled = false`) e rotulado como `Receber Código por SMS (Em breve)` via `stringResource`.
   - O campo de número de celular foi tornado desabilitado (`enabled = false`), e a ação de teclado ("Done") foi desvinculada para impedir chamadas involuntárias.
   - Foi inserido um aviso visual orientando o usuário de que o acesso por SMS estará disponível em breve e que ele deve utilizar o login por E-mail ou Conta Google.
   - Nenhuma função acionável pela interface emite mais a mensagem de stub "Login por SMS em homologação".
2. **Higienização avançada de tráfego HTTP (SEC-03):**
   - Em [`SupabaseClient.kt`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/network/supabase/SupabaseClient.kt), o nível de log do `HttpLoggingInterceptor` foi alterado de `Level.BODY` para `Level.BASIC` em modo Debug. Isso garante que nenhum corpo de requisição HTTP (contendo senhas em texto puro, e-mails, tokens ou payloads JSON) seja impresso no Logcat do desenvolvedor ou exposto a ferramentas de leitura de log.
   - Foram adicionadas regras de redação explícitas para os cabeçalhos `Authorization` e `apikey` via `redactHeader("Authorization")` e `redactHeader("apikey")`.
3. **Strings centralizadas em strings.xml:**
   - Todos os novos textos inseridos foram criados em [`strings.xml`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/res/values/strings.xml) (`auth_tab_phone`, `auth_btn_sms_coming_soon`, `auth_sms_coming_soon_notice`).

Arquivos alterados:
- [`app/src/main/res/values/strings.xml`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/res/values/strings.xml): novos recursos de texto para a tela de autenticação.
- [`app/src/main/java/com/example/network/supabase/SupabaseClient.kt`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/network/supabase/SupabaseClient.kt): configuração de `Level.BASIC` e `redactHeader` para Authorization e apikey.
- [`app/src/main/java/com/example/ui/screens/AuthGateScreen.kt`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/screens/AuthGateScreen.kt): desativação do formulário/botão de SMS e inserção do aviso com `stringResource`.

---

## 3. Evidências

### Comandos exatos executados no ambiente de desenvolvimento
```bash
# 1. Suíte completa de testes unitários e análise estática
JAVA_HOME=/home/nando/.jdk17 PATH=/home/nando/.jdk17/bin:$PATH ./gradlew testDebugUnitTest lintDebug

# 2. Varreduras automáticas de integridade pré-commit
grep -rniE "mock|stub|fake|homologa|TODO|FIXME|em breve" app/src/main/java
grep -rnE "@Ignore|assertTrue\(true\)|assertEquals\(([0-9]+), *\1\)" app/src/test app/src/androidTest
grep -rniE "(api[_-]?key|secret|password|service_role|BEGIN (RSA|PRIVATE))" --include=*.kt --include=*.kts --include=*.xml --include=*.json --include=*.properties . | grep -v ".env.example"
```

### Contagem real de testes
- **Total executados:** 159
- **Aprovados:** 159
- **Falhas:** 0
- **Erros:** 0
- **Ignorados:** 0

### Caminho dos relatórios gerados
- Testes unitários: [`app/build/reports/tests/testDebugUnitTest/index.html`](file:///home/nando/Documentos/GiroCustov0.1/app/build/reports/tests/testDebugUnitTest/index.html)
- Relatório Lint: [`app/build/reports/lint-results-debug.html`](file:///home/nando/Documentos/GiroCustov0.1/app/build/reports/lint-results-debug.html)

### Últimas linhas da saída do Gradle (Testes e Lint)
```text
> Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
> Task :app:processDebugUnitTestJavaRes UP-TO-DATE
> Task :app:testDebugUnitTest

> Task :app:lintReportDebug
Wrote HTML report to file:///home/nando/Documentos/GiroCustov0.1/app/build/reports/lint-results-debug.html

> Task :app:lintDebug
> Task :app:finalizeTestRoborazziDebug SKIPPED

BUILD SUCCESSFUL in 4m 29s
42 actionable tasks: 25 executed, 17 up-to-date
Configuration cache entry reused.
```

### Evidência de higienização de logs no aparelho físico real (`IJA67XLBNBCML7NV`)
Comando executado para extrair o registro do Logcat do aparelho:
```bash
adb logcat -d | grep -iE "okhttp|supabase"
```
Saída real obtida do aparelho durante a tentativa de login:
```text
10-23 11:06:17.515 26717  6745 I okhttp.OkHttpClient: --> POST https://ykooslcxautcpfftansj.supabase.co/auth/v1/token?grant_type=password (60-byte body)
10-23 11:06:18.330 26717  6745 I okhttp.OkHttpClient: <-- 400 https://ykooslcxautcpfftansj.supabase.co/auth/v1/token?grant_type=password (814ms, 75-byte body)
```
**Comprovação:** A senha digitada pelo usuário (`Educ@151983`), os e-mails e os tokens de autorização não foram impressos no log do aparelho. Apenas o resumo de tráfego (`Level.BASIC`) foi gerado.

---

## 4. Testes
Nenhum teste existente foi alterado, enfraquecido ou removido. Todos os 159 testes preexistentes continuam passando integralmente com 100% de sucesso.

---

## 5. O que NÃO foi feito ou está simulado
- A implementação completa de um provedor real de envio e validação de SMS (Twilio, Supabase Phone Auth com Provedor Externo) está fora do escopo desta tarefa (conforme previsto no plano `out_of_scope: ["Implementação completa de provedor SMS externo"]`).
- O método `signInWithPhone` em [`GiroCustoViewModel.kt`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/GiroCustoViewModel.kt) foi mantido sem alterações para respeitar estritamente o `files_expected` e a assinatura de ViewModel, mas não é mais acionado por nenhum elemento da interface gráfica.

---

## 6. Riscos e dívidas encontrados
- **Aba de Celular mantida para informação:** A aba "Celular" foi mantida na interface para que o usuário compreenda que o recurso existirá futuramente, exibindo o status "Em breve" em vez de desaparecer sem explicação. Quando o serviço de SMS for contratado, a lógica real poderá ser integrada diretamente.
- **Tratamento de erros da API em formato JSON (Dívida de UX identificada nos testes):** Quando o Supabase retorna erros de autenticação (ex.: `email_not_confirmed` ou `over_email_send_rate_limit`), o corpo bruto em JSON foi exibido no banner de erro. Foi registrado o apontamento para que tarefas futuras de UX/Integração traduzam essas respostas para frases amigáveis ao motorista em português.
- As demais telas do aplicativo ainda possuem textos fixos a serem migrados gradualmente nas tarefas dedicadas T-21 e T-25.

---

## 7. Registro de Execução Manual no Aparelho Real, Erros e Soluções

Durante a realização do roteiro de testes pelo usuário no aparelho físico Android (`IJA67XLBNBCML7NV`), foram executadas as ações previstas, identificados incidentes pontuais e aplicadas as respectivas soluções:

### 1. Teste da Aba "Celular" (SMS)
- **Ação executada:** O usuário abriu o aplicativo e tocou na aba "Celular".
- **Comportamento observado:** O campo de texto permaneceu inativo, o botão exibiu o rótulo `Receber Código por SMS (Em breve)` desabilitado e a mensagem explicativa foi apresentada.
- **Resultado:** Aprovado sem erros. O stub de homologação foi completamente desativado da interface.

### 2. Monitoramento de Logs via Terminal (`adb logcat`)
- **Ação executada:** O usuário executou o comando sugerido inicialmente:
  ```bash
  adb logcat -s OkHttp:D
  ```
- **Erro apresentado:** O terminal não imprimiu nenhuma linha de log após clicar no botão de login.
- **Causa raiz:** No Android com OkHttp 4, o interceptor registra as chamadas usando o logger nativo da plataforma com a tag `okhttp.OkHttpClient` e severidade `INFO` (`I`), e não `OkHttp:D`. O filtro `-s OkHttp:D` silenciou a saída por não corresponder exatamente à tag.
- **Como foi solucionado:** O comando de monitoramento foi corrigido para:
  ```bash
  adb logcat -s okhttp.OkHttpClient:I
  # ou
  adb logcat | grep -i "okhttp"
  ```
- **Resultado final:** Ao inspecionar o logcat com o filtro correto, comprovou-se que o OkHttp registrou as chamadas em `Level.BASIC` sem expor senhas, tokens ou dados pessoais no terminal.

### 3. Tentativa de Cadastro por E-mail (`c***@h***.com`)
- **Ação executada:** O usuário tentou cadastrar a conta na aba de cadastro.
- **Erro apresentado:** Banner vermelho exibiu:
  ```json
  {"code":429,"error_code":"over_email_send_rate_limit","msg":"For security purposes, you can only request this after 56 seconds."}
  ```
- **Causa raiz:** Proteção anti-spam nativa do Supabase (Rate Limit de e-mail), que impede requisições sucessivas de cadastro/confirmação para o mesmo e-mail em intervalo menor que 60 segundos.
- **Como foi solucionado:** Respeitou-se o tempo de renovação da cota de envio (aguardo de 60 segundos).

### 4. Tentativa de Login por Senha antes da Confirmação do E-mail
- **Ação executada:** O usuário alternou para a aba de login e tentou entrar com a senha criada.
- **Erro apresentado:** Banner vermelho exibiu:
  ```json
  {"code":400,"error_code":"email_not_confirmed","msg":"Email not confirmed"}
  ```
- **Causa raiz:** A configuração padrão do projeto no Supabase exige confirmação obrigatória do endereço de e-mail antes de liberar o primeiro login com senha (`Confirm email: ON`).
- **Como foi solucionado:** O usuário acessou a caixa de correio do e-mail cadastrado e clicou no link de validação. O log do Supabase registrou a verificação bem-sucedida da conta às 14:22:18Z:
  ```text
  GET | 303 | https://ykooslcxautcpfftansj.supabase.co/auth/v1/verify?token=...&type=signup
  ```
  A conta foi validada com sucesso no backend do Supabase.

### 5. Tentativa de Reenvio Manual pelo Dashboard do Supabase
- **Ação executada:** No painel do Supabase (**Users**), o usuário clicou no botão "Send confirmation email".
- **Erro apresentado:** Popup do Supabase alertou:
  ```text
  Failed to send confirmation email: ... Error message: email rate limit exceeded
  ```
- **Causa raiz:** O limite de disparos automáticos da conta gratuita do Supabase foi atingido momentaneamente. Como a conta já havia acabado de ser confirmada com sucesso via link no passo anterior (status 303), a tentativa manual de reenvio era redundante.
- **Como foi solucionado:** Orientou-se que a conta já estava ativada e indicou-se o caminho correto para desativar a exigência de confirmação para cadastros futuros (**Authentication -> Sign In / Providers -> Email -> Confirm email: OFF**).

### Resultado Final dos Testes no Aparelho
A conta `c***@h***.com` encontra-se ativada e pronta para login no aplicativo, a higienização de logs em `Level.BASIC` está plenamente validada em aparelho real e a interface encontra-se segura contra o acionamento de stubs de SMS.

---

## 8. Commit e sincronização
- **Hash do commit:** `fa6019685a9bc83f08da50937a07fc2624d62153`
- **Hash curto:** `fa60196`
- **Mensagem:** `Segurança: oculta login por SMS e higieniza logs HTTP com Level.BASIC (T-04)`
- **Status do push:** Sincronizado com sucesso com o repositório remoto (`main -> origin/main`).
