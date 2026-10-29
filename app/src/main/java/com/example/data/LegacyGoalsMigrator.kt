package com.example.data

import android.content.Context
import android.util.Log
import com.example.util.CurrencyUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

object LegacyGoalsMigrator {

    suspend fun migrate(
        db: GiroCustoDatabase,
        context: Context,
        dispatcher: CoroutineDispatcher
    ) = withContext(dispatcher) {
        val userGoalsDao = db.userGoalsDao()
        try {
            if (userGoalsDao.getUserGoals() != null) return@withContext
            val legacyPrefs = context.getSharedPreferences("giro_custo_goals", Context.MODE_PRIVATE)
            val hasLegacyGoals = legacyPrefs.contains("daily_goal_gross")
            val goals = if (hasLegacyGoals) {
                UserGoals(
                    id = 1,
                    dailyGoalGross = CurrencyUtils.toCents(legacyPrefs.getFloat("daily_goal_gross", 200.0f).toDouble()),
                    dailyGoalNet = CurrencyUtils.toCents(legacyPrefs.getFloat("daily_goal_net", 130.0f).toDouble()),
                    dailyGoalKm = legacyPrefs.getFloat("daily_goal_km", 100.0f).toDouble(),
                    dailyGoalDeliveries = legacyPrefs.getInt("daily_goal_deliveries", 15),
                    weeklyGoalGross = CurrencyUtils.toCents(legacyPrefs.getFloat("weekly_goal_gross", 1200.0f).toDouble()),
                    weeklyGoalNet = CurrencyUtils.toCents(legacyPrefs.getFloat("weekly_goal_net", 800.0f).toDouble()),
                    weeklyGoalKm = legacyPrefs.getFloat("weekly_goal_km", 600.0f).toDouble(),
                    weeklyGoalDeliveries = legacyPrefs.getInt("weekly_goal_deliveries", 90),
                    monthlyGoalGross = CurrencyUtils.toCents(legacyPrefs.getFloat("monthly_goal_gross", 5000.0f).toDouble()),
                    monthlyGoalNet = CurrencyUtils.toCents(legacyPrefs.getFloat("monthly_goal_net", 3200.0f).toDouble()),
                    monthlyGoalKm = legacyPrefs.getFloat("monthly_goal_km", 2500.0f).toDouble(),
                    monthlyGoalDeliveries = legacyPrefs.getInt("monthly_goal_deliveries", 400),
                    syncStatus = SyncStatus.PENDING_UPDATE,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                UserGoals(
                    id = 1,
                    syncStatus = SyncStatus.SYNCED
                )
            }
            userGoalsDao.insertOrUpdateUserGoals(goals)

            if (legacyPrefs.contains("is_dark_theme")) {
                val isDark = legacyPrefs.getBoolean("is_dark_theme", true)
                context.getSharedPreferences("giro_custo_preferences", Context.MODE_PRIVATE)
                    .edit().putBoolean("is_dark_theme", isDark).apply()
                context.getSharedPreferences("giro_custo_app_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("is_dark_theme", isDark).apply()
            }
            legacyPrefs.edit().clear().apply()
        } catch (e: IllegalStateException) {
            // DB pool fechado durante o encerramento do app ou teardown de testes
            Log.d("GiroCustoRepo", "Conexão de banco indisponível durante migração de metas: ${e.message}")
        }
    }
}
