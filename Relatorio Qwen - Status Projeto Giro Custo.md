# Relatório Qwen — Status do Projeto Giro Custo

**Data da análise:** 23 de novembro de 2026
**Escopo:** Análise integral do repositório `/workspace`, com foco na pasta `docs/` (48 documentos + subpasta `Aba Historico/`).
**Metodologia:** Leitura dos documentos de planejamento (`auditoria-2026-10-22.md`, `plano-correcao-v2.md`, `plano-onda-2.md`), dos relatórios de execução das tarefas T-01 a T-30, dos relatórios de fase/conclusão e da documentação de design/temas; cruzamento dos status declarados entre documentos.

---

## 1. Visão Geral do Projeto

O **GiroCusto** é um aplicativo Android nativo (Kotlin + Jetpack Compose + Room + Supabase) para motoristas e entregadores controlarem custos, faturamento, combustível, metas e histórico por plataforma. O projeto passou por um ciclo formal de governança:

1. **Auditoria completa** (`auditoria-2026-10-22.md`) — identificou **25 achados** (segurança, performance, UX, acessibilidade, arquitetura, LGPD).
2. **Plano de correção v2** (`plano-correcao-v2.md`) — transformou os 25 achados em **26 tarefas organizadas em 3 ondas**.
3. **Plano da Onda 2 revisado** (`plano-onda-2.md`, v2.2) — adicionou 7 tarefas novas (T-26 a T-30, desdobramentos T-06b/c/d, divisão T-14a/b) e manteve o quadro de situação oficial de status.
4. **Execução e homologação** — relatórios individuais por tarefa, consolidados em `relatorio-fase-onda3-lote1.md` e `relatorio-conclusao-geral-ondas-1-2-3.md`.

**Status oficial consolidado (29/out/2026):** ✅ Todas as tarefas das Ondas 1, 2 e 3 concluídas, com **379 testes unitários 100% aprovados**, Lint sem erros, R8 ativo (`releaseCheck`), Room na versão 15 (migração monetária para centavos) e homologação física no smartphone confirmada pelo proprietário. Após esse relatório, evoluções adicionais elevaram a suíte para **437–441 testes** (Linha do Tempo / Aba Histórico) e introduziram o **sistema de 5 temas visuais**.

---

## 2. O Que Foi Feito de Forma COMPLETA (✅ Verificado e Homologado)

Conforme o quadro oficial de `plano-onda-2.md` (Seção 1) e o `relatorio-conclusao-geral-ondas-1-2-3.md`:

### Onda 1 — Segurança imediata e proteção
| Tarefa | Descrição | Evidência |
|---|---|---|
| T-01 [SEC-08] | Blindagem do `.gitignore` (certificados, keystores, credenciais) | Commit `69c9a71` |
| T-02 [SEC-02] | Exclusão de preferências de auth/biometria do backup Android | Commit `077bd66` |
| T-03 [UI-01] | Remoção do `SmartphoneSimulator` do fluxo de produção | Homologado no aparelho |
| T-04 [INT-01/SEC-03] | Ocultação do stub de SMS e redação de logs HTTP (`Level.BASIC`, `Authorization`/`apikey`) | Logcat verificado no aparelho |
| T-05 [UX-01] | Confirmação de exclusão + "Desfazer" via soft delete com proteção LWW | 160/160 testes |
| T-06 [SEC-01/SEC-04] | Sessão segura com Android Keystore AES-GCM e `CryptoObject` real na biometria | 171/171 testes, homologado |
| T-06b | Remoção de fallback em memória em produção (conformidade estrita, `SessionCryptoEngine`) | 172/172 testes, 4 testes manuais OK |
| T-06c | Isolamento estrito de dados entre contas (`DataOwnerStorage`, máquina de troca de conta) | Atualizado para ✅ no plano v2.2 |
| T-06d | Expurgo de dados pessoais/senhas/CSV do repositório e documentação | Commit `5da367d` |
| T-07 [SEC-05] | Ativação do R8, buildType `releaseCheck` e regras ProGuard | Homologado no aparelho (plano v2.2) |

