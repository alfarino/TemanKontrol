package id.temankontrol.app.feature.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.temankontrol.app.BuildConfig
import id.temankontrol.app.R
import id.temankontrol.app.core.designsystem.component.AppScaffold
import id.temankontrol.app.core.designsystem.component.AppTopBar
import id.temankontrol.app.core.designsystem.component.SectionCard
import id.temankontrol.app.data.mock.MockScenario

@Composable
fun AboutScreen(
    onNavigateToNotice: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
    viewModel: AboutViewModel = hiltViewModel()
) {
    val activeScenario by viewModel.currentScenario.collectAsStateWithLifecycle()

    AboutContent(
        activeScenario = activeScenario,
        onNavigateToNotice = onNavigateToNotice,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToBackup = onNavigateToBackup,
        onNavigateToReminders = onNavigateToReminders,
        onSelectScenario = viewModel::selectScenario
    )
}

@Composable
fun AboutContent(
    activeScenario: MockScenario,
    onNavigateToNotice: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
    onSelectScenario: (MockScenario) -> Unit,
    modifier: Modifier = Modifier
) {
    AppScaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.nav_about)
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Info Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Versi ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Pengaturan, Cadangan & Pengingat
            SectionCard(
                title = "Cadangan, Pengingat & Keamanan"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onNavigateToBackup,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cadangan & Pemulihan (.tkbackup)")
                    }
                    OutlinedButton(
                        onClick = onNavigateToReminders,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pengingat & Notifikasi Lokal")
                    }
                    OutlinedButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pengaturan & Kunci Aplikasi")
                    }
                }
            }

            // Privacy & Data Storage (C-70)
            SectionCard(
                title = "Penyimpanan & Privasi Data"
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.about_data_privacy),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onNavigateToNotice,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Buka Ketentuan & Batas Penggunaan")
                }
            }

            // Developer Mock Scenario Switcher (Debug only)
            if (BuildConfig.DEBUG) {
                SectionCard(
                    title = "Developer Mock Scenario"
                ) {
                    Text(
                        text = "Pilih skenario data mock untuk pengujian UI (Normal, Kosong, Error, Lambat).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MockScenario.entries.forEach { scenario ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = activeScenario == scenario,
                                    onClick = { onSelectScenario(scenario) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = scenario.displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
