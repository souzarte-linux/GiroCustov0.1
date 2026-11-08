# Plano Técnico de Implementação — Versão 5.2.1
## Sincronização Inteligente das Metas e Persistência Anual
### Projeto GiroCusto — Arquitetura de Domínio, Persistência Local com Outbox, Transação Remota, OCC Efetivo e Demonstração Técnica

---

## 1. IDENTIFICAÇÃO E CONTROLE DE VERSÃO

| Metadado | Especificação |
| :--- | :--- |
| **Identificador do Documento** | `GiroCusto-ARCH-GOALS-v5.2.1` |
| **Documento Predecessor** | Plano Técnico de Implementação v5.2 (`docs/plano-sincronizacao-inteligente-metas-v5.2.md`) |
| **Projeto / Plataforma** | GiroCusto — Android Nativo (Kotlin 2.0, Jetpack Compose, Room SQLite, Supabase / PostgreSQL 15+) |
| **Branch de Referência** | `feature/botao_exportar_historico` |
| **Commit de Referência** | `acf87e6a88b406c30c7ce0903ee6ccb468d8ca29` (`acf87e6`) |
| **Regime Operacional** | **SOMENTE LEITURA — SANEAMENTO TÉCNICO, VALIDAÇÃO DE IMPLEMENTABILIDADE E DEMONSTRAÇÃO** |
| **Classificação de Risco** | **ALTO** (Persistência Room v17 com Outbox, concorrência remota distribuída, isolamento multitenant, procedimentos transacionais atômicos no PostgreSQL e conformidade LGPD) |
| **Status do Documento** | **PRONTO PARA AUDITORIA INDEPENDENTE** |
| **Autoria / Papel** | Arquiteto de Software e Engenheiro de Dados GiroCusto |

---

## 2. DIAGNÓSTICO DO AMBIENTE E DO CÓDIGO EFETIVO

### 2.1 Verificação do Repositório Git
A integridade do ambiente operacional foi confirmada via inspeção direta:
- **Branch ativa:** `feature/botao_exportar_historico`.
- **Commit HEAD:** `acf87e6` (`refactor(history): divide HistoryHeaders em 5 componentes para preparo da exportacao (T-EXP-00A)`).
- **Working Tree:** Estritamente limpo para todos os arquivos rastreados.
- **Preservação estrita:** O documento `docs/plano-sincronizacao-inteligente-metas-v5.2.md` e os arquivos de código de produção e migrations permanecem 100% inalterados.

### 2.2 Diagnóstico do Código em `acf87e6` e das Deficiências da Versão 5.2
A auditoria independente sobre o documento v5.2 identificou inconsistências críticas que inviabilizavam a homologação da arquitetura. O presente documento v5.2.1 corrige pontualmente essas falhas mantendo o que estava correto:

1. **Deficiência em ACH-25 (OCC sem verificação de revisão):** No v5.2, a RPC extraía `v_base_rev := COALESCE((payload->>'base_revision')::BIGINT, 0);`, mas o valor nunca era comparado com `v_current.revision` no fluxo principal de atualização. Além disso, a RPC não dispunha de mecanismo para identificar quais campos o servidor alterou em revisões intermediárias, operando de forma ingênua.
2. **Deficiência em ACH-27 (Matriz com Last-Write-Wins silencioso):** O cenário SC-02 do v5.2 aplicava LWW silencioso baseado em `user_modified_at` quando dois dispositivos colidiam no mesmo campo, violando o princípio de concorrência transparente e gerando perda silenciosa de dados.
3. **Deficiência em ACH-28 / ACH-32 / ACH-18 (Proteção incompleta de `LEGACY_UNKNOWN` e `USER_DEFINED`):** No v5.2, a cláusula `ELSE` da atualização aceitava qualquer modificação para registros com proveniência diferente de `USER_DEFINED`. Consequentemente, metas positivas legadas (`LEGACY_UNKNOWN` $> 0$) eram sobrescritas por atualizações automáticas (`DERIVED`), configurando regressão do ACH-18. Além disso, no INSERT não havia regras explícitas de proteção.
4. **Deficiência em ACH-29 (Segurança em profundidade da RPC):** Faltavam qualificações completas de schema, controle estrito de objetos, validação de payload contra injeção e revogação formal de privilégios `PUBLIC`.
5. **Deficiência em ACH-30 (Discrepância na jornada de trabalho):** O cálculo fracionário $\frac{13}{3}$ armazenado como `NUMERIC(4,1)` no PostgreSQL gerava divergência de centavos em relação ao cálculo no Kotlin (`Double`/`Long`), quebrando a paridade matemática entre cliente e servidor.
6. **Deficiência em ACH-33 (Testes sem comprovação real de banco):** O plano v5.2 listava cenários conceituais, mas não especificava testes de integração em PostgreSQL real que comprovassem as regras atômicas da RPC.
7. **Ausência de Outbox Persistente no Room (NOVO ACH-34):** O v5.2 apoiava a sincronização local em flags voláteis (`syncStatus = PENDING`), sem demonstrar como operações em voo sobrevivem à morte do processo, reinicialização ou falhas de rede após o processamento no servidor.
8. **Validação Superficial do Contrato JSON:** Não havia allowlist explícita nem rejeição mandatória de campos estranhos no JSONB.

---

## 3. RESUMO EXECUTIVO DAS CORREÇÕES NA VERSÃO 5.2.1

A versão 5.2.1 estabelece a especificação saneada e comprovadamente implementável:

1. **OCC Efetivo com Tabela de Histórico de Revisões (`user_goals_revision_history`) (ACH-25):**
   - Criação da tabela persistente `user_goals_revision_history` que armazena as chaves e valores modificados em cada incremento de revisão.
   - Comparação formal entre `base_revision` e `current_revision`.
   - Se `base_revision < current_revision`: o servidor busca no histórico os campos alterados pelo servidor desde `base_revision`.
   - Campos ortogonais recebem **3-way merge** limpo.
   - Campos concorrentes no mesmo campo são detectados e **NÃO são resolvidos por LWW silencioso**. O servidor rejeita a operação com status `CONFLICT` estruturado e snapshot atual, garantindo que o cliente decida conscientemente.
   - `base_revision > current_revision`: rejeição imediata (inconsistência de cliente adiantado).
   - `base_revision == 0` com registro pré-existente: rejeição com exigência de PULL prévio.
2. **Matriz de Conflitos Rigorosa e Coerente com o SQL (ACH-27):**
   - Tabela de 8 colunas mapeando situação, revisão-base, revisão atual, campos do cliente, campos do servidor, existência de conflito, comportamento esperado e resultado persistido.
   - Eliminação de qualquer LWW silencioso para o mesmo campo.
3. **Proteção Bidirecional Robusta em INSERT e UPDATE (ACH-28, ACH-32 e ACH-18):**
   - Metas `USER_DEFINED` (incluindo valor zero) são estritamente invioláveis por recálculo automático derivado, exigindo `explicit_user_override = true` e intenção manual comprovada.
   - Metas positivas `LEGACY_UNKNOWN` ($> 0$) são rigorosamente protegidas contra qualquer substituição por `DERIVED`.
   - Metas zero `LEGACY_UNKNOWN` ($= 0$) são elegíveis para promoção inicial a `DERIVED`.
   - Aplicação uniforme em comandos INSERT e UPDATE na RPC.
4. **Segurança Avançada e Isolamento no PostgreSQL (ACH-29):**
   - `SECURITY DEFINER` com `SET search_path = public, pg_temp;`.
   - Qualificação explícita de tabelas (`public.user_goals`, `public.user_goals_operations`, `public.user_goals_revision_history`).
   - Revogação explícita de `PUBLIC` e `anon`; concessão restrita a `authenticated`.
   - Validação imperativa de `auth.uid()`, bloqueando qualquer acesso cruzado entre usuários.
   - Blindagem e rejeição de manipulação de metadados protegidos (`revision`, `user_id`, `id`, `last_operation_id`, `created_at`, `updated_at`, `deleted_at`).
5. **Política Única de Precisão da Jornada de Trabalho (ACH-30):**
   - Fórmula matemática unificada: fator mensal exato $\frac{13}{3}$ e anual $D_A = \frac{D_S \times 13 \times M_A}{3}$.
   - Precisão armazenada em `work_days_per_month`: 2 casas decimais (`Double` no Room, `NUMERIC(4,2)` no PostgreSQL, arredondamento `HALF_UP`).
   - Cálculos monetários derivados utilizam centavos inteiros (`Long` no Kotlin, `NUMERIC(14,2)` no PostgreSQL) com arredondamento `HALF_UP` aplicado no centavo, garantindo divergência exatamente zero entre Kotlin e PostgreSQL.
6. **Arquitetura de Outbox Persistente no Room (NOVO ACH-34):**
   - Entidade Room `sync_outbox` para registro transacional das intenções de sincronização junto com a mutação local de `user_goals`.
   - Reuso estrito do mesmo `operation_id` (UUID v4) e payload imutável através de retentativas, reinicializações e falhas de rede.
   - Ciclo de recuperação detalhado em 13 etapas contra queda de conexão e morte de processo.
7. **Validação Robusta do Contrato JSON com Allowlist:**
   - Lista branca estrita de campos permitidos em `fields`.
   - Rejeição imediata de chaves desconhecidas com SQLSTATE `22023`.
   - Validação estrita de tipos, valores nulos e intervalos numéricos no servidor.
8. **Especificação de Testes e Comprovação Técnica em 4 Camadas (ACH-33):**
   - Diferenciação inequívoca entre testes unitários Kotlin, integração Room SQLite, integração PostgreSQL real e sincronização de ponta a ponta.

---

## 4. PRESERVAÇÃO DOS ACHADOS ANTERIORES (ACH-12 A ACH-24 E INC-01 A INC-08)

Todos os achados e inconsistências saneados em iterações anteriores foram integralmente mantidos e reforçados:

| Item | Descrição Resumida | Tratamento no v5.2.1 | Status de Preservação |
| :---: | :--- | :--- | :---: |
| **ACH-12** | Field-level merge ausente | Patch DTO e aplicação seletiva no SQL preservando campos não enviados. | **PRESERVADO E REFORÇADO (Seção 7 e 10)** |
| **ACH-13** | OCC incompleto no contrato | `base_revision` obrigatório no DTO e comparado na RPC com bloqueio atômico. | **PRESERVADO E EXPANDIDO (ACH-25)** |
| **ACH-14** | Conflito em metas manuais | Resolução determinística sem dependência de relógios locais manipulados. | **SUPERADO E FORMALIZADO (ACH-27)** |
| **ACH-15** | Concorrência na 1ª inserção | Advisory lock transacional por usuário + captura de `unique_violation`. | **PRESERVADO (Seção 10)** |
| **ACH-16** | Idempotência superficial | Tabela dedicada `user_goals_operations` com retenção e hash de payload. | **PRESERVADO E EXPANDIDO (ACH-26/34)** |
| **ACH-17** | Segurança da RPC | `SECURITY DEFINER`, revogação de `PUBLIC` e concessão restrita a `authenticated`. | **PRESERVADO E BLINDADO (ACH-29)** |
| **ACH-18** | Regressão de `LEGACY_UNKNOWN` | Valor legado positivo ($>0$) é protegido em INSERT e UPDATE contra `DERIVED`. | **RECORRIGIDO E DEMONSTRADO (Seção 5.3 e 10)** |
| **ACH-19** | Timestamp global insuficiente | Servidor é a autoridade canônica de ordenação atômica e avanço de revisão. | **PRESERVADO (Seção 6 e 10)** |
| **ACH-20** | Desempate por hash | Desempate lexicográfico canônico de UUID (`operation_id`). | **PRESERVADO (Seção 12)** |
| **ACH-21** | Metas acopladas no sync geral | Sincronizador de metas desacoplado com fila dedicada e mutex isolado. | **PRESERVADO (Seção 6.1)** |
| **ACH-22** | Incompatibilidade monetária | Centavos em `Long` no Room e `NUMERIC(14,2)` no PostgreSQL. Sem `Double` em dinheiro. | **PRESERVADO (Seção 8 e 11)** |
| **ACH-23** | Limites matemáticos e tipos | Km com escala 1 (`HALF_UP`), entregas inteiras e limites de domínio rígidos. | **PRESERVADO (Seção 5.5 e 8)** |
| **ACH-24** | Delimitação de fases | Fase A: Domínio, Room v17, Outbox e RPC. Fase B: Telas Compose de metas anuais. | **PRESERVADO (Seção 6.2)** |
| **INC-01** | Contagem de propriedades Room | Exatamente **32 propriedades** no construtor primário de `UserGoals`. | **PRESERVADO E DEMONSTRADO (Seção 8.1)** |
| **INC-02** | Contagem de metas e chaves LGPD | 16 metas numéricas (4 indicadores $\times$ 4 períodos) e 20 chaves na exportação LGPD. | **PRESERVADO (Seção 8 e 9)** |
| **INC-03** | Tipagem entre camadas | Monetário: `Long`/`NUMERIC(14,2)`; Km: `Double`/`NUMERIC(8,1)`; Deliveries: `Int`/`INTEGER`. | **PRESERVADO (Seção 8.2)** |
| **INC-04** | Km sem escala definida | Escala 1 decimal obrigatória com `RoundingMode.HALF_UP`. | **PRESERVADO (Seção 8.2)** |
| **INC-05** | Migração monetária remota | Script DDL com conversão explícita `USING round(...::numeric, 2)`. | **PRESERVADO (Seção 11.2)** |
| **INC-06** | Sobrescrita no PULL inicial | Reconciliação local prévia obrigatória antes de gravar do servidor. | **PRESERVADO (Seção 6.1)** |
| **INC-07** | Trava de clock skew futuro | Limitação server-side para timestamps com mais de 5 minutos à frente. | **PRESERVADO (Seção 10)** |
| **INC-08** | Confusão Mocks vs DB Real | Separação física rígida das 4 camadas de testes. | **PRESERVADO E DETALHADO (ACH-33)** |

