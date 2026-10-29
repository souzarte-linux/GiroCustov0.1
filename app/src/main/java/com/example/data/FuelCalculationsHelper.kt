package com.example.data

import com.example.util.CurrencyUtils

object FuelCalculationsHelper {

    suspend fun calculateRealAverageConsumption(fuelRefillDao: FuelRefillDao, vehicleId: Long): Double? {
        val allRefills = fuelRefillDao.getAllRefillsOrderedByOdometer(vehicleId)
        val fullTankIndices = allRefills.indices.filter { allRefills[it].isFullTank }
        if (fullTankIndices.size < 2) return null

        val segmentConsumptions = mutableListOf<Double>()
        for (i in 0 until fullTankIndices.size - 1) {
            val prevFullIdx = fullTankIndices[i]
            val currFullIdx = fullTankIndices[i + 1]

            val prevFullRefill = allRefills[prevFullIdx]
            val currFullRefill = allRefills[currFullIdx]

            val distance = currFullRefill.odometer - prevFullRefill.odometer
            if (distance <= 0) continue

            var litersSum = 0.0
            for (j in (prevFullIdx + 1)..currFullIdx) {
                litersSum += allRefills[j].liters
            }

            if (litersSum > 0) {
                val segmentConsumption = distance / litersSum
                segmentConsumptions.add(segmentConsumption)
            }
        }

        return if (segmentConsumptions.isEmpty()) null else segmentConsumptions.average()
    }

    suspend fun calculateLatestRealConsumption(fuelRefillDao: FuelRefillDao, vehicleId: Long): Double? {
        val allRefills = fuelRefillDao.getAllRefillsOrderedByOdometer(vehicleId)
        val fullTankIndices = allRefills.indices.filter { allRefills[it].isFullTank }
        if (fullTankIndices.size < 2) return null

        val prevFullIdx = fullTankIndices[fullTankIndices.size - 2]
        val currFullIdx = fullTankIndices.last()

        val prevFullRefill = allRefills[prevFullIdx]
        val currFullRefill = allRefills[currFullIdx]

        val distance = currFullRefill.odometer - prevFullRefill.odometer
        if (distance <= 0) return null

        var litersSum = 0.0
        for (j in (prevFullIdx + 1)..currFullIdx) {
            litersSum += allRefills[j].liters
        }

        if (litersSum <= 0) return null
        return distance / litersSum
    }

    suspend fun averageFuelPrice(fuelRefillDao: FuelRefillDao, vehicleId: Long): Double {
        val refills = fuelRefillDao.getAllRefillsOrderedByOdometer(vehicleId)
        if (refills.isEmpty()) return 0.0
        return refills.map { CurrencyUtils.fuelPriceMillisToDouble(it.pricePerLiter) }.average()
    }

    suspend fun averageLitersPerRefill(fuelRefillDao: FuelRefillDao, vehicleId: Long): Double {
        val refills = fuelRefillDao.getAllRefillsOrderedByOdometer(vehicleId)
        if (refills.isEmpty()) return 0.0
        return refills.map { it.liters }.average()
    }
}
