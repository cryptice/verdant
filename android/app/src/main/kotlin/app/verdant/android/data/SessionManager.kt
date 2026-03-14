package app.verdant.android.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val tokenStore: TokenStore,
) {
    private val _expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val expired = _expired.asSharedFlow()

    suspend fun onUnauthorized() {
        tokenStore.clear()
        _expired.tryEmit(Unit)
    }
}
