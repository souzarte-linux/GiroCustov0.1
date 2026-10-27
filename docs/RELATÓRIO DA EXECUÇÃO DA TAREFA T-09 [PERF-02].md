# Relatório de Execução da Tarefa T-09 [PERF-02]
## Adição de Chaves Estáveis (`key`) em Todas as Listas `LazyColumn` do Jetpack Compose

---

### 1. Resultado

🟡 **IMPLEMENTADO, NÃO VERIFICADO NO APARELHO**  
Todas as 5 ocorrências de `items()` em listas roláveis `LazyColumn` do Compose foram auditadas e atualizadas com o parâmetro `key` único e determinístico, tanto para listas planas baseadas em entidades Room (`it.id`) quanto para a lista hierárquica agrupada do histórico de corridas (`getHistoryListItemKey`). Foram adicionados 7 novos testes unitários dedicados à unicidade e determinismo das chaves, a suíte completa passou com 258 testes (0 falhas, 0 ignorados), o lint e o build de release com R8 (`assembleReleaseCheck`) foram concluídos com sucesso.  
O status é 🟡 pois a confirmação visual da eliminação de *jank* (engasgos) e cintilações na rolagem rápida a 60/120 fps requer teste prático no celular físico com volume de dados reais.

---

### 2. O que mudou

Para o usuário entregador/motorista de aplicativo, a rolagem de listas longas (como o histórico de corridas, abastecimentos e manutenções) torna-se significativamente mais suave, rápida e com menor consumo de bateria e aquecimento do aparelho sob sol forte. Antes desta alteração, sempre que um item era adicionado, editado ou o filtro mudava, o Jetpack Compose não sabia exatamente qual item havia mudado e era forçado a redesenhar a lista inteira do zero (recomposição custosa). Com as chaves estáveis (`key`), o Compose rastreia cada item de forma cirúrgica e reaproveita elementos já renderizados.

**Arquivos de produção modificados:**
- [`FuelRefillsScreen.kt`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/screens/FuelRefillsScreen.kt): adição de `key = { it.id }` na `LazyColumn` de abastecimentos de combustível (linha 300).
- [`MaintenanceScreen.kt`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/screens/MaintenanceScreen.kt): adição de `key = { it.id }` na `LazyColumn` de manutenções e trocas de peças (linha 255).
- [`PlatformsScreen.kt`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/screens/PlatformsScreen.kt): adição de `key = { it.id }` na `LazyColumn` de plataformas cadastradas (linha 196).
- [`HistoryScreen.kt`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/screens/HistoryScreen.kt):
  - No modo hierárquico agrupado: adição de `key = { getHistoryListItemKey(it) }` (linha 393).
  - No modo lista simples/plano: adição de `key = { it.id }` (linha 440).
  - Implementação da função `getHistoryListItemKey(item: HistoryListItem): String` que mapeia de forma determinística e livre de colisões cada tipo de item da lista:
    - `YearHeader` -> `"year_${item.year}"`
    - `MonthHeader` -> `"month_${item.year}_${item.month}"`
    - `WeekHeader` -> `"week_${item.weekStartMillis}_${item.weekEndMillis}"`
    - `DayHeader` -> `"day_${item.dateTimestamp}_${item.label}"`
    - `RecordRow` -> `"record_${item.record.id}"`

