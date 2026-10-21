-- ==============================================================================
-- GiroCusto - Inicialização do Schema no Supabase (PostgreSQL)
-- Data de Criação: 2026-10-21
-- Descrição:
--   Criação das 7 tabelas base do GiroCusto compatíveis com Room v9/v10:
--     1. vehicle (veículos)
--     2. vehicle_parts (peças e desgaste)
--     3. daily_records (diárias de trabalho e faturamento)
--     4. user_profile (perfil do usuário)
--     5. platforms (plataformas de entrega/transporte)
--     6. fuel_refills (abastecimentos e consumo)
--     7. maintenance_records (manutenções e histórico)
--   
--   Todos os registros possuem:
--     - Chave primária UUID (id)
--     - Vínculo direto ao usuário do Supabase Auth (user_id -> auth.users.id)
--     - Metadados de sincronização e LWW (created_at, updated_at, deleted_at)
--     - Trigger automático de atualização de updated_at
--     - Row Level Security (RLS) restrito ao proprietário do registro
--     - Índices de alta performance para sincronização e consultas
-- ==============================================================================

-- 1. Extensões necessárias
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 2. Função reutilizável para atualizar updated_at automaticamente
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = timezone('utc'::text, now());
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ==============================================================================
-- TABELA 1: vehicle
-- ==============================================================================
CREATE TABLE IF NOT EXISTS vehicle (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE,
    model TEXT NOT NULL DEFAULT 'Honda CG 160 Titan',
    average_consumption DOUBLE PRECISION NOT NULL DEFAULT 40.0 CHECK (average_consumption > 0),
    fuel_type TEXT NOT NULL DEFAULT 'Gasolina',
    monthly_fixed_costs DOUBLE PRECISION NOT NULL DEFAULT 180.0 CHECK (monthly_fixed_costs >= 0),
    planned_work_days INTEGER NOT NULL DEFAULT 22 CHECK (planned_work_days > 0 AND planned_work_days <= 31),
    current_odometer DOUBLE PRECISION NOT NULL DEFAULT 15350.0 CHECK (current_odometer >= 0),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    deleted_at TIMESTAMPTZ NULL
);

-- RLS & Políticas
ALTER TABLE vehicle ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage their own vehicle"
ON vehicle
FOR ALL
TO authenticated
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id);

-- Trigger de updated_at
CREATE TRIGGER trigger_vehicle_set_updated_at
BEFORE UPDATE ON vehicle
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

-- Índices
CREATE INDEX IF NOT EXISTS idx_vehicle_user_id ON vehicle(user_id);
CREATE INDEX IF NOT EXISTS idx_vehicle_updated_at ON vehicle(updated_at);
CREATE INDEX IF NOT EXISTS idx_vehicle_user_active ON vehicle(user_id, active) WHERE deleted_at IS NULL;


-- ==============================================================================
-- TABELA 2: vehicle_parts
-- ==============================================================================
CREATE TABLE IF NOT EXISTS vehicle_parts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE,
    vehicle_id UUID NULL REFERENCES vehicle(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    price DOUBLE PRECISION NOT NULL DEFAULT 0.0 CHECK (price >= 0),
    lifespan_km DOUBLE PRECISION NOT NULL DEFAULT 0.0 CHECK (lifespan_km >= 0),
    run_km_since_change DOUBLE PRECISION NOT NULL DEFAULT 0.0 CHECK (run_km_since_change >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    deleted_at TIMESTAMPTZ NULL
);

-- RLS & Políticas
ALTER TABLE vehicle_parts ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage their own vehicle parts"
ON vehicle_parts
FOR ALL
TO authenticated
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id);

-- Trigger de updated_at
CREATE TRIGGER trigger_vehicle_parts_set_updated_at
BEFORE UPDATE ON vehicle_parts
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

-- Índices
CREATE INDEX IF NOT EXISTS idx_vehicle_parts_user_id ON vehicle_parts(user_id);
CREATE INDEX IF NOT EXISTS idx_vehicle_parts_updated_at ON vehicle_parts(updated_at);
CREATE INDEX IF NOT EXISTS idx_vehicle_parts_vehicle_id ON vehicle_parts(vehicle_id);


