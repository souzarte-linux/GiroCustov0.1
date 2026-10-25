package com.example.network.supabase

/**
 * Exceção tipada lançada quando uma operação de sincronização (PUSH ou PULL) é tentada
 * com um usuário diferente do proprietário legítimo dos dados armazenados localmente.
 */
class DataOwnerMismatchException(
    val ownerId: String?,
    val currentUserId: String,
    val operation: String
) : IllegalStateException(
    if (ownerId != null) {
        "Operação de sincronização ($operation) bloqueada por segurança: os registros salvos neste aparelho pertencem a outro usuário ($ownerId). Usuário logado: $currentUserId."
    } else {
        "Operação de sincronização ($operation) bloqueada por segurança: existem registros locais sem proprietário definido. Usuário logado: $currentUserId."
    }
)
