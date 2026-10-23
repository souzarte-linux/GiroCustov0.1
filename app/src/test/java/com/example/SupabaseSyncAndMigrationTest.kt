package com.example

import android.content.ContentValues
import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.network.supabase.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SupabaseSyncAndMigrationTest {

    private lateinit var context: Context
    private lateinit var inMemoryDb: GiroCustoDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        inMemoryDb = Room.inMemoryDatabaseBuilder(context, GiroCustoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        inMemoryDb.close()
    }

    // =========================================================================
    // 1. TESTES DA MIGRATION_9_10 (ROOM DATABASE)
    // =========================================================================

    @Test
    fun testMigration9To10_addsColumnsAndPreservesPreExistingData() {
        val dbName = "test_migration_9_10_${System.currentTimeMillis()}.db"
        context.deleteDatabase(dbName)

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(9) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // 1. vehicle v9
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `vehicle` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`model` TEXT NOT NULL, " +
                                "`averageConsumption` REAL NOT NULL, " +
                                "`fuelType` TEXT NOT NULL, " +
                                "`monthlyFixedCosts` REAL NOT NULL, " +
                                "`plannedWorkDays` INTEGER NOT NULL, " +
                                "`currentOdometer` REAL NOT NULL, " +
                                "`active` INTEGER NOT NULL)"
                        )

                        // 2. vehicle_parts v9
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `vehicle_parts` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`name` TEXT NOT NULL, " +
                                "`price` REAL NOT NULL, " +
                                "`lifespanKm` REAL NOT NULL, " +
                                "`runKmSinceChange` REAL NOT NULL)"
                        )

                        // 3. daily_records v9
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `daily_records` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`dateString` TEXT NOT NULL, " +
                                "`dateTimestamp` INTEGER NOT NULL, " +
                                "`platform` TEXT NOT NULL, " +
                                "`grossEarnings` REAL NOT NULL, " +
                                "`deliveriesCount` INTEGER NOT NULL, " +
                                "`startOdometer` REAL NOT NULL, " +
                                "`endOdometer` REAL NOT NULL, " +
                                "`fuelPrice` REAL NOT NULL, " +
                                "`foodExpense` REAL NOT NULL, " +
                                "`fuelCost` REAL NOT NULL, " +
                                "`wearCost` REAL NOT NULL, " +
                                "`proportionalFixedCost` REAL NOT NULL, " +
                                "`netProfit` REAL NOT NULL, " +
                                "`startTime` TEXT NOT NULL, " +
                                "`endTime` TEXT NOT NULL, " +
                                "`pauseDuration` TEXT NOT NULL)"
                        )

                        // 4. user_profile v9
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `user_profile` (" +
                                "`id` INTEGER NOT NULL, " +
                                "`name` TEXT NOT NULL, " +
                                "`phone` TEXT NOT NULL, " +
                                "`city` TEXT NOT NULL, " +
                                "PRIMARY KEY(`id`))"
                        )

                        // 5. platforms v9
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `platforms` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`name` TEXT NOT NULL, " +
                                "`segment` TEXT NOT NULL, " +
                                "`paymentModel` TEXT NOT NULL, " +
                                "`cycle` TEXT NOT NULL, " +
                                "`paymentDay` TEXT NOT NULL, " +
                                "`fixedPayDelay` INTEGER NOT NULL, " +
                                "`cycleEntriesJson` TEXT NOT NULL, " +
                                "`bankName` TEXT NOT NULL, " +
                                "`bankAgency` TEXT NOT NULL, " +
                                "`bankAccount` TEXT NOT NULL, " +
                                "`pixKeyType` TEXT NOT NULL, " +
                                "`pixKey` TEXT NOT NULL, " +
                                "`active` INTEGER NOT NULL)"
                        )

                        // 6. fuel_refills v9
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `fuel_refills` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`vehicleId` INTEGER NOT NULL, " +
                                "`dateTimestamp` INTEGER NOT NULL, " +
                                "`dateString` TEXT NOT NULL, " +
                                "`gasStation` TEXT NOT NULL, " +
                                "`fuelType` TEXT NOT NULL, " +
                                "`pricePerLiter` REAL NOT NULL, " +
                                "`liters` REAL NOT NULL, " +
                                "`discount` REAL NOT NULL, " +
                                "`totalPaid` REAL NOT NULL, " +
                                "`odometer` REAL NOT NULL, " +
                                "`isFullTank` INTEGER NOT NULL, " +
                                "`paymentMethod` TEXT NOT NULL, " +
                                "`isInstallment` INTEGER NOT NULL, " +
                                "`installmentsCount` INTEGER NOT NULL)"
                        )

                        // 7. maintenance_records v9
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `maintenance_records` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`vehicleId` INTEGER NOT NULL, " +
                                "`dateTimestamp` INTEGER NOT NULL, " +
                                "`dateString` TEXT NOT NULL, " +
                                "`description` TEXT NOT NULL, " +
                                "`location` TEXT NOT NULL, " +
                                "`value` REAL NOT NULL, " +
                                "`odometer` REAL NOT NULL)"
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )

        val sqliteDb = helper.writableDatabase

        // Inserir registros pré-existentes na versão 9
        val vValues = ContentValues().apply {
            put("model", "Honda Titan 160")
            put("averageConsumption", 38.5)
            put("fuelType", "Gasolina")
            put("monthlyFixedCosts", 150.0)
            put("plannedWorkDays", 22)
            put("currentOdometer", 15200.0)
            put("active", 1)
        }
        val vehicleRowId = sqliteDb.insert("vehicle", 0, vValues)
        assertTrue(vehicleRowId > 0)

        val partValues = ContentValues().apply {
            put("name", "Óleo 10W30")
            put("price", 45.0)
            put("lifespanKm", 1500.0)
            put("runKmSinceChange", 800.0)
        }
        sqliteDb.insert("vehicle_parts", 0, partValues)

        val recordValues = ContentValues().apply {
            put("dateString", "2026-10-20")
            put("dateTimestamp", 1761000000000L)
            put("platform", "iFood")
            put("grossEarnings", 220.0)
            put("deliveriesCount", 14)
            put("startOdometer", 15000.0)
            put("endOdometer", 15110.0)
            put("fuelPrice", 5.89)
            put("foodExpense", 25.0)
            put("fuelCost", 17.07)
            put("wearCost", 8.80)
            put("proportionalFixedCost", 6.82)
            put("netProfit", 162.31)
            put("startTime", "08:00")
            put("endTime", "17:00")
            put("pauseDuration", "01:00")
        }
        sqliteDb.insert("daily_records", 0, recordValues)

        val profileValues = ContentValues().apply {
            put("id", 1)
            put("name", "Entregador GiroCusto")
            put("phone", "11999998888")
            put("city", "São Paulo")
        }
        sqliteDb.insert("user_profile", 0, profileValues)

        val platformValues = ContentValues().apply {
            put("name", "iFood")
            put("segment", "Delivery")
            put("paymentModel", "Semanal")
            put("cycle", "Qua-Ter")
            put("paymentDay", "QUA")
            put("fixedPayDelay", 7)
            put("cycleEntriesJson", "[]")
            put("bankName", "Nubank")
            put("bankAgency", "0001")
            put("bankAccount", "12345-6")
            put("pixKeyType", "Chave Aleatória")
            put("pixKey", "abcd-1234")
            put("active", 1)
        }
        sqliteDb.insert("platforms", 0, platformValues)

        val refillValues = ContentValues().apply {
            put("vehicleId", vehicleRowId)
            put("dateTimestamp", 1761000000000L)
            put("dateString", "2026-10-20")
            put("gasStation", "Posto Shell")
            put("fuelType", "Gasolina")
            put("pricePerLiter", 5.89)
            put("liters", 10.0)
            put("discount", 0.0)
            put("totalPaid", 58.90)
            put("odometer", 15000.0)
            put("isFullTank", 1)
            put("paymentMethod", "Pix")
            put("isInstallment", 0)
            put("installmentsCount", 1)
        }
        sqliteDb.insert("fuel_refills", 0, refillValues)

        val maintValues = ContentValues().apply {
            put("vehicleId", vehicleRowId)
            put("dateTimestamp", 1761000000000L)
            put("dateString", "2026-10-20")
            put("description", "Troca de óleo")
            put("location", "Oficina Central")
            put("value", 45.0)
            put("odometer", 15000.0)
        }
        sqliteDb.insert("maintenance_records", 0, maintValues)

        // 2. Executa a MIGRATION_9_10
        GiroCustoDatabase.MIGRATION_9_10.migrate(sqliteDb)

        // 3. Valida colunas e integridade dos dados em todas as 7 tabelas
        val tables = listOf(
            "vehicle",
            "vehicle_parts",
            "daily_records",
            "user_profile",
            "platforms",
            "fuel_refills",
            "maintenance_records"
        )

        for (table in tables) {
            val cursor = sqliteDb.query("SELECT * FROM `$table`")
            assertNotNull("Cursor não pode ser nulo para $table", cursor)
            assertTrue("Tabela $table deve conter pelo menos 1 registro", cursor.moveToFirst())

            // Valida que as novas colunas existem
            val remoteIdIdx = cursor.getColumnIndex("remoteId")
            val syncStatusIdx = cursor.getColumnIndex("syncStatus")
            val updatedAtIdx = cursor.getColumnIndex("updatedAt")
            val isDeletedIdx = cursor.getColumnIndex("isDeleted")

            assertTrue("Coluna remoteId deve existir em $table", remoteIdIdx != -1)
            assertTrue("Coluna syncStatus deve existir em $table", syncStatusIdx != -1)
            assertTrue("Coluna updatedAt deve existir em $table", updatedAtIdx != -1)
            assertTrue("Coluna isDeleted deve existir em $table", isDeletedIdx != -1)

            // Valida valores padrão
            assertEquals("remoteId padrão deve ser vazio em $table", "", cursor.getString(remoteIdIdx))
            assertEquals("syncStatus padrão deve ser SYNCED em $table", "SYNCED", cursor.getString(syncStatusIdx))
            assertEquals("updatedAt padrão deve ser 0 em $table", 0L, cursor.getLong(updatedAtIdx))
            assertEquals("isDeleted padrão deve ser 0 em $table", 0, cursor.getInt(isDeletedIdx))

            cursor.close()
        }

        // Valida dados específicos pré-existentes não corrompidos
        val vCursor = sqliteDb.query("SELECT model, averageConsumption, active FROM vehicle WHERE id = $vehicleRowId")
        assertTrue(vCursor.moveToFirst())
        assertEquals("Honda Titan 160", vCursor.getString(0))
        assertEquals(38.5, vCursor.getDouble(1), 0.001)
        assertEquals(1, vCursor.getInt(2))
        vCursor.close()

        val rCursor = sqliteDb.query("SELECT platform, grossEarnings, netProfit FROM daily_records LIMIT 1")
        assertTrue(rCursor.moveToFirst())
        assertEquals("iFood", rCursor.getString(0))
        assertEquals(220.0, rCursor.getDouble(1), 0.001)
        assertEquals(162.31, rCursor.getDouble(2), 0.001)
        rCursor.close()

        sqliteDb.close()
        helper.close()
    }

    @Test
    fun testMigration9To10_validatesWithRoomVersion10() = runTest {
        val dbName = "test_room_migrated_v10_${System.currentTimeMillis()}.db"
        context.deleteDatabase(dbName)

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(9) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `vehicle` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `model` TEXT NOT NULL, `averageConsumption` REAL NOT NULL, `fuelType` TEXT NOT NULL, `monthlyFixedCosts` REAL NOT NULL, `plannedWorkDays` INTEGER NOT NULL, `currentOdometer` REAL NOT NULL, `active` INTEGER NOT NULL)"
                        )
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `vehicle_parts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `price` REAL NOT NULL, `lifespanKm` REAL NOT NULL, `runKmSinceChange` REAL NOT NULL)"
                        )
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `daily_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `dateString` TEXT NOT NULL, `dateTimestamp` INTEGER NOT NULL, `platform` TEXT NOT NULL, `grossEarnings` REAL NOT NULL, `deliveriesCount` INTEGER NOT NULL, `startOdometer` REAL NOT NULL, `endOdometer` REAL NOT NULL, `fuelPrice` REAL NOT NULL, `foodExpense` REAL NOT NULL, `fuelCost` REAL NOT NULL, `wearCost` REAL NOT NULL, `proportionalFixedCost` REAL NOT NULL, `netProfit` REAL NOT NULL, `startTime` TEXT NOT NULL, `endTime` TEXT NOT NULL, `pauseDuration` TEXT NOT NULL)"
                        )
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `user_profile` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, `city` TEXT NOT NULL, PRIMARY KEY(`id`))"
                        )
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `platforms` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `segment` TEXT NOT NULL, `paymentModel` TEXT NOT NULL, `cycle` TEXT NOT NULL, `paymentDay` TEXT NOT NULL, `fixedPayDelay` INTEGER NOT NULL, `cycleEntriesJson` TEXT NOT NULL, `bankName` TEXT NOT NULL, `bankAgency` TEXT NOT NULL, `bankAccount` TEXT NOT NULL, `pixKeyType` TEXT NOT NULL, `pixKey` TEXT NOT NULL, `active` INTEGER NOT NULL)"
                        )
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `fuel_refills` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER NOT NULL, `dateTimestamp` INTEGER NOT NULL, `dateString` TEXT NOT NULL, `gasStation` TEXT NOT NULL, `fuelType` TEXT NOT NULL, `pricePerLiter` REAL NOT NULL, `liters` REAL NOT NULL, `discount` REAL NOT NULL, `totalPaid` REAL NOT NULL, `odometer` REAL NOT NULL, `isFullTank` INTEGER NOT NULL, `paymentMethod` TEXT NOT NULL, `isInstallment` INTEGER NOT NULL, `installmentsCount` INTEGER NOT NULL)"
                        )
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `maintenance_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER NOT NULL, `dateTimestamp` INTEGER NOT NULL, `dateString` TEXT NOT NULL, `description` TEXT NOT NULL, `location` TEXT NOT NULL, `value` REAL NOT NULL, `odometer` REAL NOT NULL)"
                        )
                        // Room master table hash para v9
                        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
                        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'b28ae45778af508cd50a2917665dc085')")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        val rawDb = helper.writableDatabase
        val cv = ContentValues().apply {
            put("model", "Yamaha Fazer 250")
            put("averageConsumption", 30.0)
            put("fuelType", "Gasolina")
            put("monthlyFixedCosts", 200.0)
            put("plannedWorkDays", 25)
            put("currentOdometer", 5000.0)
            put("active", 1)
        }
        rawDb.insert("vehicle", 0, cv)
        rawDb.close()
        helper.close()

        // Abre agora com Room versão 12 executando MIGRATION_9_10, MIGRATION_10_11 e MIGRATION_11_12
        val roomDb = Room.databaseBuilder(context, GiroCustoDatabase::class.java, dbName)
            .addMigrations(GiroCustoDatabase.MIGRATION_9_10, GiroCustoDatabase.MIGRATION_10_11, GiroCustoDatabase.MIGRATION_11_12)
            .allowMainThreadQueries()
            .build()

        val activeVehicle = roomDb.vehicleDao().getActiveVehicle()
        assertNotNull(activeVehicle)
        assertEquals("Yamaha Fazer 250", activeVehicle?.model)
        assertEquals("", activeVehicle?.remoteId)
        assertEquals(SyncStatus.SYNCED, activeVehicle?.syncStatus)
        assertEquals(0L, activeVehicle?.updatedAt)
        assertFalse(activeVehicle?.isDeleted ?: true)
        assertEquals("MOTO", activeVehicle?.vehicleType)
        assertEquals(16.0, activeVehicle?.tankCapacity ?: 0.0, 0.001)

        roomDb.close()
    }

    @Test
    fun testMigration10To11_addsColumnsAndPreservesPreExistingData() {
        val dbName = "test_migration_10_11_${System.currentTimeMillis()}.db"
        context.deleteDatabase(dbName)

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(10) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // user_profile v10
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `user_profile` (" +
                                "`id` INTEGER NOT NULL, " +
                                "`name` TEXT NOT NULL, " +
                                "`phone` TEXT NOT NULL, " +
                                "`city` TEXT NOT NULL, " +
                                "`remoteId` TEXT NOT NULL, " +
                                "`syncStatus` TEXT NOT NULL, " +
                                "`updatedAt` INTEGER NOT NULL, " +
                                "`isDeleted` INTEGER NOT NULL, " +
                                "PRIMARY KEY(`id`))"
                        )

                        // vehicle v10
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `vehicle` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`model` TEXT NOT NULL, " +
                                "`averageConsumption` REAL NOT NULL, " +
                                "`fuelType` TEXT NOT NULL, " +
                                "`monthlyFixedCosts` REAL NOT NULL, " +
                                "`plannedWorkDays` INTEGER NOT NULL, " +
                                "`currentOdometer` REAL NOT NULL, " +
                                "`active` INTEGER NOT NULL, " +
                                "`remoteId` TEXT NOT NULL, " +
                                "`syncStatus` TEXT NOT NULL, " +
                                "`updatedAt` INTEGER NOT NULL, " +
                                "`isDeleted` INTEGER NOT NULL)"
                        )

                        // Room master table hash para v10
                        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
                        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '94d0f328a35d91e84084f5582a00e07b')")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )

        val sqliteDb = helper.writableDatabase

        // Inserir registros existentes na versão 10
        val pValues = ContentValues().apply {
            put("id", 1)
            put("name", "Carlos Entregador")
            put("phone", "11987654321")
            put("city", "Osasco")
            put("remoteId", "remote-prof-10")
            put("syncStatus", "SYNCED")
            put("updatedAt", 1761000000000L)
            put("isDeleted", 0)
        }
        sqliteDb.insert("user_profile", 0, pValues)

        val vValues = ContentValues().apply {
            put("model", "Honda CG 160 Fan")
            put("averageConsumption", 42.0)
            put("fuelType", "Gasolina")
            put("monthlyFixedCosts", 190.0)
            put("plannedWorkDays", 26)
            put("currentOdometer", 18500.0)
            put("active", 1)
            put("remoteId", "remote-veh-10")
            put("syncStatus", "SYNCED")
            put("updatedAt", 1761000000000L)
            put("isDeleted", 0)
        }
        val vehicleId = sqliteDb.insert("vehicle", 0, vValues)
        assertTrue(vehicleId > 0)

        // Executar MIGRATION_10_11
        GiroCustoDatabase.MIGRATION_10_11.migrate(sqliteDb)

        // Valida colunas de user_profile
        val pCursor = sqliteDb.query("SELECT * FROM user_profile WHERE id = 1")
        assertTrue(pCursor.moveToFirst())
        assertEquals("Carlos Entregador", pCursor.getString(pCursor.getColumnIndexOrThrow("name")))
        assertEquals("11987654321", pCursor.getString(pCursor.getColumnIndexOrThrow("phone")))
        assertEquals("Osasco", pCursor.getString(pCursor.getColumnIndexOrThrow("city")))
        assertEquals("", pCursor.getString(pCursor.getColumnIndexOrThrow("email")))
        assertEquals(1, pCursor.getInt(pCursor.getColumnIndexOrThrow("isWhatsapp")))
        assertEquals("", pCursor.getString(pCursor.getColumnIndexOrThrow("cep")))
        assertEquals("", pCursor.getString(pCursor.getColumnIndexOrThrow("street")))
        assertEquals("", pCursor.getString(pCursor.getColumnIndexOrThrow("addressNumber")))
        assertEquals("", pCursor.getString(pCursor.getColumnIndexOrThrow("neighborhood")))
        assertEquals("", pCursor.getString(pCursor.getColumnIndexOrThrow("stateUf")))
        assertEquals(0, pCursor.getInt(pCursor.getColumnIndexOrThrow("onboardingCompleted")))
        pCursor.close()

        // Valida colunas de vehicle
        val vCursor = sqliteDb.query("SELECT * FROM vehicle WHERE id = $vehicleId")
        assertTrue(vCursor.moveToFirst())
        assertEquals("Honda CG 160 Fan", vCursor.getString(vCursor.getColumnIndexOrThrow("model")))
        assertEquals(42.0, vCursor.getDouble(vCursor.getColumnIndexOrThrow("averageConsumption")), 0.001)
        assertEquals("MOTO", vCursor.getString(vCursor.getColumnIndexOrThrow("vehicleType")))
        assertEquals("", vCursor.getString(vCursor.getColumnIndexOrThrow("brand")))
        assertEquals("", vCursor.getString(vCursor.getColumnIndexOrThrow("manufacturingYear")))
        assertEquals(16.0, vCursor.getDouble(vCursor.getColumnIndexOrThrow("tankCapacity")), 0.001)
        assertEquals(0, vCursor.getInt(vCursor.getColumnIndexOrThrow("hasTopBox")))
        assertEquals(0, vCursor.getInt(vCursor.getColumnIndexOrThrow("hasDeliveryBag")))
        assertEquals("", vCursor.getString(vCursor.getColumnIndexOrThrow("carBodyType")))
        vCursor.close()

        sqliteDb.close()
        helper.close()
    }

    @Test
    fun testMigration10To11_validatesWithRoomVersion11() = runTest {
        val dbName = "test_room_migrated_v11_${System.currentTimeMillis()}.db"
        context.deleteDatabase(dbName)

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(10) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // 1. vehicle v10
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `vehicle` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `model` TEXT NOT NULL, `averageConsumption` REAL NOT NULL, `fuelType` TEXT NOT NULL, `monthlyFixedCosts` REAL NOT NULL, `plannedWorkDays` INTEGER NOT NULL, `currentOdometer` REAL NOT NULL, `active` INTEGER NOT NULL, `remoteId` TEXT NOT NULL, `syncStatus` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL)"
                        )
                        // 2. vehicle_parts v10
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `vehicle_parts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `price` REAL NOT NULL, `lifespanKm` REAL NOT NULL, `runKmSinceChange` REAL NOT NULL, `remoteId` TEXT NOT NULL, `syncStatus` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL)"
                        )
                        // 3. daily_records v10
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `daily_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `dateString` TEXT NOT NULL, `dateTimestamp` INTEGER NOT NULL, `platform` TEXT NOT NULL, `grossEarnings` REAL NOT NULL, `deliveriesCount` INTEGER NOT NULL, `startOdometer` REAL NOT NULL, `endOdometer` REAL NOT NULL, `fuelPrice` REAL NOT NULL, `foodExpense` REAL NOT NULL, `fuelCost` REAL NOT NULL, `wearCost` REAL NOT NULL, `proportionalFixedCost` REAL NOT NULL, `netProfit` REAL NOT NULL, `startTime` TEXT NOT NULL, `endTime` TEXT NOT NULL, `pauseDuration` TEXT NOT NULL, `remoteId` TEXT NOT NULL, `syncStatus` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL)"
                        )
                        // 4. user_profile v10
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `user_profile` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, `city` TEXT NOT NULL, `remoteId` TEXT NOT NULL, `syncStatus` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                        )
                        // 5. platforms v10
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `platforms` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `segment` TEXT NOT NULL, `paymentModel` TEXT NOT NULL, `cycle` TEXT NOT NULL, `paymentDay` TEXT NOT NULL, `fixedPayDelay` INTEGER NOT NULL, `cycleEntriesJson` TEXT NOT NULL, `bankName` TEXT NOT NULL, `bankAgency` TEXT NOT NULL, `bankAccount` TEXT NOT NULL, `pixKeyType` TEXT NOT NULL, `pixKey` TEXT NOT NULL, `active` INTEGER NOT NULL, `remoteId` TEXT NOT NULL, `syncStatus` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL)"
                        )
                        // 6. fuel_refills v10
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `fuel_refills` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER NOT NULL, `dateTimestamp` INTEGER NOT NULL, `dateString` TEXT NOT NULL, `gasStation` TEXT NOT NULL, `fuelType` TEXT NOT NULL, `pricePerLiter` REAL NOT NULL, `liters` REAL NOT NULL, `discount` REAL NOT NULL, `totalPaid` REAL NOT NULL, `odometer` REAL NOT NULL, `isFullTank` INTEGER NOT NULL, `paymentMethod` TEXT NOT NULL, `isInstallment` INTEGER NOT NULL, `installmentsCount` INTEGER NOT NULL, `remoteId` TEXT NOT NULL, `syncStatus` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL)"
                        )
                        // 7. maintenance_records v10
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `maintenance_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER NOT NULL, `dateTimestamp` INTEGER NOT NULL, `dateString` TEXT NOT NULL, `description` TEXT NOT NULL, `location` TEXT NOT NULL, `value` REAL NOT NULL, `odometer` REAL NOT NULL, `remoteId` TEXT NOT NULL, `syncStatus` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL)"
                        )
                        // Room master table hash para v10
                        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
                        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '94d0f328a35d91e84084f5582a00e07b')")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        val rawDb = helper.writableDatabase

        // Inserir registro de veículo v10
        val cvVehicle = ContentValues().apply {
            put("model", "Honda CG 160 Start")
            put("averageConsumption", 41.5)
            put("fuelType", "Gasolina")
            put("monthlyFixedCosts", 175.0)
            put("plannedWorkDays", 22)
            put("currentOdometer", 12300.0)
            put("active", 1)
            put("remoteId", "remote-veh-v10-start")
            put("syncStatus", "SYNCED")
            put("updatedAt", 1761000000000L)
            put("isDeleted", 0)
        }
        val insertedVehId = rawDb.insert("vehicle", 0, cvVehicle)

        // Inserir registro de perfil v10
        val cvProfile = ContentValues().apply {
            put("id", 1)
            put("name", "Roberta Silva")
            put("phone", "11988887777")
            put("city", "Campinas")
            put("remoteId", "remote-prof-v10-roberta")
            put("syncStatus", "SYNCED")
            put("updatedAt", 1761000000000L)
            put("isDeleted", 0)
        }
        rawDb.insert("user_profile", 0, cvProfile)

        rawDb.close()
        helper.close()

        // Abre agora com Room versão 12 executando MIGRATION_10_11 e MIGRATION_11_12
        val roomDb = Room.databaseBuilder(context, GiroCustoDatabase::class.java, dbName)
            .addMigrations(GiroCustoDatabase.MIGRATION_10_11, GiroCustoDatabase.MIGRATION_11_12)
            .allowMainThreadQueries()
            .build()

        val activeVehicle = roomDb.vehicleDao().getVehicleById(insertedVehId)
        assertNotNull(activeVehicle)
        assertEquals("Honda CG 160 Start", activeVehicle?.model)
        assertEquals("remote-veh-v10-start", activeVehicle?.remoteId)
        assertEquals(SyncStatus.SYNCED, activeVehicle?.syncStatus)
        assertEquals(1761000000000L, activeVehicle?.updatedAt)
        assertFalse(activeVehicle?.isDeleted ?: true)
        // Valores default das novas colunas na migração
        assertEquals("MOTO", activeVehicle?.vehicleType)
        assertEquals("", activeVehicle?.brand)
        assertEquals("", activeVehicle?.manufacturingYear)
        assertEquals(16.0, activeVehicle?.tankCapacity ?: 0.0, 0.001)
        assertFalse(activeVehicle?.hasTopBox ?: true)
        assertFalse(activeVehicle?.hasDeliveryBag ?: true)
        assertEquals("", activeVehicle?.carBodyType)

        val profile = roomDb.userProfileDao().getUserProfile()
        assertNotNull(profile)
        assertEquals(1, profile?.id)
        assertEquals("Roberta Silva", profile?.name)
        assertEquals("11988887777", profile?.phone)
        assertEquals("Campinas", profile?.city)
        assertEquals("remote-prof-v10-roberta", profile?.remoteId)
        // Novos campos com valores default
        assertEquals("", profile?.email)
        assertEquals(true, profile?.isWhatsapp)
        assertEquals("", profile?.cep)
        assertEquals("", profile?.street)
        assertEquals("", profile?.addressNumber)
        assertEquals("", profile?.neighborhood)
        assertEquals("", profile?.stateUf)
        assertEquals(false, profile?.onboardingCompleted)

        roomDb.close()
    }

    @Test
    fun testMigration11To12_addsVehicleIdAndLinksToActiveVehicle() = runTest {
        val dbName = "test_migration_11_12_${System.currentTimeMillis()}.db"
        context.deleteDatabase(dbName)

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(11) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // vehicle v11
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `vehicle` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`model` TEXT NOT NULL, " +
                                "`averageConsumption` REAL NOT NULL, " +
                                "`fuelType` TEXT NOT NULL, " +
                                "`monthlyFixedCosts` REAL NOT NULL, " +
                                "`plannedWorkDays` INTEGER NOT NULL, " +
                                "`currentOdometer` REAL NOT NULL, " +
                                "`active` INTEGER NOT NULL, " +
                                "`remoteId` TEXT NOT NULL DEFAULT '', " +
                                "`syncStatus` TEXT NOT NULL DEFAULT 'SYNCED', " +
                                "`updatedAt` INTEGER NOT NULL DEFAULT 0, " +
                                "`isDeleted` INTEGER NOT NULL DEFAULT 0, " +
                                "`vehicleType` TEXT NOT NULL DEFAULT 'MOTO', " +
                                "`brand` TEXT NOT NULL DEFAULT '', " +
                                "`manufacturingYear` TEXT NOT NULL DEFAULT '', " +
                                "`tankCapacity` REAL NOT NULL DEFAULT 16.0, " +
                                "`hasTopBox` INTEGER NOT NULL DEFAULT 0, " +
                                "`hasDeliveryBag` INTEGER NOT NULL DEFAULT 0, " +
                                "`carBodyType` TEXT NOT NULL DEFAULT '')"
                        )

                        // vehicle_parts v11 (sem vehicleId)
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `vehicle_parts` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`name` TEXT NOT NULL, " +
                                "`price` REAL NOT NULL, " +
                                "`lifespanKm` REAL NOT NULL, " +
                                "`runKmSinceChange` REAL NOT NULL, " +
                                "`remoteId` TEXT NOT NULL DEFAULT '', " +
                                "`syncStatus` TEXT NOT NULL DEFAULT 'SYNCED', " +
                                "`updatedAt` INTEGER NOT NULL DEFAULT 0, " +
                                "`isDeleted` INTEGER NOT NULL DEFAULT 0)"
                        )

                        // daily_records v11
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `daily_records` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`dateString` TEXT NOT NULL, " +
                                "`dateTimestamp` INTEGER NOT NULL, " +
                                "`platform` TEXT NOT NULL, " +
                                "`grossEarnings` REAL NOT NULL, " +
                                "`deliveriesCount` INTEGER NOT NULL, " +
                                "`startOdometer` REAL NOT NULL, " +
                                "`endOdometer` REAL NOT NULL, " +
                                "`fuelPrice` REAL NOT NULL, " +
                                "`foodExpense` REAL NOT NULL, " +
                                "`fuelCost` REAL NOT NULL, " +
                                "`wearCost` REAL NOT NULL, " +
                                "`proportionalFixedCost` REAL NOT NULL, " +
                                "`netProfit` REAL NOT NULL, " +
                                "`startTime` TEXT NOT NULL, " +
                                "`endTime` TEXT NOT NULL, " +
                                "`pauseDuration` TEXT NOT NULL, " +
                                "`remoteId` TEXT NOT NULL DEFAULT '', " +
                                "`syncStatus` TEXT NOT NULL DEFAULT 'SYNCED', " +
                                "`updatedAt` INTEGER NOT NULL DEFAULT 0, " +
                                "`isDeleted` INTEGER NOT NULL DEFAULT 0)"
                        )

                        // user_profile v11
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `user_profile` (" +
                                "`id` INTEGER NOT NULL, " +
                                "`name` TEXT NOT NULL, " +
                                "`phone` TEXT NOT NULL, " +
                                "`city` TEXT NOT NULL, " +
                                "`isWhatsapp` INTEGER NOT NULL DEFAULT 1, " +
                                "`cep` TEXT NOT NULL DEFAULT '', " +
                                "`street` TEXT NOT NULL DEFAULT '', " +
                                "`addressNumber` TEXT NOT NULL DEFAULT '', " +
                                "`neighborhood` TEXT NOT NULL DEFAULT '', " +
                                "`stateUf` TEXT NOT NULL DEFAULT '', " +
                                "`onboardingCompleted` INTEGER NOT NULL DEFAULT 0, " +
                                "`remoteId` TEXT NOT NULL DEFAULT '', " +
                                "`syncStatus` TEXT NOT NULL DEFAULT 'SYNCED', " +
                                "`updatedAt` INTEGER NOT NULL DEFAULT 0, " +
                                "`isDeleted` INTEGER NOT NULL DEFAULT 0, " +
                                "`email` TEXT NOT NULL DEFAULT '', " +
                                "PRIMARY KEY(`id`))"
                        )

                        // platforms v11
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `platforms` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`name` TEXT NOT NULL, " +
                                "`segment` TEXT NOT NULL, " +
                                "`paymentModel` TEXT NOT NULL, " +
                                "`cycle` TEXT NOT NULL, " +
                                "`paymentDay` TEXT NOT NULL, " +
                                "`fixedPayDelay` INTEGER NOT NULL, " +
                                "`cycleEntriesJson` TEXT NOT NULL, " +
                                "`bankName` TEXT NOT NULL, " +
                                "`bankAgency` TEXT NOT NULL, " +
                                "`bankAccount` TEXT NOT NULL, " +
                                "`pixKeyType` TEXT NOT NULL, " +
                                "`pixKey` TEXT NOT NULL, " +
                                "`active` INTEGER NOT NULL, " +
                                "`remoteId` TEXT NOT NULL DEFAULT '', " +
                                "`syncStatus` TEXT NOT NULL DEFAULT 'SYNCED', " +
                                "`updatedAt` INTEGER NOT NULL DEFAULT 0, " +
                                "`isDeleted` INTEGER NOT NULL DEFAULT 0)"
                        )

                        // fuel_refills v11
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `fuel_refills` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`vehicleId` INTEGER NOT NULL, " +
                                "`dateTimestamp` INTEGER NOT NULL, " +
                                "`dateString` TEXT NOT NULL, " +
                                "`gasStation` TEXT NOT NULL, " +
                                "`fuelType` TEXT NOT NULL, " +
                                "`pricePerLiter` REAL NOT NULL, " +
                                "`liters` REAL NOT NULL, " +
                                "`discount` REAL NOT NULL, " +
                                "`totalPaid` REAL NOT NULL, " +
                                "`odometer` REAL NOT NULL, " +
                                "`isFullTank` INTEGER NOT NULL, " +
                                "`paymentMethod` TEXT NOT NULL, " +
                                "`isInstallment` INTEGER NOT NULL, " +
                                "`installmentsCount` INTEGER NOT NULL, " +
                                "`remoteId` TEXT NOT NULL DEFAULT '', " +
                                "`syncStatus` TEXT NOT NULL DEFAULT 'SYNCED', " +
                                "`updatedAt` INTEGER NOT NULL DEFAULT 0, " +
                                "`isDeleted` INTEGER NOT NULL DEFAULT 0)"
                        )

                        // maintenance_records v11
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `maintenance_records` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`vehicleId` INTEGER NOT NULL, " +
                                "`dateTimestamp` INTEGER NOT NULL, " +
                                "`dateString` TEXT NOT NULL, " +
                                "`description` TEXT NOT NULL, " +
                                "`location` TEXT NOT NULL, " +
                                "`value` REAL NOT NULL, " +
                                "`odometer` REAL NOT NULL, " +
                                "`remoteId` TEXT NOT NULL DEFAULT '', " +
                                "`syncStatus` TEXT NOT NULL DEFAULT 'SYNCED', " +
                                "`updatedAt` INTEGER NOT NULL DEFAULT 0, " +
                                "`isDeleted` INTEGER NOT NULL DEFAULT 0)"
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )

        val rawDb = helper.writableDatabase

        // Inserir veículo ativo com id 10
        val cvActiveVeh = ContentValues().apply {
            put("id", 10L)
            put("model", "Honda Titan 160")
            put("averageConsumption", 40.0)
            put("fuelType", "Gasolina")
            put("monthlyFixedCosts", 150.0)
            put("plannedWorkDays", 22)
            put("currentOdometer", 12000.0)
            put("active", 1)
        }
        rawDb.insert("vehicle", 0, cvActiveVeh)

        // Inserir veículo inativo com id 20
        val cvInactiveVeh = ContentValues().apply {
            put("id", 20L)
            put("model", "Fiat Uno")
            put("averageConsumption", 12.0)
            put("fuelType", "Gasolina")
            put("monthlyFixedCosts", 300.0)
            put("plannedWorkDays", 20)
            put("currentOdometer", 85000.0)
            put("active", 0)
        }
        rawDb.insert("vehicle", 0, cvInactiveVeh)

        // Inserir peças de desgaste v11 (sem vehicleId)
        val cvPart1 = ContentValues().apply {
            put("name", "Óleo do Motor")
            put("price", 45.0)
            put("lifespanKm", 1500.0)
            put("runKmSinceChange", 800.0)
        }
        rawDb.insert("vehicle_parts", 0, cvPart1)

        val cvPart2 = ContentValues().apply {
            put("name", "Pneu Traseiro")
            put("price", 220.0)
            put("lifespanKm", 12000.0)
            put("runKmSinceChange", 4000.0)
        }
        rawDb.insert("vehicle_parts", 0, cvPart2)

        rawDb.close()
        helper.close()

        // Abre agora com Room versão 12 executando MIGRATION_11_12
        val roomDb = Room.databaseBuilder(context, GiroCustoDatabase::class.java, dbName)
            .addMigrations(GiroCustoDatabase.MIGRATION_11_12)
            .allowMainThreadQueries()
            .build()

        val partDao = roomDb.vehiclePartDao()
        val allParts = partDao.getAllParts()
        assertEquals(2, allParts.size)

        // As peças existentes devem ter sido vinculadas ao veículo ativo (id = 10)
        assertTrue(allParts.all { it.vehicleId == 10L })

        val partsForActive = partDao.getPartsForVehicle(10L)
        assertEquals(2, partsForActive.size)

        // Adiciona peça vinculada explicitamente ao veículo 20
        partDao.insertPart(
            VehiclePart(
                vehicleId = 20L,
                name = "Pneu de Carro",
                price = 350.0,
                lifespanKm = 50000.0
            )
        )

        val partsForUno = partDao.getPartsForVehicle(20L)
        assertEquals(1, partsForUno.size)
        assertEquals("Pneu de Carro", partsForUno[0].name)

        roomDb.close()
    }

    // =========================================================================
    // 2. TESTES DE MAPEAMENTO BIDIRECIONAL (SUPABASE MAPPERS)
    // =========================================================================

    @Test
    fun testVehicleBidirectionalMapping() {
        val userId = "test_user_uuid"
        val vehicle = Vehicle(
            id = 10L,
            model = "Honda CG 160",
            averageConsumption = 36.5,
            fuelType = "Gasolina",
            monthlyFixedCosts = 180.0,
            plannedWorkDays = 24,
            currentOdometer = 24500.0,
            active = true,
            remoteId = "veh-uuid-99",
            syncStatus = SyncStatus.PENDING_INSERT,
            updatedAt = 1761000000000L,
            isDeleted = false
        )

        // Entity -> DTO
        val dto = vehicle.toDto(userId)
        assertEquals("veh-uuid-99", dto.id)
        assertEquals(userId, dto.userId)
        assertEquals("Honda CG 160", dto.model)
        assertEquals(36.5, dto.averageConsumption, 0.001)
        assertEquals("Gasolina", dto.fuelType)
        assertEquals(180.0, dto.monthlyFixedCosts, 0.001)
        assertEquals(24, dto.plannedWorkDays)
        assertEquals(24500.0, dto.currentOdometer, 0.001)
        assertTrue(dto.active)
        assertNull("deletedAt deve ser nulo quando isDeleted=false", dto.deletedAt)

        // DTO -> Entity
        val reconstructed = dto.toEntity(existingId = 10L)
        assertEquals(10L, reconstructed.id)
        assertEquals("veh-uuid-99", reconstructed.remoteId)
        assertEquals("Honda CG 160", reconstructed.model)
        assertEquals(36.5, reconstructed.averageConsumption, 0.001)
        assertEquals(SyncStatus.SYNCED, reconstructed.syncStatus)
        assertFalse(reconstructed.isDeleted)

        // Soft-delete: isDeleted = true deve preencher deletedAt no DTO
        val deletedVehicle = vehicle.copy(isDeleted = true, updatedAt = 1761000000000L)
        val deletedDto = deletedVehicle.toDto(userId)
        assertNotNull(deletedDto.deletedAt)

        val restoredFromDeleted = deletedDto.toEntity(existingId = 10L)
        assertTrue(restoredFromDeleted.isDeleted)
    }

    @Test
    fun testVehicleWithTechnicalSpecsBidirectionalMapping() {
        val userId = "user-specs-123"
        val vehicle = Vehicle(
            id = 42L,
            model = "Fiat Strada",
            averageConsumption = 12.5,
            fuelType = "Flex",
            monthlyFixedCosts = 450.0,
            plannedWorkDays = 26,
            currentOdometer = 35000.0,
            active = true,
            vehicleType = "CARRO",
            brand = "Fiat",
            manufacturingYear = "2023",
            tankCapacity = 55.0,
            hasTopBox = false,
            hasDeliveryBag = true,
            carBodyType = "Pickup",
            remoteId = "veh-strada-42",
            syncStatus = SyncStatus.PENDING_INSERT,
            updatedAt = 1761000000000L,
            isDeleted = false
        )

        val dto = vehicle.toDto(userId)
        assertEquals("veh-strada-42", dto.id)
        assertEquals(userId, dto.userId)
        assertEquals("Fiat Strada", dto.model)
        assertEquals("CARRO", dto.vehicleType)
        assertEquals("Fiat", dto.brand)
        assertEquals("2023", dto.manufacturingYear)
        assertEquals(55.0, dto.tankCapacity, 0.001)
        assertFalse(dto.hasTopBox)
        assertTrue(dto.hasDeliveryBag)
        assertEquals("Pickup", dto.carBodyType)

        val reconstructed = dto.toEntity(existingId = 42L)
        assertEquals(42L, reconstructed.id)
        assertEquals("veh-strada-42", reconstructed.remoteId)
        assertEquals("CARRO", reconstructed.vehicleType)
        assertEquals("Fiat", reconstructed.brand)
        assertEquals("2023", reconstructed.manufacturingYear)
        assertEquals(55.0, reconstructed.tankCapacity, 0.001)
        assertFalse(reconstructed.hasTopBox)
        assertTrue(reconstructed.hasDeliveryBag)
        assertEquals("Pickup", reconstructed.carBodyType)
        assertEquals(SyncStatus.SYNCED, reconstructed.syncStatus)
    }

    @Test
    fun testVehiclePartBidirectionalMapping() {
        val userId = "test_user_uuid"
        val remoteVehicleId = "veh-uuid-123"
        val part = VehiclePart(
            id = 5L,
            name = "Pneu Traseiro",
            price = 220.0,
            lifespanKm = 12000.0,
            runKmSinceChange = 4500.0,
            remoteId = "part-uuid-77",
            syncStatus = SyncStatus.PENDING_INSERT,
            updatedAt = 1761000000000L,
            isDeleted = false
        )

        val dto = part.toDto(userId, remoteVehicleId)
        assertEquals("part-uuid-77", dto.id)
        assertEquals(userId, dto.userId)
        assertEquals(remoteVehicleId, dto.vehicleId)
        assertEquals("Pneu Traseiro", dto.name)
        assertEquals(220.0, dto.price, 0.001)
        assertEquals(12000.0, dto.lifespanKm, 0.001)
        assertEquals(4500.0, dto.runKmSinceChange, 0.001)

        val entity = dto.toEntity(existingId = 5L)
        assertEquals(5L, entity.id)
        assertEquals("part-uuid-77", entity.remoteId)
        assertEquals("Pneu Traseiro", entity.name)
        assertEquals(220.0, entity.price, 0.001)
        assertEquals(SyncStatus.SYNCED, entity.syncStatus)
        assertFalse(entity.isDeleted)
    }

    @Test
    fun testDailyRecordBidirectionalMapping() {
        val userId = "test_user_uuid"
        val record = DailyRecord(
            id = 12L,
            dateString = "2026-10-21",
            dateTimestamp = 1761086400000L,
            platform = "Uber Eats",
            grossEarnings = 280.50,
            deliveriesCount = 18,
            startOdometer = 30000.0,
            endOdometer = 30140.0,
            fuelPrice = 5.99,
            foodExpense = 30.0,
            fuelCost = 22.96,
            wearCost = 11.20,
            proportionalFixedCost = 7.50,
            netProfit = 208.84,
            startTime = "07:30",
            endTime = "18:00",
            pauseDuration = "01:30",
            remoteId = "rec-uuid-33",
            syncStatus = SyncStatus.PENDING_INSERT,
            updatedAt = 1761086400000L,
            isDeleted = false
        )

        val dto = record.toDto(userId)
        assertEquals("rec-uuid-33", dto.id)
        assertEquals(userId, dto.userId)
        assertEquals("2026-10-21", dto.dateString)
        assertEquals("Uber Eats", dto.platform)
        assertEquals(280.50, dto.grossEarnings, 0.001)
        assertEquals("07:30", dto.startTime)
        assertEquals("18:00", dto.endTime)
        assertEquals("01:30", dto.pauseDuration)

        val entity = dto.toEntity(existingId = 12L)
        assertEquals(12L, entity.id)
        assertEquals("rec-uuid-33", entity.remoteId)
        assertEquals("2026-10-21", entity.dateString)
        assertEquals("Uber Eats", entity.platform)
        assertEquals(280.50, entity.grossEarnings, 0.001)
        assertEquals("07:30", entity.startTime)
        assertEquals("18:00", entity.endTime)
        assertEquals("01:30", entity.pauseDuration)
        assertEquals(SyncStatus.SYNCED, entity.syncStatus)
    }

    @Test
    fun testUserProfileBidirectionalMapping() {
        val userId = "test_user_uuid"
        val profile = UserProfile(
            id = 1,
            name = "Maria Silva",
            phone = "11988887777",
            city = "Campinas",
            remoteId = "profile-uuid-1",
            syncStatus = SyncStatus.PENDING_INSERT,
            updatedAt = 1761000000000L,
            isDeleted = false
        )

        val dto = profile.toDto(userId)
        assertEquals("profile-uuid-1", dto.id)
        assertEquals(userId, dto.userId)
        assertEquals("Maria Silva", dto.name)
        assertEquals("11988887777", dto.phone)
        assertEquals("Campinas", dto.city)

        val entity = dto.toEntity(existingId = 1)
        assertEquals(1, entity.id)
        assertEquals("Maria Silva", entity.name)
        assertEquals("11988887777", entity.phone)
        assertEquals("Campinas", entity.city)
        assertEquals(SyncStatus.SYNCED, entity.syncStatus)
    }

    @Test
    fun testUserProfileWithAddressAndOnboardingBidirectionalMapping() {
        val userId = "user-address-99"
        val profile = UserProfile(
            id = 1,
            name = "Fernando Entregador",
            phone = "11977776666",
            city = "São Paulo",
            email = "fernando@girocusto.com",
            isWhatsapp = true,
            cep = "01310-100",
            street = "Avenida Paulista",
            addressNumber = "1578",
            neighborhood = "Bela Vista",
            stateUf = "SP",
            onboardingCompleted = true,
            remoteId = "prof-fernando-99",
            syncStatus = SyncStatus.PENDING_INSERT,
            updatedAt = 1761000000000L,
            isDeleted = false
        )

        val dto = profile.toDto(userId)
        assertEquals("prof-fernando-99", dto.id)
        assertEquals(userId, dto.userId)
        assertEquals("Fernando Entregador", dto.name)
        assertEquals("11977776666", dto.phone)
        assertEquals("São Paulo", dto.city)
        assertEquals("fernando@girocusto.com", dto.email)
        assertTrue(dto.isWhatsapp)
        assertEquals("01310-100", dto.cep)
        assertEquals("Avenida Paulista", dto.street)
        assertEquals("1578", dto.addressNumber)
        assertEquals("Bela Vista", dto.neighborhood)
        assertEquals("SP", dto.stateUf)
        assertTrue(dto.onboardingCompleted)

        val reconstructed = dto.toEntity(existingId = 1)
        assertEquals(1, reconstructed.id)
        assertEquals("prof-fernando-99", reconstructed.remoteId)
        assertEquals("fernando@girocusto.com", reconstructed.email)
        assertTrue(reconstructed.isWhatsapp)
        assertEquals("01310-100", reconstructed.cep)
        assertEquals("Avenida Paulista", reconstructed.street)
        assertEquals("1578", reconstructed.addressNumber)
        assertEquals("Bela Vista", reconstructed.neighborhood)
        assertEquals("SP", reconstructed.stateUf)
        assertTrue(reconstructed.onboardingCompleted)
        assertEquals(SyncStatus.SYNCED, reconstructed.syncStatus)
    }

    @Test
    fun testPlatformBidirectionalMapping() {
        val userId = "test_user_uuid"
        val platform = Platform(
            id = 2L,
            name = "Loggi",
            segment = "Entregas",
            paymentModel = "Produção",
            cycle = "Quinzenal",
            paymentDay = "SEX",
            fixedPayDelay = 5,
            cycleEntriesJson = "[{\"day\": 1}]",
            bankName = "Banco do Brasil",
            bankAgency = "1234",
            bankAccount = "56789-0",
            pixKeyType = "CNPJ",
            pixKey = "12.345.678/0001-90",
            active = true,
            remoteId = "plat-uuid-55",
            syncStatus = SyncStatus.PENDING_INSERT,
            updatedAt = 1761000000000L,
            isDeleted = false
        )

        val dto = platform.toDto(userId)
        assertEquals("plat-uuid-55", dto.id)
        assertEquals("Loggi", dto.name)
        assertEquals("CNPJ", dto.pixKeyType)
        assertEquals("12.345.678/0001-90", dto.pixKey)

        val entity = dto.toEntity(existingId = 2L)
        assertEquals(2L, entity.id)
        assertEquals("Loggi", entity.name)
        assertEquals("CNPJ", entity.pixKeyType)
        assertEquals(SyncStatus.SYNCED, entity.syncStatus)
    }

    @Test
    fun testFuelRefillBidirectionalMapping() {
        val userId = "test_user_uuid"
        val remoteVehicleId = "veh-uuid-99"
        val refill = FuelRefill(
            id = 7L,
            vehicleId = 1L,
            dateTimestamp = 1761000000000L,
            dateString = "2026-10-20",
            gasStation = "Ipiranga",
            fuelType = "Etanol",
            pricePerLiter = 4.19,
            liters = 12.5,
            discount = 2.0,
            totalPaid = 50.375,
            odometer = 15800.0,
            isFullTank = true,
            paymentMethod = "Cartão de Débito",
            isInstallment = false,
            installmentsCount = 1,
            remoteId = "refill-uuid-11",
            syncStatus = SyncStatus.PENDING_INSERT,
            updatedAt = 1761000000000L,
            isDeleted = false
        )

        val dto = refill.toDto(userId, remoteVehicleId)
        assertEquals("refill-uuid-11", dto.id)
        assertEquals(remoteVehicleId, dto.vehicleId)
        assertEquals("Ipiranga", dto.gasStation)
        assertEquals(50.375, dto.totalPaid, 0.001)

        val entity = dto.toEntity(existingId = 7L, localVehicleId = 1L)
        assertEquals(7L, entity.id)
        assertEquals(1L, entity.vehicleId)
        assertEquals("Ipiranga", entity.gasStation)
        assertEquals(SyncStatus.SYNCED, entity.syncStatus)
    }

    @Test
    fun testMaintenanceRecordBidirectionalMapping() {
        val userId = "test_user_uuid"
        val remoteVehicleId = "veh-uuid-99"
        val maint = MaintenanceRecord(
            id = 4L,
            vehicleId = 1L,
            dateTimestamp = 1761000000000L,
            dateString = "2026-10-20",
            description = "Troca de pastilhas de freio",
            location = "Oficina do Zé",
            value = 85.0,
            odometer = 16000.0,
            remoteId = "maint-uuid-22",
            syncStatus = SyncStatus.PENDING_INSERT,
            updatedAt = 1761000000000L,
            isDeleted = false
        )

        val dto = maint.toDto(userId, remoteVehicleId)
        assertEquals("maint-uuid-22", dto.id)
        assertEquals(remoteVehicleId, dto.vehicleId)
        assertEquals("Troca de pastilhas de freio", dto.description)
        assertEquals(85.0, dto.value, 0.001)

        val entity = dto.toEntity(existingId = 4L, localVehicleId = 1L)
        assertEquals(4L, entity.id)
        assertEquals(1L, entity.vehicleId)
        assertEquals("Troca de pastilhas de freio", entity.description)
        assertEquals(SyncStatus.SYNCED, entity.syncStatus)
    }

    @Test
    fun testTimestampAndIsoFormatting() {
        val timestamp = 1761000000000L
        val iso = SupabaseMappers.toIsoString(timestamp)
        assertTrue("ISO string deve conter formato UTC com T e Z", iso.contains("T") && iso.endsWith("Z"))

        val parsed = SupabaseMappers.parseIsoTimestamp(iso)
        assertEquals(timestamp, parsed)

        // Fallback para strings sem milissegundos
        val fallbackIso = "2026-10-20T22:40:00Z"
        val parsedFallback = SupabaseMappers.parseIsoTimestamp(fallbackIso)
        assertTrue(parsedFallback > 0)

        // Null ou vazio deve retornar timestamp atual (não lançar exceção)
        val now = System.currentTimeMillis()
        val parsedNull = SupabaseMappers.parseIsoTimestamp(null)
        assertTrue(parsedNull in (now - 2000)..(now + 2000))
    }

    // =========================================================================
    // 2.1 TESTES DE SERIALIZAÇÃO E DESSERIALIZAÇÃO JSON COM MOSHI (SUPABASE DTOS)
    // =========================================================================

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun testVehicleDtoSerializationAndDeserialization_MotoAndCarScenarios() {
        val vehicleAdapter = moshi.adapter(VehicleDto::class.java)

        // 1. Cenário Moto completa
        val motoDto = VehicleDto(
            id = "veh-moto-01",
            userId = "usr-100",
            model = "Honda CG 160 Fan",
            averageConsumption = 42.0,
            fuelType = "Gasolina",
            monthlyFixedCosts = 190.0,
            plannedWorkDays = 26,
            currentOdometer = 18500.0,
            active = true,
            vehicleType = "MOTO",
            brand = "Honda",
            manufacturingYear = "2023",
            tankCapacity = 16.1,
            hasTopBox = true,
            hasDeliveryBag = true,
            carBodyType = "",
            createdAt = "2026-10-21T00:00:00Z",
            updatedAt = "2026-10-21T10:00:00Z",
            deletedAt = null
        )

        val motoJson = vehicleAdapter.toJson(motoDto)
        assertTrue(motoJson.contains("\"vehicle_type\":\"MOTO\""))
        assertTrue(motoJson.contains("\"brand\":\"Honda\""))
        assertTrue(motoJson.contains("\"manufacturing_year\":\"2023\""))
        assertTrue(motoJson.contains("\"tank_capacity\":16.1"))
        assertTrue(motoJson.contains("\"has_top_box\":true"))
        assertTrue(motoJson.contains("\"has_delivery_bag\":true"))

        val deserializedMoto = vehicleAdapter.fromJson(motoJson)
        assertNotNull(deserializedMoto)
        assertEquals(motoDto, deserializedMoto)

        // 2. Cenário Carro completo
        val carDto = VehicleDto(
            id = "veh-car-02",
            userId = "usr-100",
            model = "Fiat Argo 1.0",
            averageConsumption = 14.2,
            fuelType = "Flex",
            monthlyFixedCosts = 520.0,
            plannedWorkDays = 24,
            currentOdometer = 42000.0,
            active = true,
            vehicleType = "CARRO",
            brand = "Fiat",
            manufacturingYear = "2022",
            tankCapacity = 47.0,
            hasTopBox = false,
            hasDeliveryBag = false,
            carBodyType = "Hatch",
            createdAt = "2026-10-21T00:00:00Z",
            updatedAt = "2026-10-21T10:00:00Z",
            deletedAt = null
        )

        val carJson = vehicleAdapter.toJson(carDto)
        assertTrue(carJson.contains("\"vehicle_type\":\"CARRO\""))
        assertTrue(carJson.contains("\"brand\":\"Fiat\""))
        assertTrue(carJson.contains("\"car_body_type\":\"Hatch\""))
        assertTrue(carJson.contains("\"tank_capacity\":47.0"))
        assertTrue(carJson.contains("\"has_top_box\":false"))
        assertTrue(carJson.contains("\"has_delivery_bag\":false"))

        val deserializedCar = vehicleAdapter.fromJson(carJson)
        assertNotNull(deserializedCar)
        assertEquals(carDto, deserializedCar)
    }

    @Test
    fun testUserProfileDtoSerializationAndDeserialization() {
        val profileAdapter = moshi.adapter(UserProfileDto::class.java)

        val profileDto = UserProfileDto(
            id = "prof-999",
            userId = "usr-100",
            name = "Fernando QA",
            phone = "11987654321",
            city = "São Paulo",
            email = "qa@girocusto.com",
            isWhatsapp = true,
            cep = "01310-200",
            street = "Avenida Paulista",
            addressNumber = "1000",
            neighborhood = "Bela Vista",
            stateUf = "SP",
            onboardingCompleted = true,
            platforms = "iFood, Uber Eats",
            createdAt = "2026-10-21T00:00:00Z",
            updatedAt = "2026-10-21T10:00:00Z",
            deletedAt = null
        )

        val json = profileAdapter.toJson(profileDto)
        assertTrue(json.contains("\"email\":\"qa@girocusto.com\""))
        assertTrue(json.contains("\"is_whatsapp\":true"))
        assertTrue(json.contains("\"cep\":\"01310-200\""))
        assertTrue(json.contains("\"street\":\"Avenida Paulista\""))
        assertTrue(json.contains("\"address_number\":\"1000\""))
        assertTrue(json.contains("\"neighborhood\":\"Bela Vista\""))
        assertTrue(json.contains("\"state_uf\":\"SP\""))
        assertTrue(json.contains("\"onboarding_completed\":true"))

        val deserialized = profileAdapter.fromJson(json)
        assertNotNull(deserialized)
        assertEquals(profileDto, deserialized)
    }

    @Test
    fun testVehicleDtoAndUserProfileDto_DeserializationDefaultsAndEdgeCases() {
        val vehicleAdapter = moshi.adapter(VehicleDto::class.java)
        val profileAdapter = moshi.adapter(UserProfileDto::class.java)

        // 1. JSON legado sem os novos campos técnicos de Vehicle (deve usar defaults da classe Kotlin)
        val legacyVehicleJson = """
            {
                "id": "legacy-veh-1",
                "user_id": "usr-1",
                "model": "Honda CG 125",
                "average_consumption": 35.0,
                "fuel_type": "Gasolina",
                "monthly_fixed_costs": 150.0,
                "planned_work_days": 20,
                "current_odometer": 80000.0,
                "active": true
            }
        """.trimIndent()

        val parsedLegacyVeh = vehicleAdapter.fromJson(legacyVehicleJson)
        assertNotNull(parsedLegacyVeh)
        assertEquals("MOTO", parsedLegacyVeh?.vehicleType)
        assertEquals("", parsedLegacyVeh?.brand)
        assertEquals("", parsedLegacyVeh?.manufacturingYear)
        assertEquals(16.0, parsedLegacyVeh?.tankCapacity ?: 0.0, 0.001)
        assertEquals(false, parsedLegacyVeh?.hasTopBox)
        assertEquals(false, parsedLegacyVeh?.hasDeliveryBag)
        assertEquals("", parsedLegacyVeh?.carBodyType)
        assertNull(parsedLegacyVeh?.createdAt)
        assertNull(parsedLegacyVeh?.updatedAt)
        assertNull(parsedLegacyVeh?.deletedAt)

        // 2. JSON legado sem os novos campos de endereço e onboarding de UserProfile
        val legacyProfileJson = """
            {
                "id": "legacy-prof-1",
                "user_id": "usr-1",
                "name": "Maria Silva",
                "phone": "11988887777",
                "city": "Santo André"
            }
        """.trimIndent()

        val parsedLegacyProf = profileAdapter.fromJson(legacyProfileJson)
        assertNotNull(parsedLegacyProf)
        assertEquals("", parsedLegacyProf?.email)
        assertEquals(true, parsedLegacyProf?.isWhatsapp)
        assertEquals("", parsedLegacyProf?.cep)
        assertEquals("", parsedLegacyProf?.street)
        assertEquals("", parsedLegacyProf?.addressNumber)
        assertEquals("", parsedLegacyProf?.neighborhood)
        assertEquals("", parsedLegacyProf?.stateUf)
        assertEquals(false, parsedLegacyProf?.onboardingCompleted)
        assertNull(parsedLegacyProf?.platforms)

        // 3. Casos de borda: booleans invertidos (is_whatsapp = false, onboarding_completed = true)
        val customProfileJson = """
            {
                "user_id": "usr-2",
                "name": "",
                "phone": "",
                "city": "",
                "email": "",
                "is_whatsapp": false,
                "cep": "",
                "street": "",
                "address_number": "",
                "neighborhood": "",
                "state_uf": "",
                "onboarding_completed": true
            }
        """.trimIndent()

        val parsedCustomProf = profileAdapter.fromJson(customProfileJson)
        assertNotNull(parsedCustomProf)
        assertEquals(false, parsedCustomProf?.isWhatsapp)
        assertEquals(true, parsedCustomProf?.onboardingCompleted)
        assertEquals("", parsedCustomProf?.name)
        assertEquals("", parsedCustomProf?.email)

        // 4. Casos de borda em Vehicle: active = false, strings vazias, tankCapacity = 0.0
        val edgeVehicleJson = """
            {
                "user_id": "usr-3",
                "model": "",
                "average_consumption": 0.0,
                "fuel_type": "",
                "monthly_fixed_costs": 0.0,
                "planned_work_days": 0,
                "current_odometer": 0.0,
                "active": false,
                "vehicle_type": "OUTRO",
                "brand": "",
                "manufacturing_year": "",
                "tank_capacity": 0.0,
                "has_top_box": false,
                "has_delivery_bag": false,
                "car_body_type": ""
            }
        """.trimIndent()

        val parsedEdgeVeh = vehicleAdapter.fromJson(edgeVehicleJson)
        assertNotNull(parsedEdgeVeh)
        assertFalse(parsedEdgeVeh?.active ?: true)
        assertEquals("OUTRO", parsedEdgeVeh?.vehicleType)
        assertEquals(0.0, parsedEdgeVeh?.tankCapacity ?: 1.0, 0.001)
        assertEquals("", parsedEdgeVeh?.brand)
    }

    // =========================================================================
    // 3. TESTES DE SINCRONIZAÇÃO E RESOLUÇÃO DE CONFLITOS (LWW)
    // =========================================================================

    @Test
    fun testPushLocalPendingRecordsToSupabase() = runTest {
        val fakeApi = FakeSupabaseRestApi()
        val syncManager = SupabaseSyncManager(inMemoryDb, fakeApi)
        val userId = "user_test_push"

        // Insere veículo pendente local
        val localVehicle = Vehicle(
            id = 1L,
            model = "Yamaha Crosser 150",
            averageConsumption = 35.0,
            fuelType = "Gasolina",
            monthlyFixedCosts = 100.0,
            plannedWorkDays = 20,
            currentOdometer = 10000.0,
            active = true,
            remoteId = "", // ainda sem remoteId
            syncStatus = SyncStatus.PENDING_INSERT,
            updatedAt = 1000L
        )
        inMemoryDb.vehicleDao().insertVehicle(localVehicle)

        // Insere diária pendente
        val localRecord = DailyRecord(
            id = 1L,
            dateString = "2026-10-21",
            dateTimestamp = 1761000000000L,
            platform = "iFood",
            grossEarnings = 250.0,
            deliveriesCount = 15,
            startOdometer = 10000.0,
            endOdometer = 10120.0,
            fuelPrice = 5.80,
            foodExpense = 20.0,
            fuelCost = 19.88,
            wearCost = 9.60,
            proportionalFixedCost = 5.0,
            netProfit = 195.52,
            remoteId = "",
            syncStatus = SyncStatus.PENDING_INSERT,
            updatedAt = 1000L
        )
        inMemoryDb.dailyRecordDao().insertRecord(localRecord)

        // Executa sync (Push + Pull)
        val result = syncManager.sync(userId)
        assertTrue("Sincronização deve ter sucesso", result is SyncResult.Success)

        // Verifica que o Push enviou para o Supabase
        assertEquals(1, fakeApi.upsertedVehicles.size)
        assertEquals("Yamaha Crosser 150", fakeApi.upsertedVehicles.first().model)
        assertEquals(1, fakeApi.upsertedDailyRecords.size)

        // Verifica que os registros locais foram marcados como SYNCED e receberam remoteId
        val updatedVehicle = inMemoryDb.vehicleDao().getActiveVehicle()
        assertNotNull(updatedVehicle)
        assertEquals(SyncStatus.SYNCED, updatedVehicle?.syncStatus)
        assertTrue("Veículo local deve receber remoteId gerado", updatedVehicle?.remoteId?.isNotBlank() == true)

        val updatedRecord = inMemoryDb.dailyRecordDao().getAllRecords().first()
        assertEquals(SyncStatus.SYNCED, updatedRecord.syncStatus)
        assertTrue("Diária local deve receber remoteId gerado", updatedRecord.remoteId.isNotBlank())
    }

    @Test
    fun testPullRemoteChanges_LWW_remoteNewerOverwritesPendingLocal() = runTest {
        val fakeApi = FakeSupabaseRestApi()
        val syncManager = SupabaseSyncManager(inMemoryDb, fakeApi)
        val userId = "user_lww_1"

        // Estado local: alterado localmente com timestamp mais antigo
        val localVehicle = Vehicle(
            id = 1L,
            model = "Modelo Antigo Local",
            averageConsumption = 30.0,
            fuelType = "Gasolina",
            monthlyFixedCosts = 100.0,
            plannedWorkDays = 20,
            currentOdometer = 10000.0,
            active = true,
            remoteId = "veh-uuid-conflict",
            syncStatus = SyncStatus.PENDING_UPDATE,
            updatedAt = 1000L
        )
        inMemoryDb.vehicleDao().insertVehicle(localVehicle)

        // Estado remoto no Supabase: timestamp MAIS RECENTE
        val remoteNewer = VehicleDto(
            id = "veh-uuid-conflict",
            userId = userId,
            model = "Modelo Mais Recente Remoto",
            averageConsumption = 38.0,
            fuelType = "Gasolina",
            monthlyFixedCosts = 120.0,
            plannedWorkDays = 22,
            currentOdometer = 10500.0,
            active = true,
            updatedAt = SupabaseMappers.toIsoString(5000L) // 5000L > 1000L
        )
        fakeApi.remoteVehicles.add(remoteNewer)

        // Executa sync
        syncManager.sync(userId)

        // LWW: Remoto venceu pois remoteUpdated (5000) >= localUpdated (1000)
        val vehicleInDb = inMemoryDb.vehicleDao().getVehicleByRemoteId("veh-uuid-conflict")
        assertNotNull(vehicleInDb)
        assertEquals("Modelo Mais Recente Remoto", vehicleInDb?.model)
        assertEquals(38.0, vehicleInDb?.averageConsumption ?: 0.0, 0.001)
        assertEquals(SyncStatus.SYNCED, vehicleInDb?.syncStatus)
    }

    @Test
    fun testPullRemoteChanges_LWW_localNewerPreservesPendingLocal() = runTest {
        val fakeApi = FakeSupabaseRestApi()
        val syncManager = SupabaseSyncManager(inMemoryDb, fakeApi)
        val userId = "user_lww_2"

        // Estado local: alterado localmente com timestamp MAIS RECENTE
        val localVehicle = Vehicle(
            id = 1L,
            model = "Modelo Local Mais Recente",
            averageConsumption = 30.0,
            fuelType = "Gasolina",
            monthlyFixedCosts = 100.0,
            plannedWorkDays = 20,
            currentOdometer = 10000.0,
            active = true,
            remoteId = "veh-uuid-preserve",
            syncStatus = SyncStatus.PENDING_UPDATE,
            updatedAt = 9000L // 9000L local > 2000L remoto
        )
        inMemoryDb.vehicleDao().insertVehicle(localVehicle)

        // Remoto tem versão desatualizada
        val remoteOlder = VehicleDto(
            id = "veh-uuid-preserve",
            userId = userId,
            model = "Modelo Remoto Desatualizado",
            averageConsumption = 25.0,
            fuelType = "Gasolina",
            monthlyFixedCosts = 80.0,
            plannedWorkDays = 15,
            currentOdometer = 8000.0,
            active = true,
            updatedAt = SupabaseMappers.toIsoString(2000L)
        )
        fakeApi.remoteVehicles.add(remoteOlder)

        syncManager.sync(userId)

        val vehicleInDb = inMemoryDb.vehicleDao().getVehicleByRemoteId("veh-uuid-preserve")
        assertNotNull(vehicleInDb)
        assertEquals("Modelo Local Mais Recente", vehicleInDb?.model)
    }

    @Test
    fun testPullRemoteChanges_syncedLocalAlwaysUpdatesFromRemote() = runTest {
        val fakeApi = FakeSupabaseRestApi()
        val syncManager = SupabaseSyncManager(inMemoryDb, fakeApi)
        val userId = "user_lww_3"

        // Local está sincronizado (não tem pendências)
        val localVehicle = Vehicle(
            id = 1L,
            model = "Modelo Local Sincronizado",
            averageConsumption = 30.0,
            fuelType = "Gasolina",
            monthlyFixedCosts = 100.0,
            plannedWorkDays = 20,
            currentOdometer = 10000.0,
            active = true,
            remoteId = "veh-synced-1",
            syncStatus = SyncStatus.SYNCED,
            updatedAt = 2000L
        )
        inMemoryDb.vehicleDao().insertVehicle(localVehicle)

        // Remoto tem atualização da nuvem
        val remoteVehicle = VehicleDto(
            id = "veh-synced-1",
            userId = userId,
            model = "Modelo Atualizado da Nuvem",
            averageConsumption = 34.0,
            fuelType = "Gasolina",
            monthlyFixedCosts = 110.0,
            plannedWorkDays = 22,
            currentOdometer = 11200.0,
            active = true,
            updatedAt = SupabaseMappers.toIsoString(3000L)
        )
        fakeApi.remoteVehicles.add(remoteVehicle)

        syncManager.sync(userId)

        val updated = inMemoryDb.vehicleDao().getVehicleByRemoteId("veh-synced-1")
        assertNotNull(updated)
        assertEquals("Modelo Atualizado da Nuvem", updated?.model)
        assertEquals(34.0, updated?.averageConsumption ?: 0.0, 0.001)
    }

    @Test
    fun testSoftDeletePropagation() = runTest {
        val fakeApi = FakeSupabaseRestApi()
        val syncManager = SupabaseSyncManager(inMemoryDb, fakeApi)
        val userId = "user_soft_delete"

        // 1. PULL de soft-delete remoto -> marca isDeleted = true localmente
        val localPart = VehiclePart(
            id = 1L,
            name = "Vela de Ignição",
            price = 35.0,
            lifespanKm = 10000.0,
            runKmSinceChange = 5000.0,
            remoteId = "part-deleted-remotely",
            syncStatus = SyncStatus.SYNCED,
            updatedAt = 2000L,
            isDeleted = false
        )
        inMemoryDb.vehiclePartDao().insertPart(localPart)

        val remoteDeletedPart = VehiclePartDto(
            id = "part-deleted-remotely",
            userId = userId,
            name = "Vela de Ignição",
            price = 35.0,
            lifespanKm = 10000.0,
            runKmSinceChange = 10000.0,
            updatedAt = SupabaseMappers.toIsoString(4000L),
            deletedAt = SupabaseMappers.toIsoString(4000L)
        )
        fakeApi.remoteParts.add(remoteDeletedPart)

        syncManager.sync(userId)

        val updatedLocalPart = inMemoryDb.vehiclePartDao().getPartByRemoteId("part-deleted-remotely")
        assertNotNull(updatedLocalPart)
        assertTrue("Registro remoto com deletedAt deve ser marcado como isDeleted=true no banco local", updatedLocalPart?.isDeleted == true)

        // 2. PUSH de soft-delete local -> envia deletedAt preenchido para o Supabase
        val localDeletedVehicle = Vehicle(
            id = 2L,
            model = "Moto Antiga Vendida",
            averageConsumption = 30.0,
            fuelType = "Gasolina",
            monthlyFixedCosts = 50.0,
            plannedWorkDays = 10,
            currentOdometer = 45000.0,
            active = false,
            remoteId = "veh-sold-uuid",
            syncStatus = SyncStatus.PENDING_DELETE,
            updatedAt = 5000L,
            isDeleted = true
        )
        inMemoryDb.vehicleDao().insertVehicle(localDeletedVehicle)

        syncManager.sync(userId)

        val pushedVehicle = fakeApi.upsertedVehicles.firstOrNull { it.id == "veh-sold-uuid" }
        assertNotNull(pushedVehicle)
        assertNotNull("Push de veículo com isDeleted=true deve preencher deletedAt no DTO Supabase", pushedVehicle?.deletedAt)

        val localAfterPush = inMemoryDb.vehicleDao().getVehicleByRemoteId("veh-sold-uuid")
        assertEquals(SyncStatus.SYNCED, localAfterPush?.syncStatus)
    }

    @Test
    fun testReconciliationOfLocalRecordsWithoutRemoteId() = runTest {
        val fakeApi = FakeSupabaseRestApi()
        val syncManager = SupabaseSyncManager(inMemoryDb, fakeApi)
        val userId = "user_reconcile"

        // Veículo ativo local sem remoteId prévio
        val localVehicle = Vehicle(
            id = 1L,
            model = "Honda Fan 125",
            averageConsumption = 40.0,
            fuelType = "Gasolina",
            monthlyFixedCosts = 90.0,
            plannedWorkDays = 20,
            currentOdometer = 12000.0,
            active = true,
            remoteId = "", // vazio
            syncStatus = SyncStatus.SYNCED,
            updatedAt = 1000L
        )
        inMemoryDb.vehicleDao().insertVehicle(localVehicle)

        // Nuvem já tem o registro desse veículo
        val remoteVehicle = VehicleDto(
            id = "remote-uuid-fan",
            userId = userId,
            model = "Honda Fan 125",
            averageConsumption = 40.0,
            fuelType = "Gasolina",
            monthlyFixedCosts = 90.0,
            plannedWorkDays = 20,
            currentOdometer = 12000.0,
            active = true,
            updatedAt = SupabaseMappers.toIsoString(2000L)
        )
        fakeApi.remoteVehicles.add(remoteVehicle)

        // Executa sync
        syncManager.sync(userId)

        // Verifica que o veículo local foi reconciliado e recebeu o remoteId
        val reconciled = inMemoryDb.vehicleDao().getActiveVehicle()
        assertEquals("remote-uuid-fan", reconciled?.remoteId)
    }

    @Test
    fun testSyncFailurePreservesPendingState() = runTest {
        val fakeApi = FakeSupabaseRestApi().apply { shouldFail = true }
        val syncManager = SupabaseSyncManager(inMemoryDb, fakeApi)
        val userId = "user_offline"

        val localVehicle = Vehicle(
            id = 1L,
            model = "Moto Offline",
            averageConsumption = 32.0,
            fuelType = "Gasolina",
            monthlyFixedCosts = 100.0,
            plannedWorkDays = 20,
            currentOdometer = 5000.0,
            active = true,
            remoteId = "",
            syncStatus = SyncStatus.PENDING_INSERT,
            updatedAt = 1000L
        )
        inMemoryDb.vehicleDao().insertVehicle(localVehicle)

        val result = syncManager.sync(userId)
        assertTrue("Sync deve retornar SyncResult.Error em falha de conexão", result is SyncResult.Error)

        // Estado local deve ser preservado como PENDING_INSERT
        val vehicleInDb = inMemoryDb.vehicleDao().getActiveVehicle()
        assertNotNull(vehicleInDb)
        assertEquals(SyncStatus.PENDING_INSERT, vehicleInDb?.syncStatus)
        assertEquals("", vehicleInDb?.remoteId)
    }

    @Test
    fun testVehicleAndPart_delete_sync_undo_sync_LWWProtection() = runTest {
        val repository = GiroCustoRepository(inMemoryDb)
        val fakeApi = FakeSupabaseRestApi()
        val syncManager = SupabaseSyncManager(inMemoryDb, fakeApi)
        val userId = "user_lww_undo_test"

        // Estado inicial: Veículo e Peça inseridos no banco Room e sincronizados com a FakeSupabaseRestApi
        // (isDeleted = false, syncStatus = SYNCED, updatedAt = 1000L).
        val initialVehicle = Vehicle(
            id = 1L,
            model = "Honda Fan 160",
            averageConsumption = 38.0,
            fuelType = "Gasolina",
            monthlyFixedCosts = 120.0,
            plannedWorkDays = 22,
            currentOdometer = 12000.0,
            active = true,
            remoteId = "veh-lww-1",
            syncStatus = SyncStatus.SYNCED,
            updatedAt = 1000L,
            isDeleted = false
        )
        inMemoryDb.vehicleDao().insertVehicle(initialVehicle)
        fakeApi.remoteVehicles.add(initialVehicle.toDto(userId))

        val initialPart = VehiclePart(
            id = 1L,
            vehicleId = 1L,
            name = "Kit Relação",
            price = 150.0,
            lifespanKm = 15000.0,
            runKmSinceChange = 3000.0,
            remoteId = "part-lww-1",
            syncStatus = SyncStatus.SYNCED,
            updatedAt = 1000L,
            isDeleted = false
        )
        inMemoryDb.vehiclePartDao().insertPart(initialPart)
        fakeApi.remoteParts.add(initialPart.toDto(userId, "veh-lww-1"))

        assertEquals(1, inMemoryDb.vehicleDao().getAllVehicles().size)
        assertEquals(1, inMemoryDb.vehiclePartDao().getAllParts().size)

        // Passo 1 (Excluir / Soft-Delete): Chamar repository.deleteVehicle e repository.deletePart.
        repository.deleteVehicle(initialVehicle)
        repository.deletePart(initialPart)

        val softDeletedVehicle = inMemoryDb.vehicleDao().getVehicleById(1L)
        val softDeletedPart = inMemoryDb.vehiclePartDao().getPartById(1L)

        // Asserções Passo 1: isDeleted = true, syncStatus = PENDING_DELETE, updatedAt atualizado.
        // DAOs getAllVehicles() e getAllParts() devem retornar vazios (filtrados).
        assertNotNull(softDeletedVehicle)
        assertTrue(softDeletedVehicle!!.isDeleted)
        assertEquals(SyncStatus.PENDING_DELETE, softDeletedVehicle.syncStatus)
        assertTrue(softDeletedVehicle.updatedAt > 1000L)

        assertNotNull(softDeletedPart)
        assertTrue(softDeletedPart!!.isDeleted)
        assertEquals(SyncStatus.PENDING_DELETE, softDeletedPart.syncStatus)
        assertTrue(softDeletedPart.updatedAt > 1000L)

        assertTrue("getAllVehicles deve filtrar registros com isDeleted = true", inMemoryDb.vehicleDao().getAllVehicles().isEmpty())
        assertTrue("getAllParts deve filtrar registros com isDeleted = true", inMemoryDb.vehiclePartDao().getAllParts().isEmpty())

        val deleteVehicleTimestamp = softDeletedVehicle.updatedAt
        val deletePartTimestamp = softDeletedPart.updatedAt

        // Passo 2 (Sincronizar 1): Executar syncManager.sync(userId).
        val syncResult1 = syncManager.sync(userId)
        assertTrue("Primeiro sync deve concluir com sucesso", syncResult1 is SyncResult.Success)

        // Asserções Passo 2: No fakeApi, registros recebidos com deletedAt != null.
        // No banco local, syncStatus = SYNCED, isDeleted = true.
        val remoteDeletedVehicle = fakeApi.remoteVehicles.firstOrNull { it.id == "veh-lww-1" }
        assertNotNull(remoteDeletedVehicle)
        assertNotNull("FakeApi deve conter deletedAt não nulo para veículo", remoteDeletedVehicle?.deletedAt)

        val remoteDeletedPart = fakeApi.remoteParts.firstOrNull { it.id == "part-lww-1" }
        assertNotNull(remoteDeletedPart)
        assertNotNull("FakeApi deve conter deletedAt não nulo para peça", remoteDeletedPart?.deletedAt)

        val localVehicleAfterSync1 = inMemoryDb.vehicleDao().getVehicleById(1L)
        assertNotNull(localVehicleAfterSync1)
        assertEquals(SyncStatus.SYNCED, localVehicleAfterSync1!!.syncStatus)
        assertTrue(localVehicleAfterSync1.isDeleted)

        val localPartAfterSync1 = inMemoryDb.vehiclePartDao().getPartById(1L)
        assertNotNull(localPartAfterSync1)
        assertEquals(SyncStatus.SYNCED, localPartAfterSync1!!.syncStatus)
        assertTrue(localPartAfterSync1.isDeleted)

        // Passo 3 (Desfazer / Undo): Chamar repository.restoreVehicle e repository.restorePart.
        // Pequena pausa para garantir que System.currentTimeMillis() avance estritamente além do deleteTimestamp
        Thread.sleep(15)

        repository.restoreVehicle(localVehicleAfterSync1)
        repository.restorePart(localPartAfterSync1)

        val restoredVehicle = inMemoryDb.vehicleDao().getVehicleById(1L)
        val restoredPart = inMemoryDb.vehiclePartDao().getPartById(1L)

        // Asserções Passo 3: isDeleted = false, syncStatus = PENDING_UPDATE, updatedAt deve ser estritamente maior que o timestamp do passo anterior.
        assertNotNull(restoredVehicle)
        assertFalse(restoredVehicle!!.isDeleted)
        assertEquals(SyncStatus.PENDING_UPDATE, restoredVehicle.syncStatus)
        assertTrue("updatedAt do veículo restaurado deve ser estritamente maior que o timestamp anterior", restoredVehicle.updatedAt > deleteVehicleTimestamp)

        assertNotNull(restoredPart)
        assertFalse(restoredPart!!.isDeleted)
        assertEquals(SyncStatus.PENDING_UPDATE, restoredPart.syncStatus)
        assertTrue("updatedAt da peça restaurada deve ser estritamente maior que o timestamp anterior", restoredPart.updatedAt > deletePartTimestamp)

        assertEquals("getAllVehicles deve retornar 1 veículo restaurado", 1, inMemoryDb.vehicleDao().getAllVehicles().size)
        assertEquals("getAllParts deve retornar 1 peça restaurada", 1, inMemoryDb.vehiclePartDao().getAllParts().size)

        val restoreVehicleTimestamp = restoredVehicle.updatedAt
        val restorePartTimestamp = restoredPart.updatedAt

        // Passo 4 (Sincronizar 2): Executar syncManager.sync(userId).
        val syncResult2 = syncManager.sync(userId)
        assertTrue("Segundo sync deve concluir com sucesso", syncResult2 is SyncResult.Success)

        // Asserções Passo 4: No fakeApi, registros atualizados com deletedAt == null e novo updatedAt.
        // No banco local, syncStatus = SYNCED e isDeleted = false. O pull remoto não re-deleta os itens devido à proteção LWW.
        val finalRemoteVehicle = fakeApi.remoteVehicles.firstOrNull { it.id == "veh-lww-1" }
        assertNotNull(finalRemoteVehicle)
        assertNull("deletedAt deve ser nulo no FakeApi após restore sync", finalRemoteVehicle?.deletedAt)
        assertEquals(restoreVehicleTimestamp, SupabaseMappers.parseIsoTimestamp(finalRemoteVehicle?.updatedAt))

        val finalRemotePart = fakeApi.remoteParts.firstOrNull { it.id == "part-lww-1" }
        assertNotNull(finalRemotePart)
        assertNull("deletedAt deve ser nulo no FakeApi após restore sync", finalRemotePart?.deletedAt)
        assertEquals(restorePartTimestamp, SupabaseMappers.parseIsoTimestamp(finalRemotePart?.updatedAt))

        val finalLocalVehicle = inMemoryDb.vehicleDao().getVehicleById(1L)
        assertNotNull(finalLocalVehicle)
        assertEquals(SyncStatus.SYNCED, finalLocalVehicle!!.syncStatus)
        assertFalse("isDeleted do veículo local deve permanecer false após sincronização com proteção LWW", finalLocalVehicle.isDeleted)

        val finalLocalPart = inMemoryDb.vehiclePartDao().getPartById(1L)
        assertNotNull(finalLocalPart)
        assertEquals(SyncStatus.SYNCED, finalLocalPart!!.syncStatus)
        assertFalse("isDeleted da peça local deve permanecer false após sincronização com proteção LWW", finalLocalPart.isDeleted)

        assertEquals(1, inMemoryDb.vehicleDao().getAllVehicles().size)
        assertEquals(1, inMemoryDb.vehiclePartDao().getAllParts().size)
    }
}

