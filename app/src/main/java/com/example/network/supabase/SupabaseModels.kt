package com.example.network.supabase

import com.example.data.*
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.UUID

// ==========================================
// AUTH MODELS
// ==========================================

@JsonClass(generateAdapter = true)
data class AuthRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "data") val data: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    @Json(name = "refresh_token") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class IdTokenRequest(
    @Json(name = "provider") val provider: String = "google",
    @Json(name = "id_token") val idToken: String,
    @Json(name = "nonce") val nonce: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "access_token") val accessToken: String = "",
    @Json(name = "token_type") val tokenType: String = "",
    @Json(name = "expires_in") val expiresIn: Long = 0,
    @Json(name = "refresh_token") val refreshToken: String = "",
    @Json(name = "user") val user: SupabaseUser? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseUser(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val userEmail: String?
)

// ==========================================
// POSTGREST DTOs (7 TABELAS)
// ==========================================

@JsonClass(generateAdapter = true)
data class VehicleDto(
    @Json(name = "id") val id: String = UUID.randomUUID().toString(),
    @Json(name = "user_id") val userId: String,
    @Json(name = "model") val model: String,
    @Json(name = "average_consumption") val averageConsumption: Double,
    @Json(name = "fuel_type") val fuelType: String,
    @Json(name = "monthly_fixed_costs") val monthlyFixedCosts: Double,
    @Json(name = "planned_work_days") val plannedWorkDays: Int,
    @Json(name = "current_odometer") val currentOdometer: Double,
    @Json(name = "active") val active: Boolean,
    @Json(name = "vehicle_type") val vehicleType: String = "MOTO",
    @Json(name = "brand") val brand: String = "",
    @Json(name = "manufacturing_year") val manufacturingYear: String = "",
    @Json(name = "tank_capacity") val tankCapacity: Double = 16.0,
    @Json(name = "has_top_box") val hasTopBox: Boolean = false,
    @Json(name = "has_delivery_bag") val hasDeliveryBag: Boolean = false,
    @Json(name = "car_body_type") val carBodyType: String = "",
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "deleted_at") val deletedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class VehiclePartDto(
    @Json(name = "id") val id: String = UUID.randomUUID().toString(),
    @Json(name = "user_id") val userId: String,
    @Json(name = "vehicle_id") val vehicleId: String? = null,
    @Json(name = "name") val name: String,
    @Json(name = "price") val price: Double,
    @Json(name = "lifespan_km") val lifespanKm: Double,
    @Json(name = "run_km_since_change") val runKmSinceChange: Double,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "deleted_at") val deletedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class DailyRecordDto(
    @Json(name = "id") val id: String = UUID.randomUUID().toString(),
    @Json(name = "user_id") val userId: String,
    @Json(name = "date_string") val dateString: String,
    @Json(name = "date_timestamp") val dateTimestamp: Long,
    @Json(name = "platform") val platform: String,
    @Json(name = "gross_earnings") val grossEarnings: Double,
    @Json(name = "deliveries_count") val deliveriesCount: Int,
    @Json(name = "start_odometer") val startOdometer: Double,
    @Json(name = "end_odometer") val endOdometer: Double,
    @Json(name = "fuel_price") val fuelPrice: Double,
    @Json(name = "food_expense") val foodExpense: Double,
    @Json(name = "fuel_cost") val fuelCost: Double,
    @Json(name = "wear_cost") val wearCost: Double,
    @Json(name = "proportional_fixed_cost") val proportionalFixedCost: Double,
    @Json(name = "net_profit") val netProfit: Double,
    @Json(name = "start_time") val startTime: String = "",
    @Json(name = "end_time") val endTime: String = "",
    @Json(name = "pause_duration") val pauseDuration: String = "",
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "deleted_at") val deletedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class UserProfileDto(
    @Json(name = "id") val id: String = UUID.randomUUID().toString(),
    @Json(name = "user_id") val userId: String,
    @Json(name = "name") val name: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "city") val city: String,
    @Json(name = "email") val email: String = "",
    @Json(name = "is_whatsapp") val isWhatsapp: Boolean = true,
    @Json(name = "cep") val cep: String = "",
    @Json(name = "street") val street: String = "",
    @Json(name = "address_number") val addressNumber: String = "",
    @Json(name = "neighborhood") val neighborhood: String = "",
    @Json(name = "state_uf") val stateUf: String = "",
    @Json(name = "onboarding_completed") val onboardingCompleted: Boolean = false,
    @Json(name = "platforms") val platforms: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "deleted_at") val deletedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class PlatformDto(
    @Json(name = "id") val id: String = UUID.randomUUID().toString(),
    @Json(name = "user_id") val userId: String,
    @Json(name = "name") val name: String,
    @Json(name = "segment") val segment: String = "logistica",
    @Json(name = "payment_model") val paymentModel: String = "producao",
    @Json(name = "cycle") val cycle: String = "semanal",
    @Json(name = "payment_day") val paymentDay: String = "QUA",
    @Json(name = "fixed_pay_delay") val fixedPayDelay: Int = 7,
    @Json(name = "cycle_entries_json") val cycleEntriesJson: String = "1:7,16:7",
    @Json(name = "bank_name") val bankName: String = "",
    @Json(name = "bank_agency") val bankAgency: String = "",
    @Json(name = "bank_account") val bankAccount: String = "",
    @Json(name = "pix_key_type") val pixKeyType: String = "CPF",
    @Json(name = "pix_key") val pixKey: String = "",
    @Json(name = "color_hex") val colorHex: String = "#1976D2",
    @Json(name = "active") val active: Boolean = true,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "deleted_at") val deletedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class FuelRefillDto(
    @Json(name = "id") val id: String = UUID.randomUUID().toString(),
    @Json(name = "user_id") val userId: String,
    @Json(name = "vehicle_id") val vehicleId: String? = null,
    @Json(name = "date_timestamp") val dateTimestamp: Long,
    @Json(name = "date_string") val dateString: String,
    @Json(name = "station_name") val stationName: String = "",
    @Json(name = "station_address") val stationAddress: String = "",
    @Json(name = "gas_station") val gasStation: String = "",
    @Json(name = "fuel_type") val fuelType: String = "Gasolina",
    @Json(name = "liters") val liters: Double,
    @Json(name = "price_per_liter") val pricePerLiter: Double,
    @Json(name = "discount") val discount: Double = 0.0,
    @Json(name = "total_paid") val totalPaid: Double,
    @Json(name = "odometer") val odometer: Double,
    @Json(name = "full_tank") val fullTank: Boolean = true,
    @Json(name = "is_full_tank") val isFullTank: Boolean = true,
    @Json(name = "payment_method") val paymentMethod: String = "Dinheiro",
    @Json(name = "is_installment") val isInstallment: Boolean = false,
    @Json(name = "installments_count") val installmentsCount: Int = 1,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "deleted_at") val deletedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class MaintenanceRecordDto(
    @Json(name = "id") val id: String = UUID.randomUUID().toString(),
    @Json(name = "user_id") val userId: String,
    @Json(name = "vehicle_id") val vehicleId: String? = null,
    @Json(name = "date_timestamp") val dateTimestamp: Long,
    @Json(name = "date_string") val dateString: String,
    @Json(name = "description") val description: String,
    @Json(name = "location") val location: String = "",
    @Json(name = "value") val value: Double,
    @Json(name = "odometer") val odometer: Double,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "deleted_at") val deletedAt: String? = null
)
