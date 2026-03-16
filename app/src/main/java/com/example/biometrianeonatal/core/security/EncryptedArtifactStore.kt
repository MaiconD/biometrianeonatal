package com.example.biometrianeonatal.core.security

import android.content.Context
import androidx.core.content.FileProvider
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.example.biometrianeonatal.domain.model.OpenedArtifact
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class StoredArtifact(
    val originalFileName: String,
    val artifactUri: String,
    val sha256: String,
)

data class ArtifactInspectionResult(
    val exists: Boolean,
    val readable: Boolean,
    val checksumMatches: Boolean,
    val sizeBytes: Int,
)

interface EncryptedArtifactStore {
    suspend fun saveConsentArtifact(
        babyId: String,
        guardianName: String,
        originalFileName: String,
        content: ByteArray,
    ): StoredArtifact

    suspend fun saveCaptureArtifact(
        sessionId: String,
        fingerCode: String,
        originalFileName: String,
        content: ByteArray,
    ): StoredArtifact

    suspend fun verifyArtifact(artifactUri: String, expectedSha256: String): Boolean

    suspend fun inspectArtifact(artifactUri: String, expectedSha256: String): ArtifactInspectionResult

    suspend fun openArtifact(
        artifactUri: String,
        expectedSha256: String,
        originalFileName: String,
    ): OpenedArtifact

    suspend fun deleteArtifact(artifactUri: String)
}