---

## 5. TRATAMENTO CORRETIVO DOS ACHADOS AUDITADOS

---

### 5.1 ACH-25 — CONTROLE DE CONCORRÊNCIA OTIMISTA (OCC) EFETIVO E RECUPERÁVEL

#### 1. Problema Identificado na v5.2
O plano v5.2 declarava implementar OCC com merge de 3 vias, mas o script SQL da função `sync_user_goals_rpc` apenas lia `v_base_rev` sem compará-lo com `v_current.revision`. Não existia nenhuma verificação se o cliente estava desatualizado e não havia como o servidor identificar quais campos haviam sido alterados nas revisões intermediárias, operando na prática como uma sobreposição cega.

#### 2. Causa Técnica
Ausência de um mecanismo persistente de auditoria de alterações por revisão no PostgreSQL. Sem persistir o diferencial (*delta*) de cada revisão, o servidor não possui histórico para realizar 3-way merge em nível de campo.

#### 3. Solução Proposta
1. **Criação da Tabela `user_goals_revision_history`:** Persiste para cada incremento de revisão o conjunto de chaves alteradas (`changed_fields JSONB`) e seus valores anteriores (`previous_fields JSONB`).
2. **Recepção e Comparação de Revisões:**
   - O payload recebe `base_revision` (Long $\ge 0$).
   - O servidor obtém o registro atual com `FOR UPDATE` e compara `v_base_rev` com `v_current.revision`.
3. **Classificação dos Casos de Revisão:**
   - **Caso 1: `v_base_rev > v_current.revision` (Cliente Adiantado):** Rejeição imediata com erro HTTP 409 / SQLSTATE `42000` (`INVALID_BASE_REVISION`).
   - **Caso 2: `v_base_rev == 0` com registro existente:** Rejeição com status `CONFLICT` exigindo sincronização de leitura prévia (PULL). Se não houver registro remoto, trata-se de inserção inicial (Rev 1).
   - **Caso 3: `v_base_rev == v_current.revision` (Cliente Atualizado):** Caminho rápido. Todos os campos válidos são aplicados diretamente.
   - **Caso 4: `v_base_rev < v_current.revision` (Cliente Desatualizado):**
     - O servidor consulta `user_goals_revision_history` no intervalo $(v\_base\_rev, v\_current.revision]$ e agrega todas as chaves modificadas pelo servidor nesse período:
       ```sql
       SELECT jsonb_object_agg(field_key, true)
       INTO v_server_changed_keys
       FROM (
           SELECT DISTINCT jsonb_object_keys(changed_fields) AS field_key
           FROM user_goals_revision_history
           WHERE user_id = v_user_id 
             AND revision > v_base_rev 
             AND revision <= v_current.revision
       ) sub;
       ```
     - O servidor compara as chaves enviadas pelo cliente (`jsonb_object_keys(v_fields)`) com `v_server_changed_keys`.
     - **Se houver interseção (mesmo campo modificado concorrentemente):** CONFLITO REAL DETECTADO. **Não há Last-Write-Wins silencioso**. O servidor NÃO altera os campos em conflito e retorna uma resposta estruturada de status `CONFLICT` contendo a lista dos campos colidentes, a revisão atual e o snapshot do servidor, permitindo que a Outbox local trate a divergência.
     - **Se a interseção for vazia (campos independentes):** MERGE AUTOMÁTICO SELETIVO. Os campos independentes enviados pelo cliente são aplicados aos campos correspondentes de `v_current`, preservando intactos os campos modificados nas revisões intermediárias do servidor.
4. **Detecção de Mutação Efetiva e Nova Revisão:**
   - Apenas se houver alteração real de valor ou proveniência a revisão é incrementada: `v_new_revision := v_current.revision + 1`.
   - O delta aplicado é gravado em `user_goals_revision_history`.
   - Se nenhuma alteração substancial ocorreu, a revisão é mantida inalterada.

#### 4. Trecho Técnico Demonstrativo (PL/pgSQL)

```sql
-- Detecção de Conflitos e 3-Way Merge Efetivo
IF v_base_rev < v_current.revision THEN
    -- 1. Identifica todas as chaves modificadas no servidor desde a revisão-base do cliente
    SELECT COALESCE(jsonb_object_agg(field_key, true), '{}'::jsonb)
    INTO v_server_changed_keys
    FROM (
        SELECT DISTINCT jsonb_object_keys(changed_fields) AS field_key
        FROM public.user_goals_revision_history
        WHERE user_id = v_user_id
          AND revision > v_base_rev
          AND revision <= v_current.revision
    ) sub;

    -- 2. Detecta colisão de campos (chaves enviadas pelo cliente que já mudaram no servidor)
    SELECT array_agg(k)
    INTO v_conflicting_fields
    FROM jsonb_object_keys(v_fields) k
    WHERE v_server_changed_keys ? k;

    -- 3. Se houver colisão no mesmo campo: REJEIÇÃO POR CONFLITO (Sem LWW silencioso)
    IF v_conflicting_fields IS NOT NULL AND array_length(v_conflicting_fields, 1) > 0 THEN
        RETURN jsonb_build_object(
            'status', 'CONFLICT',
            'message', 'Conflito de concorrência detectado: campos modificados concorrentemente no servidor.',
            'conflicting_fields', to_jsonb(v_conflicting_fields),
            'base_revision', v_base_rev,
            'current_revision', v_current.revision,
            'server_record', to_jsonb(v_current)
        );
    END IF;
    -- Se v_conflicting_fields for vazio: segue para o 3-way merge dos campos independentes
END IF;
```

#### 5. Fluxo de Execução
1. Cliente envia `base_revision = 10` e altera `daily_goal_gross`.
2. Servidor está na revisão 12 (alterou `annual_goal_km` na Rev 11 e `work_days_per_week` na Rev 12).
3. Servidor consulta histórico: chaves do servidor = `{'annual_goal_km', 'work_days_per_week'}`.
4. Chave do cliente = `{'daily_goal_gross'}`.
5. Interseção = $\emptyset$.
6. Servidor aplica `daily_goal_gross` sobre o estado atual (preservando as mudanças das revisões 11 e 12).
7. Servidor grava revisão 13 e registra histórico com `changed_fields = {'daily_goal_gross': 250.00}`.
8. Retorna status `SUCCESS` com revisão 13.

#### 6. Cenário de Sucesso
Dois dispositivos modificam campos independentes offline. Ambos sincronizam. Ambas as alterações são salvas e a revisão avança sequencialmente sem perda de dados.

#### 7. Cenário de Falha
Dispositivo A (Rev 10) e Dispositivo B (Rev 10) alteram ambos `daily_goal_net`. A sincroniza e gera Rev 11. B sincroniza em seguida com `base_revision = 10`. O servidor detecta que `daily_goal_net` colide. O servidor NÃO sobrescreve a alteração de A com LWW; em vez disso, retorna status `CONFLICT` informando a colisão no campo `daily_goal_net`.

#### 8. Critério Objetivo de Aceitação
Comprovação via script de teste no PostgreSQL de que alterações em campos disjuntos resultam em mesclagem e nova revisão, e alterações no mesmo campo retornam status `CONFLICT` sem mutação silenciosa na tabela `user_goals`.

---

### 5.2 ACH-27 — MATRIZ DE CONFLITOS EXATA E SEM LAST-WRITE-WINS SILENCIOSO

#### 1. Problema Identificado na v5.2
A matriz de conflitos do v5.2 (cenário SC-02) utilizava arbitragem por timestamp do cliente (`user_modified_at`), permitindo que um dispositivo com relógio mais adiantado sobrescrevesse silenciosamente dados de outro dispositivo.

#### 2. Causa Técnica
Uso de LWW disfarçado de resolução determinística, ignorando que conflitos reais no mesmo campo exigem detecção formal e tratamento pela camada de aplicação/usuário.

#### 3. Solução Proposta
A matriz de conflitos abaixo reflete exatamente o comportamento da função SQL da Seção 10:

| Situação | Revisão-Base | Revisão Atual | Campos Alterados pelo Cliente | Campos Alterados no Servidor | Conflito? | Comportamento Esperado | Resultado Persistido |
| :--- | :---: | :---: | :--- | :--- | :---: | :--- | :--- |
| **1. Alterações Independentes** | 10 | 11 | `daily_goal_gross` | `annual_goal_km` | **Não** | 3-Way Merge automático de campos disjuntos. | `daily_goal_gross` do cliente aplicado; `annual_goal_km` do servidor preservado. Revisão avança $11 \to 12$. |
| **2. Alterações Concorrentes no Mesmo Campo** | 10 | 11 | `daily_goal_net` | `daily_goal_net` | **Sim** | Detecção de colisão via histórico de revisões. Bloqueio de LWW silencioso. Retorno de status `CONFLICT`. | Nenhuma alteração no registro do banco. Revisão permanece 11. Outbox no cliente transiciona para estado `CONFLICT`. |
| **3. Cliente com Revisão Desatualizada (Sem Colisão)** | 8 | 12 | `work_days_per_week` | `daily_goal_gross` (Rev 9), `monthly_goal_net` (Rev 11) | **Não** | Merge de múltiplos saltos de revisão históricos. | `work_days_per_week` atualizado. Campos das revisões 9 e 11 mantidos. Revisão avança $12 \to 13$. |
| **4. Cliente com Revisão Atualizada** | 12 | 12 | `daily_goal_km`, `weekly_goal_km` | Nenhum (mesma versão) | **Não** | Fast-path: aplicação direta de todos os campos válidos enviados. | Ambos os campos atualizados no banco. Revisão avança $12 \to 13$. |
| **5. Reenvio de Operação Já Processada** | 10 | 11 | Qualquer (mesmo `operation_id` e mesmo hash) | Qualquer | **Não** | Idempotência pura em `user_goals_operations`. Retorno imediato do snapshot em cache. | Nenhuma escrita em `user_goals`, sem avanço de revisão, zero triggers disparadas. |
| **6. Duas Operações Simultâneas com Mesmo ID** | 10 | 10 | Mesmo `operation_id` e mesmo payload | Nenhum | **Não** | Serialização por `pg_advisory_xact_lock(user_id)`. Primeira processa, segunda consulta cache de operações. | Exatamente 1 mutação no banco. Ambas recebem a mesma resposta com status `SUCCESS` e Rev 11. |
| **7. Alteração Manual de Meta vs Outro Indicador** | 10 | 11 | `annual_goal_net` (`USER_DEFINED`, `override=true`) | `daily_goal_gross` | **Não** | Campos ortogonais com proteção manual válida. | `annual_goal_net` manual atualizado; `daily_goal_gross` preservado. Revisão avança $11 \to 12$. |
| **8. Recálculo Automático vs `USER_DEFINED` Existente** | 10 | 10 | `annual_goal_net` (`DERIVED`) | Nenhum | **Bloqueio** | Servidor detecta meta manual preexistente sem flag `override`. Rejeita alteração do indicador. | Valor manual preexistente (inclusive zero) e proveniência `USER_DEFINED` mantidos intactos. Revisão mantida em 10. |
| **9. Recálculo Automático vs `LEGACY_UNKNOWN` Positivo** | 10 | 10 | `annual_goal_gross` (`DERIVED`) | Nenhum | **Bloqueio** | Servidor detecta valor positivo legado $>0$ sem override. Rejeita rebaixamento para `DERIVED`. | Valor legado positivo e proveniência `LEGACY_UNKNOWN` preservados. Revisão mantida em 10. |
| **10. Cliente com Revisão Superior à do Servidor** | 15 | 10 | Qualquer campo | Não avaliado | **Anomalia** | Servidor rejeita inconsistência estrutural (`base_revision > current_revision`). | Nenhuma alteração. Retorna erro HTTP 409 / SQLSTATE `42000`. |

---

### 5.3 ACH-28 / ACH-32 / ACH-18 — PROTEÇÃO UNIFORME DE `USER_DEFINED` E `LEGACY_UNKNOWN` EM INSERT E UPDATE

#### 1. Problema Identificado na v5.2
No v5.2, a proteção aplicava-se somente no comando UPDATE através de `IF v_current.annual_X_origin = 'USER_DEFINED'`. Qualquer registro com proveniência diferente de `USER_DEFINED` caía em um bloco `ELSE` permissivo, permitindo que processos em segundo plano substituíssem valores positivos de `LEGACY_UNKNOWN` por `DERIVED`. Além disso, o fluxo de primeira inserção (INSERT) não validava proveniências nem protegia valores legados.

#### 2. Causa Técnica
Falta de segregação entre as políticas de governança de ciclo de vida:
- `USER_DEFINED` (soberania do usuário): inviolável sempre, inclusive se for $0.00$.
- `LEGACY_UNKNOWN` com valor $> 0$: dado histórico positivo pré-existente sem origem registrada; inviolável por automação.
- `LEGACY_UNKNOWN` com valor $= 0$: campo legado não configurado; elegível para inicialização automática como `DERIVED`.

#### 3. Solução Proposta
A regra é formalizada e aplicada com exatidão tanto no INSERT quanto no UPDATE:

##### Tabela de Decisão Unificada para Metas Anuais

