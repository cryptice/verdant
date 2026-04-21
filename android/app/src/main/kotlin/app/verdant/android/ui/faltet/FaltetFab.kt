// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetFab.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetInk

@Composable
fun FaltetFab(
    onClick: () -> Unit,
    contentDescription: String?,
    icon: ImageVector = Icons.Default.Add,
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = FaltetInk,
        contentColor = FaltetClay,
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
        ),
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}
