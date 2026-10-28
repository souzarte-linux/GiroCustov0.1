package com.example

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.GiroCustoDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class RoomMigration14To15Test {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GiroCustoDatabase::class.java.canonicalName ?: "com.example.data.GiroCustoDatabase",
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun testMigration14To15_validatesSchemaAndRecreatesTablesWithIntegerAffinity() {
        val dbName = "migration_14_15_test_${System.currentTimeMillis()}.db"

        // 1. Cria o banco de dados na versão 14 usando o schema 14.json
        val dbV14 = helper.createDatabase(dbName, 14)
        dbV14.close()

        // 2. Executa a migração para a versão 15 e valida contra o schema 15.json
        val dbV15 = helper.runMigrationsAndValidate(
            dbName,
            15,
            true,
            GiroCustoDatabase.MIGRATION_14_15
        )

        // 3. Validação das tabelas e tipos das colunas alteradas (PRAGMA table_info)
        // vehicle.monthlyFixedCosts deve ser INTEGER
        val cursorVehicle = dbV15.query("PRAGMA table_info(`vehicle`)")
        var monthlyFixedCostsType = ""
        while (cursorVehicle.moveToNext()) {
            val name = cursorVehicle.getString(cursorVehicle.getColumnIndexOrThrow("name"))
            if (name == "monthlyFixedCosts") {
                monthlyFixedCostsType = cursorVehicle.getString(cursorVehicle.getColumnIndexOrThrow("type"))
            }
        }
        cursorVehicle.close()
        assertEquals("INTEGER", monthlyFixedCostsType)

        // vehicle_parts.price deve ser INTEGER
        val cursorParts = dbV15.query("PRAGMA table_info(`vehicle_parts`)")
        var priceType = ""
        while (cursorParts.moveToNext()) {
            val name = cursorParts.getString(cursorParts.getColumnIndexOrThrow("name"))
            if (name == "price") {
                priceType = cursorParts.getString(cursorParts.getColumnIndexOrThrow("type"))
            }
        }
        cursorParts.close()
        assertEquals("INTEGER", priceType)

        // daily_records: 7 campos monetários devem ser INTEGER
        val cursorDaily = dbV15.query("PRAGMA table_info(`daily_records`)")
        val dailyCols = mutableMapOf<String, String>()
        while (cursorDaily.moveToNext()) {
            val name = cursorDaily.getString(cursorDaily.getColumnIndexOrThrow("name"))
            val type = cursorDaily.getString(cursorDaily.getColumnIndexOrThrow("type"))
            dailyCols[name] = type
        }
        cursorDaily.close()
        assertEquals("INTEGER", dailyCols["grossEarnings"])
        assertEquals("INTEGER", dailyCols["fuelPrice"])
        assertEquals("INTEGER", dailyCols["foodExpense"])
        assertEquals("INTEGER", dailyCols["fuelCost"])
        assertEquals("INTEGER", dailyCols["wearCost"])
        assertEquals("INTEGER", dailyCols["proportionalFixedCost"])
        assertEquals("INTEGER", dailyCols["netProfit"])

        // fuel_refills: pricePerLiter, discount, totalPaid devem ser INTEGER
        val cursorFuel = dbV15.query("PRAGMA table_info(`fuel_refills`)")
        val fuelCols = mutableMapOf<String, String>()
        while (cursorFuel.moveToNext()) {
            val name = cursorFuel.getString(cursorFuel.getColumnIndexOrThrow("name"))
            val type = cursorFuel.getString(cursorFuel.getColumnIndexOrThrow("type"))
            fuelCols[name] = type
        }
        cursorFuel.close()
        assertEquals("INTEGER", fuelCols["pricePerLiter"])
        assertEquals("INTEGER", fuelCols["discount"])
        assertEquals("INTEGER", fuelCols["totalPaid"])

        // maintenance_records: value deve ser INTEGER
        val cursorMaint = dbV15.query("PRAGMA table_info(`maintenance_records`)")
        var valueType = ""
        while (cursorMaint.moveToNext()) {
            val name = cursorMaint.getString(cursorMaint.getColumnIndexOrThrow("name"))
            if (name == "value") {
                valueType = cursorMaint.getString(cursorMaint.getColumnIndexOrThrow("type"))
            }
        }
        cursorMaint.close()
        assertEquals("INTEGER", valueType)

        // user_goals: 6 campos monetários devem ser INTEGER
        val cursorGoals = dbV15.query("PRAGMA table_info(`user_goals`)")
        val goalCols = mutableMapOf<String, String>()
        while (cursorGoals.moveToNext()) {
            val name = cursorGoals.getString(cursorGoals.getColumnIndexOrThrow("name"))
            val type = cursorGoals.getString(cursorGoals.getColumnIndexOrThrow("type"))
            goalCols[name] = type
        }
        cursorGoals.close()
        assertEquals("INTEGER", goalCols["dailyGoalGross"])
        assertEquals("INTEGER", goalCols["dailyGoalNet"])
        assertEquals("INTEGER", goalCols["weeklyGoalGross"])
        assertEquals("INTEGER", goalCols["weeklyGoalNet"])
        assertEquals("INTEGER", goalCols["monthlyGoalGross"])
        assertEquals("INTEGER", goalCols["monthlyGoalNet"])

        dbV15.close()
    }

    @Test
    fun testMigration14To15_preservesAndConvertsDataAccurately() {
        val dbName = "migration_14_15_data_test_${System.currentTimeMillis()}.db"

        // 1. Cria o banco de dados na versão 14 e insere registros com valores Double
        val dbV14 = helper.createDatabase(dbName, 14)

        // vehicle com monthlyFixedCosts = 180.50
        dbV14.execSQL("""
            INSERT INTO `vehicle` (
                `id`, `model`, `averageConsumption`, `fuelType`, `monthlyFixedCosts`,
                `plannedWorkDays`, `currentOdometer`, `active`, `vehicleType`, `brand`,
                `manufacturingYear`, `tankCapacity`, `hasTopBox`, `hasDeliveryBag`,
                `carBodyType`, `remoteId`, `syncStatus`, `updatedAt`, `isDeleted`
            ) VALUES (
                1, 'Honda CG 160', 40.0, 'GASOLINE', 180.50,
                22, 15000.0, 1, 'MOTORCYCLE', 'Honda',
                '2022', 16.0, 0, 1,
                'SEDAN', 'veh-1', 'SYNCED', 1000, 0
            )
        """.trimIndent())

        // vehicle_parts com price = 45.99
        dbV14.execSQL("""
            INSERT INTO `vehicle_parts` (
                `id`, `vehicleId`, `name`, `price`, `lifespanKm`,
                `runKmSinceChange`, `remoteId`, `syncStatus`, `updatedAt`, `isDeleted`
            ) VALUES (
                1, 1, 'Óleo Motor', 45.99, 1500.0,
                500.0, 'part-1', 'SYNCED', 1000, 0
            )
        """.trimIndent())

        // daily_records com faturamento e combustível
        dbV14.execSQL("""
            INSERT INTO `daily_records` (
                `id`, `dateString`, `dateTimestamp`, `platform`, `grossEarnings`,
                `deliveriesCount`, `startOdometer`, `endOdometer`, `fuelPrice`,
                `foodExpense`, `fuelCost`, `wearCost`, `proportionalFixedCost`,
                `netProfit`, `startTime`, `endTime`, `pauseDuration`,
                `remoteId`, `syncStatus`, `updatedAt`, `isDeleted`
            ) VALUES (
                1, '2026-06-15', 1781481600000, 'iFood', 150.50,
                12, 1000.0, 1100.0, 5.899,
                25.00, 14.75, 5.89, 8.20,
                96.66, '08:00', '18:00', '01:00',
                'daily-1', 'SYNCED', 1000, 0
            )
        """.trimIndent())

        // fuel_refills com 5.899 por litro
        dbV14.execSQL("""
            INSERT INTO `fuel_refills` (
                `id`, `vehicleId`, `dateTimestamp`, `dateString`, `gasStation`,
                `fuelType`, `pricePerLiter`, `liters`, `discount`, `totalPaid`,
                `odometer`, `isFullTank`, `paymentMethod`, `isInstallment`,
                `installmentsCount`, `remoteId`, `syncStatus`, `updatedAt`, `isDeleted`
            ) VALUES (
                1, 1, 1781481600000, '2026-06-15', 'Posto Ipiranga',
                'GASOLINE', 5.899, 10.0, 2.00, 56.99,
                1100.0, 1, 'PIX', 0,
                1, 'refill-1', 'SYNCED', 1000, 0
            )
        """.trimIndent())

        // maintenance_records com valor de 19.999 (deve arredondar para 2000L centavos via ROUND)
        dbV14.execSQL("""
            INSERT INTO `maintenance_records` (
                `id`, `vehicleId`, `dateTimestamp`, `dateString`, `description`,
                `location`, `value`, `odometer`, `remoteId`, `syncStatus`,
                `updatedAt`, `isDeleted`
            ) VALUES (
                1, 1, 1781481600000, '2026-06-15', 'Ajuste de corrente',
                'Oficina Central', 19.999, 1100.0, 'maint-1', 'SYNCED',
                1000, 0
            )
        """.trimIndent())

        // user_goals com metas em reais
        dbV14.execSQL("""
            INSERT INTO `user_goals` (
                `id`, `dailyGoalGross`, `dailyGoalNet`, `dailyGoalKm`,
                `dailyGoalDeliveries`, `weeklyGoalGross`, `weeklyGoalNet`,
                `weeklyGoalKm`, `weeklyGoalDeliveries`, `monthlyGoalGross`,
                `monthlyGoalNet`, `monthlyGoalKm`, `monthlyGoalDeliveries`,
                `remoteId`, `syncStatus`, `updatedAt`, `isDeleted`
            ) VALUES (
                1, 200.50, 150.25, 100.0,
                15, 1200.00, 900.00,
                600.0, 90, 5000.00,
                3800.00, 2500.0, 360,
                'goal-1', 'SYNCED', 1000, 0
            )
        """.trimIndent())

        dbV14.close()

        // 2. Executa a migração para a versão 15
        val dbV15 = helper.runMigrationsAndValidate(
            dbName,
            15,
            true,
            GiroCustoDatabase.MIGRATION_14_15
        )

        // 3. Validação dos valores convertidos para inteiros (centavos e milésimos)
        val cursorVehicle = dbV15.query("SELECT monthlyFixedCosts FROM `vehicle` WHERE id = 1")
        assertTrue(cursorVehicle.moveToFirst())
        assertEquals(18050L, cursorVehicle.getLong(0))
        cursorVehicle.close()

        val cursorParts = dbV15.query("SELECT price FROM `vehicle_parts` WHERE id = 1")
        assertTrue(cursorParts.moveToFirst())
        assertEquals(4599L, cursorParts.getLong(0))
        cursorParts.close()

        val cursorDaily = dbV15.query("SELECT grossEarnings, fuelPrice, foodExpense, fuelCost, wearCost, proportionalFixedCost, netProfit FROM `daily_records` WHERE id = 1")
        assertTrue(cursorDaily.moveToFirst())
        assertEquals(15050L, cursorDaily.getLong(0))
        assertEquals(5899L, cursorDaily.getLong(1)) // escala 1000 (milésimos)
        assertEquals(2500L, cursorDaily.getLong(2))
        assertEquals(1475L, cursorDaily.getLong(3))
        assertEquals(589L, cursorDaily.getLong(4))
        assertEquals(820L, cursorDaily.getLong(5))
        assertEquals(9666L, cursorDaily.getLong(6))
        cursorDaily.close()

        val cursorFuel = dbV15.query("SELECT pricePerLiter, discount, totalPaid FROM `fuel_refills` WHERE id = 1")
        assertTrue(cursorFuel.moveToFirst())
        assertEquals(5899L, cursorFuel.getLong(0)) // escala 1000 (milésimos)
        assertEquals(200L, cursorFuel.getLong(1))
        assertEquals(5699L, cursorFuel.getLong(2))
        cursorFuel.close()

        val cursorMaint = dbV15.query("SELECT value FROM `maintenance_records` WHERE id = 1")
        assertTrue(cursorMaint.moveToFirst())
        // 19.999 * 100 = 1999.9 -> ROUND() -> 2000L centavos
        assertEquals(2000L, cursorMaint.getLong(0))
        cursorMaint.close()

        val cursorGoals = dbV15.query("SELECT dailyGoalGross, dailyGoalNet, weeklyGoalGross, weeklyGoalNet, monthlyGoalGross, monthlyGoalNet FROM `user_goals` WHERE id = 1")
        assertTrue(cursorGoals.moveToFirst())
        assertEquals(20050L, cursorGoals.getLong(0))
        assertEquals(15025L, cursorGoals.getLong(1))
        assertEquals(120000L, cursorGoals.getLong(2))
        assertEquals(90000L, cursorGoals.getLong(3))
        assertEquals(500000L, cursorGoals.getLong(4))
        assertEquals(380000L, cursorGoals.getLong(5))
        cursorGoals.close()

        dbV15.close()
    }
}
