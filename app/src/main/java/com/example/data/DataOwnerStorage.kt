package com.example.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Contrato para armazenamento e controle do proprietário (user_id) dos dados locais.
 * Permite isolamento estrito entre contas no mesmo dispositivo físico sem exigir migrations no Room.
 */
interface DataOwnerStorage {
    /**
     * Retorna o UUID do proprietário atual dos dados locais ou null se não houver dono registrado.
     */
    fun getDataOwnerId(): String?

    /**
     * Define o UUID do proprietário dos dados locais.
     */
    fun setDataOwnerId(userId: String)

    /**
     * Limpa o registro do proprietário.
     */
    fun clearDataOwner()

    /**
     * Retorna true se houver um proprietário registrado.
     */
    fun hasOwner(): Boolean
}

/**
 * Implementação padrão utilizando SharedPreferences privado (modo Context.MODE_PRIVATE).
 * Registrada e preservada nas regras de backup do Android para manter coerência com o Room.
 */
class SharedPreferencesDataOwnerStorage(
    context: Context,
    prefsName: String = PREFS_NAME
) : DataOwnerStorage {

    private val prefs: SharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    override fun getDataOwnerId(): String? {
        val owner = prefs.getString(KEY_OWNER_ID, null)
        return if (owner.isNullOrBlank()) null else owner.trim()
    }

    override fun setDataOwnerId(userId: String) {
        prefs.edit().putString(KEY_OWNER_ID, userId.trim()).commit()
    }

    override fun clearDataOwner() {
        prefs.edit().remove(KEY_OWNER_ID).commit()
    }

    override fun hasOwner(): Boolean {
        return !getDataOwnerId().isNullOrBlank()
    }

    companion object {
        const val PREFS_NAME = "giro_custo_data_owner"
        const val KEY_OWNER_ID = "local_data_owner_user_id"
    }
}
