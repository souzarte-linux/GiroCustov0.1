# Plano de Migração Monetária: Double para Centavos (Long)
**Projeto:** GiroCusto (Android Nativo - Kotlin, Jetpack Compose, Room, Supabase)  
**Tarefa:** T-18A [DAT-01A] - Diagnóstico e Mapeamento de Impacto (Zero Código)  
**Autor:** Agente Backend / Dados  
**Data:** 28 de Outubro de 2026  
**Status do Documento:** 🟡 PROPOSTO (Aguardando Aprovação Formal do Usuário)

---

## 1. Sumário Executivo e Diagnóstico de Causa Raiz

### 1.1 Contexto e Justificativa Financeira
O **GiroCusto** é um aplicativo financeiro e operacional voltado para motoristas e entregadores de aplicativo (iFood, Uber, etc.), que dependem da exatidão de cada centavo para saber sua remuneração líquida real após custos de combustível, manutenção corretiva, alimentação e depreciação de peças.

Atualmente, todos os valores monetários no GiroCusto estão modelados e persistidos como `Double` (ponto flutuante binário de dupla precisão, padrão IEEE 754). Embora o tipo `Double` seja intuitivo na fase inicial de prototipagem, sua utilização em sistemas financeiros é tecnicamente perigosa e condenada pelas boas práticas da engenharia de software (Clean Architecture, Martin Fowler, padrões bancários e regras de arquitetura do GiroCusto expressas em `equipe-especialistas.md` e `padroes-seguranca-ux.md`).

### 1.2 Por Que `Double` é Perigoso em Finanças
O padrão IEEE 754 codifica números na base 2 (binária). Frações decimais simples em base 10 (como `0.10`, `0.20`, `0.05`) não possuem representação finita em binário, resultando em dízimas periódicas binárias:
- `0.1 + 0.2` em ponto flutuante avalia para `0.30000000000000004`.
- `50.0 - 49.99` avalia para `0.010000000000005116`.

