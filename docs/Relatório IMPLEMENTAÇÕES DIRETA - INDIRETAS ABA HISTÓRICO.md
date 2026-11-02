# Relatório: IMPLEMENTAÇÕES DIRETAS E INDIRETAS NA ABA HISTÓRICO

## 1. Resultado

🟡 **IMPLEMENTADO, NÃO VERIFICADO NO APARELHO**: O código de produção foi totalmente implementado, a documentação de design foi sincronizada, a suíte completa de 441 testes unitários automatizados foi executada e aprovada com 100% de sucesso (0 falhas e 0 ignorados), a verificação estática do Android Lint foi aprovada sem erros e o pacote APK de debug foi gerado com sucesso. A validação visual e tátil final em dispositivo físico depende de conexão com o aparelho.

---

## 2. O que mudou

### Explicação leiga
1. **Expansão do Card e Botões de Ação (Editar e Excluir)**: Ao tocar sobre o card de qualquer plataforma na linha do tempo do Histórico, o card se expande exibindo detalhamentos operacionais da rota (horário inicial, horário final, tempo de pausa, km rodados e discriminativo de custos: combustível, desgaste de peças, custos fixos proporcionais e lucro líquido) e disponibiliza dois botões de ação acessíveis na parte inferior: **Editar** (com ícone de lápis) e **Excluir** (com ícone de lixeira e estilo visual de perigo), ambos respeitando área de toque mínima de 48dp para uso rápido na rua.
2. **Modal de Edição com ComboBox Pesquisável de Plataformas**: Ao tocar no botão de editar, o campo de Plataforma agora é uma ComboBox completa. Clicar no botão com a seta para baixo abre uma lista suspensa com todas as plataformas cadastradas e opções padrão do app, permitindo rolar e selecionar com um toque. Caso o usuário prefira, pode simplesmente digitar parte do nome no campo de texto para filtrar instantaneamente as plataformas correspondentes.
3. **Edição de Data do Registro**: No mesmo modal de edição, foi introduzido o campo **Data**, com formato amigável brasileiro (`dd/MM/yyyy`). Ao tocar no campo ou no ícone de calendário, abre-se o seletor nativo de data (`DatePickerDialog`). Com isso, lançamentos inseridos por engano em uma data errada podem ser corrigidos e movidos automaticamente para o dia correto no histórico.
4. **Dupla Trava de Segurança na Exclusão de Registros**:
   - **Trava 1 (Confirmação Prévia)**: Ao tocar no botão da lixeira, o app não apaga o registro imediatamente. É exibido um diálogo de alerta (`AlertDialog`) perguntando se o motorista realmente deseja excluir o lançamento daquela plataforma, informando que a ação poderá ser desfeita nos próximos segundos.
   - **Trava 2 (Opção Desfazer)**: Ao confirmar a exclusão, o registro é removido da lista e uma notificação flutuante temporária (Snackbar) surge na parte inferior da tela com o texto `"Lançamento de [Plataforma] excluído."` e a ação **"DESFAZER"**. Caso o usuário toque em "DESFAZER", o registro é imediatamente restaurado no banco de dados local com todos os seus dados e o desgaste de peças de reposição do veículo é reincorporado atomicamente.
5. **Card de Resumo Mensal em Grade 2x2**: A disposição dos subcards de métricas do cabeçalho do mês (`MonthHeaderCard`) foi reorganizada em uma grade simétrica 2x2:
   - **Linha Superior (Destaque Principal)**: Os dois subcards maiores, **Bruto** e **Líquido**, com altura mínima de 52dp, tipografia destacada e cores semânticas.
   - **Linha Inferior (Métricas Operacionais)**: Os dois subcards menores, **Pacotes** e **KM**, alinhados lado a lado sob os blocos financeiros.

