# Relatório de Execução da Tarefa T-10 [SEC-06]

**Data:** 27 de Outubro de 2026  
**Tarefa:** T-10 [SEC-06] - Portabilidade de Dados e Exclusão Segura de Conta (LGPD) com Limpeza de Dono e Metas  
**Risco:** Alto  
**Responsáveis:** Orquestrador, Arquiteto, Segurança & Privacidade, Backend, Frontend & UX, QA e DevOps  

---

## 1. Resultado

🟡 **IMPLEMENTADO, NÃO VERIFICADO NO APARELHO**  
(O código do app, componentes de tela, serialização JSON via SAF/Share nativo, expurgo atômico local, proteção fail-safe e suíte de testes com MockWebServer estão 100% implementados e aprovados. A criação da RPC remota no PostgreSQL do Supabase e a prova no aparelho físico dependem de ações manuais do usuário com uma conta de teste descartável).

---

## 2. O que mudou

### Explicação Leiga
Implementamos no aplicativo os direitos fundamentais de privacidade exigidos pela LGPD (Lei Geral de Proteção de Dados):
1. **Portabilidade de Dados (Exportação):** Na tela de Perfil ("Meus Dados"), o motorista agora tem uma nova seção de "Privacidade e Dados (LGPD)" onde pode exportar todos os seus dados cadastrados no aplicativo (perfil, metas de faturamento, veículos, peças de desgaste, corridas, abastecimentos, manutenções e plataformas de entrega) em um arquivo padrão JSON estruturado. Ele pode escolher entre compartilhar diretamente (WhatsApp, Drive, e-mail) ou salvar no armazenamento do próprio aparelho sem necessitar de permissões invasivas de sistema.
2. **Exclusão Definitiva de Conta e Dados:** O motorista pode solicitar a exclusão total e permanente de sua conta. Para evitar acidentes ou toques involuntários, a confirmação foi desenhada em duas etapas com bloqueio de segurança: o usuário é alertado de que todos os dados na nuvem e no celular serão destruídos e só consegue acionar o botão final após digitar manualmente a palavra **EXCLUIR**.
3. **Proteção Fail-Safe (Segurança contra Perda de Dados):** Se houver qualquer falha de internet, queda de conexão ou erro no servidor do Supabase, o aplicativo cancela a operação imediatamente e **não apaga nenhum dado local**. Nada é perdido se o servidor não confirmar a exclusão com sucesso.
4. **Expurgo Atômico e Limpeza de Dono:** Quando a exclusão na nuvem é confirmada pelo servidor, o app realiza a faxina completa no celular: apaga todas as 8 tabelas do banco Room, as metas sincronizadas, limpa o identificador do dono do aparelho (`DataOwnerStorage`), revoga a sessão salva com o Keystore, limpa a biometria cadastrada e retorna à tela inicial de boas-vindas.

### Lista de Arquivos Afetados
- `supabase/migrations/20261026_user_deletion_rpc.sql`: Script SQL contendo a RPC `delete_user_account()` com privilégios `SECURITY DEFINER`, `search_path = public, auth, pg_temp` e restrita exclusivamente ao papel `authenticated`.
- `supabase/README.md`: Documentação técnica da RPC remota, regras de permissão e guia de execução no painel do Supabase.
- `app/src/main/java/com/example/network/supabase/SupabaseApi.kt`: Endpoint da RPC `@POST("rest/v1/rpc/delete_user_account")` adicionado na interface `SupabaseRestApi`.
- `app/src/main/java/com/example/network/supabase/SupabaseClient.kt`: Método `deleteUserAccount(): Result<Unit>` que executa a exclusão remota autenticada.
- `app/src/main/java/com/example/data/Daos.kt`: Adicionados métodos de consulta ampla `getAllRefills()` e `getAllRecords()` para suportar a exportação íntegra de histórico.
- `app/src/main/java/com/example/util/UserDataExportHelper.kt`: Utilitário de serialização JSON de todas as entidades do app utilizando `org.json` puro (100% imune a problemas de reflexão ou minificação do R8).
- `app/src/main/java/com/example/data/Repository.kt`: Método `exportAllUserDataAsJson()` que reúne os dados locais e gera o arquivo JSON de portabilidade.
- `app/src/main/java/com/example/ui/GiroCustoViewModel.kt`: Métodos `exportUserData` e `deleteAccountAndData` com controle atômico, fail-safe em falha de rede e limpeza coordenada de sessão e biometria.
- `app/src/main/java/com/example/ui/components/ProfileLgpdSection.kt`: Componente modular Material 3 contendo os botões de ação e os diálogos de exportação e confirmação reforçada em duas etapas.
- `app/src/main/java/com/example/ui/screens/ProfileScreen.kt`: Integração da seção de privacidade com ActivityResultLauncher do SAF (`CreateDocument`) e Share nativo.
- `app/src/main/res/values/strings.xml`: Strings amigáveis em português do Brasil para todas as mensagens, títulos e alertas de acessibilidade.
- `app/src/test/java/com/example/UserAccountDeletionTest.kt`: Suíte completa de testes automatizados com MockWebServer cobrindo exportação, sucesso, falha remota com fail-safe e exclusão puramente local.
- `app/src/test/java/com/example/AuthBiometricsAndViaCepTest.kt`: Contrato fake atualizado.
- `app/src/test/java/com/example/SupabaseSyncAndMigrationTest.kt`: Contrato fake atualizado.
- `app/src/test/java/com/example/network/supabase/AccountIsolationAndSyncTest.kt`: Contrato fake atualizado.
- `docs/plano-onda-2.md`: Tabela de acompanhamento da Onda 2 atualizada.

