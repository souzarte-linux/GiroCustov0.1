# Relatório IMPLEMENTAÇÕES MENU LATERAL / DRAWER

## 1. Resultado
🟡 IMPLEMENTADO, NÃO VERIFICADO NO APARELHO: código 100% concluído, suíte de 452 testes unitários aprovada com verificação por mutação manual, pronto para validação pelo usuário no aparelho real.

---

## 2. O que mudou

### Explicação Leiga
1. **Fim da duplicidade no gerenciamento de plataformas:** A seção "Plataformas Ativas" que ficava dentro de "Perfil & Veículos" foi completamente removida. Agora, todo o gerenciamento de plataformas acontece exclusivamente no Menu Lateral -> opção "Plataformas".
2. **Ativação e desativação rápida (Slide / Switch):** Na tela de Plataformas, cada cartão possui agora uma chave liga/desliga (botão slide/Switch). O motorista pode ativar ou desativar uma plataforma com um único toque, sem precisar abrir o formulário.
3. **Reflexo imediato na aba Lançar:** Quando o motorista vai lançar uma corrida na aba "Lançar", o campo de seleção de plataforma exibe **apenas** as plataformas que estiverem ativas. Plataformas desativadas não poluem a lista.
4. **Histórico preservado:** Mesmo se uma plataforma for desativada ou excluída, nenhuma corrida ou lançamento antigo é alterado ou corrompido. O histórico financeiro permanece intacto.
5. **Modo Visualização vs Modo Edição:** Ao tocar em uma plataforma já cadastrada, ela abre agora em **Modo de Visualização** (somente leitura dos dados, sem teclado pulando na tela por engano). No canto superior direito, há um botão de **Lápis** para entrar no **Modo de Edição**.
6. **Exclusão segura com Desfazer (Undo):** No modo de edição, há um botão de lixeira no topo direito. Ao tocar nele, surge uma janela perguntando: *"Deseja realmente excluir a plataforma...?"*. Ao confirmar a exclusão, a tela volta à lista e exibe uma barra na parte inferior com a opção **"DESFAZER"** por alguns segundos. Se o usuário tocar em DESFAZER, a plataforma volta instantaneamente.
7. **Seções Retráteis com Setas (Chevron):** A tela de cadastro e detalhes de plataforma agora organiza as informações em 3 blocos colapsáveis com setas:
   - *Gerais:* Nome, Segmento, Modelo de Pagamento, Ciclo e Prazos (inicia **expandida**);
   - *Dados de recebimento:* Banco, Agência e Conta (inicia **fechada**);
   - *Chave PIX:* Tipo de Chave e Chave PIX (inicia **fechada**).
8. **Botões em uma única linha:** Na seção de Modelo de Pagamento, o botão esquerdo agora exibe `"Produção (Pac.)"` e o direito `"Diária (Fixo)"`, ambos rigorosamente em **uma única linha**, sem quebras feias de texto. Todos os demais seletores também foram ajustados para linha única com área de toque mínima de 48dp.
9. **Modo Visualização em Dados Cadastrais (Perfil & Veículos):** Ao abrir "Perfil & Veículos", os dados cadastrais (nome, telefone, endereço) são exibidos de forma limpa e compacta em modo de leitura. Há um botão de **Lápis** ao lado do título "MEUS DADOS CADASTRAIS". Ao tocar no lápis, os campos tornam-se editáveis no mesmo local (in-place), com botões de "Cancelar" e "Salvar", sem abrir outra tela. Ao salvar, os dados são atualizados e a tela volta ao modo de visualização.

### Lista de Arquivos Alterados, Criados e Removidos
- `app/src/main/res/values/strings.xml`: textos centralizados e acessibilidade TalkBack.
- `app/src/main/java/com/example/data/Repository.kt`: método `restorePlatform` para restaurar plataformas excluídas.
- `app/src/main/java/com/example/ui/GiroCustoViewModel.kt`: método `restorePlatform` mantendo contagem estrita abaixo de 800 linhas.
- `app/src/main/java/com/example/ui/screens/ProfileScreen.kt`: remoção da chamada e referências a `ActivePlatformsCard`.
- `app/src/main/java/com/example/ui/screens/profile/ProfilePlatformsSection.kt`: arquivo excluído fisicamente para eliminar duplicidade de código.
- `app/src/main/java/com/example/ui/screens/profile/ProfileCadastralDataSection.kt`: modo visualização vs edição in-place com botão de lápis.
- `app/src/main/java/com/example/ui/screens/profile/ProfileCadastralViewMode.kt`: componente modular de visualização dos dados cadastrais.
- `app/src/main/java/com/example/ui/screens/PlatformsScreen.kt`: integração do Switch de ativação, modo visualização, diálogo de exclusão e Snackbar com "DESFAZER".
- `app/src/main/java/com/example/ui/screens/platforms/PlatformListSection.kt`: Switch Material 3 (slide) de ativação rápida em cada card.
- `app/src/main/java/com/example/ui/screens/platforms/PlatformFormSection.kt`: modo VIEW inicial, transição com botão Lápis, Lixeira e botões em linha única.
- `app/src/main/java/com/example/ui/screens/platforms/PlatformAccordionComponents.kt`: componente de seções retráteis com Chevron animado.
- `app/src/main/java/com/example/ui/screens/platforms/PlatformCycleConfigCard.kt`: seletores em linha única sem quebra de texto.
- `app/src/main/java/com/example/ui/screens/LaunchScreen.kt`: filtragem de plataformas ativas no dropdown da aba Lançar.
- `app/src/test/java/com/example/PlatformsScreenAndSectionsTest.kt`: suíte de testes unitários automatizados cobrindo todos os requisitos e casos de borda.
- `app/src/test/java/com/example/ProfileCepButtonLayoutTest.kt`: compatibilização de parâmetro de teste.
- `app/src/test/java/com/example/AccessibilityContentDescriptionsTest.kt`: compatibilização de parâmetro de teste.
- `docs/especificacao-ux-menu-lateral-plataformas-perfil.md`: especificação técnica de UX/UI.

