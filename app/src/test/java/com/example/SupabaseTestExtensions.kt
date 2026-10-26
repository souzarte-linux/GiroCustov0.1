package com.example

import android.content.Context
import com.example.network.supabase.InMemorySessionStorage
import com.example.network.supabase.SupabaseClient

/**
 * Extensão de teste para reinicialização isolada de SupabaseClient com InMemorySessionStorage.
 * Vive exclusivamente em src/test.
 */
fun SupabaseClient.Companion.resetForTesting(context: Context? = null): SupabaseClient {
    val client = SupabaseClient(context?.applicationContext, storage = InMemorySessionStorage())
    SupabaseClient.setInstance(client)
    return client
}
