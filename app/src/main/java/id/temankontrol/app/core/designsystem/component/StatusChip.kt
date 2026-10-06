package id.temankontrol.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import id.temankontrol.app.R
import id.temankontrol.app.domain.model.ConsultationStatus
import id.temankontrol.app.ui.theme.*

private data class StatusStyle(
    val bg: Color,
    val fg: Color,
    val text: String,
    val icon: ImageVector
)

@Composable
fun StatusChip(
    status: ConsultationStatus,
    isPastDue: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val style = when {
        isPastDue && status == ConsultationStatus.SCHEDULED -> {
            val bg = if (isDark) StatusWarningDark.copy(alpha = 0.2f) else StatusWarningLight.copy(alpha = 0.15f)
            val fg = if (isDark) StatusWarningDark else StatusWarningLight
            StatusStyle(bg, fg, stringResource(R.string.status_past_due), Icons.Default.Warning)
        }
        status == ConsultationStatus.SCHEDULED -> {
            val bg = if (isDark) StatusScheduledDark.copy(alpha = 0.2f) else StatusScheduledLight.copy(alpha = 0.15f)
            val fg = if (isDark) StatusScheduledDark else StatusScheduledLight
            StatusStyle(bg, fg, stringResource(R.string.status_scheduled), Icons.Default.DateRange)
        }
        status == ConsultationStatus.COMPLETED -> {
            val bg = if (isDark) StatusCompletedDark.copy(alpha = 0.2f) else StatusCompletedLight.copy(alpha = 0.15f)
            val fg = if (isDark) StatusCompletedDark else StatusCompletedLight
            StatusStyle(bg, fg, stringResource(R.string.status_completed), Icons.Default.CheckCircle)
        }
        status == ConsultationStatus.CANCELLED -> {
            val bg = if (isDark) StatusCancelledDark.copy(alpha = 0.2f) else StatusCancelledLight.copy(alpha = 0.15f)
            val fg = if (isDark) StatusCancelledDark else StatusCancelledLight
            StatusStyle(bg, fg, stringResource(R.string.status_cancelled), Icons.Default.Close)
        }
        else -> StatusStyle(Color.LightGray, Color.DarkGray, "", Icons.Default.Close)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(style.bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = null,
            tint = style.fg,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = style.text,
            color = style.fg,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
