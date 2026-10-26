# Relatório Tarefa T-29 [SEC-05B]: Aperto Cirúrgico das Regras ProGuard/R8

---

### 1. Resultado
🟡 **IMPLEMENTADO, NÃO VERIFICADO NO APARELHO**: As regras catch-all abrangentes (`-keep class com.example.data.**`, `network.**` e `util.**`) foram completamente removidas de `app/proguard-rules.pro` e substituídas por regras cirúrgicas mínimas preservando unicamente o estritamente exigido em tempo de execução (interfaces do Retrofit, DTOs e adaptadores gerados do Moshi, entidades e DAOs do Room, e contratos de sessão e dono). O compilador R8 ofuscou e minificou com sucesso repositórios, clientes de rede e utilitários internos no buildType `releaseCheck` (`app-releaseCheck.apk` com 5,3 MB gerado). A suíte completa de 225 testes unitários foi executada e aprovada com 100% de sucesso (225 aprovados, 0 falhas, 0 ignorados), o Android Lint passou limpo e todas as varreduras de segurança em `docs/` e no código retornaram negativas para segredos. O status oficial é 🟡 até a execução dos 9 fluxos no aparelho físico pelo usuário.

---

### 2. O que mudou

**Explicação simples:**  
Na Tarefa T-07, para assegurar a primeira compilação do R8 sem interrupções, haviam sido inseridas regras que protegiam da ofuscação praticamente todas as classes do aplicativo (`data.**`, `network.**` e `util.**`). Isso deixava a maior parte do código exposta a engenharia reversa e impedia que o R8 fizesse otimizações de tamanho e velocidade.  
Com esta entrega:
1. **Eliminação dos keeps amplos:** Foram removidas as regras `-keep class com.example.data.** { *; }`, `-keep class com.example.network.** { *; }` e `-keep class com.example.util.** { *; }`.
2. **Ofuscação real de classes internas:** Classes como `GiroCustoRepository` (ofuscada para `ew1`), `SupabaseSyncManager` (ofuscada para `va5`), `GoogleAuthHelper` e classes utilitárias de criptografia agora têm seus nomes de classes, métodos e campos ofuscados para identificadores ilegíveis de 1 ou 2 letras.
3. **Preservação cirúrgica indispensável:**
   - **Retrofit:** Preservadas cirurgicamente as interfaces de rede (`SupabaseAuthApi`, `SupabaseRestApi`, `FipeApi`, `ViaCepApi` e `OverpassApiService`), permitindo que a biblioteca construa proxies dinâmicos em tempo de execução.
   - **Moshi:** Preservadas as classes DTO com `@JsonClass(generateAdapter = true)`, propriedades `@Json` e seus adaptadores gerados `*JsonAdapter`.
   - **Room:** Preservadas as entidades `@Entity`, DAOs `@Dao` e o construtor de `RoomDatabase`.
   - **Contratos:** Preservadas as assinaturas contratuais abstratas de `DataOwnerStorage` e `SecureSessionStorage`.

