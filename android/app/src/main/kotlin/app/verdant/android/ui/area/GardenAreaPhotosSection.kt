package app.verdant.android.ui.area

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.R
import app.verdant.android.data.model.BedPhotoReason
import app.verdant.android.data.model.GardenAreaPhotoResponse
import app.verdant.android.ui.activity.PhotoPicker
import app.verdant.android.ui.faltet.FaltetChipSelector
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.Field
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import coil.compose.AsyncImage

/**
 * Resolves an area photo's reason to its localized display string. Areas
 * reuse [BedPhotoReason] rather than a parallel enum — the backend stores
 * both bed and area photos against the same `BedPhotoReason` column (see
 * `GardenAreaPhoto.reason` server-side), so there is only one set of values
 * to label here.
 */
fun gardenAreaPhotoReasonLabelRes(reason: String): Int = when (reason) {
    BedPhotoReason.PROGRESS -> R.string.area_photo_reason_progress
    BedPhotoReason.ISSUE -> R.string.area_photo_reason_issue
    BedPhotoReason.HARVEST -> R.string.area_photo_reason_harvest
    BedPhotoReason.PLANTING -> R.string.area_photo_reason_planting
    BedPhotoReason.OTHER -> R.string.area_photo_reason_other
    else -> R.string.area_photo_reason_other
}

/** Mirrors [formattedEventDate] in `GardenAreaDetailScreen.kt` — locale-neutral, numeric. */
private fun formattedPhotoDate(dateIso: String): String = try {
    val d = java.time.LocalDate.parse(dateIso)
    "%02d.%02d.%04d".format(d.dayOfMonth, d.monthValue, d.year)
} catch (e: Exception) {
    dateIso
}

/** Render the "Photos" section of the area detail screen. */
fun LazyListScope.gardenAreaPhotosSection(
    photos: List<GardenAreaPhotoResponse>,
    onDelete: (Long) -> Unit,
) {
    if (photos.isEmpty()) {
        item { InlineEmptyAreaPhotos() }
    } else {
        items(photos, key = { "areaphoto_${it.id}" }) { photo ->
            GardenAreaPhotoRow(photo = photo, onDelete = { onDelete(photo.id) })
        }
    }
}

@Composable
private fun GardenAreaPhotoRow(
    photo: GardenAreaPhotoResponse,
    onDelete: () -> Unit,
) {
    val secondary = buildList {
        photo.description?.takeIf { it.isNotBlank() }?.let { add("“$it”") }
    }.joinToString(" · ").takeIf { it.isNotBlank() }

    FaltetListRow(
        title = stringResource(gardenAreaPhotoReasonLabelRes(photo.reason)),
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
                formattedPhotoDate(photo.capturedAt.take(10)),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = FaltetForest,
            )
        },
        actions = {
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.delete),
                    tint = FaltetClay,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
    )
}

@Composable
private fun InlineEmptyAreaPhotos() {
    Text(
        text = stringResource(R.string.area_photos_empty),
        fontFamily = FaltetDisplay,
        fontStyle = FontStyle.Italic,
        fontSize = 14.sp,
        color = FaltetForest,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
    )
}

@Composable
fun AddGardenAreaPhotoDialog(
    onDismiss: () -> Unit,
    onSave: (imageBase64: String, reason: String, description: String) -> Unit,
) {
    var imageBase64 by remember { mutableStateOf<String?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var reason by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }

    val reasonLabels = BedPhotoReason.values.associateWith { stringResource(gardenAreaPhotoReasonLabelRes(it)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.area_photo_add_title)) },
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
                    label = stringResource(R.string.area_photo_reason_label),
                    options = BedPhotoReason.values,
                    selected = reason,
                    onSelectedChange = { reason = it },
                    labelFor = { reasonLabels[it] ?: it },
                    required = true,
                )
                Field(
                    label = stringResource(R.string.area_field_description),
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
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