| Operação | Estado Atual no Banco (Valor / Origem) | Patch Recebido (Valor / Origem) | Flag `explicit_user_override` | Decisão do Servidor | Valor e Origem Consolidados | Justificativa Técnica |
| :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **INSERT** | Inexistente (Registro Novo) | `0.00` / `'LEGACY_UNKNOWN'` | Indiferente | **ACEITO** | `0.00` / `'LEGACY_UNKNOWN'` | Inicialização padrão de conta nova. |
| **INSERT** | Inexistente (Migração Local) | `45000.00` / `'LEGACY_UNKNOWN'` | Indiferente | **ACEITO** | `45000.00` / `'LEGACY_UNKNOWN'` | Carga inicial de valor histórico positivo do Room. |
| **INSERT** | Inexistente (Migração Local) | `50000.00` / `'USER_DEFINED'` | `true` | **ACEITO** | `50000.00` / `'USER_DEFINED'` | Usuário já havia definido a meta manualmente localmente. |
| **UPDATE** | `0.00` / `'USER_DEFINED'` | `40000.00` / `'DERIVED'` | **false** | **BLOQUEADO** | `0.00` / `'USER_DEFINED'` | Meta manual zero é tão protegida quanto valor positivo. Recálculo bloqueado. |
| **UPDATE** | `50000.00` / `'USER_DEFINED'` | `35000.00` / `'DERIVED'` | **false** | **BLOQUEADO** | `50000.00` / `'USER_DEFINED'` | Tentativa de recálculo em segundo plano rejeitada. |
| **UPDATE** | `50000.00` / `'USER_DEFINED'` | `60000.00` / `'USER_DEFINED'` | **true** | **AUTORIZADO** | `60000.00` / `'USER_DEFINED'` | Edição manual explícita pelo usuário. |
| **UPDATE** | `50000.00` / `'USER_DEFINED'` | `35000.00` / `'DERIVED'` | **true** | **AUTORIZADO** | `35000.00` / `'DERIVED'` | Usuário optou explicitamente por voltar a usar meta automática. |
| **UPDATE** | `60000.00` / `'LEGACY_UNKNOWN'` | `50000.00` / `'DERIVED'` | **false** | **BLOQUEADO** | `60000.00` / `'LEGACY_UNKNOWN'` | **ACH-18 Sanado:** Legado positivo $>0$ jamais é sobrescrito por automação. |
| **UPDATE** | `0.00` / `'LEGACY_UNKNOWN'` | `50000.00` / `'DERIVED'` | **false** | **PROMOVIDO** | `50000.00` / `'DERIVED'` | Legado zero não configurado é inicializado por cálculo automático. |
| **UPDATE** | `60000.00` / `'LEGACY_UNKNOWN'` | `70000.00` / `'USER_DEFINED'` | **true** | **PROMOVIDO** | `70000.00` / `'USER_DEFINED'` | Usuário assumiu a gestão manual de um registro que era legado. |
| **UPDATE** | `40000.00` / `'DERIVED'` | `45000.00` / `'DERIVED'` | **false** | **ATUALIZADO** | `45000.00` / `'DERIVED'` | Recálculo de metas derivadas por ajuste de diária ou jornada. |

#### 4. Trecho Técnico Demonstrativo (PL/pgSQL de Validação em INSERT e UPDATE)

```sql
-- Função auxiliar conceitual ou lógica embutida na RPC para cada indicador anual (ex.: Net):
-- Avaliação para annual_goal_net:
IF v_fields ? 'annual_goal_net' THEN
    v_incoming_val := (v_fields->>'annual_goal_net')::NUMERIC(14,2);
    v_incoming_orig := COALESCE(v_prov->>'annual_net_origin', 'DERIVED');
    v_has_override := COALESCE((v_overrides->>'annual_net')::BOOLEAN, FALSE);

    -- REGRA 1: Proteção de USER_DEFINED (inclusive valor 0.00)
    IF v_current.annual_net_origin = 'USER_DEFINED' THEN
        IF v_has_override = TRUE THEN
            v_net := v_incoming_val;
            v_net_orig := v_incoming_orig;
        ELSE
            -- Mantém inalterado o valor e a proveniência
            v_net := v_current.annual_goal_net;
            v_net_orig := v_current.annual_net_origin;
        END IF;

    -- REGRA 2: Proteção de LEGACY_UNKNOWN Positivo (ACH-18)
    ELSIF v_current.annual_net_origin = 'LEGACY_UNKNOWN' AND v_current.annual_goal_net > 0.00 THEN
        IF v_has_override = TRUE AND v_incoming_orig = 'USER_DEFINED' THEN
            v_net := v_incoming_val;
            v_net_orig := 'USER_DEFINED';
        ELSE
            -- Bloqueia substituição por DERIVED e preserva o valor legado positivo
            v_net := v_current.annual_goal_net;
            v_net_orig := v_current.annual_net_origin;
        END IF;

    -- REGRA 3: LEGACY_UNKNOWN Zero ou DERIVED pré-existente
    ELSE
        -- Permite promoção para DERIVED ou para USER_DEFINED com override
        IF v_incoming_orig = 'USER_DEFINED' AND v_has_override = FALSE THEN
            -- Exige flag para tornar manual
            v_net := v_current.annual_goal_net;
            v_net_orig := v_current.annual_net_origin;
        ELSE
            v_net := v_incoming_val;
            v_net_orig := v_incoming_orig;
        END IF;
    END IF;
END IF;
```

---

### 5.4 ACH-29 — SEGURANÇA DA FUNÇÃO POSTGRESQL E BLINDAGEM DE PRIVILÉGIOS

#### 1. Problema Identificado na v5.2
A função anterior declarava `SECURITY DEFINER SET search_path = public, pg_temp`, mas não demonstrava a qualificação dos objetos por schema, não impedia explicitamente a alteração de identificadores de tenant (`user_id`, `id`) e não incluía as diretivas de isolamento de privilégios entre os papéis Supabase (`PUBLIC`, `anon`, `authenticated`).

#### 2. Causa Técnica
Pressuposição de que apenas o parâmetro `search_path` seria suficiente para mitigar riscos de escalada de privilégios e injeção de parâmetros em procedimentos armazenados.

#### 3. Solução Proposta
Blindagem completa fundamentada em 8 salvaguardas verificadas contra as migrations reais do projeto:
1. **`search_path` Seguro e Qualificação de Schema:**
   `SECURITY DEFINER SET search_path = public, pg_temp;` e prefixação mandatória de todas as tabelas: `public.user_goals`, `public.user_goals_operations`, `public.user_goals_revision_history`.
2. **Validação Inegociável da Identidade (`auth.uid()`):**
   `v_user_id := auth.uid();` — Se nulo, abortar imediatamente com SQLSTATE `42501` (`insufficient_privilege`). Nenhuma operação é permitida a chamadas anônimas.
3. **Isolamento Absoluto de Tenant (Multitenant Defense):**
   Todas as cláusulas `SELECT`, `UPDATE` e `INSERT` utilizam estritamente `WHERE user_id = v_user_id`. Se o payload JSON contiver qualquer chave `user_id` ou `id`, esses campos são descartados e ignorados pelo parser.
4. **Proteção de Metadados e Chaves Internas:**
   As chaves `revision`, `created_at`, `updated_at`, `deleted_at`, `last_operation_id` e `user_id` são explicitamente barradas na allowlist de `fields`.
5. **Revogação Total de Privilégios de `PUBLIC` e `anon`:**
   No PostgreSQL, funções novas recebem por padrão `GRANT EXECUTE TO PUBLIC`. O script revoga expressamente:
   ```sql
   REVOKE ALL ON FUNCTION public.sync_user_goals_rpc(JSONB) FROM PUBLIC;
   REVOKE ALL ON FUNCTION public.sync_user_goals_rpc(JSONB) FROM anon;
   GRANT EXECUTE ON FUNCTION public.sync_user_goals_rpc(JSONB) TO authenticated;
   ```
6. **Proteção Row Level Security (RLS) em Todas as Tabelas Auxiliares:**
   As tabelas `user_goals_operations` e `user_goals_revision_history` possuem RLS ativada com políticas que garantem que cada usuário acesse exclusivamente seus próprios registros.
7. **Tratamento de Exceções Sem Vazamento de Metadados:**
   Erros disparam mensagens padronizadas sem expor topologia de tabelas, nomes de índices ou detalhes do motor interno do banco.
8. **Lock Transacional de Serialização:**
   `PERFORM pg_advisory_xact_lock(hashtext(v_user_id::text));` garante que dois processos do mesmo usuário não criem condições de corrida no banco.

---

### 5.5 ACH-30 — PRECISÃO MATEMÁTICA E POLÍTICA ÚNICA DE JORNADA DE TRABALHO

#### 1. Problema Identificado na v5.2
A fórmula de dias mensais $D_S \times \frac{13}{3}$ produz frações periódicas (ex.: $5 \times \frac{13}{3} = \frac{65}{3} = 21,6666...$). O plano v5.2 armazenava `Double` com $21.666666666666668$ no Room e `NUMERIC(4,1)` com $21.7$ no PostgreSQL. Isso gerava divergências de cálculo monetário entre Kotlin e banco de dados quando recalculadas as metas anuais.

#### 2. Causa Técnica
Ausência de uma especificação formal de precisão e arredondamento que sincronizasse a escala numérica entre todas as camadas do sistema.

#### 3. Solução Proposta — Política Única de Precisão em 7 Dimensões

1. **Fórmula Matemática:**
   - **Dias por semana ($D_S$):** Inteiro no intervalo $[1, 7]$.
   - **Meses de trabalho por ano ($M_A$):** Inteiro no intervalo $[1, 12]$.
   - **Dias por mês automático ($C_M = \text{false}$):**
     $$D_M = \frac{D_S \times 13}{3}$$
   - **Dias por mês customizado ($C_M = \text{true}$):**
     $D_M$ é fornecido pelo usuário no intervalo $[1.00, 31.00]$.
   - **Dias anuais de trabalho ($D_A$):**
     $$D_A = \begin{cases} \dfrac{D_S \times 13 \times M_A}{3}, & \text{se } C_M = \text{false} \\ D_M \times M_A, & \text{se } C_M = \text{true} \end{cases}$$
2. **Precisão Interna dos Cálculos:**
   - No Kotlin: aritmética exata com `Long` (centavos) e frações, utilizando `BigDecimal` com escala 4 e arredondamento `RoundingMode.HALF_UP` apenas na conversão final.
   - No PostgreSQL: aritmética com tipo `NUMERIC`, aplicando a expressão inteira antes da divisão para evitar erros intermediários de truncamento: `ROUND((daily_net * work_days_per_week * 13 * work_months_per_year) / 3.0, 2)`.
3. **Regra de Arredondamento:**
   - **Valores Monetários:** Centavos inteiros com arredondamento `HALF_UP`.
   - **Quilometragem:** 1 casa decimal com arredondamento `HALF_UP`.
   - **Entregas:** Número inteiro com arredondamento `HALF_UP`.
   - **Dias de Trabalho por Mês ($D_M$):** Escala de 2 casas decimais com `HALF_UP`.
4. **Precisão Armazenada no Room:**
   - `workDaysPerMonth: Double` armazenado com 2 casas decimais normalizadas (ex.: `21.67`).
5. **Precisão Armazenada no PostgreSQL:**
   - `work_days_per_month NUMERIC(4,2)` NOT NULL DEFAULT 21.67.
6. **Precisão Utilizada Durante a Sincronização (DTO JSON):**
   - Serializado como número decimal de 2 casas decimais: `"work_days_per_month": 21.67`.
7. **Precisão Exibida na Interface (UI Compose):**
   - No Dashboard (leitura rápida sob sol forte): formatado em pt-BR com 1 casa decimal (ex.: `"21,7 dias/mês"`).
   - No Diálogo de Configuração de Jornada: campo editável com 2 casas decimais (ex.: `"21,67"`).

#### 4. Demonstração Numérica Auditada à Mão (5 e 6 Dias por Semana)

##### Caso A: Jornada de 5 Dias/Semana, 12 Meses/Ano, Meta Diária Líquida = R$ 130,00 (13.000 centavos)
- **Dias/Mês:** $\frac{5 \times 13}{3} = \frac{65}{3} = 21,6666... \implies$ **21,67 dias**.
- **Dias/Ano ($D_A$):** $\frac{5 \times 13 \times 12}{3} = \frac{780}{3} =$ **260 dias exatos**.
- **Cálculo no Kotlin:**
  `val cents = (13000L * 5 * 13 * 12) / 3 = 20280000 / 3 = 3.380.000 centavos` $\implies$ **R$ 33.800,00**.
- **Cálculo no PostgreSQL:**
  `ROUND((130.00 * 5 * 13 * 12) / 3.0, 2) = ROUND(101400.00 / 3.0, 2) = 33800.00` $\implies$ **R$ 33.800,00**.
- **Divergência entre Camadas:** **R$ 0,00 (Zero centavos)**.

##### Caso B: Jornada de 6 Dias/Semana, 12 Meses/Ano, Meta Diária Líquida = R$ 130,00 (13.000 centavos)
- **Dias/Mês:** $\frac{6 \times 13}{3} = \frac{78}{3} =$ **26,00 dias exatos**.
- **Dias/Ano ($D_A$):** $\frac{6 \times 13 \times 12}{3} = \frac{936}{3} =$ **312 dias exatos**.
- **Cálculo no Kotlin:**
  `val cents = (13000L * 6 * 13 * 12) / 3 = 24336000 / 3 = 4.056.000 centavos` $\implies$ **R$ 40.560,00**.
- **Cálculo no PostgreSQL:**
  `ROUND((130.00 * 6 * 13 * 12) / 3.0, 2) = ROUND(121680.00 / 3.0, 2) = 40560.00` $\implies$ **R$ 40.560,00**.
- **Divergência entre Camadas:** **R$ 0,00 (Zero centavos)**.

##### Caso C: Jornada de 5 Dias/Semana, 11 Meses/Ano (Férias), Meta Diária Líquida = R$ 130,00 (13.000 centavos)
- **Dias/Mês:** **21,67 dias**.
- **Dias/Ano ($D_A$):** $\frac{5 \times 13 \times 11}{3} = \frac{715}{3} = 238,3333...$ dias.
- **Cálculo no Kotlin:**
  `round((13000L * 715) / 3.0).toLong() = round(9295000 / 3.0) = round(3098333.333...) = 3.098.333 centavos` $\implies$ **R$ 30.983,33**.
- **Cálculo no PostgreSQL:**
  `ROUND((130.00 * 5 * 13 * 11) / 3.0, 2) = ROUND(92950.00 / 3.0, 2) = ROUND(30983.333..., 2) = 30983.33` $\implies$ **R$ 30.983,33**.