-- ==============================================================================
-- TABELA 3: daily_records
-- ==============================================================================
CREATE TABLE IF NOT EXISTS daily_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE,
    date_string TEXT NOT NULL,
    date_timestamp BIGINT NOT NULL,
    platform TEXT NOT NULL DEFAULT 'Geral',
    gross_earnings DOUBLE PRECISION NOT NULL DEFAULT 0.0 CHECK (gross_earnings >= 0),
    deliveries_count INTEGER NOT NULL DEFAULT 0 CHECK (deliveries_count >= 0),
    start_odometer DOUBLE PRECISION NOT NULL DEFAULT 0.0 CHECK (start_odometer >= 0),
    end_odometer DOUBLE PRECISION NOT NULL DEFAULT 0.0 CHECK (end_odometer >= 0),
    fuel_price DOUBLE PRECISION NOT NULL DEFAULT 0.0 CHECK (fuel_price >= 0),
    food_expense DOUBLE PRECISION NOT NULL DEFAULT 0.0 CHECK (food_expense >= 0),
    fuel_cost DOUBLE PRECISION NOT NULL DEFAULT 0.0 CHECK (fuel_cost >= 0),
    wear_cost DOUBLE PRECISION NOT NULL DEFAULT 0.0 CHECK (wear_cost >= 0),
    proportional_fixed_cost DOUBLE PRECISION NOT NULL DEFAULT 0.0 CHECK (proportional_fixed_cost >= 0),
    net_profit DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    start_time TEXT NOT NULL DEFAULT '',
    end_time TEXT NOT NULL DEFAULT '',
    pause_duration TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    deleted_at TIMESTAMPTZ NULL
);

-- RLS & Políticas
ALTER TABLE daily_records ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage their own daily records"
ON daily_records
FOR ALL
TO authenticated
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id);

-- Trigger de updated_at
CREATE TRIGGER trigger_daily_records_set_updated_at
BEFORE UPDATE ON daily_records
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

-- Índices
CREATE INDEX IF NOT EXISTS idx_daily_records_user_id ON daily_records(user_id);
CREATE INDEX IF NOT EXISTS idx_daily_records_updated_at ON daily_records(updated_at);
CREATE INDEX IF NOT EXISTS idx_daily_records_user_date ON daily_records(user_id, date_string DESC);
CREATE INDEX IF NOT EXISTS idx_daily_records_user_timestamp ON daily_records(user_id, date_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_daily_records_user_platform ON daily_records(user_id, platform);


-- ==============================================================================
-- TABELA 4: user_profile
-- ==============================================================================
CREATE TABLE IF NOT EXISTS user_profile (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL DEFAULT '',
    phone TEXT NOT NULL DEFAULT '',
    city TEXT NOT NULL DEFAULT '',
    platforms TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    deleted_at TIMESTAMPTZ NULL
);

-- RLS & Políticas
ALTER TABLE user_profile ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage their own profile"
ON user_profile
FOR ALL
TO authenticated
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id);

-- Trigger de updated_at
CREATE TRIGGER trigger_user_profile_set_updated_at
BEFORE UPDATE ON user_profile
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

-- Índices
CREATE INDEX IF NOT EXISTS idx_user_profile_user_id ON user_profile(user_id);
CREATE INDEX IF NOT EXISTS idx_user_profile_updated_at ON user_profile(updated_at);


-- ==============================================================================
-- TABELA 5: platforms
-- ==============================================================================
CREATE TABLE IF NOT EXISTS platforms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    segment TEXT NOT NULL DEFAULT 'logistica',
    payment_model TEXT NOT NULL DEFAULT 'producao',
    cycle TEXT NOT NULL DEFAULT 'semanal',
    payment_day TEXT NOT NULL DEFAULT 'QUA',
    fixed_pay_delay INTEGER NOT NULL DEFAULT 7 CHECK (fixed_pay_delay >= 0),
    cycle_entries_json TEXT NOT NULL DEFAULT '1:7,16:7',
    bank_name TEXT NOT NULL DEFAULT '',
    bank_agency TEXT NOT NULL DEFAULT '',
    bank_account TEXT NOT NULL DEFAULT '',
    pix_key_type TEXT NOT NULL DEFAULT 'CPF',
    pix_key TEXT NOT NULL DEFAULT '',
    color_hex TEXT NOT NULL DEFAULT '#1976D2',
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    deleted_at TIMESTAMPTZ NULL
);

-- RLS & Políticas
ALTER TABLE platforms ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage their own platforms"
ON platforms
FOR ALL
TO authenticated
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id);

-- Trigger de updated_at
CREATE TRIGGER trigger_platforms_set_updated_at
BEFORE UPDATE ON platforms
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

