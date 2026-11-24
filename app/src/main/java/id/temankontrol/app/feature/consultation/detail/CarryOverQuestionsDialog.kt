package id.temankontrol.app.feature.consultation.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.temankontrol.app.R
import id.temankontrol.app.core.designsystem.component.DateUtils
import id.temankontrol.app.domain.model.ConsultationSummary
import id.temankontrol.app.domain.model.Question

@Composable
fun CarryOverQuestionsDialog(
    unansweredQuestions: List<Question>,
    scheduledTargets: List<ConsultationSummary>,
    onCopyToConsultation: (targetConsultationId: String, selectedQuestionIds: Set<String>) -> Unit,
    onCreateNewConsultation: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedQuestionIds by remember(unansweredQuestions) {
        mutableStateOf(unansweredQuestions.map { it.id }.toSet())
    }
    var selectedTargetId by remember(scheduledTargets) {
        mutableStateOf(scheduledTargets.firstOrNull()?.id?.value)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Bawa ${unansweredQuestions.size} pertanyaan belum dibahas?")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Pilih pertanyaan yang ingin disalin ke kunjungan berikutnya:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                unansweredQuestions.forEach { q ->
                    val checked = q.id in selectedQuestionIds
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedQuestionIds = if (checked) {
                                    selectedQuestionIds - q.id
                                } else {
                                    selectedQuestionIds + q.id
                                }
                            }
                            .padding(vertical = 2.dp)
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                selectedQuestionIds = if (isChecked) {
                                    selectedQuestionIds + q.id
                                } else {
                                    selectedQuestionIds - q.id
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = q.text,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider()

                Text(
                    text = "Pilih kunjungan tujuan:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                if (scheduledTargets.isEmpty()) {
                    Text(
                        text = "Belum ada kunjungan terjadwal lainnya.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    scheduledTargets.forEach { target ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedTargetId == target.id.value) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTargetId = target.id.value }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                RadioButton(
                                    selected = selectedTargetId == target.id.value,
                                    onClick = { selectedTargetId = target.id.value }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = target.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = DateUtils.formatRelativeDateTime(target.appointmentAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onCreateNewConsultation,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Buat Kunjungan Baru")
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedTargetId != null && selectedQuestionIds.isNotEmpty(),
                onClick = {
                    selectedTargetId?.let { targetId ->
                        onCopyToConsultation(targetId, selectedQuestionIds)
                    }
                }
            ) {
                Text("Bawa ke Kunjungan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
