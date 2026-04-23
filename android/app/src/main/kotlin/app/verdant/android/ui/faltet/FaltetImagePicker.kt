// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetImagePicker.kt
package app.verdant.android.ui.faltet

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine40

@Composable
fun FaltetImagePicker(
    label: String,
    value: Bitmap?,
    onValueChange: (Bitmap?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
    ) { bitmap: Bitmap? ->
        if (bitmap != null) onValueChange(bitmap)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri).use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()?.let(onValueChange)
        }
    }

    Column(modifier) {
        Text(
            text = label.uppercase(),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            color = FaltetForest.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(8.dp))
        if (value == null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    onClick = { cameraLauncher.launch(null) },
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, FaltetInkLine40),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FaltetAccent),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.PhotoCamera, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("KAMERA", fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 1.4.sp)
                }
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, FaltetInkLine40),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FaltetAccent),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Image, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("GALLERI", fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 1.4.sp)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .border(1.dp, FaltetInk),
            ) {
                Image(
                    bitmap = value.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                IconButton(
                    onClick = { onValueChange(null) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(32.dp)
                        .background(FaltetCream.copy(alpha = 0.8f)),
                ) {
                    Icon(Icons.Default.Close, "Ta bort bild", tint = FaltetAccent, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetImagePickerPreview_Empty() {
    FaltetImagePicker(label = "Foto", value = null, onValueChange = {})
}
