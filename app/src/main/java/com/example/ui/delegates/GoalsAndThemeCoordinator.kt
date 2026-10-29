package com.example.ui.delegates

import android.app.Application
import android.content.Context
import com.example.data.GiroCustoRepository
import com.example.data.UserGoals
import com.example.network.supabase.SupabaseClient
import com.example.util.CurrencyUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class GoalsAndThemeCoordinator(
    private val scope: CoroutineScope,
    private val application: Application,
    private val repository: GiroCustoRepository,
    private val supabaseClient: SupabaseClient,
    private val triggerSync: () -> Unit
) {
    private val appPrefs = application.getSharedPreferences("giro_custo_preferences", Context.MODE_PRIVATE)

    val dailyGoalGross = MutableStateFlow(200.0)
    val dailyGoalNet = MutableStateFlow(130.0)
    val dailyGoalKm = MutableStateFlow(100.0)
    val dailyGoalDeliveries = MutableStateFlow(15)

    val weeklyGoalGross = MutableStateFlow(1200.0)
    val weeklyGoalNet = MutableStateFlow(800.0)
    val weeklyGoalKm = MutableStateFlow(600.0)
    val weeklyGoalDeliveries = MutableStateFlow(90)

    val monthlyGoalGross = MutableStateFlow(5000.0)
    val monthlyGoalNet = MutableStateFlow(3200.0)
    val monthlyGoalKm = MutableStateFlow(2500.0)
    val monthlyGoalDeliveries = MutableStateFlow(400)

    private fun getInitialThemePreference(): Boolean {
        if (appPrefs.contains("is_dark_theme")) {
            return appPrefs.getBoolean("is_dark_theme", true)
        }
        val legacyPrefs = application.getSharedPreferences("giro_custo_goals", Context.MODE_PRIVATE)
        if (legacyPrefs.contains("is_dark_theme")) {
            val legacyVal = legacyPrefs.getBoolean("is_dark_theme", true)
            appPrefs.edit().putBoolean("is_dark_theme", legacyVal).apply()
            return legacyVal
        }
        return true
    }

    val isDarkTheme = MutableStateFlow(getInitialThemePreference())

    fun toggleTheme() {
        val newVal = !isDarkTheme.value
        isDarkTheme.value = newVal
        appPrefs.edit().putBoolean("is_dark_theme", newVal).apply()
    }

    init {
        scope.launch {
            try {
                repository.migrateLegacyGoalsIfNeeded(application)
            } catch (e: IllegalStateException) {
                // Previne falha de fechamento de conexão em encerramento de testes/lifecycle
            }
        }

        scope.launch {
            repository.userGoalsFlow.collect { goals ->
                if (goals != null) {
                    dailyGoalGross.value = CurrencyUtils.centsToDouble(goals.dailyGoalGross)
                    dailyGoalNet.value = CurrencyUtils.centsToDouble(goals.dailyGoalNet)
                    dailyGoalKm.value = goals.dailyGoalKm
                    dailyGoalDeliveries.value = goals.dailyGoalDeliveries

                    weeklyGoalGross.value = CurrencyUtils.centsToDouble(goals.weeklyGoalGross)
                    weeklyGoalNet.value = CurrencyUtils.centsToDouble(goals.weeklyGoalNet)
                    weeklyGoalKm.value = goals.weeklyGoalKm
                    weeklyGoalDeliveries.value = goals.weeklyGoalDeliveries

                    monthlyGoalGross.value = CurrencyUtils.centsToDouble(goals.monthlyGoalGross)
                    monthlyGoalNet.value = CurrencyUtils.centsToDouble(goals.monthlyGoalNet)
                    monthlyGoalKm.value = goals.monthlyGoalKm
                    monthlyGoalDeliveries.value = goals.monthlyGoalDeliveries
                } else {
                    dailyGoalGross.value = 200.0
                    dailyGoalNet.value = 130.0
                    dailyGoalKm.value = 100.0
                    dailyGoalDeliveries.value = 15

                    weeklyGoalGross.value = 1200.0
                    weeklyGoalNet.value = 800.0
                    weeklyGoalKm.value = 600.0
                    weeklyGoalDeliveries.value = 90

                    monthlyGoalGross.value = 5000.0
                    monthlyGoalNet.value = 3200.0
                    monthlyGoalKm.value = 2500.0
                    monthlyGoalDeliveries.value = 400
                }
            }
        }
    }

    fun updateGoals(
        dailyG: Double, dailyN: Double, dailyK: Double, dailyD: Int,
        weeklyG: Double, weeklyN: Double, weeklyK: Double, weeklyD: Int,
        monthlyG: Double, monthlyN: Double, monthlyK: Double, monthlyD: Int
    ) {
        dailyGoalGross.value = dailyG
        dailyGoalNet.value = dailyN
        dailyGoalKm.value = dailyK
        dailyGoalDeliveries.value = dailyD

        weeklyGoalGross.value = weeklyG
        weeklyGoalNet.value = weeklyN
        weeklyGoalKm.value = weeklyK
        weeklyGoalDeliveries.value = weeklyD

        monthlyGoalGross.value = monthlyG
        monthlyGoalNet.value = monthlyN
        monthlyGoalKm.value = monthlyK
        monthlyGoalDeliveries.value = monthlyD

        scope.launch {
            val userGoals = UserGoals(
                id = 1,
                dailyGoalGross = CurrencyUtils.toCents(dailyG),
                dailyGoalNet = CurrencyUtils.toCents(dailyN),
                dailyGoalKm = dailyK,
                dailyGoalDeliveries = dailyD,
                weeklyGoalGross = CurrencyUtils.toCents(weeklyG),
                weeklyGoalNet = CurrencyUtils.toCents(weeklyN),
                weeklyGoalKm = weeklyK,
                weeklyGoalDeliveries = weeklyD,
                monthlyGoalGross = CurrencyUtils.toCents(monthlyG),
                monthlyGoalNet = CurrencyUtils.toCents(monthlyN),
                monthlyGoalKm = monthlyK,
                monthlyGoalDeliveries = monthlyD
            )
            repository.updateUserGoals(userGoals)
            if (supabaseClient.isAuthenticated) {
                triggerSync()
            }
        }
    }
}
