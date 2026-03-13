package app.verdant.android.data.api

import app.verdant.android.data.model.*
import retrofit2.http.*

interface VerdantApi {
    @POST("api/auth/google")
    suspend fun googleAuth(@Body request: GoogleAuthRequest): AuthResponse

    @GET("api/dashboard")
    suspend fun getDashboard(): DashboardResponse

    @GET("api/users/me")
    suspend fun getMe(): UserResponse

    @PUT("api/users/me")
    suspend fun updateMe(@Body request: UpdateUserRequest): UserResponse

    @DELETE("api/users/me")
    suspend fun deleteMe()

    @GET("api/gardens")
    suspend fun getGardens(): List<GardenResponse>

    @GET("api/gardens/{id}")
    suspend fun getGarden(@Path("id") id: Long): GardenResponse

    @POST("api/gardens")
    suspend fun createGarden(@Body request: CreateGardenRequest): GardenResponse

    @PUT("api/gardens/{id}")
    suspend fun updateGarden(@Path("id") id: Long, @Body request: UpdateGardenRequest): GardenResponse

    @DELETE("api/gardens/{id}")
    suspend fun deleteGarden(@Path("id") id: Long)

    @GET("api/gardens/{gardenId}/beds")
    suspend fun getBeds(@Path("gardenId") gardenId: Long): List<BedResponse>

    @GET("api/beds/{id}")
    suspend fun getBed(@Path("id") id: Long): BedResponse

    @POST("api/gardens/{gardenId}/beds")
    suspend fun createBed(@Path("gardenId") gardenId: Long, @Body request: CreateBedRequest): BedResponse

    @PUT("api/beds/{id}")
    suspend fun updateBed(@Path("id") id: Long, @Body request: UpdateBedRequest): BedResponse

    @DELETE("api/beds/{id}")
    suspend fun deleteBed(@Path("id") id: Long)

    @GET("api/beds/{bedId}/plants")
    suspend fun getPlants(@Path("bedId") bedId: Long): List<PlantResponse>

    @GET("api/plants/{id}")
    suspend fun getPlant(@Path("id") id: Long): PlantResponse

    @POST("api/beds/{bedId}/plants")
    suspend fun createPlant(@Path("bedId") bedId: Long, @Body request: CreatePlantRequest): PlantResponse

    @PUT("api/plants/{id}")
    suspend fun updatePlant(@Path("id") id: Long, @Body request: UpdatePlantRequest): PlantResponse

    @DELETE("api/plants/{id}")
    suspend fun deletePlant(@Path("id") id: Long)
}
