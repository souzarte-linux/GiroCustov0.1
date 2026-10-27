package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object SyncStatus {
    const val SYNCED = "SYNCED"
    const val PENDING_INSERT = "PENDING_INSERT"
    const val PENDING_UPDATE = "PENDING_UPDATE"
    const val PENDING_DELETE = "PENDING_DELETE"
}

@Entity(
    tableName = "vehicle",
    indices = [
        Index(value = ["active", "isDeleted"]),
        Index(value = ["remoteId"]),
        Index(value = ["syncStatus"])
    ]
)
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val model: String = "Honda CG 160 Titan",
    val averageConsumption: Double = 40.0, // km/L
    val fuelType: String = "Gasolina",
    val monthlyFixedCosts: Double = 180.0, // IPVA, Seguro, Celular, MEI
    val plannedWorkDays: Int = 22, // Rateio de custos fixos por dia (ex: 22 dias)
    val currentOdometer: Double = 15350.0, // Último hodômetro conhecido
    val active: Boolean = true,
    val vehicleType: String = "MOTO",
    val brand: String = "",
    val manufacturingYear: String = "",
    val tankCapacity: Double = 16.0,
    val hasTopBox: Boolean = false,
    val hasDeliveryBag: Boolean = false,
    val carBodyType: String = "",
    val remoteId: String = "",
    val syncStatus: String = SyncStatus.SYNCED,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "vehicle_parts",
    indices = [
        Index(value = ["vehicleId"]),
        Index(value = ["remoteId"]),
        Index(value = ["syncStatus"])
    ]
)
data class VehiclePart(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long = 0L,
    val name: String,
    val price: Double, // Preço de troca
    val lifespanKm: Double, // Durabilidade em km
    val runKmSinceChange: Double = 0.0, // Quilômetros rodados desde a última troca
    val remoteId: String = "",
    val syncStatus: String = SyncStatus.SYNCED,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
) {
    val healthPercentage: Double
        get() = ((lifespanKm - runKmSinceChange) / lifespanKm * 100.0).coerceIn(0.0, 100.0)
    
    val wearPercentage: Double
        get() = (runKmSinceChange / lifespanKm * 100.0).coerceIn(0.0, 100.0)
    
    val wearCostPerKm: Double
        get() = if (lifespanKm > 0) price / lifespanKm else 0.0
}

@Entity(
    tableName = "daily_records",
    indices = [
        Index(value = ["isDeleted", "dateTimestamp"]),
        Index(value = ["remoteId"]),
        Index(value = ["syncStatus"])
    ]
)
data class DailyRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String, // formato "YYYY-MM-DD"
    val dateTimestamp: Long, // timestamp para ordenação e busca por período
    val platform: String = "Geral", // Nome da plataforma (ex: iFood, Uber Flash)
    val grossEarnings: Double, // Faturamento Bruto
    val deliveriesCount: Int, // Quantidade de entregas
    val startOdometer: Double, // Hodômetro Inicial
    val endOdometer: Double, // Hodômetro Final
    val fuelPrice: Double, // Preço do combustível por litro
    val foodExpense: Double, // Gasto com alimentação
    val fuelCost: Double, // (endOdometer - startOdometer) / consumption * fuelPrice
    val wearCost: Double, // (endOdometer - startOdometer) * sum(parts wearCostPerKm)
    val proportionalFixedCost: Double, // monthlyFixedCosts / plannedWorkDays
    val netProfit: Double, // grossEarnings - fuelCost - wearCost - proportionalFixedCost - foodExpense
    val startTime: String = "", // formato "HH:mm" (ex: "08:00")
    val endTime: String = "", // formato "HH:mm" (ex: "18:00")
    val pauseDuration: String = "", // formato "HH:mm" (ex: "01:00")
    val remoteId: String = "",
    val syncStatus: String = SyncStatus.SYNCED,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
) {
    val kmRodados: Double
        get() = (endOdometer - startOdometer).coerceAtLeast(0.0)

    val pauseMinutes: Long
        get() {
            val clean = pauseDuration.trim()
            if (clean.isBlank()) return 0L
            return if (clean.contains(":")) {
                val parts = clean.split(":")
                val h = parts.getOrNull(0)?.trim()?.toLongOrNull() ?: 0L
                val m = parts.getOrNull(1)?.trim()?.toLongOrNull() ?: 0L
                (h * 60L + m).coerceAtLeast(0L)
            } else {
                (clean.replace("[^0-9]".toRegex(), "").toLongOrNull() ?: 0L).coerceAtLeast(0L)
            }
        }

    val totalWorkMinutes: Long
        get() {
            val s = startTime.trim()
            val e = endTime.trim()
            if (!s.contains(":") || !e.contains(":")) return 0L
            val sParts = s.split(":")
            val eParts = e.split(":")
            val sH = sParts.getOrNull(0)?.trim()?.toLongOrNull() ?: return 0L
            val sM = sParts.getOrNull(1)?.trim()?.toLongOrNull() ?: return 0L
            val eH = eParts.getOrNull(0)?.trim()?.toLongOrNull() ?: return 0L
            val eM = eParts.getOrNull(1)?.trim()?.toLongOrNull() ?: return 0L

            val startTotal = sH * 60L + sM
            var endTotal = eH * 60L + eM
            if (endTotal < startTotal) {
                // Turno noturno que atravessou a meia-noite (ex: 22:00 às 04:00)
                endTotal += 24L * 60L
            }
            val grossMinutes = endTotal - startTotal
            return (grossMinutes - pauseMinutes).coerceAtLeast(0L)
        }

    val totalWorkHours: Double
        get() = totalWorkMinutes / 60.0

    val grossPerHour: Double
        get() = if (totalWorkHours > 0.0) grossEarnings / totalWorkHours else 0.0

    val netPerHour: Double
        get() = if (totalWorkHours > 0.0) netProfit / totalWorkHours else 0.0

    val formattedWorkDuration: String
        get() {
            val mins = totalWorkMinutes
            if (mins <= 0L) return "--"
            val h = mins / 60
            val m = mins % 60
            return if (m > 0) "${h}h ${m}m" else "${h}h"
        }
}

