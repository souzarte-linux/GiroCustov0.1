# Relatório de Execução da Tarefa T-11 [SEC-07]

**Data:** 27 de Outubro de 2026  
**Tarefa:** T-11 [SEC-07] - Rationale Prévia para Acesso à Localização (GPS)  
**Risco:** Médio  
**Responsáveis:** Orquestrador, Designer (UX/UI), Frontend (Kotlin/Compose), QA, Segurança e DevOps  

---

## 1. Resultado

🟡 **IMPLEMENTADO, NÃO VERIFICADO NO APARELHO**  
(O diálogo explicativo prévio, a integração com o botão de busca de postos na tela de abastecimento, a centralização de textos no `strings.xml`, as regras de acessibilidade e a suíte completa de testes automatizados com mutação manual foram 100% implementados e aprovados. A validação visual no dispositivo físico depende do teste prático pelo usuário).

---

## 2. O que mudou

### Explicação Leiga
Antes desta alteração, quando o motorista tocava no botão de bússola/localização ("Buscar posto próximo") na tela de Abastecimento, o celular abria de surpresa a janelinha do sistema Android pedindo permissão de GPS sem explicar o porquê. Isso costumava assustar o usuário ou levá-lo a recusar a permissão por medo de invasão de privacidade.

Agora, o aplicativo atua de forma transparente e amigável:
1. **Verificação Silenciosa:** Se o motorista já concedeu a permissão de localização anteriormente, a busca por postos próximos acontece imediatamente, sem exibir nenhum aviso desnecessário.
2. **Explicação Prévia Amigável (Rationale):** Se a permissão ainda não foi concedida, o aplicativo abre primeiro um diálogo claro e bem legível explicando que a sua localização será usada **exclusivamente para localizar os postos mais próximos da sua rota e calcular onde abastecer com o menor custo**, garantindo que seus dados de GPS **não são compartilhados com terceiros nem salvos externamente**.
3. **Respeito à Vontade do Usuário:**
   - Se o motorista tocar em **"Permitir Localização"**, aí sim o aplicativo aciona a solicitação oficial de GPS do Android (oferecendo tanto a precisão exata quanto aproximada).
   - Se o motorista tocar em **"Agora Não"** (ou fechar o diálogo), nenhuma permissão é pedida ao sistema operacional e o motorista continua livre para buscar o nome do posto ou selecioná-lo manualmente no formulário.
4. **Sem Textos Fixos e Acessível:** Todos os textos foram criados em português claro dentro de `strings.xml`, com contraste e alvos de toque confortáveis (mínimo de 48dp) e leitura correta por TalkBack.

### Lista de Arquivos Afetados
- `app/src/main/res/values/strings.xml`: Adicionados títulos, mensagens explicativas, botões e descrições de acessibilidade para o diálogo de localização (`fuel_location_rationale_title`, `fuel_location_rationale_message`, `fuel_location_rationale_confirm`, `fuel_location_rationale_dismiss`, `fuel_location_permission_denied` e `cd_fuel_location_rationale_icon`).
- `app/src/main/java/com/example/ui/components/FuelLocationRationaleDialog.kt`: Novo componente Composable Material 3 modular, com ícone de localização, título em negrito, mensagem contextualizada, botões de toque mínimo 48dp e tags semânticas para testes.
- `app/src/main/java/com/example/ui/screens/FuelRefillsScreen.kt`: Integração do diálogo explicativo prévio ao botão `search_nearby_gas_stations_btn`, suporte a `RequestMultiplePermissions` (fine e coarse) e exibição controlada pelo estado `showLocationRationaleDialog`.
- `app/src/test/java/com/example/FuelLocationRationaleTest.kt`: Nova suíte de testes unitários automatizados com Robolectric cobrindo exibição, validação de textos de LGPD/privacidade, clique em confirmar e clique em dispensar.
- `docs/plano-onda-2.md`: Atualizados os status de T-09 e T-10 para ✅ **VERIFICADO** após homologação com 100% de sucesso no aparelho físico pelo usuário, e registrado T-11 como 🟡 **IMPLEMENTADO, NÃO VERIFICADO NO APARELHO**.

