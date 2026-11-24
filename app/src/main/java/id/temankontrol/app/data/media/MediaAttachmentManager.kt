package id.temankontrol.app.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import id.temankontrol.app.core.common.AppLogger
import id.temankontrol.app.data.local.dao.AttachmentDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class ProcessedMediaResult(
    val fileName: String,
    val sizeBytes: Long,
    val uriString: String
)

@Singleton
class MediaAttachmentManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val MAX_IMAGE_DIMENSION_PX = 1600
        const val JPEG_QUALITY = 80
        const val MIN_FREE_SPACE_BYTES = 50L * 1024L * 1024L
    }

    fun attachmentsRootDir(): File = File(context.filesDir, "attachments").apply { mkdirs() }

    fun hasEnoughStorage(): Boolean = context.filesDir.usableSpace >= MIN_FREE_SPACE_BYTES

    fun createCameraTempUri(): Pair<Uri, File> {
        val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
        val tempFile = File(cameraDir, "cam_${UUID.randomUUID().toString().take(8)}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        return uri to tempFile
    }

    suspend fun saveCompressedPhoto(
        consultationId: String,
        sourceUri: Uri
    ): Result<ProcessedMediaResult> = withContext(Dispatchers.IO) {
        runCatching {
            if (!hasEnoughStorage()) {
                error("Ruang penyimpanan HP hampir penuh (< 50 MB).")
            }
            val destDir = File(attachmentsRootDir(), consultationId).apply { mkdirs() }
            val fileName = "foto_${UUID.randomUUID().toString().take(8)}.jpg"
            val destFile = File(destDir, fileName)

            val source = ImageDecoder.createSource(context.contentResolver, sourceUri)
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val maxSide = maxOf(info.size.width, info.size.height).coerceAtLeast(1)
                if (maxSide > MAX_IMAGE_DIMENSION_PX) {
                    val scale = MAX_IMAGE_DIMENSION_PX.toFloat() / maxSide.toFloat()
                    decoder.setTargetSize(
                        (info.size.width * scale).toInt().coerceAtLeast(1),
                        (info.size.height * scale).toInt().coerceAtLeast(1)
                    )
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }

            try {
                FileOutputStream(destFile).use { fos ->
                    // Re-encoding strips EXIF location/device metadata cleanly (FR-048)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos)
                    fos.flush()
                }
            } finally {
                bitmap.recycle()
            }

            ProcessedMediaResult(
                fileName = fileName,
                sizeBytes = destFile.length(),
                uriString = Uri.fromFile(destFile).toString()
            )
        }
    }

    fun prepareVoiceOutputFile(consultationId: String): Pair<String, File> {
        val destDir = File(attachmentsRootDir(), consultationId).apply { mkdirs() }
        val fileName = "vn_${UUID.randomUUID().toString().take(8)}.m4a"
        return fileName to File(destDir, fileName)
    }

    fun deleteFile(consultationId: String, fileName: String) {
        runCatching {
            val file = File(File(attachmentsRootDir(), consultationId), fileName)
            if (file.exists()) file.delete()
        }
    }

    fun deleteConsultationFolder(consultationId: String) {
        runCatching {
            val dir = File(attachmentsRootDir(), consultationId)
            if (dir.exists()) dir.deleteRecursively()
        }
    }

    suspend fun cleanupOrphanFiles(attachmentDao: AttachmentDao) = withContext(Dispatchers.IO) {
        runCatching {
            val root = attachmentsRootDir()
            if (!root.exists()) return@withContext
            val referenced = attachmentDao.getAll()
                .map { "${it.consultationId}/${it.fileName}" }
                .toSet()

            root.listFiles()?.forEach { consultDir ->
                if (consultDir.isDirectory) {
                    consultDir.listFiles()?.forEach { file ->
                        val rel = "${consultDir.name}/${file.name}"
                        if (rel !in referenced) {
                            file.delete()
                        }
                    }
                    if (consultDir.listFiles().isNullOrEmpty()) {
                        consultDir.delete()
                    }
                }
            }
            // Clear stale camera cache & shared PDFs
            File(context.cacheDir, "camera").deleteRecursively()
            File(context.cacheDir, "shared").deleteRecursively()
        }.onFailure {
            AppLogger.w("Media", "Orphan cleanup failed", it)
        }
    }
}

class VoiceRecorderSession(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start(destFile: File): Boolean {
        return try {
            stopAndRelease()
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioSamplingRate(16_000)
            mr.setAudioEncodingBitRate(32_000)
            mr.setAudioChannels(1)
            mr.setMaxDuration(600_000)
            mr.setOutputFile(destFile.absolutePath)
            mr.prepare()
            mr.start()
            recorder = mr
            outputFile = destFile
            true
        } catch (e: Exception) {
            AppLogger.e("VoiceRecorder", "Failed to start recorder", e)
            stopAndRelease()
            false
        }
    }

    fun pause() {
        runCatching { recorder?.pause() }
    }

    fun resume() {
        runCatching { recorder?.resume() }
    }

    fun stopAndRelease(): File? {
        val file = outputFile
        try {
            recorder?.apply {
                runCatching { stop() }
                reset()
                release()
            }
        } catch (_: Exception) {
        } finally {
            recorder = null
            outputFile = null
        }
        return file
    }
}

class VoicePlayerSession {
    private var player: MediaPlayer? = null

    fun start(filePath: String, speed: Float, onCompletion: () -> Unit): Boolean {
        return try {
            release()
            val file = File(filePath)
            if (!file.exists() || file.length() == 0L) return false
            val mp = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                playbackParams = playbackParams.setSpeed(speed)
                setOnCompletionListener { onCompletion() }
                start()
            }
            player = mp
            true
        } catch (e: Exception) {
            AppLogger.w("VoicePlayer", "Playback failed", e)
            release()
            false
        }
    }

    fun setSpeed(speed: Float) {
        runCatching {
            player?.let { mp ->
                if (mp.isPlaying) {
                    mp.playbackParams = mp.playbackParams.setSpeed(speed)
                }
            }
        }
    }

    fun currentPositionMs(): Long = runCatching { player?.currentPosition?.toLong() ?: 0L }.getOrDefault(0L)

    fun release() {
        runCatching {
            player?.apply {
                if (isPlaying) stop()
                release()
            }
        }
        player = null
    }
}