### Onda 2 — Performance, confiabilidade, UX, integrações e LGPD
| Tarefa | Descrição | Evidência |
|---|---|---|
| T-08 [PERF-01] | 20 índices nas 7 entidades Room + Migração v13 validada com `MigrationTestHelper` | 228 testes |
| T-09 [PERF-02] | Chaves estáveis em todas as `LazyColumn` | Homologado no aparelho |
| T-10 [SEC-06] | Portabilidade SAF (JSON) + exclusão definitiva de conta via RPC com fail-safe local | Homologado no aparelho |
| T-11 [SEC-07] | Diálogo de rationale prévia para permissão de GPS | Homologado no aparelho |
| T-12 [UX-02] | Padronização de estados Vazio/Erro/Offline/Loading (`UiStateComponents.kt`) | Homologado (plano atualizado) |
| T-13 [INT-02] | Timeouts de 25s na Overpass API + espelhos ativos | Homologado no aparelho |
| T-14 [INT-03] | Migração Overpass para HTTP POST + espelho de alta performance | Homologado no aparelho |
| T-14a [ARQ-01A] | Expurgo de ganchos de teste/reflexão em Auth e Sessão (interfaces limpas, `@VisibleForTesting`) | 218 testes + aparelho |
| T-14b [ARQ-01B] | Expurgo de ganchos públicos em clientes de rede e banco (injeção via construtor) | 225 testes + aparelho |
| T-15 [ACC-01] | Auditoria TalkBack: `contentDescription` e áreas de toque 48dp | 276 testes + aparelho |
| T-16 [ARQ-03] | Elevação de regras de negócio/cálculos de Composables para ViewModel | 24 novos testes + aparelho |
| T-17 [TST-01] | Substituição de testes de template por 30 testes de regra real + Roborazzi | 336 testes |
| T-26 [UX-03] | Tradução pt-BR dos erros de Auth do Supabase (`AuthErrorMapper.kt`) | 212 testes + aparelho |
| T-27 [ARQ-05] | Remoção de `signInWithPhone` e mensagem residual de homologação | Commit `2ea2387` |
| T-28A [ARQ-06A] | Diagnóstico + desenho técnico de metas por conta (`design-metas-por-conta.md`) | Aprovado formalmente pelo proprietário |
| T-28B [DAT-03] | Metas no Room v14 + sincronização Supabase com LWW | ✅ Verificado |
| T-29 [SEC-05B] | Aperto cirúrgico das regras ProGuard/R8 (keeps genéricos removidos) | 225 testes + 9 fluxos no aparelho, APK 5,3 MB |
| T-30 [SEC-09] | `DataOwnerStorage` obrigatório + trava fail-safe no `SupabaseSyncManager` | Homologado (plano atualizado) |

### Onda 3 — Refatoração estrutural e padrões monetários
| Tarefa | Descrição | Evidência |
|---|---|---|
| T-18A [DAT-01A] | Plano técnico de migração monetária Double → centavos (`plano-migracao-monetaria.md`) | Documento produzido |
| T-18B [DAT-01B] | Implementação da migração para centavos (`Long`) e milésimos de combustível, Room v15, `CurrencyUtils.kt` | Commit `eef134c` |
| T-19 [DAT-02] | Governança de schemas (`database-schemas.md`, 15 versões), bloqueio de migração destrutiva | Commit `aa76e89` |
| T-20A–D [ARQ-02] | Decomposição de `ProfileScreen`, `FuelRefillsScreen`, `HistoryScreen` e `GiroCustoViewModel` (<800 linhas) | Commits `3fe6580`…`412cf3f` |
| T-21 [ACC-02] | Centralização de textos de Dashboard/Launch em `strings.xml` | Commit `aa76e89` |
| T-22 [ARQ-04] | Remoção de dependências mortas de IA (`firebase.ai`, `appcheck`, `GEMINI_API_KEY`) | Commit `aa76e89` |
| T-23 [DEV-01] | Expurgo de cores órfãs do template em `colors.xml` | Commit `aa76e89` |
| T-24A–G [ARQ-02B] | Decomposição de `LaunchScreen`, `CloudSyncScreen`, `DashboardScreen`, `PlatformsScreen`, `Repository.kt`, `ReportsScreen`, `AuthGateScreen` | Commits `1a30b3d`…`3213780` |
| T-25A–E [ACC-02B] | Conclusão da migração de textos secundários para `strings.xml` | Commits `ad2e1d2`…`35f1db3` |

