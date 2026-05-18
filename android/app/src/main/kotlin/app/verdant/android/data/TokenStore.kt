package app.verdant.android.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // EncryptedSharedPreferences keeps the JWT at rest under AES-256-GCM with
    // keys held in AndroidKeystore. The "auth" prefs file name matches the
    // exclude entry in res/xml/data_extraction_rules.xml.
    @Volatile private var prefsInstance: SharedPreferences? = null
    private val initMutex = Mutex()
    private val tokenFlow = MutableStateFlow<String?>(null)
    @Volatile private var loaded = false

    val token: Flow<String?> = tokenFlow.asStateFlow()

    suspend fun getToken(): String? {
        ensureLoaded()
        return tokenFlow.value
    }

    suspend fun saveToken(token: String) = withContext(Dispatchers.IO) {
        val p = prefs()
        p.edit().putString(KEY_TOKEN, token).apply()
        tokenFlow.value = token
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        val p = prefs()
        p.edit().clear().apply()
        tokenFlow.value = null
    }

    private suspend fun ensureLoaded() {
        if (loaded) return
        initMutex.withLock {
            if (loaded) return
            withContext(Dispatchers.IO) {
                tokenFlow.value = prefs().getString(KEY_TOKEN, null)
                loaded = true
            }
        }
    }

    private fun prefs(): SharedPreferences {
        prefsInstance?.let { return it }
        synchronized(this) {
            prefsInstance?.let { return it }
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val p = EncryptedSharedPreferences.create(
                context,
                "auth",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
            prefsInstance = p
            return p
        }
    }

    companion object {
        private const val KEY_TOKEN = "jwt_token"
    }
}
