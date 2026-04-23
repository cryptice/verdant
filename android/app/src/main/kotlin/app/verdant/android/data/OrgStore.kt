package app.verdant.android.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.orgDataStore by preferencesDataStore(name = "org")

@Singleton
class OrgStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val orgIdKey = longPreferencesKey("active_org_id")

    val orgId: Flow<Long?> = context.orgDataStore.data.map { it[orgIdKey] }

    suspend fun getOrgId(): Long? = orgId.first()

    suspend fun saveOrgId(id: Long) {
        context.orgDataStore.edit { it[orgIdKey] = id }
    }

    suspend fun clear() {
        context.orgDataStore.edit { it.clear() }
    }
}
