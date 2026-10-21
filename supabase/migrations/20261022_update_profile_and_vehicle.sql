-- ==============================================================================
-- GiroCusto - Evolução da Modelagem de UserProfile e Vehicle
-- Data de Criação: 2026-10-22
-- Descrição:
--   Evolução do schema no Supabase (PostgreSQL) para suportar:
--     1. user_profile: e-mail, indicador WhatsApp, endereço completo (CEP, rua, número, bairro, UF) e status de onboarding.
--     2. vehicle: tipo de veículo, marca, ano de fabricação, capacidade do tanque, baú, bag de entrega e tipo de carroceria.
--   Todas as operações são estritamente idempotentes e compatíveis com a versão 11 do Room.
-- ==============================================================================

-- 1. Atualização da tabela user_profile
ALTER TABLE user_profile
    ADD COLUMN IF NOT EXISTS email TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS is_whatsapp BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS cep TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS street TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS address_number TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS neighborhood TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS state_uf TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS onboarding_completed BOOLEAN NOT NULL DEFAULT false;

-- Índice para verificação rápida de conclusão de onboarding
CREATE INDEX IF NOT EXISTS idx_user_profile_onboarding_completed 
ON user_profile(user_id, onboarding_completed);


-- 2. Atualização da tabela vehicle
ALTER TABLE vehicle
    ADD COLUMN IF NOT EXISTS vehicle_type TEXT NOT NULL DEFAULT 'MOTO',
    ADD COLUMN IF NOT EXISTS brand TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS manufacturingYear TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS manufacturing_year TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS tank_capacity DOUBLE PRECISION NOT NULL DEFAULT 16.0,
    ADD COLUMN IF NOT EXISTS has_top_box BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS has_delivery_bag BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS car_body_type TEXT NOT NULL DEFAULT '';

-- Limpeza preventiva caso manufacturingYear com camelCase tenha sido adicionada por engano
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'vehicle' AND column_name = 'manufacturingyear'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'vehicle' AND column_name = 'manufacturing_year'
    ) THEN
        -- Sincroniza dados se necessário e remove coluna redundante
        UPDATE vehicle SET manufacturing_year = manufacturingyear WHERE manufacturing_year = '' AND manufacturingyear != '';
        ALTER TABLE vehicle DROP COLUMN IF EXISTS manufacturingyear;
    END IF;
END $$;

-- Restrições de integridade idempotentes para vehicle
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_vehicle_tank_capacity_positive'
    ) THEN
        ALTER TABLE vehicle ADD CONSTRAINT chk_vehicle_tank_capacity_positive CHECK (tank_capacity > 0);
    END IF;
END $$;

-- Índice para consultas de veículos ativos por tipo
CREATE INDEX IF NOT EXISTS idx_vehicle_type_active 
ON vehicle(user_id, vehicle_type, active) 
WHERE deleted_at IS NULL;
