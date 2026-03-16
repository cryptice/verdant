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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import app.verdant.android.R
import app.verdant.android.data.model.CropBox
import coil.compose.AsyncImage
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
    imageUrl: String?,
    onImageCaptured: (base64: String, bitmap: Bitmap) -> Unit,
    modifier: Modifier = Modifier,
    maxImageHeight: Int = 300,
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showFullScreen by remember { mutableStateOf(false) }

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

    val hasImage = bitmap != null || imageUrl != null

    // Full screen photo dialog
    if (showFullScreen && hasImage) {
        Dialog(
            onDismissRequest = { showFullScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showFullScreen = false }
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = stringResource(R.string.photo),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = stringResource(R.string.photo),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                IconButton(
                    onClick = { showFullScreen = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Close, stringResource(R.string.back))
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        if (hasImage) {
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().clickable { showFullScreen = true }
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = stringResource(R.string.photo),
                        modifier = Modifier.fillMaxWidth().heightIn(max = maxImageHeight.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = stringResource(R.string.photo),
                        modifier = Modifier.fillMaxWidth().heightIn(max = maxImageHeight.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        OutlinedButton(
            onClick = { launchCamera() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CameraAlt, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.camera))
        }
        OutlinedButton(
            onClick = { galleryLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PhotoLibrary, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.gallery))
        }
    }
}