// =========================================================================
// FAKE SUPABASE REST API IMPLEMENTATION FOR UNIT TESTS
// =========================================================================

class FakeSupabaseRestApi : SupabaseRestApi {
    var shouldFail: Boolean = false

    val remoteVehicles = mutableListOf<VehicleDto>()
    val remoteParts = mutableListOf<VehiclePartDto>()
    val remoteDailyRecords = mutableListOf<DailyRecordDto>()
    val remoteUserProfiles = mutableListOf<UserProfileDto>()
    val remotePlatforms = mutableListOf<PlatformDto>()
    val remoteFuelRefills = mutableListOf<FuelRefillDto>()
    val remoteMaintenanceRecords = mutableListOf<MaintenanceRecordDto>()

    val upsertedVehicles = mutableListOf<VehicleDto>()
    val upsertedParts = mutableListOf<VehiclePartDto>()
    val upsertedDailyRecords = mutableListOf<DailyRecordDto>()
    val upsertedUserProfiles = mutableListOf<UserProfileDto>()
    val upsertedPlatforms = mutableListOf<PlatformDto>()
    val upsertedFuelRefills = mutableListOf<FuelRefillDto>()
    val upsertedMaintenanceRecords = mutableListOf<MaintenanceRecordDto>()

    override suspend fun getVehicles(select: String, updatedAtFilter: String?): List<VehicleDto> {
        if (shouldFail) throw IOException("Falha de rede simulada")
        return remoteVehicles.toList()
    }

