// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetDropdown.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> FaltetDropdown(
    label: String,
    options: List<T>,
    selected: T?,
    onSelectedChange: (T) -> Unit,
    labelFor: (T) -> String,
    modifier: Modifier = Modifier,
    searchable: Boolean = true,
    placeholder: String = "Välj…",
    required: Boolean = false,
) {
    var sheetOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { sheetOpen = true; query = "" }
                .drawBehind {
                    drawLine(
                        color = FaltetInk,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                .padding(vertical = 6.dp),
        ) {
            Text(
                text = selected?.let { labelFor(it) } ?: placeholder,
                fontFamily = FaltetDisplay,
                fontStyle = if (selected == null) FontStyle.Normal else FontStyle.Italic,
                fontWeight = FontWeight.W300,
                fontSize = 20.sp,
                color = if (selected == null) FaltetForest.copy(alpha = 0.4f) else FaltetInk,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = FaltetClay,
                modifier = Modifier.size(18.dp),
            )
        }
    }

    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = sheetState,
            containerColor = FaltetCream,
        ) {
            val filtered = if (searchable && query.isNotBlank()) {
                options.filter { labelFor(it).contains(query, ignoreCase = true) }
            } else {
                options
            }
            Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                if (searchable) {
                    FaltetSearchField(value = query, onValueChange = { query = it }, placeholder = "SÖK")
                }
                LazyColumn {
                    items(filtered, key = { labelFor(it) }) { option ->
                        FaltetListRow(
                            title = labelFor(option),
                            onClick = {
                                onSelectedChange(option)
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    if (!sheetState.isVisible) sheetOpen = false
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetDropdownPreview_Placeholder() {
    FaltetDropdown(
        label = "Art",
        options = listOf("Cosmos", "Zinnia", "Dahlia"),
        selected = null,
        onSelectedChange = {},
        labelFor = { it },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetDropdownPreview_Selected() {
    FaltetDropdown(
        label = "Art",
        options = listOf("Cosmos", "Zinnia", "Dahlia"),
        selected = "Cosmos",
        onSelectedChange = {},
        labelFor = { it },
        required = true,
    )
}