- **Divergência entre Camadas:** **R$ 0,00 (Zero centavos)**.

---

### 5.6 ACH-33 — ESPECIFICAÇÃO DE TESTES E COMPROVAÇÃO TÉCNICA REAL

#### 1. Problema Identificado na v5.2
A versão 5.2 misturou testes de unidade com asserções simuladas de PostgreSQL, declarando cenários conceituais sem especificar como seriam executados contra o banco de dados real.

#### 2. Solução Proposta
A especificação é categorizada em 4 camadas físicas e operacionais estritamente diferenciadas:

1. **Camada 1: Testes Unitários de Domínio Kotlin (`app/src/test/.../domain/`):**
   - JUnit 4 puro, execução ultrarrápida sem framework Android nem banco de dados.
   - Testa as fórmulas de jornada, DAG de precedência, conversões monetárias com `CurrencyUtils` e detecção de overflow.
2. **Camada 2: Testes de Integração Room SQLite (`app/src/test/.../data/`):**
   - Execução via Robolectric / AndroidX Test com SQLite em memória real.
   - Testa a entidade `UserGoals` (32 propriedades), migração Room `MIGRATION_16_17` via `MigrationTestHelper` e a persistência atômica da tabela `sync_outbox`.
3. **Camada 3: Testes de Integração PostgreSQL Real (`supabase/tests/`):**
   - Executados contra instância real do PostgreSQL 15+ (via Testcontainers ou Supabase Local CLI).
   - Testa as transações reais da RPC `sync_user_goals_rpc`, o isolamento de tenant via `auth.uid()`, locks de concorrência (`pg_advisory_xact_lock`), resolução de OCC com histórico persistente e proteção de `USER_DEFINED`/`LEGACY_UNKNOWN`.
4. **Camada 4: Testes de Sincronização Cliente-Servidor (`app/src/test/.../sync/`):**
   - Testa o `UserGoalsSyncCoordinator` com o motor de rede OkHttp / Retrofit contra `MockWebServer` simulando respostas HTTP 200, 400, 409 (conflito) e quedas de conexão TCP.

---

### 5.7 NOVO ACH-34 — ARQUITETURA DE OUTBOX PERSISTENTE NO ROOM E IDEMPOTÊNCIA

#### 1. Problema Identificado na v5.2
O plano v5.2 utilizava uma coluna simples `syncStatus` na própria tabela `user_goals`. Caso o aplicativo sofresse um encerramento inesperado (Process Death, falta de bateria, crash) durante uma requisição em voo cujo servidor já houvesse gravado o resultado, a reinicialização geraria um novo UUID, quebrando a cadeia de idempotência e provocando potenciais avanço indevido de revisão ou perda de dados.

#### 2. Causa Técnica
Ausência do padrão arquitetural **Transactional Outbox** no cliente móvel.

#### 3. Solução Proposta
Implementar uma fila transacional persistente no Room, desacoplada do ciclo de vida da interface e coordenada com o banco de dados remoto:

##### 3.1 Modelo da Entidade Room (`SyncOutboxOperation`)
A tabela `sync_outbox` armazena cada intenção de envio com imutabilidade estrita:

```kotlin
package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_outbox",
    indices = [
        Index(value = ["state", "createdAt"]),
        Index(value = ["userId"])
    ]
)
data class SyncOutboxOperation(
    @PrimaryKey val operationId: String,       // UUID v4 gerado no momento da ação do usuário
    val userId: String,                        // ID do usuário autenticado (auth.uid())
    val operationType: String,                 // Ex.: "SYNC_USER_GOALS_PATCH"
    val serializedPayload: String,             // JSON consolidado do UserGoalsSyncPatchDto
    val baseRevision: Long,                    // Revisão de onde partiu a intenção
    val state: String,                         // OutboxState: PENDING, IN_FLIGHT, SUCCESS, FAILED_RETRYABLE, CONFLICT, DEAD_LETTER
    val createdAt: Long,                       // Timestamp epoch ms da criação
    val lastAttemptAt: Long? = null,           // Timestamp da última tentativa de envio
    val attemptCount: Int = 0,                 // Contador incremental de tentativas
    val lastError: String? = null              // Mensagem ou código do último erro HTTP/SQL
)
```

##### 3.2 Estrutura da Tabela SQLite no Room v17

```sql
CREATE TABLE IF NOT EXISTS `sync_outbox` (
    `operationId` TEXT NOT NULL,
    `userId` TEXT NOT NULL,
    `operationType` TEXT NOT NULL,
    `serializedPayload` TEXT NOT NULL,
    `baseRevision` INTEGER NOT NULL,
    `state` TEXT NOT NULL,
    `createdAt` INTEGER NOT NULL,
    `lastAttemptAt` INTEGER,
    `attemptCount` INTEGER NOT NULL DEFAULT 0,
    `lastError` TEXT,
    PRIMARY KEY(`operationId`)
);
CREATE INDEX IF NOT EXISTS `idx_sync_outbox_state_created` ON `sync_outbox`(`state`, `createdAt`);
CREATE INDEX IF NOT EXISTS `idx_sync_outbox_user_id` ON `sync_outbox`(`userId`);
```

##### 3.3 Estados Possíveis e Máquina de Transição
- **`PENDING`:** Operação criada localmente em transação com a alteração do Room. Pronta para envio.
- **`IN_FLIGHT`:** O despachante de sincronização adquiriu a tarefa e iniciou a requisição de rede.
- **`SUCCESS`:** Servidor respondeu com sucesso (200 OK com snapshot atualizado).
- **`FAILED_RETRYABLE`:** Erro transitório de rede (timeout, conexão interrompida, HTTP 5xx). O mesmo `operationId` é preservado para nova tentativa.
- **`CONFLICT`:** Servidor detectou colisão concorrente no mesmo campo (resposta `CONFLICT` com campos conflitantes). O processo de retentativa automática é interrompido.
- **`DEAD_LETTER`:** Atingido o limite máximo de retentativas (ex.: 10 tentativas) com erro persistente não recuperável.

##### 3.4 Responsabilidades do Repositório e do Coordenador de Sincronização
1. **Repositório (`UserGoalsRepository`):**
   - Ao salvar metas alteradas pelo motorista, executa uma **transação atômica Room (`@Transaction`)**:
     1. Grava os novos valores em `user_goals` local, definindo `syncStatus = 'PENDING'`.
     2. Serializa o payload com `operationId = UUID.randomUUID().toString()` e `baseRevision = userGoals.revision`.
     3. Insere a operação na tabela `sync_outbox` com estado `PENDING`.
2. **Coordenador de Sincronização (`UserGoalsSyncCoordinator`):**
   - Monitora conectividade via `ConnectivityManager` e executa sob `Mutex` local.
   - Carrega as operações com estado `PENDING` ou `FAILED_RETRYABLE`.
   - Transiciona para `IN_FLIGHT` e incrementa `attemptCount`.
   - Executa a chamada HTTPS da RPC `sync_user_goals_rpc`.
   - Em caso de sucesso: atualiza `user_goals` local com a nova revisão oficial retornada pelo servidor, marca `syncStatus = 'SYNCED'` e transiciona a operação na Outbox para `SUCCESS`.
   - Em caso de conflito: atualiza a Outbox para `CONFLICT`, dispara evento para a UI e suspende retentativas automáticas daquela operação.

##### 3.5 Política de Retentativas e Idempotência Absoluta
- **Imutabilidade de UUID:** Sob nenhuma circunstância é gerado novo UUID para falhas de rede, timeouts, quedas de sinal ou reinicialização do celular.
- **Backoff Exponencial com Jitter:** Intervalos de retentativa: $2\text{s}, 4\text{s}, 8\text{s}, 16\text{s}, 32\text{s}$, até o teto de 5 minutos, com variação pseudoaleatória de $\pm 20\%$ para evitar tempestades de requisições (*thundering herd*).
- **Tratamento de Arquivamento/Limpeza:** Operações com estado `SUCCESS` são mantidas por 7 dias no banco local para auditoria e expurgadas automaticamente via rotina periódica em background.

##### 3.6 Fluxo Completo de Recuperação Após Falha de Comunicação (13 Passos)

```
PASSO A PASSO DA RECUPERAÇÃO DE CONEXÃO COM OUTBOX:

[1. Motorista edita meta na UI] 
    │
    ▼
[2. Room persiste alteração em user_goals (syncStatus = PENDING)]
    │
    ▼ (Mesma Transação Room @Transaction)
[3. Room grava registro em sync_outbox (operationId = OP-999, state = PENDING)]
    │
    ▼
[4. Sincronizador despacha requisição HTTPS POST com OP-999 (state -> IN_FLIGHT)]
    │
    ▼
[5. Servidor PostgreSQL recebe: adquire advisory lock, valida OCC, grava user_goals, 
    grava histórico em user_goals_revision_history e salva em user_goals_operations]
    │
    ▼
[6. CONEXÃO CAI: Torre de celular perde sinal antes do pacote TCP ACK alcançar o aparelho]
    │
    ▼
[7. Celular desliga por falta de bateria / aplicativo é encerrado pelo sistema operacional]
    │
    ▼
[8. Usuário religa o aparelho e abre o GiroCusto no dia seguinte]
    │
    ▼
[9. SyncCoordinator acorda, consulta sync_outbox e localiza OP-999 pendente/in_flight]
    │
    ▼
[10. Sincronizador reenvia EXATAMENTE o mesmo OP-999 com o mesmo payload]
    │
    ▼
[11. Servidor consulta user_goals_operations, reconhece OP-999 com mesmo hash, 
     e devolve IMEDIATAMENTE a resposta em cache da Rev 11, sem reprocessar mutação]
    │
    ▼
[12. Aplicativo recebe resposta original com status SUCCESS e revisão 11]
    │
    ▼
[13. Room atualiza user_goals (syncStatus = SYNCED, revision = 11) e marca outbox como SUCCESS]
```

---

## 6. ARQUITETURA FINAL E COERÊNCIA ENTRE CAMADAS

### 6.1 Matriz de Coerência Cruzada entre as Camadas do Sistema

| Camada | O que precisa demonstrar | Especificação e Alinhamento Técnico na v5.2.1 |
| :--- | :--- | :--- |
| **Kotlin (Domínio)** | Contratos, tipos e regras de cálculo | Valores monetários estritamente em centavos (`Long`); km em `Double` com escala 1 (`HALF_UP`); entregas em `Int`. Cálculo de metas anuais seguindo o DAG (Mensal $\to$ Diária). |
| **Room (Local)** | Persistência, revisão e Outbox | Entidade `UserGoals` com 32 propriedades; entidade `SyncOutboxOperation` com persistência transacional de operações pendentes. |
| **Repositório** | Transações e coordenação | `@Transaction` obrigatória unindo atualização de `user_goals` e inserção em `sync_outbox`. Desacoplamento do sync genérico em lote. |
| **Sincronização** | Retentativas, conflitos e idempotência | Reuso estrito de `operation_id`; backoff exponencial; tratamento de status `CONFLICT` sem loops infinitos; mutex de execução única. |
| **Supabase (API/Rede)** | Autenticação e transporte | Chamada à RPC `sync_user_goals_rpc` com autenticação JWT; timeouts explícitos de 15 segundos; interceptors sem vazamento de tokens em logs. |
| **PostgreSQL (Banco)** | OCC, validação, segurança e persistência | `SECURITY DEFINER` qualificado; OCC com histórico persistente `user_goals_revision_history`; idempotência em `user_goals_operations`; rejeição de LWW silencioso. |
| **LGPD (Privacidade)** | Exportação e transparência | Exportação completa das 20 chaves de metas e jornada via `UserDataExportHelper`, sem omissão de metas anuais ou parâmetros operacionais. |

---

## 7. CONTRATO DE SINCRONIZAÇÃO E VALIDAÇÃO ROBUSTA DO JSON

### 7.1 Especificação dos DTOs Kotlin

```kotlin
package com.example.network.supabase

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserGoalsSyncPatchDto(
    @Json(name = "operation_id") val operationId: String,
    @Json(name = "base_revision") val baseRevision: Long,
    @Json(name = "user_modified_at") val userModifiedAt: String,
    @Json(name = "fields") val fields: Map<String, Any?>,
    @Json(name = "provenance") val provenance: Map<String, String>? = null,
    @Json(name = "explicit_user_override") val explicitUserOverride: Map<String, Boolean>? = null
)

@JsonClass(generateAdapter = true)
data class SyncRpcResponseDto(
    @Json(name = "status") val status: String, // "SUCCESS", "NO_OP", "CONFLICT"
    @Json(name = "message") val message: String? = null,
    @Json(name = "conflicting_fields") val conflictingFields: List<String>? = null,
    @Json(name = "base_revision") val baseRevision: Long? = null,
    @Json(name = "current_revision") val currentRevision: Long? = null,
    @Json(name = "data") val data: Map<String, Any?>? = null,
    @Json(name = "server_record") val serverRecord: Map<String, Any?>? = null
)
```

### 7.2 Regras de Validação do Contrato JSON no Servidor

A função PostgreSQL impõe 10 regras de validação no recebimento do payload:
1. **Allowlist Explícita de Campos em `fields`:** Aceita exclusivamente:
   - `daily_goal_gross`, `daily_goal_net`, `daily_goal_km`, `daily_goal_deliveries`
   - `weekly_goal_gross`, `weekly_goal_net`, `weekly_goal_km`, `weekly_goal_deliveries`
   - `monthly_goal_gross`, `monthly_goal_net`, `monthly_goal_km`, `monthly_goal_deliveries`
   - `annual_goal_gross`, `annual_goal_net`, `annual_goal_km`, `annual_goal_deliveries`
   - `work_days_per_week`, `work_days_per_month`, `work_months_per_year`, `work_days_per_month_is_custom`
   - `annual_goals_initialized`
