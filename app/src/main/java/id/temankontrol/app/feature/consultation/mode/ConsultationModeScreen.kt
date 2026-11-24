package id.temankontrol.app.feature.consultation.mode

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.temankontrol.app.R
import id.temankontrol.app.core.designsystem.component.*
import id.temankontrol.app.domain.model.Attachment

@Composable
fun ConsultationModeScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNotes: (String) -> Unit,
    onNavigateToQuestions: (String) -> Unit,
    viewModel: ConsultationModeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Keep screen on during Mode Konsultasi (F-15)
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    ConsultationModeContent(
        uiState = uiState,
        consultationId = viewModel.consultationIdString,
        onNavigateBack = {
            uiState.currentQuestion?.let { viewModel.saveCurrentAnswer(it.id) }
            onNavigateBack()
        },
        onToggleDiscussed = viewModel::toggleDiscussed,
        onAnswerChange = viewModel::onAnswerDraftChange,
        onSaveAnswer = viewModel::saveCurrentAnswer,
        onPrevious = viewModel::previousQuestion,
        onNext = viewModel::nextQuestion,
        onAddPhoto = viewModel::addPhoto,
        onAddVoiceNote = viewModel::addVoiceNote,
        onDeleteAttachment = viewModel::deleteAttachment,
        onNavigateToNotes = { onNavigateToNotes(viewModel.consultationIdString) },
        onNavigateToQuestions = { onNavigateToQuestions(viewModel.consultationIdString) },
        onMarkCompleted = { viewModel.markConsultationCompleted(onDone = onNavigateBack) }
    )
}

@Composable
fun ConsultationModeContent(
    uiState: ConsultationModeUiState,
    consultationId: String = "default",
    onNavigateBack: () -> Unit,
    onToggleDiscussed: (String, Boolean) -> Unit,
    onAnswerChange: (String, String) -> Unit,
    onSaveAnswer: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onAddPhoto: (String, String?, String?) -> Unit,
    onAddVoiceNote: (String, Long, String?, String?) -> Unit,
    onDeleteAttachment: (Attachment) -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToQuestions: () -> Unit,
    onMarkCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppScaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.consultation_mode_title),
                onNavigateBack = onNavigateBack
            )
        },
        modifier = modifier
    ) { padding ->
        when {
            uiState.isLoading -> {
                LoadingView(modifier = Modifier.padding(padding))
            }
            uiState.questions.isEmpty() -> {
                EmptyView(
                    title = "Belum ada pertanyaan untuk kunjungan ini. Tambahkan pertanyaan sebelum memulai mode konsultasi.",
                    actionText = "Kelola Pertanyaan",
                    onActionClick = onNavigateToQuestions,
                    modifier = Modifier.padding(padding)
                )
            }
            uiState.isFinished -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Konsultasi Selesai Ditinjau",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${uiState.discussedCount} dari ${uiState.questions.size} pertanyaan sudah ditandai dibahas.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Button(
                        onClick = onNavigateToNotes,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Isi Hasil Konsultasi")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onMarkCompleted,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Tandai Kunjungan Selesai")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onPrevious) {
                        Text("Kembali ke Pertanyaan Terakhir")
                    }
                }
            }
            else -> {
                val question = uiState.currentQuestion ?: return@AppScaffold
                val attachments = uiState.attachmentsByQuestion[question.id].orEmpty()
                val progress = (uiState.currentIndex + 1).toFloat() / uiState.questions.size.toFloat()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .imePadding()
                        .padding(16.dp)
                ) {
                    // Progress header
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Pertanyaan ${uiState.currentIndex + 1} dari ${uiState.questions.size}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (question.isPriority) {
                            PriorityBadge()
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scrollable focus card
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = question.text,
                                    fontSize = 24.sp,
                                    lineHeight = 32.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                FilterChip(
                                    selected = question.isDiscussed,
                                    onClick = { onToggleDiscussed(question.id, !question.isDiscussed) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (question.isDiscussed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null
                                        )
                                    },
                                    label = {
                                        Text(
                                            if (question.isDiscussed) "Sudah Dibahas" else "Tandai Sudah Dibahas",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                )
                            }
                        }

                        LabeledTextField(
                            value = uiState.draftAnswer,
                            onValueChange = { onAnswerChange(question.id, it) },
                            label = "Jawaban yang Anda catat",
                            placeholder = "Tulis poin jawaban tenaga kesehatan di sini...",
                            maxLength = 1000,
                            singleLine = false,
                            minLines = 3
                        )

                        AttachmentSection(
                            attachments = attachments,
                            consultationId = consultationId,
                            title = "Lampiran untuk Pertanyaan Ini",
                            emptyHint = "Rekam penjelasan suara atau ambil foto jika diperlukan.",
                            onAddPhoto = { caption, uri -> onAddPhoto(question.id, caption, uri) },
                            onAddVoiceNote = { dur, caption, uri -> onAddVoiceNote(question.id, dur, caption, uri) },
                            onDeleteAttachment = onDeleteAttachment
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom nav buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onPrevious,
                            enabled = uiState.currentIndex > 0,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sebelumnya")
                        }

                        Button(
                            onClick = {
                                onSaveAnswer(question.id)
                                onNext()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(if (uiState.currentIndex == uiState.questions.lastIndex) "Selesai" else "Berikutnya")
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
