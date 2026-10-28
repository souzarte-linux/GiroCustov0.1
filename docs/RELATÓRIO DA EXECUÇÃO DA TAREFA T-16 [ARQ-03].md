# Relatório de Execução: Tarefa T-16 [ARQ-03]
## Elevação de Regras de Negócio e Cálculos de Composables para ViewModel

---

### 1. Resultado
🟡 **IMPLEMENTADO, NÃO VERIFICADO NO APARELHO** — Todas as regras de negócio, validações de entrada e projeções matemáticas pesadas que estavam acopladas no interior dos Composables de lançamento, abastecimento e relatórios foram migradas para o `GiroCustoViewModel`, com exposição reativa via `StateFlow` e funções puras de cálculo. Foram criados 24 novos testes unitários automatizados, com 100% de aprovação e teste de mutação manual comprovado.

---

### 2. O que mudou
- **Para o usuário (em termos leigos):**
  - O aplicativo ficou mais rápido e responsivo ao preencher odômetros de corridas, abastecimentos e gráficos de relatórios.
  - Ao digitar o odômetro final em uma corrida, a distância do dia aparece instantaneamente sem congelar a tela. Se o odômetro final for digitado menor que o inicial, o aviso em vermelho continua surgindo com precisão sem recalcular repetidamente à toa.
  - No abastecimento, ao digitar preço e litros, ou litros e valor pago, o terceiro campo é calculado instantaneamente com proteção total contra valores negativos de combustível quando houver desconto.
  - Na tela de relatórios, o gráfico, médias diárias (faturamento, lucro, km), ganho por hora e projeções mensais agora são calculados na camada interna do aplicativo (ViewModel) e não mais dentro da tela Compose enquanto você rola o conteúdo.
  - Todas as mensagens de aviso e textos de cálculo foram centralizados no arquivo oficial de textos (`strings.xml`).

- **Arquivos alterados:**
  1. `app/src/main/res/values/strings.xml`: Adição de 6 novas strings parametrizadas para labels de distância calculada, erro de hodômetro invertido, descrições de acessibilidade e validações de abastecimento.
  2. `app/src/main/java/com/example/ui/GiroCustoViewModel.kt`:
     - Criação das estruturas de estado tipadas: `LaunchDistanceState` (sealed com `None`, `Valid`, `NegativeError`), `RefillCalculationResult`, `ReportsFilterState` e `ReportMetricsState`.
     - Implementação das funções puras de cálculo: `calculateDailyDistance`, `calculateRefillMultiDirectional`, `checkExceedsTankCapacity`, `computeRefillTotal` e `calculateReportMetrics`.
     - Criação dos `StateFlows` reativos: `launchCalculatedDistance` (combinando hodômetro inicial e final) e `reportMetrics` (combinando registros, manutenções, período filtrado, veículo ativo e meta mensal).
  3. `app/src/main/java/com/example/ui/screens/LaunchScreen.kt`:
     - Coleta reativa de `launchCalculatedDistance` via `collectAsStateWithLifecycle()`.
     - Remoção da subtração matemática bruta dentro do corpo da tela Compose, renderizando via `when (val dist = distanceState)`.
  4. `app/src/main/java/com/example/ui/screens/FuelRefillsScreen.kt`:
     - Delegação do cálculo multidirecional de 66 linhas de código no `LaunchedEffect` para `viewModel.calculateRefillMultiDirectional(...)`.
     - Delegação da checagem de estouro de tanque para `viewModel.checkExceedsTankCapacity(...)`.
     - Delegação do cálculo de fechamento para `viewModel.computeRefillTotal(...)` e uso de strings centralizadas para avisos.
  5. `app/src/main/java/com/example/ui/screens/ReportsScreen.kt`:
     - Coleta reativa de `viewModel.reportMetrics.collectAsStateWithLifecycle()`.
     - Remoção de mais de 70 linhas de processamento matemático in-composable (filtros de data, somas, faturamento médio, lucro líquido, ganho por hora, projeções mensais e percentuais da barra de despesas).
     - Vinculação direta de cartões, gráficos e projeções às propriedades limpas de `metrics`.
  6. `app/src/test/java/com/example/ViewModelCalculationsTest.kt`:
     - Nova classe de testes unitários com 24 métodos cobrindo todos os cenários felizes, bordas, erros e fluxos reativos.
  7. `docs/plano-onda-2.md`:
     - Atualização do status de T-12 para ✅ **VERIFICADO** e T-16 para 🟡 **IMPLEMENTADO, NÃO VERIFICADO NO APARELHO**.

---

### 3. Evidências

#### 3.1 Execução da Suíte Completa de Testes Unitários
- **Comando executado:**
  ```bash
  ./gradlew testDebugUnitTest
  ```