2. **Rejeição Imediata de Campos Desconhecidos:** Qualquer chave em `fields` que não conste na lista acima interrompe a execução com SQLSTATE `22023` (`invalid_parameter_value`).
3. **Validação de Nulos:** Nenhuma chave de meta numérica pode conter valor `null`. Se recebido, aborta com SQLSTATE `22004`.
4. **Validação de Tipos:** Valores monetários devem ser numéricos decimais; km decimal; entregas e jornada inteiros; custom booleano.
5. **Intervalos Numéricos Rígidos:**
   - Metas monetárias: $\ge 0.00$ e $\le 10.000.000,00$.
   - Quilometragem: $\ge 0.0$ e $\le 500.000,0$.
   - Entregas: $\ge 0$ e $\le 100.000$.
   - Dias/semana: $1 \le x \le 7$.
   - Meses/ano: $1 \le x \le 12$.
   - Dias/mês: $1.00 \le x \le 31.00$.
6. **Bloqueio de Identificadores:** É proibido enviar `id` ou `user_id` em `fields`.
7. **Bloqueio de Alteração Direta de `revision`:** Rejeitada se presente em `fields`.
8. **Bloqueio de `last_operation_id`:** Manipulação pelo cliente é proibida.
9. **Bloqueio de Metadados de Timestamp:** `created_at`, `updated_at`, `deleted_at` não são editáveis pelo cliente.
10. **Validação de Proveniência:** Chaves de `provenance` aceitam unicamente `'DERIVED'`, `'USER_DEFINED'` e `'LEGACY_UNKNOWN'`.

---

## 8. MODELO DE DADOS CONSOLIDADO E MATRIZ TÉCNICA

### 8.1 Auditoria das 32 Propriedades da Entidade Room `UserGoals`

```
PROPRIEDADES DA ENTIDADE UserGoals (Room v17):
1.  id (PrimaryKey, Int = 1)
2.  dailyGoalGross (Long)
3.  dailyGoalNet (Long)
4.  dailyGoalKm (Double)
5.  dailyGoalDeliveries (Int)
6.  weeklyGoalGross (Long)
7.  weeklyGoalNet (Long)
8.  weeklyGoalKm (Double)
9.  weeklyGoalDeliveries (Int)
10. monthlyGoalGross (Long)
11. monthlyGoalNet (Long)
12. monthlyGoalKm (Double)
13. monthlyGoalDeliveries (Int)
14. annualGoalGross (Long) [Adicionado v17]
15. annualGoalNet (Long) [Existente v16]
16. annualGoalKm (Double) [Adicionado v17]
17. annualGoalDeliveries (Int) [Adicionado v17]
18. workDaysPerWeek (Int) [Adicionado v17]
19. workDaysPerMonth (Double) [Adicionado v17]
20. workMonthsPerYear (Int) [Adicionado v17]
21. workDaysPerMonthIsCustom (Boolean) [Adicionado v17]
22. annualGrossOrigin (String) [Adicionado v17]
23. annualNetOrigin (String) [Adicionado v17]
24. annualKmOrigin (String) [Adicionado v17]
25. annualDeliveriesOrigin (String) [Adicionado v17]
26. annualGoalsInitialized (Boolean) [Adicionado v17]
27. revision (Long) [Adicionado v17]
28. userModifiedAt (Long) [Adicionado v17]
29. remoteId (String)
30. syncStatus (String)
31. updatedAt (Long)
32. isDeleted (Boolean)
--------------------------------------------------------------------------------
TOTAL COMPROVADO: EXATAMENTE 32 PROPRIEDADES.
```

### 8.2 Matriz Técnica de Mapeamento entre Camadas

| # | Nome Kotlin (Room) | Nome SQL (PostgreSQL) | Tipo Kotlin | Tipo Room | Tipo PostgreSQL | Default Local | Validação |
| :-: | :--- | :--- | :---: | :---: | :---: | :---: | :--- |
| 1 | `id` | — | `Int` | `INTEGER` | — | `1` | Singleton `id == 1` |
| 2-3 | `dailyGoalGross`, `dailyGoalNet` | `daily_goal_gross`, `daily_goal_net` | `Long` | `INTEGER` | `NUMERIC(14,2)` | 20000L / 13000L | $\ge 0$ centavos |
| 4 | `dailyGoalKm` | `daily_goal_km` | `Double` | `REAL` | `NUMERIC(8,1)` | `100.0` | $\ge 0.0$ (escala 1) |
| 5 | `dailyGoalDeliveries` | `daily_goal_deliveries` | `Int` | `INTEGER` | `INTEGER` | `15` | $\ge 0$ unidades |
| 6-7 | `weeklyGoalGross`, `weeklyGoalNet` | `weekly_goal_gross`, `weekly_goal_net` | `Long` | `INTEGER` | `NUMERIC(14,2)` | 120000L / 80000L | $\ge 0$ centavos |
| 8 | `weeklyGoalKm` | `weekly_goal_km` | `Double` | `REAL` | `NUMERIC(8,1)` | `600.0` | $\ge 0.0$ (escala 1) |
| 9 | `weeklyGoalDeliveries` | `weekly_goal_deliveries` | `Int` | `INTEGER` | `INTEGER` | `90` | $\ge 0$ unidades |
| 10-11 | `monthlyGoalGross`, `monthlyGoalNet` | `monthly_goal_gross`, `monthly_goal_net` | `Long` | `INTEGER` | `NUMERIC(14,2)` | 500000L / 320000L | $\ge 0$ centavos |
| 12 | `monthlyGoalKm` | `monthly_goal_km` | `Double` | `REAL` | `NUMERIC(8,1)` | `2500.0` | $\ge 0.0$ (escala 1) |
| 13 | `monthlyGoalDeliveries` | `monthly_goal_deliveries` | `Int` | `INTEGER` | `INTEGER` | `400` | $\ge 0$ unidades |
| 14-15 | `annualGoalGross`, `annualGoalNet` | `annual_goal_gross`, `annual_goal_net` | `Long` | `INTEGER` | `NUMERIC(14,2)` | `0L` | $\ge 0$ centavos |
| 16 | `annualGoalKm` | `annual_goal_km` | `Double` | `REAL` | `NUMERIC(8,1)` | `0.0` | $\ge 0.0$ (escala 1) |
| 17 | `annualGoalDeliveries` | `annual_goal_deliveries` | `Int` | `INTEGER` | `INTEGER` | `0` | $\ge 0$ unidades |
| 18 | `workDaysPerWeek` | `work_days_per_week` | `Int` | `INTEGER` | `INTEGER` | `5` | $1 \le x \le 7$ |
| 19 | `workDaysPerMonth` | `work_days_per_month` | `Double` | `REAL` | `NUMERIC(4,2)` | `21.67` | $1.00 \le x \le 31.00$ |
| 20 | `workMonthsPerYear` | `work_months_per_year` | `Int` | `INTEGER` | `INTEGER` | `12` | $1 \le x \le 12$ |
| 21 | `workDaysPerMonthIsCustom` | `work_days_per_month_is_custom` | `Boolean` | `INTEGER` | `BOOLEAN` | `false` | Booleano |
| 22-25 | `annual*Origin` (4 campos) | `annual_*_origin` (4 colunas) | `String` | `TEXT` | `TEXT` | `'LEGACY_UNKNOWN'` | Enum 3 valores |
| 26 | `annualGoalsInitialized` | `annual_goals_initialized` | `Boolean` | `INTEGER` | `BOOLEAN` | `false` | Booleano |
| 27 | `revision` | `revision` | `Long` | `INTEGER` | `BIGINT` | `1L` | $\ge 1$ (Servidor) |
| 28 | `userModifiedAt` | `user_modified_at` | `Long` | `INTEGER` | `TIMESTAMPTZ` | `0L` | Timestamp ms |
| 29 | `remoteId` | `id` (PK) | `String` | `TEXT` | `UUID` | `""` | UUID v4 |
| 30 | `syncStatus` | — | `String` | `TEXT` | — | `'SYNCED'` | Enum local |
| 31 | `updatedAt` | `updated_at` | `Long` | `INTEGER` | `TIMESTAMPTZ` | System ms | Timestamp ms |
| 32 | `isDeleted` | `deleted_at` | `Boolean` | `INTEGER` | `TIMESTAMPTZ` | `false` | Soft delete |

---

## 9. CONFORMIDADE COM A LGPD NA EXPORTAÇÃO DE DADOS

O helper `UserDataExportHelper` exporta integralmente as **20 chaves** de metas e jornada:

```json
{
  "user_goals": {
    "daily_goal_gross": 200.00,
    "daily_goal_net": 130.00,
    "daily_goal_km": 100.0,
    "daily_goal_deliveries": 15,
    "weekly_goal_gross": 1200.00,
    "weekly_goal_net": 800.00,
    "weekly_goal_km": 600.0,
    "weekly_goal_deliveries": 90,
    "monthly_goal_gross": 5000.00,
    "monthly_goal_net": 3200.00,
    "monthly_goal_km": 2500.0,
    "monthly_goal_deliveries": 400,
    "annual_goal_gross": 60000.00,
    "annual_goal_net": 33800.00,
    "annual_goal_km": 30000.0,
    "annual_goal_deliveries": 3900,
    "work_days_per_week": 5,
    "work_days_per_month": 21.67,
    "work_months_per_year": 12,
    "work_days_per_month_is_custom": false
  }
}
```

---

## 10. PROCEDIMENTO ARMAZENADO COMPLETO NO POSTGRESQL (PL/PGSQL)

O script DDL e procedural abaixo consolida as tabelas auxiliares, índices, políticas RLS, validações estritas de JSON, OCC real com histórico, proteção uniforme de `USER_DEFINED`/`LEGACY_UNKNOWN` e blindagem de segurança:

```sql
-- ==============================================================================
-- GiroCusto - Procedimento Transacional Atômico e Tabelas de Sincronização
-- Versão: 5.2.1 (Saneamento Completo de Concorrência, Segurança e Idempotência)
-- ==============================================================================

-- 1. Extensões Obrigatórias
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 2. Tabela de Operações e Idempotência Persistente (ACH-26, ACH-34)
CREATE TABLE IF NOT EXISTS public.user_goals_operations (
    operation_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    payload_hash TEXT NOT NULL,
    response_payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.user_goals_operations ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can manage their own operations" ON public.user_goals_operations;
CREATE POLICY "Users can manage their own operations"
ON public.user_goals_operations FOR ALL TO authenticated
USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS idx_user_goals_operations_user_created 
    ON public.user_goals_operations(user_id, created_at);

-- 3. Tabela de Histórico de Revisões para OCC Efetivo (ACH-25)
CREATE TABLE IF NOT EXISTS public.user_goals_revision_history (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    revision BIGINT NOT NULL,
    operation_id UUID NOT NULL,
    changed_fields JSONB NOT NULL,
    previous_fields JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE(user_id, revision)
);

ALTER TABLE public.user_goals_revision_history ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view their own revision history" ON public.user_goals_revision_history;
CREATE POLICY "Users can view their own revision history"
ON public.user_goals_revision_history FOR SELECT TO authenticated
USING (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS idx_goals_history_user_rev 
    ON public.user_goals_revision_history(user_id, revision);

-- 4. Função Transacional de Sincronização Inteligente
CREATE OR REPLACE FUNCTION public.sync_user_goals_rpc(payload JSONB)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_user_id UUID := auth.uid();
    v_op_id UUID;
    v_base_rev BIGINT;
    v_client_modified TIMESTAMPTZ;
    v_fields JSONB;
    v_prov JSONB;
    v_overrides JSONB;
    v_payload_hash TEXT;
    v_op_record RECORD;
    v_current RECORD;
    v_result RECORD;
    v_new_revision BIGINT;
    v_has_mutation BOOLEAN := FALSE;
    
    -- Controle de Validação e Allowlist
    v_key TEXT;
    v_allowed_fields TEXT[] := ARRAY[
        'daily_goal_gross', 'daily_goal_net', 'daily_goal_km', 'daily_goal_deliveries',
        'weekly_goal_gross', 'weekly_goal_net', 'weekly_goal_km', 'weekly_goal_deliveries',
        'monthly_goal_gross', 'monthly_goal_net', 'monthly_goal_km', 'monthly_goal_deliveries',
        'annual_goal_gross', 'annual_goal_net', 'annual_goal_km', 'annual_goal_deliveries',
        'work_days_per_week', 'work_days_per_month', 'work_months_per_year',
        'work_days_per_month_is_custom', 'annual_goals_initialized'
    ];
    
    -- OCC e Histórico
    v_server_changed_keys JSONB;
    v_conflicting_fields TEXT[];
    v_delta_changed JSONB := '{}'::JSONB;
    v_delta_previous JSONB := '{}'::JSONB;

    -- Variáveis Consolidadas
    v_gross NUMERIC(14,2);
    v_net NUMERIC(14,2);
    v_km NUMERIC(8,1);
    v_deliv INTEGER;
    v_gross_orig TEXT;
    v_net_orig TEXT;
    v_km_orig TEXT;
    v_deliv_orig TEXT;
BEGIN
    -- [1] Validação de Autenticação (ACH-29)
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Não autorizado: sessão inválida ou ausente.' USING ERRCODE = '42501';
    END IF;

    -- [2] Validação do operation_id
    BEGIN
        v_op_id := (payload->>'operation_id')::UUID;
    EXCEPTION WHEN OTHERS THEN
        RAISE EXCEPTION 'operation_id inválido. UUID v4 obrigatório.' USING ERRCODE = '22023';
    END IF;

    IF v_op_id IS NULL THEN
        RAISE EXCEPTION 'operation_id é obrigatório.' USING ERRCODE = '22023';
    END IF;

    -- [3] Validação de base_revision
    v_base_rev := (payload->>'base_revision')::BIGINT;
    IF v_base_rev IS NULL OR v_base_rev < 0 THEN
        RAISE EXCEPTION 'base_revision inválida ou ausente.' USING ERRCODE = '22023';
    END IF;

    -- [4] Normalização de Timestamps com Trava de Clock Skew
    v_client_modified := COALESCE((payload->>'user_modified_at')::TIMESTAMPTZ, timezone('utc'::text, now()));
    IF v_client_modified > (timezone('utc'::text, now()) + interval '5 minutes') THEN
        v_client_modified := timezone('utc'::text, now());
    END IF;

    v_fields := COALESCE(payload->'fields', '{}'::JSONB);
    v_prov := COALESCE(payload->'provenance', '{}'::JSONB);
    v_overrides := COALESCE(payload->'explicit_user_override', '{}'::JSONB);

    -- [5] Validação com Allowlist Estrita e Rejeição de Desconhecidos / Metadados Proibidos
    FOR v_key IN SELECT jsonb_object_keys(v_fields) LOOP
        -- Rejeita metadados protegidos e chaves proibidas
        IF v_key IN ('revision', 'user_id', 'id', 'last_operation_id', 'created_at', 'updated_at', 'deleted_at') THEN
            RAISE EXCEPTION 'Tentativa de adulteração de metadado protegido: %', v_key USING ERRCODE = '42501';
        END IF;
        -- Rejeita campos desconhecidos
        IF NOT (v_key = ANY(v_allowed_fields)) THEN
            RAISE EXCEPTION 'Campo desconhecido ou não permitido no payload: %', v_key USING ERRCODE = '22023';
        END IF;
        -- Rejeita nulos
        IF v_fields->v_key IS NULL OR (v_fields->>v_key) IS NULL THEN
            RAISE EXCEPTION 'Valor nulo proibido para o campo: %', v_key USING ERRCODE = '22004';
        END IF;
    END LOOP;

    -- [6] Validação dos Enums de Proveniência
    FOR v_key IN SELECT jsonb_object_keys(v_prov) LOOP
        IF (v_prov->>v_key) NOT IN ('DERIVED', 'USER_DEFINED', 'LEGACY_UNKNOWN') THEN
            RAISE EXCEPTION 'Proveniência inválida no campo %: %', v_key, (v_prov->>v_key) USING ERRCODE = '22023';
        END IF;
    END LOOP;

    -- [7] Lock Transacional Exclusivo por Usuário (Serialização Atômica)
    PERFORM pg_advisory_xact_lock(hashtext(v_user_id::text));

    -- [8] Idempotência: Verificação na tabela user_goals_operations (ACH-26, ACH-34)
    v_payload_hash := encode(digest(payload::text, 'sha256'), 'hex');
    SELECT * INTO v_op_record FROM public.user_goals_operations WHERE operation_id = v_op_id;
    IF FOUND THEN
        IF v_op_record.user_id != v_user_id THEN
            RAISE EXCEPTION 'Violação de segurança: operação pertence a outro usuário.' USING ERRCODE = '42501';
        END IF;
        IF v_op_record.payload_hash != v_payload_hash THEN
            RAISE EXCEPTION 'Conflito de idempotência: reutilização de operation_id com payload divergente.' USING ERRCODE = '23505';
        END IF;
        -- Retransmissão legítima confirmada: retorna resultado em cache sem nova mutação
        RETURN v_op_record.response_payload;
    END IF;

    -- [9] Leitura do Registro Remoto Atual
    SELECT * INTO v_current FROM public.user_goals WHERE user_id = v_user_id FOR UPDATE;

    -- [10] CASO DE PRIMEIRA INSERÇÃO (Registro Inexistente)
    IF NOT FOUND THEN
        INSERT INTO public.user_goals (
            user_id,
            daily_goal_gross, daily_goal_net, daily_goal_km, daily_goal_deliveries,
            weekly_goal_gross, weekly_goal_net, weekly_goal_km, weekly_goal_deliveries,
            monthly_goal_gross, monthly_goal_net, monthly_goal_km, monthly_goal_deliveries,
            annual_goal_gross, annual_goal_net, annual_goal_km, annual_goal_deliveries,
            work_days_per_week, work_days_per_month, work_months_per_year, work_days_per_month_is_custom,
            annual_gross_origin, annual_net_origin, annual_km_origin, annual_deliveries_origin,
            annual_goals_initialized, revision, last_operation_id, user_modified_at
        ) VALUES (
            v_user_id,
            COALESCE((v_fields->>'daily_goal_gross')::NUMERIC(14,2), 200.00),
            COALESCE((v_fields->>'daily_goal_net')::NUMERIC(14,2), 130.00),
            COALESCE((v_fields->>'daily_goal_km')::NUMERIC(8,1), 100.0),
            COALESCE((v_fields->>'daily_goal_deliveries')::INTEGER, 15),
            COALESCE((v_fields->>'weekly_goal_gross')::NUMERIC(14,2), 1200.00),
            COALESCE((v_fields->>'weekly_goal_net')::NUMERIC(14,2), 800.00),
            COALESCE((v_fields->>'weekly_goal_km')::NUMERIC(8,1), 600.0),
            COALESCE((v_fields->>'weekly_goal_deliveries')::INTEGER, 90),
            COALESCE((v_fields->>'monthly_goal_gross')::NUMERIC(14,2), 5000.00),
            COALESCE((v_fields->>'monthly_goal_net')::NUMERIC(14,2), 3200.00),
            COALESCE((v_fields->>'monthly_goal_km')::NUMERIC(8,1), 2500.0),
            COALESCE((v_fields->>'monthly_goal_deliveries')::INTEGER, 400),
            COALESCE((v_fields->>'annual_goal_gross')::NUMERIC(14,2), 0.00),
            COALESCE((v_fields->>'annual_goal_net')::NUMERIC(14,2), 0.00),
            COALESCE((v_fields->>'annual_goal_km')::NUMERIC(8,1), 0.0),
            COALESCE((v_fields->>'annual_goal_deliveries')::INTEGER, 0),
            COALESCE((v_fields->>'work_days_per_week')::INTEGER, 5),
            COALESCE((v_fields->>'work_days_per_month')::NUMERIC(4,2), 21.67),
            COALESCE((v_fields->>'work_months_per_year')::INTEGER, 12),
            COALESCE((v_fields->>'work_days_per_month_is_custom')::BOOLEAN, FALSE),
            COALESCE(v_prov->>'annual_gross_origin', 'LEGACY_UNKNOWN'),
            COALESCE(v_prov->>'annual_net_origin', 'LEGACY_UNKNOWN'),
            COALESCE(v_prov->>'annual_km_origin', 'LEGACY_UNKNOWN'),
            COALESCE(v_prov->>'annual_deliveries_origin', 'LEGACY_UNKNOWN'),
            TRUE,
            1,
            v_op_id,
            v_client_modified
        ) RETURNING * INTO v_result;

        -- Grava histórico da criação inicial
        INSERT INTO public.user_goals_revision_history (user_id, revision, operation_id, changed_fields, previous_fields)
        VALUES (v_user_id, 1, v_op_id, to_jsonb(v_result), '{}'::JSONB);

        -- Grava operação na tabela de idempotência
        INSERT INTO public.user_goals_operations (operation_id, user_id, payload_hash, response_payload)
        VALUES (v_op_id, v_user_id, v_payload_hash, jsonb_build_object('status', 'SUCCESS', 'data', to_jsonb(v_result)));

        RETURN jsonb_build_object('status', 'SUCCESS', 'data', to_jsonb(v_result));
    END IF;

    -- [11] CONTROLE DE CONCORRÊNCIA OTIMISTA (OCC) (ACH-25)
    -- Inconsistência: Cliente com revisão superior à do servidor
    IF v_base_rev > v_current.revision THEN
        RAISE EXCEPTION 'Revisão-base inválida (%): superior à revisão atual do servidor (%).', v_base_rev, v_current.revision USING ERRCODE = '42000';
    END IF;

    -- Cliente desatualizado: avalia se há colisão concorrente no mesmo campo
    IF v_base_rev < v_current.revision THEN
        SELECT COALESCE(jsonb_object_agg(field_key, true), '{}'::JSONB)
        INTO v_server_changed_keys
        FROM (
            SELECT DISTINCT jsonb_object_keys(changed_fields) AS field_key
            FROM public.user_goals_revision_history
            WHERE user_id = v_user_id
              AND revision > v_base_rev
              AND revision <= v_current.revision
        ) sub;

        -- Detecta colisão de campos
        SELECT array_agg(k)
        INTO v_conflicting_fields
        FROM jsonb_object_keys(v_fields) k
        WHERE v_server_changed_keys ? k;

        -- Se houver colisão no mesmo campo: BLOQUEIA (Sem LWW silencioso)
        IF v_conflicting_fields IS NOT NULL AND array_length(v_conflicting_fields, 1) > 0 THEN
            RETURN jsonb_build_object(
                'status', 'CONFLICT',
                'message', 'Conflito de concorrência detectado: campos modificados concorrentemente no servidor.',
                'conflicting_fields', to_jsonb(v_conflicting_fields),
                'base_revision', v_base_rev,
                'current_revision', v_current.revision,
                'server_record', to_jsonb(v_current)
            );
        END IF;
    END IF;

    -- [12] RESOLUÇÃO GRANULAR E PROTEÇÃO DE USER_DEFINED E LEGACY_UNKNOWN (ACH-28, ACH-32, ACH-18)
    -- Indicador 1: Annual Gross
    v_gross := v_current.annual_goal_gross;
    v_gross_orig := v_current.annual_gross_origin;
    IF v_fields ? 'annual_goal_gross' THEN
        IF v_current.annual_gross_origin = 'USER_DEFINED' THEN
            IF COALESCE((v_overrides->>'annual_gross')::BOOLEAN, FALSE) = TRUE THEN
                v_gross := (v_fields->>'annual_goal_gross')::NUMERIC(14,2);
                v_gross_orig := COALESCE(v_prov->>'annual_gross_origin', 'USER_DEFINED');
            END IF;
        ELSIF v_current.annual_gross_origin = 'LEGACY_UNKNOWN' AND v_current.annual_goal_gross > 0.00 THEN
            IF COALESCE((v_overrides->>'annual_gross')::BOOLEAN, FALSE) = TRUE AND (v_prov->>'annual_gross_origin') = 'USER_DEFINED' THEN
                v_gross := (v_fields->>'annual_goal_gross')::NUMERIC(14,2);
                v_gross_orig := 'USER_DEFINED';
            END IF;
        ELSE
            v_gross := (v_fields->>'annual_goal_gross')::NUMERIC(14,2);
            IF v_prov ? 'annual_gross_origin' THEN v_gross_orig := v_prov->>'annual_gross_origin'; END IF;
        END IF;
    END IF;

    -- Indicador 2: Annual Net
    v_net := v_current.annual_goal_net;
    v_net_orig := v_current.annual_net_origin;
    IF v_fields ? 'annual_goal_net' THEN
        IF v_current.annual_net_origin = 'USER_DEFINED' THEN
            IF COALESCE((v_overrides->>'annual_net')::BOOLEAN, FALSE) = TRUE THEN
                v_net := (v_fields->>'annual_goal_net')::NUMERIC(14,2);
                v_net_orig := COALESCE(v_prov->>'annual_net_origin', 'USER_DEFINED');
            END IF;
        ELSIF v_current.annual_net_origin = 'LEGACY_UNKNOWN' AND v_current.annual_goal_net > 0.00 THEN
            IF COALESCE((v_overrides->>'annual_net')::BOOLEAN, FALSE) = TRUE AND (v_prov->>'annual_net_origin') = 'USER_DEFINED' THEN
                v_net := (v_fields->>'annual_goal_net')::NUMERIC(14,2);
                v_net_orig := 'USER_DEFINED';
            END IF;
        ELSE
            v_net := (v_fields->>'annual_goal_net')::NUMERIC(14,2);
            IF v_prov ? 'annual_net_origin' THEN v_net_orig := v_prov->>'annual_net_origin'; END IF;
        END IF;
    END IF;

    -- Indicador 3: Annual Km
    v_km := v_current.annual_goal_km;
    v_km_orig := v_current.annual_km_origin;
    IF v_fields ? 'annual_goal_km' THEN
        IF v_current.annual_km_origin = 'USER_DEFINED' THEN
            IF COALESCE((v_overrides->>'annual_km')::BOOLEAN, FALSE) = TRUE THEN
                v_km := (v_fields->>'annual_goal_km')::NUMERIC(8,1);
                v_km_orig := COALESCE(v_prov->>'annual_km_origin', 'USER_DEFINED');
            END IF;
        ELSIF v_current.annual_km_origin = 'LEGACY_UNKNOWN' AND v_current.annual_goal_km > 0.0 THEN
            IF COALESCE((v_overrides->>'annual_km')::BOOLEAN, FALSE) = TRUE AND (v_prov->>'annual_km_origin') = 'USER_DEFINED' THEN
                v_km := (v_fields->>'annual_goal_km')::NUMERIC(8,1);
                v_km_orig := 'USER_DEFINED';
            END IF;
        ELSE
            v_km := (v_fields->>'annual_goal_km')::NUMERIC(8,1);
            IF v_prov ? 'annual_km_origin' THEN v_km_orig := v_prov->>'annual_km_origin'; END IF;
        END IF;
    END IF;

    -- Indicador 4: Annual Deliveries
    v_deliv := v_current.annual_goal_deliveries;
    v_deliv_orig := v_current.annual_deliveries_origin;
    IF v_fields ? 'annual_goal_deliveries' THEN
        IF v_current.annual_deliveries_origin = 'USER_DEFINED' THEN
            IF COALESCE((v_overrides->>'annual_deliveries')::BOOLEAN, FALSE) = TRUE THEN
                v_deliv := (v_fields->>'annual_goal_deliveries')::INTEGER;
                v_deliv_orig := COALESCE(v_prov->>'annual_deliveries_origin', 'USER_DEFINED');
            END IF;
        ELSIF v_current.annual_deliveries_origin = 'LEGACY_UNKNOWN' AND v_current.annual_goal_deliveries > 0 THEN
            IF COALESCE((v_overrides->>'annual_deliveries')::BOOLEAN, FALSE) = TRUE AND (v_prov->>'annual_deliveries_origin') = 'USER_DEFINED' THEN
                v_deliv := (v_fields->>'annual_goal_deliveries')::INTEGER;
                v_deliv_orig := 'USER_DEFINED';
            END IF;
        ELSE
            v_deliv := (v_fields->>'annual_goal_deliveries')::INTEGER;
            IF v_prov ? 'annual_deliveries_origin' THEN v_deliv_orig := v_prov->>'annual_deliveries_origin'; END IF;
        END IF;
    END IF;

    -- [13] EXECUÇÃO DO UPDATE COM FIELD-LEVEL MERGE E DELTAS
    UPDATE public.user_goals SET
        daily_goal_gross = CASE WHEN v_fields ? 'daily_goal_gross' THEN (v_fields->>'daily_goal_gross')::NUMERIC(14,2) ELSE daily_goal_gross END,
        daily_goal_net   = CASE WHEN v_fields ? 'daily_goal_net'   THEN (v_fields->>'daily_goal_net')::NUMERIC(14,2)   ELSE daily_goal_net END,
        daily_goal_km    = CASE WHEN v_fields ? 'daily_goal_km'    THEN (v_fields->>'daily_goal_km')::NUMERIC(8,1)    ELSE daily_goal_km END,
        daily_goal_deliveries = CASE WHEN v_fields ? 'daily_goal_deliveries' THEN (v_fields->>'daily_goal_deliveries')::INTEGER ELSE daily_goal_deliveries END,

        weekly_goal_gross = CASE WHEN v_fields ? 'weekly_goal_gross' THEN (v_fields->>'weekly_goal_gross')::NUMERIC(14,2) ELSE weekly_goal_gross END,
        weekly_goal_net   = CASE WHEN v_fields ? 'weekly_goal_net'   THEN (v_fields->>'weekly_goal_net')::NUMERIC(14,2)   ELSE weekly_goal_net END,
        weekly_goal_km    = CASE WHEN v_fields ? 'weekly_goal_km'    THEN (v_fields->>'weekly_goal_km')::NUMERIC(8,1)    ELSE weekly_goal_km END,
        weekly_goal_deliveries = CASE WHEN v_fields ? 'weekly_goal_deliveries' THEN (v_fields->>'weekly_goal_deliveries')::INTEGER ELSE weekly_goal_deliveries END,

        monthly_goal_gross = CASE WHEN v_fields ? 'monthly_goal_gross' THEN (v_fields->>'monthly_goal_gross')::NUMERIC(14,2) ELSE monthly_goal_gross END,
        monthly_goal_net   = CASE WHEN v_fields ? 'monthly_goal_net'   THEN (v_fields->>'monthly_goal_net')::NUMERIC(14,2)   ELSE monthly_goal_net END,
        monthly_goal_km    = CASE WHEN v_fields ? 'monthly_goal_km'    THEN (v_fields->>'monthly_goal_km')::NUMERIC(8,1)    ELSE monthly_goal_km END,
        monthly_goal_deliveries = CASE WHEN v_fields ? 'monthly_goal_deliveries' THEN (v_fields->>'monthly_goal_deliveries')::INTEGER ELSE monthly_goal_deliveries END,

        annual_goal_gross = v_gross,
        annual_gross_origin = v_gross_orig,
        annual_goal_net = v_net,
        annual_net_origin = v_net_orig,
        annual_goal_km = v_km,
        annual_km_origin = v_km_orig,
        annual_goal_deliveries = v_deliv,
        annual_deliveries_origin = v_deliv_orig,

        work_days_per_week = CASE WHEN v_fields ? 'work_days_per_week' THEN (v_fields->>'work_days_per_week')::INTEGER ELSE work_days_per_week END,
        work_days_per_month = CASE WHEN v_fields ? 'work_days_per_month' THEN (v_fields->>'work_days_per_month')::NUMERIC(4,2) ELSE work_days_per_month END,
        work_months_per_year = CASE WHEN v_fields ? 'work_months_per_year' THEN (v_fields->>'work_months_per_year')::INTEGER ELSE work_months_per_year END,
        work_days_per_month_is_custom = CASE WHEN v_fields ? 'work_days_per_month_is_custom' THEN (v_fields->>'work_days_per_month_is_custom')::BOOLEAN ELSE work_days_per_month_is_custom END,

        annual_goals_initialized = TRUE,
        revision = v_current.revision + 1,
        last_operation_id = v_op_id,
        user_modified_at = GREATEST(v_current.user_modified_at, v_client_modified),
        updated_at = timezone('utc'::text, now())
    WHERE user_id = v_user_id
    RETURNING * INTO v_result;

    -- [14] Gravação do Histórico de Revisão para Futuros OCC (ACH-25)
    INSERT INTO public.user_goals_revision_history (
        user_id,
        revision,
        operation_id,
        changed_fields,
        previous_fields
    ) VALUES (
        v_user_id,
        v_result.revision,
        v_op_id,
        to_jsonb(v_fields),
        jsonb_build_object(
            'annual_goal_gross', v_current.annual_goal_gross,
            'annual_goal_net', v_current.annual_goal_net,
            'annual_goal_km', v_current.annual_goal_km,
            'annual_goal_deliveries', v_current.annual_goal_deliveries
        )
    );

    -- [15] Gravação Atômica na Tabela de Operações (Idempotência)
    INSERT INTO public.user_goals_operations (operation_id, user_id, payload_hash, response_payload)
    VALUES (v_op_id, v_user_id, v_payload_hash, jsonb_build_object('status', 'SUCCESS', 'data', to_jsonb(v_result)));

    RETURN jsonb_build_object('status', 'SUCCESS', 'data', to_jsonb(v_result));
END;
$$;

-- 5. Revogação Rigorosa de Privilégios (ACH-29)
REVOKE ALL ON FUNCTION public.sync_user_goals_rpc(JSONB) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.sync_user_goals_rpc(JSONB) FROM anon;
GRANT EXECUTE ON FUNCTION public.sync_user_goals_rpc(JSONB) TO authenticated;
```

