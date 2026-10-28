package com.example.util

import com.example.data.GiroCustoDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilitário responsável pela consolidação e exportação dos dados operacionais e cadastrais
 * do usuário em formato JSON estruturado (Portabilidade de Dados - LGPD Art. 18, V).
 */
object UserDataExportHelper {

    suspend fun export(db: GiroCustoDatabase): String {
        val root = JSONObject()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

        root.put("export_format_version", 1)
        root.put("app_name", "GiroCusto")
        root.put("exported_at", isoFormat.format(Date()))

        // 1. Perfil do Usuário
        val profile = db.userProfileDao().getUserProfile()
        if (profile != null) {
            val pObj = JSONObject().apply {
                put("name", profile.name)
                put("phone", profile.phone)
                put("city", profile.city)
                put("email", profile.email)
                put("is_whatsapp", profile.isWhatsapp)
                put("cep", profile.cep)
                put("address_street", profile.street)
                put("address_number", profile.addressNumber)
                put("address_neighborhood", profile.neighborhood)
                put("state_uf", profile.stateUf)
                put("onboarding_completed", profile.onboardingCompleted)
            }
            root.put("user_profile", pObj)
        } else {
            root.put("user_profile", JSONObject.NULL)
        }

        // 2. Metas Operacionais e Financeiras
        val goals = db.userGoalsDao().getUserGoals()
        if (goals != null) {
            val gObj = JSONObject().apply {
                put("daily_goal_gross", CurrencyUtils.centsToDouble(goals.dailyGoalGross))
                put("daily_goal_net", CurrencyUtils.centsToDouble(goals.dailyGoalNet))
                put("daily_goal_km", goals.dailyGoalKm)
                put("daily_goal_deliveries", goals.dailyGoalDeliveries)
                put("weekly_goal_gross", CurrencyUtils.centsToDouble(goals.weeklyGoalGross))
                put("weekly_goal_net", CurrencyUtils.centsToDouble(goals.weeklyGoalNet))
                put("weekly_goal_km", goals.weeklyGoalKm)
                put("weekly_goal_deliveries", goals.weeklyGoalDeliveries)
                put("monthly_goal_gross", CurrencyUtils.centsToDouble(goals.monthlyGoalGross))
                put("monthly_goal_net", CurrencyUtils.centsToDouble(goals.monthlyGoalNet))
                put("monthly_goal_km", goals.monthlyGoalKm)
                put("monthly_goal_deliveries", goals.monthlyGoalDeliveries)
            }
            root.put("user_goals", gObj)
        } else {
            root.put("user_goals", JSONObject.NULL)
        }

        // 3. Veículos Operacionais
        val vehicles = db.vehicleDao().getAllVehicles()
        val vArr = JSONArray()
        vehicles.forEach { v ->
            val vObj = JSONObject().apply {
                put("id", v.id)
                put("model", v.model)
                put("brand", v.brand)
                put("manufacturing_year", v.manufacturingYear)
                put("vehicle_type", v.vehicleType)
                put("average_consumption", v.averageConsumption)
                put("fuel_type", v.fuelType)
                put("monthly_fixed_costs", CurrencyUtils.centsToDouble(v.monthlyFixedCosts))
                put("planned_work_days", v.plannedWorkDays)
                put("current_odometer", v.currentOdometer)
                put("tank_capacity", v.tankCapacity)
                put("active", v.active)
            }
            vArr.put(vObj)
        }
        root.put("vehicles", vArr)

        // 4. Peças e Desgaste
        val parts = db.vehiclePartDao().getAllParts()
        val partsArr = JSONArray()
        parts.forEach { p ->
            val pObj = JSONObject().apply {
                put("id", p.id)
                put("vehicle_id", p.vehicleId)
                put("name", p.name)
                put("price", CurrencyUtils.centsToDouble(p.price))
                put("lifespan_km", p.lifespanKm)
                put("run_km_since_change", p.runKmSinceChange)
            }
            partsArr.put(pObj)
        }
        root.put("vehicle_parts", partsArr)

        // 5. Corridas e Diárias
        val records = db.dailyRecordDao().getAllRecords()
        val recArr = JSONArray()
        records.forEach { r ->
            val rObj = JSONObject().apply {
                put("id", r.id)
                put("date_string", r.dateString)
                put("date_timestamp", r.dateTimestamp)
                put("platform", r.platform)
                put("gross_earnings", CurrencyUtils.centsToDouble(r.grossEarnings))
                put("deliveries_count", r.deliveriesCount)
                put("start_odometer", r.startOdometer)
                put("end_odometer", r.endOdometer)
                put("fuel_price", CurrencyUtils.fuelPriceMillisToDouble(r.fuelPrice))
                put("food_expense", CurrencyUtils.centsToDouble(r.foodExpense))
                put("fuel_cost", CurrencyUtils.centsToDouble(r.fuelCost))
                put("wear_cost", CurrencyUtils.centsToDouble(r.wearCost))
                put("proportional_fixed_cost", CurrencyUtils.centsToDouble(r.proportionalFixedCost))
                put("net_profit", CurrencyUtils.centsToDouble(r.netProfit))
                put("start_time", r.startTime)
                put("end_time", r.endTime)
                put("pause_duration", r.pauseDuration)
            }
            recArr.put(rObj)
        }
        root.put("daily_records", recArr)

        // 6. Abastecimentos
        val refills = db.fuelRefillDao().getAllRefills()
        val refArr = JSONArray()
        refills.forEach { f ->
            val fObj = JSONObject().apply {
                put("id", f.id)
                put("vehicle_id", f.vehicleId)
                put("date_string", f.dateString)
                put("date_timestamp", f.dateTimestamp)
                put("gas_station", f.gasStation)
                put("fuel_type", f.fuelType)
                put("price_per_liter", CurrencyUtils.fuelPriceMillisToDouble(f.pricePerLiter))
                put("liters", f.liters)
                put("discount", CurrencyUtils.centsToDouble(f.discount))
                put("total_paid", CurrencyUtils.centsToDouble(f.totalPaid))
                put("odometer", f.odometer)
                put("is_full_tank", f.isFullTank)
                put("payment_method", f.paymentMethod)
            }
            refArr.put(fObj)
        }
        root.put("fuel_refills", refArr)

        // 7. Manutenções
        val maintenance = db.maintenanceRecordDao().getAllRecords()
        val mArr = JSONArray()
        maintenance.forEach { m ->
            val mObj = JSONObject().apply {
                put("id", m.id)
                put("vehicle_id", m.vehicleId)
                put("date_string", m.dateString)
                put("date_timestamp", m.dateTimestamp)
                put("description", m.description)
                put("location", m.location)
                put("value", CurrencyUtils.centsToDouble(m.value))
                put("odometer", m.odometer)
            }
            mArr.put(mObj)
        }
        root.put("maintenance_records", mArr)

        // 8. Plataformas
        val platforms = db.platformDao().getAllPlatforms()
        val platArr = JSONArray()
        platforms.forEach { pl ->
            val plObj = JSONObject().apply {
                put("id", pl.id)
                put("name", pl.name)
                put("segment", pl.segment)
                put("payment_model", pl.paymentModel)
                put("cycle", pl.cycle)
                put("payment_day", pl.paymentDay)
                put("fixed_pay_delay", pl.fixedPayDelay)
                put("bank_name", pl.bankName)
                put("pix_key_type", pl.pixKeyType)
                put("pix_key", pl.pixKey)
                put("active", pl.active)
            }
            platArr.put(plObj)
        }
        root.put("platforms", platArr)

        return root.toString(2)
    }
}