### Lista de arquivos alterados
- `app/src/main/java/com/example/data/Repository.kt`: Implementação da função `restoreRecord(record: DailyRecord)` em transação Room (`@Transaction`), restaurando o registro (`isDeleted = false`), reincorporando o desgaste em `VehiclePart.runKmSinceChange` e recalculando o hodômetro do veículo ativo.
- `app/src/main/java/com/example/ui/delegates/DailyLaunchCoordinator.kt`: Exposição do método `restoreDailyRecord` no coordenador de lançamentos diários.
- `app/src/main/java/com/example/ui/GiroCustoViewModel.kt`: Encaminhamento de `restoreDailyRecord` para a UI do Histórico.
- `app/src/main/java/com/example/ui/screens/history/HistoryHeaders.kt`: Reorganização do layout do `MonthHeaderCard` para o grid 2x2 (Bruto e Líquido na linha de cima; Pacotes e KM na linha de baixo).
- `app/src/main/java/com/example/ui/screens/history/HistoryEditRecordDialog.kt`: Adição do campo de Data com seletor `DatePickerDialog`, ComboBox pesquisável de plataformas com suporte à rolagem e digitação, e separação limpa de `EditRecordDialogContent`.
- `app/src/main/java/com/example/ui/screens/HistoryScreen.kt`: Integração da ComboBox de plataformas com as plataformas cadastradas no banco (`allPlatforms`), salvamento de nova data e timestamp, diálogo de confirmação de exclusão (Trava 1.3.a) e Snackbar com botão de "Desfazer" para restauração imediata (Trava 1.3.b).
- `app/src/main/res/values/strings.xml`: Adição das novas strings semânticas (`history_edit_date_label`, `history_dialog_delete_record_title`, `history_dialog_delete_record_message`, `history_snackbar_record_deleted`, `history_toast_record_restored`).
- `docs/Aba Historico/DESIGN.md`: Atualização da documentação formal de design registrando a disposição da grade 2x2 no cabeçalho mensal.
- `app/src/test/java/com/example/RepositoryAndEstimationTest.kt`: Teste unitário comprovando a integridade da restauração do registro e recálculo de desgaste de peças no Room.
- `app/src/test/java/com/example/HistoryRefactorTest.kt`: Testes de interface para a grade 2x2 mensal, expansão do card com botões de ação, edição de data e combobox de plataformas, e dupla trava de segurança de exclusão.

---

## 3. Evidências

### 3.1. Testes Unitários e de UI (`./gradlew testDebugUnitTest`)
- **Comando executado**:
  ```bash
  ./gradlew testDebugUnitTest
  ```
- **Contagem real de testes**:
  - **Executados**: 441
  - **Aprovados**: 441
  - **Falhas**: 0
  - **Ignorados / Pulados**: 0 (0% de omissão, 100% de sucesso real)
- **Caminho do relatório de testes**:
  `file:///home/nando/Documentos/GiroCustov0.1/app/build/reports/tests/testDebugUnitTest/index.html`
- **Últimas linhas da saída do Gradle**:
  ```text
  > Task :app:compileDebugUnitTestKotlin
  > Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
  > Task :app:processDebugUnitTestJavaRes UP-TO-DATE
  > Task :app:testDebugUnitTest
  > Task :app:finalizeTestRoborazziDebug SKIPPED

  BUILD SUCCESSFUL in 6m 36s
  33 actionable tasks: 7 executed, 26 up-to-date
  Configuration cache entry reused.
  ```

### 3.2. Análise Estática de Código (`./gradlew lintDebug`)
- **Comando executado**:
  ```bash
  ./gradlew lintDebug
  ```
- **Resultado**: Aprovado com zero erros de compilação ou linting.
- **Caminho do relatório de lint**:
  `file:///home/nando/Documentos/GiroCustov0.1/app/build/reports/lint-results-debug.html`
- **Últimas linhas da saída**:
  ```text
  > Task :app:lintReportDebug
  Wrote HTML report to file:///home/nando/Documentos/GiroCustov0.1/app/build/reports/lint-results-debug.html
  > Task :app:lintDebug

  BUILD SUCCESSFUL in 2m 4s
  31 actionable tasks: 8 executed, 23 up-to-date
  Configuration cache entry reused.
  ```

### 3.3. Compilação do APK de Debug (`./gradlew assembleDebug`)
- **Comando executado**:
  ```bash
  ./gradlew assembleDebug
  ```