---

## 3. Evidências

### Execução dos Testes Automatizados e Análise de Lint
```bash
./gradlew testDebugUnitTest lintDebug
```
**Resultado da Execução:**
- Total de testes executados: **262**
- Aprovados: **262 (100%)**
- Falhas: **0**
- Ignorados: **0**
- Caminho do relatório de testes: `app/build/reports/tests/testDebugUnitTest/index.html`
- Caminho do relatório do Lint: `app/build/reports/lint-results-debug.html`

**Últimas linhas da saída do Gradle:**
```text
> Task :app:compileDebugUnitTestKotlin UP-TO-DATE
> Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
> Task :app:processDebugUnitTestJavaRes UP-TO-DATE
> Task :app:generateDebugAndroidTestLintModel
> Task :app:generateDebugLintReportModel
> Task :app:generateDebugUnitTestLintModel
> Task :app:lintAnalyzeDebug
> Task :app:testDebugUnitTest
> Task :app:lintAnalyzeDebugAndroidTest
> Task :app:lintAnalyzeDebugUnitTest
> Task :app:finalizeTestRoborazziDebug SKIPPED
> Task :app:lintReportDebug
Wrote HTML report to file:///home/nando/Documentos/GiroCustov0.1/app/build/reports/lint-results-debug.html
> Task :app:lintDebug

BUILD SUCCESSFUL in 4m 52s
42 actionable tasks: 11 executed, 4 from cache, 27 up-to-date
Configuration cache entry reused.
```

### Validação do APK de Release com Minificação R8
```bash
./gradlew assembleReleaseCheck
```
**Resultado:**
- Build concluído com sucesso: APK `app-releaseCheck.apk` (5.5 MB) gerado em `app/build/outputs/apk/releaseCheck/`.
- Regras R8 e ProGuard validadas sem avisos ou quebras de ofuscação.

**Últimas linhas da saída do Gradle:**
```text
> Task :app:lintVitalAnalyzeReleaseCheck
> Task :app:minifyReleaseCheckWithR8
> Task :app:lintVitalReportReleaseCheck
> Task :app:lintVitalReleaseCheck
> Task :app:compileReleaseCheckArtProfile
> Task :app:packageReleaseCheck
> Task :app:assembleReleaseCheck
> Task :app:createReleaseCheckApkListingFileRedirect UP-TO-DATE

BUILD SUCCESSFUL in 10m 37s
50 actionable tasks: 18 executed, 4 from cache, 28 up-to-date
Configuration cache entry reused.
```

### Prova Obrigatória de Mutação Manual (Regra 4 de Integridade de Testes)
Como a Tarefa T-10 possui classificação de risco **Alto**, foi realizada a verificação compulsória por mutação manual para comprovar a eficácia da suíte de testes:
1. **Local da Quebra:** Em `app/src/main/java/com/example/ui/GiroCustoViewModel.kt`, na linha 738, o bloco de verificação fail-safe `if (rpcResult.isFailure) { ... return@launch }` foi temporariamente comentado, forçando o ViewModel a prosseguir com a destruição dos dados locais mesmo quando a chamada remota de rede ao Supabase retornasse erro HTTP 500.
2. **Execução do Teste:** Foi executado `./gradlew testDebugUnitTest --tests "com.example.UserAccountDeletionTest"`.
3. **Detecção da Falha:** O teste `testDeleteAccount_remoteRpcHttpError_failSafePreservesAllLocalDataAndDataOwnerAndSession` **FALHOU** imediatamente com `java.lang.AssertionError at UserAccountDeletionTest.kt:368`, pois detectou que os dados locais foram destruídos indevidamente em vez de serem preservados intactos.
4. **Restauração:** O código original foi restaurado e a suíte voltou a passar com **100% de sucesso (4 de 4 testes aprovados)**.

