# Relatório de Execução: Tarefa T-17 [TST-01]
## Substituição de Testes de Template por Testes de Regra Real (com Autorização Explícita)

---

### 1. Resultado
✅ **VERIFICADO** — Todos os testes de template legados e genéricos (`2 + 2 = 4`, leitura simples de `"GiroCusto"` e componente trivial Compose) foram substituídos por testes automatizados reais de validação das regras de negócio do GiroCusto (hodômetro, combustível, preços e parsing pt-BR), testes contextuais de strings formatadas no Android e teste de regressão visual com Roborazzi sobre o componente real de produção `GoalCard`. A suíte subiu de 308 para 336 testes com 100% de aprovação (0 falhas, 0 ignorados), teste de mutação manual comprovado, e compilação de release com minificação R8 aprovada com sucesso.

---

### 2. O que mudou
- **Para o usuário (em termos leigos):**
  - O projeto eliminou testes "falsos" ou genéricos que vinham de fábrica (como somar 2 + 2) e que não testavam nada do aplicativo de verdade.
  - Agora, o sistema de testes automatizados valida permanentemente as regras essenciais do dia a dia do motorista e entregador:
    * Se o motorista digitar o hodômetro final menor que o inicial, o teste garante que o erro será acusado.
    * Se digitar números com vírgula ou separadores de milhar (padrão brasileiro como "1.250,50"), o teste garante que o valor é compreendido sem travar o aplicativo.
    * Nos abastecimentos, se o motorista colocar um desconto maior que o valor total da conta, o teste garante que a conta não ficará negativa.
    * Se colocar mais combustível do que o tanque do veículo suporta, o sistema valida o alerta.
    * O teste visual inteligente (Roborazzi) agora tira uma foto exata do cartão de metas do painel (`GoalCard`), garantindo que o visual escuro, as barras de progresso verdes e as letras continuem bonitos e legíveis a cada alteração do código.

- **Arquivos alterados:**
  1. `app/src/main/java/com/example/ui/GiroCustoViewModel.kt`:
     - Extração do objeto puro `GiroCustoDomainCalculations`, encapsulando as regras de validação e cálculos matemáticos puros para execução ultraveloz na JVM.
     - Métodos da ViewModel continuam com 100% de compatibilidade pública via delegação direta.
  2. `app/src/main/res/values/strings.xml`:
     - Adição dos recursos parametrizados para alertas de combustível, mensagens de erro e labels calculados em pt-BR.
  3. `app/src/test/java/com/example/ExampleUnitTest.kt`:
     - Substituição do teste template `2 + 2 = 4` por 22 testes unitários puros JVM cobrindo limites de hodômetro, combustível, descontos, recálculo multidirecional e parsing de moeda/números pt-BR.
  4. `app/src/test/java/com/example/ExampleRobolectricTest.kt`:
     - Substituição do teste genérico por 8 testes instrumentados com Robolectric (SDK 34) validando contexto Android, injeção de parâmetros nas strings e ausência de caracteres UTF-8 corrompidos.
  5. `app/src/test/java/com/example/GreetingScreenshotTest.kt`:
     - Adaptação do teste Roborazzi para renderizar o componente real `GoalCard` configurado com metas diárias representativas dentro de `MyApplicationTheme`.
  6. `app/src/test/screenshots/goal_card.png`:
     - Novo screenshot oficial de referência visual gravado com Roborazzi.
  7. `app/src/test/screenshots/greeting.png`:
     - Arquivo legado órfão removido do repositório.
  8. `docs/plano-onda-2.md`:
     - Atualização do status de T-17 para ✅ **VERIFICADO**.

---

### 3. Evidências

#### 3.1 Execução da Suíte Completa de Testes Unitários
- **Comando executado:**
  ```bash
  ./gradlew testDebugUnitTest --rerun-tasks
  ```
- **Contagem real:**
  - **Executados:** 336
  - **Aprovados:** 336 (100% de sucesso)
  - **Falhas:** 0
  - **Ignorados / Pulados:** 0
- **Caminho do relatório de testes:**
  `app/build/reports/tests/testDebugUnitTest/index.html`
- **Últimas linhas da saída do Gradle:**
  ```text
  > Task :app:compileDebugUnitTestKotlin
  > Task :app:testDebugUnitTest
  > Task :app:finalizeTestRoborazziDebug SKIPPED

  BUILD SUCCESSFUL in 5m 22s
  33 actionable tasks: 33 executed
  Configuration cache entry reused.
  ```