- **Resultado**: `BUILD SUCCESSFUL in 37s` (APK gerado em `app/build/outputs/apk/debug/app-debug.apk`).

---

## 4. Testes

### Testes adicionados
1. `RepositoryAndEstimationTest.kt:testRepository_deleteAndRestoreDailyRecord_restoresDataAndWearCorrectly`:
   - *Motivo*: Validar a lógica de negócios da Trava 1.3.b no banco de dados Room: comprova que ao excluir um registro (`deleteRecord`) e em seguida restaurá-lo (`restoreRecord`), o flag `isDeleted` volta a `false`, o desgaste das peças do veículo (`runKmSinceChange`) é recomposto corretamente somando a quilometragem da rota, e o hodômetro do veículo reflete o valor correto.
2. `HistoryRefactorTest.kt:testHistoryRecordCard_expandAndShowEditAndDeleteButtons`:
   - *Motivo*: Validar o requisito 1: o card inicia colapsado (sem os botões visíveis) e, ao sofrer clique, expande e exibe os botões Editar e Excluir com alvos de toque maiores ou iguais a 48dp.
3. `HistoryRefactorTest.kt:testEditRecordDialog_displaysDateAndPlatformCombobox_andSavesCorrectly`:
   - *Motivo*: Validar os requisitos 1.1 e 1.2: verifica a exibição do campo de data formatado (`dd/MM/yyyy`), a abertura da lista suspensa de plataformas através do botão da combobox, a exibição de todas as opções cadastradas, a seleção de uma nova plataforma ("Shopee") e a passagem correta dos novos parâmetros no callback `onSave`.
4. `HistoryRefactorTest.kt:testHistoryScreen_deleteRecord_showsConfirmationDialogAndCancelsOrConfirms`:
   - *Motivo*: Validar as travas 1.3.a e 1.3.b no fluxo completo de tela: ao clicar na lixeira do card expandido, exibe o diálogo de confirmação; ao clicar em cancelar, o diálogo fecha sem excluir; ao confirmar, o diálogo fecha e surge na parte inferior o Snackbar com a ação "Desfazer".

### Testes alterados
1. `HistoryRefactorTest.kt:testMonthHeaderCard_grid2x2Submetrics_displaysCorrectLabelsValuesAndMinHeight`:
   - *Motivo da alteração*: O teste anterior validava a disposição antiga com três colunas. Foi atualizado para validar estritamente a nova geometria 2x2 do requisito 2 (Bruto e Líquido na linha de cima, com altura mínima de 52dp e posicionados acima dos subcards Pacotes e KM).

### Testes removidos
- **Nenhum teste existente foi removido ou enfraquecido.**

---

## 5. O que NÃO foi feito ou está simulado

- **Nada.** Todas as implementações foram entregues com código de produção real e integrado ao banco de dados Room:
  - O desfazimento da exclusão utiliza uma transação Room atômica real (`@Transaction`) que desfaz o soft delete e restaura as peças.
  - O seletor de data utiliza o `DatePickerDialog` nativo do Android.
  - A combobox utiliza o `DropdownMenu` Material 3 com filtragem de texto e rolagem ativa.
  - Nenhuma resposta ou estado foi mockado ou simulado em código de produção.

---

## 6. Riscos e dívidas encontrados

1. **Dependência de scroll em listas longas para testes Robolectric**: Componentes aninhados em `LazyColumn` precisam de `performScrollToNode` nos testes automatizados para que os nós fora da janela de visualização de 800dp sejam renderizados pelo motor de layout. O teste de tela foi blindado com esta instrução explícita.
2. **Formato de data no banco vs UI**: O banco armazena datas em strings no formato ISO `yyyy-MM-dd` e timestamps em milissegundos. Na edição, foi garantida a atualização sincronizada de ambos os campos (`dateString` e `dateTimestamp`), garantindo que tanto a ordenação hierárquica por milissegundos quanto as buscas textuais por string funcionem sem desvios.

---

## 7. O que o usuário precisa fazer (Roteiro de Teste no Aparelho)

Para validar pessoalmente no smartphone físico:

