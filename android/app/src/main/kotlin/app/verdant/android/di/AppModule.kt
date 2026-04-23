package app.verdant.android.di

import app.verdant.android.BuildConfig
import app.verdant.android.data.AppError
import app.verdant.android.data.OrgStore
import app.verdant.android.data.SessionManager
import app.verdant.android.data.TokenStore
import app.verdant.android.data.api.VerdantApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        tokenStore: TokenStore,
        orgStore: OrgStore,
        sessionManager: SessionManager,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // OkHttp interceptors are inherently synchronous, so runBlocking is the
            // standard approach for accessing coroutine-based token/org stores here.
            .addInterceptor { chain ->
                val token = runBlocking { tokenStore.getToken() }
                val orgId = runBlocking { orgStore.getOrgId() }
                val request = chain.request().newBuilder().apply {
                    if (token != null) addHeader("Authorization", "Bearer $token")
                    if (orgId != null) addHeader("X-Organization-Id", orgId.toString())
                }.build()

                val response = try {
                    chain.proceed(request)
                } catch (e: java.io.IOException) {
                    throw AppError.Network()
                }

                if (!response.isSuccessful) {
                    when (response.code) {
                        401 -> {
                            runBlocking { sessionManager.onUnauthorized() }
                            throw AppError.Unauthorized()
                        }
                        404 -> throw AppError.NotFound()
                        in 500..599 -> throw AppError.Server()
                        else -> throw AppError.Unknown("Request failed with status ${response.code}")
                    }
                }

                response
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideVerdantApi(retrofit: Retrofit): VerdantApi {
        return retrofit.create(VerdantApi::class.java)
    }
}
