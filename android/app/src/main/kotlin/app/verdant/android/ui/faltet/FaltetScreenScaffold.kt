// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetScreenScaffold.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import app.verdant.android.ui.theme.FaltetCream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaltetScreenScaffold(
    mastheadLeft: String,
    mastheadCenter: String,
    mastheadRight: @Composable (() -> Unit)? = null,
    onMastheadLeftClick: (() -> Unit)? = null,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    fab: @Composable (() -> Unit)? = null,
    snackbarHost: @Composable () -> Unit = {},
    /**
     * Optional faint botanical watermark behind the screen content.
     * Anchored bottom-end at 4.5% alpha. Pass null (default) to disable —
     * appropriate for splash, hero, and form screens that already render
     * their own large imagery.
     */
    watermark: BotanicalPlate? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = FaltetCream,
        topBar = {
            Column {
                topBar()
                Masthead(
                    left = mastheadLeft,
                    center = mastheadCenter,
                    right = mastheadRight,
                    onLeftClick = onMastheadLeftClick,
                )
            }
        },
        bottomBar = bottomBar,
        floatingActionButton = { fab?.invoke() },
        snackbarHost = snackbarHost,
        content = { padding ->
            if (watermark != null) {
                Box(Modifier.fillMaxSize()) {
                    BotanicalIllustration(
                        plate = watermark,
                        size = 360.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(padding)
                            .alpha(0.045f),
                    )
                    content(padding)
                }
            } else {
                content(padding)
            }
        },
    )
}
