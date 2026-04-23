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

// Zero-radius shape family — matches web's Fältet brutalist aesthetic.
// Components needing pill radius (Chip, FAB) apply CircleShape locally.
private val FaltetShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small      = RoundedCornerShape(0.dp),
    medium     = RoundedCornerShape(0.dp),
    large      = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
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