    override suspend fun upsertVehicles(vehicles: List<VehicleDto>): List<VehicleDto> {
        if (shouldFail) throw IOException("Falha de rede simulada")
        upsertedVehicles.addAll(vehicles)
        for (v in vehicles) {
            val existing = remoteVehicles.firstOrNull { it.id == v.id }
            val existingTime = SupabaseMappers.parseIsoTimestamp(existing?.updatedAt)
            val incomingTime = SupabaseMappers.parseIsoTimestamp(v.updatedAt)
            if (existing == null || incomingTime >= existingTime) {
                remoteVehicles.removeAll { it.id == v.id }
                remoteVehicles.add(v)
            }
        }
        return vehicles
    }

    override suspend fun deleteVehicle(idFilter: String): Response<Unit> {
        if (shouldFail) throw IOException("Falha de rede simulada")
        remoteVehicles.removeAll { it.id == idFilter }
        return Response.success(Unit)
    }

    override suspend fun getVehicleParts(select: String, updatedAtFilter: String?): List<VehiclePartDto> {
        if (shouldFail) throw IOException("Falha de rede simulada")
        return remoteParts.toList()
    }

    override suspend fun upsertVehicleParts(parts: List<VehiclePartDto>): List<VehiclePartDto> {
        if (shouldFail) throw IOException("Falha de rede simulada")
        upsertedParts.addAll(parts)
        for (p in parts) {
            val existing = remoteParts.firstOrNull { it.id == p.id }
            val existingTime = SupabaseMappers.parseIsoTimestamp(existing?.updatedAt)
            val incomingTime = SupabaseMappers.parseIsoTimestamp(p.updatedAt)
            if (existing == null || incomingTime >= existingTime) {
                remoteParts.removeAll { it.id == p.id }
                remoteParts.add(p)
            }
        }
        return parts
    }