-- Índices
CREATE INDEX IF NOT EXISTS idx_platforms_user_id ON platforms(user_id);
CREATE INDEX IF NOT EXISTS idx_platforms_updated_at ON platforms(updated_at);
CREATE INDEX IF NOT EXISTS idx_platforms_user_active ON platforms(user_id, active) WHERE deleted_at IS NULL;


-- ==============================================================================
-- TABELA 6: fuel_refills
-- ==============================================================================
CREATE TABLE IF NOT EXISTS fuel_refills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE,
    vehicle_id UUID NULL REFERENCES vehicle(id) ON DELETE CASCADE,
    date_timestamp BIGINT NOT NULL,
    date_string TEXT NOT NULL,
    station_name TEXT NOT NULL DEFAULT '',
    station_address TEXT NOT NULL DEFAULT '',
    gas_station TEXT NOT NULL DEFAULT '',
    fuel_type TEXT NOT NULL DEFAULT 'Gasolina',
    liters DOUBLE PRECISION NOT NULL DEFAULT 0.0 CHECK (liters >= 0),
    price_per_liter DOUBLE PRECISION NOT NULL DEFAULT 0.0 CHECK (price_per_liter >= 0),
    discount DOUBLE PRECISION NOT NULL DEFAULT 0.0 CHECK (discount >= 0),
    total_paid DOUBLE PRECISION NOT NULL DEFAULT 0.0 CHECK (total_paid >= 0),
    odometer DOUBLE PRECISION NOT NULL DEFAULT 0.0 CHECK (odometer >= 0),
    full_tank BOOLEAN NOT NULL DEFAULT true,
    is_full_tank BOOLEAN NOT NULL DEFAULT true,
    payment_method TEXT NOT NULL DEFAULT 'Dinheiro',
    is_installment BOOLEAN NOT NULL DEFAULT false,
    installments_count INTEGER NOT NULL DEFAULT 1 CHECK (installments_count >= 1),
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    deleted_at TIMESTAMPTZ NULL
);

-- RLS & Políticas
ALTER TABLE fuel_refills ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage their own fuel refills"
ON fuel_refills
FOR ALL
TO authenticated
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id);

-- Trigger de updated_at
CREATE TRIGGER trigger_fuel_refills_set_updated_at
BEFORE UPDATE ON fuel_refills
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

-- Índices
CREATE INDEX IF NOT EXISTS idx_fuel_refills_user_id ON fuel_refills(user_id);
CREATE INDEX IF NOT EXISTS idx_fuel_refills_updated_at ON fuel_refills(updated_at);
CREATE INDEX IF NOT EXISTS idx_fuel_refills_user_vehicle ON fuel_refills(user_id, vehicle_id);
CREATE INDEX IF NOT EXISTS idx_fuel_refills_timestamp ON fuel_refills(user_id, date_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_fuel_refills_odometer ON fuel_refills(user_id, vehicle_id, odometer ASC);


-- ==============================================================================
-- TABELA 7: maintenance_records
-- ==============================================================================
CREATE TABLE IF NOT EXISTS maintenance_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE,
    vehicle_id UUID NULL REFERENCES vehicle(id) ON DELETE CASCADE,
    date_timestamp BIGINT NOT NULL,
    date_string TEXT NOT NULL,
    description TEXT NOT NULL,
    location TEXT NOT NULL DEFAULT '',
    value DOUBLE PRECISION NOT NULL DEFAULT 0.0 CHECK (value >= 0),
    odometer DOUBLE PRECISION NOT NULL DEFAULT 0.0 CHECK (odometer >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    deleted_at TIMESTAMPTZ NULL
);

-- RLS & Políticas
ALTER TABLE maintenance_records ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage their own maintenance records"
ON maintenance_records
FOR ALL
TO authenticated
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id);

-- Trigger de updated_at
CREATE TRIGGER trigger_maintenance_records_set_updated_at
BEFORE UPDATE ON maintenance_records
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

-- Índices
CREATE INDEX IF NOT EXISTS idx_maintenance_records_user_id ON maintenance_records(user_id);
CREATE INDEX IF NOT EXISTS idx_maintenance_records_updated_at ON maintenance_records(updated_at);
CREATE INDEX IF NOT EXISTS idx_maintenance_records_user_vehicle ON maintenance_records(user_id, vehicle_id);
CREATE INDEX IF NOT EXISTS idx_maintenance_records_timestamp ON maintenance_records(user_id, date_timestamp DESC);