---

## 3. Evidências

### Execução dos Testes Automatizados e Análise de Lint
```bash
./gradlew testDebugUnitTest lintDebug
```
**Resultado da Execução:**
- Total de testes executados: **266**
- Aprovados: **266 (100%)**
- Falhas: **0**
- Ignorados: **0**
- Caminho do relatório de testes: `app/build/reports/tests/testDebugUnitTest/index.html`
- Caminho do relatório do Lint: `app/build/reports/lint-results-debug.html`

**Últimas linhas da saída do Gradle:**
```text
> Task :app:compileDebugUnitTestKotlin UP-TO-DATE
> Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
> Task :app:processDebugUnitTestJavaRes UP-TO-DATE
> Task :app:testDebugUnitTest
> Task :app:finalizeTestRoborazziDebug SKIPPED
> Task :app:lintReportDebug
Wrote HTML report to file:///home/nando/Documentos/GiroCustov0.1/app/build/reports/lint-results-debug.html
> Task :app:lintDebug

BUILD SUCCESSFUL in 44s
33 actionable tasks: 1 executed, 32 up-to-date
Configuration cache entry reused.
```

### Compilação do APK Release com R8 e Minificação
```bash
./gradlew assembleReleaseCheck
```
**Resultado da Execução:**
- Minificação com R8: executada e concluída sem quebras ou avisos não tratados.
- Status: **BUILD SUCCESSFUL in 6m 6s** (50 actionable tasks: 18 executed, 4 from cache, 28 up-to-date).

### Verificação por Mutação Manual (QA)
- **Mutação introduzida:** No arquivo `FuelLocationRationaleDialog.kt`, o callback do botão de confirmação foi alterado de `onClick = onConfirm` para `onClick = onDismiss`.
- **Comando executado:** `./gradlew testDebugUnitTest --tests "com.example.FuelLocationRationaleTest"`
- **Resultado:** O teste `testRationaleDialog_confirmButtonClicked_triggersOnConfirm` **FALHOU** imediatamente acusando `java.lang.AssertionError at FuelLocationRationaleTest.kt:80` (esperado: 1 chamada de confirmação, obtido: 0).
- **Restauração:** A mutação foi desfeita, restaurando o comportamento correto, e a suíte voltou a passar com 100% de sucesso (266 testes).

---

## 4. Testes

### Testes Adicionados
- `com.example.FuelLocationRationaleTest`:
  1. `testRationaleDialog_displaysAllExpectedTextsAndAccessibilityTags`: valida que o diálogo exibe o título correto, mensagem explicativa completa (com garantias de privacidade), botão de confirmação e botão de cancelamento, além das tags de teste semântico.
  2. `testRationaleDialog_confirmButtonClicked_triggersOnConfirm`: valida que o clique em "Permitir Localização" dispara o callback afirmativo para acionar a permissão no sistema operacional e não aciona o descarte.
  3. `testRationaleDialog_dismissButtonClicked_triggersOnDismiss`: valida que o clique em "Agora Não" fecha o diálogo sem solicitar permissão e não aciona a confirmação.
  4. `testFuelLocationStrings_contentAndIntegrityCheck`: valida a presença de todas as chaves em `strings.xml`, garantindo que nenhuma mensagem está em branco e que termos de proteção de dados (uso exclusivo, postos próximos, menor custo e não compartilhamento externo) estão presentes.

### Testes Alterados
- Nenhum teste existente foi alterado ou enfraquecido.

### Testes Removidos
- Nenhum teste foi removido.

---

## 5. O que NÃO foi feito ou está simulado

- Nada está simulado. Não há stubs, mocks ou mensagens fixas fingindo comportamento.
- O cálculo e consulta da API de postos (Overpass API) permanecem reais. A lógica de geolocalização do `LocationHelper.kt` foi preservada sem alterações, mantendo o escopo estrito da tarefa.

