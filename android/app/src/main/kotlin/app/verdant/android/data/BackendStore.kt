package app.verdant.android.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.backendDataStore by preferencesDataStore(name = "backend")

/**
 * Persists the "use local development backend" toggle. The toggle is hidden
 * from the UI for everyone except the developer account, but the storage is
 * generic so the OkHttp interceptor can read it without any role check.
 */
@Singleton
class BackendStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val useLocalKey = booleanPreferencesKey("use_local_backend")

    val useLocal: Flow<Boolean> = context.backendDataStore.data.map { it[useLocalKey] ?: false }

    suspend fun getUseLocal(): Boolean = useLocal.first()

    suspend fun setUseLocal(value: Boolean) {
        context.backendDataStore.edit { it[useLocalKey] = value }
    }
}
