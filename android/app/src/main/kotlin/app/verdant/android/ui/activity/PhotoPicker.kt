package app.verdant.android.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.verdant.android.R
import app.verdant.android.data.model.CropBox
import java.io.ByteArrayOutputStream

/** Crop a bitmap using normalized coordinates (0.0-1.0). */
fun Bitmap.cropToBox(box: CropBox): Bitmap {
    val x = (box.x * width).toInt().coerceIn(0, width - 1)
    val y = (box.y * height).toInt().coerceIn(0, height - 1)
    val w = (box.width * width).toInt().coerceIn(1, width - x)
    val h = (box.height * height).toInt().coerceIn(1, height - y)
    return Bitmap.createBitmap(this, x, y, w, h)
}

/** Rotate landscape bitmaps to portrait orientation. */
fun Bitmap.ensurePortrait(): Bitmap {
    if (width <= height) return this
    val matrix = Matrix().apply { postRotate(90f) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun Bitmap.toCompressedBase64(maxSize: Int = 800): String {
    val portrait = ensurePortrait()
    val scale = minOf(maxSize.toFloat() / portrait.width, maxSize.toFloat() / portrait.height, 1f)
    val scaled = if (scale < 1f) {
        Bitmap.createScaledBitmap(portrait, (portrait.width * scale).toInt(), (portrait.height * scale).toInt(), true)
    } else portrait
    val stream = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)
    return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
}

@Composable
fun PhotoPicker(
    imageBase64: String?,
    onImageCaptured: (base64: String, bitmap: Bitmap) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Decode existing base64 on first load
    LaunchedEffect(imageBase64) {
        if (imageBase64 != null && bitmap == null) {
            try {
                val bytes = Base64.decode(imageBase64, Base64.NO_WRAP)
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Exception) {}
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) {
            val portrait = bmp.ensurePortrait()
            bitmap = portrait
            onImageCaptured(portrait.toCompressedBase64(), portrait)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraLauncher.launch(null)
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bmp != null) {
                    val portrait = bmp.ensurePortrait()
                    bitmap = portrait
                    onImageCaptured(portrait.toCompressedBase64(), portrait)
                }
            } catch (_: Exception) {}
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { launchCamera() }) {
                Icon(Icons.Default.CameraAlt, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.camera))
            }
            OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                Icon(Icons.Default.PhotoLibrary, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.gallery))
            }
        }

        bitmap?.let { bmp ->
            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = stringResource(R.string.photo),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