- **Contagem real:**
  - **Executados:** 308
  - **Aprovados:** 308 (100% de sucesso)
  - **Falhas:** 0
  - **Ignorados / Pulados:** 0
- **Caminho do relatório de testes:**
  `app/build/reports/tests/testDebugUnitTest/index.html`
- **Últimas linhas da saída do Gradle:**
  ```text
  > Task :app:compileDebugUnitTestKotlin UP-TO-DATE
  > Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
  > Task :app:processDebugUnitTestJavaRes UP-TO-DATE
  > Task :app:testDebugUnitTest
  > Task :app:finalizeTestRoborazziDebug SKIPPED

  BUILD SUCCESSFUL in 50s
  33 actionable tasks: 3 executed, 2 from cache, 28 up-to-date
  Configuration cache entry reused.
  ```

#### 3.2 Execução do Android Lint
- **Comando executado:**
  ```bash
  ./gradlew lintDebug
  ```
- **Resultado:**
  ```text
  > Task :app:lintReportDebug
  Wrote HTML report to file:///home/nando/Documentos/GiroCustov0.1/app/build/reports/lint-results-debug.html
  > Task :app:lintDebug

  BUILD SUCCESSFUL in 3m 39s
  31 actionable tasks: 8 executed, 23 up-to-date
  ```

#### 3.3 Execução do Build Debug
- **Comando executado:**
  ```bash
  ./gradlew assembleDebug
  ```
- **Resultado:**
  ```text
  BUILD SUCCESSFUL in 1m 23s
  39 actionable tasks: 4 executed, 35 up-to-date
  ```

#### 3.4 Verificação por Mutação Manual (obrigatória)
1. **Quebra introduzida de propósito:**
   - Em `app/src/main/java/com/example/ui/GiroCustoViewModel.kt`, linha 520, invertemos a condição de hodômetro positivo de `diffKm >= 0.0` para `diffKm < 0.0`.
2. **Confirmação da quebra pelo teste:**
   - O teste `ViewModelCalculationsTest.testCalculateDailyDistance_validPositiveDistance` **FALHOU** imediatamente com `AssertionError at ViewModelCalculationsTest.kt:66` (`1 test completed, 1 failed`).
3. **Reversão e confirmação de sucesso:**
   - A condição foi restaurada para `diffKm >= 0.0` e todos os 308 testes voltaram a passar com 100% de sucesso.

---

### 4. Testes
- **Nenhum teste existente foi alterado ou enfraquecido.**
- **Testes adicionados (24 testes na nova classe `ViewModelCalculationsTest.kt`):**
  1. `testCalculateDailyDistance_validPositiveDistance`: cálculo correto e formatação de km em distância positiva.
  2. `testCalculateDailyDistance_commaDecimalFormat`: suporte a decimais com vírgula padrão pt-BR.
  3. `testCalculateDailyDistance_zeroDistance`: validação de hodômetro com distância zero km.
  4. `testCalculateDailyDistance_negativeOdometerError`: detecção precisa de hodômetro invertido (final menor que inicial).
  5. `testCalculateDailyDistance_blankOrInvalidInputs`: proteção contra entradas vazias, espaços ou caracteres não numéricos.
  6. `testLaunchCalculatedDistance_reactiveFlowUpdates`: comprovação da emissão reativa via StateFlow combinando campos do formulário.
  7. `testRefillMultiDirectional_twoFields_priceAndLiters_calculatesTotal`: cálculo de total a partir de preço e litros com desconto.
  8. `testRefillMultiDirectional_twoFields_litersAndTotal_calculatesPrice`: cálculo reverso de preço a partir de litros e valor pago.
  9. `testRefillMultiDirectional_twoFields_priceAndTotal_calculatesLiters`: cálculo reverso de litros a partir de preço e valor pago.
  10. `testRefillMultiDirectional_threeFields_focusTotal_recalculatesLiters`: recálculo de litros quando o foco do motorista está no valor total.
  11. `testRefillMultiDirectional_threeFields_focusPrice_recalculatesTotal`: recálculo de total quando o foco do motorista está no preço.
  12. `testRefillMultiDirectional_threeFields_focusDiscount_recalculatesTotal`: recálculo de total após inserção de desconto.
  13. `testRefillMultiDirectional_invalidOrZeroInputs_returnsEmptyResult`: imunidade a divisões por zero ou valores zerados.
  14. `testCheckExceedsTankCapacity_exceedsCapacity_returnsTrue`: alerta quando litros ultrapassam capacidade cadastrada.
  15. `testCheckExceedsTankCapacity_withinOrEqualCapacity_returnsFalse`: tolerância dentro da capacidade máxima do tanque.
  16. `testCheckExceedsTankCapacity_zeroOrInvalidCapacity_returnsFalse`: segurança para capacidades inválidas ou zeradas.
  17. `testComputeRefillTotal_withExplicitTotal_returnsParsedTotal`: preservação do valor digitado explicitamente.
  18. `testComputeRefillTotal_withBlankTotal_computesPriceTimesLitersMinusDiscount`: fallback matemático quando o total está em branco.
  19. `testComputeRefillTotal_negativeResult_coercedToZero`: garantia matemática de que o total não se torna negativo se o desconto superar o valor.
  20. `testCalculateReportMetrics_emptyRecords_returnsZeroedState`: imunidade a listas vazias sem falha ou divisão por zero.
  21. `testCalculateReportMetrics_singleDayFinancialCalculations`: validação das contas de combustível, desgaste, custo fixo, faturamento e ganho por hora.
  22. `testCalculateReportMetrics_multipleDaysBestAndWorst`: identificação precisa do melhor dia (maior lucro) e pior dia (menor lucro).
  23. `testCalculateReportMetrics_withMaintenanceRecords`: incorporação de despesas de manutenção no lucro líquido real.
  24. `testCalculateReportMetrics_periodFiltering_SEMANA_and_PERSONALIZADO`: filtro correto de registros por período e contagem proporcional.

