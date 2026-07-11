package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicle")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val model: String = "Honda CG 160 Titan",
    val averageConsumption: Double = 40.0, // km/L
    val fuelType: String = "Gasolina",
    val monthlyFixedCosts: Double = 180.0, // IPVA, Seguro, Celular, MEI
    val plannedWorkDays: Int = 22, // Rateio de custos fixos por dia (ex: 22 dias)
    val currentOdometer: Double = 15350.0, // Último hodômetro conhecido
    val active: Boolean = true
)

@Entity(tableName = "vehicle_parts")
data class VehiclePart(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val price: Double, // Preço de troca
    val lifespanKm: Double, // Durabilidade em km
    val runKmSinceChange: Double = 0.0 // Quilômetros rodados desde a última troca
) {
    val healthPercentage: Double
        get() = ((lifespanKm - runKmSinceChange) / lifespanKm * 100.0).coerceIn(0.0, 100.0)
    
    val wearPercentage: Double
        get() = (runKmSinceChange / lifespanKm * 100.0).coerceIn(0.0, 100.0)
    
    val wearCostPerKm: Double
        get() = if (lifespanKm > 0) price / lifespanKm else 0.0
}

@Entity(tableName = "daily_records")
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
    val netProfit: Double // grossEarnings - fuelCost - wearCost - proportionalFixedCost - foodExpense
) {
    val kmRodados: Double
        get() = (endOdometer - startOdometer).coerceAtLeast(0.0)
}

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val phone: String = "",
    val city: String = ""
)

@Entity(tableName = "platforms")
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
    val active: Boolean = true
)

@Entity(tableName = "fuel_refills")
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
    val installmentsCount: Int = 1
)