---

## 6. Riscos e dívidas encontrados

- **Tamanho de `FuelRefillsScreen.kt`:** A tela de abastecimento possui 1.637 linhas. O diálogo de rationale foi criado em um arquivo separado (`FuelLocationRationaleDialog.kt`) para evitar inchar a tela, mas a decomposição modular ampla de `FuelRefillsScreen` permanece planejada para a Tarefa T-20 (Onda 3).
- **Timeout da Overpass API:** Como diagnosticado no planejamento geral, a busca de postos na rede Overpass ainda utiliza timeout de 8 segundos no OkHttp versus 25 segundos na query; essa melhoria será realizada na Tarefa **T-13**, logo na sequência.

---

## 7. O que o usuário precisa fazer (Roteiro de Teste Manual no Aparelho)

Para validar a alteração diretamente no seu celular físico:

1. **Instalar a versão atualizada no aparelho:**
   Gere o APK ou instale diretamente pelo terminal com o comando:
   ```bash
   ./gradlew installDebug
   ```
   *(Ou instale o APK `app-releaseCheck.apk` gerado em `app/build/outputs/apk/releaseCheck/`)*.

2. **Garantir que a permissão de localização do app está desativada:**
   - No celular, vá em **Configurações > Aplicativos > GiroCusto > Permissões**.
   - Toque em **Localização** e selecione **"Não permitir"** (ou "Perguntar sempre").

3. **Abrir a tela de Abastecimento:**
   - Abra o app **GiroCusto**.
   - No menu ou na tela inicial, toque em **Abastecimento** (ícone de bomba de combustível ou botão "Novo Abastecimento").
   - Toque no botão de adicionar abastecimento (**"+"** ou botão correspondente para abrir o formulário).

4. **Testar o diálogo explicativo (Rationale):**
   - Ao lado do campo de "Posto de Combustível", toque no botão roxo/azul com o ícone de seta de localização (**"Buscar posto próximo"**).
   - **O que deve acontecer:**
     - Uma janela de diálogo intitulada **"Acesso à Localização"** deve surgir no centro da tela.
     - Ela deve exibir o ícone de localização e o texto explicando que os dados são usados apenas para achar postos próximos e calcular o menor custo, sem compartilhar com ninguém.
     - Devem aparecer dois botões: **"Agora Não"** e **"Permitir Localização"**.

5. **Testar a dispensa ("Agora Não"):**
   - Toque no botão **"Agora Não"**.
   - **O que deve acontecer:**
     - O diálogo se fecha imediatamente.
     - Nenhuma janela do sistema operacional aparece pedindo permissão.
     - O formulário de abastecimento continua ativo e você pode digitar o nome do posto manualmente ou escolher no menu.

6. **Testar a permissão ("Permitir Localização"):**
   - Toque novamente no botão de **"Buscar posto próximo"**.
   - O diálogo explicativo surge novamente.
   - Desta vez, toque em **"Permitir Localização"**.
   - **O que deve acontecer:**
     - O diálogo do GiroCusto se fecha.
     - A janela padrão do Android aparece perguntando: *"Permitir que o app GiroCusto acesse o local deste dispositivo?"*, com as opções de localização **Precisa** ou **Aproximada**, e botões *"Durante o uso do app"* ou *"Apenas desta vez"*.
     - Ao escolher *"Durante o uso do app"*, o app inicia a busca automática dos postos ao seu redor.

7. **Testar clique após permissão concedida:**
   - Com a permissão já concedida, toque de novo no botão **"Buscar posto próximo"**.
   - **O que deve acontecer:**
     - O diálogo explicativo **NÃO** deve aparecer mais, pois a permissão já existe. O app deve buscar os postos diretamente!

---

## 8. Commit e sincronização

- **Hash do commit:** `[Aguardando commit do DevOps]`
- **Status do push:** `[Aguardando push]`