#### 3.2 Gravação Visual Roborazzi
- **Comando executado:**
  ```bash
  ./gradlew recordRoborazziDebug
  ```
- **Resultado:**
  - Imagem gerada: `app/src/test/screenshots/goal_card.png` (42.586 bytes).
  - Componente validado: `GoalCard` com título "Metas Diárias", faturamento R$ 250,00 / R$ 300,00, lucro líquido R$ 180,00 / R$ 200,00, km 120,0 / 150,0 km e entregas 15 / 20.

#### 3.3 Verificação de Lint Android
- **Comando executado:**
  ```bash
  ./gradlew lintDebug
  ```
- **Resultado:**
  - Relatório: `app/build/reports/lint-results-debug.html`
  - Saída: `BUILD SUCCESSFUL in 2m 9s` (31 actionable tasks: 8 executed, 23 up-to-date, zero erros de compilação ou lint).

#### 3.4 Verificação de Release e R8
- **Comando executado:**
  ```bash
  ./gradlew assembleReleaseCheck
  ```
- **Resultado:**
  - Minificação R8 (`minifyReleaseCheckWithR8`) e Lint Vital (`lintVitalReleaseCheck`) aprovados com 100% de sucesso.
  - Saída: `BUILD SUCCESSFUL in 15m 49s` (50 actionable tasks: 16 executed, 4 from cache, 30 up-to-date).

---

### 4. Testes

- **Removidos com autorização explícita do usuário no chat (28/10/2026):**
  1. `ExampleUnitTest.addition_isCorrect()` — Testava `4 == 2 + 2` sem relação com o produto.
  2. `ExampleRobolectricTest.read string from context()` — Apenas lia estaticamente o `app_name`.
  3. `GreetingScreenshotTest.greeting_screenshot()` — Apenas tirava print de um `Text("GiroCusto")` isolado.

- **Adicionados / Expandidos (+28 novos testes líquidos):**
  - **`ExampleUnitTest` (+22 testes puros JVM):**
    1. `testOdometer_validProgression_calculatesCorrectDistance`: validação de progressão positiva (1000 a 1050 km -> 50 km).
    2. `testOdometer_commaDecimalPtBr_calculatesCorrectDistance`: aceitação de vírgula decimal brasileira ("1000,5" a "1050,7" -> 50,2 km).
    3. `testOdometer_endLessThanStart_returnsNegativeError`: detecção de hodômetro invertido.
    4. `testOdometer_startEqualsEnd_zeroKmReturnedAsValid`: hodômetro igual no dia resulta em 0,0 km válido.
    5. `testOdometer_blankOrSpaces_returnsNone`: campos vazios resultam em estado `None`.
    6. `testOdometer_invalidCharacters_returnsNone`: entrada malformada não lança exceção.
    7. `testFuel_zeroOrNegativeLiters_isInvalid`: litros menores ou iguais a zero são rejeitados.
    8. `testFuel_zeroOrNegativePrice_isInvalid`: preço por litro menor ou igual a zero é rejeitado.
    9. `testFuel_zeroOrNegativeOdometer_isInvalid`: hodômetro zerado ou negativo é rejeitado.
    10. `testFuel_validInputs_returnsTrue`: dados válidos retornam confirmação positiva.
    11. `testFuel_exceedsTankCapacity_validation`: alerta quando abastecimento excede capacidade do tanque (55L em tanque de 50L).
    12. `testFuel_computeRefillTotal_standardMath`: cálculo matemático com desconto (preço * litros - desconto).
    13. `testFuel_computeRefillTotal_discountExceedsTotal_floorsToZero`: proteção contra valor financeiro negativo.
    14. `testFuel_computeRefillTotal_explicitTotalPrioritized`: prioridade para o valor digitado diretamente.
    15. `testFuel_multiDirectional_twoFields_calculatesThirdField`: recálculo automático com 2 campos preenchidos.
    16. `testFuel_multiDirectional_threeFields_focusedRecalculation`: recálculo dinâmico ao editar com 3 campos.
    17. `testFuel_multiDirectional_invalidOrZeroInputs_returnsEmptyResult`: robustez contra entradas nulas ou zero.
    18. `testPtBrParsing_doubleWithCommaDecimal`: conversão correta de "12,50".
    19. `testPtBrParsing_doubleWithThousandSeparatorAndComma`: conversão de "1.250,50" e "5.000,00".
    20. `testPtBrParsing_doubleWithCurrencyAndSpaces`: tratamento de "R$ 45,90".
    21. `testPtBrParsing_doubleMalformed_returnsFallback`: segurança com retorno de fallback em texto inválido.
    22. `testPtBrParsing_intStandardAndMalformed`: parsing robusto de inteiros com fallback.

  - **`ExampleRobolectricTest` (+8 testes de contexto Android SDK 34):**
    1. `testResolveInvertedOdometerString_returnsExactPtBrText`: texto em pt-BR de hodômetro invertido.
    2. `testResolveExceedsTankCapacityString_formatsParameterizedArguments`: formatação de parâmetros "%1$.1f L / %2$.1f L".
    3. `testResolveCalculatedDistanceLabel_formatsCorrectly`: label parametrizado de distância calculada.
    4. `testResolveCalculatedTotalPrice_formatsCorrectly`: formatação de total calculado.
    5. `testResolveCalculatedLiters_formatsCorrectly`: formatação de litros calculados.
    6. `testResolveCalculatedPricePerLiter_formatsCorrectly`: formatação de preço por litro calculado.
    7. `testResolveGoalsSavedFeedback_returnsExactPtBrMessage`: mensagem amigável de metas salvas.
    8. `testPtBrAccentedCharactersIntegrity_doesNotCorruptUtf8`: validação de acentos e ausência de caracteres corrompidos.

  - **`GreetingScreenshotTest` (+1 teste visual Roborazzi):**
    1. `goal_card_screenshot`: teste de regressão visual sobre o `GoalCard` do painel com tema da aplicação.

