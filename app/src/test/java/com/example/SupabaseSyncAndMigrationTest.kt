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

        // Abre agora com Room versão 10 executando a MIGRATION_9_10
        val roomDb = Room.databaseBuilder(context, GiroCustoDatabase::class.java, dbName)
            .addMigrations(GiroCustoDatabase.MIGRATION_9_10)
            .allowMainThreadQueries()
            .build()

        val activeVehicle = roomDb.vehicleDao().getActiveVehicle()
        assertNotNull(activeVehicle)
        assertEquals("Yamaha Fazer 250", activeVehicle?.model)
        assertEquals("", activeVehicle?.remoteId)
        assertEquals(SyncStatus.SYNCED, activeVehicle?.syncStatus)
        assertEquals(0L, activeVehicle?.updatedAt)
        assertFalse(activeVehicle?.isDeleted ?: true)

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
