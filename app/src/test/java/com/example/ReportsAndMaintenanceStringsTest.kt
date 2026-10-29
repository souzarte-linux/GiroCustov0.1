package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes unitários para validar a integridade de internacionalização (i18n) e acessibilidade
 * das strings de Relatórios (ReportsScreen) e Manutenção Corretiva (MaintenanceScreen) - Tarefa T-25E [ACC-02C].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReportsAndMaintenanceStringsTest {

    @Test
    fun testAllReportsAndMaintenanceStringsExistAndAreNonEmpty() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        val stringResIds = listOf(
            // Relatórios - Tela Principal, Períodos e Content Descriptions
            R.string.reports_title,
            R.string.reports_period_prefix_week,
            R.string.reports_period_prefix_fortnight,
            R.string.reports_period_prefix_month,
            R.string.reports_period_prefix_last_30_days,
            R.string.reports_period_custom_range,
            R.string.reports_period_week,
            R.string.reports_period_fortnight,
            R.string.reports_period_month,
            R.string.reports_period_last_30_days,
            R.string.reports_period_custom,
            R.string.reports_cd_select_period,
            R.string.reports_cd_toggle_theme,
            R.string.reports_chart_profit_title,
            R.string.reports_chart_profit_subtitle,
            R.string.reports_chart_profit_empty,
            R.string.reports_chart_productivity_title,
            R.string.reports_chart_productivity_subtitle,
            R.string.reports_legend_gross,
            R.string.reports_legend_net,
            R.string.reports_chart_productivity_empty,

            // Relatórios - Distribuição de Despesas
            R.string.reports_expense_distribution_title,
            R.string.reports_expense_distribution_subtitle,
            R.string.reports_expense_cat_fuel,
            R.string.reports_expense_cat_food,
            R.string.reports_expense_cat_wear,
            R.string.reports_expense_cat_fixed,
            R.string.reports_expense_cat_maintenance,
            R.string.reports_expense_empty,
            R.string.reports_expense_legend_value,

            // Relatórios - Projeções e Economia Acumulada
            R.string.reports_projections_icon_desc,
            R.string.reports_projections_title,
            R.string.reports_projections_simulation_desc,
            R.string.reports_projections_gross_label,
            R.string.reports_projections_net_label,
            R.string.reports_projections_goal_progress,
            R.string.reports_projections_goal_label,

            // Relatórios - Jornada e Destaques
            R.string.reports_workhours_icon_desc,
            R.string.reports_workhours_title,
            R.string.reports_workhours_subtitle,
            R.string.reports_workhours_net_per_hour_label,
            R.string.reports_workhours_gross_per_hour_label,
            R.string.reports_workhours_rate_per_hour,
            R.string.reports_workhours_effective_time_label,
            R.string.reports_workhours_pause_time_label,
            R.string.reports_workhours_avg_shift_label,
            R.string.reports_workhours_avg_shift_value,
            R.string.reports_highlights_section_title,
            R.string.reports_highlights_best_shift_icon_desc,
            R.string.reports_highlights_best_shift_label,
            R.string.reports_highlights_worst_shift_icon_desc,
            R.string.reports_highlights_worst_shift_label,
            R.string.reports_highlights_shift_fallback,
            R.string.reports_highlights_on_day,

            // Manutenção Corretiva - Alertas, Lista e Resumo
            R.string.maintenance_title,
            R.string.maintenance_cd_add,
            R.string.maintenance_btn_add,
            R.string.maintenance_no_vehicle_alert_cd,
            R.string.maintenance_no_vehicle_title,
            R.string.maintenance_no_vehicle_desc,
            R.string.maintenance_summary_total_label,
            R.string.maintenance_empty_message,
            R.string.maintenance_odometer_format,

            // Manutenção Corretiva - Formulário, Diálogos e Toasts
            R.string.maintenance_cd_back,
            R.string.maintenance_form_title_edit,
            R.string.maintenance_form_title_new,
            R.string.maintenance_form_desc_label,
            R.string.maintenance_form_value_label,
            R.string.maintenance_form_odometer_label,
            R.string.maintenance_form_location_label,
            R.string.maintenance_form_date_label,
            R.string.maintenance_btn_delete,
            R.string.maintenance_btn_save,
            R.string.maintenance_toast_updated,
            R.string.maintenance_toast_created,
            R.string.maintenance_toast_deleted,
            R.string.maintenance_toast_error_desc,
            R.string.maintenance_toast_error_value,
            R.string.maintenance_toast_error_odometer
        )

        for (resId in stringResIds) {
            val str = app.getString(resId)
            assertNotNull("Resource $resId não deve ser nulo", str)
            assertFalse("Resource $resId não deve ser vazio", str.isBlank())
        }
    }

    @Test
    fun testInterpolatedReportsStrings() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        val customRangeStr = app.getString(R.string.reports_period_custom_range, "01/10", "15/10")
        assertTrue(customRangeStr.contains("01/10") && customRangeStr.contains("15/10"))

        val expenseLegendStr = app.getString(R.string.reports_expense_legend_value, "R$ 150,00", 35)
        assertTrue(expenseLegendStr.contains("R$ 150,00") && expenseLegendStr.contains("35%"))

        val simulationDescStr = app.getString(R.string.reports_projections_simulation_desc, 22)
        assertTrue(simulationDescStr.contains("22 dias"))

        val goalProgressStr = app.getString(R.string.reports_projections_goal_progress, 85)
        assertTrue(goalProgressStr.contains("85%") && goalProgressStr.contains("meta"))

        val goalLabelStr = app.getString(R.string.reports_projections_goal_label, "R$ 3.000")
        assertTrue(goalLabelStr.contains("Meta:") && goalLabelStr.contains("R$ 3.000"))

        val ratePerHourStr = app.getString(R.string.reports_workhours_rate_per_hour, "R$ 42,50")
        assertTrue(ratePerHourStr.contains("R$ 42,50") && ratePerHourStr.contains("/h"))

        val avgShiftStr = app.getString(R.string.reports_workhours_avg_shift_value, "7,5")
        assertTrue(avgShiftStr.contains("7,5h"))

        val onDayStr = app.getString(R.string.reports_highlights_on_day, "25/10")
        assertTrue(onDayStr.contains("no dia 25/10"))
    }

    @Test
    fun testInterpolatedMaintenanceStrings() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        val odometerStr = app.getString(R.string.maintenance_odometer_format, "12.500,0")
        assertTrue(odometerStr.contains("12.500,0 km"))
    }
}