- **Nenhum teste existente de autenticação, Room, repositório ou sincronização foi alterado ou enfraquecido.**

---

### 5. Verificação por Mutação Manual (Obrigatória por `integridade-de-testes.md`)

- **Mutação 1 (QA):**
  - Regra mutada: Inversão da condição em `calculateDailyDistance` de `diffKm >= 0.0` para `diffKm < 0.0`.
  - Resultado: Falha imediata em 4 testes de hodômetro (`testOdometer_validProgression`, `testOdometer_commaDecimalPtBr`, `testOdometer_startEqualsEnd`, `testOdometer_endLessThanStart`).
  - Restauração: Condição restaurada e suíte verde.

- **Mutação 2 (Revisor Independente):**
  - Regra mutada: Inversão da subtração de desconto em `computeRefillTotal` de `(price * lts - disc)` para `(price * lts + disc)`.
  - Resultado: Falha imediata em 2 testes de abastecimento (`testFuel_computeRefillTotal_standardMath` e `testFuel_computeRefillTotal_discountExceedsTotal_floorsToZero`).
  - Restauração: Condição restaurada e suíte verde.

---

### 6. O que NÃO foi feito ou está simulado
- Nada. Todos os testes são reais, sem mocks nas regras de negócio testadas, sem `@Ignore` e sem stubs.

---

### 7. Riscos e dívidas encontrados
- **Ressalva da Revisão (Dívida Mapeada):** A string `err_exceeds_tank_capacity` adicionada em `strings.xml` para validação no teste contextual está pronta para substituir o texto ainda fixo (*hardcoded*) na linha 1311 de `FuelRefillsScreen.kt`, o que deve ser consumido na futura tarefa de migração de textos da referida tela.
- **Ressalva de Screenshots Resolvida:** A imagem obsoleta `greeting.png` foi removida via `git rm`, mantendo rastreado apenas o screenshot oficial `goal_card.png`.

---

### 8. O que o usuário precisa fazer
Nenhuma ação manual é necessária neste momento no aparelho físico, pois esta tarefa é de melhoria interna da suíte de testes de desenvolvimento. Caso deseje verificar os testes rodando no seu computador:
1. Abra o terminal na raiz do projeto.
2. Execute `./gradlew testDebugUnitTest` e confira a aprovação dos 336 testes unitários.

---

### 9. Commit e sincronização
- **Hash do commit:** `[Aguardando execução do commit]`
- **Status do push:** `[Aguardando envio ao repositório]`
