package com.example.network.supabase

import android.content.Context

/**
 * Extensão utilitária de testes para reinicialização isolada do SupabaseClient com InMemorySessionStorage.
 * Vive exclusivamente em src/test, proibido em código de produção.
 */
fun SupabaseClient.Companion.resetForTesting(context: Context? = null): SupabaseClient {
    val client = SupabaseClient(context?.applicationContext, storage = InMemorySessionStorage())
    SupabaseClient.setInstance(client)
    return client
}
