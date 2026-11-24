package id.temankontrol.app.feature.summary

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.temankontrol.app.R
import id.temankontrol.app.core.designsystem.component.*
import id.temankontrol.app.data.summary.SummaryPdfGenerator
import kotlinx.coroutines.launch

@Composable
fun SummaryPreviewScreen(
    onNavigateBack: () -> Unit,
    viewModel: SummaryPreviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val disclaimer = stringResource(R.string.summary_disclaimer)

    SummaryPreviewContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        disclaimer = disclaimer,
        onNavigateBack = onNavigateBack,
        onToggleProvider = viewModel::setIncludeProvider,
        onToggleQuestions = viewModel::setIncludeQuestions,
        onToggleNotes = viewModel::setIncludeNotes,
        onToggleFollowUps = viewModel::setIncludeFollowUps,
        onShareText = {
            val text = uiState.buildPlainTextSummary(disclaimer)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Bagikan Ringkasan"))
        },
        onSharePdf = {
            val c = uiState.consultation ?: return@SummaryPreviewContent
            val bodyText = uiState.buildPlainTextSummary(disclaimer)
            scope.launch {
                val res = SummaryPdfGenerator.generatePdfUri(
                    context = context,
                    consultationId = c.id.value,
                    title = c.title,
                    bodyText = bodyText
                )
                res.onSuccess { pdfUri ->
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, pdfUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Bagikan Ringkasan PDF"))
                }.onFailure {
                    snackbarHostState.showSnackbar("Gagal membuat berkas PDF ringkasan.")
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SummaryPreviewContent(
    uiState: SummaryPreviewUiState,
    snackbarHostState: SnackbarHostState,
    disclaimer: String,
    onNavigateBack: () -> Unit,
    onToggleProvider: (Boolean) -> Unit,
    onToggleQuestions: (Boolean) -> Unit,
    onToggleNotes: (Boolean) -> Unit,
    onToggleFollowUps: (Boolean) -> Unit,
    onShareText: () -> Unit,
    onSharePdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppScaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.summary_preview_title),
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHostState = snackbarHostState,
        modifier = modifier
    ) { padding ->
        if (uiState.isLoading) {
            LoadingView(modifier = Modifier.padding(padding))
            return@AppScaffold
        }

        val consultation = uiState.consultation
        if (consultation == null) {
            ErrorView(
                message = stringResource(R.string.error_consultation_not_found),
                actionText = stringResource(R.string.action_back),
                onActionClick = onNavigateBack,
                modifier = Modifier.padding(padding)
            )
            return@AppScaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Pilih bagian yang ingin disertakan:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = uiState.includeProvider,
                    onClick = { onToggleProvider(!uiState.includeProvider) },
                    label = { Text("Tenaga Kesehatan") }
                )
                FilterChip(
                    selected = uiState.includeQuestions,
                    onClick = { onToggleQuestions(!uiState.includeQuestions) },
                    label = { Text("Pertanyaan & Jawaban") }
                )
                FilterChip(
                    selected = uiState.includeNotes,
                    onClick = { onToggleNotes(!uiState.includeNotes) },
                    label = { Text("Hasil Konsultasi") }
                )
                FilterChip(
                    selected = uiState.includeFollowUps,
                    onClick = { onToggleFollowUps(!uiState.includeFollowUps) },
                    label = { Text("Tindak Lanjut") }
                )
            }

            // Document preview card (A4 look)
            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = consultation.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = DateUtils.formatRelativeDateTime(consultation.appointmentAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!consultation.facilityName.isNullOrBlank()) {
                        Text(
                            text = "Fasilitas: ${consultation.facilityName}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (uiState.includeProvider && !consultation.providerName.isNullOrBlank()) {
                        Text(
                            text = "Tenaga Kesehatan: ${consultation.providerName}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = "Alasan: ${consultation.reason}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (uiState.includeQuestions && uiState.questions.isNotEmpty()) {
                        HorizontalDivider()
                        Text(
                            text = "Pertanyaan & Jawaban",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        uiState.questions.forEachIndexed { index, q ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "${index + 1}. ${if (q.isDiscussed) "[✓] " else ""}${q.text}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                if (!q.answer.isNullOrBlank()) {
                                    Text(
                                        text = "Jawaban dicatat: ${q.answer}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.includeNotes && uiState.note != null && uiState.note.isNotEmpty()) {
                        HorizontalDivider()
                        Text(
                            text = "Hasil Konsultasi (Catatan Pengguna)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (!uiState.note.summary.isNullOrBlank()) {
                            Text("Ringkasan: ${uiState.note.summary}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (!uiState.note.providerInstruction.isNullOrBlank()) {
                            Text("Arahan: ${uiState.note.providerInstruction}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (!uiState.note.additionalNotes.isNullOrBlank()) {
                            Text("Catatan Tambahan: ${uiState.note.additionalNotes}", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    if (uiState.includeFollowUps && uiState.followUps.isNotEmpty()) {
                        HorizontalDivider()
                        Text(
                            text = "Tindak Lanjut",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        uiState.followUps.forEach { action ->
                            val status = if (action.isCompleted) "Selesai" else "Terbuka"
                            val due = action.dueDate?.let { " • Tenggat ${DateUtils.formatLocalDate(it)}" } ?: ""
                            Text(
                                text = "• [$status] ${action.text}$due",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    HorizontalDivider()
                    Text(
                        text = disclaimer,
                        style = MaterialTheme.typography.labelSmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onSharePdf,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bagikan PDF")
                }

                Button(
                    onClick = onShareText,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bagikan Teks")
                }
            }
        }
    }
}