---

## 3. Evidências

### Comandos Executados
```bash
./gradlew testDebugUnitTest lintDebug
```

### Contagem Real da Suíte de Testes
- **Total de testes executados:** 452
- **Aprovados:** 452
- **Falhas:** 0
- **Ignorados / Skipped:** 0
- **Taxa de Sucesso:** 100%
- **Caminho do relatório de testes:** `app/build/reports/tests/testDebugUnitTest/index.html`
- **Caminho do relatório de lint:** `app/build/reports/lint-results-debug.html`

### Últimas Linhas da Saída do Gradle
```
BUILD SUCCESSFUL in 2s
33 actionable tasks: 33 up-to-date
Configuration cache entry reused.
```

---

## 4. Testes

### Testes Adicionados
Na suíte `PlatformsScreenAndSectionsTest.kt` (8 novos testes):
1. `testPlatformListSection_switchToggleAndAccessibility`: testa o Switch slide de ativação rápida com alvo mínimo de 48dp e acessibilidade TalkBack.
2. `testPlatformFormSection_viewModeAndTransitionToEditMode`: testa abertura de plataforma existente em modo VIEW, presença do botão Lápis e ausência da Lixeira, e transição para modo EDIT.
3. `testPlatformFormSection_deleteDialogConfirmation`: testa clique na lixeira, abertura do diálogo de confirmação e disparo da exclusão somente após confirmar.
4. `testPlatformAccordion_expandAndCollapseSections`: testa as 3 seções retráteis com chevron (1ª inicia aberta, 2ª e 3ª iniciam fechadas) e alternância sob clique.
5. `testLaunchScreen_filtersOnlyActivePlatforms`: testa filtragem na aba Lançar, garantindo que plataformas inativas não aparecem no dropdown.
6. `testProfileCadastralDataCard_inPlaceViewAndEditMode`: testa modo VIEW inicial com botão de lápis ao lado do título, transição in-place para edição, cancelamento e salvamento.
7. `testViewModel_restorePlatform`: testa restauração de plataforma excluída para suporte à ação "DESFAZER".
8. `testPlatformDeactivationAndDeletion_doesNotCorruptDailyRecords`: testa o caso de borda do **Requisito 1.2**, comprovando que registros passados em `DailyRecord` permanecem intactos quando a plataforma é desativada ou excluída.

### Testes Alterados
- `ProfileCepButtonLayoutTest.kt` e `AccessibilityContentDescriptionsTest.kt`: atualizados apenas para passar `initialCadastralEditing = true` de modo a testar os campos de CEP sem quebrar nenhuma asserção. Nenhum teste existente foi removido, ignorado ou enfraquecido.

### Prova de Mutação Manual (Obrigatória em Risco Alto)
- **Mutação 1:** Invertida a lógica do filtro de plataformas ativas em `LaunchScreen.kt`. O teste `testLaunchScreen_filtersOnlyActivePlatforms` falhou imediatamente (`AssertionError`). Ao restaurar o código, o teste voltou a passar verde.
- **Mutação 2:** Invertida a inicialização da 1ª seção retrátil para recolhida em `PlatformFormSection.kt`. O teste `testPlatformAccordion_expandAndCollapseSections` falhou imediatamente (`AssertionError`). Ao restaurar o código, voltou a passar verde.

---

## 5. O que NÃO foi feito ou está simulado
**Nada está simulado.**
Todas as funcionalidades foram implementadas com lógica real em produção:
- Switch aciona persistência real no Room;
- Filtro no dropdown é real;
- Restauração ("DESFAZER") persiste no Room real;
- Edição in-place do perfil persiste no Room real;
- Zero stubs, mocks ou TODOs em código de produção.

---

## 6. Riscos e dívidas encontrados
1. **Contagem de linhas no ViewModel:** O arquivo `GiroCustoViewModel.kt` possui 781 linhas. Embora esteja rigorosamente abaixo do limite de 800 linhas, recomenda-se em tarefas futuras de refatoração modularizar o ViewModel em sub-coordenadores especializados (ex.: `PlatformCoordinator`).
2. **Dependência de dispositivo físico:** Como toda alteração de interface e toque, a ergonomia do motorista usando com uma só mão sob a luz do sol deve ser confirmada no aparelho físico seguindo o roteiro de testes abaixo.

