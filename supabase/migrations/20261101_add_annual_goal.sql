-- ==============================================================================
-- GiroCusto - Adição de Meta Anual (annual_goal_net) à tabela user_goals
-- Data: 2026-11-01
-- Descrição:
--   Adiciona a coluna annual_goal_net à tabela user_goals para sincronização
--   da meta anual de lucro líquido do usuário.
-- ==============================================================================

ALTER TABLE user_goals 
ADD COLUMN IF NOT EXISTS annual_goal_net DOUBLE PRECISION NOT NULL DEFAULT 0.0;