**Arquivos de teste criados:**
- [`LazyListKeysAndStabilityTest.kt`](file:///home/nando/Documentos/GiroCustov0.1/app/src/test/java/com/example/LazyListKeysAndStabilityTest.kt): nova suíte com 7 testes unitários cobrindo estabilidade, unicidade, isolamento temporal e ausência de colisões.

---

### 3. Evidências

#### 3.1 Testes Unitários Completos (`./gradlew testDebugUnitTest`)
- **Comando:** `./gradlew testDebugUnitTest`
- **Contagem real:** Executados: **258**, Aprovados: **258**, Falhas: **0**, Ignorados: **0**
- **Relatório HTML:** `app/build/reports/tests/testDebugUnitTest/index.html`
- **Trecho final da saída:**
```text
BUILD SUCCESSFUL in 52s
42 actionable tasks: 13 executed, 29 up-to-date
```

#### 3.2 Análise Estática de Código (`./gradlew lintDebug`)
- **Comando:** `./gradlew lintDebug`
- **Resultado:** Aprovado sem erros impeditivos.
- **Trecho final da saída:**
```text
BUILD SUCCESSFUL in 3m 19s
40 actionable tasks: 1 executed, 39 up-to-date
```

#### 3.3 Verificação de Minificação e Compilação de Release R8 (`./gradlew assembleReleaseCheck`)
- **Comando:** `./gradlew assembleReleaseCheck`
- **Resultado:** Compilação do APK de release minificado com ProGuard/R8 aprovada com sucesso.
- **Trecho final da saída:**
```text
BUILD SUCCESSFUL in 8m 54s
42 actionable tasks: 16 executed, 26 up-to-date
```

#### 3.4 Registro da Mutação Manual Obrigatória (Regra 4 de Integridade)
- **Local:** `app/src/main/java/com/example/ui/screens/HistoryScreen.kt:789`
- **Mutação aplicada:** Quebra deliberada da lógica geradora de chave para linhas de registro (`RecordRow`), forçando colisão com uma string estática:
  ```kotlin
  // Mutação deliberada:
  is HistoryListItem.RecordRow -> "record_duplicate_collision"
  ```
- **Execução:** `./gradlew testDebugUnitTest --tests "com.example.LazyListKeysAndStabilityTest"`
- **Resultado da mutação:** 2 testes falharam imediatamente detectando a colisão:
  - `testHistoryHierarchicalKeys_acrossMonthsAndYearsAreCompletelyUnique` -> `java.lang.AssertionError: Chave duplicada encontrada no agrupamento: record_duplicate_collision (esperado tamanho 13, obtido 12)`
  - `testHistoryHierarchicalKeys_areUniqueWithMultipleRecordsPerDay` -> `java.lang.AssertionError: Chave duplicada encontrada em múltiplos registros: record_duplicate_collision (esperado tamanho 3, obtido 2)`
- **Restauração e Prova Verde:** O código foi revertido para a expressão original `"record_${item.record.id}"`. A reexecução comprovou que todos os 7 testes voltaram a passar com 100% de sucesso.

#### 3.5 Varreduras Obrigatórias Pré-Commit
- **Verificação 1 (Stubs/Fakes em produção):**
  `grep -rniE "\b(mock|stub|fake|homologa|TODO|FIXME|em breve)\b" app/src/main/java`
  *Resultado:* Nenhum stub, mock ou pendência nova introduzida (apenas TODO de drawer e mockup do smartphone simulator em preview previamente catalogados).
- **Verificação 2 (Testes desativados ou vazios):**
  `grep -rnE "@Ignore|assertTrue\(true\)|assertEquals\(([0-9]+), *\1\)" app/src/test app/src/androidTest`
  *Resultado:* Vazio (código 1). Nenhum teste desativado ou asserção trivial.
- **Verificação 3 (Segredos no código):**
  `grep -rniE "(api[_-]?key|secret|password|service_role|BEGIN (RSA|PRIVATE))" ... | grep -v ".env.example"`
  *Resultado:* Apenas referências esperadas a campos de formulário, redaction de logs do OkHttp e chaves de keystore Android. Nenhum segredo ou chave privada vazada.

---

### 4. Testes

- **Testes adicionados (7 testes novos em `app/src/test/java/com/example/LazyListKeysAndStabilityTest.kt`):**
  1. `testFuelRefillsKeys_areUniqueAndDeterministic`: valida unicidade e determinismo de chaves para abastecimentos.
  2. `testMaintenanceKeys_areUniqueAndDeterministic`: valida unicidade e determinismo de chaves para registros de manutenção.
  3. `testPlatformKeys_areUniqueAndDeterministic`: valida unicidade e determinismo de chaves para plataformas de trabalho.
  4. `testHistoryFlatKeys_areUniqueAndDeterministic`: valida unicidade e determinismo de chaves no histórico modo simples.
  5. `testHistoryHierarchicalKeys_allTypesProduceDistinctPrefixes`: valida que cada tipo de nó hierárquico (ano, mês, semana, dia, registro) gera prefixos únicos.
  6. `testHistoryHierarchicalKeys_acrossMonthsAndYearsAreCompletelyUnique`: valida ausência de colisões entre semanas, meses e anos distintos com registros reais.
  7. `testHistoryHierarchicalKeys_areUniqueWithMultipleRecordsPerDay`: valida que múltiplos registros de corridas em um mesmo dia possuem chaves estritamente únicas.
- **Testes alterados:** Nenhum teste existente foi alterado.
- **Testes removidos:** Nenhum teste foi removido.

---

### 5. O que NÃO foi feito ou está simulado

- **Nada está simulado ou pendente em código:** Todas as listas do app utilizam chaves reais derivadas de chaves primárias do Room ou de marcadores temporais determinísticos.
- **Validação no aparelho físico:** Permanece como 🟡 até que o usuário execute a rolagem em seu dispositivo móvel para atestar a fluidez a 60/120 fps com tela sob sol e múltiplas recomposições.

---

### 6. Riscos e dívidas encontrados

- A tela `HistoryScreen.kt` possui atualmente 1.488 linhas e gerencia tanto a lógica de apresentação quanto os cálculos de agrupamento hierárquico. A função `getHistoryListItemKey` foi introduzida no escopo mínimo seguro para a T-09, mas a decomposição desta tela e de seu agrupamento deve ser contemplada na Tarefa T-20 (refatoração arquitetural de telas grandes da Onda 3).
- Não foi introduzida nenhuma dependência nova.

---

### 7. O que o usuário precisa fazer

Para verificar e homologar no aparelho físico que a rolagem das listas está suave e sem travamentos (*jank*), siga o roteiro abaixo:

#### Roteiro de Teste de Performance de Rolagem (60/120 fps)

1. **Compilar e instalar a versão atualizada no celular:**
   - Conecte o aparelho físico via cabo USB com depuração ativada e execute no terminal:
     ```bash
     ./gradlew installDebug
     ```
   - Ou abra o app GiroCusto já instalado caso utilize a compilação local.

2. **Teste 1: Rolagem na aba "Abastecimentos" (FuelRefillsScreen):**
   - No menu inferior, toque em **Abastecimentos**.
   - Se já houver registros cadastrados, faça rolagens rápidas com o polegar de cima para baixo e de baixo para cima repetidas vezes.
   - **Resultado esperado:** A lista deve rolar sem "soluços", engasgos ou cards piscando/cintilando.
   - Toque no botão `+` e adicione um novo abastecimento. Ao salvar, a lista deve se atualizar suavemente posicionando o novo item sem recompor a lista toda.

3. **Teste 2: Rolagem na aba "Histórico" (HistoryScreen):**
   - No menu inferior, toque em **Histórico**.
   - No seletor de visualização no topo, confirme que o modo "Agrupado" (padrão) está selecionado.
   - Role a lista rapidamente para visualizar os cabeçalhos de Ano, Mês, Semana e Dia, além dos cards de corridas.
   - **Resultado esperado:** A rolagem deve responder imediatamente ao toque, sem atrasos visíveis (*touch latency*) e sem sumiço temporário de textos nos cabeçalhos.
   - Alterne para o modo de lista simples (se disponível) e repita a rolagem rápida.

4. **Teste 3: Rolagem na aba "Manutenção" (MaintenanceScreen):**
   - Acesse o menu de **Manutenção**.
   - Role a lista de manutenções e peças cadastradas.
   - **Resultado esperado:** Movimentação estável, sem cintilação nos cartões de status de quilometragem.

5. **Teste 4: Rolagem na tela "Plataformas" (PlatformsScreen):**
   - Acesse as configurações ou menu de **Plataformas**.
   - Role a lista de aplicativos cadastrados (Uber, 99, iFood, etc.).
   - **Resultado esperado:** Cards com logos e taxas rolam fluidamente.

---

### 8. Commit e sincronização

- **Hash do Commit:** `94663f4`
- **Status do Push:** Pronto para envio para `origin/main`.