    override suspend fun deleteVehiclePart(idFilter: String): Response<Unit> {
        if (shouldFail) throw IOException("Falha de rede simulada")
        remoteParts.removeAll { it.id == idFilter }
        return Response.success(Unit)
    }

    override suspend fun getDailyRecords(select: String, updatedAtFilter: String?): List<DailyRecordDto> {
        if (shouldFail) throw IOException("Falha de rede simulada")
        return remoteDailyRecords.toList()
    }

    override suspend fun upsertDailyRecords(records: List<DailyRecordDto>): List<DailyRecordDto> {
        if (shouldFail) throw IOException("Falha de rede simulada")
        upsertedDailyRecords.addAll(records)
        for (r in records) {
            val existing = remoteDailyRecords.firstOrNull { it.id == r.id }
            val existingTime = SupabaseMappers.parseIsoTimestamp(existing?.updatedAt)
            val incomingTime = SupabaseMappers.parseIsoTimestamp(r.updatedAt)
            if (existing == null || incomingTime >= existingTime) {
                remoteDailyRecords.removeAll { it.id == r.id }
                remoteDailyRecords.add(r)
            }
        }
        return records
    }

    override suspend fun deleteDailyRecord(idFilter: String): Response<Unit> {
        if (shouldFail) throw IOException("Falha de rede simulada")
        remoteDailyRecords.removeAll { it.id == idFilter }
        return Response.success(Unit)
    }

