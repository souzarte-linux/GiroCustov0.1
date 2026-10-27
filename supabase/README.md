# Supabase Schema - GiroCusto

Este diretório contém os scripts de migração DDL e a arquitetura técnica do banco de dados remoto no Supabase (PostgreSQL) para o aplicativo **GiroCusto**.

---

## 1. Estrutura do Schema

O schema espelha as 7 entidades Room do GiroCusto (versão 9) com suporte completo a sincronização **offline-first**, resolução de conflito **Last-Write-Wins (LWW)** e **Soft Deletes**:

| Tabela | Descrição | Chave Primária | Relações Chave |
| :--- | :--- | :--- | :--- |
| `vehicle` | Dados do veículo cadastrado, consumo médio e custos fixos | `id UUID` | `user_id -> auth.users.id` |
| `vehicle_parts` | Peças, durabilidade prevista e km rodado para cálculo de desgaste | `id UUID` | `user_id`, `vehicle_id -> vehicle.id` |
| `daily_records` | Registros diários de trabalho, hodômetros, faturamento bruto e lucro líquido | `id UUID` | `user_id -> auth.users.id` |
| `user_profile` | Perfil do motorista/entregador (nome, telefone, cidade) | `id UUID` | `user_id -> auth.users.id (UNIQUE)` |
| `platforms` | Configurações de plataformas (iFood, Uber, etc.), modelos de repasse e ciclos | `id UUID` | `user_id -> auth.users.id` |
| `fuel_refills` | Histórico detalhado de abastecimentos, litragem, valores e cálculo de tanque | `id UUID` | `user_id`, `vehicle_id -> vehicle.id` |
| `maintenance_records` | Registros de oficina, serviços realizados e odômetro da manutenção | `id UUID` | `user_id`, `vehicle_id -> vehicle.id` |
| `user_goals` | Metas operacionais e financeiras sincronizadas por conta (12 metas: diárias, semanais e mensais) | `id UUID` | `user_id -> auth.users.id (UNIQUE)` |

---

## 2. Padrões de Arquitetura Aplicados

1. **UUIDv4 (`gen_random_uuid()`):** Permite a geração de IDs únicos tanto no SQLite/Room local (via `UUID.randomUUID().toString()`) quanto no Supabase remoto, eliminando colisões de autoincrement integer.
2. **Multi-tenancy com Isolamento RLS:**
   - Todas as tabelas têm `user_id UUID NOT NULL DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE`.
   - `ROW LEVEL SECURITY` habilitado em todas as tabelas.
   - Política `ALL` permitindo operações exclusivamente para `auth.uid() = user_id`.
3. **Colunas de Sincronização e Resolução de Conflitos (LWW):**
   - `created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())`
   - `updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())`
   - `deleted_at TIMESTAMPTZ NULL` (Tombstone para deleções lógicas que precisam propagar para outros dispositivos do mesmo usuário)
4. **Trigger `set_updated_at`:**
   - Disparado antes de cada `UPDATE` para manter `updated_at` sempre atualizado com o timestamp do servidor.
5. **Índices de Performance:**
   - Índices em `(user_id)` e `(updated_at)` em todas as tabelas para suportar consultas de sincronização delta (`SELECT * FROM table WHERE user_id = :uid AND updated_at > :lastSyncTimestamp`).
   - Índices em colunas de ordenação e filtro frequente (`date_string`, `date_timestamp`, `vehicle_id`, `active`).

---

## 3. Como Aplicar a Migração no Supabase

### Opção A: Via Supabase SQL Editor (Web Console)
1. Acesse o painel do seu projeto no [Supabase Dashboard](https://supabase.com/dashboard).
2. No menu lateral esquerdo, clique em **SQL Editor**.
3. Clique em **+ New Query**.
4. Copie todo o conteúdo do arquivo [`migrations/20261021_init_girocusto_schema.sql`](./migrations/20261021_init_girocusto_schema.sql) e cole no editor.
5. Clique no botão **Run** (ou pressione `Ctrl + Enter` / `Cmd + Enter`).
6. Verifique se o resultado exibe `Success. No rows returned`.

### Opção B: Via Supabase CLI
Se você possui a Supabase CLI configurada localmente:
```bash
# Linkar ao projeto remoto (se ainda não vinculado)
supabase link --project-ref <SEU_PROJECT_REF>

# Aplicar as migrações locais
supabase db push
```

---

## 4. Integração com a Migração Room (MIGRATION_9_10)

Para suportar o sincronizador bidirecional no Android:
1. As entidades Room locais passarão a ter:
   - `remoteId: String` (UUID) ou migrar a PK local para UUID.
   - `updatedAt: Long` (timestamp para Last-Write-Wins).
   - `isSynced: Boolean` (flag local para indicar alterações pendentes).
   - `isDeleted: Boolean` (soft delete local correspondente ao `deleted_at`).
2. O repositório sincronizador fará:
   - **Upload (Push):** Envia registros locais com `isSynced = false` para o Supabase (Upsert).
   - **Download (Pull):** Busca no Supabase registros com `updated_at > lastSyncTimestamp` e atualiza a base Room local.

---

## 5. Migração de Metas Sincronizadas por Conta (T-28B - `user_goals`)

O arquivo [`migrations/20261026_user_goals.sql`](./migrations/20261026_user_goals.sql) implementa a persistência e sincronização de metas de faturamento, lucro, quilometragem e entregas (diárias, semanais e mensais):

- **Isolamento Multi-tenant:** Chave estrangeira `user_id UUID NOT NULL UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE`.
- **Row Level Security (RLS):** Política `Users can manage their own goals` (`auth.uid() = user_id`) para SELECT, INSERT, UPDATE, DELETE.
- **Trigger `trigger_user_goals_set_updated_at`:** Atualiza automaticamente o campo `updated_at` a cada UPDATE.
- **Índices:** `idx_user_goals_user_id` e `idx_user_goals_updated_at`.
- **Aplicação:** Executar o script `migrations/20261026_user_goals.sql` no SQL Editor do Supabase Dashboard.

