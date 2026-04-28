// android/app/src/main/kotlin/app/verdant/android/ui/faltet/Masthead.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk

@Composable
fun Masthead(
    left: String,
    center: String,
    right: @Composable (() -> Unit)? = null,
    onLeftClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val drawerOpen = LocalDrawerOpen.current
    val accountOpen = LocalAccountOpen.current
    val dashboardOpen = LocalDashboardOpen.current
    val navigateBack = LocalNavigateBack.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(FaltetCream)
            // Push the masthead content below the status bar / camera notch.
            // Background extends all the way to the top edge so the cream
            // bleeds into the system bar instead of leaving an awkward strip.
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .drawBehind {
                drawLine(
                    color = FaltetInk,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(end = 22.dp, top = 6.dp, bottom = 6.dp),
    ) {
        when {
            // Detail / child screens get a back arrow at the start. The
            // back arrow takes priority over the burger so child screens
            // don't display two leading icons.
            navigateBack != null -> {
                IconButton(onClick = navigateBack, modifier = Modifier.padding(start = 6.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tillbaka", tint = FaltetForest)
                }
            }
            drawerOpen != null -> {
                IconButton(onClick = drawerOpen, modifier = Modifier.padding(start = 6.dp)) {
                    Icon(Icons.Default.Menu, contentDescription = "Meny", tint = FaltetForest)
                }
                if (dashboardOpen != null) {
                    IconButton(onClick = dashboardOpen) {
                        Icon(Icons.Default.Dashboard, contentDescription = "Översikt", tint = FaltetForest)
                    }
                }
            }
            else -> Spacer(Modifier.width(22.dp))
        }
        val leftMod = if (onLeftClick != null) {
            Modifier.weight(1f).clickable(onClick = onLeftClick)
        } else {
            Modifier.weight(1f)
        }
        Text(
            text = left,
            fontFamily = FaltetDisplay,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.W400,
            fontSize = 13.sp,
            color = FaltetForest,
            modifier = leftMod,
        )
        Text(
            text = center,
            fontFamily = FaltetDisplay,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.W500,
            fontSize = 16.sp,
            color = FaltetForest,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(2f),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                right?.invoke()
                if (accountOpen != null) {
                    IconButton(onClick = accountOpen) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Konto", tint = FaltetForest)
                    }
                }
            }
        }
    }
}
