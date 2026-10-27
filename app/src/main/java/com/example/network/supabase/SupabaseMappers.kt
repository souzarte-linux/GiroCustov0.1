package com.example.network.supabase

import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

object SupabaseMappers {
    private val isoFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    fun toIsoString(timestamp: Long): String {
        return isoFormat.format(Date(timestamp))
    }

    fun parseIsoTimestamp(isoString: String?): Long {
        if (isoString.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            isoFormat.parse(isoString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            try {
                val clean = isoString.substringBefore("+").substringBefore("Z")
                val fallback = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                fallback.parse(clean)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }
    }
}

// ==========================================
// VEHICLE MAPPERS
// ==========================================
fun Vehicle.toDto(userId: String): VehicleDto {
    val assignedRemoteId = if (remoteId.isNotBlank()) remoteId else UUID.randomUUID().toString()
    return VehicleDto(
        id = assignedRemoteId,
        userId = userId,
        model = model,
        averageConsumption = averageConsumption,
        fuelType = fuelType,
        monthlyFixedCosts = monthlyFixedCosts,
        plannedWorkDays = plannedWorkDays,
        currentOdometer = currentOdometer,
        active = active,
        vehicleType = vehicleType,
        brand = brand,
        manufacturingYear = manufacturingYear,
        tankCapacity = tankCapacity,
        hasTopBox = hasTopBox,
        hasDeliveryBag = hasDeliveryBag,
        carBodyType = carBodyType,
        updatedAt = SupabaseMappers.toIsoString(if (updatedAt > 0) updatedAt else System.currentTimeMillis()),
        deletedAt = if (isDeleted) SupabaseMappers.toIsoString(updatedAt) else null
    )
}

fun VehicleDto.toEntity(existingId: Long = 0): Vehicle {
    return Vehicle(
        id = existingId,
        model = model,
        averageConsumption = averageConsumption,
        fuelType = fuelType,
        monthlyFixedCosts = monthlyFixedCosts,
        plannedWorkDays = plannedWorkDays,
        currentOdometer = currentOdometer,
        active = active,
        vehicleType = vehicleType,
        brand = brand,
        manufacturingYear = manufacturingYear,
        tankCapacity = tankCapacity,
        hasTopBox = hasTopBox,
        hasDeliveryBag = hasDeliveryBag,
        carBodyType = carBodyType,
        remoteId = id,
        syncStatus = SyncStatus.SYNCED,
        updatedAt = SupabaseMappers.parseIsoTimestamp(updatedAt),
        isDeleted = deletedAt != null
    )
}

// ==========================================
// VEHICLE PARTS MAPPERS
// ==========================================
fun VehiclePart.toDto(userId: String, remoteVehicleId: String?): VehiclePartDto {
    val assignedRemoteId = if (remoteId.isNotBlank()) remoteId else UUID.randomUUID().toString()
    return VehiclePartDto(
        id = assignedRemoteId,
        userId = userId,
        vehicleId = remoteVehicleId,
        name = name,
        price = price,
        lifespanKm = lifespanKm,
        runKmSinceChange = runKmSinceChange,
        updatedAt = SupabaseMappers.toIsoString(if (updatedAt > 0) updatedAt else System.currentTimeMillis()),
        deletedAt = if (isDeleted) SupabaseMappers.toIsoString(updatedAt) else null
    )
}

fun VehiclePartDto.toEntity(existingId: Long = 0, vehicleId: Long = 0L): VehiclePart {
    return VehiclePart(
        id = existingId,
        vehicleId = vehicleId,
        name = name,
        price = price,
        lifespanKm = lifespanKm,
        runKmSinceChange = runKmSinceChange,
        remoteId = id,
        syncStatus = SyncStatus.SYNCED,
        updatedAt = SupabaseMappers.parseIsoTimestamp(updatedAt),
        isDeleted = deletedAt != null
    )
}

// ==========================================
// DAILY RECORDS MAPPERS
// ==========================================
fun DailyRecord.toDto(userId: String): DailyRecordDto {
    val assignedRemoteId = if (remoteId.isNotBlank()) remoteId else UUID.randomUUID().toString()
    return DailyRecordDto(
        id = assignedRemoteId,
        userId = userId,
        dateString = dateString,
        dateTimestamp = dateTimestamp,
        platform = platform,
        grossEarnings = grossEarnings,
        deliveriesCount = deliveriesCount,
        startOdometer = startOdometer,
        endOdometer = endOdometer,
        fuelPrice = fuelPrice,
        foodExpense = foodExpense,
        fuelCost = fuelCost,
        wearCost = wearCost,
        proportionalFixedCost = proportionalFixedCost,
        netProfit = netProfit,
        startTime = startTime,
        endTime = endTime,
        pauseDuration = pauseDuration,
        updatedAt = SupabaseMappers.toIsoString(if (updatedAt > 0) updatedAt else System.currentTimeMillis()),
        deletedAt = if (isDeleted) SupabaseMappers.toIsoString(updatedAt) else null
    )
}

fun DailyRecordDto.toEntity(existingId: Long = 0): DailyRecord {
    return DailyRecord(
        id = existingId,
        dateString = dateString,
        dateTimestamp = dateTimestamp,
        platform = platform,
        grossEarnings = grossEarnings,
        deliveriesCount = deliveriesCount,
        startOdometer = startOdometer,
        endOdometer = endOdometer,
        fuelPrice = fuelPrice,
        foodExpense = foodExpense,
        fuelCost = fuelCost,
        wearCost = wearCost,
        proportionalFixedCost = proportionalFixedCost,
        netProfit = netProfit,
        startTime = startTime,
        endTime = endTime,
        pauseDuration = pauseDuration,
        remoteId = id,
        syncStatus = SyncStatus.SYNCED,
        updatedAt = SupabaseMappers.parseIsoTimestamp(updatedAt),
        isDeleted = deletedAt != null
    )
}

// ==========================================
// USER PROFILE MAPPERS
// ==========================================
fun UserProfile.toDto(userId: String): UserProfileDto {
    val assignedRemoteId = if (remoteId.isNotBlank()) remoteId else UUID.randomUUID().toString()
    return UserProfileDto(
        id = assignedRemoteId,
        userId = userId,
        name = name,
        phone = phone,
        city = city,
        email = email,
        isWhatsapp = isWhatsapp,
        cep = cep,
        street = street,
        addressNumber = addressNumber,
        neighborhood = neighborhood,
        stateUf = stateUf,
        onboardingCompleted = onboardingCompleted,
        updatedAt = SupabaseMappers.toIsoString(if (updatedAt > 0) updatedAt else System.currentTimeMillis()),
        deletedAt = if (isDeleted) SupabaseMappers.toIsoString(updatedAt) else null
    )
}

fun UserProfileDto.toEntity(existingId: Int = 1): UserProfile {
    return UserProfile(
        id = existingId,
        name = name,
        phone = phone,
        city = city,
        email = email,
        isWhatsapp = isWhatsapp,
        cep = cep,
        street = street,
        addressNumber = addressNumber,
        neighborhood = neighborhood,
        stateUf = stateUf,
        onboardingCompleted = onboardingCompleted,
        remoteId = id,
        syncStatus = SyncStatus.SYNCED,
        updatedAt = SupabaseMappers.parseIsoTimestamp(updatedAt),
        isDeleted = deletedAt != null
    )
}

// ==========================================
// PLATFORMS MAPPERS
// ==========================================
fun Platform.toDto(userId: String): PlatformDto {
    val assignedRemoteId = if (remoteId.isNotBlank()) remoteId else UUID.randomUUID().toString()
    return PlatformDto(
        id = assignedRemoteId,
        userId = userId,
        name = name,
        segment = segment,
        paymentModel = paymentModel,
        cycle = cycle,
        paymentDay = paymentDay,
        fixedPayDelay = fixedPayDelay,
        cycleEntriesJson = cycleEntriesJson,
        bankName = bankName,
        bankAgency = bankAgency,
        bankAccount = bankAccount,
        pixKeyType = pixKeyType,
        pixKey = pixKey,
        active = active,
        updatedAt = SupabaseMappers.toIsoString(if (updatedAt > 0) updatedAt else System.currentTimeMillis()),
        deletedAt = if (isDeleted) SupabaseMappers.toIsoString(updatedAt) else null
    )
}

fun PlatformDto.toEntity(existingId: Long = 0): Platform {
    return Platform(
        id = existingId,
        name = name,
        segment = segment,
        paymentModel = paymentModel,
        cycle = cycle,
        paymentDay = paymentDay,
        fixedPayDelay = fixedPayDelay,
        cycleEntriesJson = cycleEntriesJson,
        bankName = bankName,
        bankAgency = bankAgency,
        bankAccount = bankAccount,
        pixKeyType = pixKeyType,
        pixKey = pixKey,
        active = active,
        remoteId = id,
        syncStatus = SyncStatus.SYNCED,
        updatedAt = SupabaseMappers.parseIsoTimestamp(updatedAt),
        isDeleted = deletedAt != null
    )
}

// ==========================================
// FUEL REFILLS MAPPERS
// ==========================================
fun FuelRefill.toDto(userId: String, remoteVehicleId: String?): FuelRefillDto {
    val assignedRemoteId = if (remoteId.isNotBlank()) remoteId else UUID.randomUUID().toString()
    return FuelRefillDto(
        id = assignedRemoteId,
        userId = userId,
        vehicleId = remoteVehicleId,
        dateTimestamp = dateTimestamp,
        dateString = dateString,
        stationName = gasStation,
        gasStation = gasStation,
        fuelType = fuelType,
        pricePerLiter = pricePerLiter,
        liters = liters,
        discount = discount,
        totalPaid = totalPaid,
        odometer = odometer,
        fullTank = isFullTank,
        isFullTank = isFullTank,
        paymentMethod = paymentMethod,
        isInstallment = isInstallment,
        installmentsCount = installmentsCount,
        updatedAt = SupabaseMappers.toIsoString(if (updatedAt > 0) updatedAt else System.currentTimeMillis()),
        deletedAt = if (isDeleted) SupabaseMappers.toIsoString(updatedAt) else null
    )
}

fun FuelRefillDto.toEntity(existingId: Long = 0, localVehicleId: Long): FuelRefill {
    return FuelRefill(
        id = existingId,
        vehicleId = localVehicleId,
        dateTimestamp = dateTimestamp,
        dateString = dateString,
        gasStation = if (gasStation.isNotBlank()) gasStation else stationName,
        fuelType = fuelType,
        pricePerLiter = pricePerLiter,
        liters = liters,
        discount = discount,
        totalPaid = totalPaid,
        odometer = odometer,
        isFullTank = isFullTank || fullTank,
        paymentMethod = paymentMethod,
        isInstallment = isInstallment,
        installmentsCount = installmentsCount,
        remoteId = id,
        syncStatus = SyncStatus.SYNCED,
        updatedAt = SupabaseMappers.parseIsoTimestamp(updatedAt),
        isDeleted = deletedAt != null
    )
}

// ==========================================
// MAINTENANCE RECORDS MAPPERS
// ==========================================
fun MaintenanceRecord.toDto(userId: String, remoteVehicleId: String?): MaintenanceRecordDto {
    val assignedRemoteId = if (remoteId.isNotBlank()) remoteId else UUID.randomUUID().toString()
    return MaintenanceRecordDto(
        id = assignedRemoteId,
        userId = userId,
        vehicleId = remoteVehicleId,
        dateTimestamp = dateTimestamp,
        dateString = dateString,
        description = description,
        location = location,
        value = value,
        odometer = odometer,
        updatedAt = SupabaseMappers.toIsoString(if (updatedAt > 0) updatedAt else System.currentTimeMillis()),
        deletedAt = if (isDeleted) SupabaseMappers.toIsoString(updatedAt) else null
    )
}

fun MaintenanceRecordDto.toEntity(existingId: Long = 0, localVehicleId: Long): MaintenanceRecord {
    return MaintenanceRecord(
        id = existingId,
        vehicleId = localVehicleId,
        dateTimestamp = dateTimestamp,
        dateString = dateString,
        description = description,
        location = location,
        value = value,
        odometer = odometer,
        remoteId = id,
        syncStatus = SyncStatus.SYNCED,
        updatedAt = SupabaseMappers.parseIsoTimestamp(updatedAt),
        isDeleted = deletedAt != null
    )
}

// ==========================================
// USER GOALS MAPPERS
// ==========================================
fun UserGoals.toDto(userId: String): UserGoalsDto {
    val assignedRemoteId = if (remoteId.isNotBlank()) remoteId else UUID.randomUUID().toString()
    return UserGoalsDto(
        id = assignedRemoteId,
        userId = userId,
        dailyGoalGross = dailyGoalGross,
        dailyGoalNet = dailyGoalNet,
        dailyGoalKm = dailyGoalKm,
        dailyGoalDeliveries = dailyGoalDeliveries,
        weeklyGoalGross = weeklyGoalGross,
        weeklyGoalNet = weeklyGoalNet,
        weeklyGoalKm = weeklyGoalKm,
        weeklyGoalDeliveries = weeklyGoalDeliveries,
        monthlyGoalGross = monthlyGoalGross,
        monthlyGoalNet = monthlyGoalNet,
        monthlyGoalKm = monthlyGoalKm,
        monthlyGoalDeliveries = monthlyGoalDeliveries,
        updatedAt = SupabaseMappers.toIsoString(if (updatedAt > 0) updatedAt else System.currentTimeMillis()),
        deletedAt = if (isDeleted) SupabaseMappers.toIsoString(updatedAt) else null
    )
}

fun UserGoalsDto.toEntity(existingId: Int = 1): UserGoals {
    return UserGoals(
        id = existingId,
        dailyGoalGross = dailyGoalGross,
        dailyGoalNet = dailyGoalNet,
        dailyGoalKm = dailyGoalKm,
        dailyGoalDeliveries = dailyGoalDeliveries,
        weeklyGoalGross = weeklyGoalGross,
        weeklyGoalNet = weeklyGoalNet,
        weeklyGoalKm = weeklyGoalKm,
        weeklyGoalDeliveries = weeklyGoalDeliveries,
        monthlyGoalGross = monthlyGoalGross,
        monthlyGoalNet = monthlyGoalNet,
        monthlyGoalKm = monthlyGoalKm,
        monthlyGoalDeliveries = monthlyGoalDeliveries,
        remoteId = id,
        syncStatus = SyncStatus.SYNCED,
        updatedAt = SupabaseMappers.parseIsoTimestamp(updatedAt),
        isDeleted = deletedAt != null
    )
}

