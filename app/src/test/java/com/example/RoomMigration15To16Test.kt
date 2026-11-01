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
class RoomMigration15To16Test {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GiroCustoDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun testMigration15To16_addsAnnualGoalNetColumnAndPreservesData() {
        val dbName = "migration_15_16_test_${System.currentTimeMillis()}.db"

        // 1. Cria o banco de dados na versão 15 usando o schema 15.json
        val dbV15 = helper.createDatabase(dbName, 15)
        dbV15.execSQL(
            """
            INSERT INTO `user_goals` (
                `id`, `dailyGoalGross`, `dailyGoalNet`, `dailyGoalKm`, `dailyGoalDeliveries`,
                `weeklyGoalGross`, `weeklyGoalNet`, `weeklyGoalKm`, `weeklyGoalDeliveries`,
                `monthlyGoalGross`, `monthlyGoalNet`, `monthlyGoalKm`, `monthlyGoalDeliveries`,
                `remoteId`, `syncStatus`, `updatedAt`, `isDeleted`
            ) VALUES (
                1, 20000, 13000, 100.0, 15,
                120000, 80000, 600.0, 90,
                500000, 320000, 2500.0, 400,
                'goal_remote_1', 'SYNCED', 1700000000000, 0
            )
            """.trimIndent()
        )
        dbV15.close()

        // 2. Executa a migração para a versão 16 e valida contra o schema 16.json
        val dbV16 = helper.runMigrationsAndValidate(
            dbName,
            16,
            true,
            GiroCustoDatabase.MIGRATION_15_16
        )

        // 3. Validação da presença e tipo da coluna annualGoalNet
        val cursorInfo = dbV16.query("PRAGMA table_info(`user_goals`)")
        var hasAnnualGoalNet = false
        var annualGoalNetType = ""
        while (cursorInfo.moveToNext()) {
            val name = cursorInfo.getString(cursorInfo.getColumnIndexOrThrow("name"))
            if (name == "annualGoalNet") {
                hasAnnualGoalNet = true
                annualGoalNetType = cursorInfo.getString(cursorInfo.getColumnIndexOrThrow("type"))
            }
        }
        cursorInfo.close()

        assertTrue("A coluna annualGoalNet deve existir na tabela user_goals", hasAnnualGoalNet)
        assertEquals("INTEGER", annualGoalNetType)

        // 4. Validação da preservação de dados existentes e valor padrão 0 para annualGoalNet
        val cursorData = dbV16.query("SELECT * FROM `user_goals` WHERE `id` = 1")
        assertTrue(cursorData.moveToFirst())
        assertEquals(20000L, cursorData.getLong(cursorData.getColumnIndexOrThrow("dailyGoalGross")))
        assertEquals(320000L, cursorData.getLong(cursorData.getColumnIndexOrThrow("monthlyGoalNet")))
        assertEquals("goal_remote_1", cursorData.getString(cursorData.getColumnIndexOrThrow("remoteId")))
        assertEquals(0L, cursorData.getLong(cursorData.getColumnIndexOrThrow("annualGoalNet")))
        cursorData.close()
    }
}