@Entity(
    tableName = "user_profile",
    indices = [
        Index(value = ["remoteId"]),
        Index(value = ["syncStatus"])
    ]
)
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val phone: String = "",
    val city: String = "",
    val email: String = "",
    val isWhatsapp: Boolean = true,
    val cep: String = "",
    val street: String = "",
    val addressNumber: String = "",
    val neighborhood: String = "",
    val stateUf: String = "",
    val onboardingCompleted: Boolean = false,
    val remoteId: String = "",
    val syncStatus: String = SyncStatus.SYNCED,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "platforms",
    indices = [
        Index(value = ["remoteId"]),
        Index(value = ["syncStatus"])
    ]
)
data class Platform(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val segment: String = "logistica", // "logistica" | "delivery"
    val paymentModel: String = "producao", // "producao" | "diaria"
    val cycle: String = "semanal", // "semanal" | "quinzenal" | "mensal" | "misto"
    val paymentDay: String = "QUA",
    val fixedPayDelay: Int = 7,
    val cycleEntriesJson: String = "1:7,16:7",
    val bankName: String = "",
    val bankAgency: String = "",
    val bankAccount: String = "",
    val pixKeyType: String = "CPF",
    val pixKey: String = "",
    val active: Boolean = true,
    val remoteId: String = "",
    val syncStatus: String = SyncStatus.SYNCED,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "fuel_refills",
    indices = [
        Index(value = ["vehicleId", "dateTimestamp"]),
        Index(value = ["vehicleId", "odometer"]),
        Index(value = ["remoteId"]),
        Index(value = ["syncStatus"])
    ]
)
data class FuelRefill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val dateTimestamp: Long,
    val dateString: String,
    val gasStation: String,
    val fuelType: String,
    val pricePerLiter: Double,
    val liters: Double,
    val discount: Double = 0.0,
    val totalPaid: Double,
    val odometer: Double,
    val isFullTank: Boolean,
    val paymentMethod: String,
    val isInstallment: Boolean = false,
    val installmentsCount: Int = 1,
    val remoteId: String = "",
    val syncStatus: String = SyncStatus.SYNCED,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "maintenance_records",
    indices = [
        Index(value = ["vehicleId", "dateTimestamp"]),
        Index(value = ["remoteId"]),
        Index(value = ["syncStatus"])
    ]
)
data class MaintenanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val dateTimestamp: Long,
    val dateString: String,
    val description: String,
    val location: String,
    val value: Double,
    val odometer: Double,
    val remoteId: String = "",
    val syncStatus: String = SyncStatus.SYNCED,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "user_goals",
    indices = [
        Index(value = ["remoteId"]),
        Index(value = ["syncStatus"])
    ]
)
data class UserGoals(
    @PrimaryKey val id: Int = 1,

    // Metas Diárias
    val dailyGoalGross: Double = 200.0,
    val dailyGoalNet: Double = 130.0,
    val dailyGoalKm: Double = 100.0,
    val dailyGoalDeliveries: Int = 15,

    // Metas Semanais
    val weeklyGoalGross: Double = 1200.0,
    val weeklyGoalNet: Double = 800.0,
    val weeklyGoalKm: Double = 600.0,
    val weeklyGoalDeliveries: Int = 90,

    // Metas Mensais
    val monthlyGoalGross: Double = 5000.0,
    val monthlyGoalNet: Double = 3200.0,
    val monthlyGoalKm: Double = 2500.0,
    val monthlyGoalDeliveries: Int = 400,

    // Campos de Sincronização e Resolução de Conflitos
    @ColumnInfo(defaultValue = "")
    val remoteId: String = "",
    @ColumnInfo(defaultValue = "SYNCED")
    val syncStatus: String = SyncStatus.SYNCED,
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    val isDeleted: Boolean = false
)

