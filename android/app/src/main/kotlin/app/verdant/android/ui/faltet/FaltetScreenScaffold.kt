// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetScreenScaffold.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import app.verdant.android.ui.theme.FaltetCream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaltetScreenScaffold(
    mastheadLeft: String,
    mastheadCenter: String,
    mastheadRight: @Composable (() -> Unit)? = null,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    fab: @Composable (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = FaltetCream,
        topBar = {
            Column {
                topBar()
                Masthead(left = mastheadLeft, center = mastheadCenter, right = mastheadRight)
            }
        },
        bottomBar = bottomBar,
        floatingActionButton = { fab?.invoke() },
        content = content,
    )
}
