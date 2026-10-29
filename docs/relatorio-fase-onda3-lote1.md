# RELATÓRIO DA FASE: ONDA 3 — LOTE 1 (LIMPEZA, DOCUMENTAÇÃO E HIGIENIZAÇÃO)

**Data:** 28 de Outubro de 2026  
**Autor:** Agente Orquestrador  
**Tarefas Integradas:** T-19 [DAT-02], T-21 [ACC-02], T-22 [ARQ-04], T-23 [DEV-01]  

---

## 1. Resultado
🟡 **IMPLEMENTADO, NÃO VERIFICADO NO APARELHO**: código pronto, documentação formalizada, dependências mortas e cores órfãs expurgadas, 104 strings centralizadas em `strings.xml`, 364 testes unitários 100% aprovados, Lint sem erros e compilação minificada com R8 (`assembleReleaseCheck`) bem-sucedida; aguarda verificação no aparelho físico.

---

## 2. O que mudou
Esta fase focou na higienização do código, formalização da governança de banco de dados e acessibilidade de textos, compreendendo quatro frentes:

1. **Formalização de Schemas Room (T-19):** Criação do documento canônico [docs/database-schemas.md](file:///home/nando/Documentos/GiroCustov0.1/docs/database-schemas.md) (467 linhas) formalizando o histórico completo das 15 versões do banco de dados Room (`giro_custo_database`), a obrigatoriedade inegociável de `exportSchema = true` para rastreamento de esquemas JSON, a proibição absoluta de migração destrutiva (`fallbackToDestructiveMigration`) e a regra de migração sequencial estrita.
2. **Migração de Textos para strings.xml (T-21):** Centralização de mais de 70 textos fixos em código Kotlin das telas `DashboardScreen` e `LaunchScreen` no arquivo de recursos `strings.xml`. Isso inclui títulos, subtítulos, rótulos de campos numéricos (hodômetros, faturamento, despesas), mensagens de estimativa de combustível, estimador em tempo real, resumos de turno e todos os textos do formulário de cadastro de plataformas, permitindo internacionalização e TalkBack acessível.
3. **Remoção de Dependências Mortas de IA (T-22):** Remoção das dependências `firebase.ai` e `firebase-appcheck-recaptcha` de `app/build.gradle.kts` e `gradle/libs.versions.toml`, além da limpeza da chave `GEMINI_API_KEY` do arquivo `.env.example`, enxugando o build e reduzindo a superfície de dependências sem uso.
4. **Limpeza de Recursos Órfãos (T-23):** Eliminação das 7 cores não utilizadas herdadas do template padrão do Android Studio (`purple_200`, `purple_500`, `purple_700`, `teal_200`, `teal_700`, `black`, `white`) do arquivo `colors.xml`, limpando apontamentos de recursos não utilizados do Android Lint.

### Arquivos afetados:
- [docs/database-schemas.md](file:///home/nando/Documentos/GiroCustov0.1/docs/database-schemas.md) *(novo)*
- [app/src/main/res/values/strings.xml](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/res/values/strings.xml)
- [app/src/main/java/com/example/ui/screens/DashboardScreen.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/screens/DashboardScreen.kt)
- [app/src/main/java/com/example/ui/screens/LaunchScreen.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/ui/screens/LaunchScreen.kt)
- [app/src/main/res/values/colors.xml](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/res/values/colors.xml)
- [app/build.gradle.kts](file:///home/nando/Documentos/GiroCustov0.1/app/build.gradle.kts)
- [gradle/libs.versions.toml](file:///home/nando/Documentos/GiroCustov0.1/gradle/libs.versions.toml)
- [.env.example](file:///home/nando/Documentos/GiroCustov0.1/.env.example)

---

## 3. Evidências
- **Comando executado:**
  ```bash
  ./gradlew testDebugUnitTest lintDebug assembleReleaseCheck
  ```
- **Contagem real de testes:**
  - **Executados:** 364
  - **Aprovados:** 364
  - **Falhas:** 0
  - **Ignorados / Skipped:** 0
  - **Taxa de Sucesso:** 100%
  - **Duração da execução de testes:** 2m 43.85s
- **Caminho do relatório de testes:**
  `app/build/reports/tests/testDebugUnitTest/index.html`
- **Caminho do relatório do Lint:**
  `app/build/reports/lint-results-debug.html`
- **Binário gerado e minificado com R8:**
  `app/build/outputs/apk/releaseCheck/app-releaseCheck.apk`
- **Últimas linhas da saída do Gradle:**
  ```
  > Task :app:compileDebugUnitTestKotlin
  > Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
  > Task :app:processDebugUnitTestJavaRes UP-TO-DATE
  > Task :app:testDebugUnitTest
  > Task :app:lintReportDebug
  Wrote HTML report to file:///home/nando/Documentos/GiroCustov0.1/app/build/reports/lint-results-debug.html
  > Task :app:lintDebug
  > Task :app:finalizeTestRoborazziDebug SKIPPED
  > Task :app:minifyReleaseCheckWithR8
  > Task :app:packageReleaseCheck
  > Task :app:assembleReleaseCheck
  
  BUILD SUCCESSFUL in 14m 12s
  91 actionable tasks: 66 executed, 8 from cache, 17 up-to-date
  Configuration cache entry stored.
  ```

---

## 4. Testes
- **Nenhum teste existente foi alterado ou enfraquecido.**
- A suíte integral de 364 testes unitários automatizados foi executada e validada com 100% de sucesso.
- Todas as asserções de validação de Room, migrações v12→v13, v13→v14 e v14→v15, sincronização com Supabase, isolamento de conta, criptografia de sessão via Keystore AES-GCM e regras de negócio de hodômetro e consumo continuam intactas.

---

## 5. O que NÃO foi feito ou está simulado
- Nada está simulado ou em modo mock. Não foram criados stubs nem mensagens falsas.
- As telas secundárias (`HistoryScreen`, `FuelRefillsScreen`, `PlatformsScreen`, `ProfileScreen`) permanecem com textos fixos a serem migrados na tarefa **T-25 [ACC-02B]**.
- A decomposição dos arquivos extensos com mais de 800 linhas (`GiroCustoViewModel`, `ProfileScreen`, `FuelRefillsScreen`, etc.) será tratada na tarefa **T-20 [ARQ-02]**.

---

## 6. Riscos e dívidas encontrados
1. **Tamanho dos arquivos de tela:** `LaunchScreen.kt` possui 1.252 linhas e `DashboardScreen.kt` possui 1.072 linhas. Ambas continuam acima do teto de 800 linhas recomendado pelas regras de arquitetura. Sua quebra em subcomponentes atômicos reutilizáveis está prevista nas tarefas T-20 e T-24 da Onda 3.
2. **Avisos de depreciação do MigrationTestHelper:** O compilador emitiu avisos informando que o construtor legado do `MigrationTestHelper` do Room está obsoleto quando se usa auto-migrations. Como o projeto adota estritamente migrações manuais e sequenciais (sem auto-migration), não há risco iminente, mas recomenda-se atualizar a assinatura nas tarefas futuras de manutenção de testes.

---

## 7. O que o usuário precisa fazer
Para validar esta entrega no aparelho físico, siga o passo a passo completo:

### Passo 1: Instalação da Versão Atualizada
1. Conecte o celular ao computador via cabo USB (com a depuração USB ativada).
2. Abra o terminal e execute:
   ```bash
   ./gradlew installDebug
   ```
   *(Ou instale o APK releaseCheck gerado em `app/build/outputs/apk/releaseCheck/app-releaseCheck.apk`)*

### Passo 2: Verificação Visual da Tela Inicial (Dashboard)
1. Abra o aplicativo **GiroCusto** no celular.
2. Na tela inicial (**Dashboard**):
   - Verifique o seletor de períodos no topo: os botões "Semana", "Quinzena", "Mês Atual" e "Últimos 30 dias" devem aparecer com os textos corretos.
   - Verifique o cartão de faturamento: o título "Faturamento Bruto" e "Custo Operacional" devem estar visíveis.
   - Role até a seção "RESUMO DE DESPESAS DO PERÍODO": confirme que os itens "Combustível", "Alimentação", "Desgaste de Peças (Oculto)", "Custos Fixos Proporcionais" e "Manutenção Corretiva (Gasto Real)" continuam legíveis e com layout idêntico.
   - Verifique os indicadores "Custo Real / Km" e "Lucro Líq / Km".

### Passo 3: Verificação Visual da Tela de Lançamento de Turno
1. Toque no ícone de **Lançamento** (botão do meio na barra inferior ou no menu lateral).
2. Na tela **Lançar Turno do Dia**:
   - Confirme o título "Lançar Turno do Dia" e o subtítulo "Insira as informações de hodômetro e faturamento do seu fechamento de turno.".
   - Toque no botão de alternar tema (ícone de sol/lua no topo direito) para confirmar que o modo claro e escuro funcionam sem quebrar os textos.
   - Verifique os campos de entrada: "Data do Lançamento", "Hodômetro Inicial", "Hodômetro Final", "Ganhos Brutos (R$)", "Nº Entregas", "Preço do Litro (R$)" e "Alimentação / Outros".
   - Digite um hodômetro inicial (ex.: `10000`) e um final (ex.: `10100`). Confirme que o cartão verde exibe a quilometragem calculada (`100.0 KM`).
   - Role até o final e verifique os textos do estimador: "ESTIMADOR EM TEMPO REAL", "Lucro Líquido Projetado:", "Detalhamento de Custos Operacionais:" e o botão verde "Salvar Turno do Dia".
3. Toque no campo **Plataforma** e depois na opção "Cadastrar Nova Plataforma?":
   - O diálogo deve abrir exibindo o título "Cadastrar Nova Plataforma", com os seletores "Segmento", "Modelo de Pagamento", "Ciclo de Pagamento", o botão "Cancelar" e o botão "Salvar".
   - Toque em "Cancelar" para fechar o diálogo.

---

## 8. Commit e sincronização
- **Hash do Commit:** `aa76e89`
- **Mensagem do Commit:** `feat(onda-3): formaliza schemas Room, migra textos Dashboard/Launch, limpa dependências de IA e cores órfãs (T-19, T-21, T-22, T-23)`
- **Status do Push:** Sincronizado com sucesso com o repositório remoto (`origin/main`)