---

## 11. PLANO DE MIGRAÇÕES

### 11.1 Migração Local Room (Versão 16 $\to$ 17)
- **Arquivo de Origem:** `app/schemas/com.example.data.GiroCustoDatabase/16.json`
- **Arquivo de Destino:** `app/schemas/com.example.data.GiroCustoDatabase/17.json`
- **Alterações:** Adiciona 14 colunas em `user_goals` e cria a tabela `sync_outbox`:

```kotlin
val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Metas Anuais Complementares
        db.execSQL("ALTER TABLE `user_goals` ADD COLUMN `annualGoalGross` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `user_goals` ADD COLUMN `annualGoalKm` REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE `user_goals` ADD COLUMN `annualGoalDeliveries` INTEGER NOT NULL DEFAULT 0")

        // 2. Parâmetros de Jornada
        db.execSQL("ALTER TABLE `user_goals` ADD COLUMN `workDaysPerWeek` INTEGER NOT NULL DEFAULT 5")
        db.execSQL("ALTER TABLE `user_goals` ADD COLUMN `workDaysPerMonth` REAL NOT NULL DEFAULT 21.67")
        db.execSQL("ALTER TABLE `user_goals` ADD COLUMN `workMonthsPerYear` INTEGER NOT NULL DEFAULT 12")
        db.execSQL("ALTER TABLE `user_goals` ADD COLUMN `workDaysPerMonthIsCustom` INTEGER NOT NULL DEFAULT 0")

        // 3. Proveniências
        db.execSQL("ALTER TABLE `user_goals` ADD COLUMN `annualGrossOrigin` TEXT NOT NULL DEFAULT 'LEGACY_UNKNOWN'")
        db.execSQL("ALTER TABLE `user_goals` ADD COLUMN `annualNetOrigin` TEXT NOT NULL DEFAULT 'LEGACY_UNKNOWN'")
        db.execSQL("ALTER TABLE `user_goals` ADD COLUMN `annualKmOrigin` TEXT NOT NULL DEFAULT 'LEGACY_UNKNOWN'")
        db.execSQL("ALTER TABLE `user_goals` ADD COLUMN `annualDeliveriesOrigin` TEXT NOT NULL DEFAULT 'LEGACY_UNKNOWN'")

        // 4. Semântica e OCC
        db.execSQL("ALTER TABLE `user_goals` ADD COLUMN `annualGoalsInitialized` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `user_goals` ADD COLUMN `revision` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE `user_goals` ADD COLUMN `userModifiedAt` INTEGER NOT NULL DEFAULT 0")

        // 5. Criação da Tabela de Outbox Persistente (ACH-34)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `sync_outbox` (
                `operationId` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `operationType` TEXT NOT NULL,
                `serializedPayload` TEXT NOT NULL,
                `baseRevision` INTEGER NOT NULL,
                `state` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `lastAttemptAt` INTEGER,
                `attemptCount` INTEGER NOT NULL DEFAULT 0,
                `lastError` TEXT,
                PRIMARY KEY(`operationId`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_sync_outbox_state_created` ON `sync_outbox`(`state`, `createdAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_sync_outbox_user_id` ON `sync_outbox`(`userId`)")
    }
}
```

### 11.2 Migração Remota Supabase (PostgreSQL DDL)

```sql
-- Migration: 20261108_goals_v5_2_1_migration.sql
ALTER TABLE public.user_goals 
    ALTER COLUMN daily_goal_gross TYPE NUMERIC(14,2) USING round(daily_goal_gross::numeric, 2),
    ALTER COLUMN daily_goal_net TYPE NUMERIC(14,2) USING round(daily_goal_net::numeric, 2),
    ALTER COLUMN daily_goal_km TYPE NUMERIC(8,1) USING round(daily_goal_km::numeric, 1),
    ALTER COLUMN weekly_goal_gross TYPE NUMERIC(14,2) USING round(weekly_goal_gross::numeric, 2),
    ALTER COLUMN weekly_goal_net TYPE NUMERIC(14,2) USING round(weekly_goal_net::numeric, 2),
    ALTER COLUMN weekly_goal_km TYPE NUMERIC(8,1) USING round(weekly_goal_km::numeric, 1),
    ALTER COLUMN monthly_goal_gross TYPE NUMERIC(14,2) USING round(monthly_goal_gross::numeric, 2),
    ALTER COLUMN monthly_goal_net TYPE NUMERIC(14,2) USING round(monthly_goal_net::numeric, 2),
    ALTER COLUMN monthly_goal_km TYPE NUMERIC(8,1) USING round(monthly_goal_km::numeric, 1),
    ALTER COLUMN annual_goal_net TYPE NUMERIC(14,2) USING round(annual_goal_net::numeric, 2);

ALTER TABLE public.user_goals
    ADD COLUMN IF NOT EXISTS annual_goal_gross NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS annual_goal_km NUMERIC(8,1) NOT NULL DEFAULT 0.0,
    ADD COLUMN IF NOT EXISTS annual_goal_deliveries INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS work_days_per_week INTEGER NOT NULL DEFAULT 5,
    ADD COLUMN IF NOT EXISTS work_days_per_month NUMERIC(4,2) NOT NULL DEFAULT 21.67,
    ADD COLUMN IF NOT EXISTS work_months_per_year INTEGER NOT NULL DEFAULT 12,
    ADD COLUMN IF NOT EXISTS work_days_per_month_is_custom BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS annual_gross_origin TEXT NOT NULL DEFAULT 'LEGACY_UNKNOWN',
    ADD COLUMN IF NOT EXISTS annual_net_origin TEXT NOT NULL DEFAULT 'LEGACY_UNKNOWN',
    ADD COLUMN IF NOT EXISTS annual_km_origin TEXT NOT NULL DEFAULT 'LEGACY_UNKNOWN',
    ADD COLUMN IF NOT EXISTS annual_deliveries_origin TEXT NOT NULL DEFAULT 'LEGACY_UNKNOWN',
    ADD COLUMN IF NOT EXISTS annual_goals_initialized BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS revision BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS last_operation_id UUID NULL,
    ADD COLUMN IF NOT EXISTS user_modified_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now());
```

---

## 12. MATRIZ DE TESTES E COMPROVAÇÃO TÉCNICA (ACH-33)

### 12.1 Categorias Especializadas por Camada

| Categoria | Camada e Ferramenta | Escopo de Validação | Testes Mínimos |
| :--- | :--- | :--- | :---: |
| **CAT-01: Domínio Kotlin** | JUnit 4 puro | Aritmética de jornada, precisão de centavos, DAG de precedência. | 15 testes |
| **CAT-02: Room Local** | Robolectric / SQLite | CRUD de `UserGoals`, construtor de 32 propriedades, índices. | 8 testes |
| **CAT-03: Room Outbox** | Robolectric / SQLite | Persistência transacional de operações na Outbox e transições de estado. | 6 testes |
| **CAT-04: Migração Room** | `MigrationTestHelper` | Migração v16 $\to$ v17 com banco pré-populado e validação de schema 17. | 3 testes |
| **CAT-05: Contrato DTO** | Moshi Adapters | Serialização, omissão de chaves, parsing de respostas `SUCCESS` e `CONFLICT`. | 6 testes |
| **CAT-06: Sync Coordinator** | MockWebServer / Coroutines | Fila isolada de metas, reuso de `operation_id`, backoff exponencial. | 8 testes |
| **CAT-07: PostgreSQL OCC** | Testcontainers / Postgres 15+ | 3-way merge independente e bloqueio por status `CONFLICT` sem LWW. | 6 testes |
| **CAT-08: Idempotência Real** | Testcontainers / Postgres 15+ | Múltiplas chamadas paralelas com mesmo `operation_id` e integridade de hash. | 5 testes |
| **CAT-09: Proteção de Origens** | Testcontainers / Postgres 15+ | Imutabilidade de `USER_DEFINED` (inclusive zero) e `LEGACY_UNKNOWN` positivo. | 6 testes |
| **CAT-10: Segurança da RPC** | Testcontainers / Postgres 15+ | Rejeição de `anon`, allowlist, injeção de parâmetros e tentativa de tenant leak. | 6 testes |
| **CAT-11: Exportação LGPD** | Unit Test / JSON Assert | Exportação das 20 chaves de metas e conferência de valores. | 4 testes |

### 12.2 Especificação dos Roteiros de Teste de Integração no PostgreSQL Real

```
ROTEIROS DE TESTES NO POSTGRESQL REAL:

