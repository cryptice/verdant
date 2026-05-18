package app.verdant.android.di

import app.verdant.android.BuildConfig
import app.verdant.android.data.AppError
import app.verdant.android.data.BackendStore
import app.verdant.android.data.OrgStore
import app.verdant.android.data.SessionManager
import app.verdant.android.data.TokenStore
import app.verdant.android.data.api.VerdantApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Invocation
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.WildcardType
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Wires OkHttp / Retrofit / [VerdantApi]. Authentication, org-scoping, and
 * the backend-toggle interceptor live here so the rest of the app talks to a
 * configured Retrofit instance and nothing else.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        tokenStore: TokenStore,
        orgStore: OrgStore,
        sessionManager: SessionManager,
        backendStore: BackendStore,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // Backend URL rewrite — when "use local backend" is set the request
            // host/scheme/port get swapped to LOCAL_API_BASE_URL on every call,
            // so toggling the preference takes effect without rebuilding Retrofit.
            .addInterceptor { chain ->
                val req = chain.request()
                val newReq = if (runBlocking { backendStore.getUseLocal() }) {
                    val local = BuildConfig.LOCAL_API_BASE_URL.toHttpUrl()
                    val rewritten = req.url.newBuilder()
                        .scheme(local.scheme)
                        .host(local.host)
                        .port(local.port)
                        .build()
                    req.newBuilder().url(rewritten).build()
                } else req
                chain.proceed(newReq)
            }
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
                    // 401 side-effect always — even for Response<*> callers, the
                    // session is dead and SessionManager must broadcast it.
                    if (response.code == 401) {
                        runBlocking { sessionManager.onUnauthorized() }
                    }
                    // Only map to AppError when the call declares a bare `T` return.
                    // For `Response<T>` returns, let the Response propagate so the
                    // caller can inspect status/body.
                    val returnsResponseWrapper = request.tag(Invocation::class.java)
                        ?.let { methodReturnsResponse(it.method()) } ?: false
                    if (!returnsResponseWrapper) {
                        when (response.code) {
                            401 -> throw AppError.Unauthorized()
                            404 -> throw AppError.NotFound()
                            in 500..599 -> throw AppError.Server()
                            else -> throw AppError.Unknown("Request failed with status ${response.code}")
                        }
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

    // Detects whether a Retrofit interface method's effective return type is
    // retrofit2.Response<*>. For suspend functions Retrofit hides the real
    // return type inside the trailing Continuation<? super T> parameter.
    private fun methodReturnsResponse(method: Method): Boolean {
        val params = method.genericParameterTypes
        if (params.isNotEmpty()) {
            val last = params.last()
            if (last is ParameterizedType && last.rawType == kotlin.coroutines.Continuation::class.java) {
                val arg = last.actualTypeArguments.firstOrNull() ?: return false
                val tType = if (arg is WildcardType) arg.lowerBounds.firstOrNull() ?: return false else arg
                val raw = when (tType) {
                    is ParameterizedType -> tType.rawType
                    is Class<*> -> tType
                    else -> return false
                }
                return raw == Response::class.java
            }
        }
        val rt = method.genericReturnType
        val raw = when (rt) {
            is ParameterizedType -> rt.rawType
            is Class<*> -> rt
            else -> return false
        }
        return raw == Response::class.java
    }
}
