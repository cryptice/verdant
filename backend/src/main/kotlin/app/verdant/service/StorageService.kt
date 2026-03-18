package app.verdant.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageOptions
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.io.FileInputStream
import java.util.Base64
import java.util.Optional
import java.util.UUID
import java.util.logging.Logger

@ApplicationScoped
class StorageService(
    @ConfigProperty(name = "verdant.gcs.service-account-key") private val serviceAccountKeyPath: Optional<String>,
) {
    private val log = Logger.getLogger(StorageService::class.java.name)
    private val bucketName = "verdant-species"
    private val publicBase = "https://storage.googleapis.com/$bucketName"

    private val storage by lazy {
        val path = serviceAccountKeyPath.orElse(null)
        val credentials = if (!path.isNullOrBlank() && java.io.File(path).exists()) {
            GoogleCredentials.fromStream(FileInputStream(path))
        } else {
            GoogleCredentials.getApplicationDefault()
        }
        StorageOptions.newBuilder().setCredentials(credentials).build().service
    }

    fun uploadImage(base64: String, path: String): String {
        val bytes = Base64.getDecoder().decode(base64)
        val blobId = BlobId.of(bucketName, path)
        val blobInfo = BlobInfo.newBuilder(blobId).setContentType("image/jpeg").build()
        try {
            storage.create(blobInfo, bytes)
            log.info("Uploaded ${bytes.size} bytes to gs://$bucketName/$path")
        } catch (ex: Exception) {
            log.severe("Failed to upload to gs://$bucketName/$path: ${ex.message}")
            throw ex
        }
        return "$publicBase/$path"
    }

    fun uploadSpeciesFront(speciesId: Long, base64: String): String =
        uploadImage(base64, "species/$speciesId/front.jpg")

    fun uploadSpeciesBack(speciesId: Long, base64: String): String =
        uploadImage(base64, "species/$speciesId/back.jpg")

    fun uploadSpeciesPhoto(speciesId: Long, base64: String): String =
        uploadImage(base64, "species/$speciesId/photos/${UUID.randomUUID()}.jpg")

    fun uploadEventPhoto(eventId: Long, base64: String): String =
        uploadImage(base64, "events/$eventId.jpg")

    fun deleteByPath(url: String) {
        val path = url.removePrefix("$publicBase/")
        if (path != url) {
            storage.delete(BlobId.of(bucketName, path))
        }
    }
}