### Varreduras de Integridade Pré-Commit
1. **Stubs e pendências em código de produção:** `grep -rniE "mock|stub|fake|homologa|TODO|FIXME|em breve" app/src/main/java` -> Nenhuma ocorrência de stubs ou fakes. Apenas o rótulo "Em breve" no login SMS (T-04) já documentado.
2. **Testes desativados ou vazios:** `grep -rnE "@Ignore|assertTrue\(true\)|assertEquals\(([0-9]+), *\1\)" app/src/test app/src/androidTest` -> Limpo (código 1).
3. **Segredos no repositório:** `grep -rniE "(api[_-]?key|secret|password|service_role|BEGIN (RSA|PRIVATE))" ...` -> Nenhum segredo ou chave privada exposta. Apenas constantes de cabeçalho e referências de visual transformation do Compose.

---

## 4. Testes

### Testes Adicionados
Foram criados 4 novos testes automatizados de integração em `app/src/test/java/com/example/UserAccountDeletionTest.kt`:
1. `testExportUserData_containsAllEntitiesAndValidJson`: Valida que o utilitário `UserDataExportHelper` extrai todas as tabelas (perfil, metas, veículos, peças, corridas, abastecimentos, manutenções, plataformas) e gera um JSON íntegro, formatado e legível.
2. `testDeleteAccount_remoteRpcSuccess_clearsAllLocalDataAndDataOwnerAndSession`: Simula resposta remota HTTP 200 da RPC `delete_user_account()`. Valida que o app apaga todas as 8 tabelas Room, limpa o `DataOwnerStorage`, revoga a sessão segura e limpa as credenciais biométricas.
3. `testDeleteAccount_remoteRpcHttpError_failSafePreservesAllLocalDataAndDataOwnerAndSession`: Simula resposta remota de erro (HTTP 500) do servidor Supabase. Comprova a proteção fail-safe: nenhum registro do Room é apagado, as metas são preservadas, o dono da conta (`DataOwnerStorage`) continua ativo e a sessão não é encerrada.
4. `testDeleteAccount_unauthenticatedLocalOnly_clearsLocalDataAndDataOwner`: Simula a exclusão solicitada por um usuário offline/sem conta remota vinculada. Valida a limpeza de dados e retorno limpo sem dependência de chamadas remotas de rede.

### Testes Alterados
- `AuthBiometricsAndViaCepTest.kt`, `SupabaseSyncAndMigrationTest.kt` e `AccountIsolationAndSyncTest.kt`: Apenas implementaram o novo método de contrato `deleteUserAccount(): Response<Unit>` nos seus respectivos fakes de teste de rede. **Nenhum teste existente foi alterado ou enfraquecido.**

### Testes Removidos
- Nenhum teste foi removido.

---

## 5. O que NÃO foi feito ou está simulado

- **Nada está simulado no código.** Não foram criados mocks, stubs ou telas fictícias. A exportação gera o arquivo JSON real e a exclusão aciona a RPC real no banco.
- **Pendente de Ação Manual do Usuário:**
  1. A execução do script SQL `supabase/migrations/20261026_user_deletion_rpc.sql` no painel web do Supabase para criar a RPC remota no PostgreSQL (passo a passo detalhado na Seção 7).
  2. O teste prático no celular físico com uma **conta de teste descartável**.

---

## 6. Riscos e Dívidas Encontrados

- **Risco de Exclusão Acidental da Própria Conta de Produção:** A exclusão de conta é um ato irreversível tanto no PostgreSQL do Supabase quanto no aparelho. Por esse motivo, o diálogo foi projetado com confirmação reforçada (exige digitar exatamente a palavra "EXCLUIR") e o usuário é expressamente orientado a nunca testar essa função com sua conta de trabalho real, criando uma conta descartável exclusivamente para testes.
- **Tamanho de `GiroCustoViewModel.kt`:** Com a inclusão dos métodos da LGPD, o ViewModel atingiu 1.699 linhas. A decomposição arquitetural em ViewModels de escopo menor está agendada para a Tarefa T-20 na Onda 3.

---

## 7. O que o usuário precisa fazer

> [!CAUTION]
> **AVISO IMPORTANTE DE SEGURANÇA:**  
> A exclusão de conta apaga **definitivamente e sem volta** todos os dados no servidor e no aparelho.  
> **NUNCA teste a função de exclusão com a sua conta principal do aplicativo!**  
> Para validar os roteiros abaixo, crie uma **conta de teste descartável** (exemplo: `teste_lgpd@exemplo.com`).

