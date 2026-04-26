// android/app/src/main/kotlin/app/verdant/android/ui/faltet/ConfirmDiscard.kt
package app.verdant.android.ui.faltet

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetForest

/**
 * Helper bundle returned from [rememberUnsavedChangesGuard]. Wraps a
 * dismiss/back action in a confirm-discard prompt when [isDirty] is true.
 *
 * Usage:
 * ```
 * val guard = rememberUnsavedChangesGuard(isDirty = name != initial)
 * AlertDialog(onDismissRequest = guard.requestDismiss { onDismiss() }, ...)
 * guard.RenderConfirmDialog()  // place once at the top of the composable
 * ```
 */
class UnsavedChangesGuard internal constructor(
    private val isDirty: () -> Boolean,
    private val pending: androidx.compose.runtime.MutableState<(() -> Unit)?>,
) {
    /** Wrap a dismiss action: if dirty, raise the confirm prompt; else run it. */
    fun requestDismiss(close: () -> Unit): () -> Unit = {
        if (isDirty()) pending.value = close else close()
    }

    /** Wrap the system back press: if dirty, raise the confirm prompt and run
     *  [onBack] when the user discards; otherwise let the back press through. */
    @Composable
    fun InterceptBack(onBack: () -> Unit) {
        BackHandler(enabled = isDirty()) {
            pending.value = onBack
        }
    }

    /** Render the confirm dialog. Place once near the top of the composable. */
    @Composable
    fun RenderConfirmDialog(message: String = "Ändringar går förlorade. Är du säker?") {
        val current = pending.value
        if (current != null) {
            AlertDialog(
                onDismissRequest = { pending.value = null },
                title = { Text("Kasta ändringar?") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = {
                        pending.value = null
                        current()
                    }) { Text("Kasta", color = FaltetAccent) }
                },
                dismissButton = {
                    TextButton(onClick = { pending.value = null }) {
                        Text("Avbryt", color = FaltetForest)
                    }
                },
            )
        }
    }
}

@Composable
fun rememberUnsavedChangesGuard(isDirty: Boolean): UnsavedChangesGuard {
    val pending = remember { mutableStateOf<(() -> Unit)?>(null) }
    val dirtyState = remember { mutableStateOf(isDirty) }
    dirtyState.value = isDirty
    return remember { UnsavedChangesGuard(isDirty = { dirtyState.value }, pending = pending) }
}
