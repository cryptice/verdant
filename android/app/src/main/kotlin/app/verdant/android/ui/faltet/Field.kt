// android/app/src/main/kotlin/app/verdant/android/ui/faltet/Field.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk

@Composable
fun Field(
    label: String,
    value: String,
    onValueChange: ((String) -> Unit)? = null,
    accent: FaltetTone? = null,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    error: String? = null,
    required: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val valueColor = accent?.color() ?: FaltetInk
    val underlineColor = if (error != null) FaltetClay else FaltetInk
    Column(modifier) {
        Text(
            text = buildAnnotatedString {
                append(label.uppercase())
                if (required) {
                    withStyle(SpanStyle(color = FaltetClay)) { append(" *") }
                }
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            color = FaltetForest.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(4.dp))
        if (onValueChange != null) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontFamily = FaltetDisplay,
                    fontWeight = FontWeight.W300,
                    fontSize = 20.sp,
                    color = valueColor,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = underlineColor,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    .padding(vertical = 4.dp),
                decorationBox = { inner ->
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            fontFamily = FaltetDisplay,
                            fontSize = 20.sp,
                            color = FaltetForest.copy(alpha = 0.4f),
                        )
                    }
                    inner()
                },
            )
        } else {
            Text(
                text = value,
                fontFamily = FaltetDisplay,
                fontWeight = FontWeight.W300,
                fontSize = 20.sp,
                color = valueColor,
            )
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(underlineColor),
            )
        }
        if (error != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = error,
                fontSize = 12.sp,
                color = FaltetClay,
            )
        }
    }
}
