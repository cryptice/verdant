package app.verdant.android.ui.bed

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.data.model.BedPhotoReason
import app.verdant.android.data.model.BedPhotoResponse
import app.verdant.android.ui.activity.PhotoPicker
import app.verdant.android.ui.faltet.FaltetChipSelector
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.Field
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import coil.compose.AsyncImage

internal fun bedPhotoReasonLabelSv(reason: String): String = when (reason) {
    BedPhotoReason.PROGRESS -> "Tillväxt"
    BedPhotoReason.ISSUE -> "Problem"
    BedPhotoReason.HARVEST -> "Skörd"
    BedPhotoReason.PLANTING -> "Plantering"
    BedPhotoReason.OTHER -> "Övrigt"
    else -> reason.lowercase().replaceFirstChar { it.uppercase() }
}

/** Render the "Bilder" section of the bed detail screen. */
fun LazyListScope.bedPhotosSection(
    photos: List<BedPhotoResponse>,
    onDelete: (Long) -> Unit,
) {
    if (photos.isEmpty()) {
        item { InlineEmptyPhotos("Inga bilder ännu.") }
    } else {
        items(photos, key = { "bedphoto_${it.id}" }) { photo ->
            BedPhotoRow(photo = photo, onDelete = { onDelete(photo.id) })
        }
    }
}

@Composable
private fun BedPhotoRow(
    photo: BedPhotoResponse,
    onDelete: () -> Unit,
) {
    val secondary = buildList {
        photo.description?.takeIf { it.isNotBlank() }?.let { add("“$it”") }
    }.joinToString(" · ").takeIf { it.isNotBlank() }

    FaltetListRow(
        title = bedPhotoReasonLabelSv(photo.reason),
        meta = secondary,
        metaMaxLines = 3,
        leading = {
            AsyncImage(
                model = photo.photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(6.dp)),
            )
        },
        stat = {
            Text(
                formattedDate(photo.capturedAt.take(10)).uppercase(),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = FaltetForest,
            )
        },
        actions = {
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.DeleteOutline, "Ta bort", tint = FaltetClay, modifier = Modifier.size(18.dp))
            }
        },
    )
}

@Composable
private fun InlineEmptyPhotos(text: String) {
    Text(
        text = text,
        fontFamily = FaltetDisplay,
        fontStyle = FontStyle.Italic,
        fontSize = 14.sp,
        color = FaltetForest,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
    )
}

@Composable
fun AddBedPhotoDialog(
    onDismiss: () -> Unit,
    onSave: (imageBase64: String, reason: String, description: String) -> Unit,
) {
    var imageBase64 by remember { mutableStateOf<String?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var reason by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ny bild") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PhotoPicker(
                    imageUrl = null,
                    onImageCaptured = { b64, bmp ->
                        imageBase64 = b64
                        bitmap = bmp
                    },
                )
                Spacer(Modifier.height(4.dp))
                FaltetChipSelector(
                    label = "Anledning",
                    options = BedPhotoReason.values,
                    selected = reason,
                    onSelectedChange = { reason = it },
                    labelFor = { bedPhotoReasonLabelSv(it) },
                    required = true,
                )
                Field(
                    label = "Beskrivning (valfri)",
                    value = description,
                    onValueChange = { description = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = imageBase64 != null && reason != null,
                onClick = {
                    val b64 = imageBase64 ?: return@TextButton
                    val r = reason ?: return@TextButton
                    onSave(b64, r, description)
                },
            ) { Text("Spara") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Avbryt") }
        },
    )
}
