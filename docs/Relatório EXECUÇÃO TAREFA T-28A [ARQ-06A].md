# Relatório de Execução — Tarefa T-28A [ARQ-06A]
## Diagnóstico, Inventário de Preferências e Desenho de Metas na Nuvem (Opção B)

**Data de Execução:** 26 de Outubro de 2026  
**Responsável Técnico:** Agente Arquiteto (com apoio de Banco de Dados)  
**Revisão Independente:** Agente Designer (UX/UI e Acessibilidade) e Agente Revisor Independente  
**Verificação DevOps:** Agente DevOps  
**Coordenação e Governança:** Agente Orquestrador  
**Documentos de Referência:**
- [docs/plano-onda-2.md](file:///home/nando/Documentos/GiroCustov0.1/docs/plano-onda-2.md) (Seção 3.d.1 e Seção 6 - Ajustes v2.2)
- [docs/design-metas-por-conta.md](file:///home/nando/Documentos/GiroCustov0.1/docs/design-metas-por-conta.md) (Especificação Técnica Aprovada)
- [.agents/rules/equipe-agentes.md](file:///home/nando/Documentos/GiroCustov0.1/.agents/rules/equipe-agentes.md)
- [.agents/rules/integridade-de-testes.md](file:///home/nando/Documentos/GiroCustov0.1/.agents/rules/integridade-de-testes.md)
- [.agents/workflows/commit.md](file:///home/nando/Documentos/GiroCustov0.1/.agents/workflows/commit.md)

---

### 1. Resultado
✅ **VERIFICADO**: Diagnóstico no código real, inventário de preferências e desenho técnico completo concluídos com 100% de conformidade com a Seção 3.d.1 do Plano da Onda 2. A especificação técnica foi formalizada em `docs/design-metas-por-conta.md`, aprovada com ressalvas construtivas pelo Designer e Revisor Independente, e validada com suíte completa de 225 testes unitários e lint aprovados. Nenhuma linha de código-fonte foi alterada. O projeto atinge o ponto de **PARADA OBRIGATÓRIA**, aguardando aprovação formal do proprietário para liberação da Tarefa T-28B.

---

### 2. O que mudou (Explicação Leiga e Clara)

#### Explicação para o Usuário
No GiroCusto atual, as 12 metas financeiras e de rotina de trabalho do entregador (como meta de faturamento diário de R$ 200, líquido de R$ 130, quilometragem e quantidade de entregas) eram salvas em um arquivo temporário no celular (`giro_custo_goals.xml`).

Descobrimos dois problemas sérios nesse formato:
1. **Perda de Metas ao Trocar de Conta:** Sempre que alguém deslogava e confirmava a troca de conta com descarte de dados, o aplicativo apagava esse arquivo por inteiro (`clear()`), destruindo todas as metas personalizadas que o motorista havia configurado.
2. **Tema Escuro Misturado com Metas:** A configuração de tema claro/escuro (`is_dark_theme`) estava guardada por engano dentro do mesmo arquivo de metas. Para o tema não sumir quando as metas eram apagadas, o app tinha que fazer uma "gambiarra": ler o tema na memória, apagar tudo e regravar o tema logo depois.

O senhor escolheu a **Opção B**: fazer com que as metas fiquem salvas com segurança no banco de dados local do celular (Room) e sincronizem automaticamente na nuvem (Supabase), acompanhando o motorista mesmo se ele trocar de smartphone ou reinstalar o aplicativo.

Para que essa mudança seja 100% segura e não quebre o aplicativo, realizamos esta Tarefa **T-28A** como um planejamento rigoroso (sem mexer em nenhum código ainda), produzindo o documento arquitetural completo em `docs/design-metas-por-conta.md`. As principais decisões foram:

- **Tabela Própria para Metas (`user_goals`):** Em vez de entupir o cadastro do motorista (`user_profile`) com as 12 metas, criamos uma tabela exclusiva só para metas. Assim, quando o motorista mudar sua meta na rua, o celular gasta pouquíssima internet móvel e bateria, sem perigo de sobrescrever seus dados cadastrais (como CEP ou endereço).
- **Funciona 100% Sem Internet (Offline-First):** O motorista pode alterar suas metas mesmo sem internet (em túneis, garagens ou na rua). A mudança aparece na tela imediatamente (0 segundo de espera). Quando a conexão voltar, o app envia para a nuvem de forma invisível.
- **Regra do Mais Recente (LWW):** Se o motorista mexer nas metas em dois celulares diferentes, a alteração com o horário mais recente sempre prevalecerá com segurança.
- **Valores Padrão Humanizados:** Baseados na realidade dos entregadores brasileiros (jornada de 6 dias na semana com 1 dia de folga; margem líquida de 65% descontando combustível, óleo, manutenção e alimentação; R$ 5.000 bruto / R$ 3.200 líquido mensal).
- **Migração Transparente:** Ao atualizar o app na T-28B, as metas antigas salvas no aparelho serão lidas e guardadas automaticamente no banco de dados, sem o motorista perder nada. O tema escuro será transferido para um arquivo próprio e nunca mais será apagado em trocas de conta.
- **Isolamento de Contas e LGPD:** Ao trocar de conta, as metas antigas não são perdidas na nuvem e o novo usuário baixa suas próprias metas. Se o usuário pedir para excluir a conta (T-10), suas metas são totalmente apagadas do servidor e do aparelho.
- **Melhorias de Usabilidade apontadas pelo Designer e Revisor:** O diálogo de ajuste aceitará digitação com vírgula (`250,00`), abrirá teclado com casas decimais, exibirá aviso verde de sucesso e terá contraste perfeito para sol forte.

#### Arquivos Afetados nesta Tarefa
- Criado: [`docs/design-metas-por-conta.md`](file:///home/nando/Documentos/GiroCustov0.1/docs/design-metas-por-conta.md) (Especificação técnica completa cobrindo os itens 'a' a 'g' da Seção 3.d.1).
- Criado: [`docs/Relatório EXECUÇÃO TAREFA T-28A [ARQ-06A].md`](file:///home/nando/Documentos/GiroCustov0.1/docs/Relatório%20EXECUÇÃO%20TAREFA%20T-28A%20[ARQ-06A].md) (Este relatório de execução).
- Arquivos de código-fonte (.kt, .xml, .sql, .gradle): **Nenhum arquivo alterado** (0 alterações).

---

### 3. Evidências

#### 3.1 Execução da Suíte Completa de Testes Unitários
**Comando:**
```bash
./gradlew testDebugUnitTest --rerun-tasks
```
**Saída do Gradle:**
```
> Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
> Task :app:processDebugUnitTestJavaRes
> Task :app:testDebugUnitTest
> Task :app:finalizeTestRoborazziDebug SKIPPED

BUILD SUCCESSFUL in 4m 19s
33 actionable tasks: 33 executed
Configuration cache entry reused.
```
- **Total de Testes Executados:** 225
- **Aprovados:** 225
- **Falhas:** 0
- **Ignorados:** 0
- **Taxa de Aprovação:** 100%
- **Relatório HTML:** `app/build/reports/tests/testDebugUnitTest/index.html`

#### 3.2 Execução do Lint do Android
**Comando:**
```bash
./gradlew lintDebug
```
**Saída do Gradle:**
```
> Task :app:lintReportDebug UP-TO-DATE
> Task :app:lintDebug

BUILD SUCCESSFUL in 2s
31 actionable tasks: 1 executed, 30 up-to-date
Configuration cache entry reused.
```

#### 3.3 Varreduras Obrigatórias de Privacidade em `docs/` (.agents/workflows/commit.md)
1. **E-mails não mascarados em docs/:** 0 ocorrências (`grep -rnE "[A-Za-z0-9._%+-]{2,}@[A-Za-z0-9.-]+\.[A-Za-z]{2,}" docs | grep -v "\*\*\*"` -> saída vazia).
2. **Telefones em docs/:** 0 ocorrências nos documentos novos. Apenas CEPs de teste e dízima de ponto flutuante em relatórios antigos.
3. **Senhas/passwords em docs/:** 0 senhas reais.
4. **Tokens JWT em docs/:** 0 tokens reais.
5. **URLs Supabase em docs/:** 0 ocorrências no documento de metas.

#### 3.4 Varreduras de Integridade (Código e Testes)
1. **Stubs, mocks e pendências:** 0 stubs acionáveis ou mocks em código de produção.
2. **Testes desativados ou vazios:** 0 testes ignorados (`@Ignore`), vazios ou tautológicos.
3. **Segredos no repositório:** 0 chaves privadas, senhas ou tokens vazados.

---

### 4. Testes
- **Testes adicionados:** Nenhum (tarefa de diagnóstico e especificação técnica, sem alteração de código).
- **Testes alterados:** Nenhum teste existente foi alterado ou enfraquecido.
- **Testes removidos:** Nenhum teste existente foi removido.
- **Planejamento de Testes para a T-28B:** Especificadas 5 suítes automatizadas completas em `docs/design-metas-por-conta.md` (migração de schema com `MigrationTestHelper`, testes de DAO, testes de sincronização PUSH/PULL com `MockWebServer`, teste de migração de SharedPreferences legados e teste de isolamento na troca de conta).

---

### 5. O que NÃO foi feito ou está simulado
- **NENHUMA linha de código foi implementada nesta etapa:** Conforme a regra mandatória de T-28A, as entidades, DAOs, migrations Room, scripts Supabase e telas só serão codificados na Tarefa **T-28B**, após a conclusão da T-08 e a aprovação expressa deste desenho pelo proprietário do projeto.
- **Nenhum mock, stub ou simulação existe no código:** O app de produção continua 100% autêntico e íntegro.
- **Script SQL Remoto:** O script de criação de tabela e RLS no Supabase foi documentado em `docs/design-metas-por-conta.md`, com status oficial inicial ⛔ **BLOQUEADO** até que seja executado pelo usuário no painel web do Supabase durante a Tarefa T-28B.

---

### 6. Riscos e Dívidas Encontrados
1. **Conflito de Versão do Room:** O plano original previa a T-28B e a T-18B na mesma versão 14. O desenho técnico reafirmou a resolução formal: a Tarefa T-08 gerará a versão 13 (`13.json`), a Tarefa T-28B gerará a versão 14 (`14.json`), e a futura migração para centavos na T-18B gerará a versão 15 (`15.json`), mantendo o banco sequencial e sem quebras.
2. **Fragilidade do Diálogo Atual de Metas:** Identificado pelo Designer e Revisor que o diálogo atual em `DashboardScreen.kt:835` faz `toDoubleOrNull()` sem converter vírgulas, fazendo com que números no formato brasileiro (`250,00`) sejam descartados silenciosamente. Isso foi registrado como requisito de correção mandatória para a T-28B.
3. **Migração de Textos para `strings.xml`:** O diálogo atual possui diversos textos fixos (*hardcoded*). A Tarefa T-28B deverá externalizá-los obrigatoriamente para `strings.xml`.

---

### 7. O que o Usuário precisa fazer

#### Nesta Etapa (T-28A - Parada Obrigatória)
1. **Revisar o Desenho Técnico:** Leia a explicação na Seção 2 deste relatório e, se desejar detalhes técnicos profundos, consulte o arquivo [`docs/design-metas-por-conta.md`](file:///home/nando/Documentos/GiroCustov0.1/docs/design-metas-por-conta.md).
2. **Autorizar o Próximo Passo:** Se concordar com a modelagem de tabela separada (`user_goals`), com o funcionamento offline-first e com os valores padrão propostos, responda no chat aprovando o desenho para que o plano da Onda 2 possa seguir para as tarefas seguintes.
3. **Nenhum teste no aparelho é necessário agora:** Como não houve alteração de código, não há necessidade de testes no celular nesta tarefa T-28A. Os roteiros de teste manual no smartphone serão executados na homologação da futura Tarefa T-28B.

---

### 8. Commit e Sincronização
- **Hash do Commit:** *(Será preenchido imediatamente após a execução do commit pelo DevOps)*
- **Status do Push:** *(Será preenchido imediatamente após o push)*