---

### 5. O que NÃO foi feito ou está simulado
- Nada está simulado e nenhum stub foi introduzido.
- Os layouts visuais das três telas (`LaunchScreen`, `FuelRefillsScreen`, `ReportsScreen`) e suas paletas de cores foram rigorosamente mantidos.

---

### 6. Riscos e dívidas encontrados
- O `GiroCustoViewModel.kt` centralizou os cálculos, mas seu tamanho geral aumentou para cerca de 2.000 linhas. As tarefas arquiteturais de decomposição (**T-20**) na Onda 3 serão fundamentais para separar essas responsabilidades em UseCases/Domain delegados.

---

### 7. O que o usuário precisa fazer (Roteiro de Teste no Aparelho Físico)

Para certificar a tarefa no celular físico, siga este roteiro de verificação:

#### Passo 1: Instalação da Versão Atualizada
1. Conecte o celular ao computador via USB com a Depuração USB ativada.
2. No terminal do computador, execute:
   ```bash
   ./gradlew installDebug
   ```
3. Abra o app **GiroCusto** no celular.

#### Passo 2: Teste de Lançamento de Corrida (`LaunchScreen`)
1. No menu inferior, toque em **Novo Lançamento** (ou no botão flutuante **+** de lançamento diário).
2. No campo **Hodômetro Inicial**, digite `10000`.
3. No campo **Hodômetro Final**, digite `10080`.
   - **Verificação visual:** Veja se logo abaixo surge o badge verde indicando `"Distância calculada no dia: 80,0 km"`.
4. Agora altere o **Hodômetro Final** para `9950` (valor menor que o inicial).
   - **Verificação visual:** O badge verde deve sumir e no lugar deve aparecer o alerta em vermelho: `"Hodômetro final não pode ser menor que o inicial."`.

#### Passo 3: Teste de Abastecimento (`FuelRefillsScreen`)
1. No menu inferior ou lateral, acesse **Abastecimentos**.
2. Toque em **Novo Abastecimento**.
3. No campo **Preço por Litro**, digite `5,80`.
4. No campo **Litros**, digite `10`.
   - **Verificação visual:** O campo **Total Pago** deve se autopreencher automaticamente com `58,00`.
5. Apague o campo **Litros** e digite no campo **Total Pago** o valor `116,00`.
   - **Verificação visual:** O campo **Litros** deve se autopreencher com `20,00` litros.
6. Digite no campo **Litros** um valor absurdo, como `100` litros (maior que o tanque do veículo cadastrado).
   - **Verificação visual:** Deve aparecer o aviso em amarelo indicando que a quantidade ultrapassa a capacidade máxima do tanque.

#### Passo 4: Teste da Tela de Relatórios (`ReportsScreen`)
1. No menu, navegue até a aba **Relatórios**.
2. Alterne entre os filtros de período: **Semana**, **Quinzena**, **Mês Atual** e **Últimos 30 dias**.
   - **Verificação visual:** Note a transição suave e rápida dos cartões (Total Faturado, Lucro Líquido Real, Média/Dia, Ganho por Hora) e da barra colorida de despesas (Combustível, Alimentação, Desgaste, Custos Fixos e Manutenções).

---

### 8. Commit e sincronização
- **Hash do commit:** `PENDENTE` (será gerado a seguir)
- **Status do push:** `PENDENTE`
