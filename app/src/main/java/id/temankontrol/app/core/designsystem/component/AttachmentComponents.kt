package id.temankontrol.app.core.designsystem.component

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import id.temankontrol.app.R
import id.temankontrol.app.data.media.MediaAttachmentManager
import id.temankontrol.app.data.media.VoicePlayerSession
import id.temankontrol.app.data.media.VoiceRecorderSession
import id.temankontrol.app.domain.model.Attachment
import id.temankontrol.app.domain.model.AttachmentKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@Composable
fun AttachmentSection(
    attachments: List<Attachment>,
    onAddPhoto: (caption: String?, uriString: String?) -> Unit,
    onAddVoiceNote: (durationMs: Long, caption: String?, uriString: String?) -> Unit,
    onDeleteAttachment: (Attachment) -> Unit,
    onUpdateCaption: (String, String?) -> Unit = { _, _ -> },
    consultationId: String = "default",
    title: String = "Lampiran (Foto & Catatan Suara)",
    emptyHint: String = "Tambahkan foto hasil pemeriksaan atau rekaman suara penjelasan.",
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showPhotoDialog by remember { mutableStateOf(false) }
    var showVoiceSheet by remember { mutableStateOf(false) }
    var selectedPhoto by remember { mutableStateOf<Attachment?>(null) }
    var attachmentToDelete by remember { mutableStateOf<Attachment?>(null) }

    val photos = remember(attachments) { attachments.filter { it.kind == AttachmentKind.PHOTO } }
    val voices = remember(attachments) { attachments.filter { it.kind == AttachmentKind.VOICE } }

    if (showPhotoDialog) {
        PhotoPickerSourceDialog(
            consultationId = consultationId,
            onConfirm = { caption, uriString ->
                showPhotoDialog = false
                onAddPhoto(caption, uriString)
            },
            onDismiss = { showPhotoDialog = false }
        )
    }

    if (showVoiceSheet) {
        VoiceRecorderBottomSheet(
            consultationId = consultationId,
            onSave = { durationMs, caption, uriString ->
                showVoiceSheet = false
                onAddVoiceNote(durationMs, caption, uriString)
            },
            onDismiss = { showVoiceSheet = false }
        )
    }

    if (selectedPhoto != null) {
        PhotoViewerDialog(
            attachment = selectedPhoto!!,
            onSaveCaption = { newCaption ->
                onUpdateCaption(selectedPhoto!!.id, newCaption)
                selectedPhoto = selectedPhoto!!.copy(caption = newCaption)
            },
            onDelete = {
                attachmentToDelete = selectedPhoto
                selectedPhoto = null
            },
            onDismiss = { selectedPhoto = null }
        )
    }

    if (attachmentToDelete != null) {
        val kindLabel = if (attachmentToDelete!!.kind == AttachmentKind.PHOTO) "foto" else "catatan suara"
        ConfirmDialog(
            title = "Hapus $kindLabel?",
            message = "Lampiran ini akan dihapus secara permanen.",
            confirmButtonText = stringResource(R.string.action_delete),
            isDestructive = true,
            onConfirm = {
                onDeleteAttachment(attachmentToDelete!!)
                attachmentToDelete = null
            },
            onDismiss = { attachmentToDelete = null }
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        if (!compact) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (attachments.isEmpty()) {
            Text(
                text = emptyHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            if (photos.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(photos, key = { it.id }) { photo ->
                        PhotoThumbnailCard(
                            attachment = photo,
                            onClick = { selectedPhoto = photo }
                        )
                    }
                }
            }

            if (voices.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    voices.forEach { voice ->
                        VoiceNotePlayerCard(
                            attachment = voice,
                            onDeleteClick = { attachmentToDelete = voice }
                        )
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { showPhotoDialog = true },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tambah Foto", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            OutlinedButton(
                onClick = { showVoiceSheet = true },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Rekam VN", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun CompactAttachmentBadges(
    photoCount: Int,
    voiceCount: Int,
    modifier: Modifier = Modifier
) {
    if (photoCount == 0 && voiceCount == 0) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        if (photoCount > 0) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$photoCount Foto",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        if (voiceCount > 0) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$voiceCount VN",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoThumbnailCard(
    attachment: Attachment,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, attachment.uriString) {
        val uriStr = attachment.uriString
        if (!uriStr.isNullOrBlank()) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    val uri = Uri.parse(uriStr)
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        val maxDim = maxOf(info.size.width, info.size.height).coerceAtLeast(1)
                        val scale = minOf(1f, 320f / maxDim)
                        decoder.setTargetSize(
                            (info.size.width * scale).toInt().coerceAtLeast(1),
                            (info.size.height * scale).toInt().coerceAtLeast(1)
                        )
                    }
                }.getOrNull()
            }
        }
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .width(120.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = attachment.caption ?: attachment.fileName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = attachment.caption ?: attachment.fileName,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = attachment.caption ?: attachment.fileName,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatSizeKb(attachment.sizeBytes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VoiceNotePlayerCard(
    attachment: Attachment,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val totalMs = (attachment.durationMs ?: 30_000L).coerceAtLeast(1_000L)
    var isPlaying by remember(attachment.id) { mutableStateOf(false) }
    var elapsedMs by remember(attachment.id) { mutableLongStateOf(0L) }
    var speedIndex by remember(attachment.id) { mutableIntStateOf(0) }
    val speeds = listOf(1f, 1.5f, 2f)
    val speed = speeds[speedIndex]
    val playerSession = remember(attachment.id) { VoicePlayerSession() }
    var usingRealAudio by remember(attachment.id) { mutableStateOf(false) }

    val localFile = remember(attachment) {
        val fromUri = attachment.uriString?.let { Uri.parse(it).path }?.let { File(it) }
        if (fromUri != null && fromUri.exists()) fromUri
        else File(File(File(context.filesDir, "attachments"), attachment.consultationId.value), attachment.fileName)
    }

    DisposableEffect(lifecycleOwner, attachment.id) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && isPlaying) {
                isPlaying = false
                playerSession.release()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            playerSession.release()
        }
    }

    LaunchedEffect(isPlaying, speed) {
        if (isPlaying) {
            if (localFile.exists() && localFile.length() > 0L) {
                usingRealAudio = playerSession.start(localFile.absolutePath, speed) {
                    isPlaying = false
                    elapsedMs = 0L
                }
            }
            while (isPlaying && elapsedMs < totalMs) {
                delay(200L)
                if (usingRealAudio) {
                    elapsedMs = playerSession.currentPositionMs().coerceAtMost(totalMs)
                } else {
                    elapsedMs = (elapsedMs + (200L * speed).toLong()).coerceAtMost(totalMs)
                    if (elapsedMs >= totalMs) {
                        isPlaying = false
                        elapsedMs = 0L
                    }
                }
            }
        } else {
            playerSession.release()
            usingRealAudio = false
        }
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                RoundedCornerShape(10.dp)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            IconButton(
                onClick = { isPlaying = !isPlaying }
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                    contentDescription = if (isPlaying) "Jeda catatan suara" else "Putar catatan suara",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.caption ?: attachment.fileName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (elapsedMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${formatDuration(elapsedMs)} / ${formatDuration(totalMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatSizeKb(attachment.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TextButton(
                onClick = {
                    speedIndex = (speedIndex + 1) % speeds.size
                    if (usingRealAudio) playerSession.setSpeed(speeds[speedIndex])
                },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (speed == 1f) "1x" else if (speed == 1.5f) "1,5x" else "2x",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Hapus catatan suara",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PhotoPickerSourceDialog(
    consultationId: String,
    onConfirm: (caption: String?, uriString: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mediaManager = remember(context) { MediaAttachmentManager(context.applicationContext) }

    var caption by remember { mutableStateOf("") }
    var processedUri by remember { mutableStateOf<String?>(null) }
    var isCompressing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingCameraFile by remember { mutableStateOf<Pair<Uri, File>?>(null) }

    fun processSourceUri(sourceUri: Uri, cleanupTemp: File? = null) {
        scope.launch {
            isCompressing = true
            errorMessage = null
            val res = mediaManager.saveCompressedPhoto(consultationId, sourceUri)
            cleanupTemp?.let { runCatching { it.delete() } }
            isCompressing = false
            res.onSuccess { processed ->
                processedUri = processed.uriString
            }.onFailure { err ->
                errorMessage = err.message ?: "Gagal memproses foto."
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            processSourceUri(uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val pair = pendingCameraFile
        if (success && pair != null) {
            processSourceUri(pair.first, cleanupTemp = pair.second)
        } else {
            pair?.second?.let { runCatching { it.delete() } }
        }
        pendingCameraFile = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Lampiran Foto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PrivacyTip,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.hint_photo_privacy),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        enabled = !isCompressing,
                        onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Galeri")
                    }

                    OutlinedButton(
                        enabled = !isCompressing,
                        onClick = {
                            runCatching {
                                val pair = mediaManager.createCameraTempUri()
                                pendingCameraFile = pair
                                cameraLauncher.launch(pair.first)
                            }.onFailure {
                                errorMessage = "Kamera tidak tersedia di perangkat ini."
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kamera")
                    }
                }

                if (isCompressing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = "Mengompresi & membersihkan metadata EXIF…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (processedUri != null) {
                    Text(
                        text = "Foto terkompresi (≤ 1.600 px, tanpa EXIF lokasi) siap dilampirkan.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                LabeledTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = "Keterangan Foto (Opsional)",
                    placeholder = "Contoh: Hasil cek lab darah",
                    maxLength = 100
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isCompressing,
                onClick = {
                    onConfirm(caption.ifBlank { null }, processedUri)
                }
            ) {
                Text("Lampirkan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceRecorderBottomSheet(
    consultationId: String,
    onSave: (durationMs: Long, caption: String?, uriString: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mediaManager = remember(context) { MediaAttachmentManager(context.applicationContext) }
    val recorderSession = remember(context) { VoiceRecorderSession(context.applicationContext) }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var caption by remember { mutableStateOf("") }
    var activeFile by remember { mutableStateOf<File?>(null) }
    var statusError by remember { mutableStateOf<String?>(null) }
    val maxDurationMs = 600_000L

    fun startRealRecording() {
        if (!mediaManager.hasEnoughStorage()) {
            statusError = "Ruang penyimpanan HP hampir penuh (< 50 MB)."
            return
        }
        val (_, file) = mediaManager.prepareVoiceOutputFile(consultationId)
        if (recorderSession.start(file)) {
            activeFile = file
            isRecording = true
            isPaused = false
            statusError = null
        } else {
            runCatching { file.delete() }
            statusError = "Mikrofon tidak tersedia pada perangkat ini."
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        permissionDenied = !granted
        if (granted) {
            startRealRecording()
        }
    }

    // Auto-stop & preserve when going to background (FR-050)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && (isRecording || isPaused)) {
                val savedFile = recorderSession.stopAndRelease()
                isRecording = false
                isPaused = false
                if (savedFile != null && savedFile.exists() && elapsedMs >= 1_000L) {
                    onSave(elapsedMs, caption.ifBlank { null }, Uri.fromFile(savedFile).toString())
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            recorderSession.stopAndRelease()
        }
    }

    LaunchedEffect(isRecording, isPaused) {
        while (isRecording && !isPaused && elapsedMs < maxDurationMs) {
            delay(1000L)
            elapsedMs = (elapsedMs + 1000L).coerceAtMost(maxDurationMs)
            if (elapsedMs >= maxDurationMs) {
                recorderSession.stopAndRelease()
                isRecording = false
                isPaused = false
            }
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "rec_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    ModalBottomSheet(
        onDismissRequest = {
            recorderSession.stopAndRelease()?.let { if (elapsedMs < 1_000L) runCatching { it.delete() } }
            onDismiss()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Rekam Catatan Suara (VN)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.hint_voice_consent),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            if (permissionDenied && !hasAudioPermission) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text(
                            text = "Izin mikrofon diperlukan untuk merekam catatan suara.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }) {
                            Text("Pengaturan")
                        }
                    }
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(84.dp)
                    .scale(if (isRecording && !isPaused) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(
                        if (isRecording && !isPaused) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    )
                    .clickable {
                        when {
                            !hasAudioPermission -> audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            !isRecording -> startRealRecording()
                            !isPaused -> {
                                recorderSession.pause()
                                isPaused = true
                            }
                            else -> {
                                recorderSession.resume()
                                isPaused = false
                            }
                        }
                    }
            ) {
                Icon(
                    imageVector = if (isRecording && !isPaused) Icons.Default.Pause else Icons.Default.Mic,
                    contentDescription = if (isRecording && !isPaused) "Jeda rekaman" else "Mulai rekaman",
                    tint = if (isRecording && !isPaused) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(38.dp)
                )
            }

            Text(
                text = "${formatDuration(elapsedMs)} / 10:00",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isRecording && !isPaused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = when {
                    isRecording && !isPaused -> "Sedang merekam… ketuk ikon untuk jeda."
                    isPaused -> "Rekaman dijeda. Ketuk ikon untuk lanjut atau simpan."
                    else -> "Ketuk ikon mikrofon untuk mulai merekam."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (statusError != null) {
                Text(
                    text = statusError!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            LabeledTextField(
                value = caption,
                onValueChange = { caption = it },
                label = "Keterangan Rekaman (Opsional)",
                placeholder = "Contoh: Penjelasan dosis & efek samping",
                maxLength = 100
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        recorderSession.stopAndRelease()?.let { runCatching { it.delete() } }
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }

                Button(
                    onClick = {
                        val savedFile = recorderSession.stopAndRelease() ?: activeFile
                        isRecording = false
                        isPaused = false
                        val dur = elapsedMs.coerceAtLeast(1_000L)
                        val uriStr = savedFile?.takeIf { it.exists() && it.length() > 0L }?.let { Uri.fromFile(it).toString() }
                        onSave(dur, caption.ifBlank { null }, uriStr)
                    },
                    enabled = elapsedMs >= 1_000L,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Simpan VN")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PhotoViewerDialog(
    attachment: Attachment,
    onSaveCaption: (String?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var caption by remember(attachment.id) { mutableStateOf(attachment.caption ?: "") }
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, attachment.uriString) {
        val uriStr = attachment.uriString
        if (!uriStr.isNullOrBlank()) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    val uri = Uri.parse(uriStr)
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                }.getOrNull()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = attachment.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus foto",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = attachment.caption ?: attachment.fileName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Pratinjau Foto (${formatSizeKb(attachment.sizeBytes)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                LabeledTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = "Keterangan Foto",
                    placeholder = "Tambahkan keterangan...",
                    maxLength = 100
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveCaption(caption.ifBlank { null })
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return String.format(Locale.US, "%02d:%02d", min, sec)
}

fun formatSizeKb(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    return if (kb < 1024.0) {
        String.format(Locale.US, "%.0f KB", kb)
    } else {
        String.format(Locale.US, "%.1f MB", kb / 1024.0)
    }
}
