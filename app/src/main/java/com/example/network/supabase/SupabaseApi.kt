package com.example.network.supabase

import retrofit2.Response
import retrofit2.http.*

interface SupabaseAuthApi {
    @POST("auth/v1/signup")
    suspend fun signUp(
        @Body request: AuthRequest
    ): Response<AuthResponse>

    @POST("auth/v1/token")
    suspend fun signInWithPassword(
        @Query("grant_type") grantType: String = "password",
        @Body request: AuthRequest
    ): Response<AuthResponse>

    @POST("auth/v1/logout")
    suspend fun logout(): Response<Unit>

    @GET("auth/v1/user")
    suspend fun getUser(): Response<SupabaseUser>
}

interface SupabaseRestApi {

    // VEHICLE
    @GET("rest/v1/vehicle")
    suspend fun getVehicles(
        @Query("select") select: String = "*",
        @Query("updated_at") updatedAtFilter: String? = null
    ): List<VehicleDto>

    @POST("rest/v1/vehicle")
    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    suspend fun upsertVehicles(
        @Body vehicles: List<VehicleDto>
    ): List<VehicleDto>

    @DELETE("rest/v1/vehicle")
    suspend fun deleteVehicle(
        @Query("id") idFilter: String
    ): Response<Unit>

    // VEHICLE PARTS
    @GET("rest/v1/vehicle_parts")
    suspend fun getVehicleParts(
        @Query("select") select: String = "*",
        @Query("updated_at") updatedAtFilter: String? = null
    ): List<VehiclePartDto>

    @POST("rest/v1/vehicle_parts")
    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    suspend fun upsertVehicleParts(
        @Body parts: List<VehiclePartDto>
    ): List<VehiclePartDto>

    @DELETE("rest/v1/vehicle_parts")
    suspend fun deleteVehiclePart(
        @Query("id") idFilter: String
    ): Response<Unit>

    // DAILY RECORDS
    @GET("rest/v1/daily_records")
    suspend fun getDailyRecords(
        @Query("select") select: String = "*",
        @Query("updated_at") updatedAtFilter: String? = null
    ): List<DailyRecordDto>

    @POST("rest/v1/daily_records")
    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    suspend fun upsertDailyRecords(
        @Body records: List<DailyRecordDto>
    ): List<DailyRecordDto>

    @DELETE("rest/v1/daily_records")
    suspend fun deleteDailyRecord(
        @Query("id") idFilter: String
    ): Response<Unit>

    // USER PROFILE
    @GET("rest/v1/user_profile")
    suspend fun getUserProfiles(
        @Query("select") select: String = "*",
        @Query("updated_at") updatedAtFilter: String? = null
    ): List<UserProfileDto>

    @POST("rest/v1/user_profile")
    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    suspend fun upsertUserProfiles(
        @Body profiles: List<UserProfileDto>
    ): List<UserProfileDto>

    // PLATFORMS
    @GET("rest/v1/platforms")
    suspend fun getPlatforms(
        @Query("select") select: String = "*",
        @Query("updated_at") updatedAtFilter: String? = null
    ): List<PlatformDto>

    @POST("rest/v1/platforms")
    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    suspend fun upsertPlatforms(
        @Body platforms: List<PlatformDto>
    ): List<PlatformDto>

    @DELETE("rest/v1/platforms")
    suspend fun deletePlatform(
        @Query("id") idFilter: String
    ): Response<Unit>

    // FUEL REFILLS
    @GET("rest/v1/fuel_refills")
    suspend fun getFuelRefills(
        @Query("select") select: String = "*",
        @Query("updated_at") updatedAtFilter: String? = null
    ): List<FuelRefillDto>

    @POST("rest/v1/fuel_refills")
    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    suspend fun upsertFuelRefills(
        @Body refills: List<FuelRefillDto>
    ): List<FuelRefillDto>

    @DELETE("rest/v1/fuel_refills")
    suspend fun deleteFuelRefill(
        @Query("id") idFilter: String
    ): Response<Unit>

    // MAINTENANCE RECORDS
    @GET("rest/v1/maintenance_records")
    suspend fun getMaintenanceRecords(
        @Query("select") select: String = "*",
        @Query("updated_at") updatedAtFilter: String? = null
    ): List<MaintenanceRecordDto>

    @POST("rest/v1/maintenance_records")
    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    suspend fun upsertMaintenanceRecords(
        @Body records: List<MaintenanceRecordDto>
    ): List<MaintenanceRecordDto>

    @DELETE("rest/v1/maintenance_records")
    suspend fun deleteMaintenanceRecord(
        @Query("id") idFilter: String
    ): Response<Unit>
}
