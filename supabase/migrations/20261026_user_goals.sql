-- ==============================================================================
-- GiroCusto - Tabela de Metas Sincronizadas por Conta (user_goals)
-- Data: 2026-10-26
-- Tarefa: T-28B [DAT-03]
-- Descrição:
--   Cria a tabela user_goals com chave estrangeira estrita para auth.users,
--   restrição UNIQUE por user_id (1:1), políticas RLS isoladas por proprietário,
--   trigger de atualização de timestamp e índices de performance.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS user_goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
    
    -- Metas Diárias
    daily_goal_gross DOUBLE PRECISION NOT NULL DEFAULT 200.0,
    daily_goal_net DOUBLE PRECISION NOT NULL DEFAULT 130.0,
    daily_goal_km DOUBLE PRECISION NOT NULL DEFAULT 100.0,
    daily_goal_deliveries INTEGER NOT NULL DEFAULT 15,
    
    -- Metas Semanais
    weekly_goal_gross DOUBLE PRECISION NOT NULL DEFAULT 1200.0,
    weekly_goal_net DOUBLE PRECISION NOT NULL DEFAULT 800.0,
    weekly_goal_km DOUBLE PRECISION NOT NULL DEFAULT 600.0,
    weekly_goal_deliveries INTEGER NOT NULL DEFAULT 90,
    
    -- Metas Mensais
    monthly_goal_gross DOUBLE PRECISION NOT NULL DEFAULT 5000.0,
    monthly_goal_net DOUBLE PRECISION NOT NULL DEFAULT 3200.0,
    monthly_goal_km DOUBLE PRECISION NOT NULL DEFAULT 2500.0,
    monthly_goal_deliveries INTEGER NOT NULL DEFAULT 400,
    
    -- Timestamps e Soft Delete
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    deleted_at TIMESTAMPTZ NULL
);

-- 1. Ativação Obrigatória de Row Level Security (RLS)
ALTER TABLE user_goals ENABLE ROW LEVEL SECURITY;

-- 2. Políticas RLS: Apenas o proprietário autenticado pode ler e alterar suas metas
DROP POLICY IF EXISTS "Users can manage their own goals" ON user_goals;
CREATE POLICY "Users can manage their own goals"
ON user_goals
FOR ALL
TO authenticated
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id);

-- 3. Trigger para manter updated_at sempre atualizado no servidor
DROP TRIGGER IF EXISTS trigger_user_goals_set_updated_at ON user_goals;
CREATE TRIGGER trigger_user_goals_set_updated_at
BEFORE UPDATE ON user_goals
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

-- 4. Índices de Otimização
CREATE INDEX IF NOT EXISTS idx_user_goals_user_id ON user_goals(user_id);
CREATE INDEX IF NOT EXISTS idx_user_goals_updated_at ON user_goals(updated_at);