1. **Instalar o aplicativo**:
   - Conecte o smartphone ao computador via cabo USB (com a depuração USB ativada) ou copie o APK `app/build/outputs/apk/debug/app-debug.apk` para o aparelho.
   - Execute no terminal:
     ```bash
     adb install -r app/build/outputs/apk/debug/app-debug.apk
     ```
2. **Acessar a Aba Histórico**:
   - Abra o app **GiroCusto** no celular.
   - Na barra de navegação inferior, toque na aba **Histórico** (segundo ícone).
3. **Validar o Card de Resumo Mensal (Requisito 2)**:
   - Observe o primeiro card grande do topo referente ao mês atual (ex.: "Novembro 2026").
   - Verifique a disposição dos 4 blocos de métricas organizados em grade 2x2:
     - Na linha de cima: os blocos maiores de **Bruto** (verde/destacado) e **Líquido** (azul/destacado).
     - Na linha de baixo: os dois blocos menores de **Pacotes** e **KM** alinhados lado a lado.
4. **Validar Expansão do Card de Corrida (Requisito 1)**:
   - Localize um card de corrida na linha do tempo. Note que ele começa colapsado, mostrando a sigla da plataforma, KM, tempo online e valor bruto.
   - Dê **um toque** no card:
     - O card deve se expandir suavemente.
     - Devem surgir a faixa de horários (início, fim e pausas), a grade de custos (combustível, desgaste, fixo e lucro líquido) e **dois botões na parte inferior**: **Editar** e **Excluir** (ícone de lixeira vermelha).
5. **Validar a Edição de Plataforma e Data (Requisitos 1.1 e 1.2)**:
   - Toque no botão **Editar** do card expandido.
   - Uma janela modal ("Editar Lançamento") será aberta.
   - **Teste da Plataforma (ComboBox - Requisito 1.1)**:
     - Toque na seta para baixo do campo "Plataforma": a lista suspensa deve abrir com todas as plataformas cadastradas (iFood, Uber, Mercado Livre, Shopee, etc.). Role a lista para cima e para baixo e selecione uma plataforma diferente.
     - Alternativamente, apague o texto e digite as primeiras letras de uma plataforma (ex.: "Shop"): a lista deve filtrar automaticamente mostrando apenas as opções correspondentes.
   - **Teste da Data (Requisito 1.2)**:
     - Toque no campo "Data" ou no ícone de calendário à direita: o calendário nativo do Android se abrirá.
     - Escolha uma data diferente (ex.: dia anterior) e toque em **OK**.
     - Observe que o campo reflete a nova data no padrão brasileiro (`dd/MM/yyyy`).
   - Toque no botão **Salvar**: o registro é atualizado e reposicionado no dia correspondente da linha do tempo.
6. **Validar as Travas de Exclusão e Desfazer (Requisito 1.3)**:
   - Toque novamente no card para expandi-lo e toque no botão **Excluir (Lixeira)**.
   - **Trava 1.3.a (Confirmação)**: Uma caixa de diálogo surge na tela com o título `"Excluir lançamento?"` e a pergunta confirmando a exclusão.
     - Toque em **Cancelar**: o diálogo fecha e o registro **permanece intacto**.
     - Toque novamente na lixeira e agora toque em **Excluir**: o diálogo fecha e o registro desaparece da lista.
   - **Trava 1.3.b (Desfazer)**:
     - Imediatamente após a confirmação, observe a barra preta/cinza (Snackbar) que surge na parte inferior da tela com o texto `"Lançamento de [Plataforma] excluído."` e o botão **"DESFAZER"**.
     - Toque rapidamente em **"DESFAZER"** antes da mensagem sumir: o lançamento reaparece imediatamente na linha do tempo e uma mensagem `"Lançamento restaurado com sucesso!"` é exibida.

---

## 8. Commit e sincronização

- **Branch de trabalho**: `feature/ajuste-layout-historico-mes`
- **Hash do commit**: `a8a990c` (atualizado no amend)
- **Status do push**: Sincronizado com `origin/feature/ajuste-layout-historico-mes`
