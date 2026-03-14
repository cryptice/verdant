package app.verdant.android.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream

fun Bitmap.toCompressedBase64(maxSize: Int = 800): String {
    val scale = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height, 1f)
    val scaled = if (scale < 1f) {
        Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), true)
    } else this
    val stream = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)
    return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
}

@Composable
fun PhotoPicker(
    imageBase64: String?,
    onImageCaptured: (base64: String, bitmap: Bitmap) -> Unit,
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
            bitmap = bmp
            onImageCaptured(bmp.toCompressedBase64(), bmp)
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
                    bitmap = bmp
                    onImageCaptured(bmp.toCompressedBase64(), bmp)
                }
            } catch (_: Exception) {}
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { launchCamera() }) {
                Icon(Icons.Default.CameraAlt, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Camera")
            }
            OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                Icon(Icons.Default.PhotoLibrary, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Gallery")
            }
        }

        bitmap?.let { bmp ->
            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Photo",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