class AndroidEncryptedArtifactStore @Inject constructor(
    private val context: Context,
) : EncryptedArtifactStore {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    override suspend fun saveConsentArtifact(
        babyId: String,
        guardianName: String,
        originalFileName: String,
        content: ByteArray,
    ): StoredArtifact {
        val safeName = buildString {
            append(sanitizeSegment(babyId))
            append("-")
            append(sanitizeSegment(guardianName).ifBlank { "responsavel" })
            append("-")
            append(System.currentTimeMillis())
            append(".bin")
        }
        return saveArtifact(
            relativePath = "consents/$safeName",
            originalFileName = originalFileName,
            content = content,
        )
    }

    override suspend fun saveCaptureArtifact(
        sessionId: String,
        fingerCode: String,
        originalFileName: String,
        content: ByteArray,
    ): StoredArtifact {
        val safeExtension = originalFileName.substringAfterLast('.', "bin")
            .lowercase()
            .ifBlank { "bin" }
        val safeName = buildString {
            append(sanitizeSegment(sessionId))
            append("-")
            append(sanitizeSegment(fingerCode))
            append("-")
            append(System.currentTimeMillis())
            append(".")
            append(safeExtension)
        }
        return saveArtifact(
            relativePath = "captures/$safeName",
            originalFileName = originalFileName,
            content = content,
        )
    }

    override suspend fun verifyArtifact(artifactUri: String, expectedSha256: String): Boolean {
        return inspectArtifact(artifactUri, expectedSha256).checksumMatches
    }

    override suspend fun inspectArtifact(
        artifactUri: String,
        expectedSha256: String,
    ): ArtifactInspectionResult {
        if (artifactUri.isBlank() || expectedSha256.isBlank()) {
            return ArtifactInspectionResult(
                exists = false,
                readable = false,
                checksumMatches = false,
                sizeBytes = 0,
            )
        }
        return withContext(Dispatchers.IO) {
            val file = resolveArtifactFile(artifactUri)
                ?: return@withContext ArtifactInspectionResult(
                    exists = false,
                    readable = false,
                    checksumMatches = false,
                    sizeBytes = 0,
                )
            if (!file.exists()) {
                return@withContext ArtifactInspectionResult(
                    exists = false,
                    readable = false,
                    checksumMatches = false,
                    sizeBytes = 0,
                )
            }
            runCatching {
                val decryptedBytes = newEncryptedFile(file).openFileInput().use { it.readBytes() }
                ArtifactInspectionResult(
                    exists = true,
                    readable = true,
                    checksumMatches = sha256(decryptedBytes) == expectedSha256.lowercase(),
                    sizeBytes = decryptedBytes.size,
                )
            }.getOrElse {
                ArtifactInspectionResult(
                    exists = true,
                    readable = false,
                    checksumMatches = false,
                    sizeBytes = 0,
                )
            }
        }
    }

    override suspend fun deleteArtifact(artifactUri: String) {
        if (artifactUri.isBlank()) return
        withContext(Dispatchers.IO) {
            resolveArtifactFile(artifactUri)?.takeIf(File::exists)?.delete()
        }
    }

    override suspend fun openArtifact(
        artifactUri: String,
        expectedSha256: String,
        originalFileName: String,
    ): OpenedArtifact {
        val inspection = inspectArtifact(artifactUri, expectedSha256)
        check(inspection.exists) { "Arquivo criptografado não encontrado." }
        check(inspection.readable) { "Arquivo criptografado ilegível." }
        check(inspection.checksumMatches) { "Falha de integridade do arquivo criptografado." }

        return withContext(Dispatchers.IO) {
            val encryptedFile = resolveArtifactFile(artifactUri)
                ?: error("Arquivo criptografado inválido.")
            val bytes = newEncryptedFile(encryptedFile).openFileInput().use { it.readBytes() }
            val tempFileName = buildTempFileName(originalFileName)
            val tempFile = File(tempArtifactsRootDir, tempFileName).apply {
                parentFile?.mkdirs()
                writeBytes(bytes)
            }
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile,
            )
            OpenedArtifact(
                fileName = originalFileName.ifBlank { tempFile.name },
                contentUri = contentUri.toString(),
                mimeType = guessMimeType(originalFileName),
            )
        }
    }

    private suspend fun saveArtifact(
        relativePath: String,
        originalFileName: String,
        content: ByteArray,
    ): StoredArtifact {
        return withContext(Dispatchers.IO) {
            val targetFile = File(artifactsRootDir, relativePath).apply {
                parentFile?.mkdirs()
                if (exists()) {
                    delete()
                }
            }
            val digest = sha256(content)
            newEncryptedFile(targetFile).openFileOutput().use { output ->
                output.write(content)
                output.flush()
            }
            check(verifyArtifact(buildArtifactUri(relativePath), digest)) {
                "Encrypted artifact integrity verification failed for $relativePath"
            }
            StoredArtifact(
                originalFileName = originalFileName,
                artifactUri = buildArtifactUri(relativePath),
                sha256 = digest,
            )
        }
    }

    private fun newEncryptedFile(targetFile: File): EncryptedFile {
        return EncryptedFile.Builder(
            context,
            targetFile,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
        ).build()
    }

    private fun resolveArtifactFile(artifactUri: String): File? {
        if (!artifactUri.startsWith(ARTIFACT_URI_PREFIX)) return null
        val relativePath = artifactUri.removePrefix(ARTIFACT_URI_PREFIX)
        return File(artifactsRootDir, relativePath)
    }

    private fun buildArtifactUri(relativePath: String): String {
        return ARTIFACT_URI_PREFIX + relativePath.replace(File.separatorChar, '/')
    }

    private fun buildTempFileName(originalFileName: String): String {
        val baseName = originalFileName.ifBlank { "consentimento" }
        val extension = baseName.substringAfterLast('.', "bin")
        val nameWithoutExtension = baseName.substringBeforeLast('.', baseName)
        return "${sanitizeSegment(nameWithoutExtension)}-${System.currentTimeMillis()}.$extension"
    }

    private fun guessMimeType(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "pdf" -> "application/pdf"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
    }

    private fun sha256(content: ByteArray): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(content)
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun sanitizeSegment(value: String): String {
        return value.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9._-]+"), "-")
            .trim('-')
            .take(80)
    }

    private companion object {
        const val ARTIFACT_URI_PREFIX = "secure://artifacts/"
    }

    private val artifactsRootDir: File
        get() = File(context.filesDir, "secure-artifacts")

    private val tempArtifactsRootDir: File
        get() = File(context.cacheDir, "opened-artifacts")
}

