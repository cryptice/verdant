// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetAvatar.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine40
import coil.compose.AsyncImage

@Composable
fun FaltetAvatar(
    url: String?,
    displayName: String?,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(FaltetCream)
            .border(1.dp, FaltetInkLine40, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val initials = displayName?.trim()?.takeIf { it.isNotBlank() }
                ?.substring(0, 1)
                ?.uppercase()
                ?: "?"
            Text(
                text = initials,
                fontFamily = FaltetDisplay,
                fontStyle = FontStyle.Italic,
                fontSize = (size.value * 0.4).sp,
                color = FaltetInk,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetAvatarPreview_Initials() {
    FaltetAvatar(url = null, displayName = "Erik Lindblad")
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetAvatarPreview_Unknown() {
    FaltetAvatar(url = null, displayName = null)
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetAvatarPreview_Large() {
    FaltetAvatar(url = null, displayName = "Astrid", size = 88.dp)
}
