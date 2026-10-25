# Relatório Final de Execução: Tarefa T-05 [UX-01]

## 1. Resultado
✅ **VERIFICADO**: Todas as ações previstas para o usuário foram executadas e validadas com sucesso no aparelho físico real pelo proprietário do projeto (diálogos de confirmação prévia via `AlertDialog`, cancelamento, exclusão lógica de veículos e peças de desgaste, recuperação imediata via ação "Desfazer" no `Snackbar` com proteção Last-Write-Wins e persistência correta dos dados). A suíte completa de testes unitários (160/160 aprovados) e Android Lint passaram com 100% de sucesso, a sensibilidade do teste foi comprovada por mutação manual e o commit atômico encontra-se devidamente sincronizado no repositório remoto.

---

## 2. O que mudou

1. **Diálogos de Confirmação Prévia (`AlertDialog`):**
   - Na tela de perfil ([`ProfileScreen.kt`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/screens/ProfileScreen.kt)), toques nos botões de exclusão de veículos (na lista e no formulário de edição) ou de peças de desgaste não realizam mais a exclusão imediata.
   - O aplicativo agora abre uma janela de confirmação explícita (`AlertDialog`) exibindo o modelo do veículo ou o nome da peça a ser excluída, com botões claros de "Cancelar" e "Excluir" (com destaque em cor destrutiva/vermelha `ColorRose`).

2. **Ação "Desfazer" (Undo) via Snackbar:**
   - Ao confirmar a exclusão, o item é marcado como excluído logicamente (`isDeleted = true`) e removido visualmente da lista.
   - Imediatamente surge na parte inferior da tela um `Snackbar` informativo (`Veículo "[nome]" excluído.` ou `Peça "[nome]" excluída.`) contendo o botão de ação **"Desfazer"**.
   - Se o motorista tocar em "Desfazer", o aplicativo restaura imediatamente o registro e apresenta um aviso de sucesso via `Toast` (`Veículo restaurado com sucesso!` ou `Peça restaurada com sucesso!`).

3. **Proteção LWW (Last-Write-Wins) na Restauração:**
   - No [`Repository.kt`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/data/Repository.kt) e [`GiroCustoViewModel.kt`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/GiroCustoViewModel.kt), foram adicionadas as funções `restoreVehicle` e `restorePart`.
   - Ao restaurar, o registro recebe `isDeleted = false`, `syncStatus = SyncStatus.PENDING_UPDATE` e compulsoriamente `updatedAt = System.currentTimeMillis()`. O carimbo de tempo mais recente garante que a sincronização posterior com a nuvem (Supabase) não reverta indevidamente a recuperação feita pelo usuário.

4. **Acessibilidade e Ergonomia dos Botões (Área Mínima de 48dp):**
   - Os botões de exclusão e ações adjacentes (edição e zerar desgaste) em `MyVehiclesSection`, `PartsWearSection` e `VehicleFormSection` tiveram suas áreas de toque aumentadas para o padrão mínimo acessível de `48.dp` (com ícones internos de `20.dp`), prevenindo toques errôneos durante o uso com uma mão na rua.
   - Foi inserido `contentDescription` acessível em português para TalkBack em todos os botões.

5. **Centralização de Textos em strings.xml:**
   - Todas as mensagens de diálogo, títulos, botões, snackbars e toasts foram adicionados a [`strings.xml`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/res/values/strings.xml) sem nenhum texto fixo (hardcoded) nas telas.