### Funcionalidades pós-ondas (feature work documentado)
- **Sistema de 5 temas visuais** (Emerald Flow, Electric Kinetic, Solar Náutico, Titan Amber, Orchid Glint) com catálogo, seletor no menu lateral, persistência e suporte claro/escuro com contraste WCAG AAA — especificações em `Tema_*.md`, `design-troca-de-temas.md`, `RELATÓRIO DE CRIAÇÃO E SELEÇÃO DE TEMAS NO MENU LATERAL.md`.
- **Migração de todas as telas para tokens semânticos** do tema ativo (`MaterialTheme.colorScheme`) — `RELATÓRIO DE MIGRAÇÃO DAS TELAS PARA AS CORES DO TEMA ATIVO.md`.
- **Reformulação da Linha do Tempo na Aba Histórico**: card enriquecido (entregas, KM, horas online, valor bruto), grid 2x2 semanal com 4º subcard de KM, acordeão inteligente (somente semana/dia mais recentes abertos), acessibilidade TalkBack — `Relatório Linha do Tempo.md` (437 testes, instalado e aberto no aparelho `IJA67XLBNBCML7NV`).
- **Implementações diretas/indiretas na Aba Histórico** (expandir card, botões Editar/Excluir 48dp) — `Relatório IMPLEMENTAÇÕES DIRETA - INDIRETAS ABA HISTÓRICO.md` (441 testes).
- Documentação de design da nova aba: `docs/Aba Historico/DESIGN.md` (+ `code.html` e `screen.png` de referência).

---

## 3. O Que Foi Feito de Forma INCOMPLETA ou Condicionada (🟡)

Estes itens constam nos relatórios individuais com ressalvas que precisam de atenção, mesmo após a conclusão geral declarada:

1. **Divergência de status entre relatórios individuais e o quadro oficial.** Os relatórios das tarefas **T-09, T-10, T-11, T-12, T-13, T-30 e T-07** foram publicados com selo 🟡 *"IMPLEMENTADO, NÃO VERIFICADO NO APARELHO"*, mas depois foram promovidos a ✅ no `plano-onda-2.md` (v2.2) e no relatório conclus geral, **sem relatórios complementares de homologação física anexados à pasta `docs/`**. A evidência da promoção baseia-se apenas em confirmação verbal do proprietário. Recomenda-se registrar formalmente as evidências de teste físico nesses documentos.

2. **Deploy não executado nos trabalhos de temas (3 itens 🟡):**
   - `RELATÓRIO DE CRIAÇÃO E SELEÇÃO DE TEMAS NO MENU LATERAL.md` (394 testes OK): o aparelho **não estava conectado via USB/ADB** no momento — instalação manual pendente.
   - `RELATÓRIO DE CORREÇÃO DO MENU LATERAL E VISIBILIDADE DE TEMAS.md` (395 testes OK): idem — APK pronto, deploy pendente.
   - `RELATÓRIO DE MIGRAÇÃO DAS TELAS PARA AS CORES DO TEMA ATIVO.md` (394 testes OK): sem dispositivo ADB — validação visual pendente.
   - `Relatório IMPLEMENTAÇÕES DIRETA - INDIRETAS ABA HISTÓRICO.md` (441 testes OK): validação visual/tátil final em dispositivo físico pendente.

3. **Scripts SQL no Supabase dependem de ação manual do usuário (Decisão 2 do plano):**
   - `supabase/migrations/20261026_user_deletion_rpc.sql` (RPC `delete_user_account()` — T-10);
   - `supabase/migrations/20261026_user_goals.sql` (tabela/RLS de metas — T-28B).
   Os scripts estão redigidos e validados, mas **a execução no painel web do Supabase nunca foi comprovada documentalmente**; sem eles, a exclusão de conta em nuvem e a sincronização de metas falham em produção.

4. **Renovação de token da T-06 (70 minutos):** o registro v2.2 do plano aponta que restava o teste longo de renovação automática de sessão no aparelho — não há documento dedicado confirmando sua execução.

5. **Login por SMS:** permanece **desabilitado como "Em breve"** (stub ocultado na T-04; código morto removido na T-27). A funcionalidade em si **nunca foi implementada/homologada** — é um recurso prometido e pendente.

6. **Documentos de design ainda com status administrativo pendente:**
   - `plano-migracao-monetaria.md` (T-18A) consta no cabeçalho como 🟡 *"PROPOSTO (Aguardando Aprovação Formal do Usuário)"*, embora a implementação T-18B tenha sido realizada — o documento deveria ser atualizado para "Aprovado/Concluído".
   - `design-isolamento-contas.md` está "Aprovado com Condições"; as condições não têm checklist de encerramento registrado.

7. **Pendência de conformidade de processo:** o commit HEAD do repositório de trabalho (`e5b0a3c fix(historico)…`) não segue integralmente as `diretrizes-de-branches-e-pull-requests.md` (que exigem ramificação por tarefa e PR); o snapshot analisado contém 1 único commit, sem histórico das branches/PRs referenciados nos relatórios.