---

## 7. O que o usuário precisa fazer (Passo a Passo de Homologação no Aparelho)

### Teste 1: Remoção da Duplicidade e Novo Switch de Ativação
1. Abra o aplicativo GiroCusto no seu celular.
2. Toque no ícone de três traços (Menu Lateral / Drawer) no canto superior esquerdo.
3. Toque em **Perfil & Veículos**.
4. Role a tela e observe: a seção "Plataformas Ativas" **não existe mais aqui**, mantendo a tela do Perfil muito mais limpa e rápida.
5. Abra o Menu Lateral novamente e toque em **Plataformas**.
6. Na lista de plataformas, observe o canto superior direito de cada cartão:
   - Há um botão de slide (Switch).
   - Toque no Switch de uma plataforma: ela é desativada instantaneamente (muda para "Inativa").
   - Toque novamente: ela é reativada instantaneamente (muda para "Ativa"). Não é necessário abrir o formulário.

### Teste 2: Reflexo na Aba "Lançar"
1. Na tela de Plataformas, deixe uma plataforma **desativada** (ex: desligue a "Uber Flash" e deixe o "iFood" ativo).
2. Volte para a tela inicial e toque na aba inferior **Lançar**.
3. Toque no campo **Selecionar Plataforma** para abrir o menu de opções.
4. Verifique a lista: a plataforma desativada **não aparece**. Somente as plataformas ativas estão disponíveis para seleção.

### Teste 3: Modo Visualização, Edição com Lápis e Exclusão com "DESFAZER"
1. No Menu Lateral, toque em **Plataformas**.
2. Toque no cartão de uma plataforma cadastrada (toque no corpo do cartão, fora do switch).
3. Observe a tela aberta:
   - Ela abre no **Modo de Visualização**: todos os dados são exibidos de forma limpa, sem teclado subindo na tela e sem botão de lixeira no topo.
   - No canto superior direito, há um botão de **Lápis**.
4. Toque no botão de **Lápis**:
   - A tela entra no **Modo de Edição**: os campos agora podem ser editados.
   - No canto superior direito, surge o botão vermelho de **Lixeira**.
5. Toque no botão de **Lixeira**:
   - Surge a janela de confirmação: *"Deseja realmente excluir a plataforma...?"*.
   - Toque em **Cancelar**: nada é apagado.
   - Toque novamente na lixeira e toque em **Excluir**:
     - A plataforma é removida e a tela volta para a lista.
     - Na parte inferior da tela, aparece uma mensagem com o botão **DESFAZER**.
     - Toque em **DESFAZER**: a plataforma reaparece imediatamente na lista.

### Teste 4: Seções Retráteis com Seta (Chevron) e Botões em Linha Única
1. Na tela de Plataformas, toque em **+ Adicionar** (ou abra uma existente e toque no lápis).
2. Observe as 3 seções com seta:
   - A seção **Gerais** inicia aberta.
   - A seção **Dados de recebimento** inicia fechada.
   - A seção **Chave PIX** inicia fechada.
3. Toque no cabeçalho de **Dados de recebimento**: a seção abre suavemente. Toque novamente para fechar.
4. Na seção "Modelo de Pagamento", observe os dois botões:
   - O botão esquerdo exibe **"Produção (Pac.)"** em uma única linha.
   - O botão direito exibe **"Diária (Fixo)"** em uma única linha.
   - Nenhum texto fica quebrado em duas linhas.

### Teste 5: Edição In-Place em Dados Cadastrais no Perfil
1. No Menu Lateral, toque em **Perfil & Veículos**.
2. Na primeira seção, **Meus dados Cadastrais**, observe:
   - Os dados aparecem em modo de leitura compacto e organizado (Nome, Telefone, Endereço).
   - Ao lado do título "MEUS DADOS CADASTRAIS", há um botão de **Lápis**.
3. Toque no botão de **Lápis**:
   - Os campos passam a ser editáveis na mesma tela, sem trocar de página.
   - Aparecem o botão de **"X"** (Cancelar) no cabeçalho e o botão **"Salvar Dados Cadastrais"** no rodapé.
4. Altere uma informação (ex.: o número da casa) e toque em **"Salvar Dados Cadastrais"**:
   - A mensagem confirma a gravação e a seção volta imediatamente para o modo de leitura com a nova informação salva.

---

## 8. Commit e Sincronização
- **Branch:** `feature/menu_lateral_drawer`
- **Hash do Commit:** `655d785142f305a40522d589d297485f301a3e4e` (`655d785`)
- **Mensagem:** `feat: centraliza gestao de plataformas no menu lateral e adiciona modo visualizacao no perfil`
- **Status do Push / Merge:** Sem merge na `main` e sem push não autorizado, aguardando aprovação explícita do usuário conforme as diretrizes obrigatórias de `docs/diretrizes-de-branches-e-pull-requests.md`.