### Arquivos alterados:
- [`app/src/main/res/values/strings.xml`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/res/values/strings.xml): recursos de texto para diálogos de confirmação, snackbars e toasts.
- [`app/src/main/java/com/example/data/Repository.kt`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/data/Repository.kt): métodos `restoreVehicle` e `restorePart` com atualização de timestamp LWW.
- [`app/src/main/java/com/example/ui/GiroCustoViewModel.kt`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/GiroCustoViewModel.kt): métodos `restoreVehicle` e `restorePart` integrados ao `viewModelScope`.
- [`app/src/main/java/com/example/ui/screens/ProfileScreen.kt`](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/screens/ProfileScreen.kt): `Box` com `SnackbarHost`, diálogos `AlertDialog`, botões com área de toque mínima de 48dp e fluxos de confirmação/desfazer.
- [`app/src/test/java/com/example/SupabaseSyncAndMigrationTest.kt`](file:///home/nando/Documentos/GiroCustov0.1/app/src/test/java/com/example/SupabaseSyncAndMigrationTest.kt): teste automatizado de ponta a ponta cobrindo excluir -> sincronizar -> desfazer -> sincronizar com proteção LWW.

---

## 3. Evidências

### Comandos exatos executados
```bash
# 1. Execução da suíte completa de testes unitários
JAVA_HOME=/home/nando/.jdk17 PATH=/home/nando/.jdk17/bin:$PATH ./gradlew testDebugUnitTest

# 2. Execução do Android Lint
JAVA_HOME=/home/nando/.jdk17 PATH=/home/nando/.jdk17/bin:$PATH ./gradlew lintDebug

# 3. Verificações pré-commit de integridade
grep -rniE "mock|stub|fake|homologa|TODO|FIXME|em breve" app/src/main/java
grep -rnE "@Ignore|assertTrue\(true\)|assertEquals\(([0-9]+), *\1\)" app/src/test app/src/androidTest
grep -rniE "(api[_-]?key|secret|password|service_role|BEGIN (RSA|PRIVATE))" --include=*.kt --include=*.kts --include=*.xml --include=*.json --include=*.properties . | grep -v ".env.example"
```

### Contagem real de testes
- **Executados:** 160
- **Aprovados:** 160
- **Falhas:** 0
- **Erros:** 0
- **Ignorados (skipped):** 0

### Caminho dos relatórios de testes
- Testes unitários: [`app/build/reports/tests/testDebugUnitTest/index.html`](file:///home/nando/Documentos/GiroCustov0.1/app/build/reports/tests/testDebugUnitTest/index.html)
- Relatório Lint: [`app/build/reports/lint-results-debug.html`](file:///home/nando/Documentos/GiroCustov0.1/app/build/reports/lint-results-debug.html)

### Últimas linhas da saída do Gradle (Testes e Lint)
```text
> Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
> Task :app:processDebugUnitTestJavaRes
> Task :app:testDebugUnitTest

> Task :app:lintReportDebug
Wrote HTML report to file:///home/nando/Documentos/GiroCustov0.1/app/build/reports/lint-results-debug.html

> Task :app:lintDebug
> Task :app:finalizeTestRoborazziDebug SKIPPED

BUILD SUCCESSFUL in 3m 55s
42 actionable tasks: 25 executed, 17 up-to-date
Configuration cache entry reused.
```

### Verificação por Mutação Manual (Seção 4 de integridade-de-testes.md)
- **Mutação introduzida:** Em `Repository.kt`, foi alterada a linha da restauração para manter `isDeleted = true`.
- **Resultado:** O teste automatizado falhou imediatamente (`AssertionError` em `assertFalse("isDeleted deve ser false após restore")`).
- **Reversão:** A mutação foi desfeita, restaurando `isDeleted = false` e a suíte voltou a passar integralmente com 160 testes verdes.

---

## 4. Testes
- **Adicionado:**
  - `SupabaseSyncAndMigrationTest.testVehicleAndPart_delete_sync_undo_sync_LWWProtection`: valida o ciclo completo de exclusão lógica, envio do DTO com `deletedAt` preenchido ao Supabase, acionamento do Desfazer local com avanço do timestamp `updatedAt`, re-sincronização com limpeza de `deletedAt` na nuvem e proteção Last-Write-Wins evitando que o pull remoto re-deleta o item.
- **Alterados:** Nenhum teste existente foi alterado ou enfraquecido.
- **Removidos:** Nenhum teste foi removido.

---

## 5. O que NÃO foi feito ou está simulado
- Nada está simulado ou em stub. Toda a lógica de soft-delete, diálogo, snackbar e restauração LWW é 100% funcional.
- Modificações no schema do Room e migrações estruturais do banco de dados estavam fora do escopo desta tarefa (previstas para a Tarefa T-08).

---

## 6. Riscos e dívidas encontrados
- **Solicitação de Ajuste de UX na Tela de Perfil (Botão 'Consultar Correios'):**
  - Durante os testes manuais no aparelho físico, o usuário identificou que o botão **"Consultar Correios"** (`profile_search_cep_web_btn`), localizado abaixo do campo de CEP na seção de endereço, está desproporcionalmente grande e na orientação errada (ocupando a largura total verticalmente em uma coluna empilhada, em vez de se apresentar como um link ou botão de ação compacto alinhado horizontalmente ao lado do campo de CEP).
  - Esta observação foi formalmente catalogada como dívida de interface/UX para correção nas tarefas de refatoração dedicadas da tela de perfil (**T-20 [ARQ-02]** ou **T-25 [ACC-02B]**).
- **Tamanho do arquivo ProfileScreen.kt:** `ProfileScreen.kt` possui mais de 2.000 linhas e concentra lógica de formulários e seções múltiplas. Esse acoplamento histórico será sanado na Tarefa dedicada de refatoração **T-20 [ARQ-02]**, que dividirá o arquivo em componentes menores (<800 linhas) sem alteração de comportamento.
- **Textos remanescentes:** A tela de perfil ainda possui textos literais não relacionados à exclusão que serão migrados para `strings.xml` na Tarefa dedicada **T-25 [ACC-02B]**.

---

## 7. Registro de Execução Manual no Aparelho Real, Erros e Soluções

O proprietário do projeto realizou no dispositivo físico Android a validação de todas as etapas do roteiro de testes, obtendo os seguintes resultados:

### 1. Teste do Diálogo de Confirmação e Cancelamento (Veículos)
- **Ação executada:** O usuário acessou **"Meu Perfil"**, rolou até a seção **"MEUS VEÍCULOS"** e tocou no ícone de lixeira (vermelho) de um veículo secundário.
- **Comportamento observado:** O veículo **não** foi apagado diretamente. A caixa de diálogo `AlertDialog` com o título *"Excluir veículo?"* e o texto contextualizado foi exibida. Ao tocar em "Cancelar", o diálogo fechou e o veículo permaneceu intacto na lista.
- **Resultado:** Aprovado com sucesso.

### 2. Teste de Exclusão e Ação "Desfazer" via Snackbar (Veículos)
- **Ação executada:** O usuário confirmou a exclusão tocando em "Excluir". Em seguida, tocou no botão de ação **"DESFAZER"** no Snackbar exibido na parte inferior da tela.
- **Comportamento observado:** O veículo desapareceu de imediato da lista, o Snackbar surgiu com o botão "Desfazer" e, ao ser acionado, o veículo reapareceu instantaneamente na lista com todos os dados íntegros, acompanhado do Toast *"Veículo restaurado com sucesso!"*.
- **Resultado:** Aprovado com sucesso.

### 3. Teste de Exclusão e "Desfazer" para Peças de Desgaste
- **Ação executada:** Na seção **"MONITORAMENTO DE DESGASTE"**, o usuário testou a exclusão e o "Desfazer" em uma peça cadastrada.
- **Comportamento observado:** O diálogo *"Excluir peça?"* solicitou a confirmação prévia, e a ação "Desfazer" no Snackbar restaurou com exatidão a peça com seus quilômetros e valores preservados.
- **Resultado:** Aprovado com sucesso.

### 4. Teste de Exclusão no Formulário de Edição
- **Ação executada:** O usuário abriu o formulário de edição de veículo e acionou a lixeira no canto superior direito.
- **Comportamento observado:** O diálogo de confirmação foi exibido, a exclusão fechou o formulário retornando à tela de perfil e a opção "Desfazer" funcionou perfeitamente.
- **Resultado:** Aprovado com sucesso.

### 5. Teste de Ergonomia e Área de Toque (48dp)
- **Ação executada:** O usuário manipulou os botões de ação e lixeira com uma única mão.
- **Comportamento observado:** A ampliação para o target mínimo de 48dp facilitou o toque preciso sem cliques falsos ou acidentais.
- **Resultado:** Aprovado com sucesso.

### 6. Solicitação de Ajuste Registrada pelo Usuário
- **Apontamento:** O botão **"Consultar Correios"** na seção de endereço está muito grande e em orientação visual inadequada (vertical/full-width).
- **Encaminhamento:** Registrado na Seção 6 para ajuste de layout na próxima passagem de refatoração da tela de perfil (**T-20** / **T-25**).

---

## 8. Commit e sincronização
- **Hash do commit:** `b2714f0817b2cfa9c362abd142ce413b3dea518f`
- **Hash curto:** `b2714f0`
- **Mensagem:** `UX: adiciona diálogo de confirmação e desfazer com proteção LWW ao excluir veículos e peças (T-05)`
- **Status do push:** Sincronizado com sucesso com o repositório remoto (`main -> origin/main`).
