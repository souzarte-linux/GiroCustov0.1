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
 * das strings da tela de Perfil e seus subcomponentes (T-25D [ACC-02D]).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfileScreenStringsTest {

    @Test
    fun testAllProfileStringsExistAndAreNonEmpty() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        val stringResIds = listOf(
            R.string.profile_title,
            R.string.profile_subtitle,
            R.string.profile_toast_vehicle_updated,
            R.string.profile_toast_vehicle_created,
            R.string.profile_toast_vehicle_activated,
            R.string.profile_toast_part_wear_reset,
            R.string.profile_toast_part_added,
            R.string.profile_toast_part_updated,
            R.string.profile_toast_cadastral_saved,
            R.string.profile_cadastral_card_title,
            R.string.profile_cadastral_name_label,
            R.string.profile_cadastral_name_placeholder,
            R.string.profile_cadastral_phone_label,
            R.string.profile_cadastral_phone_placeholder,
            R.string.profile_cadastral_whatsapp_title,
            R.string.profile_cadastral_whatsapp_desc,
            R.string.profile_cadastral_whatsapp_icon_desc,
            R.string.profile_cadastral_cep_label,
            R.string.profile_cadastral_cep_optional_label,
            R.string.profile_cadastral_cep_placeholder,
            R.string.profile_cd_cep_found,
            R.string.profile_cd_cep_error,
            R.string.profile_cadastral_street_label,
            R.string.profile_cadastral_street_placeholder,
            R.string.profile_cadastral_number_label,
            R.string.profile_cadastral_number_placeholder,
            R.string.profile_cadastral_neighborhood_label,
            R.string.profile_cadastral_neighborhood_placeholder,
            R.string.profile_cadastral_city_label,
            R.string.profile_cadastral_city_placeholder,
            R.string.profile_cadastral_uf_label,
            R.string.profile_cadastral_uf_placeholder,
            R.string.profile_cadastral_save_btn,
            R.string.profile_platforms_card_title,
            R.string.profile_platforms_manage_btn,
            R.string.profile_platforms_empty_message,
            R.string.profile_platforms_register_btn,
            R.string.profile_vehicles_section_title,
            R.string.profile_vehicles_btn_new,
            R.string.profile_vehicles_empty_title,
            R.string.profile_vehicles_empty_desc,
            R.string.profile_vehicles_badge_topbox,
            R.string.profile_vehicles_badge_bag,
            R.string.profile_vehicles_badge_active,
            R.string.profile_vehicles_btn_activate,
            R.string.profile_vehicles_metric_consumption,
            R.string.profile_vehicles_metric_tank_odometer,
            R.string.profile_vehicles_metric_fixed_costs,
            R.string.profile_parts_title,
            R.string.profile_parts_title_with_vehicle,
            R.string.profile_parts_subtitle,
            R.string.profile_parts_subtitle_with_vehicle,
            R.string.profile_parts_empty_fallback_vehicle,
            R.string.profile_parts_empty_message,
            R.string.profile_parts_item_meta,
            R.string.profile_parts_item_usage,
            R.string.profile_parts_item_health,
            R.string.profile_parts_dialog_add_title,
            R.string.profile_parts_dialog_edit_title,
            R.string.profile_parts_dialog_associated_vehicle,
            R.string.profile_parts_dialog_name_label_hint,
            R.string.profile_parts_dialog_name_label,
            R.string.profile_parts_dialog_price_label,
            R.string.profile_parts_dialog_lifespan_label,
            R.string.profile_parts_dialog_run_km_label,
            R.string.profile_parts_dialog_add_btn,
            R.string.profile_fipe_section_title,
            R.string.profile_fipe_brand_label,
            R.string.profile_fipe_brand_placeholder,
            R.string.profile_fipe_loading_brands,
            R.string.profile_fipe_model_label,
            R.string.profile_fipe_model_placeholder,
            R.string.profile_fipe_loading_models,
            R.string.profile_fipe_year_label,
            R.string.profile_fipe_year_placeholder,
            R.string.profile_fipe_loading_years,
            R.string.profile_fipe_card_value_title,
            R.string.profile_fipe_card_code,
            R.string.profile_fipe_card_ref,
            R.string.profile_fipe_consulting_base,
            R.string.profile_vehicle_form_title_edit,
            R.string.profile_vehicle_form_title_new,
            R.string.profile_vehicle_type_moto,
            R.string.profile_vehicle_type_car,
            R.string.profile_vehicle_manual_title,
            R.string.profile_vehicle_manual_subtitle,
            R.string.profile_vehicle_manual_spec_title,
            R.string.profile_vehicle_manual_brand_label,
            R.string.profile_vehicle_manual_brand_placeholder,
            R.string.profile_vehicle_manual_model_label,
            R.string.profile_vehicle_manual_model_placeholder,
            R.string.profile_vehicle_manual_year_label,
            R.string.profile_vehicle_manual_year_placeholder,
            R.string.profile_vehicle_tech_costs_title,
            R.string.profile_vehicle_consumption_label,
            R.string.profile_vehicle_fuel_type_label,
            R.string.profile_vehicle_fuel_type_placeholder,
            R.string.profile_vehicle_tank_capacity_label,
            R.string.profile_vehicle_fixed_costs_label,
            R.string.profile_vehicle_planned_days_label,
            R.string.profile_vehicle_odometer_label,
            R.string.profile_vehicle_moto_accessories_title,
            R.string.profile_vehicle_has_topbox_title,
            R.string.profile_vehicle_has_topbox_desc,
            R.string.profile_vehicle_has_bag_title,
            R.string.profile_vehicle_has_bag_desc,
            R.string.profile_vehicle_carbody_title,
            R.string.profile_vehicle_btn_save_changes,
            R.string.profile_vehicle_btn_register
        )

        for (resId in stringResIds) {
            val str = app.getString(resId)
            assertNotNull("Resource $resId não deve ser nulo", str)
            assertFalse("Resource $resId não deve ser vazio", str.isBlank())
        }
    }

    @Test
    fun testFormattedProfileStrings() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        val consumptionStr = app.getString(R.string.profile_vehicles_metric_consumption, 40.0, "Gasolina")
        assertTrue(consumptionStr.contains("40") && consumptionStr.contains("Gasolina"))

        val tankOdoStr = app.getString(R.string.profile_vehicles_metric_tank_odometer, 16.0, 15000L)
        assertTrue(tankOdoStr.contains("16") && tankOdoStr.contains("15000"))

        val fixedCostsStr = app.getString(R.string.profile_vehicles_metric_fixed_costs, "R$ 180,00", 22)
        assertTrue(fixedCostsStr.contains("R$ 180,00/mês") && fixedCostsStr.contains("22d"))

        val partMetaStr = app.getString(R.string.profile_parts_item_meta, "R$ 45,00", 2000L)
        assertTrue(partMetaStr.contains("Troca: R$ 45,00") && partMetaStr.contains("2000 km"))
    }
}