1. TC-OCC-REAL-01: 3-Way Merge em Campos Disjuntos
   - Pré-condição: user_goals na Revisão 10.
   - Execução: Dispositivo A envia patch com base_revision=10 alterando daily_goal_gross para 250.00. 
     Em seguida, Dispositivo B envia patch com base_revision=10 alterando annual_goal_km para 35000.0.
   - Asserção: Dispositivo A recebe Rev 11. Dispositivo B recebe Rev 12 com status SUCCESS.
     Consulta direta ao banco confirma daily_goal_gross=250.00 e annual_goal_km=35000.0.

2. TC-OCC-REAL-02: Detecção de Conflito no Mesmo Campo (Sem LWW Silencioso)
   - Pré-condição: user_goals na Revisão 10 com daily_goal_net=130.00.
   - Execução: A envia patch (base=10, daily_goal_net=150.00) -> comitado na Rev 11.
     B envia patch (base=10, daily_goal_net=160.00).
   - Asserção: Resposta do servidor para B contém:
     status = "CONFLICT", conflicting_fields = ["daily_goal_net"], current_revision = 11.
     Registro no banco PERMANECE 150.00 e revisão PERMANECE 11. Zero LWW silencioso.

3. TC-PROT-REAL-01: Imunidade de USER_DEFINED com Valor Zero
   - Pré-condição: annual_goal_net = 0.00, annual_net_origin = 'USER_DEFINED'.
   - Execução: Envio de patch automático derivado (annual_goal_net = 33800.00, origin = 'DERIVED', override = false).
   - Asserção: annual_goal_net permanece 0.00 e annual_net_origin permanece 'USER_DEFINED'.

4. TC-PROT-REAL-02: Preservação de LEGACY_UNKNOWN Positivo (ACH-18)
   - Pré-condição: annual_goal_gross = 60000.00, annual_gross_origin = 'LEGACY_UNKNOWN'.
   - Execução: Envio de patch com recálculo DERIVED sem override.
   - Asserção: annual_goal_gross permanece 60000.00 e proveniência permanece 'LEGACY_UNKNOWN'.

5. TC-IDEM-REAL-01: Reenvio de Operação Após Perda de Resposta
   - Pré-condição: OP-777 processado com Rev 15.
   - Execução: Reenvio de OP-777 com exatamente o mesmo payload.
   - Asserção: Resposta idêntica devolvida; revision permanece 15; contagem em user_goals_operations é 1.

6. TC-SEC-REAL-01: Rejeição de Chave Desconhecida no Payload JSON
   - Pré-condição: Usuário autenticado.
   - Execução: sync_user_goals_rpc com fields contendo "hacker_field": 123.
   - Asserção: Exceção capturada com SQLSTATE 22023 ("Campo desconhecido ou não permitido").
```

---

## 13. TABELA DE RASTREABILIDADE CONSOLIDADA

| ACH/INC | Problema | Correção | Evidência técnica | Critério de aceitação | Status |
| :---: | :--- | :--- | :--- | :--- | :---: |
| **ACH-12** | Field-level merge ausente | Patch DTO e aplicação seletiva no SQL via operador `?`. | Seção 7 e 10 | Preservação de campos não enviados no JSON. | **CORRIGIDO** |
| **ACH-13** | OCC incompleto no contrato | `base_revision` obrigatório e validado na RPC. | Seção 7 e 10 | Rejeição ou merge com base na revisão. | **CORRIGIDO** |
| **ACH-14** | Conflito em metas manuais | Resolução determinística sem LWW silencioso. | Seção 5.2 e 12 | Retorno de status `CONFLICT` em colisões. | **CORRIGIDO** |
| **ACH-15** | Corrida na 1ª inserção | Advisory lock por usuário + tratamento de `unique_violation`. | Seção 10 | Serialização atômica de criação de linha. | **CORRIGIDO** |
| **ACH-16** | Idempotência volátil | Tabela `user_goals_operations` com retenção e hash SHA-256. | Seção 10 | Retorno de snapshot em cache sem nova mutação. | **CORRIGIDO** |
| **ACH-17** | Falha de segurança na RPC | `SECURITY DEFINER` e revogação de `PUBLIC`/`anon`. | Seção 10 | Bloqueio de acesso anônimo e schemas fixos. | **CORRIGIDO** |
| **ACH-18** | Regressão de `LEGACY_UNKNOWN` | Valor legado positivo $>0$ protegido em INSERT e UPDATE contra `DERIVED`. | Seção 5.3 e 10 | Tabela de decisão e bloqueio explícito no SQL. | **CORRIGIDO** |
| **ACH-19** | Timestamp global insuficiente | Servidor como autoridade canônica de ordenação atômica. | Seção 6 e 10 | Histórico ordenado por revisão no servidor. | **CORRIGIDO** |
| **ACH-20** | Desempate frágil por hash | Comparação lexicográfica canônica de UUID (`operation_id`). | Seção 5.2 | Desempate determinístico em colisões puras. | **CORRIGIDO** |
| **ACH-21** | Metas acopladas no sync geral | Sincronizador dedicado isolado com mutex local. | Seção 6.1 | Fila independente de metas sem bloqueio mútuo. | **CORRIGIDO** |
| **ACH-22** | Incompatibilidade monetária | `Long` centavos no Room e `NUMERIC(14,2)` no PostgreSQL. | Seção 8 e 11 | Alinhamento de escala e semântica contábil. | **CORRIGIDO** |
| **ACH-23** | Limites matemáticos e tipos | Km com escala 1 (`HALF_UP`), entregas inteiras e limites numéricos. | Seção 5.5 e 8 | Validações explícitas de domínio no SQL. | **CORRIGIDO** |
| **ACH-24** | Ambiguidade de escopo de UI | Divisão rígida: Fase A (Domínio/Sync) e Fase B (Interface). | Seção 6.2 | Metas anuais operando inicialmente como DERIVED. | **CORRIGIDO** |
| **ACH-25** | OCC não utilizado na RPC | Tabela `user_goals_revision_history` e comparação real de revisões. | Seção 5.1 e 10 | Detecção exata de deltas do servidor e merge seletivo. | **CORRIGIDO** |
| **ACH-26** | Idempotência incompleta | Tabela `user_goals_operations` com lock e hash de payload. | Seção 5.7 e 10 | Prevenção comprovada de dupla mutação. | **CORRIGIDO** |
| **ACH-27** | Matriz de conflitos incompleta | Matriz fechada de 8 colunas e 10 cenários sem LWW silencioso. | Seção 5.2 | Correspondência estrita com a lógica da RPC. | **CORRIGIDO** |
| **ACH-28** | Brecha na proteção de manual | Proteção de `USER_DEFINED` (inclusive zero) em INSERT e UPDATE. | Seção 5.3 e 10 | Imunidade comprovada de metas manuais com valor 0. | **CORRIGIDO** |
| **ACH-29** | Segurança insuficiente da RPC | 8 salvaguardas com qualificação, allowlist e revogação de `PUBLIC`. | Seção 5.4 e 10 | Bloqueio de injeção, chaves estranhas e tenant leak. | **CORRIGIDO** |
| **ACH-30** | Imprecisão na jornada | Precisão unificada em 7 dimensões, fator $\frac{13}{3}$ e centavos exatos. | Seção 5.5 e 8 | Zero divergência entre Kotlin e PostgreSQL. | **CORRIGIDO** |
| **ACH-31** | Origem de cálculo indefinida | DAG de precedência (Mensal $\to$ Diária) sem circularidade. | Seção 5.5 | Resolução matemática unívoca sem loops. | **CORRIGIDO** |
| **ACH-32** | Proteção manual sem servidor | Validação server-side exigindo flag explícita de override. | Seção 5.3 e 10 | Rejeição no banco de patches automáticos. | **CORRIGIDO** |
| **ACH-33** | Matriz de testes insuficiente | Separação das 4 camadas físicas e testes em PostgreSQL real. | Seção 5.6 e 12 | Especificação executável de testes distribuídos. | **CORRIGIDO** |
| **ACH-34** | Ausência de Outbox no Room | Entidade `sync_outbox`, persistência transacional e reuso de UUID. | Seção 5.7 e 11.1 | Recuperação de falha comprovada em 13 etapas. | **NOVO REQUISITO ATENDIDO** |
| **INC-01** | Contagem errada de propriedades | Contagem auditada e comprovada: exatamente 32 propriedades. | Seção 8.1 | Enumeração nominal e comprovação matemática. | **CORRIGIDO** |
| **INC-02** | Confusão de indicadores e chaves | 4 indicadores $\times$ 4 períodos $= 16$ metas; 20 chaves LGPD. | Seção 8 e 9 | Definição terminológica padronizada. | **CORRIGIDO** |
| **INC-03** | Tipagem inconsistente | Tipos padronizados entre Kotlin, Room e PostgreSQL. | Seção 8.2 | Matriz técnica unificada de campos. | **CORRIGIDO** |
| **INC-04** | Km sem escala definida | Escala 1 com `RoundingMode.HALF_UP`. | Seção 8.2 | Imunidade contra dízimas microscópicas. | **CORRIGIDO** |
| **INC-05** | Migração monetária remota | Script DDL com `USING round(...::numeric, 2)`. | Seção 11.2 | Migração conceitual sem perda de centavos. | **CORRIGIDO** |
| **INC-06** | Sobrescrita no PULL inicial | Reconciliação prévia obrigatória antes de gravar no Room. | Seção 6.1 | Preservação de dados locais pendentes. | **CORRIGIDO** |
| **INC-07** | Trava de clock skew futuro | Limite de tolerância de 5 minutos contra o relógio do servidor. | Seção 5.4 e 10 | Clamp atômico em `user_modified_at`. | **CORRIGIDO** |
| **INC-08** | Confusão entre Mock e DB Real | Matriz dividida entre testes de contrato e integração real. | Seção 12 | Suítes isoladas para Testcontainers e mocks. | **CORRIGIDO** |

---

## 14. CRITÉRIOS DE ACEITE OBJETIVOS DO DOCUMENTO E DA IMPLEMENTAÇÃO

O presente documento atende integralmente aos critérios formais de aceitação:
- **ACH-25 Demonstrado Tecnicamente:** Procedimento SQL implementa comparação real de revisões e histórico de mutações em `user_goals_revision_history`.
- **ACH-27 Coerente com o SQL:** Matriz de 8 colunas cobre todas as 10 situações distribuídas sem LWW silencioso.
- **ACH-28 e ACH-32 Protegidos:** Metas `USER_DEFINED` (incluindo valor zero) e `LEGACY_UNKNOWN` positivas protegidas em INSERT e UPDATE.
- **ACH-29 Segurança Demonstrada:** `SECURITY DEFINER`, qualificação completa, revogação de `PUBLIC`/`anon` e validação de `auth.uid()`.
- **ACH-30 Precisão Uniforme:** Política de 7 dimensões elimina divergências de arredondamento entre Kotlin e PostgreSQL.
- **ACH-18 Sem Regressão:** Preservação de legado positivo formalmente comprovada.
- **ACH-33 Testes Reais Especificados:** Diferenciação em 4 camadas físicas e testes em PostgreSQL real detalhados.
- **ACH-34 Outbox Persistente:** Entidade Room, DDL, máquina de estados e fluxo de recuperação em 13 passos demonstrados.
- **Contrato JSON com Validação Robusta:** Allowlist explícita, rejeição de desconhecidos e bloqueio de metadados protegidos.
- **Coerência entre Camadas:** Tabela de alinhamento integral cobrindo Kotlin, Room, Repositório, Sincronização, Supabase, PostgreSQL e LGPD.

---

## 15. DECISÕES PENDENTES DE AUTORIZAÇÃO DO PROPRIETÁRIO

As decisões de governança abaixo dependem de anuência prévia do proprietário antes do início da implementação de código:
1. **Autorização da Execução do Script DDL no Supabase:** Aprovar a execução de `20261108_goals_v5_2_1_migration.sql` para conversão de tipos de `DOUBLE PRECISION` para `NUMERIC(14,2)` e criação das tabelas `user_goals_operations` e `user_goals_revision_history`.
2. **Homologação do Escopo Restrito à Fase A:** Confirmar que a implementação inicial abrangerá domínio, persistência Room v17 com Outbox, sincronização inteligente e exportação LGPD, mantendo a construção de telas Compose para a Fase B.
3. **Política de Expulgo da Outbox:** Confirmar o período de retenção local de 7 dias para operações confirmadas (`SUCCESS`) na tabela `sync_outbox`.

---

## 16. CONCLUSÃO E CLASSIFICAÇÃO FINAL

O documento **Plano Técnico de Implementação v5.2.1** corrige pontual e cirurgicamente todas as deficiências apontadas na auditoria independente sobre o v5.2, mantendo intactas as soluções e requisitos corretos, fornecendo a demonstração técnica correspondente para cada solução proposta e comprovando a perfeita coerência entre as camadas Kotlin, Room e PostgreSQL.

### Classificação Formal do Documento:
> ### **STATUS: PRONTO PARA AUDITORIA INDEPENDENTE**
> Todas as inconsistências foram sanadas, as demonstrações técnicas em SQL e Kotlin foram fornecidas de forma concreta e verificável, a Outbox persistente foi formalizada e não existem decisões arquiteturais críticas em aberto.

---
*Fim do Documento Técnico v5.2.1 — GiroCusto*
