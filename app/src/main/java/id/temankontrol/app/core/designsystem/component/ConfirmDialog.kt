package id.temankontrol.app.core.designsystem.component

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import id.temankontrol.app.R

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmButtonText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissButtonText: String = stringResource(R.string.action_cancel),
    isDestructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(text = confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissButtonText)
            }
        }
    )
}

@Composable
fun UnsavedChangesDialog(
    onConfirmDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmDialog(
        title = stringResource(R.string.dialog_unsaved_title),
        message = stringResource(R.string.dialog_unsaved_body),
        confirmButtonText = stringResource(R.string.action_discard),
        dismissButtonText = stringResource(R.string.action_stay),
        isDestructive = true,
        onConfirm = onConfirmDiscard,
        onDismiss = onDismiss
    )
}