---

### PARTE A: Executar o Script SQL no Painel Web do Supabase

Para que o servidor Supabase reconheça o comando de exclusão de conta, você precisa cadastrar a função RPC no seu painel web:

1. Abra o navegador no seu computador e acesse o site do Supabase: [https://supabase.com/dashboard](https://supabase.com/dashboard)
2. Faça login com a sua conta do Supabase e selecione o seu projeto do **GiroCusto**.
3. No menu lateral esquerdo, clique no ícone **SQL Editor** (o símbolo `>_`).
4. Clique no botão verde **"+ New Query"** (no canto superior esquerdo do editor).
5. Abra o arquivo [supabase/migrations/20261026_user_deletion_rpc.sql](file:///home/nando/Documentos/GiroCustov0.1/supabase/migrations/20261026_user_deletion_rpc.sql) no seu computador e copie todo o seu conteúdo.
6. Cole o texto copiado dentro do editor de SQL do Supabase.
7. Clique no botão verde **"Run"** (no canto inferior direito do painel de SQL).
8. Verifique se na parte inferior apareceu a mensagem: **"Success. No rows returned"**.  
   *Pronto! A função segura de exclusão de conta está ativa e protegida na sua nuvem.*

---

### PARTE B: Roteiro de Teste Manual no Aparelho Físico

Após gerar o APK ou instalar o aplicativo atualizado no seu celular, siga os passos abaixo:

#### Roteiro 1: Testar a Exportação de Dados (Portabilidade LGPD)
1. Abra o aplicativo GiroCusto no seu celular.
2. Toque na aba **Perfil** ("Meus Dados") na barra inferior.
3. Role a tela até a seção **"Privacidade e Dados (LGPD)"**.
4. Toque no botão **"Exportar Todos os Meus Dados"**.
5. Um diálogo será aberto perguntando como você deseja exportar. Escolha **"Compartilhar"** ou **"Salvar no Aparelho"**:
   - Se escolher **"Compartilhar"**: A folha nativa do Android será aberta permitindo enviar o arquivo para seu Drive, WhatsApp ou e-mail.
   - Se escolher **"Salvar no Aparelho"**: O seletor de arquivos oficial do Android (SAF) será aberto sugerindo o nome `girocusto_dados_exportados_[data].json`. Escolha uma pasta (ex.: Downloads) e toque em **"Salvar"**.
6. Uma mensagem rápida (Snackbar) informará: *"Arquivo salvo com sucesso!"*.
7. Abra o arquivo JSON salvo: confirme que seus dados cadastrados (veículo, corridas, etc.) estão organizados em formato legível.

#### Roteiro 2: Testar a Trava de Segurança contra Exclusão Acidental
1. Na tela **Perfil**, toque no botão vermelho **"Excluir Minha Conta e Dados"**.
2. O primeiro diálogo de confirmação será exibido, explicando que a ação é permanente e irreversível. Toque em **"Continuar para Exclusão"**.
3. O segundo diálogo será aberto, exigindo a digitação da palavra **EXCLUIR**.
4. Digite algo diferente, por exemplo: `teste` ou `excluir` (em minúsculas).
5. Observe que o botão vermelho **"Excluir Minha Conta Agora"** permanece **desabilitado**, impedindo toques acidentais.
6. Toque em **"Cancelar"** e confirme que o diálogo fecha e nada foi alterado.

#### Roteiro 3: Testar a Exclusão Definitiva (com Conta Descartável)
1. Certifique-se de que você está logado com uma **conta de teste descartável** (criada apenas para este teste).
2. Toque novamente em **"Excluir Minha Conta e Dados"** -> **"Continuar para Exclusão"**.
3. Digite exatamente a palavra **EXCLUIR** (em letras maiúsculas).
4. O botão vermelho será habilitado. Toque em **"Excluir Minha Conta Agora"**.
5. O aplicativo mostrará um indicador circular de progresso enquanto chama o servidor.
6. Com a confirmação da nuvem, o aplicativo expurgará todos os dados locais e voltará automaticamente para a tela inicial de autenticação/onboarding.
7. Se você tentar fazer login novamente com essa conta descartável, verá que a conta não existe mais.

---

## 8. Commit e Sincronização

- **Hash do Commit:** `7719147`
- **Mensagem do Commit:** `feat(lgpd): portabilidade de dados e exclusão segura de conta (T-10)`
- **Status do Push:** Sincronizado com `origin/main` (`b806646..7719147`).