    override suspend fun getUserProfiles(select: String, updatedAtFilter: String?): List<UserProfileDto> {
        if (shouldFail) throw IOException("Falha de rede simulada")
        return remoteUserProfiles.toList()
    }

    override suspend fun upsertUserProfiles(profiles: List<UserProfileDto>): List<UserProfileDto> {
        if (shouldFail) throw IOException("Falha de rede simulada")
        upsertedUserProfiles.addAll(profiles)
        for (p in profiles) {
            val existing = remoteUserProfiles.firstOrNull { it.id == p.id }
            val existingTime = SupabaseMappers.parseIsoTimestamp(existing?.updatedAt)
            val incomingTime = SupabaseMappers.parseIsoTimestamp(p.updatedAt)
            if (existing == null || incomingTime >= existingTime) {
                remoteUserProfiles.removeAll { it.id == p.id }
                remoteUserProfiles.add(p)
            }
        }
        return profiles
    }

    override suspend fun getPlatforms(select: String, updatedAtFilter: String?): List<PlatformDto> {
        if (shouldFail) throw IOException("Falha de rede simulada")
        return remotePlatforms.toList()
    }

    override suspend fun upsertPlatforms(platforms: List<PlatformDto>): List<PlatformDto> {
        if (shouldFail) throw IOException("Falha de rede simulada")
        upsertedPlatforms.addAll(platforms)
        for (p in platforms) {
            val existing = remotePlatforms.firstOrNull { it.id == p.id }
            val existingTime = SupabaseMappers.parseIsoTimestamp(existing?.updatedAt)
            val incomingTime = SupabaseMappers.parseIsoTimestamp(p.updatedAt)
            if (existing == null || incomingTime >= existingTime) {
                remotePlatforms.removeAll { it.id == p.id }
                remotePlatforms.add(p)
            }
        }
        return platforms
    }

