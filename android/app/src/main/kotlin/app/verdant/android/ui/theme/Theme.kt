package app.verdant.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val FaltetColorScheme = lightColorScheme(
    primary            = FaltetInk,
    onPrimary          = FaltetCream,
    primaryContainer   = FaltetPaper,
    onPrimaryContainer = FaltetInk,
    secondary          = FaltetAccent,
    onSecondary        = FaltetCream,
    tertiary           = FaltetSage,
    onTertiary         = FaltetCream,
    background         = FaltetCream,
    onBackground       = FaltetInk,
    surface            = FaltetPaper,
    onSurface          = FaltetInk,
    surfaceVariant     = FaltetCream,
    onSurfaceVariant   = FaltetForest,
    outline            = FaltetInk,
    outlineVariant     = FaltetInkLine20,
    error              = FaltetClay,
    onError            = FaltetCream,
)

// Soft, journal-page corners. Earlier we used 0dp everywhere for a
// brutalist edge; bumping to small radii makes cards and dialogs feel
// like pressed paper instead of cut card-stock. Components needing pill
// radius (Chip, FAB) still apply CircleShape locally.
private val FaltetShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(8.dp),
    medium     = RoundedCornerShape(12.dp),
    large      = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun verdantTopAppBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor             = FaltetCream,
    titleContentColor          = FaltetInk,
    navigationIconContentColor = FaltetInk,
    actionIconContentColor     = FaltetInk,
)

@Composable
fun VerdantTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FaltetColorScheme,
        typography  = FaltetTypography,
        shapes      = FaltetShapes,
        content     = content,
    )
}