---

## 4. O Que Ainda DEVE SER FEITO (Recomendações e Pendências)

### Prioridade Alta (bloqueantes para publicação em loja)
1. **Executar e comprovar os scripts SQL no Supabase** (exclusão de conta T-10 e metas T-28B), registrando a evidência nos relatórios correspondentes.
2. **Instalar os APKs dos trabalhos de temas e da Aba Histórico em aparelho físico** e converter os status 🟡 em ✅ com relatórios de homologação (deploy falhou por ausência de cabo/ADB, não por defeito de código).
3. **Gerar chave de assinatura de produção (`upload-key.jks`)** — hoje o `releaseCheck` usa chave de debug; imprescindível para Google Play Console (item listado como oportunidade no relatório conclusivo).
4. **Configurar o app no Google Play Console** (ficha, trilha de teste interno, compliance de privacidade/LGPD na política de dados).

### Prioridade Média (qualidade e fechamento documental)
5. **Centralizar textos estáticos de `OnboardingProfileScreen.kt` em `strings.xml`** (única tela fora da governança ACC-02, apontada no relatório conclusivo).
6. **Rodar o teste de 70 minutos de renovação de token (T-06)** no aparelho e lavrar o resultado.
7. **Harmonizar os status dos relatórios individuais com o quadro oficial** (atualizar os documentos 🟡 de T-07, T-09 a T-13 e T-30 com adenda de homologação física).
8. **Atualizar `plano-migracao-monetaria.md`** de "PROPOSTO" para "APROVADO/EXECUTADO", referenciando o commit `eef134c`.
9. **Consolidar um índice/catálogo da pasta `docs/`** (48 arquivos com nomenclatura heterogênea — "Tarefa T-XX.md" vs "RELATÓRIO DA EXECUÇÃO DA TAREFA T-XX.md" vs "Resultado EXECUÇÃO…"), facilitando rastreabilidade tarefa→relatório→commit.

### Prioridade Baixa (evolução de produto)
10. **Implementar e homologar o Login por SMS** quando houver contrato de gateway (homi­logação citada como futura na T-04/T-26).
11. **Perfilamento real de bateria/GPS/FPS (JankStats)** no aparelho — item que a auditoria original declarou impossível de verificar em contêiner (Seção 7 de `auditoria-2026-10-22.md`).
12. **Internacionalização (i18n):** com os textos já centralizados em `strings.xml`, adicionar traduções além de pt-BR, se desejado.
13. **Aplicar rigorosamente as diretrizes de branches/PR** nas próximas entregas (branch por tarefa, proibição de alteração direta na `main`).

---

## 5. Indicadores de Saúde do Projeto (última medição documentada)

| Indicador | Valor | Fonte |
|---|---|---|
| Testes unitários (última suíte citada) | 441 aprovados / 0 falhas | Relatório Aba Histórico |
| Testes na conclusão das ondas | 379 aprovados (100%) | Relatório conclusivo Ondas 1–3 |
| Android Lint | 0 erros | Diversos relatórios |
| Minificação R8 | Ativa (`assembleReleaseCheck`, APK ~5,3 MB) | T-29 |
| Banco Room | v15 (centavos `Long`, schemas exportados, sem migração destrutiva) | T-18B/T-19 |
| Governança de arquivos | 100% abaixo de 800 linhas (maior: 785) | Relatório conclusivo |
| Precisão monetária | Centavos inteiros e milésimos de combustível | Relatório conclusivo |
| Achados da auditoria sanados | 25/25 | Relatório conclusivo |

---

## 6. Conclusão

O projeto **Giro Custo encontra-se em estado muito maduro**: as três ondas de correção derivadas da auditoria de 22/10/2026 foram **integralmente implementadas no código**, com evolução comprovada da suíte de testes de 159 para até 441 testes automatizados, segurança de sessão/blindagem R8/LGPD resolvidas e refatoração arquitetural concluída. As lacunas reais **não são de código**, e sim de **verificação física e operação externa**: (a) deploys/homologações em aparelho que ficaram 🟡 por ausência de conexão ADB nos ciclos de temas e Histórico; (b) scripts SQL no Supabase dependentes de clique manual do proprietário; (c) preparação final para loja (chave de release e Play Console); e (d) pequena higienização documental de status. Endereçados esses pontos, o app está apto à publicação.

*Relatório gerado por Qwen a partir da análise estática do repositório e da pasta `docs/` em 23/11/2026.*