    override suspend fun deletePlatform(idFilter: String): Response<Unit> {
        if (shouldFail) throw IOException("Falha de rede simulada")
        remotePlatforms.removeAll { it.id == idFilter }
        return Response.success(Unit)
    }

    override suspend fun getFuelRefills(select: String, updatedAtFilter: String?): List<FuelRefillDto> {
        if (shouldFail) throw IOException("Falha de rede simulada")
        return remoteFuelRefills.toList()
    }

    override suspend fun upsertFuelRefills(refills: List<FuelRefillDto>): List<FuelRefillDto> {
        if (shouldFail) throw IOException("Falha de rede simulada")
        upsertedFuelRefills.addAll(refills)
        for (r in refills) {
            val existing = remoteFuelRefills.firstOrNull { it.id == r.id }
            val existingTime = SupabaseMappers.parseIsoTimestamp(existing?.updatedAt)
            val incomingTime = SupabaseMappers.parseIsoTimestamp(r.updatedAt)
            if (existing == null || incomingTime >= existingTime) {
                remoteFuelRefills.removeAll { it.id == r.id }
                remoteFuelRefills.add(r)
            }
        }
        return refills
    }

    override suspend fun deleteFuelRefill(idFilter: String): Response<Unit> {
        if (shouldFail) throw IOException("Falha de rede simulada")
        remoteFuelRefills.removeAll { it.id == idFilter }
        return Response.success(Unit)
    }

    override suspend fun getMaintenanceRecords(select: String, updatedAtFilter: String?): List<MaintenanceRecordDto> {
        if (shouldFail) throw IOException("Falha de rede simulada")
        return remoteMaintenanceRecords.toList()
    }

    override suspend fun upsertMaintenanceRecords(records: List<MaintenanceRecordDto>): List<MaintenanceRecordDto> {
        if (shouldFail) throw IOException("Falha de rede simulada")
        upsertedMaintenanceRecords.addAll(records)
        for (m in records) {
            val existing = remoteMaintenanceRecords.firstOrNull { it.id == m.id }
            val existingTime = SupabaseMappers.parseIsoTimestamp(existing?.updatedAt)
            val incomingTime = SupabaseMappers.parseIsoTimestamp(m.updatedAt)
            if (existing == null || incomingTime >= existingTime) {
                remoteMaintenanceRecords.removeAll { it.id == m.id }
                remoteMaintenanceRecords.add(m)
            }
        }
        return records
    }

    override suspend fun deleteMaintenanceRecord(idFilter: String): Response<Unit> {
        if (shouldFail) throw IOException("Falha de rede simulada")
        remoteMaintenanceRecords.removeAll { it.id == idFilter }
        return Response.success(Unit)
    }
}
