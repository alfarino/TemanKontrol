package id.temankontrol.composeapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.temankontrol.composeapp.platform.createPlatformAlarmScheduler
import id.temankontrol.composeapp.platform.createPlatformMediaAdapter
import id.temankontrol.composeapp.platform.platformName

object SharedThemeTokens {
    val HeroTealStart = Color(0xFF0B6E72)
    val HeroTealEnd = Color(0xFF14919B)
    val AccentCoral = Color(0xFFF97316)
    val PorcelainMint = Color(0xFFF3F8F8)
}

@Composable
fun TemanKontrolSharedPreviewApp(
    onTriggerTestAlarm: (() -> Unit)? = null
) {
    val alarmScheduler = remember { createPlatformAlarmScheduler() }
    val mediaAdapter = remember { createPlatformMediaAdapter() }
    var testTriggered by remember { mutableStateOf(false) }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = SharedThemeTokens.HeroTealStart,
            background = SharedThemeTokens.PorcelainMint,
            surface = Color.White
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = SharedThemeTokens.PorcelainMint
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhoneIphone,
                        contentDescription = null,
                        tint = SharedThemeTokens.HeroTealStart
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Teman Kontrol (${platformName()})",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(SharedThemeTokens.HeroTealStart, SharedThemeTokens.HeroTealEnd)
                                )
                            )
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sinkronisasi Cloud • temankontrol.unand.tech",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Siapkan Pertanyaan Terbuka, Catat Jawaban Dokter, & Alarm Pengingat Kunjungan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Format Audio: ${mediaAdapter.supportedVoiceExtension()} • ${mediaAdapter.compressPhotoHint()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Button(
                    onClick = {
                        if (onTriggerTestAlarm != null) {
                            onTriggerTestAlarm()
                        } else {
                            alarmScheduler.triggerTestAlarm(
                                title = "Uji Coba Alarm Kunjungan",
                                message = "Alarm Teman Kontrol siap berbunyi di ${platformName()}!"
                            )
                        }
                        testTriggered = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.Alarm, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tes Bunyi Alarm Sekarang", fontWeight = FontWeight.Bold)
                }

                if (testTriggered) {
                    Text(
                        text = "Alarm uji telah dikirim ke sistem ${platformName()}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SharedThemeTokens.HeroTealStart
                    )
                }
            }
        }
    }
}