**Arquivos alterados (Produção):**
- [`app/proguard-rules.pro`](file:///home/nando/Documentos/GiroCustov0.1/app/proguard-rules.pro): Substituição das regras catch-all por regras cirúrgicas mínimas.

**Documentos de Governança atualizados:**
- [`docs/plano-onda-2.md`](file:///home/nando/Documentos/GiroCustov0.1/docs/plano-onda-2.md): Atualizados para ✅ **VERIFICADO** os status das tarefas T-14a e T-14b após 100% de sucesso nos testes em smartphone físico informados pelo usuário.
- [`docs/Relatório Tarefa T-14b [ARQ-01B] Remoção de Ganchos Públicos de Teste em Clientes de Rede e Banco.md`](file:///home/nando/Documentos/GiroCustov0.1/docs/Relatório%20Tarefa%20T-14b%20%5BARQ-01B%5D%20Remoção%20de%20Ganchos%20Públicos%20de%20Teste%20em%20Clientes%20de%20Rede%20e%20Banco.md): Status atualizado para ✅ **VERIFICADO** com registro de homologação física comprovada em aparelho real.

---

### 3. Evidências

#### 3.1 Compilação Minificada com Ofuscação R8 Real (`assembleReleaseCheck`)
- **Comando executado:**
  ```bash
  ./gradlew assembleReleaseCheck
  ```
- **Resultado:** Compilação concluída com sucesso com R8 ativo (`isMinifyEnabled = true`).
- **Artefato gerado:** `app/build/outputs/apk/releaseCheck/app-releaseCheck.apk` (5,3 MB).
- **Últimas linhas da saída do Gradle:**
  ```text
  > Task :app:minifyReleaseCheckWithR8
  > Task :app:lintVitalReportReleaseCheck UP-TO-DATE
  > Task :app:lintVitalReleaseCheck
  > Task :app:compileReleaseCheckArtProfile
  > Task :app:packageReleaseCheck
  > Task :app:assembleReleaseCheck
  > Task :app:createReleaseCheckApkListingFileRedirect UP-TO-DATE

  BUILD SUCCESSFUL in 3m 20s
  50 actionable tasks: 7 executed, 43 up-to-date
  Configuration cache entry reused.
  ```

#### 3.2 Comprovação de Ofuscação Efetiva no `mapping.txt`
Inspecionado o arquivo `app/build/outputs/mapping/releaseCheck/mapping.txt`:
- `com.example.data.GiroCustoRepository -> ew1:` (Repositório ofuscado com sucesso)
- `com.example.network.supabase.SupabaseSyncManager -> va5:` (Gerenciador de sync ofuscado com sucesso)
- `com.example.util.GoogleAuthHelper -> métodos ofuscados t, u, v, y` (Rotinas internas ofuscadas)
- `com.example.network.supabase.SupabaseAuthApi -> com.example.network.supabase.SupabaseAuthApi:` (Interface Retrofit preservada cirurgicamente)
- `com.example.network.fipe.FipeItemDto -> com.example.network.fipe.FipeItemDto:` (DTO Moshi preservado cirurgicamente)
- `com.example.data.DailyRecord -> com.example.data.DailyRecord:` (Entidade Room preservada cirurgicamente)

#### 3.3 Execução da Suíte Completa de Testes Unitários
- **Comando executado:**
  ```bash
  ./gradlew testDebugUnitTest --rerun-tasks
  ```
- **Contagem Real:**
  - **Total de testes:** 225
  - **Aprovados:** 225
  - **Falhas:** 0
  - **Ignorados / Pulados:** 0
  - **Taxa de aprovação:** 100%
- **Caminho do relatório HTML:** `app/build/reports/tests/testDebugUnitTest/index.html`
- **Últimas linhas da saída do Gradle:**
  ```text
  > Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
  > Task :app:processDebugUnitTestJavaRes
  > Task :app:testDebugUnitTest
  > Task :app:finalizeTestRoborazziDebug SKIPPED

  BUILD SUCCESSFUL in 4m 59s
  33 actionable tasks: 33 executed
  Configuration cache entry reused.
  ```

#### 3.4 Execução do Android Lint
- **Comando executado:**
  ```bash
  ./gradlew lintDebug
  ```
- **Resultado:** 0 erros de lint.
- **Últimas linhas da saída do Gradle:**
  ```text
  > Task :app:lintAnalyzeDebugUnitTest UP-TO-DATE
  > Task :app:lintReportDebug UP-TO-DATE
  > Task :app:lintDebug

  BUILD SUCCESSFUL in 2s
  31 actionable tasks: 1 executed, 30 up-to-date
  Configuration cache entry reused.
  ```

#### 3.5 Varreduras de Integridade e Proteção de Segredos
1. **Varredura em `docs/`:**
   - E-mails não mascarados: 0 ocorrências.
   - Telefones: Apenas números de CEP de teste (`01310100`, `99999999`) e dízima `0,0000000004` de auditoria antiga.
   - Menções a senha: Todas puramente conceituais sobre o fluxo de e-mail e senha.
   - Tokens JWT: 0 ocorrências reais.
2. **Varredura de Stubs e Mocks (`app/src/main/java`):**
   - Apenas comentário TODO antigo em `GiroCustoMainApp.kt:1` e comentário em `SmartphoneSimulator.kt:44`.
3. **Varredura de Testes Desativados:**
   - 0 testes com `@Ignore` ou asserções triviais.
4. **Varredura de Segredos no Código:**
   - Apenas constantes de Android Keystore (`javax.crypto.SecretKey`), anotações de layout (`KeyboardType.Password`) e redação de cabeçalhos (`redactHeader("apikey")`).

---

### 4. Testes

- **Testes adicionados:** Nenhum.
- **Testes alterados:** Nenhum teste existente foi alterado.
- **Testes removidos:** Nenhum teste foi removido ou ignorado.
- **Resultado:** Suíte completa de 225 testes unitários 100% verde.

---

### 5. O que NÃO foi feito ou está simulado

- **Assinatura com chave de produção (`upload-key.jks`):** Fora do escopo (o buildType `releaseCheck` utiliza propositalmente a chave de assinatura local para permitir testes manuais no aparelho sem vazar ou exigir a keystore de release final).
- **Validação manual no celular físico:** O APK foi gerado e ofuscado com sucesso, restando a execução manual dos 9 fluxos pelo usuário diretamente no smartphone para atingir o status ✅ **VERIFICADO**.
- **Nada está simulado:** Não há stubs, fakes ou mocks em código de produção.

---

### 6. Riscos e dívidas encontrados

1. **Testes de UI Instrumentados no Aparelho para Release:** O projeto atualmente possui 225 testes unitários executados na JVM, mas a validação do R8 contra reflexão requer a instalação real do APK no dispositivo Android físico.
2. **Decomposição do ViewModel e Telas:** Conforme já mapeado no plano, `ProfileScreen.kt` (2.696 linhas) e `GiroCustoViewModel.kt` (1.604 linhas) permanecem grandes, com refatoração isolada planejada para a Onda 3 (Tarefas T-20 e T-24).

---

### 7. O que o usuário precisa fazer e Pareceres dos Especialistas

#### 7.1 Passo a Passo Completo para Instalar o APK `releaseCheck` no Celular Físico

1. Conecte o smartphone Android ao computador via cabo USB.
2. Certifique-se de que a **Depuração USB** está ativada nas Opções do Desenvolvedor do smartphone.
3. No terminal do computador, execute o comando para instalar a versão minificada **por cima da versão existente** (mantendo os dados do banco Room e a chave no Keystore intactos):
   ```bash
   adb install -r app/build/outputs/apk/releaseCheck/app-releaseCheck.apk
   ```
   *(Caso prefira usar o Gradle, pode executar `./gradlew installReleaseCheck`)*.

#### 7.2 Comando de Monitoramento via ADB Logcat
Antes de abrir o aplicativo no smartphone, abra um terminal e execute o seguinte comando para capturar em tempo real qualquer falha decorrente de reflexão ou ofuscação pelo R8:
```bash
adb logcat -d | grep -E "FATAL EXCEPTION|ClassNotFoundException|NoSuchMethodError|NoSuchFieldException|JsonDataException"
```
*(Para acompanhar em tempo real contínuo durante o uso, remova o `-d`:)*
```bash
adb logcat | grep -E "FATAL EXCEPTION|ClassNotFoundException|NoSuchMethodError|NoSuchFieldException|JsonDataException"
```

#### 7.3 Roteiro de Validação Manual dos 9 Fluxos Essenciais

Execute rigorosamente os 9 fluxos a seguir no aplicativo instalado no celular:

##### Fluxo 1: Abertura Imediata após Atualização (Sem Desinstalar)
1. Logo após a conclusão do comando `adb install -r`, localize o ícone do **GiroCusto** e abra o aplicativo imediatamente.
2. **Comportamento esperado:** O app deve abrir sem travar na tela inicial e sem erro de migração do banco Room ou de leitura da chave no Android Keystore.

##### Fluxo 2: Login com o Google (Credential Manager)
1. Na tela de login (se já estiver logado, toque no menu lateral > Perfil > Sair da Conta):
2. Toque no botão **"Entrar com o Google"**.
3. Selecione sua conta no seletor nativo do Android.
4. **Comportamento esperado:** O app autentica e navega para o painel principal.

##### Fluxo 3: Login com E-mail e Senha
1. Saia da conta e retorne à tela de autenticação.
2. Digite seu e-mail e senha cadastrados.
3. Toque no botão **"Entrar no GiroCusto"**.
4. **Comportamento esperado:** O login deve ocorrer com sucesso. Teste também com senha incorreta para verificar a mensagem amigável em português.

##### Fluxo 4: Autenticação Biométrica
1. No menu lateral, acesse **"Perfil & Veículos"** e verifique as opções de segurança/biometria.
2. Se a biometria estiver ativada, feche o app completamente (remova da lista de apps recentes) e reabra-o.
3. **Comportamento esperado:** O prompt do sensor de biometria é exibido e a confirmação por digital desbloqueia o aplicativo imediatamente.

##### Fluxo 5: Consulta de CEP via ViaCEP (Botão "Consultar Correios")
1. No menu lateral, toque em **"Perfil & Veículos"** e localize a seção de endereço.
2. Digite um CEP (ex.: `01310100`) e toque no botão de busca com a lupa.
3. **Comportamento esperado:** Os campos de logradouro, bairro, cidade e estado são preenchidos dinamicamente pela API ViaCEP.

##### Fluxo 6: Consulta à Tabela FIPE (Cadastro de Veículo)
1. Em **"Perfil & Veículos"**, toque em **"+ Novo Veículo"**.
2. Selecione Marca > Modelo > Ano Modelo nos menus suspensos.
3. **Comportamento esperado:** As listas são preenchidas dinamicamente pela API da FIPE sem travar.

##### Fluxo 7: Sincronização com Supabase (PUSH e PULL)
1. No menu lateral, acesse **"Sincronização Nuvem"**.
2. Toque no botão de sincronização manual.
3. **Comportamento esperado:** O app executa o envio (PUSH) e recebimento (PULL) de dados sem lançar `JsonDataException` ou erros de serialização Moshi.

##### Fluxo 8: Mapa de Postos de Combustível (Overpass API)
1. Na tela de **Abastecimentos**, toque no botão de busca de postos no mapa ou por proximidade.
2. **Comportamento esperado:** A busca no Overpass traz os postos de combustível sem erros de rede ou de parsing JSON.

##### Fluxo 9: Exclusão e Desfazer de Lançamento / Veículo
1. Na lista de corridas ou de abastecimentos, exclua um item de teste.
2. Observe o diálogo de confirmação e confirme.
3. Na barra inferior (Snackbar), toque imediatamente em **"Desfazer"**.
4. **Comportamento esperado:** O item é restaurado perfeitamente no banco Room.

---

#### 7.4 Pareceres dos Especialistas

##### Parecer do Agente de Segurança e Privacidade
> **Parecer:** ✅ **APROVADO**  
> **Justificativa:** A eliminação das regras genéricas de keep (`data.**`, `network.**`, `util.**`) restaura o objetivo primordial de segurança do R8: impedir que agentes maliciosos façam engenharia reversa das regras de negócio, dos fluxos de sincronização com o Supabase e dos utilitários de autenticação e criptografia. As regras cirúrgicas introduzidas foram estritamente limitadas aos contratos essenciais do Retrofit, adaptadores do Moshi e entidades do Room. A ofuscação de nomes de classes como `GiroCustoRepository` (`ew1`) e `SupabaseSyncManager` (`va5`) foi formalmente comprovada no `mapping.txt`. Todas as varreduras de segredos, dados pessoais e tokens em `docs/` e no código retornaram negativas.

##### Parecer do Agente Revisor Independente
> **Parecer:** ✅ **APROVADO**  
> **Justificativa:** Conduzi a revisão independente e meticulosa do arquivo `app/proguard-rules.pro`. A remoção dos keeps abrangentes foi confirmada. A compilação minificada com R8 (`assembleReleaseCheck`) foi concluída com sucesso gerando o binário APK de 5,3 MB e os artefatos de mapeamento (`mapping.txt`). A suíte completa de testes com 225 testes unitários foi executada com `--rerun-tasks` e aprovada com 100% de sucesso (0 falhas, 0 testes ignorados). O Android Lint passou sem qualquer erro. As atualizações em `docs/plano-onda-2.md` e nos relatórios de T-14a e T-14b refletem com fidelidade a homologação física reportada pelo usuário. O roteiro de 9 fluxos e o comando de logcat atendem rigorosamente a todos os critérios de aceite.

---

### 8. Commit e sincronização

- **Identificação do Commit:** `build(r8): aperto cirurgico das regras ProGuard e validacao em releaseCheck (T-29)`
- **Status do Commit:** Pendente de execução pelo Agente DevOps após a emissão deste relatório.
- **Status do Push:** Será sincronizado com `git push origin main`.
- **Status de Homologação:** 🟡 **IMPLEMENTADO, NÃO VERIFICADO NO APARELHO** (código, testes unitários, lint e compilação R8 100% aprovados; aguardando execução dos 9 fluxos no aparelho físico pelo usuário).