No GiroCusto, a fórmula central de fechamento de turno em [Entities.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/data/Entities.kt#L97):
```kotlin
val netProfit: Double // grossEarnings - fuelCost - wearCost - proportionalFixedCost - foodExpense
```
realiza sucessivas subtrações de ponto flutuante. Ao acumular diárias em relatórios mensais e anuais (dezenas ou centenas de registros), esse ruído binário se multiplica:
1. **Divergência de Consolidação:** A soma dos lucros líquidos diários individuais não bate exatamente com o lucro calculado sobre a soma dos faturamentos e despesas agregadas.
2. **Comparação Falha:** Qualquer comparação direta de igualdade (`if (totalPaid == calculatedTotal)`) falha silenciosamente devido a resíduos na 16ª casa decimal.
3. **Instabilidade em Validações:** Para evitar loops infinitos em recálculos automáticos na tela de abastecimento, a equipe anterior foi obrigada a adotar gambiarras com tolerâncias arbitrárias, como em [GiroCustoViewModel.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/GiroCustoViewModel.kt#L153):
   ```kotlin
   if (Math.abs(currentPrice - autoPrice) > 0.009) { ... }
   ```

### 1.3 Diagnóstico dos Hacks de `Locale.GERMAN` e Parsing Manual no Código Atual
A auditoria minuciosa no código-fonte revelou um sintoma grave gerado pela indefinição de um padrão monetário:

1. **Uso de `Locale.GERMAN` em vez de `Locale("pt", "BR")`:**
   Como a formatação de moeda brasileira exige vírgula para centavos e ponto para milhares (`R$ 1.234,56`), mas a JVM/Android muitas vezes opera em `Locale.US` (`1,234.56`), foram espalhadas mais de 30 chamadas com `Locale.GERMAN` por todo o projeto como um atalho incorreto para forçar o uso da vírgula decimal alemã:
   - [HistoryScreen.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/screens/HistoryScreen.kt#L713): Linhas 713, 748, 758, 768, 803, 1033, 1046, 1088, 1101, 1149, 1162 (`String.format(Locale.GERMAN, "R$ %.2f", ...)`)
   - [DashboardScreen.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/screens/DashboardScreen.kt#L391): Linhas 391, 400, 418, 439, 464, 468, 472, 476, 480, 506, 540
   - [ReportsScreen.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/screens/ReportsScreen.kt#L490): Linhas 490, 505, 586, 614, 666, 676, 697, 747
   - [LaunchScreen.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/screens/LaunchScreen.kt#L848): Linhas 848, 905, 952, 1235
   - [ProfileScreen.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/screens/ProfileScreen.kt#L1479): Linhas 1479, 1609
   - [GiroCustoViewModel.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/GiroCustoViewModel.kt#L714): Linhas 714, 718

2. **Inconsistência Entre Telas:**
   Enquanto as telas acima usam `Locale.GERMAN`, a tela [FuelRefillsScreen.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/screens/FuelRefillsScreen.kt#L258) (linhas 258 e 347) usa `Locale.getDefault()`, gerando formatações com ponto em aparelhos configurados em inglês, e [MaintenanceScreen.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/screens/MaintenanceScreen.kt#L155) (linha 155) instancia localmente `NumberFormat.getCurrencyInstance(Locale("pt", "BR"))`.

3. **Bugs de Parsing Silencioso nos Formulários:**
   A ausência de uma classe centralizada gerou diversas implementações ad-hoc de parsing:
   - Em [DashboardScreen.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/screens/DashboardScreen.kt#L815-L837), foi criada uma função local `parsePtBrDouble` com 5 condicionais de `replace(".", "").replace(',', '.')` e captura genérica de `catch (_: Exception)`.
   - Em [ProfileScreen.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/screens/ProfileScreen.kt#L2498), linha 2498 (`val fCostsVal = fixedCosts.toDoubleOrNull() ?: 180.0`) e linhas 2633 e 2750 (`val p = price.toDoubleOrNull() ?: 0.0`), o código chama `toDoubleOrNull()` diretamente na string digitada. Se o entregador brasileiro digitar `"180,50"` ou `"45,00"`, o Kotlin retorna `null`, fazendo com que os custos fixos caiam silenciosamente para o valor padrão de 180.0 ou a peça nem sequer seja cadastrada!
   - Em [HistoryScreen.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/screens/HistoryScreen.kt#L1415-L1418), as linhas 1415, 1417 e 1418 chamam `gross.toDoubleOrNull()` diretamente, quebrando a edição de registros se o usuário usar vírgula.
   - Em [FuelRefillsScreen.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/screens/FuelRefillsScreen.kt#L1476) e [GiroCustoViewModel.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/GiroCustoViewModel.kt#L1946-L1950), há dezenas de repetições isoladas de `.replace(",", ".").toDoubleOrNull()`.

### 1.4 A Solução Arquitetural: Centavos em `Long`
A solução definitiva e aderente aos padrões da arquitetura GiroCusto é a representação de valores monetários como números inteiros de centavos em `Long` (64 bits):
- R$ 1,00 = `100L`
- R$ 150,50 = `15050L`
- R$ 0,01 = `1L`
- R$ -45,20 = `-4520L`

**Por que `Long` e não `Int` ou `BigDecimal` no Room?**
1. `Long` tem limite de $9 \times 10^{18}$ centavos (mais de 90 quatrilhões de reais), eliminando qualquer risco de estouro de inteiro (`overflow`).
2. Operações de soma, subtração e ordenação em SQLite e na JVM são atômicas e executadas diretamente em registradores da CPU, com custo computacional nulo e sem alocação de objetos em heap (diferente de `BigDecimal`).
3. Elimina completamente erros de ponto flutuante em relatórios e consolidações.
4. Para taxas fracionárias (ex: consumo de combustível por km ou rateio por dias), a conta utiliza divisão escalar com arredondamento explícito (`RoundingMode.HALF_UP`).

---

## 2. Mapeamento Exaustivo dos Campos Monetários

Auditoria realizada diretamente nas entidades Room em [Entities.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/data/Entities.kt) e no schema `14.json`.

### 2.1 Campos Que DEVEM Mudar para `Long` (Centavos)

A tabela abaixo relaciona os **19 campos monetários** em **6 entidades**:

| Entidade / Tabela Room | Campo Atual | Tipo Atual | Tipo Proposto | Descrição Financeira | Exemplo Atual | Exemplo em Centavos |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **`Vehicle`** (`vehicle`) | `monthlyFixedCosts` | `Double` | `Long` | Custo fixo mensal (IPVA, seguro, MEI) | `180.0` | `18000L` |
| **`VehiclePart`** (`vehicle_parts`) | `price` | `Double` | `Long` | Preço da peça de reposição | `45.0` | `4500L` |
| **`DailyRecord`** (`daily_records`) | `grossEarnings` | `Double` | `Long` | Faturamento bruto da diária | `250.50` | `25050L` |
| **`DailyRecord`** (`daily_records`) | `fuelPrice` | `Double` | `Long` | Preço por litro do combustível usado | `5.89` | `589L` |
| **`DailyRecord`** (`daily_records`) | `foodExpense` | `Double` | `Long` | Gasto com alimentação no turno | `25.0` | `2500L` |
| **`DailyRecord`** (`daily_records`) | `fuelCost` | `Double` | `Long` | Custo total de combustível do turno | `32.40` | `3240L` |
| **`DailyRecord`** (`daily_records`) | `wearCost` | `Double` | `Long` | Custo de desgaste acumulado de peças | `15.80` | `1580L` |
| **`DailyRecord`** (`daily_records`) | `proportionalFixedCost` | `Double` | `Long` | Custo fixo rateado por dia | `8.18` | `818L` |
| **`DailyRecord`** (`daily_records`) | `netProfit` | `Double` | `Long` | Lucro líquido final apurado | `169.12` | `16912L` |
| **`FuelRefill`** (`fuel_refills`) | `pricePerLiter` | `Double` | `Long` | Preço pago por litro | `5.89` | `589L` |
| **`FuelRefill`** (`fuel_refills`) | `discount` | `Double` | `Long` | Desconto em dinheiro obtido | `2.0` | `200L` |
| **`FuelRefill`** (`fuel_refills`) | `totalPaid` | `Double` | `Long` | Valor total pago no abastecimento | `56.90` | `5690L` |
| **`MaintenanceRecord`** (`maintenance_records`) | `value` | `Double` | `Long` | Valor pago pelo serviço/peça | `120.0` | `12000L` |
| **`UserGoals`** (`user_goals`) | `dailyGoalGross` | `Double` | `Long` | Meta diária de faturamento bruto | `200.0` | `20000L` |
| **`UserGoals`** (`user_goals`) | `dailyGoalNet` | `Double` | `Long` | Meta diária de lucro líquido | `130.0` | `13000L` |
| **`UserGoals`** (`user_goals`) | `weeklyGoalGross` | `Double` | `Long` | Meta semanal de faturamento bruto | `1200.0` | `120000L` |
| **`UserGoals`** (`user_goals`) | `weeklyGoalNet` | `Double` | `Long` | Meta semanal de lucro líquido | `800.0` | `80000L` |
| **`UserGoals`** (`user_goals`) | `monthlyGoalGross` | `Double` | `Long` | Meta mensal de faturamento bruto | `5000.0` | `500000L` |
| **`UserGoals`** (`user_goals`) | `monthlyGoalNet` | `Double` | `Long` | Meta mensal de lucro líquido | `3200.0` | `320000L` |

### 2.2 Lista Explícita dos Campos Que NÃO SÃO Monetários (Devem Permanecer Inalterados)

É proibido alterar os seguintes campos para `Long` em centavos, pois representam grandezas físicas, contagens, porcentagens ou métricas temporais:

1. **Quilometragens e Hodômetros (`Double`):**
   - `Vehicle.currentOdometer` (km)
   - `DailyRecord.startOdometer` e `DailyRecord.endOdometer` (km)
   - `DailyRecord.kmRodados` (km)
   - `FuelRefill.odometer` (km)
   - `MaintenanceRecord.odometer` (km)
   - `VehiclePart.lifespanKm` e `VehiclePart.runKmSinceChange` (km)
   - `UserGoals.dailyGoalKm`, `weeklyGoalKm`, `monthlyGoalKm` (km)
2. **Volumes e Consumo (`Double`):**
   - `Vehicle.averageConsumption` (km/L)
   - `Vehicle.tankCapacity` (Litros)
   - `FuelRefill.liters` (Litros)
3. **Contadores e Dias (`Int`):**
   - `Vehicle.plannedWorkDays` (dias úteis de trabalho)
   - `DailyRecord.deliveriesCount` (quantidade de entregas realizadas)
   - `FuelRefill.installmentsCount` (número de parcelas)
   - `UserGoals.dailyGoalDeliveries`, `weeklyGoalDeliveries`, `monthlyGoalDeliveries` (quantidade de entregas)
4. **Porcentagens e Taxas Calculadas:**
   - `VehiclePart.healthPercentage` e `wearPercentage` (`Double`, percentual de 0 a 100%)
   - `VehiclePart.wearCostPerKm`: taxa em R$/km (ver análise detalhada na Seção 4)
5. **Horários e Durações:**
   - `DailyRecord.startTime`, `endTime`, `pauseDuration` (`String`, "HH:mm")
   - `DailyRecord.totalWorkMinutes`, `pauseMinutes` (`Long`, minutos inteiros)
   - `DailyRecord.totalWorkHours` (`Double`, horas fracionadas)
6. **Entidades Sem Campos Monetários:**
   - `UserProfile`: armazena apenas dados cadastrais (nome, telefone, endereço, flags).
   - `Platform`: armazena regras de repasse e dados bancários (`fixedPayDelay` é dias).

---

## 3. Mapeamento de DTOs e Sincronização com Supabase PostgreSQL

### 3.1 Situação Atual do Schema Remoto no Supabase
Nas migrations do Supabase em [20261021_init_girocusto_schema.sql](file:///home/nando/Documentos/GiroCustov0.1/supabase/migrations/20261021_init_girocusto_schema.sql) e [20261026_user_goals.sql](file:///home/nando/Documentos/GiroCustov0.1/supabase/migrations/20261026_user_goals.sql), as colunas monetárias estão definidas como `DOUBLE PRECISION`:
- `vehicle.monthly_fixed_costs`
- `vehicle_parts.price`
- `daily_records.gross_earnings`, `fuel_price`, `food_expense`, `fuel_cost`, `wear_cost`, `proportional_fixed_cost`, `net_profit`
- `fuel_refills.price_per_liter`, `discount`, `total_paid`
- `maintenance_records.value`
- `user_goals.daily_goal_gross`, `daily_goal_net`, etc.

Os DTOs em [SupabaseModels.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/network/supabase/SupabaseModels.kt) espelham esses campos como `Double` via anotações `@Json` do Moshi.

### 3.2 Comparação de Estratégias

```
+---------------------------------------------------------------------------------------------------+
| ESTRATÉGIA A: DTO em Double na Borda (Wire) + Conversão nos Mappers Kotlin (RECOMENDADA)           |
+---------------------------------------------------------------------------------------------------+
|  [Room SQLite Local]           [SupabaseMappers]           [Moshi DTOs]           [Supabase Cloud]|
|  grossEarnings: Long (centavos) <== toEntity() <======== grossEarnings: Double <== PostgreSQL     |
|                                 ==> toDto() ===========> (reais 150.50)           DOUBLE PRECISION|
+---------------------------------------------------------------------------------------------------+
| ESTRATÉGIA B: Migração Total de Schema Remoto no PostgreSQL para BIGINT (NÃO RECOMENDADA)         |
+---------------------------------------------------------------------------------------------------+
|  [Room SQLite Local]                                       [Moshi DTOs]           [Supabase Cloud]|
|  grossEarnings: Long (centavos) <========================> grossEarnings: Long <== PostgreSQL     |
|                                                            (centavos 15050)       BIGINT          |
+---------------------------------------------------------------------------------------------------+
```

#### Análise da Estratégia A (Recomendada)
- **Como funciona:**
  - O banco local SQLite/Room e toda a lógica de negócio interna passam a operar 100% com `Long` (centavos).
  - Os DTOs de rede (`DailyRecordDto`, `VehicleDto`, etc.) continuam declarando esses campos como `Double` (reais), preservando exatamente o contrato JSON da API REST PostgREST.
  - As conversões ocorrem estritamente nos adaptadores em [SupabaseMappers.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/network/supabase/SupabaseMappers.kt):
    - `Entity.toDto()`: `grossEarnings = CurrencyUtils.centsToDouble(grossEarnings)` (ou seja, `grossEarnings / 100.0`).
    - `Dto.toEntity()`: `grossEarnings = CurrencyUtils.toCents(grossEarnings)` (ou seja, `Math.round(grossEarnings * 100.0)`).
- **Vantagens:**
  1. **Risco Zero de Quebra em Produção:** Nenhuma migration SQL precisa ser aplicada no Supabase em produção. Usuários com dados já salvos na nuvem continuam sincronizando normalmente.
  2. **Compatibilidade LWW (Last-Write-Wins):** O mecanismo de resolução de conflitos baseado em timestamp UTC (`updatedAt` / `updated_at`) opera nos metadados, sem qualquer interferência do tipo de dados monetário.
  3. **Isolamento de Camadas:** A borda da rede permanece desacoplada do modelo de domínio interno.
  4. **Segurança do Algoritmo de Portabilidade LGPD:** O script [UserDataExportHelper.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/util/UserDataExportHelper.kt) continuará exportando valores em reais formatados para o usuário leigo, sem exigir que ele interprete centavos.

#### Análise da Estratégia B (Não Recomendada)
- **Como funciona:**
  - Aplicar migration `ALTER TABLE ... ALTER COLUMN ... TYPE BIGINT USING ROUND(col * 100)::bigint` nas 6 tabelas do Supabase.
  - Alterar todos os DTOs do Moshi para `Long`.
- **Riscos Graves e Pontos de Falha:**
  1. **Incompatibilidade Retroativa:** Caso existam instalações em aparelhos que ainda não atualizaram o APK, o envio de payload com números decimais quebrará o PostgREST com erro HTTP 400 (`invalid input syntax for type bigint`).
  2. **Ambiguidade de Escala:** Se por um milissegundo houver descompasso entre a versão remota e local, um registro de R$ 150,00 poderá ser gravado como 150 centavos (R$ 1,50) ou interpretado como R$ 15.000,00.
  3. **Manutenção de Views / RPCs:** Qualquer stored procedure futura ou script de painel administrativo que leia o banco precisará conhecer a convenção interna de centavos.

### 3.3 Recomendação Técnica
**Aprovação exclusiva da Estratégia A.** Ela oferece todos os benefícios matemáticos da precisão inteira no aplicativo do entregador, sem gerar dependência de sincronização em duas pontas ou risco de corrupção dos dados remotos.

---

## 4. Mapeamento nos ViewModels, Cálculos e Telas

### 4.1 `GiroCustoViewModel.kt`
Arquivo central de cálculos. Mapeamento das funções e estados que serão ajustados:

1. **Validação de Abastecimento:**
   - [Linhas 104-109](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/GiroCustoViewModel.kt#L104-L109): `isValidFuelInput` -> valida `pricePerLiterCents > 0L`.
   - [Linhas 116-123](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/GiroCustoViewModel.kt#L116-L123): `computeRefillTotal` -> cálculo em centavos:
     `totalCents = Math.round(liters * pricePerLiterCents) - discountCents`.
   - [Linhas 125-202](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/GiroCustoViewModel.kt#L125-L202): `calculateRefillMultiDirectional` -> Eliminação do hack de tolerância `> 0.009`! Em centavos, a comparação de divergência de arredondamento é exata (`Math.abs(currentTotalCents - autoTotalCents) > 0L`).
2. **Estrutura `ReportMetricsState`:**
   - [Linhas 215-224](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/GiroCustoViewModel.kt#L215-L224): Os campos `totalEarnings`, `totalFuelCost`, `totalWearCost`, `totalFixedCost`, `totalFoodCost`, `totalExpenses`, `totalMaintenanceCost`, `realExpenses`, `netProfit`, `realNetProfit`, `avgEarningsPerDay`, `avgProfitPerDay`, `projectedMonthlyProfit`, `projectedMonthlyEarnings`, `sumAllExpenses` passam para `Long` (centavos).
   - `grossPerHour` e `netPerHour`: passam para `Long` (centavos por hora trabalhada).
3. **Cálculo de Relatórios (`calculateReportMetrics`):**
   - [Linhas 669-774](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/GiroCustoViewModel.kt#L669-L774):
     - `val totalEarnings = filtered.sumOf { it.grossEarnings }` (soma atômica de `Long`).
     - `val totalExpenses = totalFuel + totalWear + totalFixed + totalFood` (soma de `Long`).
     - `val netProfit = totalEarnings - totalExpenses` (subtração exata de inteiros, sem resíduos).
     - Projeções: `avgProfitPerDay * workingDays` onde `avgProfitPerDay = realNetProfit / count`.
4. **Metas (`UserGoals`):**
   - [Linhas 1621-1665](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/GiroCustoViewModel.kt#L1621-L1665): `updateGoals` recebe parâmetros monetários como `Long` (centavos).
5. **Estimativa em Tempo Real (`realTimeEstimation`):**
   - [Linhas 1940-1991](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/GiroCustoViewModel.kt#L1940-L1991): `EstimationDetail` passa a conter campos de custo e lucro em `Long` (centavos).

### 4.2 Telas da Interface do Usuário (Compose)

| Tela | Localização | O Que Muda | Linhas de Referência |
| :--- | :--- | :--- | :--- |
| **`LaunchScreen.kt`** | Diária & Estimativas | Substituir `String.format(Locale.GERMAN, "R$ %.2f", ...)` por `CurrencyUtils.formatCents(...)`. Parsing dos campos de texto (faturamento, alimentação, preço combustível) utilizando `CurrencyUtils.parseToCents(...)`. | Linhas 848, 905, 952, 1235 |
| **`FuelRefillsScreen.kt`** | Abastecimentos | Substituir `String.format(Locale.getDefault(), ...)` por `CurrencyUtils.formatCents(...)`. Parsing de preço do litro, desconto e valor pago com `CurrencyUtils.parseToCents(...)`. | Linhas 258, 347, 1476-1479 |
| **`ReportsScreen.kt`** | Relatórios & Gráficos | Formatação dos cartões de métricas, valores da legenda segmentada, rendimento por hora e projeções mensais via `CurrencyUtils.formatCents(...)`. | Linhas 380-384, 490, 505, 586, 614, 666, 676, 697, 747 |
| **`HistoryScreen.kt`** | Histórico & Edição | Eliminar todos os `Locale.GERMAN` do item de histórico e dos cabeçalhos agrupadores (ano, mês, semana). Corrigir diálogo de edição substituindo `gross.toDoubleOrNull()` por `CurrencyUtils.parseToCents(...)`. | Linhas 713, 748, 758, 768, 803, 1033, 1046, 1088, 1101, 1149, 1162, 1413-1422 |
| **`DashboardScreen.kt`** | Painel Principal & Metas | Exibição de lucro líquido real, manutenção e resumo de custos formatados por `CurrencyUtils.formatCents(...)`. Eliminar métodos locais duplicados `parsePtBrDouble` e `formatMoneyGoal` em favor da classe central. | Linhas 391, 400, 418, 439, 464, 468, 472, 476, 480, 506, 540, 815-850 |
| **`ProfileScreen.kt`** | Veículo & Peças | Corrigir bugs de digitação em custos fixos e preço de peças (substituir `toDoubleOrNull()` que ignora vírgula por `CurrencyUtils.parseToCents(...)`). Formatação de custos e peças via `CurrencyUtils`. | Linhas 1479, 1609, 2498, 2633, 2750 |
| **`MaintenanceScreen.kt`**| Manutenção | Substituir instância local de `NumberFormat` e chamadas `valueStr.replace(",", ".").toDoubleOrNull()` por `CurrencyUtils`. | Linhas 155, 225, 284, 511-512 |

---

## 5. Especificação Técnica da Classe Central `CurrencyUtils.kt`

A classe utilitária única será criada no pacote `com.example.util` com o objetivo de centralizar todas as operações de conversão, formatação, parsing e operações aritméticas com dinheiro no GiroCusto.

### 5.1 Contrato da Classe

```kotlin
package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyUtils {

    const val CENTS_PER_REAL = 100L
    val ptBrLocale: Locale = Locale("pt", "BR")

    private val symbols = DecimalFormatSymbols(ptBrLocale).apply {
        currencySymbol = "R$"
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    // Formato com símbolo: "R$ 1.234,56" ou "- R$ 1.234,56"
    private val currencyFormatWithSymbol = DecimalFormat("R$ #,##0.00", symbols)

    // Formato sem símbolo: "1.234,56"
    private val currencyFormatWithoutSymbol = DecimalFormat("#,##0.00", symbols)

    /**
     * Converte um valor monetário em Double (reais) para centavos (Long).
     * Exemplo: 150.50 -> 15050L; 150.556 -> 15056L (arredondamento matemático)
     */
    fun toCents(reais: Double): Long {
        if (reais.isNaN() || reais.isInfinite()) return 0L
        return Math.round(reais * CENTS_PER_REAL)
    }

    /**
     * Converte centavos (Long) para Double (reais).
     * Exemplo: 15050L -> 150.50
     */
    fun centsToDouble(cents: Long): Double {
        return cents.toDouble() / CENTS_PER_REAL.toDouble()
    }

    /**
     * Formata um valor em centavos no padrão da moeda brasileira (R$ 1.234,56).
     * Trata valores negativos de forma explícita (- R$ 45,00).
     */
    fun formatCents(cents: Long, showSymbol: Boolean = true): String {
        val isNegative = cents < 0
        val absCents = Math.abs(cents)
        val reais = absCents.toDouble() / CENTS_PER_REAL.toDouble()
        val formatted = if (showSymbol) {
            currencyFormatWithSymbol.format(reais)
        } else {
            currencyFormatWithoutSymbol.format(reais)
        }
        return if (isNegative) "- $formatted" else formatted
    }

    /**
     * Formata centavos sem o prefixo "R$" (ex: "1.234,56").
     * Ideal para inputs de texto onde o "R$" já está fixo no leadingIcon do TextField.
     */
    fun formatCentsWithoutSymbol(cents: Long): String {
        return formatCents(cents, showSymbol = false)
    }

    /**
     * Formata centavos suprimindo casas decimais quando apropriado (ex: "R$ 1.235").
     * Utilizado em cabeçalhos compactos e metas resumidas.
     */
    fun formatCentsCompact(cents: Long): String {
        val reaisRounded = Math.round(cents.toDouble() / CENTS_PER_REAL.toDouble())
        val df = DecimalFormat("R$ #,##0", symbols)
        return df.format(reaisRounded)
    }

    /**
     * Parsing resiliente de string digitada pelo usuário para centavos (Long).
     * Aceita:
     * - "150,50" -> 15050L
     * - "150.50" -> 15050L
     * - "R$ 1.234,56" -> 123456L
     * - "100" -> 10000L
     * - "  R$  50,2 " -> 5020L
     * - Espaços não-quebráveis (\u00A0 e \u202F) comuns no teclado Android
     * - Valores nulos, vazios ou inválidos retornam o fallback fornecido.
     */
    fun parseToCents(input: String?, fallbackCents: Long = 0L): Long {
        if (input.isNullOrBlank()) return fallbackCents
        val clean = input.trim()
            .replace("R$", "")
            .replace(" ", "")
            .replace("\u00A0", "")
            .replace("\u202F", "")

        if (clean.isBlank()) return fallbackCents

        return try {
            val normalized = if (clean.contains('.') && clean.contains(',')) {
                // Formato padrão pt-BR: 1.234,56 (ponto é milhar, vírgula é decimal)
                if (clean.lastIndexOf(',') > clean.lastIndexOf('.')) {
                    clean.replace(".", "").replace(',', '.')
                } else {
                    // Formato US acidental: 1,234.56
                    clean.replace(",", "")
                }
            } else if (clean.contains(',')) {
                clean.replace(',', '.')
            } else {
                clean
            }

            val doubleVal = normalized.toDoubleOrNull() ?: return fallbackCents
            toCents(doubleVal)
        } catch (_: Exception) {
            fallbackCents
        }
    }

    /**
     * Rateio proporcional seguro: divide centavos por um número inteiro de dias ou entregas,
     * aplicando arredondamento bancário (HALF_UP).
     * Exemplo: R$ 180,00 em 22 dias = 18000L / 22 = 818 centavos (R$ 8,18/dia).
     */
    fun divideByInt(cents: Long, divisor: Int, roundingMode: RoundingMode = RoundingMode.HALF_UP): Long {
        if (divisor <= 0) return 0L
        return BigDecimal.valueOf(cents)
            .divide(BigDecimal.valueOf(divisor.toLong()), 0, roundingMode)
            .toLong()
    }

    /**
     * Multiplicação escalar segura para cálculo de consumo e desgaste fracionário.
     * Exemplo: taxa de 1.25 litros a 589 centavos/litro.
     */
    fun multiplyByDouble(cents: Long, factor: Double, roundingMode: RoundingMode = RoundingMode.HALF_UP): Long {
        if (cents == 0L || factor == 0.0 || factor.isNaN() || factor.isInfinite()) return 0L
        return BigDecimal.valueOf(cents)
            .multiply(BigDecimal.valueOf(factor))
            .setScale(0, roundingMode)
            .toLong()
    }
}
```

---

## 6. Estratégia de Migração do Banco de Dados Room (v14 para v15)

### 6.1 Fundamentação da Migração SQLite
No SQLite, o comando `ALTER TABLE` **não suporta** alteração de tipo de coluna (`ALTER COLUMN`). Além disso, o Room Database Compiler valida rigorosamente o schema local exportado (`15.json`) contra as tabelas do banco de dados na inicialização do aplicativo (`RoomOpenHelper.validateMigration`). Se uma coluna estiver com afinidade `REAL` no SQLite e a entidade esperar `Long` (`INTEGER`), o aplicativo lança uma `IllegalStateException` e aborta a execução na abertura.

Por essa razão, a migração **MIGRATION_14_15** deve seguir o padrão oficial de 12 etapas recomendado pelo SQLite para as **6 tabelas afetadas**:
1. Criar tabela temporária `_new` com os novos tipos `INTEGER`.
2. Copiar os dados existentes da tabela original, convertendo os campos monetários de `REAL` para `INTEGER` com a instrução `CAST(ROUND(coluna * 100) AS INTEGER)`.
3. Dropar a tabela antiga.
4. Renomear a tabela temporária `_new` para o nome oficial.
5. Recriar todos os índices originais.

### 6.2 Queries SQL Exatas de `MIGRATION_14_15`

```kotlin
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // =========================================================================
        // 1. TABELA vehicle (monthlyFixedCosts: REAL -> INTEGER)
        // =========================================================================
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `vehicle_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `model` TEXT NOT NULL,
                `averageConsumption` REAL NOT NULL,
                `fuelType` TEXT NOT NULL,
                `monthlyFixedCosts` INTEGER NOT NULL,
                `plannedWorkDays` INTEGER NOT NULL,
                `currentOdometer` REAL NOT NULL,
                `active` INTEGER NOT NULL,
                `vehicleType` TEXT NOT NULL,
                `brand` TEXT NOT NULL,
                `manufacturingYear` TEXT NOT NULL,
                `tankCapacity` REAL NOT NULL,
                `hasTopBox` INTEGER NOT NULL,
                `hasDeliveryBag` INTEGER NOT NULL,
                `carBodyType` TEXT NOT NULL,
                `remoteId` TEXT NOT NULL,
                `syncStatus` TEXT NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `isDeleted` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `vehicle_new` (
                `id`, `model`, `averageConsumption`, `fuelType`, `monthlyFixedCosts`,
                `plannedWorkDays`, `currentOdometer`, `active`, `vehicleType`, `brand`,
                `manufacturingYear`, `tankCapacity`, `hasTopBox`, `hasDeliveryBag`,
                `carBodyType`, `remoteId`, `syncStatus`, `updatedAt`, `isDeleted`
            )
            SELECT 
                `id`, `model`, `averageConsumption`, `fuelType`, CAST(ROUND(`monthlyFixedCosts` * 100) AS INTEGER),
                `plannedWorkDays`, `currentOdometer`, `active`, `vehicleType`, `brand`,
                `manufacturingYear`, `tankCapacity`, `hasTopBox`, `hasDeliveryBag`,
                `carBodyType`, `remoteId`, `syncStatus`, `updatedAt`, `isDeleted`
            FROM `vehicle`
        """.trimIndent())

        db.execSQL("DROP TABLE `vehicle`")
        db.execSQL("ALTER TABLE `vehicle_new` RENAME TO `vehicle`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_vehicle_active_isDeleted` ON `vehicle` (`active`, `isDeleted`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_vehicle_remoteId` ON `vehicle` (`remoteId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_vehicle_syncStatus` ON `vehicle` (`syncStatus`)")

        // =========================================================================
        // 2. TABELA vehicle_parts (price: REAL -> INTEGER)
        // =========================================================================
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `vehicle_parts_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `vehicleId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `price` INTEGER NOT NULL,
                `lifespanKm` REAL NOT NULL,
                `runKmSinceChange` REAL NOT NULL,
                `remoteId` TEXT NOT NULL,
                `syncStatus` TEXT NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `isDeleted` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `vehicle_parts_new` (
                `id`, `vehicleId`, `name`, `price`, `lifespanKm`,
                `runKmSinceChange`, `remoteId`, `syncStatus`, `updatedAt`, `isDeleted`
            )
            SELECT 
                `id`, `vehicleId`, `name`, CAST(ROUND(`price` * 100) AS INTEGER), `lifespanKm`,
                `runKmSinceChange`, `remoteId`, `syncStatus`, `updatedAt`, `isDeleted`
            FROM `vehicle_parts`
        """.trimIndent())

        db.execSQL("DROP TABLE `vehicle_parts`")
        db.execSQL("ALTER TABLE `vehicle_parts_new` RENAME TO `vehicle_parts`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_vehicle_parts_vehicleId` ON `vehicle_parts` (`vehicleId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_vehicle_parts_remoteId` ON `vehicle_parts` (`remoteId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_vehicle_parts_syncStatus` ON `vehicle_parts` (`syncStatus`)")

        // =========================================================================
        // 3. TABELA daily_records (7 campos monetários: REAL -> INTEGER)
        // =========================================================================
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `daily_records_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `dateString` TEXT NOT NULL,
                `dateTimestamp` INTEGER NOT NULL,
                `platform` TEXT NOT NULL,
                `grossEarnings` INTEGER NOT NULL,
                `deliveriesCount` INTEGER NOT NULL,
                `startOdometer` REAL NOT NULL,
                `endOdometer` REAL NOT NULL,
                `fuelPrice` INTEGER NOT NULL,
                `foodExpense` INTEGER NOT NULL,
                `fuelCost` INTEGER NOT NULL,
                `wearCost` INTEGER NOT NULL,
                `proportionalFixedCost` INTEGER NOT NULL,
                `netProfit` INTEGER NOT NULL,
                `startTime` TEXT NOT NULL,
                `endTime` TEXT NOT NULL,
                `pauseDuration` TEXT NOT NULL,
                `remoteId` TEXT NOT NULL,
                `syncStatus` TEXT NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `isDeleted` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `daily_records_new` (
                `id`, `dateString`, `dateTimestamp`, `platform`, `grossEarnings`,
                `deliveriesCount`, `startOdometer`, `endOdometer`, `fuelPrice`,
                `foodExpense`, `fuelCost`, `wearCost`, `proportionalFixedCost`,
                `netProfit`, `startTime`, `endTime`, `pauseDuration`,
                `remoteId`, `syncStatus`, `updatedAt`, `isDeleted`
            )
            SELECT 
                `id`, `dateString`, `dateTimestamp`, `platform`, CAST(ROUND(`grossEarnings` * 100) AS INTEGER),
                `deliveriesCount`, `startOdometer`, `endOdometer`, CAST(ROUND(`fuelPrice` * 100) AS INTEGER),
                CAST(ROUND(`foodExpense` * 100) AS INTEGER), CAST(ROUND(`fuelCost` * 100) AS INTEGER),
                CAST(ROUND(`wearCost` * 100) AS INTEGER), CAST(ROUND(`proportionalFixedCost` * 100) AS INTEGER),
                CAST(ROUND(`netProfit` * 100) AS INTEGER), `startTime`, `endTime`, `pauseDuration`,
                `remoteId`, `syncStatus`, `updatedAt`, `isDeleted`
            FROM `daily_records`
        """.trimIndent())

        db.execSQL("DROP TABLE `daily_records`")
        db.execSQL("ALTER TABLE `daily_records_new` RENAME TO `daily_records`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_records_isDeleted_dateTimestamp` ON `daily_records` (`isDeleted`, `dateTimestamp`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_records_remoteId` ON `daily_records` (`remoteId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_records_syncStatus` ON `daily_records` (`syncStatus`)")

        // =========================================================================
        // 4. TABELA fuel_refills (3 campos monetários: REAL -> INTEGER)
        // =========================================================================
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `fuel_refills_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `vehicleId` INTEGER NOT NULL,
                `dateTimestamp` INTEGER NOT NULL,
                `dateString` TEXT NOT NULL,
                `gasStation` TEXT NOT NULL,
                `fuelType` TEXT NOT NULL,
                `pricePerLiter` INTEGER NOT NULL,
                `liters` REAL NOT NULL,
                `discount` INTEGER NOT NULL,
                `totalPaid` INTEGER NOT NULL,
                `odometer` REAL NOT NULL,
                `isFullTank` INTEGER NOT NULL,
                `paymentMethod` TEXT NOT NULL,
                `isInstallment` INTEGER NOT NULL,
                `installmentsCount` INTEGER NOT NULL,
                `remoteId` TEXT NOT NULL,
                `syncStatus` TEXT NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `isDeleted` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `fuel_refills_new` (
                `id`, `vehicleId`, `dateTimestamp`, `dateString`, `gasStation`,
                `fuelType`, `pricePerLiter`, `liters`, `discount`, `totalPaid`,
                `odometer`, `isFullTank`, `paymentMethod`, `isInstallment`,
                `installmentsCount`, `remoteId`, `syncStatus`, `updatedAt`, `isDeleted`
            )
            SELECT 
                `id`, `vehicleId`, `dateTimestamp`, `dateString`, `gasStation`,
                `fuelType`, CAST(ROUND(`pricePerLiter` * 100) AS INTEGER), `liters`,
                CAST(ROUND(`discount` * 100) AS INTEGER), CAST(ROUND(`totalPaid` * 100) AS INTEGER),
                `odometer`, `isFullTank`, `paymentMethod`, `isInstallment`,
                `installmentsCount`, `remoteId`, `syncStatus`, `updatedAt`, `isDeleted`
            FROM `fuel_refills`
        """.trimIndent())

        db.execSQL("DROP TABLE `fuel_refills`")
        db.execSQL("ALTER TABLE `fuel_refills_new` RENAME TO `fuel_refills`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_fuel_refills_vehicleId_dateTimestamp` ON `fuel_refills` (`vehicleId`, `dateTimestamp`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_fuel_refills_vehicleId_odometer` ON `fuel_refills` (`vehicleId`, `odometer`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_fuel_refills_remoteId` ON `fuel_refills` (`remoteId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_fuel_refills_syncStatus` ON `fuel_refills` (`syncStatus`)")

        // =========================================================================
        // 5. TABELA maintenance_records (value: REAL -> INTEGER)
        // =========================================================================
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `maintenance_records_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `vehicleId` INTEGER NOT NULL,
                `dateTimestamp` INTEGER NOT NULL,
                `dateString` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `location` TEXT NOT NULL,
                `value` INTEGER NOT NULL,
                `odometer` REAL NOT NULL,
                `remoteId` TEXT NOT NULL,
                `syncStatus` TEXT NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `isDeleted` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `maintenance_records_new` (
                `id`, `vehicleId`, `dateTimestamp`, `dateString`, `description`,
                `location`, `value`, `odometer`, `remoteId`, `syncStatus`,
                `updatedAt`, `isDeleted`
            )
            SELECT 
                `id`, `vehicleId`, `dateTimestamp`, `dateString`, `description`,
                `location`, CAST(ROUND(`value` * 100) AS INTEGER), `odometer`,
                `remoteId`, `syncStatus`, `updatedAt`, `isDeleted`
            FROM `maintenance_records`
        """.trimIndent())

        db.execSQL("DROP TABLE `maintenance_records`")
        db.execSQL("ALTER TABLE `maintenance_records_new` RENAME TO `maintenance_records`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_records_vehicleId_dateTimestamp` ON `maintenance_records` (`vehicleId`, `dateTimestamp`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_records_remoteId` ON `maintenance_records` (`remoteId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_records_syncStatus` ON `maintenance_records` (`syncStatus`)")

        // =========================================================================
        // 6. TABELA user_goals (6 campos monetários: REAL -> INTEGER)
        // =========================================================================
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `user_goals_new` (
                `id` INTEGER NOT NULL,
                `dailyGoalGross` INTEGER NOT NULL,
                `dailyGoalNet` INTEGER NOT NULL,
                `dailyGoalKm` REAL NOT NULL,
                `dailyGoalDeliveries` INTEGER NOT NULL,
                `weeklyGoalGross` INTEGER NOT NULL,
                `weeklyGoalNet` INTEGER NOT NULL,
                `weeklyGoalKm` REAL NOT NULL,
                `weeklyGoalDeliveries` INTEGER NOT NULL,
                `monthlyGoalGross` INTEGER NOT NULL,
                `monthlyGoalNet` INTEGER NOT NULL,
                `monthlyGoalKm` REAL NOT NULL,
                `monthlyGoalDeliveries` INTEGER NOT NULL,
                `remoteId` TEXT NOT NULL DEFAULT '',
                `syncStatus` TEXT NOT NULL DEFAULT 'SYNCED',
                `updatedAt` INTEGER NOT NULL DEFAULT 0,
                `isDeleted` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `user_goals_new` (
                `id`, `dailyGoalGross`, `dailyGoalNet`, `dailyGoalKm`,
                `dailyGoalDeliveries`, `weeklyGoalGross`, `weeklyGoalNet`,
                `weeklyGoalKm`, `weeklyGoalDeliveries`, `monthlyGoalGross`,
                `monthlyGoalNet`, `monthlyGoalKm`, `monthlyGoalDeliveries`,
                `remoteId`, `syncStatus`, `updatedAt`, `isDeleted`
            )
            SELECT 
                `id`, CAST(ROUND(`dailyGoalGross` * 100) AS INTEGER),
                CAST(ROUND(`dailyGoalNet` * 100) AS INTEGER), `dailyGoalKm`,
                `dailyGoalDeliveries`, CAST(ROUND(`weeklyGoalGross` * 100) AS INTEGER),
                CAST(ROUND(`weeklyGoalNet` * 100) AS INTEGER), `weeklyGoalKm`,
                `weeklyGoalDeliveries`, CAST(ROUND(`monthlyGoalGross` * 100) AS INTEGER),
                CAST(ROUND(`monthlyGoalNet` * 100) AS INTEGER), `monthlyGoalKm`,
                `monthlyGoalDeliveries`, `remoteId`, `syncStatus`, `updatedAt`, `isDeleted`
            FROM `user_goals`
        """.trimIndent())

        db.execSQL("DROP TABLE `user_goals`")
        db.execSQL("ALTER TABLE `user_goals_new` RENAME TO `user_goals`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_goals_remoteId` ON `user_goals` (`remoteId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_goals_syncStatus` ON `user_goals` (`syncStatus`)")
    }
}
```

### 6.3 Garantias do Procedimento
- **Integridade de Dados:** O uso de `CAST(ROUND(col * 100) AS INTEGER)` garante que um valor como `180.0` seja convertido exatamente para `18000L`, e que um eventual ruído de ponto flutuante pré-existente (como `179.99999999999997`) seja arredondado para `18000L` em vez de truncado para `17999L`.
- **Preservação de Chaves e Relacionamentos:** O campo `id` e todos os IDs e timestamps são copiados sem alteração.
- **Validação com `MigrationTestHelper`:** No pipeline de testes, a migração será testada contra o schema `14.json` exportado, garantindo que 100% dos dados pré-existentes de v14 migrem sem perdas para v15.

---

## 7. Impacto em Relatórios e Performance

### 7.1 Somas e Agregações Atômicas
No SQLite e na JVM, a soma de `Long` é processada em um único ciclo de clock de instrução inteira (`ADD` em registrador x86_64 ou ARM64), dispensando a unidade de ponto flutuante (FPU).
- Em consultas Room como `SELECT SUM(grossEarnings) FROM daily_records`, o retorno é um inteiro puro.
- Nas listas de registros em memória, `filtered.sumOf { it.grossEarnings }` evita qualquer alocação intermediária e garante que a soma de 100 corridas seja exata ao centavo.

### 7.2 Ganho por Hora (`grossPerHour` e `netPerHour`)
Atualmente, o ganho por hora é calculado dividindo reais por horas decimais fracionadas:
```kotlin
val grossPerHour = if (totalWorkHours > 0) totalEarnings / totalWorkHours else 0.0
```
Com a migração, o cálculo se mantém matematicamente limpo:
```kotlin
val grossPerHourCents: Long = if (totalWorkHours > 0.0) {
    Math.round(totalEarningsCents.toDouble() / totalWorkHours)
} else {
    0L
}
```
Exemplo: Se o entregador faturou R$ 250,50 (`25050L`) em 8,5 horas:
- `25050 / 8.5` = `2947.0588...` -> `2947L` (R$ 29,47/hora).
- A exibição formata como `"R$ 29,47 /h"`, sem desvios.

### 7.3 Médias Diárias e Projeções
- Média diária de faturamento: `totalEarningsCents / count` com `RoundingMode.HALF_UP`.
- Projeção mensal: `avgProfitPerDayCents * workingDays`.

---

## 8. Plano de Execução Detalhado para a Tarefa T-18B

A implementação prática da migração ocorrerá na Tarefa subsequente **T-18B [DAT-01B]**, estruturada em 7 fases atômicas e sequenciais:

### 8.1 Fases Sequenciais

1. **Fase 1: Utilitário `CurrencyUtils.kt` e Testes de Unidade Isolados**
   - Criar `app/src/main/java/com/example/util/CurrencyUtils.kt`.
   - Criar suíte de testes dedicada `app/src/test/java/com/example/util/CurrencyUtilsTest.kt` cobrindo 100% das funções, casos felizes, números negativos, zeros, strings vazias, formatação pt-BR e parsing com múltiplos separadores.
2. **Fase 2: Schema Room v15 e `MIGRATION_14_15`**
   - Alterar `version = 15` em [GiroCustoDatabase.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/data/GiroCustoDatabase.kt#L13).
   - Inserir a implementação de `MIGRATION_14_15`.
   - Alterar os tipos dos 19 campos em [Entities.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/data/Entities.kt) de `Double` para `Long`.
   - Executar `./gradlew kspDebugKotlin` para compilar e gerar o arquivo de schema `app/schemas/com.example.data.GiroCustoDatabase/15.json`.
   - Criar teste com `MigrationTestHelper` em `app/src/test/java/com/example/RoomMigration14To15Test.kt` validando migração de banco v14 com dados reais para v15.
3. **Fase 3: Repositório e DAOs**
   - Ajustar assinaturas de métodos em [Daos.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/data/Daos.kt) e [Repository.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/data/Repository.kt) (`insertDailyRecord`, `updateDailyRecord`, `saveFuelRefill`, etc.) para receberem valores em `Long` (centavos).
   - Ajustar o método `seedJune2026Data` para semear centavos inteiros.
4. **Fase 4: Adaptadores Supabase (Estratégia A)**
   - Ajustar [SupabaseMappers.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/network/supabase/SupabaseMappers.kt) para converter `Long` (centavos) <-> `Double` (reais) nos métodos `toDto` e `toEntity` de todas as entidades.
   - Garantir que nenhum teste de contrato do Supabase seja quebrado.
5. **Fase 5: ViewModel e Modelos de Apresentação**
   - Ajustar [GiroCustoViewModel.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/GiroCustoViewModel.kt), `ReportMetricsState`, `EstimationDetail` e métodos de cálculo para operarem com `Long`.
6. **Fase 6: Telas e Componentes Compose**
   - Substituição gradual e cirúrgica de todos os `Locale.GERMAN` e formatações manuais por `CurrencyUtils.formatCents`.
   - Ajuste dos formulários de entrada de dados para usar `CurrencyUtils.parseToCents`.
   - Correção dos bugs de parsing silencioso em `ProfileScreen.kt` e `HistoryScreen.kt`.
7. **Fase 7: Portabilidade LGPD e Validação Global da Suíte de Testes**
   - Atualizar [UserDataExportHelper.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/util/UserDataExportHelper.kt) para exportar em formato legível ao usuário.
   - Atualizar asserções dos testes unitários existentes que esperavam `Double` para `Long` em centavos.
   - Executar `./gradlew testDebugUnitTest lintDebug` e assegurar que todos os 336+ testes passem com 100% de sucesso.

### 8.2 Roteiro de Testes com Mutação Manual (Obrigatório em T-18B)
Conforme exigido em `integridade-de-testes.md`, o QA aplicará mutação manual deliberada no código novo:
1. Inverter o fator de conversão em `CurrencyUtils`: trocar `* 100L` por `* 10L`.
   - *Resultado esperado:* Falha imediata de dezenas de testes de conversão e cálculo.
2. Alterar o arredondamento de `MIGRATION_14_15` para truncar em vez de arredondar (`CAST((coluna * 100) AS INTEGER)` sem `ROUND`).
   - *Resultado esperado:* Falha no teste de migração com amostras decimais como `19.999`.
3. Reverter as mutações e confirmar 100% verde.

### 8.3 Roteiro de Homologação Manual no Aparelho Físico (Risco Alto)
Após a conclusão da T-18B, o seguinte roteiro será executado e fotografado no aparelho:
1. **Lançamento de Diária:** Lançar faturamento de `R$ 150,50`, alimentação `R$ 25,00` e combustível a `R$ 5,89`. Verificar que os valores são salvos e exibidos com vírgula e alinhados.
2. **Abastecimento:** Inserir 10 litros a `R$ 5,50` com desconto de `R$ 2,00`. Confirmar total calculado de `R$ 53,00` sem delay ou loop.
3. **Histórico:** Conferir os agrupadores mensais e anuais; somar manualmente os itens na calculadora e confirmar que o cabeçalho bate exatamente ao centavo.
4. **Perfil e Peças:** Alterar os custos fixos para `R$ 215,50` utilizando teclado numérico com vírgula; fechar o app, reabrir e confirmar que o valor foi persistido como R$ 215,50 (e não R$ 180,00).
5. **Sincronização Cloud:** Acionar a sincronização com o Supabase e conferir se os registros aparecem no banco remoto com os valores decimais corretos em reais.

---

## 9. PARADA OBRIGATÓRIA

> [!IMPORTANT]
> **DECLARAÇÃO DE PARADA OBRIGATÓRIA (GATE DE APROVAÇÃO):**  
> Em estrito cumprimento ao protocolo de orquestração (`equipe-agentes.md`, item 1.1 e 1.5) e às diretrizes da tarefa T-18A, **NENHUMA LINHA DE CÓDIGO DE PRODUÇÃO, TESTES OU SCRIPT DE BUILD FOI ALTERADA**.  
> Qualquer implementação prática da migração monetária para centavos (`Long`), criação de utilitários, alteração de schemas ou refatoração de telas está **FORMALMENTE BLOQUEADA** até que o usuário/dono do projeto revise e aprove formalmente este plano técnico.
