-- ==============================================================================
-- GiroCusto - Procedimento Remoto de Exclusão Segura de Conta (LGPD)
-- Data: 2026-10-27
-- Tarefa: T-10 [SEC-06]
-- Descrição:
--   Implementa a RPC delete_user_account() com SECURITY DEFINER para expurgo
--   definitivo dos dados do motorista e da sua respectiva conta auth.users.
--   Acesso estritamente restrito a usuários autenticados (auth.uid()).
-- ==============================================================================

CREATE OR REPLACE FUNCTION delete_user_account()
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_user_id UUID;
BEGIN
    -- Captura o ID do usuário que está chamando a função a partir do JWT autenticado
    v_user_id := auth.uid();
    
    -- Bloqueio defensivo fail-safe: chamada anônima é proibida
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Operação não permitida: usuário não autenticado';
    END IF;

    -- 1. Exclusão explícita de todas as tabelas operacionais associadas ao usuário
    DELETE FROM public.user_goals WHERE user_id = v_user_id;
    DELETE FROM public.maintenance_records WHERE user_id = v_user_id;
    DELETE FROM public.fuel_refills WHERE user_id = v_user_id;
    DELETE FROM public.daily_records WHERE user_id = v_user_id;
    DELETE FROM public.platforms WHERE user_id = v_user_id;
    DELETE FROM public.vehicle_parts WHERE user_id = v_user_id;
    DELETE FROM public.vehicle WHERE user_id = v_user_id;
    DELETE FROM public.user_profile WHERE user_id = v_user_id;

    -- 2. Exclusão definitiva do usuário no sistema de autenticação Supabase
    DELETE FROM auth.users WHERE id = v_user_id;
END;
$$;

-- 3. Blindagem de Permissões: Revoga acesso público e anônimo, concede somente a authenticated
REVOKE ALL ON FUNCTION delete_user_account() FROM PUBLIC;
REVOKE ALL ON FUNCTION delete_user_account() FROM anon;
GRANT EXECUTE ON FUNCTION delete_user_account() TO authenticated;
